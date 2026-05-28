package mtkdex.core.build.ssmen.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePrefUtil {
    private static final String SECURE_PREFS_NAME = "secure_user_creds";
    private static SharedPreferences encryptedPrefs;

    public static SharedPreferences getEncryptedPrefs(Context context) {
        if (encryptedPrefs == null) {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                encryptedPrefs = EncryptedSharedPreferences.create(
                        context,
                        SECURE_PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                // Fallback to regular prefs if encryption fails (not ideal but avoids crash)
                return context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE);
            }
        }
        return encryptedPrefs;
    }
}
