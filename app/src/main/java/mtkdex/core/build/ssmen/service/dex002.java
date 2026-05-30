package mtkdex.core.build.ssmen.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;

import com.v2ray.ang.MainApplication;

import com.v2ray.ang.handler.MmkvManager;
import com.v2ray.ang.handler.V2RayServiceManager;
import com.v2ray.ang.service.ServiceControl;
import com.v2ray.ang.util.SpeedtestUtil;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.activities.MainActivity;
import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import mtkdex.core.build.ssmen.logger.ConnectionStatus;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import mtkdex.core.build.ssmen.thread.rm_002;
import mtkdex.core.build.ssmen.thread.SocketProxyThread;
import mtkdex.core.build.ssmen.thread.rm_003;
import mtkdex.core.build.ssmen.utils.util;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class dex002 extends Service implements Handler.Callback, SettingsConstants, hLogStatus.StateListener, hLogStatus.ByteCountListener {
    private static final String TAG = "dex002";
    public static final String START_SERVICE = "mtkdex.core.build.org:startTunnel";
    public static final String STOP_SERVICE = "mtkdex.core.build.org:stopTunnel";
    public static final String RECONNECT_SERVICE = "mtkdex.core.build.org:reconnecTunnel";
    public static final String RESTART_SERVICE = "mtkdex.core.build.org:restartTunnel";
    private final int NOTIFICATION_ID = 123;
    public int OVPN = 0,SSH_DNS = 1,V2RAY = 2,UDP = 3,PROXY_TUNNEL = 4;
    public static final String NOTIFICATION_CHANNEL_BG_ID = "NOTIFICATION_CHANNEL_ID";
    private NotificationManager nm;
    private NotificationCompat.Builder mNotifyBuilder = null;
    public static boolean isRunning = false;
    private ConfigUtil mConfig;
    private rm_003 mRm003;
    public static boolean mStopping = false;
    private dex002.InjectorListener InjectorListener;
    private static boolean mDisplayBytecount = false;
    private rm_002 mRm002;

    private dex001 DNSTT_TunnelThread;
    private SharedPreferences mPref;
    public Handler mHandler;
    private static String mServer_type;
    private static String mStateReceiver;
    private PowerManager.WakeLock wakeLock;

    private final Handler expiryHandler = new Handler(Looper.getMainLooper());
    private final Runnable expiryCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (isVPNRunning()) {
                checkAccountExpiry();
                expiryHandler.postDelayed(this, 60000); // Check every minute
            }
        }
    };

    private void checkAccountExpiry() {
        String rawExpiry = mPref.getString("_AccountRawXp", "");
        if (!rawExpiry.isEmpty() && !rawExpiry.equals("none")) {
            if (util.getDaysLeft(rawExpiry).equals("Expired")) {
                hLogStatus.logInfo("<font color='#d50000'>Account expired! Disconnecting...</font>");
                sendExpiryNotification();
                closeAll();
            }
        }
    }

    private void sendExpiryNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "expiry_alerts";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Account Expiry", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.icon_icon)
                .setContentTitle("Account Expired")
                .setContentText("Your VPN account has expired. Disconnected.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(ConfigUtil.getPendingIntent(this));

        notificationManager.notify(1001, builder.build());
    }

    private void startDNSTTTunnel() {
        if (DNSTT_TunnelThread != null) {
            DNSTT_TunnelThread.interrupt();
            DNSTT_TunnelThread = null;
        }
        addLogInfo("Ssh Socket");
        DNSTT_TunnelThread = new dex001(dex002.this, this::closeAll);
        DNSTT_TunnelThread.start();
    }
    public static boolean isVPNRunning(){
        return isRunning&&hLogStatus.isTunnelActive();
    }

    @Override
    public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, int progress) {
        String stateMsg = getString(hLogStatus.getLocalizedState(hLogStatus.getLastState()));
        mDisplayBytecount = (level.equals(ConnectionStatus.LEVEL_CONNECTED));
        update_notification_event(stateMsg, level);
        if(state.equals("Disconnected")){
            stopForeground(true);
        }
    }

    public interface InjectorListener {
        void startOpenVPN();
    }

    public void setInjectorListener(InjectorListener InjectorListener) {
        this.InjectorListener = InjectorListener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }


    public class MyBinder extends Binder {
        public dex002 getService() {
            return dex002.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);
        mHandler = new Handler(Looper.getMainLooper(), dex002.this);
        mPref = MainApplication.getPrivateSharedPreferences();
        mConfig = ConfigUtil.getInstance(dex002.this);
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSMen:VPNWakeLock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Add null check for intent to prevent NullPointerException
        if (intent == null) {
            Log.e(TAG, "Service started with null intent");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        Bundle bundle = intent.getExtras();
        if (action == null){
            return START_NOT_STICKY;
        }
        mConfig = ConfigUtil.getInstance(dex002.this);
        mServer_type = mConfig.getServerType();
        switch (action) {
            case START_SERVICE:
                mStateReceiver = "";
                hLogStatus.addStateListener(dex002.this);
                hLogStatus.addByteCountListener(dex002.this);
                start_notification();
                isRunning = true;
                mStopping = false;
                mConfig.initializeStartingMsg();

                if (wakeLock != null && !wakeLock.isHeld()) {
                    wakeLock.acquire();
                }

                expiryHandler.removeCallbacks(expiryCheckRunnable);
                expiryHandler.postDelayed(expiryCheckRunnable, 10000); // Start checking after 10s
                
                // PERFORMANCE: Start V2Ray IMMEDIATELY without waiting for Handler queue
                if (SERVER_TYPE_V2RAY.equals(mServer_type)) {
                    startV2RayTunnel();
                } else {
                    startRun();
                }
                break;
            case STOP_SERVICE:
                if (bundle!=null){
                    mStateReceiver = bundle.getString("stateSTOP_SERVICE");
                }
                closeAll();
                break;
            case RECONNECT_SERVICE:
                if (bundle!=null){
                    mStateReceiver = bundle.getString("mStateReceiver");
                    network_reconnect();
                    break;
                }
                if (util.isNetworkAvailable(dex002.this)){
                    if (isVPNRunning()){
                        hLogStatus.clearLog();
                        hLogStatus.updateStateString(hLogStatus.VPN_RECONNECTING, getResources().getString(R.string.state_reconnecting));
                    }
                    if (mServer_type.equals(SERVER_TYPE_OVPN) && isVPNRunning()) {
                        startService(new Intent(dex002.this, dex003.class).setAction(dex003.ACTION_RECONNECT));
                        break;
                    }
                    mStateReceiver = getResources().getString(R.string.state_reconnecting);
                    network_reconnect();
                }
                break;
            case "START_PINGER":
                startPingStatus();
                break;
            case "CONNECTION_TEST_FAILD":
                util.showToast(getResources().getString(R.string.app_name), getResources().getString(R.string.connection_test_fail));
                if (mServer_type.equals(SERVER_TYPE_V2RAY)){
                    if (bundle != null) {
                        mStateReceiver = bundle.getString("stateSTOP_SERVICE");
                    }
                    closeAll();
                }
                break;
            case RESTART_SERVICE:
                startService(new Intent(dex002.this, dex003.class).setAction(dex003.ACTION_DISCONNECT).putExtra(dex003.INTENT_PREFIX + ".STOP", true));
                addLogInfo(getResources().getString(R.string.state_reconnecting));
                mHandler.sendEmptyMessage(PROXY_TUNNEL);
                break;
        }
        return START_NOT_STICKY;
    }


    private void closeAll() {

        try {
            if (isVPNRunning())
                addLogInfo(String.format("<b>%s %s</b>", getResources().getString(R.string.app_name), getResources().getString(R.string.state_stopping)));
            isRunning = false;
            mStopping = true;
            VPNTunnel_handler(false);
            if (mServer_type.equals(SERVER_TYPE_DNS)) {
                closeAll1();
            }
            if (mServer_type.equals(SERVER_TYPE_V2RAY)) {
                SoftReference<ServiceControl> s = V2RayServiceManager.INSTANCE.getServiceControl();
                if (s != null) {
                    s.get().stopService();
                }
            }
            if (mServer_type.equals(SERVER_TYPE_OVPN)) {
                startService(new Intent(dex002.this, dex003.class).setAction(dex003.ACTION_DISCONNECT).putExtra(dex003.INTENT_PREFIX + ".STOP", true));

            }

            new Thread(dex002.this::stopAll).start();
            if (mStateReceiver.equals(getResources().getString(R.string.state_auth_failed))) {
                hLogStatus.updateStateString(hLogStatus.VPN_AUTH_FAILED, getResources().getString(R.string.state_auth_failed));
            } else {
                hLogStatus.updateStateString(hLogStatus.VPN_DISCONNECTED, getResources().getString(R.string.state_disconnected));
            }
            endTunnelService();
        } catch (Exception e) {
            addLogInfo(e.toString());
        }
    }

    private void closeAll1() {
        try {
            if (DNSTT_TunnelThread != null) {
                DNSTT_TunnelThread.interrupt();
                DNSTT_TunnelThread = null;
            }
            DNSTT_TunnelThread = new dex001(this, this::closeAll);
            // Using interrupt() instead of deprecated stop()
            DNSTT_TunnelThread.interrupt();
        } catch (Exception ignored) {

        }
    }



    private void startRun() {
        int tunnel_type = mConfig.getPayloadType();
        hLogStatus.updateStateString(hLogStatus.VPN_CONNECTING, getResources().getString(R.string.state_connecting));
        while (isVPNRunning()){
            if (mServer_type.equals(SERVER_TYPE_SSH) && mConfig.getPayloadType()==PAYLOAD_TYPE_HTTP_PROXY || mServer_type.equals(SERVER_TYPE_SSH) && mConfig.getPayloadType()==PAYLOAD_TYPE_SSL) {
                addLogInfo(getResources().getString(R.string.state_connecting));
                mHandler.sendEmptyMessage(PROXY_TUNNEL);
                break;
            } else if (mServer_type.equals(SERVER_TYPE_DNS)) {
                addLogInfo(getResources().getString(R.string.state_connecting));
                mHandler.sendEmptyMessage(SSH_DNS);

                break;
            }else if ( mServer_type.equals(SERVER_TYPE_SSH)) {
                addLogInfo(getResources().getString(R.string.state_connecting));
                if (tunnel_type == PAYLOAD_TYPE_SSL_PROXY){
                    mHandler.postDelayed(this::starSocketProxy, 300L);
                }else {
                    mHandler.sendEmptyMessage(SSH_DNS);
                }
                break;
            }  else if (mServer_type.equals(SERVER_TYPE_UDP_HYSTERIA_V1)) {
                addLogInfo(getResources().getString(R.string.state_connecting));
                addLogInfo(getResources().getString(R.string.state_get_config));
                addLogInfo(getResources().getString(R.string.state_assign_ip));
                addLogInfo(getResources().getString(R.string.state_wait));
                mHandler.sendEmptyMessage(UDP);
                break;
            } else if (mServer_type.equals(SERVER_TYPE_V2RAY)){
                String nana = MmkvManager.getSelectServer();
                V2RayServiceManager.startVService(this,nana);
                addLogInfo(getResources().getString(R.string.state_connecting));
                addLogInfo(getResources().getString(R.string.state_get_config));
                addLogInfo(getResources().getString(R.string.state_assign_ip));
                addLogInfo(getResources().getString(R.string.state_wait));
                break;
            } else {
                mHandler.sendEmptyMessage(mConfig.mUseProxy()? PROXY_TUNNEL:OVPN);
                break;
            }
        }
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what==OVPN){
            InjectorListener.startOpenVPN();
            return false;
        } else if (msg.what==SSH_DNS){
            String s = mConfig.getServerType();
            if (s.equals(SERVER_TYPE_DNS)){
                startDNSTTTunnel();
            } else {
                startSSHTunnel();
            }

            return false;
        } else if (msg.what==V2RAY){
            startV2RayTunnel();
            return false;
        } else if (msg.what==UDP){
            startUDPTunnel();
            return false;
        } else if (msg.what==PROXY_TUNNEL){
            starSocketProxy();
            return false;
        }
        return true;
    }
    
    private void startV2RayTunnel() {
        try {
            String guid = MmkvManager.getSelectServer();
            if (guid == null || guid.isEmpty()) {
                addLogInfo("No V2Ray server selected");
                hLogStatus.updateStateString(hLogStatus.VPN_DISCONNECTED, getResources().getString(R.string.state_disconnected));
                return;
            }
            
            Log.d(TAG, "Starting V2Ray tunnel with GUID: " + guid);
            V2RayServiceManager.startVService(this, guid);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start V2Ray tunnel", e);
            addLogInfo("Failed to start V2Ray: " + e.getMessage());
            hLogStatus.updateStateString(hLogStatus.VPN_DISCONNECTED, getResources().getString(R.string.state_disconnected));
        }
    }

    private void network_reconnect(){
        if (mStopping || !isVPNRunning()) {
            return;
        }
        VPNTunnel_handler(false);
        if (mServer_type.equals(SERVER_TYPE_V2RAY)) {
            MainActivity.updateMainViews(this, MainActivity.STOP_V2RAY_TUNNEL);
        }
        if (!util.isNetworkAvailable(dex002.this)){
            hLogStatus.updateStateString(hLogStatus.VPN_PAUSE, getResources().getString(R.string.state_pause));
            addLogInfo(getResources().getString(R.string.state_pause));
        }else if (util.isNetworkAvailable(dex002.this) && mConfig.getIsScreenOn()){
            if (mStateReceiver.equals(getResources().getString(R.string.state_pause))){
                hLogStatus.updateStateString(hLogStatus.VPN_PAUSE, getResources().getString(R.string.state_pause));
            }
            if (mStateReceiver.equals(getResources().getString(R.string.state_resume))) {
                hLogStatus.updateStateString(hLogStatus.VPN_RESUME, getResources().getString(R.string.state_resume));
            }
            if (mStateReceiver.equals(getResources().getString(R.string.state_reconnecting))) {
                hLogStatus.updateStateString(hLogStatus.VPN_RECONNECTING, getResources().getString(R.string.state_reconnecting));
            }
            addLogInfo(mStateReceiver.isEmpty()? getResources().getString(R.string.state_reconnecting):mStateReceiver);
            try {
                switch (mServer_type){
                    case SERVER_TYPE_UDP_HYSTERIA_V1:
                        addLogInfo(getResources().getString(R.string.state_get_config));
                        addLogInfo(getResources().getString(R.string.state_assign_ip));
                        addLogInfo(getResources().getString(R.string.state_wait));
                        mHandler.sendEmptyMessage(UDP);
                        break;
                    case SERVER_TYPE_V2RAY:
                        addLogInfo(getResources().getString(R.string.state_get_config));
                        addLogInfo(getResources().getString(R.string.state_assign_ip));
                        addLogInfo(getResources().getString(R.string.state_wait));
                        mHandler.sendEmptyMessage(V2RAY);
                        break;
                    case SERVER_TYPE_DNS:
                    case SERVER_TYPE_SSH:
                        if (mRm002 !=null){
                            mRm002.reconnectSSH();
                        }
                        break;
                }
            } catch (Exception e) {
                closeAll();
            }
        }
    }


    private void stopAll() {
        try {
            if (thPing != null) {
                thPing.interrupt();
                thPing = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (mRm002 != null) {
                mRm002.interrupt();
                mRm002 = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (mRm003 != null) {
                mRm003.interrupt();
                mRm003 = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (mSocketProxyThread != null) {
                mSocketProxyThread.interrupt();
                mSocketProxyThread = null;
            }
        } catch (Exception ignored) {
        }
    }


    private void startPingStatus() {
        if (isVPNRunning()){
            mPref.edit().putBoolean("TIMEOUT_TRIES_KEY",true).apply();
            try {
                startPinger();
            } catch (Exception e) {
                hLogStatus.logInfo("startPinger error:"+e.getMessage());
            }
        }
    }
    private Thread thPing;
    private int TIMEOUT_TRIES = 0;
    private void startPinger() throws Exception {
        TIMEOUT_TRIES = 0;
        int timePing = mConfig.getPingThread();
        if (!isVPNRunning()) {
            throw new Exception();
        }
        hLogStatus.logInfo("starting pinger");
        thPing = new Thread() {
            @Override
            public void run() {
                while (isVPNRunning()) {
                    TIMEOUT_TRIES++;
                    if (mPref.getBoolean("TIMEOUT_TRIES_KEY", false) && TIMEOUT_TRIES==Integer.parseInt(mPref.getString("ping_timeout","10"))){
                        hLogStatus.logInfo("<font color = #FF9600>Ping timeout");
                        thPing.interrupt();
                        thPing = null;
                        break;
                    }
                    try {
                        makePinger();
                    } catch(InterruptedException e) {
                        break;
                    }
                }
                TIMEOUT_TRIES = 0;
                mPref.edit().putBoolean("TIMEOUT_TRIES_KEY",true).apply();
                hLogStatus.logInfo("pinger stopped");
            }
            private synchronized void makePinger() throws InterruptedException {
                try {
                    String newPing;
                    long ping = SpeedtestUtil.getPing(mPref.getString("ping_destination", "www.google.com"), String.valueOf(mConfig.getPingThread()));
                    if (ping >= 400 || ping == 0 || ping == 1 || ping == -1){
                        newPing = "Ping status (<font color = #BA000F>"+ping+"ms</font>)";
                    }else {
                        newPing = "Ping status (<font color = #68B86B>"+ping+"ms</font>)";
                    }
                    mPref.edit().putBoolean("TIMEOUT_TRIES_KEY",false).apply();
                    hLogStatus.logInfo(newPing);
                } catch(Exception e) {
                    Log.e("makePinger", "ping error", e);
                }
                if (timePing == 0)
                    return;
                if (timePing > 0)
                    sleep(timePing* 1000L);
                else {
                    hLogStatus.logInfo("ping invalid");
                    throw new InterruptedException();
                }
            }
        };
        thPing.start();
    }


    private SocketProxyThread mSocketProxyThread;
    private void starSocketProxy() {
        if (mSocketProxyThread != null) {
            mSocketProxyThread.interrupt();
            mSocketProxyThread = null;
        }
        addLogInfo("Ovpn socket");
        mSocketProxyThread = new SocketProxyThread(dex002.this, this::closeAll);
        mSocketProxyThread.start();
    }

    private void startSSHTunnel() {
        if (mRm002 != null) {
            mRm002.interrupt();
            mRm002 = null;
        }
        addLogInfo("Ssh Socket");
        mRm002 = new rm_002(dex002.this, this::closeAll);
        mRm002.start();
    }


    private void startUDPTunnel() {
        if (mRm003 != null) {
            mRm003.interrupt();
            mRm003 = null;
        }
        mRm003 = new rm_003(this, new rm_003.OnUDPListener() {
            @Override
            public void onReconnect() {
                network_reconnect();
            }
            @Override
            public void onStop() {
                closeAll();
            }
        });
        mRm003.start();
    }

    public void VPNTunnel_handler(boolean on) {
        try {
            Intent intent = new Intent(dex002.this, dex004.class);
            if (on) {
                startService(intent.setAction(dex004.START_VPN_SERVICE));
            } else {
                startService(intent.setAction(dex004.STOP_VPN_SERVICE));
            }
        } catch (Exception e) {
            addLogInfo("<font color = #d50000>Something wen't wrong in VPNService.");
        }
    }

    @Override
    @SuppressLint("StringFormatMatches")
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (mDisplayBytecount) {
            String netstat = String.format(getResources().getString(R.string.statusline_bytecount,
                    ConfigUtil.render_bandwidth(in, false),
                    ConfigUtil.render_bandwidth(diffIn, true),
                    ConfigUtil.render_bandwidth(out, false),
                    ConfigUtil.render_bandwidth(diffOut, true)));
           // update_notification_event(netstat, ConnectionStatus.LEVEL_CONNECTED);
        }
    }




    private String getConnection_name(){
        String serverName = mConfig.getServerName();
        if (mServer_type.equals(SERVER_TYPE_V2RAY)){
            return serverName;
        }
        String payloadName = mConfig.getPayloadName();
        if (serverName != null && payloadName != null) {
            if (serverName.equalsIgnoreCase(payloadName)) {
                return serverName;
            }
            if (serverName.toLowerCase().contains(payloadName.toLowerCase())) {
                return serverName;
            }
            if (payloadName.toLowerCase().contains(serverName.toLowerCase())) {
                return payloadName;
            }
        }
        return serverName + " • " + payloadName;
    }


    private void start_notification() {
        if (SERVER_TYPE_V2RAY.equals(mServer_type)) {
            return;
        }
        mNotifyBuilder = new NotificationCompat.Builder(dex002.this, NOTIFICATION_CHANNEL_BG_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_BG_ID, getResources().getString(R.string.channel_name_background), importance);
            notificationChannel.setDescription(getResources().getString(R.string.channel_description_background));
            notificationChannel.enableVibration(true);
            notificationChannel.setLightColor(Color.parseColor("#00BCD4"));
            nm.createNotificationChannel(notificationChannel);
            mNotifyBuilder.setChannelId(NOTIFICATION_CHANNEL_BG_ID);
        }
        mNotifyBuilder.setContentIntent(ConfigUtil.getPendingIntent(this)).
                setSmallIcon(R.drawable.cloud_off).
                setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_icon)).
                setContentTitle(getConnection_name()).
                setContentText("Status: " + getResources().getString(R.string.state_connecting)).
                setOnlyAlertOnce(true).
                setOngoing(true).
                setWhen(new Date().getTime()).
                setPriority(NotificationCompat.PRIORITY_DEFAULT);
        addVpnActionsToNotification(mNotifyBuilder);
        jbNotificationExtras(mNotifyBuilder);
        lpNotificationExtras(mNotifyBuilder);
        Notification notification = mNotifyBuilder.build();
        nm.notify(NOTIFICATION_ID, notification);
        startForeground(NOTIFICATION_ID, notification);
    }


    private void addVpnActionsToNotification(NotificationCompat.Builder nbuilder) {
        int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)? (PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT) : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent reconnectSSHService = PendingIntent.getService(dex002.this, 0, new Intent(dex002.this, dex002.class).setAction(RECONNECT_SERVICE),flags);
        PendingIntent disconnectSSHService = PendingIntent.getService(dex002.this, 0, new Intent(dex002.this, dex002.class).setAction(STOP_SERVICE),flags);
        nbuilder.addAction(R.drawable.cloud_on, "Reconnect", reconnectSSHService);
        nbuilder.addAction(R.drawable.cloud_off, "Disconnect", disconnectSSHService);
    }

    private void update_notification_event(String str, ConnectionStatus status){
        int icon = getIconByConnectionStatus(status);
        if (mNotifyBuilder != null) {
            if(status.equals(ConnectionStatus.LEVEL_CONNECTED)){
                mNotifyBuilder.setTicker(getResources().getString(R.string.state_connected));
            }
            mNotifyBuilder.setSmallIcon(icon);
            mNotifyBuilder.setContentTitle(getConnection_name());
            mNotifyBuilder.setContentText((mDisplayBytecount)?str:"Status: "+str);
            Notification notification = mNotifyBuilder.build();
            nm.notify(NOTIFICATION_ID, notification);
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void lpNotificationExtras(NotificationCompat.Builder nbuilder) {
        nbuilder.setCategory(Notification.CATEGORY_SERVICE);
        nbuilder.setLocalOnly(true);
    }

    private void jbNotificationExtras(NotificationCompat.Builder nbuilder) {
        try {
            if (NotificationManager.IMPORTANCE_LOW != 0) {
                Method setpriority = nbuilder.getClass().getMethod("setPriority", int.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setpriority.invoke(nbuilder, NotificationManager.IMPORTANCE_LOW);
                }
                Method setUsesChronometer = nbuilder.getClass().getMethod("setUsesChronometer", boolean.class);
                setUsesChronometer.invoke(nbuilder, true);
            }
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException ignored) {
        }
    }

    private void endTunnelService(){
        expiryHandler.removeCallbacks(expiryCheckRunnable);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        new Thread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                // For backward compatibility
                stopForeground(true);
            }
            nm.cancel(NOTIFICATION_ID);
            hLogStatus.removeStateListener(dex002.this);
            hLogStatus.removeByteCountListener(dex002.this);
        }).start();
    }

    private int getIconByConnectionStatus(ConnectionStatus level) {
        if (level.equals(ConnectionStatus.LEVEL_CONNECTED)) {
            return R.drawable.cloud_on;
        }
        return R.drawable.cloud_off;
    }


    public void addLogInfo(String msg){
        String hst = mConfig.getSecureString(SERVER_KEY);
        String prx = mConfig.getSecureString(PROXY_IP_KEY);
        if (!msg.contains("Socket close")||!msg.contains(hst)||!msg.contains(prx)){
            hLogStatus.logInfo(msg.trim());
        }
    }



}
