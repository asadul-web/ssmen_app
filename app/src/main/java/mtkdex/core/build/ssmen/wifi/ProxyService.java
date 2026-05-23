package mtkdex.core.build.ssmen.wifi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;


import com.v2ray.ang.R;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyService extends Service {
    final String LOG_TAG = "myLogs";
    private ServerThreadTask serverThreadTask;

    public void onCreate() {
        super.onCreate();
        //Log.d(LOG_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        //Log.d(LOG_TAG, "onStartCommand");
        int port = intent.getIntExtra("port", 8080);
        Intent notificationIntent = new Intent(this, MainActivityWifi.class);
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flag);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(getPackageName(),
                    "ProxyService", NotificationManager.IMPORTANCE_NONE);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(notificationChannel);
        }
        startForeground(1,
                new NotificationCompat.Builder(this, getPackageName()).setOngoing(true)
                        .setSmallIcon(R.drawable.icon_icon).setContentTitle("Hotspot")
                        .setContentText("Hotspot service is running").setContentIntent(pendingIntent).build());
        serverThreadTask = new ServerThreadTask(port);
        serverThreadTask.setDaemon(true);
        serverThreadTask.start();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        serverThreadTask.interrupt();
        super.onDestroy();

        Log.d(LOG_TAG, "onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    static class ServerThreadTask extends Thread {
        private final int port;

        public ServerThreadTask(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                ServerSocket httpSocket = new ServerSocket(port);
                Socket clientSocket;
                while (!interrupted()) {
                    clientSocket = httpSocket.accept();
                    System.out.println("Socket accepted");
                    new ClientSocketHandler(clientSocket).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
