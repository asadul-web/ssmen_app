package mtkdex.core.build.ssmen.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.v2ray.ang.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.utils.appUtil;
import mtkdex.core.build.ssmen.utils.c_01;
import mtkdex.core.build.ssmen.utils.util;

public class LoginActivity extends MainBaseActivity {

    private EditText mUsername, mPassword;
    private ConfigUtil mConfig;
    private TextView mWarningText;
    private BottomSheetBehavior progressSheetBehavior;
    private ImageView mPoint, mTogglePassword;
    private AppCompatButton loginBtn;

    private static final String ERR_AUTH_TITLE = "Wrong Account";
    private static final String ERR_AUTH_MSG = "Authentication failed, check your username and password";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set StatusBar color to White with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(android.graphics.Color.WHITE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        setContentView(R.layout.login_activity);
        new util(this);

        mPassword = findViewById(R.id.login_password);
        mTogglePassword = findViewById(R.id.toggle_password);
        mUsername = findViewById(R.id.login_username); // Move up for typeface access
        
        final boolean[] isPasswordVisible = {false};
        final android.graphics.Typeface originalTypeface = mPassword.getTypeface();

        // Show/Hide icon based on text input
        mPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    mTogglePassword.setVisibility(View.VISIBLE);
                    // Ensure the icon matches current state when appearing
                    if (isPasswordVisible[0]) {
                        mTogglePassword.setImageResource(R.drawable.ic_visibility_off_grey_900_24dp);
                    } else {
                        mTogglePassword.setImageResource(R.drawable.ic_visibility_grey_900_24dp);
                    }
                } else {
                    mTogglePassword.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        mTogglePassword.setOnClickListener(v -> {
            isPasswordVisible[0] = !isPasswordVisible[0];
            if (isPasswordVisible[0]) {
                // Show password
                mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                mTogglePassword.setImageResource(R.drawable.ic_visibility_off_grey_900_24dp);
                mTogglePassword.setAlpha(1.0f);
            } else {
                // Hide password
                mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                mTogglePassword.setImageResource(R.drawable.ic_visibility_grey_900_24dp);
                mTogglePassword.setAlpha(1.0f); // Keep it visible but use the off icon
            }
            mPassword.setSelection(mPassword.getText().length());
            // Restore typeface because setInputType resets it
            mPassword.setTypeface(originalTypeface);
        });

        // Handle back button → minimize app instead of killing
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
            }
        });

        // Init config
        mConfig = ConfigUtil.getInstance(this);

        // Init views
        mWarningText = findViewById(R.id.warning_text);
        loginBtn = findViewById(R.id.login_button);

        // Load saved credentials
        mUsername.setText(getStoredUsername());
        mPassword.setText(getStoredPassword());

        // Bottom sheet setup
        View progbottomSheet = findViewById(R.id.progress_bottom_sheet);
        mPoint = findViewById(R.id.progPoint);
        progressSheetBehavior = BottomSheetBehavior.from(progbottomSheet);
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Read API info from assets
        try {
            String data = c_01.readFromAsset(LoginActivity.this, "mtk.hs");
            JSONObject obj = new JSONObject(data);
            obj.has("account_api"); // check key exists
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        // Handle login click
        loginBtn.setOnClickListener(v -> LoginApi());

        String errorMsg = getIntent().getStringExtra("error_msg");
        if (errorMsg != null) {
            mWarningText.setText(errorMsg);
            mWarningText.setTextColor(ContextCompat.getColor(this, R.color.colorBtnStroke));
        }

        ImageView whatsapp = findViewById(R.id.ivWhatsapp);
        whatsapp.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://whatsapp.com/"));
            startActivity(intent);
        });

        ImageView facebook = findViewById(R.id.ivFacebook);
        facebook.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://facebook.com/"));
            startActivity(intent);
        });
    }


    private boolean containsSpecialCharacters(String input) {
        return input.matches(".*[^a-zA-Z0-9]+.*");
    }

    @SuppressLint("HardwareIds")
    private void doLogin() {
        final String user = mUsername.getText().toString().trim();
        final String pass = Objects.requireNonNull(mPassword.getText()).toString().trim();

        if (containsSpecialCharacters(user) || containsSpecialCharacters(pass)) {
            mWarningText.setTextColor(ContextCompat.getColor(this, R.color.colorBtnStroke));
            return;
        }
        if (user.isEmpty()) {
            mUsername.setError("Username is empty");
            return;
        }
        if (pass.isEmpty()) {
            mPassword.setError("Password is empty");
            return;
        }

        // Mark as logged in
        mConfig.setHasAccount(true);
        navigateToMain();
    }

    private void LoginApi() {

        String api = new appUtil().x_api;
        String user = mUsername.getText().toString().trim();
        String pass = Objects.requireNonNull(mPassword.getText()).toString().trim();

        showProgrss();
        loginBtn.setEnabled(false);

        if (user.isEmpty() || pass.isEmpty()) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                hideProgrss();
                loginBtn.setEnabled(true);
                if (user.isEmpty()) {
                    mUsername.setError("Username is empty");
                    mUsername.requestFocus();
                } else if (pass.isEmpty()) {
                    mPassword.setError("Password is empty");
                    mPassword.requestFocus();
                }
                util.showToast(ERR_AUTH_TITLE, ERR_AUTH_MSG);
            }, 1000);
            return;
        }

        String model = Build.MODEL;
        @SuppressLint("HardwareIds")
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        String jsonUrl = api + "?username=" + user + "&password=" + pass +
                "&device_id=" + id + "&device_model=" + model;

        showProgrss();
        loginBtn.setEnabled(false);

        StringRequest req = new StringRequest(jsonUrl,
                response -> {
                    hideProgrss();
                    loginBtn.setEnabled(true);

                    try {
                        JSONObject js = new JSONObject(response);

                        // Load saved account
                        String savedUser = getStoredUsername();
                        boolean sameAccount = user.equals(savedUser);

                        // ❌ ERROR CASES
                        boolean expiryNone = js.getString("expiry").equals("none");
                        boolean authFalse = js.getString("auth").equals("false");
                        boolean deviceNone = js.getString("device_match").equals("none");
                        boolean deviceFalse = js.getString("device_match").equals("false");

                        if (expiryNone || authFalse || deviceNone || deviceFalse) {

                            // Delete saved account ONLY if the same user fails login
                            if (sameAccount) {
                                secureEditor.remove("_screenUsername_key").apply();
                                secureEditor.remove("_screenPassword_key").apply();
                            }

                            util.showToast(ERR_AUTH_TITLE, ERR_AUTH_MSG);
                            return;
                        }

                        // ✅ SUCCESS CASE
                        if (js.getString("device_match").equals("true")) {

                            // Always save successful account
                            secureEditor.putString("_screenUsername_key", user).apply();
                            secureEditor.putString("_screenPassword_key", pass).apply();

                            // Save user's personal xray UUID for V2Ray connection
                            try {
                                if (js.has("xray_uuid") && !js.getString("xray_uuid").isEmpty()) {
                                    secureEditor.putString("_xray_uuid_key", js.getString("xray_uuid")).apply();
                                }
                            } catch (Exception ignored) {}

                            mWarningText.setTextColor(ContextCompat.getColor(this, R.color.dataIn));
                            onExpireDate(js.getString("expiry"));
                            doLogin();
                            util.showToast(resString(R.string.app_name), "Your authentication was successful.");
                        }

                    } catch (JSONException e) {
                        util.showToast(resString(R.string.app_name), "Invalid server response");
                    }
                },
                error -> {
                    hideProgrss();
                    loginBtn.setEnabled(true);

                    if (error.getMessage() == null) {
                        util.showToast(resString(R.string.app_name), "Please Check Your Internet Connection!");
                    } else {
                        util.showToast(ERR_AUTH_TITLE, ERR_AUTH_MSG);
                    }
                });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(req);
    }





    private String getDaysLeft(String thatDate) {
        return util.getDaysLeft(thatDate);
    }

    @SuppressLint("SetTextI18n")
    public void onExpireDate(String expiry) {
        if (expiry == null || expiry.equals("none")) {
            getEditor().putString("_AccountXp", "Expiry: none").apply();
            getEditor().putString("_AccountRawXp", "none").apply();
            return;
        }

        String formattedDate = util.getExpireDateFormatted(expiry);
        String daysLeft = util.getDaysLeft(expiry);

        String dateStr = "Expiry: " + formattedDate + " | " + daysLeft;
        getEditor().putString("_AccountXp", dateStr).apply();
        getEditor().putString("_AccountRawXp", expiry).apply();
        mWarningText.setText("Account Validity: " + daysLeft);
    }

    private void navigateToMain() {
        AtomicInteger waited = new AtomicInteger(-1);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> runOnUiThread(() -> {
            waited.getAndIncrement();
            if (waited.get() == 2) {
                scheduler.shutdown();
                waited.set(0);
                Intent home_intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(home_intent);
                finish();
            }
        }), 0, 1, TimeUnit.SECONDS);
    }

    private void hideProgrss() {
        if (mPoint != null) mPoint.clearAnimation();
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void showProgrss() {
        ((TextView) findViewById(R.id.progTv)).setText("Checking Please Wait...");
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        RotateAnimation ra = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_PARENT, 0.37f,
                Animation.RELATIVE_TO_PARENT, 0.37f);
        ra.setDuration(2000);
        ra.setRepeatCount(Animation.INFINITE);
        ra.setRepeatMode(Animation.RESTART);
        mPoint.startAnimation(ra);
    }

    private final android.os.Handler expiryHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable expiryRunnable = new Runnable() {
        @Override
        public void run() {
            String rawExpiry = getPref().getString("_AccountRawXp", "");
            if (!rawExpiry.isEmpty() && !rawExpiry.equals("none")) {
                String daysLeft = util.getDaysLeft(rawExpiry);
                mWarningText.setText("Account Validity: " + daysLeft);
                if (daysLeft.equals("Expired")) {
                    mWarningText.setTextColor(android.graphics.Color.RED);
                }
            }
            expiryHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        expiryHandler.post(expiryRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        expiryHandler.removeCallbacks(expiryRunnable);
    }

}
