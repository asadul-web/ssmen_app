package mtkdex.core.build.ssmen.wifi;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.github.angads25.toggle.widget.LabeledSwitch;
import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.activities.MainBaseActivity;
import mtkdex.core.build.ssmen.config.ConfigUtil;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivityWifi extends MainBaseActivity {
    private TextView tv_status, tv_address, tv_port, timers;
    private LabeledSwitch labeledSwitch;

    public ConfigUtil mConfig;
    private final int port = Integer.parseInt("8080");
    private SharedPreferences myPrefs;
    private SharedPreferences.Editor editor;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.fragment_tethering);
        myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = myPrefs.edit();

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        mConfig = ConfigUtil.getInstance(this);
        // Creating  an Ad Request


        timers = findViewById(R.id.tv_timer);
        tv_status = findViewById(R.id.tv_status);
        tv_address = findViewById(R.id.tv_address);
        tv_port = findViewById(R.id.tv_port);
        tv_address = findViewById(R.id.tv_address);
        labeledSwitch = findViewById(R.id.switch_tether);
        boolean hotspot = mConfig.getEnableHotspot();
        if (hotspot) {
            Intent inters = new Intent(MainActivityWifi.this, mtkdex.core.build.ssmen.wifi.ProxyService.class);
            inters.putExtra("h_port", port);
            myPrefs.edit().putString("h_port", tv_port.getText().toString()).apply();
            startService(inters);
            tv_address.setVisibility(View.VISIBLE);
            tv_port.setVisibility(View.VISIBLE);
            tv_status.setText(getString(R.string.proxy_is_running));
            tv_address.setText("Address: " + getIPAddress(true));
            tv_port.setText("Port: " + port);
            labeledSwitch.setOn(true);
        } else {
            tv_port.setText("");
            stopService(new Intent(MainActivityWifi.this, ProxyService.class));
            tv_status.setText(getString(R.string.proxy_stopped));
            tv_address.setText("");
            mConfig.setEnableHotspot(false);
        }
        labeledSwitch.setOnToggledListener((buttonView, isChecked) -> {

            String ip = getIPAddress(true);
            if (!ip.trim().startsWith("192.")) {
                openTetheringSettings();
                labeledSwitch.setOn(false);
            }
            if (isChecked) {
                Intent inters = new Intent(MainActivityWifi.this, ProxyService.class);
                inters.putExtra("h_port", port);
                myPrefs.edit().putString("h_port", tv_port.getText().toString()).apply();
                startService(inters);
                tv_address.setVisibility(View.VISIBLE);
                tv_port.setVisibility(View.VISIBLE);
                tv_status.setText(getString(R.string.proxy_is_running));
                tv_address.setText("Address: " + getIPAddress(true));
                tv_port.setText("Port: " + port);
                labeledSwitch.setOn(true);
                mConfig.setEnableHotspot(true);


            } else {
                tv_port.setText("");
                stopService(new Intent(MainActivityWifi.this, ProxyService.class));
                tv_status.setText(getString(R.string.proxy_stopped));
                tv_address.setText("");
                mConfig.setEnableHotspot(false);
            }


        });

    }

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_share) {
            launchMarket();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void launchMarket() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
        }
    }

    public String getIPAddress(boolean useIPv4) {
        try {
            boolean isIPv4 = true;
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null) {
                            isIPv4 = sAddr.indexOf(':') < 0;
                        }

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } // for now eat exceptions
        return "";
    }

    private void openTetheringSettings() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
        this.startActivity(intent);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

}
