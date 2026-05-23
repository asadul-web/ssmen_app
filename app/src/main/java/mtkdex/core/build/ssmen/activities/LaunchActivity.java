package mtkdex.core.build.ssmen.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import mtkdex.core.build.ssmen.config.ConfigUtil;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        boolean hasAccount = ConfigUtil.getInstance(this).hasAccount();

        if (hasAccount) {
            // Logged-in user → second splash only
            startActivity(new Intent(this, SplashLoggedInActivity.class));
        } else {
            // New user → first splash
            startActivity(new Intent(this, SpashActivity.class));
        }

        finish();
    }
}