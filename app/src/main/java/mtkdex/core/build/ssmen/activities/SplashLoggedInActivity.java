package mtkdex.core.build.ssmen.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.v2ray.ang.MainApplication;
import com.v2ray.ang.R;

import org.json.JSONObject;

import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.utils.SecurePrefUtil;
import mtkdex.core.build.ssmen.utils.appUtil;
import mtkdex.core.build.ssmen.utils.util;

public class SplashLoggedInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_logged_in);

        validateAndProceed();
    }

    private void validateAndProceed() {
        String user = SecurePrefUtil.getEncryptedPrefs(this).getString("_screenUsername_key", "");
        String pass = SecurePrefUtil.getEncryptedPrefs(this).getString("_screenPassword_key", "");

        if (user.isEmpty() || pass.isEmpty()) {
            redirectToLogin("Session expired. Please login again.");
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
                            
                            // Valid account, save raw expiry for MainActivity
                            SecurePrefUtil.getEncryptedPrefs(this).edit()
                                    .putString("_AccountRawXp", expiry)
                                    .apply();
                            
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            }, 2000); // Give user time to see splash
                        } else {
                            if (expiry.equals("Expired") || util.getDaysLeft(expiry).equals("Expired")) {
                                redirectToLogin("Your account has expired.");
                            } else if (!deviceMatch) {
                                redirectToLogin("Account used on another device.");
                            } else {
                                redirectToLogin("Authentication failed.");
                            }
                        }
                    } catch (Exception e) {
                        // On error, proceed anyway to allow offline use if possible
                        proceedToMain();
                    }
                },
                error -> proceedToMain());

        MainApplication.getRequestQueue().add(req);
    }

    private void redirectToLogin(String msg) {
        ConfigUtil.getInstance(this).setHasAccount(false);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("error_msg", msg);
        startActivity(intent);
        finish();
    }

    private void proceedToMain() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 1000);
    }
}
