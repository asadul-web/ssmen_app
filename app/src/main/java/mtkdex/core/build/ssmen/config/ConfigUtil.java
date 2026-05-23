package mtkdex.core.build.ssmen.config;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.annotation.NonNull;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.v2ray.ang.MainApplication;
import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import io.michaelrocks.paranoid.Obfuscate;
import mtkdex.core.build.ssmen.utils.c_01;

@Obfuscate
@SuppressLint("StaticFieldLeak")
public class ConfigUtil implements SettingsConstants {

    private static ConfigUtil instance;
    private static SharedPreferences pPref;
    private static SharedPreferences.Editor pEditor;
    private static SharedPreferences dPref;
    private static SharedPreferences.Editor dEditor;

    private static Context mContext;
    public static String hide(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    public ConfigUtil(Context context) {
        mContext = context;
        pPref = MainApplication.getPrivateSharedPreferences();
        dPref = MainApplication.getDefaultSharedPreferences();
        pEditor = pPref.edit();
        dEditor = dPref.edit();
        connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public String getSecureString(String key){
        if(key.equals(SERVER_KEY)){
            return getQueryHost().split(":")[0];
        }
        if(key.equals(USERNAME_KEY)){
            return c_01.decrypt(pPref.getString(USERNAME_KEY, ""));
        }
        if(key.equals(PASSWORD_KEY)){
            return c_01.decrypt(pPref.getString(PASSWORD_KEY, ""));
        }
        if(key.equals(CUSTOM_PAYLOAD_KEY)){
            if (getAutoReplace()){
                return c_01.decrypt(pPref.getString(CUSTOM_PAYLOAD_KEY, "")).replace("[mediatek]",c_01.decrypt(pPref.getString(PROXY_IP_KEY, ""))).replace("[host]",c_01.decrypt(pPref.getString(PROXY_IP_KEY, "")));
            }
            return c_01.decrypt(pPref.getString(CUSTOM_PAYLOAD_KEY, "")).replace("[mediatek]",getQueryHost().split(":")[0]).replace("[host]",getQueryHost().split(":")[0]);
        }
        if(key.equals(SNI_HOST_KEY)){
            return c_01.decrypt(pPref.getString(SNI_HOST_KEY, "")).replace("[mediatek]",getQueryHost().split(":")[0]).replace("[host]",getQueryHost().split(":")[0]);
        }
        if(key.equals(PROXY_IP_KEY)){
            return (getAutoReplace())? getQueryHost().split(":")[0] : c_01.decrypt(pPref.getString(PROXY_IP_KEY, ""));
        }
        if(key.equals(DNS_NAME_SERVER_KEY)){
            return c_01.decrypt(pPref.getString(DNS_NAME_SERVER_KEY, ""));
        }
        if(key.equals(DNS_ADDRESS_KEY)){
            return c_01.decrypt(pPref.getString(DNS_ADDRESS_KEY, ""));
        }
        if(key.equals(FRONT_QUERY)){
            return c_01.decrypt(pPref.getString(FRONT_QUERY, ""));
        }
        if(key.equals(BACK_QUERY)){
            return c_01.decrypt(pPref.getString(BACK_QUERY, ""));
        }
        if(key.equals(DNS_PUBLIC_KEY)){
            String publickey = c_01.decrypt(pPref.getString(DNS_PUBLIC_KEY, ""));
            if(publickey.contains("hrKlev65")){
                return publickey.replace("hrKlev65","");
            }
            return publickey;
        }

        if(key.equals(CONFIG_V2RAY)){
            String mV2ray = pPref.getString(CONFIG_V2RAY, "");
//            if(mV2ray.contains("JHsx382oL")){
//                return mV2ray.replace("JHsx382oL","");
//            }
            return mV2ray;
        }

        if(key.equals(CONFIG_V2RAY_ID)){
            String mV2ray = pPref.getString(CONFIG_V2RAY_ID, "");
//            if(mV2ray.contains("JHsx382oL")){
//                return mV2ray.replace("JHsx382oL","");
//            }
            return mV2ray;
        }

        if(key.equals(SERVER_PORT_KEY)){
            if (sPort[pPref.getInt(CUSTOM_SERVER_POR_KEY,0)].equals("Default")){
                String[] hostParts = getQueryHost().split(":");
                // Add check to prevent ArrayIndexOutOfBoundsException
                if (hostParts.length > 1) {
                    return hostParts[1];
                } else {
                    // Default port if not found in the host string
                    return "1194";
                }
            }
            return sPort[pPref.getInt(CUSTOM_SERVER_POR_KEY,0)];
        }
        if(key.equals(PROXY_PORT_KEY)){
            if (pPort[pPref.getInt(CUSTOM_NETWORK_PORT_KEY,0)].equals("Default")){
                return pPref.getString(PROXY_PORT_KEY, "80");
            }
            return pPort[pPref.getInt(CUSTOM_NETWORK_PORT_KEY,0)];
        }
        return pPref.getString(key, "");
    }


    public static ConfigUtil getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigUtil(context);
        }
        mContext = context;
        return instance;
    }

