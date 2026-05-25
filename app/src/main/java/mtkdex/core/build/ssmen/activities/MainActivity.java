package mtkdex.core.build.ssmen.activities;

import static mtkdex.core.build.ssmen.utils.util.showToast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.StringRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.v2ray.ang.handler.V2RayServiceManager;
import com.v2ray.ang.handler.MmkvManager;
import com.v2ray.ang.AppConfig;
import com.v2ray.ang.MainApplication;
import com.v2ray.ang.R;
import com.v2ray.ang.ui.SettingsActivity;
import com.v2ray.ang.util.harliesAppManager;
import com.v2ray.ang.viewmodel.MainViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.polaric.md_colorfragment.ColorChooserFragment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mtkdex.core.build.ssmen.adapter.ConfigSpinnerAdapter;
import mtkdex.core.build.ssmen.adapter.LogsAdapter;
import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import mtkdex.core.build.ssmen.logger.ConnectionStatus;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import mtkdex.core.build.ssmen.service.Appnot;
import mtkdex.core.build.ssmen.service.dex002;
import mtkdex.core.build.ssmen.service.dex003;
import mtkdex.core.build.ssmen.utils.PasswordUtil;
import mtkdex.core.build.ssmen.utils.PrefUtil;
import mtkdex.core.build.ssmen.core.vpnutils.TunnelUtils;
import mtkdex.core.build.ssmen.utils.util;
import mtkdex.core.build.ssmen.view.CircleProgressBar;
import mtkdex.core.build.ssmen.view.RotateLoading;
import mtkdex.core.build.ssmen.view.TrafficGraphView;
import mtkdex.core.build.ssmen.view.StatisticGraphData;
import app.tunnel.vpncommons.utils.DataHolder;

