package com.v2ray.ang.handler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.ProfileItem
import com.v2ray.ang.extension.toBitSpeedString
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.util.MessageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mtkdex.core.build.ssmen.activities.MainBaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationManager {
    private const val NOTIFICATION_ID = 512
    private const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
    private const val NOTIFICATION_PENDING_INTENT_STOP_V2RAY = 1
    private const val NOTIFICATION_PENDING_INTENT_RESTART_V2RAY = 2

    private var lastQueryTime = 0L
    private var lastUp = 0L
    private var lastDown = 0L
    private var sessionTotalUp = 0L
    private var sessionTotalDown = 0L
    private var mBuilder: NotificationCompat.Builder? = null
    private var speedNotificationJob: Job? = null
    private var mNotificationManager: NotificationManager? = null

    /**
     * Starts the speed notification.
     * @param currentConfig The current profile configuration.
     */
    fun startSpeedNotification(currentConfig: ProfileItem?) {
        if (speedNotificationJob != null || !V2RayServiceManager.isRunning()) return

        lastQueryTime = System.currentTimeMillis()
        lastUp = V2RayServiceManager.queryStats(AppConfig.TAG_PROXY, AppConfig.UPLINK)
        lastDown = V2RayServiceManager.queryStats(AppConfig.TAG_PROXY, AppConfig.DOWNLINK)

        // Send initial stats immediately to UI
        getService()?.let {
            MessageUtil.sendMsg2UI(it, AppConfig.MSG_STATE_STATS, longArrayOf(sessionTotalDown, sessionTotalUp))
        }

        val loopStartTime = System.currentTimeMillis()
        speedNotificationJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsedSinceStart = now - loopStartTime
                val nextTick = ((elapsedSinceStart / 1000) + 1) * 1000
                delay(nextTick - elapsedSinceStart)
                
                val queryTime = System.currentTimeMillis()
                val sinceLastQueryInSeconds = (queryTime - lastQueryTime) / 1000.0
                if (sinceLastQueryInSeconds < 0.1) continue

                val currentUpProxy = V2RayServiceManager.queryStats(AppConfig.TAG_PROXY, AppConfig.UPLINK)
                val currentDownProxy = V2RayServiceManager.queryStats(AppConfig.TAG_PROXY, AppConfig.DOWNLINK)
                
                // Fallback to system traffic if proxy tag returns 0
                val currentUpRaw = if (currentUpProxy > 0) currentUpProxy else V2RayServiceManager.queryStats("", AppConfig.UPLINK)
                val currentDownRaw = if (currentDownProxy > 0) currentDownProxy else V2RayServiceManager.queryStats("", AppConfig.DOWNLINK)
                
                // Track monotonic session totals (handles core restarts)
                // If core restarts, currentUpRaw will be less than lastUp (reset to 0)
                if (currentUpRaw >= lastUp) {
                    sessionTotalUp += (currentUpRaw - lastUp)
                } else {
                    sessionTotalUp += currentUpRaw
                }
                
                if (currentDownRaw >= lastDown) {
                    sessionTotalDown += (currentDownRaw - lastDown)
                } else {
                    sessionTotalDown += currentDownRaw
                }

                val upSpeed = ((currentUpRaw - lastUp).coerceAtLeast(0L) / sinceLastQueryInSeconds).coerceAtLeast(0.0).toLong()
                val downSpeed = ((currentDownRaw - lastDown).coerceAtLeast(0L) / sinceLastQueryInSeconds).coerceAtLeast(0.0).toLong()

                // Send stats to UI process for real-time display
                getService()?.let {
                    MessageUtil.sendMsg2UI(it, AppConfig.MSG_STATE_STATS, longArrayOf(sessionTotalDown, sessionTotalUp))
                }

                val serverName = MmkvManager.decodeSettingsString(MmkvManager.KEY_SELECTED_SERVER_NAME) ?: currentConfig?.remarks ?: "Server"
                val payloadName = MmkvManager.decodeSettingsString(MmkvManager.KEY_SELECTED_PAYLOAD_NAME) ?: "Default"

                val statsText = "↓ ${downSpeed.toBitSpeedString()} ⚡ ${sessionTotalDown.toTrafficString()} ↑ ${upSpeed.toBitSpeedString()} ⚡ ${sessionTotalUp.toTrafficString()}"
                
                val notificationContent = "$serverName • $payloadName\n$statsText"
                updateNotification(notificationContent)

                lastUp = currentUpRaw
                lastDown = currentDownRaw
                lastQueryTime = queryTime
            }
        }
    }

    /**
     * Resets the session totals. Should be called when the user disconnects.
     */
    fun resetSessionStats() {
        sessionTotalUp = 0L
        sessionTotalDown = 0L
        lastUp = 0L
        lastDown = 0L
    }

    /**
     * Shows the notification.
     * @param currentConfig The current profile configuration.
     */
    fun showNotification(currentConfig: ProfileItem?) {
        val service = getService() ?: return
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val startMainIntent = Intent(service, MainBaseActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(service, NOTIFICATION_PENDING_INTENT_CONTENT, startMainIntent, flags)

        val stopV2RayIntent = Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        stopV2RayIntent.putExtra("key", AppConfig.MSG_STATE_STOP)
        val stopV2RayPendingIntent = PendingIntent.getBroadcast(service, NOTIFICATION_PENDING_INTENT_STOP_V2RAY, stopV2RayIntent, flags)

        val restartV2RayIntent = Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        restartV2RayIntent.putExtra("key", AppConfig.MSG_STATE_RESTART)
        val restartV2RayPendingIntent = PendingIntent.getBroadcast(service, NOTIFICATION_PENDING_INTENT_RESTART_V2RAY, restartV2RayIntent, flags)

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                ""
            }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())

        mBuilder = NotificationCompat.Builder(service, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setLargeIcon(BitmapFactory.decodeResource(service.resources, R.drawable.icon_icon))
            .setContentTitle("FIGHTER V2RAY  $time")
            .setContentText("Connecting...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_delete_24dp,
                "Reconnect",
                restartV2RayPendingIntent
            )
            .addAction(
                R.drawable.ic_delete_24dp,
                "Disconnect",
                stopV2RayPendingIntent
            )

        service.startForeground(NOTIFICATION_ID, mBuilder?.build())
    }

    /**
     * Cancels the notification.
     */
    fun cancelNotification() {
        val service = getService() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            service.stopForeground(true)
        }

        mBuilder = null
        speedNotificationJob?.cancel()
        speedNotificationJob = null
        mNotificationManager = null
    }

    /**
     * Stops the speed notification.
     * @param currentConfig The current profile configuration.
     */
    fun stopSpeedNotification(currentConfig: ProfileItem?) {
        speedNotificationJob?.let {
            it.cancel()
            speedNotificationJob = null
            updateNotification("Disconnected")
        }
    }

    /**
     * Creates a notification channel for Android O and above.
     * @return The channel ID.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = AppConfig.RAY_NG_CHANNEL_ID
        val channelName = AppConfig.RAY_NG_CHANNEL_NAME
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_LOW
        )
        chan.lightColor = Color.DKGRAY
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getNotificationManager()?.createNotificationChannel(chan)
        return channelId
    }

    /**
     * Updates the notification with the given content text.
     * @param contentText The content text for BigTextStyle.
     */
    private fun updateNotification(contentText: String?) {
        if (mBuilder != null) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = timeFormat.format(Date())
            
            val lines = contentText?.split("\n")
            val nameText = lines?.getOrNull(0) ?: ""
            val statsText = if (lines != null && lines.size > 1) lines[1] else ""
            
            // Server Name as Title
            mBuilder?.setContentTitle(nameText)
            // Speed Stats as Content Text (Line 2)
            mBuilder?.setContentText(statsText)
            // "Connected" and Time as Subtext
            mBuilder?.setSubText("Connected  •  $time")
            
            // Expanded view
            mBuilder?.setStyle(NotificationCompat.BigTextStyle().bigText("$nameText\n$statsText"))

            getNotificationManager()?.notify(NOTIFICATION_ID, mBuilder?.build())
        }
    }

    /**
     * Gets the notification manager.
     */
    private fun getNotificationManager(): NotificationManager? {
        if (mNotificationManager == null) {
            val service = getService() ?: return null
            mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return mNotificationManager
    }

    /**
     * Gets the service instance.
     * @return The service instance.
     */
    private fun getService(): Service? {
        return V2RayServiceManager.serviceControl?.get()?.getService()
    }
}
