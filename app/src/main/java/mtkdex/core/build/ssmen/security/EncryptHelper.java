package mtkdex.core.build.ssmen.security;

/**
 * Run this ONCE to generate new encrypted strings with updated keys
 * Then DELETE this file before release
 */
public class EncryptHelper {
    public static void main(String[] args) {
        // Your actual URLs
        String apiUrl = "https://asalo.site";
        String apiKey = "https://www.facebook.com/mohammad.abdula24";
        
        // Generate encrypted versions with NEW keys
        String encryptedUrl = SecurityUtil.encrypt(apiUrl);
        String encryptedKey = SecurityUtil.encrypt(apiKey);
        
        System.out.println("=== NEW ENCRYPTED STRINGS (with updated keys) ===");
        System.out.println("\nEncrypted API URL:");
        System.out.println(encryptedUrl);
        System.out.println("\nEncrypted API Key:");
        System.out.println(encryptedKey);
        
        // Verify decryption works
        System.out.println("\n=== VERIFICATION ===");
        System.out.println("Decrypted URL: " + SecurityUtil.decrypt(encryptedUrl));
        System.out.println("Decrypted Key: " + SecurityUtil.decrypt(encryptedKey));
        
        System.out.println("\n=== NEXT STEPS ===");
        System.out.println("1. Copy the encrypted strings above");
        System.out.println("2. Update SecurityUtil.java with these new encrypted strings");
        System.out.println("3. DELETE this EncryptHelper.java file");
        System.out.println("4. Build release APK");
    }
}