    public String getQueryHost(){
        if (isQueryMode()){
            if (!c_01.decrypt(pPref.getString(BACK_QUERY, "")).isEmpty()) {
                return c_01.decrypt(pPref.getString(SERVER_KEY, ""))+"@"+c_01.decrypt(pPref.getString(BACK_QUERY, ""))+":"+"80";
            } else if (!c_01.decrypt(pPref.getString(FRONT_QUERY, "")).isEmpty()) {
                return c_01.decrypt(pPref.getString(FRONT_QUERY, ""))+"@"+c_01.decrypt(pPref.getString(SERVER_KEY, ""))+":"+"80";
            }
        }
        return c_01.decrypt(pPref.getString(SERVER_KEY, ""))+":"+pPref.getString(SERVER_PORT_KEY, "1194");
    }



    public String getProxySetups(){
        if (getOvpnCert().contains("http-proxy-option")){
            return "";
        }
        String tweak = getSecureString(CUSTOM_PAYLOAD_KEY);
        String[] proxyAddress = getProxyAddress().split(":");
        boolean def = tweak.contains("http-proxy-option");
        StringBuilder config = new StringBuilder();
        String a1 = new String(new byte[]{34,});
        if (def){
            config.append(String.format("\nhttp-proxy %s %s", getSecureString(PROXY_IP_KEY),getSecureString(PROXY_PORT_KEY)));
            config.append("\n");
            config.append(tweak);
            config.append("\n");
        }else {
            config.append(String.format("\nhttp-proxy %s %s", proxyAddress[0],proxyAddress[1]));
            config.append("\n");
        }
        return config.toString();
    }


    public boolean mUseProxy(){
        if (getPayloadType()==PAYLOAD_TYPE_DIRECT){
            return false;
        }
        if (getPayloadType()==PAYLOAD_TYPE_OVPN_UDP){
            return false;
        }
        if (getOvpnCert().contains("http-proxy-option")){
            return false;
        }
        if (getPayloadType()==PAYLOAD_TYPE_SSL){
            return true;
        }
        boolean useInjector = !getSecureString(CUSTOM_PAYLOAD_KEY).contains("http-proxy-option");
        if (getPayloadType()==PAYLOAD_TYPE_SSL_PAYLOAD){
            return useInjector;
        }
        if (getPayloadType()==PAYLOAD_TYPE_SSL_PROXY){
            return useInjector;
        }
        return useInjector;
    }

    public boolean getEnableHotspot() {
        return dPref.getBoolean("EnableHotspot", false);
    }

    public void setEnableHotspot(boolean enable) {
        dEditor.putBoolean("EnableHotspot", enable).apply();
    }
    public void setFrontQuery(String query)
    {
        pEditor.putString(FRONT_QUERY, query).apply();
    }
    public void setBackQuery(String query)
    {
        pEditor.putString(BACK_QUERY, query).apply();
    }

