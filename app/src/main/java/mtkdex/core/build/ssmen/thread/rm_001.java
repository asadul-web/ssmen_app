package mtkdex.core.build.ssmen.thread;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;

import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import mtkdex.core.build.ssmen.core.vpnutils.CustomNativeLoader;
import mtkdex.core.build.ssmen.core.vpnutils.StreamGobbler;
import mtkdex.core.build.ssmen.core.vpnutils.VpnUtils;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import mtkdex.core.build.ssmen.service.dex002;
import mtkdex.core.build.ssmen.utils.util;


public class rm_001 extends Thread implements SettingsConstants {

    private static final String DNS_BIN = "libdns";
    private Process dnsProcess;
    private File filedns;
    private final Context context;
    private final ConfigUtil mConfig;

    public rm_001(Context context) {
        this.context = context;
        mConfig = ConfigUtil.getInstance(context);
    }

    @Override
    public final void run() {
        super.run();
        if (!dex002.isRunning) {
            interrupt();
            return;
        }
        try {
            hLogStatus.updateStateString(hLogStatus.VPN_CONNECTING, context.getString(R.string.state_connecting));
            addLogInfo("<b>DNS Tunnel: </b>" + context.getString(R.string.state_connecting));

            StringBuilder cmd1 = new StringBuilder();

            filedns = CustomNativeLoader.loadNativeBinary(context, DNS_BIN, new File(context.getFilesDir(), DNS_BIN));
            if (filedns == null) {
                interrupt();
                throw new IOException("<b>DNS Tunnel: </b> bin not found");
            }
            cmd1.append(filedns.getCanonicalPath());
            cmd1.append(" -udp ").append(this.mConfig.getSecureString(DNS_ADDRESS_KEY)).append(":53   -pubkey ").append(mConfig.getSecureString(DNS_PUBLIC_KEY)).append(" ").append(this.mConfig.getSecureString(DNS_NAME_SERVER_KEY)).append(" ").append("127.0.0.1:2222");
           // cmd1.append(" -udp ").append(dns).append(":53   -pubkey ").append(key).append(" ").append(ns).append(" ").append("127.0.0.1:2222");

            dnsProcess = Runtime.getRuntime().exec(cmd1.toString());

            StreamGobbler.OnLineListener onLineListener = log -> {
                addLogInfo("<b>DNS Tunnel: </b>" + log);
                if (log.contains("address of UDP DNS resolver") && mConfig.getSecureString(DNS_PUBLIC_KEY).isEmpty() && hLogStatus.isTunnelActive()) {
                    addLogInfo("<b>DNS Tunnel: </b> Connection error!");
                    context.startService(new Intent(context, dex002.class).setAction(dex002.RECONNECT_SERVICE));
                }
            };

            StreamGobbler stdoutGobbler = new StreamGobbler(dnsProcess.getInputStream(), onLineListener);
            StreamGobbler stderrGobbler = new StreamGobbler(dnsProcess.getErrorStream(), onLineListener);

            stdoutGobbler.start();
            stderrGobbler.start();

            dnsProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            interrupt();
            addLogInfo("DNS Tunnel error: " + e.getMessage());
        } catch (Exception ex) {
            hLogStatus.logDebug("DNS Tunnel Error" + ex.getMessage());
        }

        dnsProcess = null;
    }


    private void addLogInfo(String mLog) {
        if (util.isNetworkAvailable(context) && hLogStatus.isTunnelActive()) {
            if (mLog.contains(this.mConfig.getSecureString(DNS_ADDRESS_KEY)) || mLog.contains(this.mConfig.getSecureString(DNS_NAME_SERVER_KEY))) {
                mLog = mLog.trim().replace(this.mConfig.getSecureString(DNS_ADDRESS_KEY), "[dns address]").replace(this.mConfig.getSecureString(DNS_NAME_SERVER_KEY), "[dns ServerName]");
            }
            if (!mLog.contains("network is unreachable") && util.isNetworkAvailable(context) && !mLog.contains("DNS Tunnel error: null")) {
                hLogStatus.logInfo(mLog);
            }
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        if (dnsProcess != null)
            dnsProcess.destroy();
        try {
            if (filedns != null)
                VpnUtils.killProcess(filedns);
        } catch (Exception ignored) {
        }
        dnsProcess = null;
        filedns = null;
    }

}


