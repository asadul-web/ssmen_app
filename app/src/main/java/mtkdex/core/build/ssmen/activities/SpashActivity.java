package mtkdex.core.build.ssmen.activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.airbnb.lottie.LottieAnimationView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.config.ConfigUtil;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;


public class SpashActivity extends AppCompatActivity {

    private SharedPreferences myPrefs;
    private SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set StatusBar color to White with dark icons
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(android.graphics.Color.WHITE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        setContentView(R.layout.activity_splash);

        LottieAnimationView lottieSplash = findViewById(R.id.lottieSplash);

        myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = myPrefs.edit();

        ConfigUtil mConfig = ConfigUtil.getInstance(this);
        boolean hasAccount = mConfig.hasAccount();


//        //       transaction off
        lottieSplash.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startActivity(new Intent(SpashActivity.this, LoginActivity.class));
                overridePendingTransition(0, 0); // 🔥 REMOVE TRANSITION
                finish();
            }
        });


                             //       default transaction
//        lottieSplash.addAnimatorListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                startActivity(new Intent(SpashActivity.this, LoginActivity.class));
//                finish();
//            }
//        });
    }
    
    private void startTypingAnimation(final TextView textView, final String text) {
        final Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (index <= text.length()) {
                    textView.setText(text.substring(0, index));
                    index++;
                    handler.postDelayed(this, 100);
                }
            }
        }, 300);
    }


    @Override
    public void onResume() {
        super.onResume();
    }


}
