############################################################
# SAFE PROGUARD CONFIG FOR V2RAY + OPENVPN + FIREBASE
############################################################

##############################
# KEEP JNI / OPENVPN (CRITICAL)
##############################

-keep class net.openvpn.** { *; }
-keep class net.openvpn.openvpn.** { *; }

# Keep all JNI bridge classes
-keep class *JNI* { *; }
-keep class *Swig* { *; }
-keep class *Director* { *; }

# Keep native methods exactly
-keepclasseswithmembernames class * {
    native <methods>;
}

# Do not rename OpenVPN packages
-keepnames class net.openvpn.**

##############################
# KEEP V2RAY CORE
##############################

-keep class go.** { *; }
-keep class libv2ray.** { *; }

-keep class com.v2ray.ang.** { *; }
-keep interface com.v2ray.ang.** { *; }

-keep class com.v2ray.ang.service.** { *; }
-keep class com.v2ray.ang.receiver.** { *; }
-keep class com.v2ray.ang.handler.** { *; }
-keep class com.v2ray.ang.util.** { *; }

-keep class com.v2ray.ang.MainApplication { *; }

##############################
# KEEP VPN SERVICE
##############################

-keep class * extends android.net.VpnService {
    *;
}

##############################
# KEEP FIREBASE
##############################

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

##############################
# KEEP MMKV
##############################

-keep class com.tencent.mmkv.** { *; }

##############################
# KEEP OKHTTP
##############################

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

##############################
# KEEP ANDROIDX + KOTLIN
##############################

-keep class androidx.** { *; }
-keep class kotlin.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

##############################
# KEEP PARCELABLE
##############################

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

##############################
# KEEP SERIALIZABLE
##############################

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

##############################
# REMOVE LOG CALLS
##############################

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String,int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

##############################
# GENERAL SAFE SETTINGS
##############################

-ignorewarnings
-allowaccessmodification