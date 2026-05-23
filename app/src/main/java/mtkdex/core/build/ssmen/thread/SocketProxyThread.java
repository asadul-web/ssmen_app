package mtkdex.core.build.ssmen.thread;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import java.io.*;
import java.net.*;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.regex.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import mtkdex.core.build.ssmen.core.vpnutils.VpnUtils;
import com.v2ray.ang.MainApplication;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import mtkdex.core.build.ssmen.service.dex002;
import mtkdex.core.build.ssmen.service.dex004;
import mtkdex.core.build.ssmen.utils.SSLUtil;

public class SocketProxyThread extends Thread implements SettingsConstants {
    private CountDownLatch mTunnelThreadStopSignal;
    private final dex002 service;
    public static HttpsURLConnection huc;
    public static BackServer mBackServerThread;
    private ServerSocket ss;
    private Socket client;
    public static Socket server;
    private int repeatCount = 0;
    public static SSLSocket mSSLSocket;
    private static int mPayload_type;
    private final OnWSTunnelListener mListener;
    public interface OnWSTunnelListener {
        void onStop();
    }
    private final int mProxyAddress;
    private final String mBufferSend;
    private final String mBufferReceive;
    private final int tunnel;
    private final ConfigUtil mConfig;

    public SocketProxyThread(dex002 service, OnWSTunnelListener mListener) {
        this.service = service;
        mConfig = ConfigUtil.getInstance(service);
        SharedPreferences mPref = MainApplication.getPrivateSharedPreferences();
        mProxyAddress = Integer.parseInt(mConfig.getProxyAddress().split(":")[1]);
        mBufferSend = mPref.getString("buffer_send", "16384");
        mBufferReceive = mPref.getString("buffer_receive", "32768");
        mPayload_type = mConfig.getPayloadType();
        tunnel = mConfig.getServerType().equals(SERVER_TYPE_SSH)? service.SSH_DNS:service.OVPN;
        if (mListener == null) {
            throw new NullPointerException();
        }
        this.mListener = mListener;
        try {
            ConnectivityManager cm = (ConnectivityManager) service.getSystemService(Context.CONNECTIVITY_SERVICE);
            ProxyInfo proxy = cm.getDefaultProxy();
            if (proxy != null) {
                addLogInfo("<b>Network Proxy:</b> " + String.format("%s:%d", proxy.getHost(), proxy.getPort()));
            }
        }catch (Exception ignored){}
    }

    private void connectSocket(String host, int port) throws Exception {
        int tunnel_type = mConfig.getPayloadType();
        server = new Socket();
        if (tunnel_type == PAYLOAD_TYPE_SSL || tunnel_type == PAYLOAD_TYPE_SSL_PAYLOAD|| tunnel_type == PAYLOAD_TYPE_SSL_PROXY) {
            server.bind(new InetSocketAddress(0));
        }
        server.connect(new InetSocketAddress(host, port));
        doVpnProtect(server);


    }


