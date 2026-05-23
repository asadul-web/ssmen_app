package mtkdex.core.build.ssmen.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

import com.v2ray.ang.R;
import com.v2ray.ang.MainApplication;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import mtkdex.core.build.ssmen.service.dex004;


public class c_01 {



    private static char decodeChar(char c) {
        // Example transformation: reverse the shift by 3 positions
        if (c >= '0' && c <= '9') {
            return (char) ('0' + (c - '0' + 7) % 10);
        } else if (c >= 'a' && c <= 'z') {
            return (char) ('a' + (c - 'a' + 23) % 26);
        } else if (c >= 'A' && c <= 'Z') {
            return (char) ('A' + (c - 'A' + 23) % 26);
        }
        return c; // Non-alphanumeric characters are unchanged
    }
    public static String readAsset(Context context, String filename) throws IOException {
        return readStream(context.getResources().getAssets().open(filename), 0, filename);
    }
    public static String readStream(InputStream stream, long max_len, String fn) throws IOException {
        try {
            Reader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            while (true) {
                int read = reader.read(buffer, 0, buffer.length);
                if (read <= 0) {
                    break;
                }
                builder.append(buffer, 0, read);
                if (max_len > 0 && ((long) builder.length()) > max_len) {
                    throw new FileTooLarge(fn, max_len);
                }
            }
            String stringBuilder = builder.toString();
            return stringBuilder;
        } finally {
            stream.close();
        }
    }
    public static String readFileAppPrivate(Context context, String filename) throws IOException {
        return readStream(context.openFileInput(filename), 0, filename);
    }
    public static class FileTooLarge extends IOException {
        public FileTooLarge(String fn, long max_size) {
            super(String.format(MainApplication.resString(R.string.file_too_large), new Object[]{fn, Long.valueOf(max_size)}));
        }
    }
    public static boolean renameFile(String from_path, String to_path) {
        if (from_path == null || to_path == null) {
            return false;
        }
        return new File(from_path).renameTo(new File(to_path));
    }
    public static void writeFileAppPrivate(Context context, String filename, String content) throws IOException {
        FileOutputStream fos = context.openFileOutput(filename, 0);
        try {
            fos.write(content.getBytes());
        } finally {
            fos.close();
        }
    }

    public static String dirname(String path) {
        if (path != null) {
            return new File(path).getParent();
        }
        return null;
    }

