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
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.tunnel.vpncommons.utils.DataHolder;
import mtkdex.core.build.ssmen.adapter.ConfigSpinnerAdapter;
import mtkdex.core.build.ssmen.adapter.LogsAdapter;
import mtkdex.core.build.ssmen.adapter.portAdapter;
import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import mtkdex.core.build.ssmen.core.vpnutils.TunnelUtils;
import mtkdex.core.build.ssmen.logger.ConnectionStatus;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import mtkdex.core.build.ssmen.service.Appnot;
import mtkdex.core.build.ssmen.service.dex002;
import mtkdex.core.build.ssmen.service.dex003;
import mtkdex.core.build.ssmen.service.dex003.ConnectionStats;
import mtkdex.core.build.ssmen.thread.checkUpdate;
import mtkdex.core.build.ssmen.utils.ExpiryUpdate;
import mtkdex.core.build.ssmen.utils.PasswordUtil;
import mtkdex.core.build.ssmen.utils.PrefUtil;
import mtkdex.core.build.ssmen.utils.appUtil;
import mtkdex.core.build.ssmen.utils.c_01;
import mtkdex.core.build.ssmen.utils.dnsUtil.dnsActivity;
import mtkdex.core.build.ssmen.utils.util;
import mtkdex.core.build.ssmen.view.CircleProgressBar;

import mtkdex.core.build.ssmen.view.TrafficGraphView;
import mtkdex.core.build.ssmen.view.RotateLoading;
import mtkdex.core.build.ssmen.view.StatisticGraphData;
import mtkdex.core.build.ssmen.wifi.MainActivityWifi;



import java.io.BufferedReader;
import java.io.InputStreamReader;