    public void setUDPConfig(String obfs){
        pEditor.putString(DIRECT_UDP_CONFIG_KEY, obfs).apply();
    }
    public String getUDPConfig(){
        String udp = c_01.decrypt(pPref.getString(DIRECT_UDP_CONFIG_KEY,""));
        if (udp.contains("[user]")){
            return udp.replace("[user]",getSecureString(USERNAME_KEY)+":"+getSecureString(PASSWORD_KEY));
        }else{
            try {
                JSONObject js = new JSONObject(udp);
                String mObfs = js.getString("obfs");
                String ServerPort = "[host]:20000-50000";
                String mUser = getSecureString(USERNAME_KEY)+":"+getSecureString(PASSWORD_KEY);
                int up = js.getInt("up_mbps");
                int dw = js.getInt("down_mbps");
                int rt = js.getInt("retry");
                int rt_in = js.getInt("retry_interval");
                String mSocks5 =  js.getJSONObject("socks5").getString("listen");
                String mHttp =  js.getJSONObject("http").getString("listen");
                boolean mInsecure =  js.getBoolean("insecure");
                String mCa =  js.getString("ca");
                int mRecv_window_conn =  js.getInt("recv_window_conn");
                int mRecv_window =  js.getInt("recv_window");
                return String.format(UDP_CLI, ServerPort, mObfs, mUser, up, dw, rt, rt_in, mSocks5, mHttp, mInsecure, mCa, mRecv_window_conn, mRecv_window);
            } catch (Exception e) {
                return udp;
            }
        }
    }
    public void setOvpnCert(String ca) {
        pEditor.putString("OVPN_CERT_KEY", ca).apply();
    }
    public String getOvpnCert(){
        String ca = c_01.decrypt(pPref.getString("OVPN_CERT_KEY",""));
        if (ca.contains("remote ")){
            return c_01.decrypt(pPref.getString("OVPN_CERT_KEY",""));
        }
        return c_01.decrypt(pPref.getString("OVPN_CERT_KEY", "")) + "\nremote " + getSecureString(SERVER_KEY) + " " + getSecureString(SERVER_PORT_KEY) + "\n";
    }
    public void setConfigV2ray(String v2){pEditor.putString(CONFIG_V2RAY, v2).apply();
    }


    public String geConfigV2ray() {
        return dPref.getString(CONFIG_V2RAY, "");
    }


    public void setSni(String sni){pEditor.putString(SNI_HOST_KEY, sni).apply();
    }

    public void setV2rayID(String id){pEditor.putString(CONFIG_V2RAY_ID, id).apply();
    }

    public void setPayload(String payload){pEditor.putString(CUSTOM_PAYLOAD_KEY, payload).apply();
    }

    public void setServerHost(String ip){
        pEditor.putString(SERVER_KEY, ip).apply();
    }
    public void setServerPort(String port){
        pEditor.putString(SERVER_PORT_KEY, port).apply();
    }

    public void setProxyHost(String proxy){
        pEditor.putString(PROXY_IP_KEY, proxy).apply();
    }
    public void setProxyPort(String proxyPort){
        pEditor.putString(PROXY_PORT_KEY, proxyPort).apply();
    }
    public void setUser(String str){
        pEditor.putString(USERNAME_KEY,str).apply();
    }

    public String getUser() {
        return dPref.getString(USERNAME_KEY, "");
    }
    public String getUserPass() {
        return dPref.getString(PASSWORD_KEY, "");
    }

    public void setUserPass(String str){
        pEditor.putString(PASSWORD_KEY,str).apply();
    }
    public void setDNSaddress(String dns){
        pEditor.putString(DNS_ADDRESS_KEY,dns).apply();
    }
    public void setDNSpublicKey(String key){
        pEditor.putString(DNS_PUBLIC_KEY,key).apply();
    }
    public void setDNSnameServer(String dnsName){
        pEditor.putString(DNS_NAME_SERVER_KEY,dnsName).apply();
    }
    public boolean hasAccount() {
        return pPref.getBoolean("hasAccount", false);
    }

    public void setHasAccount(boolean hasAccount) {
        pEditor.putBoolean("hasAccount", hasAccount).apply();
    }

    public void logout() {
        pEditor.putBoolean("hasAccount", false).apply();
        pEditor.putString(USERNAME_KEY, "").apply();
        pEditor.putString(PASSWORD_KEY, "").apply();
    }

