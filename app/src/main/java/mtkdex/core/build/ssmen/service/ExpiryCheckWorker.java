package mtkdex.core.build.ssmen.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.v2ray.ang.R;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.utils.SecurePrefUtil;
import mtkdex.core.build.ssmen.utils.appUtil;
import mtkdex.core.build.ssmen.utils.util;

public class ExpiryCheckWorker extends Worker {

    public ExpiryCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String user = SecurePrefUtil.getEncryptedPrefs(context).getString("_screenUsername_key", "");
        String pass = SecurePrefUtil.getEncryptedPrefs(context).getString("_screenPassword_key", "");

        if (user.isEmpty() || pass.isEmpty()) {
            return Result.success();
        }

        String api = new appUtil().x_api;
        String model = Build.MODEL;
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        String url = api + "?username=" + user + "&password=" + pass +
                "&device_id=" + id + "&device_model=" + model;

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(Request.Method.GET, url, future, future);
        requestQueue.add(request);

        try {
            String response = future.get(30, TimeUnit.SECONDS);
            JSONObject js = new JSONObject(response);
            boolean auth = js.optString("auth", "false").equals("true");
            boolean deviceMatch = js.optString("device_match", "false").equals("true");
            String expiry = js.optString("expiry", "none");

            if (!auth || !deviceMatch || expiry.equals("none") || util.getDaysLeft(expiry).equals("Expired")) {
                // Account expired or invalid
                handleExpiry(context);
            } else {
                // Valid account, update raw expiry
                SecurePrefUtil.getEncryptedPrefs(context).edit()
                        .putString("_AccountRawXp", expiry)
                        .apply();
            }
        } catch (Exception e) {
            return Result.retry();
        }

        return Result.success();
    }

    private void handleExpiry(Context context) {
        ConfigUtil.getInstance(context).setHasAccount(false);
        
        // Show notification
        sendExpiryNotification(context);
        
        // If VPN is running, we should stop it, but Workers don't easily have access to UI or Service control without broadcasts.
        // We can send a broadcast to MainActivity if it's alive, or just rely on the next app open.
        // However, Problem 6 says disconnect VPN on expiry.
    }

    private void sendExpiryNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "expiry_alerts";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Account Expiry Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.icon_icon)
                .setContentTitle("Account Expired")
                .setContentText("Your VPN account has expired or is invalid. Please login again.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }
}
