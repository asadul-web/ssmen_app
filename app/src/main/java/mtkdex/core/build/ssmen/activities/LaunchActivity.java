package mtkdex.core.build.ssmen.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.v2ray.ang.MainApplication;

import org.json.JSONObject;

import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.utils.SecurePrefUtil;
import mtkdex.core.build.ssmen.utils.appUtil;
import mtkdex.core.build.ssmen.utils.util;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        boolean hasAccount = ConfigUtil.getInstance(this).hasAccount();

        if (hasAccount) {
            validateAndNavigate(splashScreen);
        } else {
            // New user → first splash
            startActivity(new Intent(this, SpashActivity.class));
            finish();
        }
    }

    private void validateAndNavigate(SplashScreen splashScreen) {
        // Keep splash screen on until we decide
        splashScreen.setKeepOnScreenCondition(() -> true);

        String user = SecurePrefUtil.getEncryptedPrefs(this).getString("_screenUsername_key", "");
        String pass = SecurePrefUtil.getEncryptedPrefs(this).getString("_screenPassword_key", "");

        if (user.isEmpty() || pass.isEmpty()) {
            ConfigUtil.getInstance(this).setHasAccount(false);
            startActivity(new Intent(this, SpashActivity.class));
            finish();
            return;
        }

        String api = new appUtil().x_api;
        String model = Build.MODEL;
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        String url = api + "?username=" + user + "&password=" + pass +
                "&device_id=" + id + "&device_model=" + model;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject js = new JSONObject(response);
                        boolean auth = js.optString("auth", "false").equals("true");
                        boolean deviceMatch = js.optString("device_match", "false").equals("true");
                        String expiry = js.optString("expiry", "none");

                        if (auth && deviceMatch && !expiry.equals("none") && 
                            !util.getDaysLeft(expiry).equals("Expired")) {
                            
                            // Valid account
                            startActivity(new Intent(this, SplashLoggedInActivity.class));
                        } else {
                            // Expired or invalid
                            ConfigUtil.getInstance(this).setHasAccount(false);
                            Intent intent = new Intent(this, LoginActivity.class);
                            if (expiry.equals("Expired") || util.getDaysLeft(expiry).equals("Expired")) {
                                intent.putExtra("error_msg", "Your account has expired.");
                            } else if (!deviceMatch) {
                                intent.putExtra("error_msg", "Account used on another device.");
                            } else {
                                intent.putExtra("error_msg", "Authentication failed.");
                            }
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        // On parse error, proceed to SplashLoggedInActivity which has its own validation
                        startActivity(new Intent(this, SplashLoggedInActivity.class));
                    }
                    finish();
                },
                error -> {
                    // On network error, proceed as offline
                    startActivity(new Intent(this, SplashLoggedInActivity.class));
                    finish();
                });

        MainApplication.getRequestQueue().add(req);
    }
}
