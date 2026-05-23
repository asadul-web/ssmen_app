package mtkdex.core.build.ssmen.thread;

import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import mtkdex.core.build.ssmen.service.dex002;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class HTTPInjectorThread extends Thread {
    public static int f = 0;
    String e = "1024";
    String d = "4096";
    boolean c;
    Socket a;
    Socket b;
    ConfigUtil j;
    public HTTPInjectorThread(){}

    HTTPInjectorThread(Socket a, Socket b, boolean c, String e, String d, dex002 s) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.e = e;
        this.d = d;
        j = ConfigUtil.getInstance(s);
    }

    public static void connect(Socket first, Socket second, String e, String d, dex002 s) {
        new HTTPInjectorThread(first, second, true, e, d, s).start();
        new HTTPInjectorThread(second, first, false, e, d, s).start();
    }

    public final void run() {
        byte[] buffer;
        if (this.c) {
            buffer = new byte[Integer.parseInt(this.e)];
        } else {
            buffer = new byte[Integer.parseInt(this.d)];
        }
        try {
            InputStream FromClient = this.a.getInputStream();
            OutputStream ToClient = this.b.getOutputStream();
            while (true) {
                int numberRead = FromClient.read(buffer);
                if (numberRead == -1) {
                    break;
                }
                try {
                    String result = new String(buffer, 0, numberRead);
                    if (this.c) {
                        ToClient.write(buffer, 0, numberRead);
                        ToClient.flush();
                    } else {
                        String[] split = result.split("\r\n");
                        if (split[0].startsWith("HTTP/")) {
                            String line = split[0];
                            int code = Integer.parseInt(line.substring(9, 12));
                            f = code;
                           //addLog(line);
                            if (code == 200) {
                                ToClient.write(buffer, 0, numberRead);
                                ToClient.flush();
                            } else {
                                addLog("<b>Proxy: </b><font color = #FF9600>Auto Replace Header");
                                if (split[0].split(" ")[0].equals("HTTP/1.1")) {
                                    ToClient.write("HTTP/1.0 200 OK\r\n\r\n".getBytes());
                                    ToClient.flush();
                                    addLog(split[0].split(" ")[0]+" 200 OK");
                                } else {
                                    ToClient.write("HTTP/1.0 200 Connection Established\r\n\r\n".getBytes());
                                    ToClient.flush();
                                    addLog(split[0].split(" ")[0]+" 200 Connection established");
                                }
                            }
                        } else {
                            ToClient.write(buffer, 0, numberRead);
                            ToClient.flush();
                        }
                    }
                } catch (Exception e) {
                    try {
                        if (this.a != null) {
                            this.a.close();
                        }
                        if (this.b != null) {
                            this.b.close();
                            return;
                        }
                        return;
                    } catch (IOException e2) {
                        return;
                    }
                } catch (Throwable th) {
                    try {
                        if (this.a != null) {
                            this.a.close();
                        }
                        if (this.b != null) {
                            this.b.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            }

        } catch (Exception ignored) {
        }
    }

    @Override
    public void interrupt() {
        try {
            if (this.a != null) {
                this.a.close();
                this.a = null;
            }
            if (this.b != null) {
                this.b.close();
                this.b = null;
            }
        } catch (IOException ignored) {
        }
        super.interrupt();
    }

    private void addLog(String msg) {
        hLogStatus.logInfo(msg);
    }

}