    public String getSocketTYPE(){
        return dPref.getString(SERVER_SOCKET_TYPE_KEY, "cf");
    }
    public void setSocketTYPE(String str){
        dEditor.putString(SERVER_SOCKET_TYPE_KEY, str).apply();
    }
    public String getServerType(){
        return dPref.getString(SERVER_TYPE, SERVER_TYPE_OVPN);
    }
    public void setServerType(String str){
        dEditor.putString(SERVER_TYPE, str).apply();
    }
    public void setPaylodType(int type)
    {
        dEditor.putInt(PAYLOAD_TYPE_KEY, type).apply();
    }
    public int getPayloadType(){
        return dPref.getInt(PAYLOAD_TYPE_KEY,PAYLOAD_TYPE_DIRECT);
    }
    public void setPayloadName(String nm){
        dEditor.putString("OVPN_PAYLOAD_NAME", nm).apply();
    }
    public String getPayloadName(){
        return dPref.getString("OVPN_PAYLOAD_NAME","");
    }
    public void setServerName(String proto){
        dEditor.putString("OVPN_SERVER_NAME", proto).apply();
    }
    public String getServerName(){
        return dPref.getString("OVPN_SERVER_NAME","");
    }
    public boolean getAutoReplace(){
        if (getServerType().equals(SERVER_TYPE_OVPN) || getServerType().equals(SERVER_TYPE_SSH)){
            return dPref.getBoolean(ISAUTO_REPLACE,false);
        }
        return false;
    }
    public void setAutoReplace(boolean ar){
        dEditor.putBoolean(ISAUTO_REPLACE,ar).apply();
    }
    public boolean isQueryMode()
    {
        return dPref.getBoolean(ISQUERY_MODE, false);
    }
    public void setIsQueryMode(boolean enable)
    {
        dEditor.putBoolean(ISQUERY_MODE, enable).apply();
    }


    public boolean getConfigIsAutoLogIn(){
        return dPref.getBoolean(IS_AUTO_LOGIN, false);
    }
    public void setConfigIsAutoLogIn(boolean a){
        dEditor.putBoolean(IS_AUTO_LOGIN, a).apply();
    }
    public String getLocalPort(){
        return dPref.getString(PORTA_LOCAL_KEY, "1080");
    }
    public void setLocalPort(String str){
        dEditor.putString(PORTA_LOCAL_KEY,str).apply();
    }

    public boolean getAutoClearLog() {
        return dPref.getBoolean(AUTO_CLEAR_LOGS_KEY, true);
    }
    public void setAutoClearLog(boolean use) {
        dEditor.putBoolean(AUTO_CLEAR_LOGS_KEY, use).apply();
    }
    public boolean getIsFilterApps() {
        return dPref.getBoolean(FILTER_APPS, false);
    }
    public void setIsFilterApps(boolean use) {
        dEditor.putBoolean(FILTER_APPS, use).apply();
    }

    public boolean getIsFilterBypassMode() {
        return dPref.getBoolean(FILTER_BYPASS_MODE, false);
    }
    public void setIsFilterBypassMode(boolean use) {
        dEditor.putBoolean(FILTER_BYPASS_MODE, use).apply();
    }

    public Set<String> getPackageFilterApps() {
        return dPref.getStringSet(FILTER_APPS_LIST, new HashSet<>());
    }
    public void setPackageFilterApps(Set<String> list) {
        dEditor.putStringSet(FILTER_APPS_LIST, list).apply();
    }

    public boolean getIsTetheringSubnet() {
        return dPref.getBoolean(TETHERING_SUBNET, false);
    }

    public void setTetheringSubnet(boolean use) {
        dEditor.putBoolean(TETHERING_SUBNET, use).apply();
    }

    public boolean getIsDisabledDelaySSH() {
        return dPref.getBoolean(DISABLE_DELAY_KEY, true);
    }
    public void setDisabledDelaySSH(boolean use) {
        dEditor.putBoolean(DISABLE_DELAY_KEY, use).apply();
    }

    public boolean getCompression() {
        return dPref.getBoolean(DATA_COMPRESSION, true);
    }
    public void setCompression(boolean use) {
        dEditor.putBoolean(DATA_COMPRESSION, use).apply();
    }

    public boolean getVpnDnsForward(){
        return dPref.getBoolean(DNSFORWARD_KEY, false);
    }
    public void setVpnDnsForward(boolean use){
        dEditor.putBoolean(DNSFORWARD_KEY, use).apply();
    }

    public String[] getVpnDnsResolver(){
        String[] dns = dPref.getString(DNSRESOLVER_KEY, "1.1.1.1:9.9.9.9").split(":");
        if (Arrays.toString(dns).length()<=9){
            return new String[]{"1.1.1.1","9.9.9.9"};
        }
        return new String[]{dns[0],dns[1]};
    }
    public void setVpnDnsResolver(String dns) {
        if (dns.length()<=9) {
            dns =  "1.1.1.1:9.9.9.9";
        }
        dEditor.putString(DNSRESOLVER_KEY, dns).apply();
    }