    private void connectSSL() throws Exception {
        SSLSocketFactory factory = new SSLUtil(this);
        String mSni = mConfig.getSecureString(SNI_HOST_KEY).replace("[tknetwork]", mConfig.getSecureString(SERVER_KEY)).replace("[tk]", mConfig.getSecureString(SERVER_KEY));
        URL url = new URL("https://" + mSni);
        mSni = url.getHost();
        if (url.getPort() > 0) {
            mSni = mSni + ":" + url.getPort();
        }
        if (!url.getPath().equals("/")) {
            mSni = mSni + url.getPath();
        }
        log("(SNI) Host: " + (ConfigUtil.hide(mSni)));

        huc = (HttpsURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, mBackServerThread.getLocalSocketAddr()));
        huc.setHostnameVerifier((str, sSLSession) -> true);
        huc.setSSLSocketFactory(factory);
        huc.connect();

    }


    private void log(String tag, String msg) {
        log(String.format("%s: %s", tag, msg));
    }
    public void log(String msg) {
        hLogStatus.logInfo(msg);
    }

    private boolean connectSocket() {
        try {
            int mTunnelType = mConfig.getPayloadType();
            String readLine;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            StringBuilder stringBuffer = new StringBuilder();
            while (true) {
                readLine = bufferedReader.readLine();
                if (readLine != null && !readLine.isEmpty()) {
                    stringBuffer.append(readLine);
                    stringBuffer.append("\r\n");
                } else {
                    break;
                }
                if (stringBuffer.toString().isEmpty()) {
                    log("Get Request", "Get request data failed, empty requestline");
                    return false;
                }

                log("Selected Server", mConfig.getServerName());
                if (mTunnelType != PAYLOAD_TYPE_DIRECT) {
                    log("Selected Network", mConfig.getPayloadName());
                }


                if (mTunnelType == PAYLOAD_TYPE_DIRECT) {
                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);
                    send200Status(client.getOutputStream());


                } else if (mTunnelType == PAYLOAD_TYPE_DIRECT_PAYLOAD) {

                    String c = c(stringBuffer.toString());
                    if (c == null) {
                        return false;
                    }
                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);

                    if (!c.isEmpty()) {
                        a(c, server);
                    }
                    send200Status(client.getOutputStream());


                } else if (mTunnelType == PAYLOAD_TYPE_HTTP_PROXY) {

                    String c = c(stringBuffer.toString());
                    if (c == null) {
                        return false;
                    }
                    String proxy = mConfig.getSecureString(PROXY_IP_KEY);
                    int proxyPort = Integer.parseInt(mConfig.getSecureString(PROXY_PORT_KEY));
                    String remote = proxy + ":" + proxyPort;
                    log("[Proxy Server]", "Connecting to " + ConfigUtil.hide(remote));
                    connectSocket(proxy, proxyPort);
                    if (!c.isEmpty()) {
                        a(c, server);

                    }
                } else if (mTunnelType == PAYLOAD_TYPE_SSL) {

                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);
                    connectSSL();
                    send200Status(client.getOutputStream());

                } else if (mTunnelType == PAYLOAD_TYPE_SSL_PAYLOAD) {

                    String c = c(stringBuffer.toString());
                    if (c == null) {
                        return false;
                    }
                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);
                    connectSSL();

                    if (!c.isEmpty()) {
                        a(c, mSSLSocket);
                    }
                    send200Status(client.getOutputStream());
                } else if (mTunnelType == PAYLOAD_TYPE_SSL_PROXY) {

                    String c = c(stringBuffer.toString());
                    if (c == null) {
                        return false;
                    }
                    String proxy = mConfig.getSecureString(PROXY_IP_KEY);
                    int proxyPort = Integer.parseInt(mConfig.getSecureString(PROXY_PORT_KEY));

                    String remote = proxy + ":" + proxyPort;

                    log("[Proxy Server]", "Connecting to " + ConfigUtil.hide(remote));
                    connectSocket(proxy, proxyPort);
                    connectSSL();
                    if (!c.isEmpty()) {
                        a(c, mSSLSocket);
                    }
                } else if (mTunnelType == PAYLOAD_TYPE_OVPN_UDP) {

                    String[] split = stringBuffer.toString().split("\r\n");
                    String str = split[0].split(" ")[1];
                    String host = str.split(":")[0];
                    int port = Integer.parseInt(str.split(":")[1]);

                    connectSocket(host, port);
                    send200Status(client.getOutputStream());

                }
                if (mSSLSocket != null) {
                    return !client.isClosed() && server.isConnected() && mSSLSocket.isConnected();
                } else {
                    return !client.isClosed() && server.isConnected();
                }
            }

        } catch (Exception e) {
            //  log("Socket Server", e.toString());
        }
        return false;
    }


    private void a(String str, Socket socket) throws Exception {
        int i = 0;
        Random g;

        OutputStream outputStream = socket.getOutputStream();
        if (str.contains("[random]")) {
            g = new Random();
            String[] split = str.split(Pattern.quote("[random]"));
            str = split[g.nextInt(split.length)];
        }
        if (str.contains("[repeat]")) {
            String[] split = str.split(Pattern.quote("[repeat]"));
            str = split[repeatCount];
            repeatCount++;
            if (repeatCount > split.length - 1) {
                repeatCount = 0;
            }
        }
        String payload = str.replace("\r\n", "\\r\\n");
        log(String.format("Payload: %s", ConfigUtil.hide(payload)));
        log("Injecting");

        if (str.contains("[split_delay]")) {
            String[] split = str.split(Pattern.quote("[split_delay]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    Thread.sleep(1500);
                }
                i++;
            }
        } else if (str.contains("[split_instant]")) {
            String[] split = str.split(Pattern.quote("[split_instant]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    Thread.sleep(0);
                }
                i++;
            }
        } else if (str.contains("[instant_split]")) {
            String[] split = str.split(Pattern.quote("[instant_split]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    Thread.sleep(0);
                }
                i++;
            }
        } else if (str.contains("[delay_split]")) {
            String[] split = str.split(Pattern.quote("[delay_split]"));
            int length = split.length;
            while (i < length) {
                String str2 = split[i];
                if (a(str2, outputStream)) {
                    outputStream.write(str2.getBytes());
                    outputStream.flush();
                    Thread.sleep(1500);
                }
                i++;
            }
        } else if (a(str, outputStream)) {
            outputStream.write(str.getBytes());
            outputStream.flush();
        }
    }

    private boolean a(String str, OutputStream outputStream) throws Exception {
        if (!str.contains("[split]")) {
            return true;
        }
        int sleepTime = 1;
        for (String str2 : str.split(Pattern.quote("[split]"))) {
            outputStream.write(str2.getBytes());
            Thread.sleep(sleepTime * 1000);
            outputStream.flush();
        }
        return false;
    }


    private String c(String str) {
        String str2;
        if (str != null) {
            try {
                if (!str.isEmpty()) {
                    String charSequence = str.split("\r\n")[0];
                    String[] split = charSequence.split(" ");
                    String[] split2 = split[1].split(":");
                    String host = split2[0];
                    String port = split2[1];
                    str2 = d(mConfig.getSecureString(CUSTOM_PAYLOAD_KEY).replace("[real_raw]", str).replace("[raw]", charSequence).replace("[method]", split[0]).replace("[host_port]", split[1]).replace("[host]", host).replace("[port]", port).replace("[protocol]", split[2]).replace("[cr]", "\r").replace("[lf]", "\n").replace("[crlf]", "\r\n").replace("[lfcr]", "\n\r").replace("[tknetwork]", mConfig.getSecureString(SERVER_KEY)).replace("[tk]", mConfig.getSecureString(SERVER_KEY)).replace("\\r", "\r").replace("\\n", "\n"));

                    return str2;
                }
            } catch (Exception e) {
                log("Payload Error", e.toString());
            }
        }
        log("Payload Error", "Payload is null or empty");
        return null;
    }
    private String d(String str) {
        if (str.contains("[cr*")) {
            str = a(str, "[cr*", "\r");
        }
        if (str.contains("[lf*")) {
            str = a(str, "[lf*", "\n");
        }
        if (str.contains("[crlf*")) {
            str = a(str, "[crlf*", "\r\n");
        }
        return str.contains("[lfcr*") ? a(str, "[lfcr*", "\n\r") : str;
    }
    private String a(String str, String str2, String str3) {
        while (str.contains(str2)) {
            Matcher matcher = Pattern.compile("\\[.*?\\*(.*?[0-9])]").matcher(str);
            if (matcher.find()) {
                int intValue = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
                StringBuilder charSequence = new StringBuilder();
                for (int i = 0; i < intValue; i++) {
                    charSequence.append(str3);
                }
                str = str.replace(str2 + intValue + "]", charSequence.toString());
            }
        }
        return str;
    }


    private void send200Status(OutputStream output) throws Exception {
        output.write("HTTP/1.0 200 Connection Established\r\n\r\n".getBytes());
        output.flush();
    }

    @Override
    public void run() {
        super.run();
        mTunnelThreadStopSignal = new CountDownLatch(1);
        try {
            int prxAdrss = Integer.parseInt(mConfig.getProxyAddress().split(":")[1]);
            ss = new ServerSocket(prxAdrss);
            if (mPayload_type == PAYLOAD_TYPE_SSL || mPayload_type == PAYLOAD_TYPE_SSL_PAYLOAD || mPayload_type == PAYLOAD_TYPE_SSL_PROXY) {
                if (mBackServerThread != null) {
                    mBackServerThread.Stop();
                }
                mBackServerThread = new BackServer();
                mBackServerThread.start();
            }
            service.mHandler.sendEmptyMessage(tunnel);
            while (dex002.isVPNRunning()) {
                client = ss.accept();
                if (client != null && !client.isClosed() && connectSocket()) {
                    client.setKeepAlive(true);
                    if (mSSLSocket != null && mSSLSocket.isConnected()) {
                        mSSLSocket.setKeepAlive(true);
                        server.setKeepAlive(true);
                        doVpnProtect(mSSLSocket);
                        PayloadInjector.connect(client, mSSLSocket, true);
                    } else if (server != null && server.isConnected()) {
                        server.setKeepAlive(true);
                        doVpnProtect(server);
                        PayloadInjector.connect(client, server, true);

                    }
                }

            }
        } catch (Exception e) {
            String msg = e.toString();
            if (msg.contains("bind failed")) {
                interrupt();
                addLogInfo(e.toString());
                mListener.onStop();
            }
        }
        if (!dex002.mStopping) {
            try {
                mTunnelThreadStopSignal.await();
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    /*  private void doVpnProtect(Socket socket) {
          if (VpnUtils.isProtected(socket)) {
              addLogInfo("Socket Protected!");
          }
          // TODO: Implement this method
      }*/
    private void doVpnProtect(Socket socket) {
        if (tunnel==service.SSH_DNS){
            new dex004().protect(socket);
        }else{
            VpnUtils.isProtected(socket);
        }
    }

    private void addLogInfo(String mLog){
        hLogStatus.logInfo(mLog);
    }


    @Override
    public void interrupt(){
        repeatCount = 0;
        new HTTPInjectorThread().interrupt();
        try {
            if (ss != null) {
                ss.close();
                ss = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (server != null) {
                server.close();
                server = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (mSSLSocket != null) {
                mSSLSocket.close();
                mSSLSocket = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (huc != null) {
                huc.disconnect();
            }
        } catch (Exception ignored) {
        }
        try {
            if (mBackServerThread != null) {
                mBackServerThread.interrupt();
                mBackServerThread = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (mSSLSocket != null) {
                mSSLSocket.close();
                mSSLSocket = null;
            }
        } catch (Exception ignored) {
        }
        if (mTunnelThreadStopSignal != null) mTunnelThreadStopSignal.countDown();
        super.interrupt();
    }

}


