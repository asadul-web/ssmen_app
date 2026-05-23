package mtkdex.core.build.ssmen.utils;

import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.io.*;
import java.util.*;
import android.annotation.*;
import android.text.TextUtils;

import mtkdex.core.build.ssmen.thread.SocketProxyThread;

import javax.net.ssl.*;

public class SSLUtil extends SSLSocketFactory
{
	private final SSLContext mSSLContext;

	private final SocketProxyThread mInjector;

	public SSLUtil(SocketProxyThread mInjector) throws Exception {
		this.mInjector = mInjector;
		mSSLContext = SSLContext.getInstance("TLS");
		mSSLContext.init(null, new TrustManager[]{new MyX509TrustManager()}, new SecureRandom());
	}

	private void createSSLSocket(String host, int port, boolean z) throws IOException {
		SocketProxyThread.mSSLSocket = (SSLSocket) mSSLContext.getSocketFactory().createSocket(SocketProxyThread.server, host, port, z);
		LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
		Collections.addAll(linkedHashSet, SocketProxyThread.mSSLSocket.getEnabledProtocols());
		SocketProxyThread.mSSLSocket.setEnabledProtocols(linkedHashSet.toArray(new String[0]));
		log("Enabled Protocols: " + TextUtils.join(", ", SocketProxyThread.mSSLSocket.getEnabledProtocols()));
		SocketProxyThread.mSSLSocket.addHandshakeCompletedListener(handshakeCompletedEvent -> {
			SSLSession session = handshakeCompletedEvent.getSession();
			log(String.format("<b>Cipher Suite: %s</b>", session.getCipherSuite()));
			log("HandshakeCompleted!");
		});
	}

	private void log(String msg) {
		mInjector.log(msg);
	}

	public Socket createSocket(String host, int port) throws IOException {
		createSSLSocket(host, port, true);
		return SocketProxyThread.mSSLSocket;
	}

	public Socket createSocket(String str, int i, InetAddress inetAddress, int i2) {
		return null;
	}

	public Socket createSocket(InetAddress inetAddress, int i) {
		return null;
	}

	public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) {
		return null;
	}

	public Socket createSocket(Socket socket, String host, int port, boolean z) throws IOException {
		createSSLSocket(host, port, z);
		return SocketProxyThread.mSSLSocket;
	}

	public String[] getDefaultCipherSuites() {
		return new String[0];
	}

	public String[] getSupportedCipherSuites() {
		return new String[0];
	}

	@SuppressLint("CustomX509TrustManager")
	public static class MyX509TrustManager implements X509TrustManager {
		@SuppressLint({"TrustAllX509TrustManager"})
		public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) {
		}

		@SuppressLint({"TrustAllX509TrustManager"})
		public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
}