    public boolean getVpnUdpForward(){
        return dPref.getBoolean(UDPFORWARD_KEY, true);
    }
    public void setVpnUdpForward(boolean use){
        dEditor.putBoolean(UDPFORWARD_KEY, use).apply();
    }

    public String getVpnUdpResolver(){
        return dPref.getString(UDPRESOLVER_KEY, "127.0.0.1:7300");
    }
    public void setVpnUdpResolver(String str) {
        dEditor.putString(UDPRESOLVER_KEY, str).apply();
    }

    public boolean getPowerSaver() {
        return dPref.getBoolean(PAUSE_VPN_ON_BLANKED_SCREEN_KEY, false);
    }
    public void setPowerSaver(boolean use) {
        dEditor.putBoolean(PAUSE_VPN_ON_BLANKED_SCREEN_KEY, use).apply();
    }
    public boolean getIsScreenOn() {
        return dPref.getBoolean("IS_SCREEN_ON_KEY", true);
    }
    public void setIsScreenOn(boolean use) {
        dEditor.putBoolean("IS_SCREEN_ON_KEY", use).apply();
    }

    public void setPingServer(String png){
        dEditor.putString("_mPingServer", png).apply();
    }
    public String getPingServer(){
        return dPref.getString("_mPingServer", "clients3.google.com:443");
    }

    public void setProxyAddress(String adr){
        dEditor.putString(PREF_PROXY_ADDRESS_KEY, adr).apply();
    }
    public String getProxyAddress(){
        return dPref.getString(PREF_PROXY_ADDRESS_KEY, "127.0.0.1:8989");
    }

    public String getContactUrl(){
        String cUrl = pPref.getString(CONTACT_SUPPORT, "");
        if (cUrl.isEmpty()){
            return cUrl;
        }
        return cUrl.startsWith("http")? cUrl:"https://"+cUrl;
    }

    public int getReconnTime(){
        return dPref.getInt(AUTO_RECONN_TIME_KEY,5);
    }
    public void setReconnTime(int r){
        dEditor.putInt(AUTO_RECONN_TIME_KEY,r).apply();
    }

    public int getPingThread() {
        return dPref.getInt(PINGER_KEY, 3);
    }
    public void setPingThread(int p) {
        dEditor.putInt(PINGER_KEY, p).apply();
    }

    public String getSSHKeypath() {
        return dPref.getString("KEYPATH_KEY", "");
    }

