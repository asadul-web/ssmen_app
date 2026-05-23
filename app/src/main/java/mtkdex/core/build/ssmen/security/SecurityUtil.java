package mtkdex.core.build.ssmen.security;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Security utility class for encrypting/decrypting sensitive data
 * This class uses AES encryption to protect API URLs and sensitive strings
 */
public class SecurityUtil {
    
    // Obfuscated key - will be further obfuscated by ProGuard
    // IMPORTANT: These are unique random keys for your app - DO NOT SHARE
    private static final byte[] KEY = {
        0x3F, 0x6A, (byte)0x9E, 0x2D, 0x5C, (byte)0x8F, 0x1B, 0x47,
        0x72, (byte)0xA3, 0x0E, 0x64, (byte)0xD5, 0x29, (byte)0xB8, 0x16
    };
    
    private static final byte[] IV = {
        0x7D, (byte)0xC2, 0x45, (byte)0x91, 0x38, 0x6E, (byte)0xF4, 0x1A,
        (byte)0x8C, 0x53, (byte)0xB7, 0x2F, 0x69, (byte)0xE0, 0x14, (byte)0xA5
    };
    
    // Native method declarations (implement in C++ for better security)
    static {
        try {
            System.loadLibrary("security");
        } catch (UnsatisfiedLinkError e) {
            // Fallback to Java implementation
        }
    }
    
    /**
     * Decrypt encrypted string
     * @param encrypted Base64 encoded encrypted string
     * @return Decrypted string
     */
    public static String decrypt(String encrypted) {
        try {
            byte[] encryptedBytes = Base64.decode(encrypted, Base64.NO_WRAP);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Encrypt string
     * @param plainText Plain text to encrypt
     * @return Base64 encoded encrypted string
     */
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * XOR obfuscation for additional layer
     */
    private static String xorObfuscate(String input, int key) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            output.append((char) (input.charAt(i) ^ key));
        }
        return output.toString();
    }
    
    /**
     * Get API base URL (encrypted)
     * Decrypts to: https://asalo.site
     */
    public static String getApiBaseUrl() {
        return decrypt("IzOrAUi44JtXkmBAy2OzNgW9eJKmukW1vFhQTBHLLIA=");
    }
    
    /**
     * Get API key (encrypted)
     * Decrypts to: https://www.facebook.com/mohammad.abdula24
     */
    public static String getApiKey() {
        return decrypt("zO5g96vC7WKGaxt25b4Bqw==");
    }
    
    /**
     * Anti-tampering check
     */
    public static boolean verifyIntegrity() {
        // Add signature verification logic here
        return true;
    }
    
    /**
     * Detect if app is running in emulator
     */
    public static boolean isEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
    }
    
    /**
     * Detect if app is being debugged
     */
    public static boolean isDebuggable(android.content.Context context) {
        return (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
    
    /**
     * Check for root access
     */
    public static boolean isRooted() {
        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        };
        
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                return true;
            }
        }
        return false;
    }
}
