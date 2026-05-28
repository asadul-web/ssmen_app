package mtkdex.core.build.ssmen.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Toast;
import android.view.View;
import android.widget.TextView;
import com.v2ray.ang.R;
import com.v2ray.ang.MainApplication;
import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import android.telephony.TelephonyManager;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
@SuppressLint("StaticFieldLeak")
public class util implements SettingsConstants {

    private static Context mContext;
    private static ConfigUtil mConfig;

    public static String x = new String(new byte[]{68,101,120,116,101,114,69,115,107,97,108,97,114,116,101,50,48,50,52});

    public util(Context c)
    {
        mContext = c;
        mConfig = ConfigUtil.getInstance(c);
    }
    private static Context mContext(){
        if(mContext==null){
            return MainApplication.getApp();
        }
        return mContext;
    }

    public static String getDaysLeft(String expiryDate) {
        if (expiryDate == null || expiryDate.equals("none") || expiryDate.isEmpty()) {
            return "none";
        }
        try {
            // Most panels use UTC for expiry dates
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            
            Date expiry;
            try {
                expiry = format.parse(expiryDate);
            } catch (ParseException e) {
                format = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                expiry = format.parse(expiryDate);
            }

            if (expiry == null) {
                return "0 days";
            }

            long now = System.currentTimeMillis();
            long diff = expiry.getTime() - now;

            if (diff <= 0) {
                return "Expired";
            }

            if (diff >= 86400000) { // 1 day or more
                int d = (int) Math.ceil(diff / 86400000.0);
                return d + (d == 1 ? " day" : " days");
            } else if (diff >= 3600000) { // 1 hour or more
                int h = (int) Math.ceil(diff / 3600000.0);
                return h + (h == 1 ? " hour" : " hours");
            } else { // Less than 1 hour
                int m = (int) Math.ceil(diff / 60000.0);
                return m + (m == 1 ? " minute" : " minutes");
            }
        } catch (Exception e) {
            return "0 days";
        }
    }

    public static String getExpireDateFormatted(String date) {
        if (date == null || date.equals("none") || date.isEmpty()) {
            return "-- ---, ----";
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            
            Date newDate;
            try {
                newDate = format.parse(date);
            } catch (ParseException e) {
                format = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                newDate = format.parse(date);
            }

            if (newDate == null) return date;

            // Display in local time for the user's convenience
            format = new SimpleDateFormat("dd MMM, yyyy", java.util.Locale.US);
            format.setTimeZone(java.util.TimeZone.getDefault());
            return format.format(newDate);
        } catch (Exception e) {
            return date;
        }
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

    public void overrideFont(String defaultFontNameToOverride, String customFontFileNameInAssets) {
        // Method disabled as per branding guidelines to use system "serif" and "sans-serif"
        /*
        try {
            final Typeface customFontTypeface = Typeface.createFromAsset(mContext().getAssets(), customFontFileNameInAssets);
            final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
            defaultFontTypefaceField.setAccessible(true);
            defaultFontTypefaceField.set(null, customFontTypeface);
        } catch (Exception e) {
            Toast.makeText(mContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        */
    }

    public static void showToast(String message){
        showToast("FIGHTER V2RAY", message);
    }

    public static void showToast(String title, String subtitle){
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast toast = new Toast(mContext());
                toast.setDuration(Toast.LENGTH_LONG);
                View custom_view = LayoutInflater.from(mContext()).inflate(R.layout.snackbar, null);
                
                TextView tv1 = custom_view.findViewById(R.id.itemtoastTv1);
                TextView tv2 = custom_view.findViewById(R.id.itemtoastTv2);
                if (tv1 != null) tv1.setText(title);
                if (tv2 != null) tv2.setText(subtitle);
                
                toast.setView(custom_view);
                toast.setGravity(Gravity.BOTTOM, 0, 150); // Set gravity like in the image
                toast.show();
            } catch (Exception e) {
                try {
                    Toast.makeText(mContext(), title + ": " + subtitle, Toast.LENGTH_LONG).show();
                } catch (Exception ignored) {}
            }
        });
    }

    public static boolean isMyApp(){
        return mContext().getPackageName().equals("site.asalo.ssmen");
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isAvailable() && info.isConnected();
    }

    public static String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager)mContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isConnected())
            return "No Internet"; // not connected
        if (info.getType() == ConnectivityManager.TYPE_WIFI)
            return "Wifi connection";
        if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            int networkType = info.getSubtype();

            if (networkType == TelephonyManager.NETWORK_TYPE_GPRS || networkType == TelephonyManager.NETWORK_TYPE_EDGE || networkType == TelephonyManager.NETWORK_TYPE_CDMA || networkType == TelephonyManager.NETWORK_TYPE_1xRTT || networkType == TelephonyManager.NETWORK_TYPE_IDEN || networkType == TelephonyManager.NETWORK_TYPE_GSM) {      // api<25: replace by 16
                return "Mobile Data/2G";
            } else if (networkType == TelephonyManager.NETWORK_TYPE_UMTS || networkType == TelephonyManager.NETWORK_TYPE_EVDO_0 || networkType == TelephonyManager.NETWORK_TYPE_EVDO_A || networkType == TelephonyManager.NETWORK_TYPE_HSDPA || networkType == TelephonyManager.NETWORK_TYPE_HSUPA || networkType == TelephonyManager.NETWORK_TYPE_HSPA || networkType == TelephonyManager.NETWORK_TYPE_EVDO_B || networkType == TelephonyManager.NETWORK_TYPE_EHRPD || networkType == TelephonyManager.NETWORK_TYPE_HSPAP || networkType == TelephonyManager.NETWORK_TYPE_TD_SCDMA) { // api<25: replace by 17
                return "Mobile Date/3G";
            } else if (networkType == TelephonyManager.NETWORK_TYPE_LTE || networkType == TelephonyManager.NETWORK_TYPE_IWLAN || networkType == 19) { // LTE_CA
                return "Mobile Data/4G-LTE";
            } else if (networkType == TelephonyManager.NETWORK_TYPE_NR) {       // api<29: replace by 20
                return "Mobile Data/5G";
            }
            return "Unknowns network";
        }
        return "Unknowns network";
    }

    public static String pw_repl(String user, String pw) {
        return pw;
    }

    public static String b(Context context) {
        try {
            StringBuilder sb = new StringBuilder();
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature sign : info.signatures) {
                sb.append(sign.toCharsString());
            }
            return sb.toString();
        }catch(Exception e){
            return "";
        }
    }
}
   