    private final ConnectivityManager connMgr;
    private static String lastStateMsg;
    public void networkStateChange(boolean showStatusRepetido) {
        String netstatestring;
        try {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo == null) {
                netstatestring = "not connected";
            } else {
                String subtype = networkInfo.getSubtypeName();
                if (subtype == null)
                    subtype = "";
                String extrainfo = networkInfo.getExtraInfo();
                if (extrainfo == null)
                    extrainfo = "";
                netstatestring = String.format("%2$s %4$s to %1$s %3$s", networkInfo.getTypeName(),networkInfo.getDetailedState(), extrainfo, subtype);
            }
        } catch (Exception e) {
            netstatestring = e.getMessage();
        }
        if (showStatusRepetido || !netstatestring.equals(lastStateMsg))
            hLogStatus.logInfo(netstatestring);
        lastStateMsg = netstatestring;
    }


    public void initializeStartingMsg(){
        hLogStatus.updateStateString(hLogStatus.VPN_STARTING, mContext.getResources().getString(R.string.state_starting));
        if (!getServerType().equals(SERVER_TYPE_OVPN)){
            hLogStatus.logInfo(String.format("<b>%s %s</b>", getServerType(),mContext.getResources().getString(R.string.state_starting)));
        }
    }


    private static Class<? extends Activity> mNotificationActivityClass;
    public static String render_bandwidth(long bytes, boolean mbit) {
        if(mbit)
            bytes = bytes *8;
        int unit = mbit ? 1000 : 1024;
        if (bytes < unit)
            return bytes + (mbit ? " bit" : " B");

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (mbit ? "kMGTPE" : "KMGTPE").charAt(exp-1) + ("");
        if(mbit)
            return String.format(Locale.getDefault(),"%.1f %sbit", bytes / Math.pow(unit, exp), pre);
        else
            return String.format(Locale.getDefault(),"%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static PendingIntent getPendingIntent(Context c) {
        PendingIntent contentPendingIntent = getContentIntent(c);
        return contentPendingIntent != null ? contentPendingIntent : getGraphPendingIntent(c);
    }

    public static void setNotificationActivityClass(Class<? extends Activity> activityClass) {
        mNotificationActivityClass = activityClass;
    }
    public static PendingIntent getContentIntent(Context c) {
        try {
            if (mNotificationActivityClass != null) {
                Intent intent = new Intent(c, mNotificationActivityClass);
                try {
                    String typeStart = Objects.requireNonNull(mNotificationActivityClass.getField("TYPE_START").get(null)).toString();
                    Integer typeFromNotify = Integer.parseInt(Objects.requireNonNull(mNotificationActivityClass.getField("TYPE_FROM_NOTIFY").get(null)).toString());
                    intent.putExtra(typeStart, typeFromNotify);
                } catch (Exception ignored) {
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_SINGLE_TOP);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
                }
                return PendingIntent.getActivity(c, 0, intent, flags);
            }
        } catch (Exception e) {
            hLogStatus.logDebug(c.getClass().getCanonicalName()+ " Build detail intent error: "+e.getMessage());
        }
        return null;
    }

    public static PendingIntent getGraphPendingIntent(Context c) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(c, "mtkdex.core.build.org.activities.MainActivity"));
        intent.putExtra("PAGE", "graph");
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent startLW = PendingIntent.getActivity(c, 0, intent, flags);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return startLW;
    }


    public int getColorAccent(){
        return dPref.getInt(APP_COLORS, mContext.getResources().getColor(R.color.colorPrimary));
    }
    public void setColorAccent(int color){
        dEditor.putInt(APP_COLORS,color).apply();
    }
    public int getAppThemeDark(){
        if (getAppThemeUtil()){
            return R.style.AppTheme_dark;
        }
        return R.style.AppTheme;
    }

    public int getAlertDialog(){
        if (getAppThemeUtil()){
            return R.style.AppAlertDialog_Dark;
        }
        return R.style.AppAlertDialog_Light;
    }
    public int getFullAlertDialog(){
        if (getAppThemeUtil()){
            return R.style.AppAlertDialog_full_Dark;
        }
        return R.style.AppAlertDialog_full_Light;
    }

    public void setAppThemeUtil(boolean is){
        dEditor.putBoolean(THEME_UTIL_KEY,is).apply();
    }
    public boolean getAppThemeUtil(){
        return dPref.getBoolean(THEME_UTIL_KEY,false);
    }
    public int gettextColor(){
        if (getAppThemeUtil()){
            return mContext.getResources().getColor(R.color.white);
        }
        return mContext.getResources().getColor(R.color.black_light);
    }
    public int getMainLayoutBG(){
        if (getAppThemeUtil()){
            return mContext.getResources().getColor(R.color.windowBackground_dark);
        }
        return mContext.getResources().getColor(R.color.windowBackground_light);
    }
    public int getProgLayoutBG(){
        if (getAppThemeUtil()){
            return mContext.getResources().getColor(R.color.prog_dark);
        }
        return mContext.getResources().getColor(R.color.prog_light);
    }

    public int getHintextColor(){
        if (getAppThemeUtil()){
            return mContext.getResources().getColor(R.color.hint_d);
        }
        return mContext.getResources().getColor(R.color.hint_l);
    }


    public boolean getVersionCompare(String NewVersion, String OldVersion) {
        String[] vals1 = NewVersion.split("\\.");
        String[] vals2 = OldVersion.split("\\.");
        int i = 0;
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff) > 0;
        }
        return Integer.signum(vals1.length - vals2.length) > 0;
    }

    public String gets1() {
        return dPref.getString("s1", "");
    }

    public void sets1(String config) {
        dEditor.putString("s1", config).apply();
    }

    public String getputv2() {
        return dPref.getString("putv2", "");
    }

    public void setputv2(String config) {
        dEditor.putString("putv2", config).apply();
    }


    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }

}
