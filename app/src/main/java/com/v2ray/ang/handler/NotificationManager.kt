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
import android.net.TrafficStats
import android.os.Process
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
    
    // Track UID stats to get accurate overall traffic
    private var startUidUp = 0L
    private var startUidDown = 0L
    
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
        
        // Initialize UID stats snapshots
        val uid = Process.myUid()
        startUidDown = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0L)
        startUidUp = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0L)
        
        // Initial V2Ray stats snapshot for speed calculation
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

                // 1. Calculate Session Totals using TrafficStats (Most Accurate for total volume)
                val currentUidDown = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0L)
                val currentUidUp = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0L)
                
                if (currentUidDown >= startUidDown) {
                    sessionTotalDown = currentUidDown - startUidDown
                }
                if (currentUidUp >= startUidUp) {
                    sessionTotalUp = currentUidUp - startUidUp
                }

                // 2. Calculate Real-time Speed using V2Ray core stats (More responsive for speed)
                val tags = mutableListOf(AppConfig.TAG_PROXY, AppConfig.TAG_DIRECT, AppConfig.TAG_FRAGMENT, AppConfig.TAG_DNS, "dns-out")
                for (i in 1..8) tags.add("${AppConfig.TAG_PROXY}-$i")

                var currentUpRaw = 0L
                var currentDownRaw = 0L
                for (tag in tags) {
                    currentUpRaw += V2RayServiceManager.queryStats(tag, AppConfig.UPLINK)
                    currentDownRaw += V2RayServiceManager.queryStats(tag, AppConfig.DOWNLINK)
                }
                
                // Fallback for speed query
                if (currentUpRaw <= 0) currentUpRaw = V2RayServiceManager.queryStats("", AppConfig.UPLINK)
                if (currentDownRaw <= 0) currentDownRaw = V2RayServiceManager.queryStats("", AppConfig.DOWNLINK)

                val upSpeed = ((currentUpRaw - lastUp).coerceAtLeast(0L) / sinceLastQueryInSeconds).coerceAtLeast(0.0).toLong()
                val downSpeed = ((currentDownRaw - lastDown).coerceAtLeast(0L) / sinceLastQueryInSeconds).coerceAtLeast(0.0).toLong()

                // Send stats to UI process for real-time display
                getService()?.let {
                    MessageUtil.sendMsg2UI(it, AppConfig.MSG_STATE_STATS, longArrayOf(sessionTotalDown, sessionTotalUp))
                }

                val serverName = MmkvManager.decodeSettingsString(MmkvManager.KEY_SELECTED_SERVER_NAME) ?: currentConfig?.remarks ?: "Server"
                val payloadName = MmkvManager.decodeSettingsString(MmkvManager.KEY_SELECTED_PAYLOAD_NAME) ?: "Default"

                val statsText = "↓ ${downSpeed.toBitSpeedString()} ⚡ ${sessionTotalDown.toTrafficString()} ↑ ${upSpeed.toBitSpeedString()} ⚡ ${sessionTotalUp.toTrafficString()}"
                
                val combinedInfo = when {
                    serverName.equals(payloadName, ignoreCase = true) -> serverName
                    serverName.contains(payloadName, ignoreCase = true) -> serverName
                    payloadName.contains(serverName, ignoreCase = true) -> payloadName
                    else -> "$serverName • $payloadName"
                }
                val notificationContent = "$combinedInfo\n$statsText"
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
            
            // Expanded view - only show stats to avoid title repetition
            mBuilder?.setStyle(NotificationCompat.BigTextStyle().bigText(statsText))

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
