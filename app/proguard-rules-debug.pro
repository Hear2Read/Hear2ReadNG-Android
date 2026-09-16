# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# Keep the H2RNGCallback interface and all its methods
-keep interface org.hear2read.h2rng.tts.H2RNGCallback {
    *;
}
# Keep H2RNGCallback implementations
#-keep class * implements org.hear2read.h2rng.tts.H2RNGCallback {
#    void onAudio(short[]);
#    void onDone();
#    void onIndexReached(int);
#}
# This rule only keeps the implemented methods
-keep class * implements org.hear2read.h2rng.tts.H2RNGCallback

# Remove debug and verbose logs
# Keep in debug rules
#-assumenosideeffects class android.util.Log {
#    public static *** d(...);
#    public static *** v(...);
#}