public class MainActivity extends MainBaseActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ExpiryUpdate.ExpiryTknetwork.ExpireDateListener,
        ColorChooserFragment.ColorFragmentCallback,
        hLogStatus.StateListener,
        SettingsConstants,
        hLogStatus.ByteCountListener,
        OnClickListener {

    //-----------------------Auto app update notification
    private boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");

            int length = Math.max(latestParts.length, currentParts.length);

            for (int i = 0; i < length; i++) {
                int latestVal = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentVal = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

                if (latestVal > currentVal) return true;
                if (latestVal < currentVal) return false;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void showUpdateDialog(String version) {

        View view = getLayoutInflater().inflate(R.layout.dialog_update, null);

        TextView message = view.findViewById(R.id.updateMessage);
        Button btnUpdate = view.findViewById(R.id.btnUpdate);
        Button btnLater = view.findViewById(R.id.btnLater);

        message.setText("New version " + version + " is available.\n\nUpgrade for better performance and security.");

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(view)
                        .setCancelable(false)
                        .create();

        btnUpdate.setOnClickListener(v -> {

            String updatePageUrl = "https://app.asalo.site/";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(updatePageUrl));
            startActivity(intent);

            dialog.dismiss();
        });

        btnLater.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }



    public static final String STOP_V2RAY_TUNNEL = "STOP_V2RAY_TUNNEL_KEY";
    private boolean shouldFetchAccountDetails = true;
    private static final int START_BIND_CALLED = 1;
    private static final int REQUEST_IMPORT_FILE = 2;
    private static boolean isConnected = false;
    private boolean hasConnectedOnce = false;
    private boolean isDisconnecting = false;
    private boolean showFrozenOnDisconnect = false;
    private static long m_SentBytes = 0;
    private static long m_ReceivedBytes = 0;
    private final Handler stats_timer_handler = new Handler();
    public DrawerLayout mDrawerLayout;
    float inValue = 0;
    float outValue = 0;
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
    TrafficGraphView trafficGraph;
    private BottomSheetBehavior logSheetBehavior, progressSheetBehavior;
    private ImageView mDrawerMenu;
    private ImageView mPoint;
    private Animation animation;
    private int mSelectedColor;
    private LinearLayout serverDialog, networkDialog;
    private boolean isMostrarSenha = false;
    private TextView mTunnelType, mLogConnectionStatus, mDataInTv, mDataOutTv, val1, val2;    // Activity Result API launchers
    private RotateLoading mRotateLoading;
    private Button btn_connector;
    private final ActivityResultLauncher<Intent> vpnServiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            start_connect();
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
            String mData = c_01.decrypt(c_01.readTextUri(MainActivity.this, uri));
            try {
                JSONArray sjarr = new JSONArray();
                JSONArray pjarr = new JSONArray();
                JSONObject obj = new JSONObject(mData);
                if (getConfig().getVersionCompare(obj.getString("Version"), getPref().getString(CONFIG_VERSION, "0"))) {
                    if (addOrEditedServers().length() != 0)
                        for (int i = 0; i < addOrEditedServers().length(); i++) {
                            sjarr.put(addOrEditedServers().getJSONObject(i));
                        }
                    if (obj.getJSONArray("Servers").length() != 0)
                        for (int i = 0; i < obj.getJSONArray("Servers").length(); i++) {
                            sjarr.put(obj.getJSONArray("Servers").getJSONObject(i));
                        }
                    if (addOrEditedNetwork().length() != 0)
                        for (int i = 0; i < addOrEditedNetwork().length(); i++) {
                            pjarr.put(addOrEditedNetwork().getJSONObject(i));
                        }
                    if (obj.getJSONArray("HTTPNetworks").length() != 0)
                        for (int i = 0; i < obj.getJSONArray("HTTPNetworks").length(); i++) {
                            pjarr.put(obj.getJSONArray("HTTPNetworks").getJSONObject(i));
                        }

                    getServerData().updateData("1", sjarr.toString());
                    getNetworkData().updateData("1", pjarr.toString());
                    loadServerArrayDragaPosition();
                    loadNetworkArrayDragaPosition();
                    getEditor().putInt(SERVER_POSITION, getPref().getInt(SERVER_POSITION, 0)).apply();
                    getEditor().putInt(NETWORK_POSITION, getPref().getInt(NETWORK_POSITION, 0)).apply();
                    getEditor().putInt(SERVER_POSITION, getPref().getInt(SERVER_POSITION, 0)).apply();
                    getEditor().putInt(NETWORK_POSITION, getPref().getInt(NETWORK_POSITION, 0)).apply();
                    getEditor().putString(CONFIG_VERSION, obj.getString("Version")).apply();
                    getEditor().putString(RELEASE_NOTE, obj.getString("ReleaseNotes")).apply();
                    getEditor().putString(CONTACT_SUPPORT, obj.getString("contactSupport")).apply();
                    getEditor().putString(OPEN_VPN_CERT, obj.getString("Ovpn_Cert")).apply();
                    getEditor().putString(CONFIG_URL, c_01.decrypt(obj.getString("config_url"))).apply();
                    getEditor().putString(CONFIG_API, obj.has("account_api") ? c_01.decrypt(obj.getString("account_api")) : "").apply();
                    getEditor().putString(UPLOAD_GET_API, obj.has("upload_get_api") ? c_01.decrypt(obj.getString("upload_get_api")) : "").apply();
                    getEditor().putString(UPLOAD_POST_API, obj.has("upload_post_api") ? c_01.decrypt(obj.getString("upload_post_api")) : "").apply();
                    getEditor().putString(CONFIG_EDITOR_CODE, obj.has("AppConfPass") ? c_01.decrypt(obj.getString("AppConfPass")) : "").apply();
                    if (obj.has("JSONsettings"))
                        getJSONsettings(obj.getJSONArray("JSONsettings").toString());
                    getEditor().putBoolean("isRandom", false).apply();
                    getEditor().putBoolean("isAdminAccept", false).apply();
                    if (Config_vers != null) {
                        String ver = String.format("Config: %s", getPref().getString(CONFIG_VERSION, "1.1"));
                        Config_vers.setText(ver);
                        if (configVers != null) configVers.setText(ver);
                    }
                    showDialog("Release Note", obj.getString("ReleaseNotes"));
                    reLoad_Configs();
                    loadServers();
                    loadNetwork();
                }
            } catch (JSONException e) {
                showToast(resString(R.string.app_name), "Invalid Config File");
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
        public void run() {
            // This loop triggers stat fetching for services
            MainActivity.this.show_stats();
            
            // Check for real-time account expiry and update UI
            String rawExpiry = getPref().getString("_AccountRawXp", "");
            if (!rawExpiry.isEmpty() && !rawExpiry.equals("none")) {
                String daysLeft = util.getDaysLeft(rawExpiry);
                if (daysLeft.equals("Expired")) {
                    if (hLogStatus.isTunnelActive()) {
                        addlogInfo("<font color = #d50000>Account expired! Disconnecting...");
                        stopTunnelService();
                        showToast("Expired", "Your account has just expired.");
                    }
                    if (ac_xp != null) {
                        String fDate = util.getExpireDateFormatted(rawExpiry);
                        ac_xp.setText("Expiry: " + fDate + " | Expired");
                    }
                } else {
                    // Update label in real-time for short trial users
                    if (ac_xp != null && i11 % 5 == 0) { // Update every 5 seconds to save battery
                        String fDate = util.getExpireDateFormatted(rawExpiry);
                        ac_xp.setText("Expiry: " + fDate + " | " + daysLeft);
                    }
                }
            }

            MainActivity.this.schedule_stats();
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

    public static void updateMainViews(Context context, String str) {
        Intent mIntent = new Intent(str);
        LocalBroadcastManager.getInstance(context).sendBroadcast(mIntent);
    }

    private void cancel_stats() {
        this.stats_timer_handler.removeCallbacks(this.stats_timer_task);
    }

    private void schedule_stats() {
        cancel_stats();
        this.stats_timer_handler.postDelayed(this.stats_timer_task, 1000);
    }

    public void show_stats() {
        try {
            if (hLogStatus.isTunnelActive()) {
                String serverType = getConfig().getServerType();
                if (serverType.equals(SERVER_TYPE_OVPN)) {
                    ConnectionStats stats = get_connection_stats();
                    if (stats != null) {
                        hLogStatus.updateByteCount(stats.bytes_in, stats.bytes_out);
                    }
                } else if (serverType.equals(SERVER_TYPE_V2RAY)) {
                    // Handled via broadcasts from NotificationManager (MainViewModel)
                } else {
                    // For SSH/UDP types that might update StatisticGraphData
                    hLogStatus.updateByteCount(getUpDateBytes().getTotalBytesReceived(), getUpDateBytes().getTotalBytesSent());
                }
            }
        } catch (Exception ignored) {
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
        if (s_name != null) s_name.setTextColor(getConfig().getAppThemeUtil() ? Color.WHITE : Color.BLACK);
        if (p_name != null) p_name.setTextColor(getConfig().getAppThemeUtil() ? Color.WHITE : Color.BLACK);
        if (xUser != null) {
            xUser.setEnabled(!isRunning);
            xUser.setHintTextColor(getConfig().getHintextColor());
        }
        if (xPass != null) {
            xPass.setEnabled(!isRunning);
            xPass.setHintTextColor(getConfig().getHintextColor());
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
                if (getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                    layout_test.setVisibility(View.GONE);
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
                !isDisconnecting &&
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
            if (!getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
                testServerPing();
            }
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
            if (pingText != null && !pingText.equals("~ ms") && pingText.contains("ms")) {
                try {
                    String cleanPing = pingText.replace("ms", "").trim();
                    long pingValue = Long.parseLong(cleanPing);

                    int colorRes;
                    if (pingValue < 1500) {
                        colorRes = R.color.ping_green;
                    } else if (pingValue < 2500) {
                        colorRes = R.color.ping_yellow;
                    } else {
                        colorRes = R.color.ping_red;
                    }
                    v2ray_ping.setTextColor(ContextCompat.getColor(this, colorRes));
                } catch (NumberFormatException e) {
                    v2ray_ping.setTextColor(ContextCompat.getColor(this, R.color.connected_color));
                }
            } else {
                v2ray_ping.setTextColor(ContextCompat.getColor(this, R.color.colorText));
            }
        }
    }

    private void testServerPing() {
        new Thread(() -> {
            try {
                String pingDest = getPref().getString("ping_destination", "1.1.1.1");
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
                if (val1 != null) val1.setText(inStr);
                if (val2 != null) val2.setText(outStr);
                
                // Keep timer updating with every traffic tick
                if (duration_view != null && getUpDateBytes().isConnected()) {
                    duration_view.setText(getUpDateBytes().elapsedTimeToDisplay(getUpDateBytes().getElapsedTime()));
                }
            });
        } else {
            // Only reset if NOT active.
            inValue = 0f;
            outValue = 0f;
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
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, which) -> {
                            Intent intent = getBaseContext()
                                    .getPackageManager()
                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                            finishAffinity();
                            Runtime.getRuntime().exit(0);
                        })
                        .show();
            }
        });
        ar.start(); // hypothetically replaces start()

        xPass.setText(getPref().getString("_screenPassword_key", ""));
        xPass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String u = xPass.getText().toString().trim();
                if (!u.equals("******")) getEditor().putString("_screenPassword_key", u).apply();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        show_password = findViewById(R.id.show_password);

        show_password.setOnClickListener(v -> {
            isMostrarSenha = !isMostrarSenha;
            String u = getPref().getString("_screenPassword_key", "");
            if (isMostrarSenha) {
                xPass.setText(u);
            } else {
                xPass.setText(u.isEmpty() ? "" : "******");
            }
        });
        RecyclerView logRecycle = findViewById(R.id.lRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mAdapter = new LogsAdapter(layoutManager, this);
        logRecycle.setAdapter(mAdapter);
        logRecycle.setLayoutManager(layoutManager);
        View logbottomSheet = findViewById(R.id.log_bottom_sheet);
        View progbottomSheet = findViewById(R.id.progress_bottom_sheet);
        mPoint = findViewById(R.id.progPoint);
        logSheetBehavior = BottomSheetBehavior.from(logbottomSheet);
        progressSheetBehavior = BottomSheetBehavior.from(progbottomSheet);
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        showLog = findViewById(R.id.show_log_view);
        ImageView status_log_menu = findViewById(R.id.status_log_menu);
        status_log_menu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            popup.getMenu().add(1, 1, 1, "Clear logs");
            popup.getMenu().add(2, 2, 2, "Copy logs");
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        mAdapter.clearLog();
                        break;
                    case 2:
                        if (c_01.copyToClipboard(MainActivity.this, hLogStatus.CopyLogs())) {
                            showToast(resString(R.string.app_name), "Logs copy to clipboard");
                        }
                        break;
                }
                return true;
            });
            popup.show();
        });
        logSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    showLog.animate().rotation(180f).setDuration(200).start();
                    mAdapter.scrollToLastPosition(); // Show latest logs
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    showLog.animate().rotation(0f).setDuration(200).start();
                } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    // Update behavior if content changed while hidden/collapsed
                    logSheetBehavior.setFitToContents(true);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Rotate arrow dynamically while dragging (0.0 to 1.0 -> 0° to 180°)
                if (!Float.isNaN(slideOffset) && slideOffset >= 0) {
                    showLog.setRotation(slideOffset * 180f);
                }
            }
        });

        findViewById(R.id.log_view).setOnClickListener(v -> {
            if (logSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                logSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                logSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        port_spin = findViewById(R.id.portSpin);
        prx_spin = findViewById(R.id.prxSpin);
        port_spin.setAdapter(new portAdapter(MainActivity.this, sPort));
        prx_spin.setAdapter(new portAdapter(MainActivity.this, pPort));
        port_spin.setSelection(getPref().getInt(CUSTOM_SERVER_POR_KEY, 0));
        prx_spin.setSelection(getPref().getInt(CUSTOM_NETWORK_PORT_KEY, 0));
        port_spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getEditor().putInt(CUSTOM_SERVER_POR_KEY, position).apply();
                V2RAY_TYPE();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        prx_spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getEditor().putInt(CUSTOM_NETWORK_PORT_KEY, position).apply();
                V2RAY_TYPE();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (mDrawerMenu != null) mDrawerMenu.setOnClickListener(v -> {
            hideProgrss();
            open();
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
                int item = menuItem.getItemId();
                if (item == R.id.a_update) {
                    mUpdate();
                } else if (item == R.id.a_tele) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                } else if (item == R.id.a_exit) {
                    showExitDialog();
                }
                return true;
            });
        }
    }

    private void showExitDialog() {
        if (cBuiler != null && cBuiler.isShowing()) {
            cBuiler.dismiss();
        }

        View inflate = LayoutInflater.from(MainActivity.this).inflate(R.layout.notif2, null);
        cBuiler = new AlertDialog.Builder(MainActivity.this).create();

        RelativeLayout btn = inflate.findViewById(R.id.appButton2);
        TextView title = inflate.findViewById(R.id.notiftext1);
        TextView ms = inflate.findViewById(R.id.confimsg);
        TextView ok = inflate.findViewById(R.id.appButton2txt);
        TextView cancel = inflate.findViewById(R.id.appButton1);

        ms.setTextColor(getConfig().gettextColor());
        cancel.setTextColor(getConfig().getColorAccent());
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        btn.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        title.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        ok.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);

        title.setText("Confirmation");
        ms.setText("Do you want exit?");
        cancel.setText("Minimize");
        ok.setText("Exit");

        // Exit button
        btn.setOnClickListener(p1 -> {
            try {
                Log.d("ExitDebug", "Exit button clicked in onBackPressed dialog - starting exit sequence");

                // Explicitly disable auto-reconnect before stopping, to prevent a loop during exit
                getEditor().putBoolean("auto_reconnect_enabled", false).apply();

                if (dex002.isVPNRunning()) {
                    stopTunnelService();
                }
                stopService(new Intent(MainActivity.this, dex002.class));

                Intent forceStopIntent = new Intent(MainActivity.this, dex003.class);
                forceStopIntent.setAction(dex003.ACTION_FORCE_STOP);
                startService(forceStopIntent);
                stopService(new Intent(MainActivity.this, dex003.class));

                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancelAll();

                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                }

                if (Build.VERSION.SDK_INT >= 21) {
                    finishAndRemoveTask();
                } else {
                    finishAffinity();
                }

                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    for (ActivityManager.AppTask task : am.getAppTasks()) {
                        task.finishAndRemoveTask();
                    }
                }

                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cBuiler.dismiss();
            }
        });

        // Minimize button
        inflate.findViewById(R.id.appButton0).setOnClickListener(p1 -> {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            cBuiler.dismiss();
        });

        cBuiler.setView(inflate);
        cBuiler.setCancelable(true);
        cBuiler.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        cBuiler.show();
    }

    private void hideProgrss() {
        if (mPoint != null) mPoint.clearAnimation();
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void showProgrss() {
        ((TextView) findViewById(R.id.progTv)).setText("Checking Please Wait...");
        progressSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        RotateAnimation ra = new RotateAnimation(0, 360, Animation.RELATIVE_TO_PARENT, 0.37f, Animation.RELATIVE_TO_PARENT, 0.37f);
        ra.setDuration(2000);
        ra.setRepeatCount(Animation.INFINITE);
        ra.setRepeatMode(Animation.RESTART);
        mPoint.startAnimation(ra);
    }

    public void teststate1() {
        mainViewModel.getUpdateTestResultAction().observe(this, this::setTestState);
        mainViewModel.startListenBroadcast();
    }

    public void testConnectivity() {
        mainViewModel.getUpdateTestResultAction().getValue();
        setTestState(this.getString(R.string.connection_test_testing));
        String state = hLogStatus.getLastState();
        if (hLogStatus.VPN_CONNECTED.equals(state)) {
            setTestState(this.getString(R.string.connection_test_testing));
            mainViewModel.testCurrentServerRealPing(); // Call to ViewModel method

        }

    }

    public void setTestState(String content) {
        if (content == null) return;

        tv_test_state.setText(content);

        // Extract and display ping value
        if (content.contains("Success")) {
            try {
                // Extract ping value from content like "✔ Success: 123ms"
                String pingValue = content.replaceAll("[^0-9]", "");
                if (!pingValue.isEmpty()) {
                    updateServerPing(pingValue + " ms");

                    // Show custom toast/snackbar
                    showHandshakeToast(pingValue);
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Failed to extract ping value", e);
            }
        }
    }

    private void showHandshakeToast(String ms) {
        if (isFinishing()) return;
        runOnUiThread(() -> {
            try {
                // Cancel existing toast if showing to prevent stacking
                if (mCurrentToast != null) {
                    mCurrentToast.cancel();
                }

                View layout = getLayoutInflater().inflate(R.layout.snackbar, null);
                TextView title = layout.findViewById(R.id.itemtoastTv1);
                TextView subtitle = layout.findViewById(R.id.itemtoastTv2);

                if (title != null) title.setText("Handshake");
                if (subtitle != null) subtitle.setText("Success: HTTPS handshake took " + ms + " ms");

                mCurrentToast = new Toast(getApplicationContext());
                mCurrentToast.setDuration(Toast.LENGTH_LONG);
                mCurrentToast.setGravity(Gravity.BOTTOM, 0, 150);
                mCurrentToast.setView(layout);
                mCurrentToast.show();
            } catch (Exception e) {
                util.showToast("Handshake", "Success: HTTPS handshake took " + ms + " ms");
            }
        });
    }

    private Toast mCurrentToast;

    private void showMoreOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("App Link");
        popup.getMenu().add("Clear app");
        popup.getMenu().add("Exit All");
        popup.getMenu().add("Logout");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            switch (title) {
                case "App Link":
                    // Open play store or website
                    try {
                        Intent webIntent = new Intent(this, mtk0005.class);
                        webIntent.putExtra("mConfigPanelRenew", "https://app.asalo.site");
                        startActivity(webIntent);
                    } catch (Exception e) {
                        showToast("Error", "Could not open browser: " + e.getMessage());
                    }
                    return true;
                case "Clear app":
                    // Use custom layout dialog_clear_data.xml
                    if (cBuiler != null && cBuiler.isShowing()) cBuiler.dismiss();
                    View inflateClear = LayoutInflater.from(this).inflate(R.layout.dialog_clear_data, null);
                    cBuiler = new AlertDialog.Builder(this).create();

                    View btnClear = inflateClear.findViewById(R.id.btn_clear);
                    View btnCancel = inflateClear.findViewById(R.id.btn_cancel);

                    btnClear.setOnClickListener(v1 -> {
                        try {
                            // 1. Stop VPN services
                            submitDisconnectIntent();
                            stoptV2Ray();

                            // 2. Stop Other known services explicitly
                            try {
                                stopService(new Intent(this, mtkdex.core.build.ssmen.service.dex002.class));
                                stopService(new Intent(this, mtkdex.core.build.ssmen.service.dex003.class));
                                stopService(new Intent(this, mtkdex.core.build.ssmen.service.dex004.class));
                                stopService(new Intent(this, com.v2ray.ang.service.V2RayVpnService.class));
                            } catch (Exception ignored) {}

                            // 3. Explicitly Cancel Notifications
                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            if (notificationManager != null) {
                                notificationManager.cancelAll();
                            }

                            // 4. Clear SQL Databases
                            deleteDatabase("mServerData.db");
                            deleteDatabase("mNetwrokData.db");

                            // 5. Clear all SharedPreferences
                            if (getPref() != null) getPref().edit().clear().commit();
                            if (getDPrefs() != null) getDPrefs().edit().clear().commit();
                            getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE).edit().clear().commit();

                            // 6. Clear MMKV (V2Ray core storage)
                            com.tencent.mmkv.MMKV.defaultMMKV().clearAll();

                            // 7. Kill background processes and the app
                            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                            if (am != null) {
                                am.killBackgroundProcesses(getPackageName());
                            }

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    finishAffinity();
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                    System.exit(0);
                                } catch (Exception ignored) {}
                            }, 800);
                        } catch (Exception e) {
                            showToast("Error", "Could not clear data: " + e.getMessage());
                        }
                        cBuiler.dismiss();
                    });

                    btnCancel.setOnClickListener(v1 -> cBuiler.dismiss());

                    cBuiler.setView(inflateClear);
                    if (cBuiler.getWindow() != null) {
                        cBuiler.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        cBuiler.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
                    }
                    cBuiler.show();

                    // Set to full width and centered
                    if (cBuiler.getWindow() != null) {
                        cBuiler.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                        cBuiler.getWindow().setGravity(Gravity.CENTER);
                    }
                    return true;
                case "Exit All":
                    // Exit app
                    finishAffinity();
                    System.exit(0);
                    return true;
                case "Logout":
                    // Logout and go to LoginActivity
                    ConfigUtil.getInstance(this).logout();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void updateTunnelTypeText() {
        int index = getPref().getInt(manual_tunnel_radio_key, 0);
        // Map V2Ray variations (4, 5, 6) to the V2Ray label (index 4)
        int displayIndex = index;
        if (index >= 4 && index <= 6) {
            displayIndex = 4;
        }

        if (displayIndex >= 0 && displayIndex < mTypeList.length) {
            mTunnelType.setText(mTypeList[displayIndex]);
        } else {
            mTunnelType.setText("Unknown");
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
        
        // Start listening for service broadcasts immediately to restore state
        teststate1();

        doBindService();
        LoadDefaultConfig();
        findViewById(R.id.main_window_bg).setBackgroundColor(getConfig().getMainLayoutBG());
        loadIds();

        // Animate connection status badge
//        try {
//            ImageView statusIcon = findViewById(R.id.status_icon);
//            TextView connectionSecure = findViewById(R.id.connection_secure);
//
//            if (statusIcon != null) {
//                Animation signalPulse = AnimationUtils.loadAnimation(this, R.anim.signal_pulse);
//                statusIcon.startAnimation(signalPulse);
//            }
//
//            if (connectionSecure != null) {
//                // TypeWriter effect for connection status text
//                final String fullText = connectionSecure.getText().toString();
//                connectionSecure.setText("");
//
//                final Handler typeHandler = new Handler();
//                final int[] charIndex = {0};
//
//                Runnable typeRunnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        if (charIndex[0] < fullText.length()) {
//                            connectionSecure.setText(fullText.substring(0, charIndex[0] + 1));
//                            charIndex[0]++;
//                            typeHandler.postDelayed(this, 100); // 100ms per character
//                        } else {
//                            // Reset and repeat after 2 seconds
//                            typeHandler.postDelayed(() -> {
//                                charIndex[0] = 0;
//                                connectionSecure.setText("");
//                                typeHandler.post(this);
//                            }, 2000);
//                        }
//                    }
//                };
//
//                typeHandler.postDelayed(typeRunnable, 500); // Start after 500ms
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        // Set world map background


        loadV2RaySetups();
        TextView ipText = findViewById(R.id.ipTextView);
        ipText.setText(getLocalIpAddress());
        mTunnelType = findViewById(R.id.tunnel_spin);
        mLogConnectionStatus = findViewById(R.id.log_connection_status);
        v2ray_ping = findViewById(R.id.v2ray_ping);
        
        ImageView moreOptions = findViewById(R.id.imageView2);
        if (moreOptions != null) {
            moreOptions.setOnClickListener(v -> showMoreOptionsMenu(v));
        }

        updateTunnelTypeText();
        updateServerPing("~ ms");
        serverDialog.setOnClickListener(MainActivity.this);
        networkDialog.setOnClickListener(MainActivity.this);
        btn_connector.setOnClickListener(MainActivity.this);

        // submitReloadProfileIntent(getPref().getString(SERVER_TYPE_OVPN, "[]"));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (logSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && isDrawerOpen()) {
                    close();
                    logSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else if (logSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED && isDrawerOpen()) {
                    close();
                } else if (logSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && !isDrawerOpen()) {
                    logSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    if (progressSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "Unknown";
    }

    private void inboxNotification(int icon, String title, String msg, int ntfy) {
        Notification.Builder mBuilder = new Notification.Builder(MainActivity.this).setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_icon)).setSmallIcon(icon).setContentTitle("Message Received").setContentText(msg).setAutoCancel(true);
        Notification.BigTextStyle inboxStyle = new Notification.BigTextStyle();
        inboxStyle.setBigContentTitle(title);
        inboxStyle.bigText(msg);
        mBuilder.setStyle(inboxStyle);
        Intent intent = getIntent();
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(MainActivity.this);
        stackBuilder.addNextIntent(intent);
        mBuilder.setContentIntent(ConfigUtil.getPendingIntent(MainActivity.this));
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name_userreq);
            NotificationChannel mChannel = new NotificationChannel("openvpn_userreq", name, NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription(resString(R.string.channel_description_userreq));
            mChannel.enableVibration(true);
            mChannel.setLightColor(Color.parseColor("#00BCD4"));
            mBuilder.setChannelId("openvpn_userreq");
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(mChannel);
            }
        } else {
            mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
        }
        if (mNotificationManager != null) {
            mNotificationManager.notify(ntfy, mBuilder.build());
        }
    }

    private void autoUpdate() {
        if (!util.isNetworkAvailable(MainActivity.this)) return;

        isCheckUpdateIsRunning = true;
        String url = getPref().getString(CONFIG_URL, "");

        new checkUpdate(url, new checkUpdate.Listener() {
            @Override
            public void onError(String errorMsg) {
                isCheckUpdateIsRunning = false;
            }

            @Override
            public void onCompleted(final String config) {
                isCheckUpdateIsRunning = false;
                try {
                    String decrypted = c_01.decrypt(config);
                    JSONObject obj = new JSONObject(decrypted);

                    if (getConfig().getVersionCompare(
                            obj.getString("Version"),
                            getPref().getString(CONFIG_VERSION, "0"))) {

                        // ✅ Reuse shared update logic
                        applyConfigUpdate(obj);

                        // Show notification instead of dialog
                        inboxNotification(
                                R.drawable.icon_icon,
                                "New config release",
                                obj.getString("ReleaseNotes"),
                                3
                        );
                    }
                } catch (Exception e) {
                    isCheckUpdateIsRunning = false;
                    e.printStackTrace(); // ✅ Don’t swallow errors
                }
            }
        }).start();
    }

    /**
     * Shared logic for applying config update
     */
    private void applyConfigUpdate(JSONObject obj) throws JSONException {
        JSONArray sjarr = new JSONArray();
        JSONArray pjarr = new JSONArray();

        // Merge servers + networks
        mergeArrays(addOrEditedServers(), sjarr);
        mergeArrays(obj.getJSONArray("Servers"), sjarr);
        mergeArrays(addOrEditedNetwork(), pjarr);
        mergeArrays(obj.getJSONArray("HTTPNetworks"), pjarr);

        // Save database
        getServerData().updateData("1", sjarr.toString());
        getNetworkData().updateData("1", pjarr.toString());
        loadServerArrayDragaPosition();
        loadNetworkArrayDragaPosition();

        // Bulk preference update
        SharedPreferences.Editor editor = getEditor();
        editor.putInt(SERVER_POSITION, getPref().getInt(SERVER_POSITION, 0));
        editor.putInt(NETWORK_POSITION, getPref().getInt(NETWORK_POSITION, 0));
        editor.putString(CONFIG_VERSION, obj.getString("Version"));
        editor.putString(RELEASE_NOTE, obj.getString("ReleaseNotes"));
        editor.putString(CONTACT_SUPPORT, obj.getString("contactSupport"));
        editor.putString(OPEN_VPN_CERT, obj.getString("Ovpn_Cert"));
        editor.putString(CONFIG_URL, c_01.decrypt(obj.getString("config_url")));
        editor.putString(CONFIG_API, obj.has("account_api") ? c_01.decrypt(obj.getString("account_api")) : "");
        editor.putString(UPLOAD_GET_API, obj.has("upload_get_api") ? c_01.decrypt(obj.getString("upload_get_api")) : "");
        editor.putString(UPLOAD_POST_API, obj.has("upload_post_api") ? c_01.decrypt(obj.getString("upload_post_api")) : "");
        editor.putString(CONFIG_EDITOR_CODE, obj.has("AppConfPass") ? c_01.decrypt(obj.getString("AppConfPass")) : "");
        editor.putBoolean("isRandom", false);
        editor.putBoolean("isAdminAccept", false);
        editor.apply();

        if (obj.has("JSONsettings")) {
            getJSONsettings(obj.getJSONArray("JSONsettings").toString());
        }

        // UI + reload (run on UI thread just in case)
        runOnUiThread(() -> {
            doUpdateLayout();
            reLoad_Configs();
            loadServers();
            loadNetwork();

            Config_vers.setText(String.format("Config: %s", obj.optString("Version")));
            if (configVers != null) configVers.setText(String.format("Config: %s", obj.optString("Version")));
            updateTunnelTypeText();

            submitReloadProfileIntent(getPref().getString(SERVER_TYPE_OVPN, "[]"));
        });
    }


    /**
     * Helper to append all items from source into target JSONArray.
     */
    private void appendArray(JSONArray target, JSONArray source) throws JSONException {
        if (source == null || source.length() == 0) return;
        for (int i = 0; i < source.length(); i++) {
            target.put(source.getJSONObject(i));
        }
    }

    private void mUpdate() {
        new util(MainActivity.this);
        isCheckUpdateIsRunning = true;

        String url = getPref().getString(CONFIG_URL, "");
        showProgrss();

        new checkUpdate(url, new checkUpdate.Listener() {
            @Override
            public void onError(String errorMsg) {
                isCheckUpdateIsRunning = false;
                hideProgrss();
                util.showToast("Oppss...!", errorMsg);
            }

            @Override
            public void onCompleted(final String config) {
                isCheckUpdateIsRunning = false;
                hideProgrss();
                try {
                    String mData = c_01.decrypt(config);
                    JSONObject obj = new JSONObject(mData);

                    // Compare versions
                    if (getConfig().getVersionCompare(
                            obj.getString("Version"),
                            getPref().getString(CONFIG_VERSION, "0"))) {

                        JSONArray sjarr = new JSONArray();
                        JSONArray pjarr = new JSONArray();

                        // Merge servers
                        mergeArrays(addOrEditedServers(), sjarr);
                        mergeArrays(obj.getJSONArray("Servers"), sjarr);

                        // Merge networks
                        mergeArrays(addOrEditedNetwork(), pjarr);
                        mergeArrays(obj.getJSONArray("HTTPNetworks"), pjarr);

                        // Save updated arrays
                        getServerData().updateData("1", sjarr.toString());
                        getNetworkData().updateData("1", pjarr.toString());
                        loadServerArrayDragaPosition();
                        loadNetworkArrayDragaPosition();

                        // Bulk SharedPreferences update
                        SharedPreferences.Editor editor = getEditor();
                        editor.putInt(SERVER_POSITION, getPref().getInt(SERVER_POSITION, 0));
                        editor.putInt(NETWORK_POSITION, getPref().getInt(NETWORK_POSITION, 0));
                        editor.putString(CONFIG_VERSION, obj.getString("Version"));
                        editor.putString(RELEASE_NOTE, obj.getString("ReleaseNotes"));
                        editor.putString(CONTACT_SUPPORT, obj.getString("contactSupport"));
                        editor.putString(OPEN_VPN_CERT, obj.getString("Ovpn_Cert"));
                        editor.putString(CONFIG_URL, c_01.decrypt(obj.getString("config_url")));
                        editor.putString(CONFIG_API, obj.has("account_api") ? c_01.decrypt(obj.getString("account_api")) : "");
                        editor.putString(UPLOAD_GET_API, obj.has("upload_get_api") ? c_01.decrypt(obj.getString("upload_get_api")) : "");
                        editor.putString(UPLOAD_POST_API, obj.has("upload_post_api") ? c_01.decrypt(obj.getString("upload_post_api")) : "");
                        editor.putString(CONFIG_EDITOR_CODE, obj.has("AppConfPass") ? c_01.decrypt(obj.getString("AppConfPass")) : "");
                        editor.putBoolean("isRandom", false);
                        editor.putBoolean("isAdminAccept", false);
                        editor.apply();

                        if (obj.has("JSONsettings")) {
                            getJSONsettings(obj.getJSONArray("JSONsettings").toString());
                        }

                        // UI & reload on main thread
                        runOnUiThread(() -> {
                            doUpdateLayout();
                            reLoad_Configs();
                            loadServers();
                            loadNetwork();
                            Config_vers.setText(String.format("Config: %s", obj.optString("Version")));
                            if (configVers != null) configVers.setText(String.format("Config: %s", obj.optString("Version")));
                            updateTunnelTypeText();

                            showDialog("Release Note", obj.optString("ReleaseNotes"));
                            submitReloadProfileIntent(getPref().getString(SERVER_TYPE_OVPN, "[]"));
                        });

                    } else {
                        // Up to date → just show dialog
                        showDialog("Your config is up to date", getPref().getString(RELEASE_NOTE, ""));
                    }

                } catch (Exception e) {
                    isCheckUpdateIsRunning = false;
                    util.showToast("Error...!", e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Helper to merge one JSONArray into another.
     */
    private void mergeArrays(JSONArray source, JSONArray dest) throws JSONException {
        if (source != null) {
            for (int i = 0; i < source.length(); i++) {
                dest.put(source.getJSONObject(i));
            }
        }
    }

    private void mImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        // Using Activity Result API instead of deprecated startActivityForResult
        fileImportLauncher.launch(intent);
    }

    private void mIphunt() {
        if (cBuiler != null && cBuiler.isShowing()) cBuiler.dismiss();
        View inflate = LayoutInflater.from(this).inflate(R.layout.notif2, null);
        cBuiler = new AlertDialog.Builder(this).create();

        TextView title = inflate.findViewById(R.id.notiftext1);
        final TextView ms = inflate.findViewById(R.id.confimsg);
        RelativeLayout btn = inflate.findViewById(R.id.appButton2);
        TextView cancel = inflate.findViewById(R.id.appButton1);
        final TextView ok = inflate.findViewById(R.id.appButton2txt);

        // UI styling
        ms.setTextColor(getConfig().gettextColor());
        cancel.setTextColor(getConfig().getColorAccent());
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        btn.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        title.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        ok.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);

        title.setText("GTM IP Hunter");
        ms.setText("To connect to GTM No Load No Blocking, make sure that you are now in the Magic IP. Click the button to check your IP!");
        ok.setText("Check Now");
        cancel.setText("Close");

        // Background executor
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler uiHandler = new Handler(Looper.getMainLooper());

        btn.setOnClickListener(p1 -> {
            ms.setText("Please wait while we are checking your IP...");
            ok.setEnabled(false);
            ok.setText("Checking...");

            executor.execute(() -> {
                String magic = "✅ Congrats!! You are now connected to MAGIC IP.";
                String fail = "🚫 Disconnected. Please Airplane Mode On/Off and Try Again.";
                String resultMsg = fail;

                HttpURLConnection connection = null;
                InputStream in = null;
                try {
                    URL whatismyip = new URL("http://noloadbalance.globe.com.ph");
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("104.16.213.74", 80));
                    connection = (HttpURLConnection) whatismyip.openConnection(proxy);
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int l = 0;
                    in = connection.getInputStream();
                    byte[] buffer = new byte[4096];
                    int countBytesRead;
                    while ((countBytesRead = in.read(buffer)) != -1) {
                        l += countBytesRead;
                    }

                    if (l == 333 || connection.getResponseCode() == 200) {
                        resultMsg = magic;
                    }

                } catch (Exception e) {
                    resultMsg = fail;
                } finally {
                    if (in != null) try { in.close(); } catch (IOException ignored) {}
                    if (connection != null) connection.disconnect();
                }

                String finalResultMsg = resultMsg;
                uiHandler.post(() -> {
                    ms.setText(finalResultMsg);
                    ok.setText("Check Again");
                    ok.setEnabled(true);
                });
            });
        });

        cancel.setOnClickListener(p1 -> cBuiler.dismiss());

        cBuiler.setView(inflate);
        cBuiler.setCancelable(false);
        cBuiler.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        cBuiler.show();
    }


    private void showDialog(String t, final String message) {
        if (cBuiler != null) if (cBuiler.isShowing()) cBuiler.dismiss();
        View inflate = LayoutInflater.from(this).inflate(R.layout.notif2, null);
        cBuiler = new AlertDialog.Builder(this).create();
        RelativeLayout btn = inflate.findViewById(R.id.appButton2);
        TextView title = inflate.findViewById(R.id.notiftext1);
        TextView ms = inflate.findViewById(R.id.confimsg);
        TextView cancel = inflate.findViewById(R.id.appButton2txt);
        ms.setTextColor(getConfig().gettextColor());
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        btn.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        cancel.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        title.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        title.setText(t);
        cancel.setText("OKA'Y");
        inflate.findViewById(R.id.appButton1).setVisibility(View.GONE);
        btn.setOnClickListener(p1 -> cBuiler.dismiss());
        final Handler handler = new Handler();
        new Thread(() -> {
            for (i1 = 0; i1 < message.length(); i1++) {
                handler.post(() -> {
                    try {
                        ms.setText(message.substring(0, i1 + 1));
                    } catch (Exception ignored) {
                    }
                });
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
        cBuiler.setView(inflate);
        cBuiler.setCancelable(false);
        cBuiler.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        cBuiler.show();
    }

    private boolean LoadDefaultConfig() {
        boolean showFirstTime = getPref().getBoolean("connect_first_time", true);
        if (Boolean.valueOf(showFirstTime).booleanValue()) {
            init_default_preferences(prefs, getDEditor());
            try {
                String data = c_01.readFromAsset(MainActivity.this, "mtk.hs");
                JSONObject obj = new JSONObject(data);
                JSONArray jarr = new JSONArray();
                if (obj.getJSONArray("HTTPNetworks").length() != 0)
                    for (int i = 0; i < obj.getJSONArray("HTTPNetworks").length(); i++) {
                        jarr.put(obj.getJSONArray("HTTPNetworks").getJSONObject(i));
                    }
                if (jarr.length() == 0) {
                    getNetworkData().insertData("[]");
                } else if (jarr.length() != 0) {
                    getNetworkData().insertData(jarr.toString());
                }
                getServerData().insertData(obj.getJSONArray("Servers").toString());
                loadServerArrayDragaPosition();
                loadNetworkArrayDragaPosition();
                getEditor().putString(CONFIG_VERSION, obj.getString("Version")).apply();
                getEditor().putString(RELEASE_NOTE, obj.getString("ReleaseNotes")).apply();
                getEditor().putString(CONTACT_SUPPORT, obj.getString("contactSupport")).apply();
                getEditor().putString(OPEN_VPN_CERT, obj.getString("Ovpn_Cert")).apply();
                getEditor().putString(CONFIG_URL, c_01.decrypt(obj.getString("config_url"))).apply();
                getEditor().putString(CONFIG_API, obj.has("account_api") ? c_01.decrypt(obj.getString("account_api")) : "").apply();
                getEditor().putString(UPLOAD_GET_API, obj.has("upload_get_api") ? c_01.decrypt(obj.getString("upload_get_api")) : "").apply();
                getEditor().putString(UPLOAD_POST_API, obj.has("upload_post_api") ? c_01.decrypt(obj.getString("upload_post_api")) : "").apply();
                getEditor().putString(CONFIG_EDITOR_CODE, obj.has("AppConfPass") ? c_01.decrypt(obj.getString("AppConfPass")) : "").apply();
                if (obj.has("JSONsettings"))
                    getJSONsettings(obj.getJSONArray("JSONsettings").toString());
                getEditor().putBoolean("isRandom", false).apply();
                getEditor().putBoolean("isAdminAccept", false).apply();
                reLoad_Configs();
                getEditor().putBoolean("connect_first_time", false).apply();
                return true;
            } catch (Exception e) {
                showToast("LoadDefaultConfig Error!", e.getMessage());
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mFirstNotes(getPref().getString(RELEASE_NOTE, ""));
    }

    private void mFirstNotes() {
        // 1. Dismiss existing dialog if already showing
        if (cBuiler != null)
            if (cBuiler.isShowing()) cBuiler.dismiss();

        // 2. Only show dialog if user hasn't responded before
        if (!getDPrefs().getBoolean("join_tele", false)) {

            // Inflate custom layout
            View inflate = LayoutInflater.from(this).inflate(R.layout.notification_dialog, null);
            cBuiler = new AlertDialog.Builder(this).create();

            // 3. Set dialog message text
            ((TextView)inflate.findViewById(R.id.notification_message)).setText(
                    "We have a Admin support channel where we post\n" +
                            "and discuss about Settings, new Features, and also\n" +
                            "assist our Users.\n" +
                            "Would you like to Contact us there?"
            );

            // 4. "No" button → Save preference and dismiss
            inflate.findViewById(R.id.notification_btn_no).setOnClickListener(p1 -> {
                getDEditor().putBoolean("join_tele", false).apply();
                cBuiler.dismiss();
            });

            // 5. "Yes" button → Save preference, open Telegram link, then dismiss
            inflate.findViewById(R.id.notification_btn_yes).setOnClickListener(p1 -> {
                getDEditor().putBoolean("join_tele", true).apply();
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getConfig().getContactUrl()));
                    startActivity(Intent.createChooser(intent, "launch Whatsapp"));
                } catch (Exception e) {
                    util.showToast("Error", "Please download the Whatsapp app");
                }
                cBuiler.dismiss();
            });

            // 6. Show the dialog
            cBuiler.setView(inflate);
            cBuiler.setCancelable(false);
            cBuiler.show();
        }
    }

    private void mFirstNotes(String message) {
        View inflate = LayoutInflater.from(this).inflate(R.layout.notif2, null);
        AlertDialog alertDialogBuilder = new AlertDialog.Builder(this).create();
        TextView title = inflate.findViewById(R.id.notiftext1);
        final TextView ms = inflate.findViewById(R.id.confimsg);
        TextView cancel = inflate.findViewById(R.id.appButton2txt);
        RelativeLayout btn = inflate.findViewById(R.id.appButton2);
        ms.setTextColor(getConfig().gettextColor());
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        btn.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        cancel.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        title.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        title.setText("Release Note");
        ms.setText(message);
        cancel.setText("OKA'Y");
        inflate.findViewById(R.id.appButton1).setVisibility(View.GONE);
        btn.setOnClickListener(p1 -> alertDialogBuilder.dismiss());
        alertDialogBuilder.setView(inflate);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        alertDialogBuilder.show();
    }

    private void showRenewServDialog() {
        if (cBuiler != null) if (cBuiler.isShowing()) cBuiler.dismiss();
        View inflate = LayoutInflater.from(this).inflate(R.layout.notif2, null);
        cBuiler = new AlertDialog.Builder(this).create();
        RelativeLayout btn = inflate.findViewById(R.id.appButton2);
        TextView title = inflate.findViewById(R.id.notiftext1);
        TextView ms = inflate.findViewById(R.id.confimsg);
        TextView cancel = inflate.findViewById(R.id.appButton2txt);
        ms.setTextColor(getConfig().gettextColor());
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        btn.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        cancel.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        title.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        title.setText("Server Expired!");
        cancel.setText("RENEW");
        inflate.findViewById(R.id.appButton0).setVisibility(View.GONE);
        btn.setOnClickListener(p1 -> {
            startActivity(new Intent(MainActivity.this, mtk0005.class).putExtra("mConfigPanelRenew", getPref().getString(SERVER_WEB_RENEW_KEY, "")));
            cBuiler.dismiss();
        });
        String message = "Your server is now expired,\nclick the renew button to create new server";
        final Handler handler = new Handler();
        new Thread(() -> {
            for (i11 = 0; i11 < message.length(); i11++) {
                handler.post(() -> {
                    try {
                        ms.setText(message.substring(0, i11 + 1));
                    } catch (Exception ignored) {
                    }
                });
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
        cBuiler.setView(inflate);
        cBuiler.setCancelable(true);
        cBuiler.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        cBuiler.show();
    }

    private boolean containsSpecialCharacter(String input) {
        String AllowedCharacters = "[^a-zA-Z0-9]+";
        return input.matches(".*" + AllowedCharacters + ".*");
    }

    private boolean checkConfiguration() {
        if (!util.isMyApp()) {
            submitDisconnectIntent();
            showToast("Oppss...!", new String(new byte[]{80, 108, 97, 101, 115, 101, 32, 105, 110, 115, 116, 97, 108, 108, 32, 116, 104, 101, 32, 111, 114, 105, 103, 105, 110, 97, 108,}) + " " + resString(R.string.app_name));
            addlogInfo("<font color = #d50000>" + new String(new byte[]{80, 108, 97, 101, 115, 101, 32, 105, 110, 115, 116, 97, 108, 108, 32, 116, 104, 101, 32, 111, 114, 105, 103, 105, 110, 97, 108,}) + " " + resString(R.string.app_name));
            return false;
        } else if (!reLoad_Configs()) {
            showToast("Oppss...!", "Config load error!");
            return false;
        } else if (!util.isNetworkAvailable(MainActivity.this)) {
            showToast("Oppss...!", "Please connect to the internet");
            return false;
        }

        String rawExpiry = getPref().getString("_AccountRawXp", "");
        if (!rawExpiry.isEmpty() && !rawExpiry.equals("none")) {
            if (util.getDaysLeft(rawExpiry).equals("Expired")) {
                showToast("Account Expired", "Your account has expired. Please renew to continue.");
                addlogInfo("<font color = #d50000>Account expired. Connection rejected.");
                return false;
            }
        }

        if (getPref().getBoolean(CONFIG_EXP_KEY, false)) {
            addlogInfo("<font color = #FFFF002E>Oppss sorry! Your server is now expired,click the renew button to create new and fresh server");
            showRenewServDialog();
            return false;
        } else if (getConfig().getConfigIsAutoLogIn()) {
            String user = getPref().getString("_screenUsername_key", "");
            String pass = getPref().getString("_screenPassword_key", "");
            if (getConfig().getSecureString(USERNAME_KEY).isEmpty() || getConfig().getSecureString(PASSWORD_KEY).isEmpty() || containsSpecialCharacter(user) || containsSpecialCharacter(pass) || user.length() < 4 || pass.length() < 4) {
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
            if (val1 != null) val1.setText("0 bit");
            if (val2 != null) val2.setText("0 bit");
            if (trafficGraph != null) {
                trafficGraph.clear();
                trafficGraph.setShowPath(true);
            }
            if (getConfig().getAutoClearLog()) mAdapter.clearLog();
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
        isDisconnecting = true;
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
        }
        if (isDrawerOpen()) close();
        autoUpdate();
        
        // Ensure UI is populated correctly even when VPN is active (e.g. after process kill)
        reLoad_Configs();
        loadServers();
        loadNetwork();
        updateTunnelTypeText();
        
        if (!isActive) {
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

        if (logSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
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
        isDisconnecting = false;
        m_SentBytes = 0;
        m_ReceivedBytes = 0;
        hLogStatus.resetTrafficHistory();
        StatisticGraphData.getStatisticData().getDataTransferStats().stop();
        if (byteIn_view != null) byteIn_view.setText("0 B");
        if (byteOut_view != null) byteOut_view.setText("0 B");
        if (mDataInTv != null) mDataInTv.setText("0 bit");
        if (mDataOutTv != null) mDataOutTv.setText("0 bit");
        if (val1 != null) val1.setText("0 bit");
        if (val2 != null) val2.setText("0 bit");
        if (duration_view != null) duration_view.setText("00h:00m:00s");

        if (trafficGraph != null) {
            trafficGraph.clear();
            trafficGraph.setShowPath(true);
            trafficGraph.setFrozen(false);
        }

        // 1. Give immediate UI feedback
        hLogStatus.updateStateString(hLogStatus.VPN_CONNECTING, getString(R.string.state_connecting));
        
        // 2. Start Authentication in parallel (Instant fire)
        // Only fetch if we don't have it or it's needed
        if (shouldFetchAccountDetails) {
            dataAuthetication();
        }
        
        TunnelUtils.restartRotateAndRandom();
        StatisticGraphData.getStatisticData().getDataTransferStats().startConnected();
        schedule_stats();
        show_stats(); // Trigger first UI update immediately
        
        // 3. Move heavy config loading and service start to background to avoid UI lag
        if (statsExecutor != null && !statsExecutor.isShutdown()) {
            statsExecutor.execute(() -> {
                if (getConfig().getServerType().equals(SERVER_TYPE_V2RAY)) {
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
        boolean is_auth_pwd_save = false;
        String profile_name = getConfig().getServerName();
        String vpn_proto = prefs.get_string("vpn_proto");
        String conn_timeout = prefs.get_string("conn_timeout");
        String compression_mode = prefs.get_string("compression_mode");
        String ipv6 = this.prefs.get_string("ipv6");
        submitConnectIntent(profile_name, server, vpn_proto, ipv6, conn_timeout, username, password, is_auth_pwd_save, pk_password, response, epki_alias, compression_mode, proxy_name, null, null, true, get_gui_version(app_name));
    }

    /**
     * @deprecated This method is deprecated. Use the Activity Result API instead.
     * The functionality has been migrated to vpnServiceLauncher and fileImportLauncher.
     */
    @Override
    @Deprecated
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        if (result != RESULT_OK) {
            // No successful result → just return or refresh if needed
            if (request == REQUEST_IMPORT_FILE) {
                recreate();
            }
            return;
        }

        switch (request) {
            case START_BIND_CALLED:
                start_connect();
                return;

            case REQUEST_IMPORT_FILE:
                if (data == null || data.getData() == null) {
                    util.showToast("Import failed", "Invalid file data");
                    return;
                }

                try {
                    Uri uri = data.getData();
                    String decryptedData = c_01.decrypt(c_01.readTextUri(MainActivity.this, uri));
                    JSONObject obj = new JSONObject(decryptedData);

                    // Compare versions before merging
                    if (!getConfig().getVersionCompare(
                            obj.getString("Version"),
                            getPref().getString(CONFIG_VERSION, "0"))) {
                        showDialog("Your config is up to date",
                                getPref().getString(RELEASE_NOTE, ""));
                        return;
                    }

                    // Merge server/network arrays
                    JSONArray sjarr = new JSONArray();
                    JSONArray pjarr = new JSONArray();

                    appendArray(sjarr, addOrEditedServers());
                    appendArray(sjarr, obj.optJSONArray("Servers"));
                    appendArray(pjarr, addOrEditedNetwork());
                    appendArray(pjarr, obj.optJSONArray("HTTPNetworks"));

                    // Update stored data
                    getServerData().updateData("1", sjarr.toString());
                    getNetworkData().updateData("1", pjarr.toString());
                    loadServerArrayDragaPosition();
                    loadNetworkArrayDragaPosition();

                    // Bulk apply editor updates instead of repeating
                    SharedPreferences.Editor editor = getEditor();
                    editor.putInt(SERVER_POSITION, getPref().getInt(SERVER_POSITION, 0));
                    editor.putInt(NETWORK_POSITION, getPref().getInt(NETWORK_POSITION, 0));
                    editor.putString(CONFIG_VERSION, obj.getString("Version"));
                    editor.putString(RELEASE_NOTE, obj.getString("ReleaseNotes"));
                    editor.putString(CONTACT_SUPPORT, obj.getString("contactSupport"));
                    editor.putString(OPEN_VPN_CERT, obj.getString("Ovpn_Cert"));
                    editor.putString(CONFIG_URL, c_01.decrypt(obj.getString("config_url")));
                    editor.putString(CONFIG_API, obj.has("account_api") ?
                            c_01.decrypt(obj.getString("account_api")) : "");
                    editor.putString(UPLOAD_GET_API, obj.has("upload_get_api") ?
                            c_01.decrypt(obj.getString("upload_get_api")) : "");
                    editor.putString(UPLOAD_POST_API, obj.has("upload_post_api") ?
                            c_01.decrypt(obj.getString("upload_post_api")) : "");
                    editor.putString(CONFIG_EDITOR_CODE, obj.has("AppConfPass") ?
                            c_01.decrypt(obj.getString("AppConfPass")) : "");
                    editor.putBoolean("isRandom", false);
                    editor.putBoolean("isAdminAccept", false);
                    editor.apply();

                    // Extra JSON settings
                    if (obj.has("JSONsettings")) {
                        getJSONsettings(obj.getJSONArray("JSONsettings").toString());
                    }

                    // Update UI
                    Config_vers.setText(
                            String.format("Config Ver: %s",
                                    getPref().getString(CONFIG_VERSION, "1.1")));
                    showDialog("Release Note", obj.getString("ReleaseNotes"));
                    reLoad_Configs();
                    loadServers();
                    loadNetwork();
                    updateTunnelTypeText();

                    // Sync VPN profile
                    submitReloadProfileIntent(getPref().getString(SERVER_TYPE_OVPN, "[]"));
                    if (dex002.isVPNRunning()) stopTunnelService();

                } catch (Exception e) {
                    util.showToast("Error importing config", e.getMessage());
                    Log.e("ConfigImport", "Error parsing import file", e);
                }
                return;

            default:
                super.onActivityResult(request, result, data);
        }
    }

    private void mPaste() {
        if (cBuiler != null) if (cBuiler.isShowing()) cBuiler.dismiss();
        View inflate = LayoutInflater.from(MainActivity.this).inflate(R.layout.clip_dialog, null);
        final AlertDialog clipBuilder = new AlertDialog.Builder(MainActivity.this).create();
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        ((TextView) inflate.findViewById(R.id.appButton1)).setTextColor(getConfig().getColorAccent());
        ((TextView) inflate.findViewById(R.id.notiftext1)).setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        ((TextView) inflate.findViewById(R.id.import_tv)).setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        inflate.findViewById(R.id.appButton2).setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        inflate.findViewById(R.id.appButton0).setOnClickListener(p1 -> {
            clipBuilder.dismiss();
        });
        inflate.findViewById(R.id.appButton2).setOnClickListener(v -> {
            String clipData = c_01.getClipboard(MainActivity.this);
            if (clipData.isEmpty()) {
                showToast("Error!", "Config Clipboard is empty!");
                return;
            }
            String mData = c_01.decrypt(clipData);
            try {
                JSONArray sjarr = new JSONArray();
                JSONArray pjarr = new JSONArray();
                JSONObject obj = new JSONObject(mData);
                if (getConfig().getVersionCompare(obj.getString("Version"), getPref().getString(CONFIG_VERSION, "0"))) {
                    if (addOrEditedServers().length() != 0)
                        for (int i = 0; i < addOrEditedServers().length(); i++) {
                            sjarr.put(addOrEditedServers().getJSONObject(i));
                        }
                    if (obj.getJSONArray("Servers").length() != 0)
                        for (int i = 0; i < obj.getJSONArray("Servers").length(); i++) {
                            sjarr.put(obj.getJSONArray("Servers").getJSONObject(i));
                        }
                    if (addOrEditedNetwork().length() != 0)
                        for (int i = 0; i < addOrEditedNetwork().length(); i++) {
                            pjarr.put(addOrEditedNetwork().getJSONObject(i));
                        }
                    if (obj.getJSONArray("HTTPNetworks").length() != 0)
                        for (int i = 0; i < obj.getJSONArray("HTTPNetworks").length(); i++) {
                            pjarr.put(obj.getJSONArray("HTTPNetworks").getJSONObject(i));
                        }
                    getServerData().updateData("1", sjarr.toString());
                    getNetworkData().updateData("1", pjarr.toString());
                    loadServerArrayDragaPosition();
                    loadNetworkArrayDragaPosition();
                    getEditor().putInt(SERVER_POSITION, getPref().getInt(SERVER_POSITION, 0)).apply();
                    getEditor().putInt(NETWORK_POSITION, getPref().getInt(NETWORK_POSITION, 0)).apply();
                    getEditor().putString(CONFIG_VERSION, obj.getString("Version")).apply();
                    getEditor().putString(RELEASE_NOTE, obj.getString("ReleaseNotes")).apply();
                    getEditor().putString(CONTACT_SUPPORT, obj.getString("contactSupport")).apply();
                    getEditor().putString(OPEN_VPN_CERT, obj.getString("Ovpn_Cert")).apply();
                    getEditor().putString(CONFIG_URL, c_01.decrypt(obj.getString("config_url"))).apply();
                    getEditor().putString(CONFIG_API, obj.has("account_api") ? c_01.decrypt(obj.getString("account_api")) : "").apply();
                    getEditor().putString(UPLOAD_GET_API, obj.has("upload_get_api") ? c_01.decrypt(obj.getString("upload_get_api")) : "").apply();
                    getEditor().putString(UPLOAD_POST_API, obj.has("upload_post_api") ? c_01.decrypt(obj.getString("upload_post_api")) : "").apply();
                    getEditor().putString(CONFIG_EDITOR_CODE, obj.has("AppConfPass") ? c_01.decrypt(obj.getString("AppConfPass")) : "").apply();
                    if (obj.has("JSONsettings"))
                        getJSONsettings(obj.getJSONArray("JSONsettings").toString());
                    getEditor().putBoolean("isRandom", false).apply();
                    getEditor().putBoolean("isAdminAccept", false).apply();
                    showDialog("Release Note", obj.getString("ReleaseNotes"));
                    reLoad_Configs();
                    loadServers();
                    loadNetwork();
                    if (Config_vers != null) {
                        String ver = String.format("Config: %s", getPref().getString(CONFIG_VERSION, "1.1"));
                        Config_vers.setText(ver);
                        if (configVers != null) configVers.setText(ver);
                    }
                    updateTunnelTypeText();
                    submitReloadProfileIntent(getPref().getString(SERVER_TYPE_OVPN, "[]"));
                    if (dex002.isVPNRunning()) stopTunnelService();
                    clipBuilder.dismiss();
                } else {
                    showDialog("Your config is up to date", getPref().getString(RELEASE_NOTE, ""));
                    clipBuilder.dismiss();
                }
            } catch (Exception e) {
                showToast("Error...!", e.getMessage());
            }
        });
        clipBuilder.setView(inflate);
        clipBuilder.setCancelable(false);
        clipBuilder.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        clipBuilder.show();
    }


    private void loadServers() {
        try {
            JSONObject js = serverArrayDragaPosition().getJSONObject(getPref().getInt(SERVER_POSITION, 0));
            s_name.setText(js.getString("Name"));
            MmkvManager.INSTANCE.encodeSettings(MmkvManager.KEY_SELECTED_SERVER_NAME, js.getString("Name"));
            TextView tv2 = findViewById(R.id.sname3);
            tv2.setText(getServerType(js));

            // Update visible server card
            TextView serverName = findViewById(R.id.server_name);
            TextView serverInfo = findViewById(R.id.server_info);
            if (serverName != null) serverName.setText(js.getString("Name"));
            if (serverInfo != null) serverInfo.setText(getServerType(js));

            InputStream open = getAssets().open("flags/" + "flag_" + js.getString("FLAG") + ".webp");
            Drawable flagDrawable = Drawable.createFromStream(open, null);
            ((ImageView) findViewById(R.id.sicon)).setImageDrawable(flagDrawable);

            // Set visible server icon
            ImageView serverSpinIcon = findViewById(R.id.server_spin_icon);
            if (serverSpinIcon != null && flagDrawable != null) {
                serverSpinIcon.setImageDrawable(flagDrawable);
            }
            s_name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 7);
            if (getPref().getBoolean("isRandom", false) && getPref().getBoolean("show_random_layout", false)) {
                s_name.setText("Auto select servers");
                tv2.setText("Random");
                if (serverName != null) serverName.setText("Auto select servers");
                if (serverInfo != null) serverInfo.setText("Random");
                ((ImageView) findViewById(R.id.sicon)).setImageResource(R.drawable.auto_server_list);
                if (serverSpinIcon != null) serverSpinIcon.setImageResource(R.drawable.auto_server_list);
            }
            serverDialog.setVisibility(View.VISIBLE);
            if (getPref().getBoolean(CONFIG_EXP_KEY, false)) {
                tv2.setText("Server Expired");
            }
            V2RAY_TYPE();
            open.close();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in loadServers", e);
            serverDialog.setVisibility(View.GONE);
            loadNetwork();
            V2RAY_TYPE();
        }
    }

    private String getServerType(JSONObject js) throws JSONException {
        if (js.getInt("Category") == 0) {
            return "Websocket Ovpn";
        } else if (js.getInt("Category") == 1) {
            return "Fast V2ray";
        } else if (js.getInt("Category") == 2) {
            return "Fast Ssh";
        } else if (js.getInt("Category") == 3) {
            return "Dnstt Server";
        } else if (js.getInt("Category") == 4) {
            return "Fast Udp";
        }
        return "Random";
    }

    private void loadNetwork() {
        try {
            networkDialog.setVisibility((serverDialog.getVisibility() == View.GONE) ? View.GONE : View.VISIBLE);
            // Check visible server card instead of hidden s_name
            TextView serverName = findViewById(R.id.server_name);
            if (serverName != null && serverName.getVisibility() == View.GONE) {
                networkDialog.setVisibility(View.GONE);
                return;
            }
            if (s_name != null && s_name.getVisibility() == View.GONE && (serverName == null || serverName.getVisibility() == View.GONE)) {
                networkDialog.setVisibility(View.GONE);
                return;
            }
            JSONObject js = networkArrayDragaPosition().getJSONObject(getPref().getInt(NETWORK_POSITION, 0));
            p_name.setText(js.getString("Name"));
            MmkvManager.INSTANCE.encodeSettings(MmkvManager.KEY_SELECTED_PAYLOAD_NAME, js.getString("Name"));

            // Update visible network card
            TextView payloadName = findViewById(R.id.payload_name);
            if (payloadName != null) payloadName.setText(js.getString("Name"));

            TextView payload_info = findViewById(R.id.payload_info);
            // payload_info.setTextColor(getConfig().getAppThemeUtil() ? Color.WHITE : Color.BLACK);
            if (js.has("Info") && !js.getString("Info").isEmpty()) {
                payload_info.setText(js.getString("Info"));
            } else {
                payload_info.setText(R.string.app_name);
            }
            TextView tv4 = findViewById(R.id.pname3);
            tv4.setText(getNetworkType(js));

            // Update payload tag text with protocol/tunnel type
            TextView payloadTagText = findViewById(R.id.payload_tag_text);
            if (payloadTagText != null) {
                payloadTagText.setText(getNetworkType(js));
            }
            InputStream open = getAssets().open("networks/" + "icon_" + js.getString("FLAG") + ".png");
            Drawable networkDrawable = Drawable.createFromStream(open, null);
            ((ImageView) findViewById(R.id.picon)).setImageDrawable(networkDrawable);

            // Set visible payload icon
            ImageView payloadSpinIcon = findViewById(R.id.payload_spin_icon);
            if (payloadSpinIcon != null && networkDrawable != null) {
                payloadSpinIcon.setImageDrawable(networkDrawable);
            }
            p_name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            tv4.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 7);
            networkDialog.setVisibility(View.VISIBLE);
            V2RAY_TYPE();
            open.close();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in loadNetwork", e);
            networkDialog.setVisibility(View.GONE);
            V2RAY_TYPE();
        }
    }

    private void V2RAY_TYPE() {
        if (getConfig().getServerType().equals(SERVER_TYPE_OVPN) || getConfig().getServerType().equals(SERVER_TYPE_SSH)) {
            boolean isCustom_prxPort = getConfig().getPayloadType() != PAYLOAD_TYPE_OVPN_UDP && getConfig().getPayloadType() != PAYLOAD_TYPE_DIRECT && getConfig().getPayloadType() != PAYLOAD_TYPE_DIRECT_PAYLOAD && getConfig().getPayloadType() != PAYLOAD_TYPE_SSL && getConfig().getPayloadType() != PAYLOAD_TYPE_SSL_PAYLOAD;
            if (!isCustom_prxPort) {
                getEditor().putInt(CUSTOM_NETWORK_PORT_KEY, 0).apply();
                port_spin.setSelection(getPref().getInt(CUSTOM_SERVER_POR_KEY, 0));
                prx_spin.setSelection(0);
            }
        } else {
            getEditor().putInt(CUSTOM_SERVER_POR_KEY, 0).apply();
            getEditor().putInt(CUSTOM_NETWORK_PORT_KEY, 0).apply();
            port_spin.setSelection(0);
            prx_spin.setSelection(0);
        }
    }

    private String getNetworkType(JSONObject js) throws JSONException {
        int proto = js.getInt("proto_spin");
        String name = js.getString("Name");
        boolean isDirect = name.contains("Direct") || name.contains("direct");

        return switch (proto) {
            case 0 -> {
                if (isDirect) {
                    yield js.getString("NetworkPayload").isEmpty()
                            ? "Direct"
                            : "Direct Payload";
                }
                yield "TcpV4 | Http | Proxy";
            }
            case 1 -> "Hysteria V2";
            case 2 -> "Slowdns";
            case 3 -> "TcpV4 | Direct | Ssl";
            case 4 -> "TcpV4 | Ssl | Payload";
            case 5 -> "TcpV4 | Ssl | Proxy";
            case 6 -> "V2ray | Xray";
            default -> "Unknown!";
        };
    }


    private void getJSONsettings(String obj) {
        try {
            JSONArray jarr = new JSONArray(obj.trim());
            for (int i = 0; i < jarr.length(); i++) {
                JSONObject js = jarr.getJSONObject(i);
                getConfig().setLocalPort(js.getString("mLocalPort"));
                getConfig().setAutoClearLog(js.getBoolean("mAutoClearLog"));
                getConfig().setDisabledDelaySSH(js.getBoolean("mIsDisabledDelaySSH"));
                getConfig().setCompression(js.getBoolean("mCompression"));
                getConfig().setVpnDnsForward(js.getBoolean("mVpnDnsForward"));
                getConfig().setVpnDnsResolver(js.getString("mVpnDnsResolver"));
                getConfig().setVpnUdpForward(js.getBoolean("mVpnUdpForward"));
                getConfig().setVpnUdpResolver(js.getString("mVpnUdpResolver"));
                getConfig().setPingThread(Integer.parseInt(js.getString("mSSHPinger").isEmpty() ? "3" : js.getString("mSSHPinger")));
                getConfig().setPingServer(js.getString("mPingServer"));
                getConfig().setProxyAddress(js.getString("mProxyAddress"));
                getConfig().setReconnTime(js.getInt("mReconnTime"));
                getConfig().setTetheringSubnet(js.getBoolean("mIsTetheringSubnet"));
            }
        } catch (JSONException e) {
            showToast("getJSONsettings Error!", e.getMessage());
        }
    }

    /*private void setupBTNanimation(boolean isRunning) {

        // Start Graph Animation
        findViewById(R.id.graph_layout).setVisibility(isRunning ? View.VISIBLE : View.GONE);

        // Case 1: Screen OFF or No Network
        if (!getConfig().getIsScreenOn() || !util.isNetworkAvailable(MainActivity.this)) {
            //stopAnimations();

            btn_connector.setBackground(ContextCompat.getDrawable(this, R.drawable.button_connect));
            btn_connector.setText("CONNECT");
            btn_connector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

            //clearAllDataAnim(!isRunning);
            return;
        }

        // Case 2: Already connected
        if (isConnected) {
            stopAnimations();

            btn_connector.setBackground(ContextCompat.getDrawable(this, R.drawable.button_disconnect));
            btn_connector.setText("DISCONNECTED");
            btn_connector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            btn_connector.setTextColor(Color.WHITE);
            return;
        }

        // Case 3: Trying to connect (running animation)
        if (isRunning) {
            if (!mRotateLoading.isStart()) {
                mRotateLoading.start();
                //btn_connector.startAnimation(animation);
            }
            // Optional: btn_connector.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            //circleProgressBar.setProgressWithAnimation(0);
        }
        // Case 4: Not connected, not running
        else {
            //clearAllDataAnim(true);

            btn_connector.setBackground(ContextCompat.getDrawable(this, R.drawable.button_connect));
            btn_connector.setText("CONNECT");
            btn_connector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
    }*/

   /* private void setupBTNanimation(boolean isRunning) {
        // Cache frequently used views
        View btnIcon = findViewById(R.id.btn_connect_icon);

        //Start Graph Animation
        findViewById(R.id.graph_layout).setVisibility(isRunning? View.VISIBLE:View.GONE);

        // Case 1: Screen OFF or No Network
        if (!getConfig().getIsScreenOn() || !util.isNetworkAvailable(MainActivity.this)) {
            stopAnimations();
            btnIcon.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            circleProgressBar.setProgressWithAnimation(isConnected ? 100 : 0);
            clearAllDataAnim(!isRunning);
            return;
        }

        // Case 2: Already connected
        if (isConnected) {
            stopAnimations();
            btnIcon.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            circleProgressBar.setProgressWithAnimation(100);
            return;
        }

        // Case 3: Trying to connect (running animation)
        if (isRunning) {
            if (!mRotateLoading.isStart()) {
                mRotateLoading.start();
                btn_connector.startAnimation(animation);
            }
            //btnIcon.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            circleProgressBar.setProgressWithAnimation(0);
        }
        // Case 4: Not connected, not running
        else {
            clearAllDataAnim(true);
            btnIcon.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        }
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
            if (findViewById(R.id.livedata_dot) != null) {
                findViewById(R.id.livedata_dot).setBackgroundResource(R.drawable.livedata_dot_red);
            }
            if (animation != null) animation.cancel();
            clearAllTestDelay();
        }
    }*/

    private void setupBTNanimation(boolean isRunning) {
        // Show/hide graph animation
        if (graphLayout != null) {
            graphLayout.setVisibility(isRunning ? View.VISIBLE : View.INVISIBLE);
        }

        // Handle animations based on connection state
        if (!getConfig().getIsScreenOn() || !util.isNetworkAvailable(MainActivity.this)) {
            stopAnimations();
            circleProgressBar.setProgressWithAnimation(isConnected ? 100 : 0);
            clearAllDataAnim(!isRunning);
            return;
        }

        if (isConnected) {
            // Connected state - stop animations, show full progress
            stopAnimations();
            circleProgressBar.setProgressWithAnimation(100);
            return;
        }

        if (isRunning) {
            // Connecting state - start rotating animation
            if (!mRotateLoading.isStart()) {
                mRotateLoading.start();
            }
            circleProgressBar.setProgressWithAnimation(0);
        } else {
            // Disconnected state - clear animations
            clearAllDataAnim(true);
        }
    }

    /*private void setupBTNanimation_OLD(boolean isRunning) {

        //Start Graph Animation
        findViewById(R.id.graph_layout).setVisibility(isRunning ? View.VISIBLE : View.GONE);

        // --- Handle button appearance ---
        if (!getConfig().getIsScreenOn() || !util.isNetworkAvailable(MainActivity.this)) {
            stopAnimations();
            circleProgressBar.setProgressWithAnimation(isConnected ? 100 : 0);
            clearAllDataAnim(!isRunning);
            btn_connector.setBackground(ContextCompat.getDrawable(this, R.drawable.buttonx));
            btn_connector.setText("START");
            btn_connector.setTextColor(Color.WHITE);
            btn_connector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            return;
        }

        if (isConnected) {
            // ✅ Connected — show disconnect button
            stopAnimations();
            circleProgressBar.setProgressWithAnimation(100);
            btn_connector.setBackground(ContextCompat.getDrawable(this, R.drawable.buttonx));
            btn_connector.setText("STOP");
            btn_connector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            btn_connector.setTextColor(Color.WHITE);
            return;
        }

        if (isRunning) {
            // ✅ Connecting — same look as disconnect
            if (!mRotateLoading.isStart()) {
                mRotateLoading.start();
            }


            btn_connector.setBackground(ContextCompat.getDrawable(this, R.drawable.buttonx));
            btn_connector.setText("STOP");
            btn_connector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            btn_connector.setTextColor(Color.WHITE);
        }

        else {
            // ✅ Default state
            clearAllDataAnim(true);
            btn_connector.setBackground(ContextCompat.getDrawable(this, R.drawable.buttonx));
            btn_connector.setText("START");
            btn_connector.setTextColor(Color.WHITE);
            btn_connector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
    }*/

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
            if (findViewById(R.id.livedata_dot) != null) {
                findViewById(R.id.livedata_dot).setBackgroundResource(R.drawable.livedata_dot_red);
            }
            if (animation != null) animation.cancel();
            clearAllTestDelay();
        }
    }


    private void loadMainDrawer() {
        drawerNavigationView = findViewById(R.id.drawerNavigationView);
        mDrawerLayout = findViewById(R.id.drawerLayoutMain);
        contentView = findViewById(R.id.main_content);
        View v = drawerNavigationView.getHeaderView(0);
        // The lines below were overriding XML properties.
        // Commented out to allow XML changes to take effect.
        /*
        TextView navheaderTextView2 = v.findViewById(R.id.navheaderTextView2);
        TextView navheaderTextView3 = v.findViewById(R.id.nav_headerAppVersion);
        navheaderTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        navheaderTextView3.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        navheaderTextView2.setText("FIGHTER V2RAY");
        navheaderTextView2.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.BLACK);
        */
        MenuItem checkbox = drawerNavigationView.getMenu().findItem(R.id.item01).setActionView(new CheckBox(MainActivity.this));
        pingbox = (CheckBox) checkbox.getActionView();
        pingbox.setChecked(getPref().getBoolean("isAutoPinger", false));
        pingbox.setOnCheckedChangeListener((p1, isChecked) -> {
            if (!hLogStatus.isTunnelActive()) {
                getEditor().putBoolean("isAutoPinger", isChecked).apply();
            } else {
                pingbox.setChecked(getPref().getBoolean("isAutoPinger", false));
            }
        });
        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Scale and shift effect
                float moveFactor = (drawerView.getWidth() * slideOffset);

                // Get the main content view
                View contentView = findViewById(R.id.main_content);
                if (contentView != null) {
                    // Set pivot to the left-center so it scales towards the drawer
                    contentView.setPivotX(0f);
                    contentView.setPivotY(contentView.getHeight() / 2.0f);

                    contentView.setTranslationX(moveFactor);

                    // Shrink the home content significantly (to 70% of its size)
                    float scaleFactor = 1 - (slideOffset * 0.3f);
                    contentView.setScaleX(scaleFactor);
                    contentView.setScaleY(scaleFactor);
                }
            }

            @Override
            public void onDrawerOpened(View view) {
                try {
                    if (mDrawerMenu.getRotation() == 0) {
                        mDrawerMenu.animate().setDuration(200).rotation(180);
                    } else {
                        mDrawerMenu.animate().setDuration(200).rotation(0);
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                try {
                    if (mDrawerMenu.getRotation() == 0) {
                        mDrawerMenu.animate().setDuration(200).rotation(180);
                    } else {
                        mDrawerMenu.animate().setDuration(200).rotation(0);
                    }
                } catch (Exception ignored) {
                }
            }
        });
        drawerNavigationView.setNavigationItemSelectedListener(this);
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public void close() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
        }
    }

    public void open() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        close(); // only called once here

        if (id == R.id.item0) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));

        } else if (id == R.id.item01) {
            if (!hLogStatus.isTunnelActive()) {
                pingDislog();
            }

        } else if (id == R.id.item02) {
            mReleaseNotes();

        } else if (id == R.id.item05) {
            mIphunt();

        } else if (id == R.id.item06) {
            mUpdate();

        } else if (id == R.id.logout) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();

        } else if (id == R.id.item07) {
            new ColorChooserFragment()
                    .preselect(getConfig().getColorAccent())
                    .show(getSupportFragmentManager(), "ColorChooserFragment");

        } else if (id == R.id.item08) {
            openRadioInfo();
        } else if (id == R.id.item09) {
            startActivity(new Intent(MainActivity.this, MainActivityWifi.class));

        } else if (id == R.id.item10) {
            startActivity(new Intent(MainActivity.this, dnsActivity.class));

        } else if (id == R.id.item11) {
            if (!hLogStatus.isTunnelActive())
                startActivity(new Intent(MainActivity.this, harliesAppManager.class));

        } else if (id == R.id.item_battery_optimizer) {
            openBatteryOptimizationSettings();
        }

        return true;
    }

    private void openRadioInfo() {
        Intent in = new Intent(Intent.ACTION_MAIN);
        in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            in.setClassName("com.android.settings", "com.android.settings.RadioInfo");
            startActivity(in);
        } catch (Exception ex) {
            try {
                in.setClassName("com.android.phone", "com.android.phone.settings.RadioInfo");
                startActivity(in);
            } catch (Exception e) {
                showToast(resString(R.string.app_name), "Function not supported by your device");
            }
        }
    }


    public void mTelegram() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.link/nnypmi"));
            startActivity(Intent.createChooser(intent, "launch Whatsapp"));
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Please download the Whatsapp!", Toast.LENGTH_LONG).show();
        }
    }

    private void pingDislog() {
        View inflate = LayoutInflater.from(MainActivity.this).inflate(R.layout.ping_dialog, null);
        final AlertDialog clipBuilder = new AlertDialog.Builder(MainActivity.this).create();
        TextInputLayout destination = inflate.findViewById(R.id.destination);
        destination.setBoxStrokeColor(getConfig().getColorAccent());
        final EditText ed_destination = inflate.findViewById(R.id.ed_destination);
        ed_destination.setTextColor(getConfig().gettextColor());
        TextInputLayout timeout = inflate.findViewById(R.id.timeout);
        timeout.setBoxStrokeColor(getConfig().getColorAccent());
        final EditText ed_timeout = inflate.findViewById(R.id.ed_timeout);
        ed_timeout.setTextColor(getConfig().gettextColor());
        TextInputLayout thread = inflate.findViewById(R.id.thread);
        thread.setBoxStrokeColor(getConfig().getColorAccent());
        final EditText ed_thread = inflate.findViewById(R.id.ed_thread);
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        RelativeLayout save = inflate.findViewById(R.id.save);
        ((TextView) inflate.findViewById(R.id.appButton1)).setTextColor(getConfig().getColorAccent());
        ((TextView) inflate.findViewById(R.id.notiftext1)).setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        ((TextView) inflate.findViewById(R.id.savetv)).setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        ed_destination.setText(getPref().getString("ping_destination", "www.google.com"));
        ed_timeout.setText(getPref().getString("ping_timeout", "10"));
        ed_thread.setText(String.valueOf(getConfig().getPingThread()));
        save.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        save.setOnClickListener(p1 -> {
            String p = ed_thread.getText().toString().trim();
            pingbox.setChecked(true);
            getEditor().putBoolean("isAutoPinger", true).apply();
            getEditor().putString("ping_destination", ed_destination.getText().toString().trim()).apply();
            getEditor().putString("ping_timeout", ed_timeout.getText().toString().trim()).apply();
            getConfig().setPingThread(Integer.parseInt(p.isEmpty() ? "3" : p));
            clipBuilder.dismiss();
        });
        inflate.findViewById(R.id.appButton0).setOnClickListener(p1 -> {
            pingbox.setChecked(false);
            getEditor().putBoolean("isAutoPinger", false).apply();
            clipBuilder.dismiss();
        });
        clipBuilder.setView(inflate);
        clipBuilder.setCancelable(false);
        clipBuilder.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        clipBuilder.show();
    }

    private void mReleaseNotes() {
        View inflate = LayoutInflater.from(MainActivity.this).inflate(R.layout.notif2, null);
        final AlertDialog cBuiler = new AlertDialog.Builder(MainActivity.this).create();
        TextView title = inflate.findViewById(R.id.notiftext1);
        final TextView ms = inflate.findViewById(R.id.confimsg);
        TextView cancel = inflate.findViewById(R.id.appButton2txt);
        RelativeLayout btn = inflate.findViewById(R.id.appButton2);
        inflate.findViewById(R.id.color_bg).setBackgroundColor(getConfig().getColorAccent());
        btn.setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        ms.setTextColor(getConfig().gettextColor());
        cancel.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        title.setTextColor(getConfig().getAppThemeUtil() ? Color.BLACK : Color.WHITE);
        title.setText("Release Note");
        cancel.setText("OKA'Y");
        inflate.findViewById(R.id.appButton1).setVisibility(View.GONE);
        btn.setOnClickListener(p1 -> {
            cBuiler.dismiss();
        });
        String message = getPref().getString(RELEASE_NOTE, "");
        new Thread(() -> {
            for (i4 = 0; i4 < message.length(); i4++) {
                mHandler.post(() -> {
                    try {
                        ms.setText(message.substring(0, i4 + 1));
                    } catch (Exception ignored) {
                    }
                });
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
        cBuiler.setView(inflate);
        cBuiler.setCancelable(false);
        cBuiler.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
        cBuiler.show();
    }

    private void dataAuthetication() {
        String api = new appUtil().x_api;
        String user = getPref().getString("_screenUsername_key", "");
        String pass = getPref().getString("_screenPassword_key", "");
        if (user.isEmpty() || pass.isEmpty()) {
            return;
        }

        String model = Build.MODEL;
        @SuppressLint("HardwareIds") String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String jsonUrl = api + "?username=" + user + "&password=" + pass + "&device_id=" + id + "&device_model=" + model;

        // Use the global warm RequestQueue for much faster networking
        StringRequest req = new StringRequest(
                jsonUrl,
                response -> {
                    Log.d("here", response);
                    try {
                        JSONObject js = new JSONObject(response);

                        // ✅ Save username locally (SharedPreferences)
                        getEditor().putString("_screenUsername_key", user).apply();

                        if (js.getString("device_match").equals("none")) {
                            onAuthFailed("Authentication Failed");
                            if (dex002.isVPNRunning()) stopTunnelService();
                            return;
                        }
                        if (js.getString("device_match").equals("false")) {
                            showDeviceIdNotMatch();
                            return;
                        }
                        onExpireDate(js.getString("expiry"));
                    } catch (Exception ignored) {
                    }
                }, error -> onError("Expire Date: "+ error.getMessage()));

        MainApplication.getRequestQueue().add(req);
    }

    @Override
    public void onExpireDate(String expiry) {
        if (expiry == null || expiry.equals("none")) {
            ac_xp.setText("Expiry: none");
            getEditor().putString("_AccountXp", "Expiry: none").apply();
            getEditor().putString("_AccountRawXp", "none").apply();
            return;
        }

        String formattedDate = util.getExpireDateFormatted(expiry);
        String daysLeft = util.getDaysLeft(expiry);

        date = "Expiry: " + formattedDate + " | " + daysLeft;
        getEditor().putString("_AccountXp", date).apply();
        getEditor().putString("_AccountRawXp", expiry).apply();
        ac_xp.setText(date);

        if (daysLeft.equals("Expired")) {
            if (hLogStatus.isTunnelActive()) stopTunnelService();
        }

        shouldFetchAccountDetails = false;
    }

    void showDeviceIdNotMatch() {
        {
            if (dex002.isVPNRunning()) stopTunnelService();
            util.showToast(resString(R.string.app_name), "Account is used in another device!!");
        }
    }

    @Override
    public void onDeviceNotMatch(String s) {
        getEditor().putString("_AccountXp", "This account using other device!").apply();
        ac_xp.setText("This account using other device!");
        if (dex002.isVPNRunning()) stopTunnelService();
    }

    private String getDaysLeft(String expiryDate) {
        return util.getDaysLeft(expiryDate);
    }

    @SuppressLint("SimpleDateFormat")
    private String getExpireDate(String date) {
        return util.getExpireDateFormatted(date);
    }

    @Override
    public void onAuthFailed(String authenticationFailed) {
        getEditor().putString("_AccountXp", resString(R.string.state_auth_failed)).apply();
        ac_xp.setText(resString(R.string.state_auth_failed));
        if (dex002.isVPNRunning()) stopTunnelService();
    }

    @Override
    public void onError(String error) {
        System.out.println(error);
    }

    private void toggleAutoConnect() {
        boolean isAutoConnectEnabled = getPref().getBoolean("auto_connect_enabled", false);

        if (isAutoConnectEnabled) {
            // Disable auto connect
            getEditor().putBoolean("auto_connect_enabled", false).apply();
            showToast("Auto Connect", "Auto Connect Disabled");
        } else {
            // Enable auto connect
            getEditor().putBoolean("auto_connect_enabled", true).apply();
            showToast("Auto Connect", "Auto Connect Enabled");

            // If not connected, connect now
            if (!hLogStatus.isTunnelActive()) {
                new Handler().postDelayed(() -> {
                    if (!hLogStatus.isTunnelActive()) {
                        startTunnelService();
                    }
                }, 1000);
            }
        }
    }

    private void toggleAutoReconnect() {
        boolean isAutoReconnectEnabled = getPref().getBoolean("auto_reconnect_enabled", false);

        if (isAutoReconnectEnabled) {
            // Disable auto reconnect
            getEditor().putBoolean("auto_reconnect_enabled", false).apply();
            showToast("Auto Reconnect", "Auto Reconnect Disabled");
        } else {
            // Enable auto reconnect
            getEditor().putBoolean("auto_reconnect_enabled", true).apply();
            showToast("Auto Reconnect", "Auto Reconnect Enabled - Will reconnect if connection drops");
        }
    }

    private void openBatteryOptimizationSettings() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);

            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                // App is being optimized, show dialog to disable it
                showBatteryOptimizationDialog();
            } else {
                // App is already excluded from battery optimization
                showToast("Battery Optimizer", "App is already excluded from battery optimization");
                // Still open settings so user can verify or change
                try {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    showToast("Error", "Could not open battery settings");
                }
            }
        } else {
            showToast("Battery Optimizer", "Battery optimization not available on this Android version");
        }
    }

    private void showBatteryOptimizationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Battery Optimization")
            .setMessage("To ensure VPN stays connected in background, please disable battery optimization for this app.\n\nThis will:\n• Keep VPN running reliably\n• Enable auto-reconnect to work properly\n• Prevent Android from killing the VPN service\n\nWould you like to disable battery optimization now?")
            .setPositiveButton("Disable Optimization", (d, which) -> {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    showToast("Error", "Could not open battery optimization settings");
                }
            })
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Open Settings", (d, which) -> {
                try {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    showToast("Error", "Could not open battery settings");
                }
            })
            .create();

        dialog.show();

        // Style the buttons with proper colors
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getConfig().getColorAccent());
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getConfig().getColorAccent());
        }
    }

    private void showClearCacheDialog() {
        // Calculate cache and data size
        long cacheSize = getCacheSize();
        long dataSize = getDataSize();
        long totalSize = cacheSize + dataSize;
        String cacheSizeText = formatFileSize(cacheSize);
        String dataSizeText = formatFileSize(dataSize);
        String totalSizeText = formatFileSize(totalSize);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Clear App Data & Cache")
            .setMessage("Current usage:\n• Cache: " + cacheSizeText + "\n• Data: " + dataSizeText + "\n• Total: " + totalSizeText + "\n\n⚠️ Clear All will:\n• Delete ALL app data\n• Remove all settings\n• Clear all configs\n• Reset app to fresh state\n• You'll need to login again\n\nChoose what to clear:")
            .setPositiveButton("Clear All Data", (d, which) -> {
                confirmClearAllData();
            })
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Cache Only", (d, which) -> {
                clearAppCache();
            })
            .create();

        dialog.show();

        // Style the buttons with proper colors
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getConfig().getColorAccent());
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getConfig().getColorAccent());
        }
    }

    private void confirmClearAllData() {
        AlertDialog confirmDialog = new AlertDialog.Builder(this)
            .setTitle("⚠️ Confirm Clear All Data")
            .setMessage("This will permanently delete:\n• All app settings\n• All configurations\n• All saved data\n• All databases\n\nThe app will restart and you'll need to login again.\n\nAre you absolutely sure?")
            .setPositiveButton("Yes, Clear Everything", (d, which) -> {
                clearAllAppData();
            })
            .setNegativeButton("Cancel", null)
            .create();

        confirmDialog.show();

        // Style the buttons
        if (confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
        }
        if (confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getConfig().getColorAccent());
        }
    }

    private long getCacheSize() {
        long size = 0;
        try {
            File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                size = getDirSize(cacheDir);
            }
            File externalCacheDir = getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                size += getDirSize(externalCacheDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    private long getDataSize() {
        long size = 0;
        try {
            File dataDir = getFilesDir();
            if (dataDir != null && dataDir.exists()) {
                size = getDirSize(dataDir);
            }
            File externalFilesDir = getExternalFilesDir(null);
            if (externalFilesDir != null && externalFilesDir.exists()) {
                size += getDirSize(externalFilesDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    private long getDirSize(File dir) {
        long size = 0;
        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirSize(file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void clearAppCache() {
        try {
            File cacheDir = getCacheDir();
            File externalCacheDir = getExternalCacheDir();

            long clearedSize = 0;
            int filesDeleted = 0;

            // Clear internal cache
            if (cacheDir != null && cacheDir.exists()) {
                clearedSize += getDirSize(cacheDir);
                filesDeleted += deleteDirContents(cacheDir);
            }

            // Clear external cache
            if (externalCacheDir != null && externalCacheDir.exists()) {
                clearedSize += getDirSize(externalCacheDir);
                filesDeleted += deleteDirContents(externalCacheDir);
            }

            String clearedSizeText = formatFileSize(clearedSize);
            showToast("Cache Cleared", "Cleared " + clearedSizeText + " (" + filesDeleted + " files)");

        } catch (Exception e) {
            showToast("Error", "Failed to clear cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearAppCacheAndData() {
        try {
            File cacheDir = getCacheDir();
            File externalCacheDir = getExternalCacheDir();
            File dataDir = getFilesDir();
            File externalFilesDir = getExternalFilesDir(null);

            long clearedSize = 0;
            int filesDeleted = 0;

            // Clear internal cache
            if (cacheDir != null && cacheDir.exists()) {
                clearedSize += getDirSize(cacheDir);
                filesDeleted += deleteDirContents(cacheDir);
            }

            // Clear external cache
            if (externalCacheDir != null && externalCacheDir.exists()) {
                clearedSize += getDirSize(externalCacheDir);
                filesDeleted += deleteDirContents(externalCacheDir);
            }

            // Clear internal data (excluding databases and shared_prefs)
            if (dataDir != null && dataDir.exists()) {
                File[] files = dataDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        // Skip databases and shared_prefs to preserve settings
                        if (!file.getName().equals("databases") &&
                            !file.getName().equals("shared_prefs")) {
                            clearedSize += getDirSize(file);
                            if (file.isDirectory()) {
                                filesDeleted += deleteDirContents(file);
                                if (file.delete()) filesDeleted++;
                            } else {
                                if (file.delete()) filesDeleted++;
                            }
                        }
                    }
                }
            }

            // Clear external data
            if (externalFilesDir != null && externalFilesDir.exists()) {
                clearedSize += getDirSize(externalFilesDir);
                filesDeleted += deleteDirContents(externalFilesDir);
            }

            String clearedSizeText = formatFileSize(clearedSize);
            showToast("Data & Cache Cleared", "Cleared " + clearedSizeText + " (" + filesDeleted + " files)");

        } catch (Exception e) {
            showToast("Error", "Failed to clear data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearAllAppData() {
        try {
            File cacheDir = getCacheDir();
            File externalCacheDir = getExternalCacheDir();
            File dataDir = getFilesDir().getParentFile(); // Get app data root directory

            long clearedSize = 0;
            int filesDeleted = 0;

            // Clear everything in app data directory
            if (dataDir != null && dataDir.exists()) {
                File[] files = dataDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        clearedSize += getDirSize(file);
                        if (file.isDirectory()) {
                            filesDeleted += deleteDirContents(file);
                            if (file.delete()) filesDeleted++;
                        } else {
                            if (file.delete()) filesDeleted++;
                        }
                    }
                }
            }

            String clearedSizeText = formatFileSize(clearedSize);
            showToast("All Data Cleared", "Cleared " + clearedSizeText + " - App will restart");

            // Restart app after clearing all data
            new Handler().postDelayed(() -> {
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                finish();
                System.exit(0);
            }, 2000);

        } catch (Exception e) {
            showToast("Error", "Failed to clear all data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int deleteDirContents(File dir) {
        int count = 0;
        try {
            if (dir != null && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            count += deleteDirContents(file);
                            if (file.delete()) {
                                count++;
                            }
                        } else {
                            if (file.delete()) {
                                count++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

//-----------------------Auto app update notification
    private void checkAppUpdate() {

        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/asadul-web/SSMen-Release/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                String latestTag = jsonObject.getString("tag_name"); // v1.0.2

                String latestVersion = latestTag.replace("v", "");
                String currentVersion;
                try {
                    currentVersion = getPackageManager()
                            .getPackageInfo(getPackageName(), 0)
                            .versionName;
                } catch (Exception e) {
                    currentVersion = "0";
                }

                if (isNewerVersion(latestVersion, currentVersion)) {
                    runOnUiThread(() -> showUpdateDialog(latestVersion));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}



