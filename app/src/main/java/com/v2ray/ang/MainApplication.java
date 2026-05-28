package com.v2ray.ang;

import static com.v2ray.ang.AppConfig.ANG_PACKAGE;

import android.content.SharedPreferences;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import mtkdex.core.build.ssmen.TopExceptionHandler;
import mtkdex.core.build.ssmen.activities.MainActivity;
import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.utils.util;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.tencent.mmkv.MMKV;
import com.v2ray.ang.BuildConfig;
import com.v2ray.ang.handler.SettingsManager;

public class MainApplication extends MultiDexApplication
{
    private static RequestQueue requestQueue;
    private static SharedPreferences privateSharedPreferences;
    private static SharedPreferences defaultSharedPreferences;
    private static MainApplication MainApp;
    private static final String PREF_LAST_VERSION = "pref_last_version";
    private static final String PREFS_GERAL = "HarlieApplication";
    private final Configuration workManagerConfiguration = new Configuration.Builder()
            .setDefaultProcessName(ANG_PACKAGE + ":bg")
            .build();
    @Override
    public void onCreate() {
        super.onCreate();
        MainApp = MainApplication.this;
        TopExceptionHandler.init(MainApplication.this);
        privateSharedPreferences = getSharedPreferences(PREFS_GERAL, MODE_PRIVATE);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainApplication.this);
        boolean firstRun = defaultSharedPreferences.getInt(PREF_LAST_VERSION, 0) != BuildConfig.VERSION_CODE;
        if (firstRun) defaultSharedPreferences.edit().putInt(PREF_LAST_VERSION, BuildConfig.VERSION_CODE).apply();
        ConfigUtil.getInstance(MainApplication.this);
        ConfigUtil.setNotificationActivityClass(MainActivity.class);

        MMKV.initialize(MainApplication.this);
        SettingsManager.setNightMode();
        // Initialize WorkManager with the custom configuration
        WorkManager.initialize(this, workManagerConfiguration);

        SettingsManager.initRoutingRulesets(this);

        requestQueue = Volley.newRequestQueue(this);

        scheduleExpiryCheck();
    }

    private void scheduleExpiryCheck() {
        androidx.work.PeriodicWorkRequest expiryCheckRequest =
                new androidx.work.PeriodicWorkRequest.Builder(mtkdex.core.build.ssmen.service.ExpiryCheckWorker.class, 30, java.util.concurrent.TimeUnit.MINUTES)
                        .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ExpiryCheckWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                expiryCheckRequest
        );
    }

    public static RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public static MainApplication getApp() {
        return MainApp;
    }

    public static String resString(int res_id) {
        return MainApp.getResources().getString(res_id);
    }

    public static SharedPreferences getDefaultSharedPreferences() {
        return defaultSharedPreferences;
    }
    public static SharedPreferences getPrivateSharedPreferences() {
        return privateSharedPreferences;
    }

        
}