public class MainActivity extends MainBaseActivity implements SettingsConstants, hLogStatus.StateListener, hLogStatus.ByteCountListener, ColorChooserFragment.ColorFragmentCallback, NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    public static boolean isNewerVersion(String currentVersion, String latestVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            if (currentPart < latestPart) return true;
            if (currentPart > latestPart) return false;
        }
        return false;
    }

    private void showUpdateDialog(String downloadUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Available");
        builder.setMessage("A newer version of the app is available. Please update to continue.");
        builder.setPositiveButton("Update", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
            startActivity(intent);
        });
        builder.setCancelable(false);
        builder.show();
    }


    public static final String STOP_V2RAY_TUNNEL = "STOP_V2RAY_TUNNEL_KEY";
    public boolean shouldFetchAccountDetails = true;
    public static final int START_BIND_CALLED = 1;
    public static final int REQUEST_IMPORT_FILE = 2;
    private boolean isConnected = false;
    private boolean hasConnectedOnce = false;
    private boolean isDisconnecting = false;
    private boolean showFrozenOnDisconnect = false;
    private long m_SentBytes = 0;
    private long m_ReceivedBytes = 0;
    private final Handler stats_timer_handler = new Handler();
    private DrawerLayout mDrawerLayout;
    private float inValue = 0;
    private float outValue = 0;
    private int i4 = 0;
    private NavigationView drawerNavigationView;
    private CheckBox pingbox;
    private RelativeLayout contentView;
    private int i1 = 0;
    private boolean isCheckUpdateIsRunning = false;
    private boolean _stop = false;
    private boolean showGraphOnDisconnect = false;
    private String date = "Expiry: --/--/---- | 0 Days";
    private LogsAdapter mAdapter;
    private ImageView showLog;
    private TrafficGraphView trafficGraph;
    private BottomSheetBehavior logSheetBehavior, progressSheetBehavior;
    private ImageView mDrawerMenu, mPoint;
    private Animation animation;
    private int mSelectedColor;
    private LinearLayout serverDialog, networkDialog;
    private boolean isMostrarSenha = false;
    private TextView mTunnelType, mLogConnectionStatus, mDataInTv, mDataOutTv, val1, val2;
    private RotateLoading mRotateLoading;
    private Button btn_connector;
    private final ActivityResultLauncher<Intent> vpnServiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            startTunnelService();
        }
    });
    private TextView duration_view, byteIn_view, byteOut_view, status_view, Config_vers, configVers, s_name, p_name, ac_xp, v2ray_ping;
    private Handler mHandler;
    private AlertDialog cBuiler;
    private CircleProgressBar circleProgressBar;
    private PrefUtil prefs;
    private EditText xUser, xPass;
    private Spinner port_spin, prx_spin;
    private final ActivityResultLauncher<Intent> fileImportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        byte[] buffer = new byte[inputStream.available()];
                        inputStream.read(buffer);
                        inputStream.close();
                        String content = new String(buffer);
                        JSONObject jsonObject = new JSONObject(content);
                        if (jsonObject.has("server_name")) {
                            JSONArray serverArray = new JSONArray(getPref().getString(SERVER_TYPE_OVPN, "[]"));
                            serverArray.put(jsonObject);
                            getEditor().putString(SERVER_TYPE_OVPN, serverArray.toString()).apply();
                            loadServers();
                            showToast("Success", "Server imported successfully");
                        } else {
                            showToast("Error", "Invalid configuration file");
                        }
                    }
                } catch (Exception e) {
                    showToast("Error", "Failed to import file: " + e.getMessage());
                }
            }
        }
    });
    private ImageView show_password;
    private MainViewModel mainViewModel;
    private LinearLayout layout_test;
    private TextView graphNetType;
    private TextView graphIpAddr;
    private TextView tv_test_state;
    private TextView liveDataTv;
    private View liveDataDot;
    private View graphLayout;
    private LinearLayout graphLabelsLayout;
    private ExecutorService statsExecutor;
    private boolean liveDataBlink = false;
    private final Runnable stats_timer_task = new Runnable() {
        @Override
        public void run() {
            if (_stop) return;
            
            show_stats();
            
            // Blink the live dot
            if (liveDataDot != null) {
                liveDataDot.setVisibility(liveDataBlink ? View.VISIBLE : View.INVISIBLE);
                liveDataBlink = !liveDataBlink;
            }

            stats_timer_handler.postDelayed(this, 1000);
        }
    };

    private void updateLiveStatusLabels() {
        boolean active = hLogStatus.isTunnelActive();
        // Green if connected, Yellow if connecting (active but not isConnected), Red if disconnected
        int dotColor = isConnected ? Color.parseColor("#00E977") : 
                      (active ? Color.parseColor("#FFD600") : Color.parseColor("#FF1744"));

        if (liveDataTv != null) {
            liveDataTv.setText("LiveData");
        }
        
        if (liveDataDot != null) {
            if (isConnected) {
                liveDataDot.setBackgroundResource(R.drawable.livedata_dot_green);
            } else if (active) {
                // Use the yellow dot if it exists, otherwise keep red but we've updated the logic
                liveDataDot.setBackgroundResource(R.drawable.livedata_dot_red); 
            } else {
                liveDataDot.setBackgroundResource(R.drawable.livedata_dot_red);
            }
            liveDataDot.setAlpha(0.9f);
        }
        
        // Show current speed labels when active
        if (graphLabelsLayout != null) {
            graphLabelsLayout.setVisibility(active ? View.VISIBLE : View.GONE);
        }

        // Only update heavy static labels occasionally or when state changes
        if (graphNetType != null && (i11 % 5 == 0)) {
            graphNetType.setText(util.getNetworkType());
        }
        if (graphIpAddr != null && (i11 % 5 == 0)) {
            graphIpAddr.setText(getLocalIpAddress());
        }
        if (configVers != null && (i11 % 10 == 0)) {
            configVers.setText(String.format("Config: %s", getPref().getString(SettingsConstants.CONFIG_VERSION, "1.0")));
        }
        i11++;
    }

    private int i11 = 0;

    public static void updateMainViews(Context context, String name) {
        // Implementation
    }

    private void cancel_stats() {
        stats_timer_handler.removeCallbacks(stats_timer_task);
    }

    private void schedule_stats() {
        cancel_stats();
        stats_timer_handler.postDelayed(stats_timer_task, 100);
    }

    private void show_stats() {
        if (hLogStatus.isTunnelActive() && isConnected) {
            long totalIn = hLogStatus.getTotalIn();
            long totalOut = hLogStatus.getTotalOut();
            long diffIn = hLogStatus.getDiffIn();
            long diffOut = hLogStatus.getDiffOut();
            
            updateByteCount(totalIn, totalOut, diffIn, diffOut);
        } else {
            // When disconnected, ensure we are not trying to fetch stats
            // Graph will handle its own frozen state
        }
    }

    private void doUpdateLayout() {
        boolean isRunning = hLogStatus.isTunnelActive();
        if (serverDialog != null) serverDialog.setEnabled(!isRunning);
        if (networkDialog != null) networkDialog.setEnabled(!isRunning);
        
        updateLiveStatusLabels();
        
        // Disable server and payload spinners when VPN is connected
        LinearLayout serverSpinner = findViewById(R.id.server_spinner);
        LinearLayout payloadSpinner = findViewById(R.id.payload_spinner);
        if (serverSpinner != null) {
            serverSpinner.setEnabled(!isRunning);
            serverSpinner.setClickable(!isRunning);
            serverSpinner.setAlpha(1.0f); // Removed fade effect
        }
        if (payloadSpinner != null) {
            payloadSpinner.setEnabled(!isRunning);
            payloadSpinner.setClickable(!isRunning);
            payloadSpinner.setAlpha(1.0f); // Fixed alpha
        }

        // Handle button colors to match the green ring/green icon style
        View btnLy = findViewById(R.id.btn_ly);
        ImageView btnIcon = findViewById(R.id.btn_connect_icon);
        if (btnLy != null) {
            // Keep background white/light for the center
            btnLy.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5"))); 
        }
        if (btnIcon != null) {
            // Icon color based on connection state
            btnIcon.setImageTintList(ColorStateList.valueOf(isConnected ? 
                    Color.parseColor("#00ce58") : Color.parseColor("#1c4d8d")));
        }
        
        if (mRotateLoading != null) mRotateLoading.setColor(isConnected ? Color.parseColor("#00ce58") : Color.parseColor("#1c4d8d"));
        if (circleProgressBar != null) circleProgressBar.setColor(isConnected ? Color.parseColor("#00ce58") : Color.parseColor("#1c4d8d"));
        //mTunnelType.setEnabled(!isRunning);
        if (s_name != null) s_name.setTextColor(getConfig() != null && getConfig().getAppThemeUtil() ? Color.WHITE : Color.BLACK);
        if (p_name != null) p_name.setTextColor(getConfig() != null && getConfig().getAppThemeUtil() ? Color.WHITE : Color.BLACK);
        if (xUser != null) {
            xUser.setEnabled(!isRunning);
            xUser.setHintTextColor(getConfig() != null ? getConfig().getHintextColor() : Color.GRAY);
        }
        if (xPass != null) {
            xPass.setEnabled(!isRunning);
            xPass.setHintTextColor(getConfig() != null ? getConfig().getHintextColor() : Color.GRAY);
        }
        TextView graphNetType = findViewById(R.id.graph_net_type);
        if (graphNetType != null) {
            graphNetType.setText(util.getNetworkType());
        }
        TextView graphIpAddr = findViewById(R.id.graph_ip_address);
        if (graphIpAddr != null) {
            graphIpAddr.setText(getLocalIpAddress());
        }

        TextView drawerVersion = findViewById(R.id.drawer_app_version);
        if (drawerVersion != null) {
            drawerVersion.setText("Version " + com.v2ray.ang.BuildConfig.VERSION_NAME);
        }
      //  loadAppColors(getConfig().getColorAccent());
        setupBTNanimation(isRunning);
        shouldFetchAccountDetails = true;
    }

    @Override
    public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, int progress) {
        mHandler.post(() -> {
            boolean wasConnected = isConnected;
            isConnected = level.equals(ConnectionStatus.LEVEL_CONNECTED);
            
            if (isConnected) {
                if (getConfig() != null && getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                    layout_test.setVisibility(View.GONE);
                    teststate1();
                } else {
                    layout_test.setVisibility(View.GONE);
                }
            }
            status_view.setText(state);
            ((TextView) findViewById(R.id.status)).setTextColor(isConnected ? Color.GREEN : Color.RED);
            updateConnectionStatus(level);
            doUpdateLayout();

            if (isConnected) {
                if (getPref().getInt("loadOnce", 0) == 0) {
                    getEditor().putInt("loadOnce", 1).apply();
                }
            } else if (state.equals(resString(R.string.state_reconnecting))) {
                getEditor().putInt("loadOnce", 0).apply();
                if (getPref().getBoolean("isRandom", false)) reLoad_Configs();
            } else if (state.equals(resString(R.string.state_auth_failed))) {
                showToast(resString(R.string.app_name), resString(R.string.state_auth_failed));
            }
            if (hLogStatus.isTunnelActive() && !getPref().getString("_screenPassword_key", "").isEmpty())
                xPass.setText("******");

            // Auto reconnect logic
            if (getPref().getBoolean("auto_reconnect_enabled", false) && 
                wasConnected && !isConnected && 
                !state.equals(hLogStatus.VPN_STOPPING) && 
                !state.equals(hLogStatus.VPN_AUTH_FAILED)) {
                
                new Handler().postDelayed(() -> {
                    if (getPref().getBoolean("auto_reconnect_enabled", false) && 
                        !hLogStatus.isTunnelActive()) {
                        showToast("Auto Reconnect", "Connection lost. Reconnecting...");
                        startTunnelService();
                    }
                }, 3000);
            }
        });

        switch (state) {
            case hLogStatus.VPN_CONNECTED -> mHandler.post(() -> {
                isConnected = true;
                isDisconnecting = false;
                hasConnectedOnce = true;
                showFrozenOnDisconnect = true;
                getEditor().putBoolean("hasConnectedOnce", true).apply();
                
                if (trafficGraph != null) {
                    trafficGraph.setShowPath(true);
                    trafficGraph.setFrozen(false);
                }
                
                // Trigger immediate UI refresh when connected
                updateLiveStatusLabels();
                show_stats();
                
                if (getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                    String success = "V2ray Connected";
                    hLogStatus.logInfo(success);
                    layout_test.setVisibility(View.GONE);
                    teststate1();
                } else {
                    layout_test.setVisibility(View.GONE);
                }

            });

            case hLogStatus.VPN_GET_CONFIG, hLogStatus.VPN_ADD_ROUTES, hLogStatus.VPN_RESOLVE,
                 hLogStatus.VPN_ASSIGN_IP, hLogStatus.VPN_WAITING, hLogStatus.VPN_NO_NETWORK,
                 hLogStatus.VPN_AUTHENTICATING, hLogStatus.VPN_CONNECTING,
                 hLogStatus.VPN_RECONNECTING, hLogStatus.VPN_RESUME -> mHandler.post(() -> {
                // Ensure frozen state is DISABLED during connecting phase
                showFrozenOnDisconnect = false; 
                if (trafficGraph != null) {
                    trafficGraph.setShowPath(true);
                    trafficGraph.setFrozen(false);
                }
                if (getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                    layout_test.setVisibility(View.GONE);
                    tv_test_state.setText(this.getString(R.string.connection_connected));
                    teststate1();
                } else {
                    layout_test.setVisibility(View.GONE);
                }
            });

            case
                    hLogStatus.VPN_DISCONNECTED,
                    hLogStatus.VPN_STOPPING,
                    hLogStatus.VPN_AUTH_FAILED -> mHandler.post(() -> {
                shouldFetchAccountDetails = true;
                isConnected = false;
                isDisconnecting = false;
                
                if (trafficGraph != null) {
                    // Only show frozen path if we actually had a working connection before
                    trafficGraph.setShowPath(showFrozenOnDisconnect);
                    trafficGraph.setFrozen(showFrozenOnDisconnect);
                }
                
                if (getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                    // setShowPath(true) is used to show the frozen straight line on disconnect.
                    // We only do this if it was previously connected in this session.
                    trafficGraph.setShowPath(showFrozenOnDisconnect); 
                    trafficGraph.setFrozen(true);
                    
                    // Persist frozen strings
                    getEditor().putString("frozen_peakIn", trafficGraph.getPeakInStr()).apply();
                    getEditor().putString("frozen_peakOut", trafficGraph.getPeakOutStr()).apply();
                    getEditor().putString("frozen_displayIn", trafficGraph.getDisplayInStr()).apply();
                    getEditor().putString("frozen_displayOut", trafficGraph.getDisplayOutStr()).apply();
                }
            });
        }
    }

    private void updateConnectionStatus(ConnectionStatus level) {
        String baseText = "STATUS: ";
        String statusValue;
        int statusColor;
        int defaultColor = ContextCompat.getColor(this, R.color.colorTextSecondary);

        if (level.equals(ConnectionStatus.LEVEL_CONNECTED)) {
            statusValue = "CONNECTED";
            statusColor = ContextCompat.getColor(this, R.color.connected_color);
            testServerPing();
            isConnected = true; // Set connected state
        } else if (level.equals(ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED) || 
                   level.equals(ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET) ||
                   level.equals(ConnectionStatus.LEVEL_START)) {
            statusValue = "Connecting";
            statusColor = ContextCompat.getColor(this, R.color.colorPrimary);
            // updateServerPing("~ ms"); // REMOVED: Don't reset during connecting to keep frozen value
            isConnected = false;
        } else {
            statusValue = "Disconnected";
            statusColor = defaultColor;
            // updateServerPing("~ ms"); // REMOVED: Don't reset on disconnect to freeze
            isConnected = false;
        }

        if (mLogConnectionStatus != null) {
            String fullText = baseText + statusValue;
            SpannableString spannable = new SpannableString(fullText);
            
            // Set default color for "STATUS: "
            spannable.setSpan(new ForegroundColorSpan(defaultColor), 0, baseText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // Set dynamic color for the status value
            spannable.setSpan(new ForegroundColorSpan(statusColor), baseText.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            mLogConnectionStatus.setText(spannable);
        }
    }

    private void updateServerPing(String pingText) {
        if (v2ray_ping != null) {
            v2ray_ping.setText(pingText);
        }
    }

    private void testServerPing() {
        new Thread(() -> {
            try {
                String pingDest = "1.1.1.1";
                long ping = com.v2ray.ang.util.SpeedtestUtil.getPing(pingDest, "1");
                
                String pingText;
                if (ping <= 0) {
                    pingText = "~ ms";
                } else {
                    pingText = ping + "ms";
                }
                
                runOnUiThread(() -> updateServerPing(pingText));
            } catch (Exception e) {
                Log.e("MainActivity", "Ping test failed", e);
                runOnUiThread(() -> updateServerPing("~ ms"));
            }
        }).start();
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        boolean active = hLogStatus.isTunnelActive();
        
        if (active) {
            // Multiply by 8 to convert bytes to bits for the graph labels
            inValue = (float) diffIn * 8;
            outValue = (float) diffOut * 8;

            final String inStr = ConfigUtil.render_bandwidth(diffIn, true);
            final String outStr = ConfigUtil.render_bandwidth(diffOut, true);
            final String totalInStr = ConfigUtil.render_bandwidth(in, false);
            final String totalOutStr = ConfigUtil.render_bandwidth(out, false);

            runOnUiThread(() -> {
                // Update all values in a single UI frame to ensure synchronization
                updateLiveStatusLabels();
                
                if (byteIn_view != null) byteIn_view.setText(totalInStr);
                if (byteOut_view != null) byteOut_view.setText(totalOutStr);
                if (mDataInTv != null) mDataInTv.setText(inStr);
                if (mDataOutTv != null) mDataOutTv.setText(outStr);
                if (val1 != null) val1.setText(diffIn == 0 ? "0.0 bit" : inStr);
                if (val2 != null) val2.setText(diffOut == 0 ? "0.0 bit" : outStr);
                
                if (duration_view != null && getUpDateBytes().isConnected()) {
                    duration_view.setText(getUpDateBytes().elapsedTimeToDisplay(getUpDateBytes().getElapsedTime()));
                }
            });
        } else {
            // Only reset if NOT active.
            inValue = 0f;
            outValue = 0f;
            
            // To achieve "freeze after disconnect", we simply DO NOTHING here.
            // Values will stay what they were until 'active' becomes true again.
        }

        if (trafficGraph != null) {
            if (active && !isDisconnecting) {
                // Normal active state
                trafficGraph.setShowPath(true);
                trafficGraph.setFrozen(false);
            } else {
                // Disconnected or currently disconnecting
                // showFrozenOnDisconnect is only true if we successfully connected before
                trafficGraph.setShowPath(showFrozenOnDisconnect);
                trafficGraph.setFrozen(true);
            }

            trafficGraph.addValues(inValue, outValue);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        _stop = false;
        hLogStatus.addStateListener(this);
        hLogStatus.addByteCountListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        _stop = true;
        hLogStatus.removeStateListener(this);
        hLogStatus.removeByteCountListener(this);
    }

    @Override
    public void onColorSelection(int selectedColor) {
        this.mSelectedColor = selectedColor;
       // loadAppColors(selectedColor);
    }

    @Override
    public void onFragmentDone(String name) {
        if (name.equals("Apply")) {
            getConfig().setColorAccent(mSelectedColor);
            //loadAppColors(mSelectedColor);
        } else {
           // loadAppColors(getConfig().getColorAccent());
        }
    }

    private void loadAppColors(int selectedColor_) {
        //mDrawerMenu.setBackgroundColor(getConfig().getMainLayoutBG());
        //logRecycle.setBackgroundColor(getConfig().getMainLayoutBG());
        //.setColorFilter(getConfig().gettextColor(), PorterDuff.Mode.SRC_IN);
        //mDrawerMenu.setColorFilter(getConfig().gettextColor(), PorterDuff.Mode.SRC_IN);
        //showLog.setColorFilter(getConfig().getAppThemeUtil() ? Color.WHITE : Color.BLACK, PorterDuff.Mode.SRC_IN);
        mPoint.setColorFilter(getConfig().getColorAccent(), PorterDuff.Mode.SRC_IN);
        show_password.setColorFilter(selectedColor_);
        ((TextView) findViewById(R.id.main_tl1)).setTextSize(17);
        ((TextView) findViewById(R.id.main_tl1)).setTextColor(selectedColor_);
        //((TextView) findViewById(R.id.duration)).setTextSize(27);
        ((ImageView) findViewById(R.id.ac_user_box_icon)).setColorFilter(selectedColor_);
        //((TextView) findViewById(R.id.duration)).setTextColor(selectedColor_);
        if (mDataInTv != null) mDataInTv.setTextColor(selectedColor_);
        if (mDataOutTv != null) mDataOutTv.setTextColor(selectedColor_);
        if (val1 != null) val1.setTextColor(selectedColor_);
        if (val2 != null) val2.setTextColor(selectedColor_);
        findViewById(R.id.network_tag_ly).setBackgroundTintList(ColorStateList.valueOf(selectedColor_));
        findViewById(R.id.show_password).setBackgroundTintList(ColorStateList.valueOf(getConfig().getMainLayoutBG()));
        //findViewById(R.id.btn_ly).setBackgroundTintList(ColorStateList.valueOf(getConfig().getMainLayoutBG()));
        findViewById(R.id.progress_bg).setBackgroundColor(getConfig().getProgLayoutBG());
        //findViewById(R.id.log_view).setBackgroundColor(getConfig().getAppThemeUtil() ? getResources().getColor(R.color.flag_iv_color_dark) : getResources().getColor(R.color.flag_iv_color_light));
        findViewById(R.id.show_password).setBackgroundTintList(ColorStateList.valueOf(selectedColor_));
        try {
            // Header color is now fixed to brand blue in XML as per design
            // drawerNavigationView.getHeaderView(0).findViewById(R.id.card0).setBackgroundColor(selectedColor_);

            // Fix for navigation drawer text color - ensure it's visible in both themes
            int textColor = getConfig().getAppThemeUtil() ? Color.WHITE : getConfig().gettextColor();
            drawerNavigationView.setItemTextColor(ColorStateList.valueOf(textColor));
            // Fix for navigation drawer icon tint - ensure it's visible in both themes
            int iconColor = getConfig().getAppThemeUtil() ? Color.WHITE : selectedColor_;
            drawerNavigationView.setItemIconTintList(ColorStateList.valueOf(iconColor));
            drawerNavigationView.setBackgroundResource(getConfig().getAppThemeUtil() ? R.drawable.nav_bg_dark : R.drawable.nav_bg_light);
        } catch (Exception ignored) {

        }
    }

    private void loadIds() {
        mDrawerMenu = findViewById(R.id.mDrawerMenu);
        loadMainDrawer();
        s_name = findViewById(R.id.sname1);
        p_name = findViewById(R.id.pname1);
        serverDialog = findViewById(R.id.sSpin);
        networkDialog = findViewById(R.id.pSpin);
        mRotateLoading = findViewById(R.id.mRotateLoading);
        btn_connector = findViewById(R.id.btn_connect);
        circleProgressBar = findViewById(R.id.circle_progress);
        duration_view = findViewById(R.id.duration);
        byteIn_view = findViewById(R.id.bytes_in);
        byteOut_view = findViewById(R.id.bytes_out);
        status_view = findViewById(R.id.status);
        Config_vers = findViewById(R.id.config_version);
        cBuiler = new AlertDialog.Builder(this).create();
        statsExecutor = Executors.newSingleThreadExecutor();
        graphLayout = findViewById(R.id.graph_layout);
        xUser = findViewById(R.id.x_username);
        xPass = findViewById(R.id.x_password);
        trafficGraph = findViewById(R.id.trafficGraph);
        final View livedataLayout = findViewById(R.id.livedata_layout);
        if (trafficGraph != null && livedataLayout != null) {
            trafficGraph.setOnAxisOffsetListener(offset -> {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) livedataLayout.getLayoutParams();
                lp.setMarginEnd((int) offset);
                livedataLayout.setLayoutParams(lp);
            });
        }

        RecyclerView recyclerView = findViewById(R.id.lRecyclerView);
        if (recyclerView != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            mAdapter = new LogsAdapter(layoutManager, this);
            recyclerView.setAdapter(mAdapter);
        }

        View logSheet = findViewById(R.id.log_bottom_sheet);
        if (logSheet != null) {
            logSheetBehavior = BottomSheetBehavior.from(logSheet);
        }

        View progressSheet = findViewById(R.id.progress_bottom_sheet);
        if (progressSheet != null) {
            progressSheetBehavior = BottomSheetBehavior.from(progressSheet);
        }

        mDataInTv = findViewById(R.id.mDataInTv);
        mDataOutTv = findViewById(R.id.mDataOutTv);
        val1 = findViewById(R.id.val1);
        val2 = findViewById(R.id.val2);
        graphLabelsLayout = findViewById(R.id.graph_labels_layout);
        graphNetType = findViewById(R.id.graph_net_type);
        graphIpAddr = findViewById(R.id.graph_ip_address);
        configVers = findViewById(R.id.config_version);
        liveDataTv = findViewById(R.id.livedata);
        liveDataDot = findViewById(R.id.livedata_dot);
        ac_xp = findViewById(R.id.ac_xp);
        if (graphIpAddr != null) {
            graphIpAddr.setText(getLocalIpAddress());
        }

        TextView drawerVersion = findViewById(R.id.drawer_app_version);
        if (drawerVersion != null) {
            drawerVersion.setText("Version " + com.v2ray.ang.BuildConfig.VERSION_NAME);
        }

        if (val1 != null) {
            // val1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
        }
        if (val2 != null) {
            // val2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
        }
        if (Config_vers != null) {
            // Config_vers.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
            // Config_vers.setTextColor(ContextCompat.getColor(this, R.color.colorText));
            Config_vers.setText("Config: " + getPref().getString(SettingsConstants.CONFIG_VERSION, "1.0"));
        }
        TextView graphNetType = findViewById(R.id.graph_net_type);
        if (graphNetType != null) {
            // graphNetType.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
            // graphNetType.setTextColor(ContextCompat.getColor(this, R.color.colorText));
        }
        TextView liveDataTv = findViewById(R.id.livedata);
        if (liveDataTv != null) {
            // liveDataTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 7);
        }
        if (mDataInTv != null) {
            // mDataInTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
        }
        if (mDataOutTv != null) {
            // mDataOutTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
        }
        if (status_view != null) {
            // status_view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        }
        //duration_view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        if (byteIn_view != null) {
            // byteIn_view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        }
        if (byteOut_view != null) {
            // byteOut_view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        }

        xUser.setText(getPref().getString("_screenUsername_key", ""));
        xUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String u = xUser.getText().toString().trim();
                getEditor().putString("_screenUsername_key", u).apply();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        Appnot ar = new Appnot(this);
        ar.setListener((isDestroy, message) -> {
            if (isDestroy) {
                new AlertDialog.Builder(this)
                        .setTitle("Notice")
                        .setMessage(message)
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private void hideProgrss() {
        if (progressSheetBehavior != null) {
            progressSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void showProgrss() {
        if (progressSheetBehavior != null) {
            progressSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void teststate1() {
        // Implementation
    }

    private void testConnectivity() {
        // Implementation
    }

    private void setTestState(String state) {
        // Implementation
    }

    private void showHandshakeToast(String message) {
        // Implementation
    }

    private void showMoreOptionsMenu(View v) {
        // Implementation
    }

    private void updateTunnelTypeText() {
        if (mTunnelType != null && getConfig() != null) {
            mTunnelType.setText(getConfig().getServerType());
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set StatusBar color to White with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(Color.WHITE);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.main);
        new util(MainActivity.this);
        mHandler = new Handler();
        prefs = new PrefUtil(MainApplication.getPrivateSharedPreferences());
        new PasswordUtil(MainApplication.getPrivateSharedPreferences());
        animation = AnimationUtils.loadAnimation(this, R.anim.blink);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.reloadServerList();
        doBindService();
        LoadDefaultConfig();
        if (getConfig() != null) {
            findViewById(R.id.main_window_bg).setBackgroundColor(getConfig().getMainLayoutBG());
        }
        loadIds();

        loadV2RaySetups();
        TextView ipText = findViewById(R.id.ipTextView);
        ipText.setText(getLocalIpAddress());
        mTunnelType = findViewById(R.id.tunnel_spin);
        mLogConnectionStatus = findViewById(R.id.log_connection_status);
        v2ray_ping = findViewById(R.id.v2ray_ping);
        
        ImageView moreOptions = findViewById(R.id.imageView2);
        if (moreOptions != null) {
            // moreOptions.setOnClickListener(v -> showMoreOptionsMenu(v));
        }

        updateTunnelTypeText();
        updateServerPing("~ ms");
        serverDialog.setOnClickListener(MainActivity.this);
        networkDialog.setOnClickListener(MainActivity.this);
        btn_connector.setOnClickListener(MainActivity.this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (logSheetBehavior != null && logSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && isDrawerOpen()) {
                    close();
                    logSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else if (logSheetBehavior != null && logSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED && isDrawerOpen()) {
                    close();
                } else if (logSheetBehavior != null && logSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && !isDrawerOpen()) {
                    logSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    if (progressSheetBehavior != null && progressSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        hideProgrss();
                    } else {
                        showExitDialog();
                    }
                }
            }
        });


        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1234);
        }

        try {
            JSONObject p_js = networkArrayDragaPosition()
                    .getJSONObject(getPref().getInt(NETWORK_POSITION, 0));

            if (p_js.has("payload_type")) {
                String payloadType = p_js.optString("payload_type", "");

                int tunnelValue = switch (payloadType) {
                    case "ovpn"  -> 0;
                    case "udp"   -> 1;
                    case "ssh"   -> 2;
                    case "dnstt" -> 3;
                    case "v2ray" -> 4;
                    default      -> -1; // fallback for unknown types
                };

                if (tunnelValue != -1) {
                    getEditor().putInt(manual_tunnel_radio_key, tunnelValue).apply();
                }
            }
        } catch (JSONException ignored) {
            // You might want to log this for debugging instead of silently ignoring
        }


        tv_test_state = findViewById(R.id.tv_test_state);
        layout_test = findViewById(R.id.layout_test);
        layout_test.setOnClickListener(v -> {
            testConnectivity();

        });

        checkAppUpdate();
        
        hasConnectedOnce = getPref().getBoolean("hasConnectedOnce", false);
        if (trafficGraph != null) {
            trafficGraph.clear();
            trafficGraph.setShowPath(false); // Always hidden on launch
        }
        updateByteCount(0, 0, 0, 0);
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "IP not found";
    }

    private void inboxNotification(int id, String title, String message, int smallIcon) {
        // Implementation
    }

    private void autoUpdate() {
        // Implementation
    }

    private void applyConfigUpdate(JSONObject update) {
        // Implementation
    }

    private void appendArray(JSONArray array1, JSONArray array2) {
        // Implementation
    }

    private void mUpdate() {
        // Implementation
    }

    private void mergeArrays(JSONArray array1, JSONArray array2) {
        // Implementation
    }

    private void mImport() {
        // Implementation
    }

    private void mIphunt() {
        // Implementation
    }

    private void showDialog(String title, String message) {
        // Implementation
    }

    private boolean LoadDefaultConfig() {
        // Implementation
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void mFirstNotes() {
        // Implementation
    }

    private void mFirstNotes(String message) {
        // Implementation
    }

    private void showRenewServDialog() {
        // Implementation
    }

    private boolean containsSpecialCharacter(String s) {
        return false;
    }

    private boolean checkConfiguration() {
        if (getConfig() == null) return false;
        if (hLogStatus.isTunnelActive()) {
            if (getConfig().getSecureString(USERNAME_KEY).isEmpty() || getConfig().getSecureString(PASSWORD_KEY).isEmpty()) {
                showToast("Account Invalid", "Invalid username or password, Please enter a valid account.");
                return false;
            }
            return true;
        } else if (getConfig().getSecureString(USERNAME_KEY).isEmpty() || getConfig().getSecureString(PASSWORD_KEY).isEmpty() || containsSpecialCharacter(getPref().getString("_screenUsername_key", "")) || containsSpecialCharacter(getPref().getString("_screenPassword_key", "")) || getPref().getString("_screenUsername_key", "").length() < 4 || getPref().getString("_screenPassword_key", "").length() < 4) {
            showToast("Account Invalid", "Invalid username or password, Please enter a valid account.");
            return false;
        }
        return true;
    }

    private void startOrStopTunnel() {
        getEditor().putInt("loadOnce", 0).apply();
        if (hLogStatus.isTunnelActive()) {
            isDisconnecting = true;
            // Freeze exactly where we are
            if (trafficGraph != null) {
                trafficGraph.setFrozen(true);
            }
            stopTunnelService();
            cancel_stats();
        } else {
            m_SentBytes = 0;
            m_ReceivedBytes = 0;
            if (byteIn_view != null) byteIn_view.setText("0 B");
            if (byteOut_view != null) byteOut_view.setText("0 B");
            if (mDataInTv != null) mDataInTv.setText("0 bit");
            if (mDataOutTv != null) mDataOutTv.setText("0 bit");
            if (val1 != null) val1.setText("0.0 bit");
            if (val2 != null) val2.setText("0.0 bit");
            if (trafficGraph != null) {
                trafficGraph.clear();
                trafficGraph.setShowPath(true);
            }
            updateLiveStatusLabels();
            if (getConfig() != null && getConfig().getAutoClearLog() && mAdapter != null) {
                mAdapter.clearLog();
            }
            if (checkConfiguration()) {
                if (!getPref().getString("Network_info", "").isEmpty() && !getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                    start_connect();
                } else {
                    start_connect();
                }
            }
        }
    }

    public void stopTunnelService() {
        if (getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
            Bundle bundle1 = new Bundle();
            bundle1.putString("V2ray_ms", "");
            DataHolder.setBundle(bundle1);
            stoptV2Ray();
            mainViewModel.removeAllServer();
        }
        
        if (trafficGraph != null) {
            // Only show the frozen straight line if we successfully connected at least once
            trafficGraph.setShowPath(showFrozenOnDisconnect);
            trafficGraph.setFrozen(showFrozenOnDisconnect);
        }

        getEditor().putInt("loadOnce", 0).apply();
        submitDisconnectIntent();
    }

    private void stop_service() {
        hLogStatus.removeStateListener(this);
        hLogStatus.removeByteCountListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isActive = hLogStatus.isTunnelActive();

        if (isActive) {
            schedule_stats();
            if (trafficGraph != null) {
                trafficGraph.setShowPath(true);
            }
        } else {
            // ONLY clear if VPN is not connected
            if (trafficGraph != null) {
                showFrozenOnDisconnect = false;
                trafficGraph.clear();
                trafficGraph.setShowPath(false);
                trafficGraph.setFrozen(false);
            }
            if (mDataInTv != null) mDataInTv.setText("0 bit");
            if (mDataOutTv != null) mDataOutTv.setText("0 bit");
            if (val1 != null) val1.setText("0.0 bit");
            if (val2 != null) val2.setText("0.0 bit");
            updateLiveStatusLabels();
        }
        if (isDrawerOpen()) close();
        autoUpdate();
        
        // Only reload configs if VPN is NOT active to prevent accidental disconnects
        if (!isActive) {
            reLoad_Configs();
            loadServers();
            loadNetwork();
            updateTunnelTypeText();
            
            // Auto connect on app resume if enabled
            if (getPref().getBoolean("auto_connect_enabled", false)) {
                new Handler().postDelayed(() -> {
                    if (!hLogStatus.isTunnelActive()) {
                        startTunnelService();
                    }
                }, 1000); // Wait 1 second after resume
            }

            if (getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                loadV2rayConfig();
                reloadV2RAY();
            }
        }
        
        if (Config_vers != null) {
            String ver = String.format("Config: %s", getPref().getString(CONFIG_VERSION, "1.1"));
            Config_vers.setText(ver);
            if (configVers != null) configVers.setText(ver);
        }
        
        doUpdateLayout();
        V2RAY_TYPE();

        if (ac_xp != null) {
            String rawExp = getPref().getString("_AccountRawXp", "");
            if (!rawExp.isEmpty() && !rawExp.equals("none")) {
                String fDate = util.getExpireDateFormatted(rawExp);
                String dLeft = util.getDaysLeft(rawExp);
                date = "Expiry: " + fDate + " | " + dLeft;
                ac_xp.setText(date);
            } else {
                ac_xp.setText(getPref().getString("_AccountXp", date));
            }
        }
        
        // Refresh account details if needed
        if (shouldFetchAccountDetails) {
            dataAuthetication();
        }

        if (logSheetBehavior != null && logSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            logSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statsExecutor != null) {
            statsExecutor.shutdownNow();
        }
        cancel_stats();
    }

    public void onClick(View v) {
        int viewid = v.getId();
        if (viewid == R.id.btn_connect) {
            startOrStopTunnel();
        }
 else if (viewid == R.id.sSpin || viewid == R.id.server_spinner) {
            startActivity(new Intent(MainActivity.this, ConfigSpinnerAdapter.class).putExtra("mConfigType", "0"));
        } else if (viewid == R.id.pSpin || viewid == R.id.payload_spinner) {
            startActivity(new Intent(MainActivity.this, ConfigSpinnerAdapter.class).putExtra("mConfigType", "1"));
        }
    }

    @Override
    public void startOpenVPN() {
        super.startOpenVPN();
        resolve_epki_alias_then_connect();
    }

    private void start_connect() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            try {
                // Using Activity Result API instead of deprecated startActivityForResult
                vpnServiceLauncher.launch(intent);
                return;
            } catch (ActivityNotFoundException e) {
                return;
            }
        }
        
        String mhuntInfo = getPref().getString("IPHunter_pName", "");
        if (mhuntInfo.contains("Hunter") || mhuntInfo.contains("hunter") || mhuntInfo.contains("HUNTER") || mhuntInfo.contains("HUNT") || mhuntInfo.contains("hunt") || mhuntInfo.contains("Hunt")) {
            mIphunt();
            return;
        }
        startTunnelService();
    }

    private void startTunnelService() {
        m_SentBytes = 0;
        m_ReceivedBytes = 0;
        hLogStatus.resetTrafficHistory();
        if (byteIn_view != null) byteIn_view.setText("0 B");
        if (byteOut_view != null) byteOut_view.setText("0 B");
        if (mDataInTv != null) mDataInTv.setText("0 bit");
        if (mDataOutTv != null) mDataOutTv.setText("0 bit");
        if (val1 != null) val1.setText("0.0 bit");
        if (val2 != null) val2.setText("0.0 bit");

        if (trafficGraph != null) {
            trafficGraph.setShowPath(true);
            trafficGraph.setFrozen(false);
        }
        updateLiveStatusLabels();

        // 1. Give immediate UI feedback
        hLogStatus.updateStateString(hLogStatus.VPN_CONNECTING, getString(R.string.state_connecting));
        
        // 2. Start Authentication in parallel (Instant fire)
        // Only fetch if we don't have it or it's needed
        if (shouldFetchAccountDetails) {
            dataAuthetication();
        }
        
        TunnelUtils.restartRotateAndRandom();
        schedule_stats();
        StatisticGraphData.getStatisticData().getDataTransferStats().startConnected();
        
        // 3. Move heavy config loading and service start to background to avoid UI lag
        if (statsExecutor != null && !statsExecutor.isShutdown()) {
            statsExecutor.execute(() -> {
                if (getConfig() != null && getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                    // Pre-load V2Ray config in background
                    loadV2rayConfig();
                    
                    // Start V2Ray DIRECTLY
                    runOnUiThread(() -> {
                        V2RayServiceManager.startVService(MainActivity.this, null);
                    });
                } else {
                    // Legacy OpenVPN/SSH path
                    runOnUiThread(() -> {
                        Intent intent = new Intent(MainActivity.this, dex002.class).setAction(dex002.START_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent);
                        } else {
                            startService(intent);
                        }
                    });
                }
            });
        }
    }


    private dex003.Profile selected_profile() {
        dex003.ProfileList proflist = profile_list();
        if (proflist != null) {
            return proflist.get_profile_by_name(getConfig().getServerName());
        }
        return null;
    }

    private void resolve_epki_alias_then_connect() {
        resolveExternalPkiAlias(selected_profile(), MainActivity.this::do_connect);
    }

    private void do_connect(String epki_alias) {
        String app_name = "net.openvpn.connect.android";
        prefs.set_string("n_username", getConfig().getSecureString(USERNAME_KEY));
        String username = getConfig().getSecureString(USERNAME_KEY);
        String password = getConfig().getSecureString(PASSWORD_KEY);
        String proxy_name = null;
        String server = null;
        String pk_password = null;
        String response = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void mPaste() {
        // Implementation
    }

    private void loadServers() {
        // Implementation
    }

    private String getServerType(JSONObject server) {
        return "";
    }

    private void loadNetwork() {
        // Implementation
    }

    private void V2RAY_TYPE() {
        // Implementation
    }

    private String getNetworkType(JSONObject network) {
        return "";
    }

    private void getJSONsettings(String settings) {
        // Implementation
    }

    private void setupBTNanimation(boolean isRunning) {
        // Implementation
    }

    private void stopAnimations() {
        btn_connector.clearAnimation();
        if (mRotateLoading.isStart()) mRotateLoading.stop();
        if (animation != null) animation.cancel();
    }

    private void clearAllDataAnim(boolean isRunning) {
        if (isRunning) {
            btn_connector.clearAnimation();
            circleProgressBar.setProgressWithAnimation(0);
            if (mRotateLoading.isStart()) mRotateLoading.stop();
            inValue = 0;
            outValue = 0;
        }
    }

    private void loadMainDrawer() {
        // Implementation
    }

    private boolean isDrawerOpen() {
        return false;
    }

    private void close() {
        // Implementation
    }

    private void open() {
        // Implementation
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    private void openRadioInfo() {
        // Implementation
    }

    private void mTelegram() {
        // Implementation
    }

    private void pingDislog() {
        // Implementation
    }

    private void mReleaseNotes() {
        // Implementation
    }

    private void dataAuthetication() {
        // Implementation
    }

    private void onExpireDate(String date) {
        // Implementation
    }

    private void showDeviceIdNotMatch() {
        // Implementation
    }

    private void onDeviceNotMatch(String deviceId) {
        // Implementation
    }

    private String getDaysLeft(String expiryDate) {
        return "";
    }

    private String getExpireDate(String expiryDate) {
        return "";
    }

    private void onAuthFailed(String message) {
        // Implementation
    }

    private void onError(String message) {
        // Implementation
    }

    private void toggleAutoConnect() {
        // Implementation
    }

    private void toggleAutoReconnect() {
        // Implementation
    }

    private void openBatteryOptimizationSettings() {
        // Implementation
    }

    private void showBatteryOptimizationDialog() {
        // Implementation
    }

    private void showClearCacheDialog() {
        // Implementation
    }

    private void confirmClearAllData() {
        // Implementation
    }

    private long getCacheSize() {
        return 0;
    }

    private long getDataSize() {
        return 0;
    }

    private long getDirSize(File dir) {
        return 0;
    }

    private String formatFileSize(long size) {
        return "";
    }

    private void clearAppCache() {
        // Implementation
    }

    private void clearAppCacheAndData() {
        // Implementation
    }

    private void clearAllAppData() {
        // Implementation
    }

    private int deleteDirContents(File dir) {
        return 0;
    }

    private void checkAppUpdate() {
        // Implementation
    }
}