    public static boolean copyToClipboard(Context context, String text){
        try{
            int sdk = android.os.Build.VERSION.SDK_INT;
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                clipboard.setText(text);
            }
            else {
                android.content.ClipData clip = android.content.ClipData.newPlainText("Message", text);
                clipboard.setPrimaryClip(clip);
            }
            return true;
        } catch (Exception e) {
            hLogStatus.logDebug(e.getMessage());
            return false;
        }
    }
    public static String readFromRaw(Context context, int resId) {
        InputStream in = context.getResources().openRawResource(resId);
        Scanner scanner = new Scanner(in,"UTF-8").useDelimiter("\\A");
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            sb.append(scanner.next());
        }
        scanner.close();
        return sb.toString();
    }

    public static String hideSTR(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    public static String encrypt(String str) {
        try {
            str = c_00.encrypt(dex004.x, str);
        } catch (GeneralSecurityException ignored) {
        }
        return str;
    }

    public static String decrypt(String str) {
        try {
            str = c_00.decrypt(dex004.x, str);
        } catch (GeneralSecurityException ignored) {
        }
        return str;
    }


    public static String readFromAsset(final AppCompatActivity c,String name){
        try {
            File file = new File(c.getFilesDir(),name);
            StringBuilder b = new StringBuilder();
            Reader reader;
            char[] buff = new char[1024];
            if (file.exists()) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            } else {
                reader = new BufferedReader(new InputStreamReader(c.getAssets().open(name)));
            }
            while (true) {
                int read = reader.read(buff,0,buff.length);
                if (read <= 0) {
                    break;
                }
                b.append(buff,0,read);
            }
            return decrypt(b.toString());
        } catch (Exception e) {
            Toast.makeText(c,"readFromAsset error! "+e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }
    public static String readTextFile(File f){
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String st;
            while((st = br.readLine()) != null){
                text.append(st + "\n");
            }
            br.close();
            return text.toString();
        } catch(Exception e){
            e.printStackTrace();
        }
        return "";
    }
    public static String readTextUri(Context c, Uri uri){
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(c.getContentResolver().openInputStream(uri)));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }
    public static void overrideFont(Context c,String defaultFontNameToOverride, String customFontFileNameInAssets) {
        // Method disabled as per branding guidelines to use system "serif" and "sans-serif"
        /*
        try {
            final Typeface customFontTypeface = Typeface.createFromAsset(c.getAssets(), customFontFileNameInAssets);
            final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
            defaultFontTypefaceField.setAccessible(true);
            defaultFontTypefaceField.set(null, customFontTypeface);
        } catch (Exception e) {
            Toast.makeText(c, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        */
    }
    public static void setAppStatusNaviColor(AppCompatActivity activity, int color_status, int color_navigation){
        Window window = activity.getWindow();
        window.setStatusBarColor(color_status);
        window.setNavigationBarColor(color_navigation);
    }
    public static String save(Context c,String title,String content,int p)
    {
        File dir=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/TkProGen");
        dir.mkdirs();
        File file=new File(dir,title+".hs");
        try
        {
            OutputStream os=new FileOutputStream(file);
            os.write(content.getBytes());
            os.flush();
            os.close();
            if(p==1)Toast.makeText(c,"File save at "+dir.getAbsolutePath()+"/"+title+".hs", Toast.LENGTH_LONG).show();
            return dir+"/"+title+".hs";
        } catch (IOException e) {
            Toast.makeText(c,e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return "";
    }

    public static boolean setClipboard(Context context, String text){
        try{
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            }else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Message", text);
                clipboard.setPrimaryClip(clip);
            }
            return true;
        }catch (Exception e){
            Toast.makeText(context,e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public static String getClipboard(Context context){
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            return clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                Enumeration<InetAddress> inetAddresses = networkInterfaces.nextElement().getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress nextElement = inetAddresses.nextElement();
                    if (!nextElement.isLoopbackAddress() && (nextElement instanceof Inet4Address)) {
                        return nextElement.getHostAddress();
                    }
                }
            }
            return "No IP Available";
        } catch (SocketException unused) {
            return "ERROR Obtaining IP";
        }
    }

    public static void restart_app(Activity a) {
        PackageManager packageManager = a.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(a.getPackageName());
        assert intent != null;
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        a.startActivity(mainIntent);
        System.exit(0);
    }
    public static String encodeID(String realID) {
        StringBuilder fakeID = new StringBuilder();
        for (char c : realID.toCharArray()) {
            fakeID.append(encodeChar(c));
        }
        return fakeID.toString();
    }
    // Helper function to encode a character
    private static char encodeChar(char c) {
        // Example transformation: shift alphanumeric characters by 3 positions
        if (c >= '0' && c <= '9') {
            return (char) ('0' + (c - '0' + 3) % 10);
        } else if (c >= 'a' && c <= 'z') {
            return (char) ('a' + (c - 'a' + 3) % 26);
        } else if (c >= 'A' && c <= 'Z') {
            return (char) ('A' + (c - 'A' + 3) % 26);
        }
        return c; // Non-alphanumeric characters are unchanged
    }

    // Helper function to decode a character

    // Function to decode the fake V2Ray ID back to the real ID
    public static String decodeID(String fakeID) {
        StringBuilder realID = new StringBuilder();
        for (char c : fakeID.toCharArray()) {
            realID.append(decodeChar(c));
        }
        return realID.toString();
    }
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

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
        return "127.0.0.1";
    }
    public static File zipFile(Context cont) {
        try {
            File file = new File(cont.getFilesDir(), "mtkConfig.zip");
            InputStream in = cont.getAssets().open("mtkConfig.zip");
            OutputStream out = new FileOutputStream(file);
            byte[] bits = new byte[1024];
            while (true) {
                int read = in.read(bits, 0, bits.length);
                if (read <= 0) {
                    break;
                }
                out.write(bits, 0, read);
            }
            out.flush();
            out.close();
            in.close();
            return file;
        } catch (Exception e) {
        }
        return null;
    }




    public static String readFile(Context context, File file) {
        try {
            StringBuilder sb = new StringBuilder();
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            char[] buff = new char[1024];
            while (true) {
                int read = reader.read(buff, 0, buff.length);
                if (read <= 0) {
                    break;
                }
                sb.append(buff, 0, read);
            }
            return sb.toString();
        } catch (Exception e) {
        }
        return null;
    }

    public static String getConfigVersion(Context context) {
        File file = new File(context.getFilesDir(), "Servers.js");
        if (file.exists()) {
            try {
                String str = readFile(context, file);
                JSONObject obj = new JSONObject(str);
                return obj.getString("Version");
            } catch (Exception e) {

            }
        } else {
            return "(Built-in)";
        }
        return "(Built-in)";
    }


    public static class Parser {

        public Parser() {
        }

        public static String encode(byte[] b) {
            if (b == null)
                return null;
            byte[] d = new byte[b.length];
            for (int i = 0; i < d.length; i++) {
                d[i] = (byte) (b[i] - 18);
            }
            byte[] data = new byte[d.length + 2];
            System.arraycopy(d, 0, data, 0, d.length);
            byte[] dest = new byte[(data.length / 3) * 4];


            for (int sidx = 0, didx = 0; sidx < d.length; sidx += 3, didx += 4) {
                dest[didx] = (byte) ((data[sidx] >>> 2) & 077);
                dest[didx + 1] = (byte) ((data[sidx + 1] >>> 4) & 017 | (data[sidx] << 4) & 077);
                dest[didx + 2] = (byte) ((data[sidx + 2] >>> 6) & 003 | (data[sidx + 1] << 2) & 077);
                dest[didx + 3] = (byte) (data[sidx + 2] & 077);
            }


            for (int idx = 0; idx < dest.length; idx++) {
                if (dest[idx] < 26)
                    dest[idx] = (byte) (dest[idx] + 'A');
                else if (dest[idx] < 52)
                    dest[idx] = (byte) (dest[idx] + 'a' - 26);
                else if (dest[idx] < 62)
                    dest[idx] = (byte) (dest[idx] + '0' - 52);
                else if (dest[idx] < 63)
                    dest[idx] = (byte) '+';
                else
                    dest[idx] = (byte) '/';
            }


            for (int idx = dest.length - 1; idx > (d.length * 4) / 3; idx--) {
                dest[idx] = (byte) '=';
            }
            return new String(dest);
        }


        public static String parse(String str) {
            try {
                str = c_00.encrypt("tknetwork1987", str);
            } catch (GeneralSecurityException e) {
            }
            return str;
        }

        public static String parseToString(String str) {
            try {
                str = c_00.decrypt("tknetwork1987", str);
            } catch (GeneralSecurityException e) {
            }
            return str;
        }

        public static byte[] decode(byte[] data) {
            int tail = data.length;
            while (data[tail - 1] == '=')
                tail--;
            byte[] dest = new byte[tail - data.length / 4];

            for (int idx = 0; idx < data.length; idx++) {
                if (data[idx] == '=')
                    data[idx] = 0;
                else if (data[idx] == '/')
                    data[idx] = 63;
                else if (data[idx] == '+')
                    data[idx] = 62;
                else if (data[idx] >= '0' && data[idx] <= '9')
                    data[idx] = (byte) (data[idx] - ('0' - 52));
                else if (data[idx] >= 'a' && data[idx] <= 'z')
                    data[idx] = (byte) (data[idx] - ('a' - 26));
                else if (data[idx] >= 'A' && data[idx] <= 'Z')
                    data[idx] = (byte) (data[idx] - 'A');
            }
            int sidx, didx;
            for (sidx = 0, didx = 0; didx < dest.length - 2; sidx += 4, didx += 3) {
                dest[didx] = (byte) (((data[sidx] << 2) & 255) | ((data[sidx + 1] >>> 4) & 3));
                dest[didx + 1] = (byte) (((data[sidx + 1] << 4) & 255) | ((data[sidx + 2] >>> 2) & 017));
                dest[didx + 2] = (byte) (((data[sidx + 2] << 6) & 255) | (data[sidx + 3] & 077));
            }
            if (didx < dest.length) {
                dest[didx] = (byte) (((data[sidx] << 2) & 255) | ((data[sidx + 1] >>> 4) & 3));
            }
            if (++didx < dest.length) {
                dest[didx] = (byte) (((data[sidx + 1] << 4) & 255) | ((data[sidx + 2] >>> 2) & 017));
            }
            return dest;
        }
    }
}
