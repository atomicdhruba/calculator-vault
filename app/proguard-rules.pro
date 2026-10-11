# ProGuard / R8 Rules for Calculator Stealth Vault
# Task 7: Obfuscation, Anti-Reverse Engineering & Build Automation

# ------------------------------------------------------------------------------
# 1. Application Entry Points
# ------------------------------------------------------------------------------
-keep class com.atomicdhruba.calculator.CalcApplication { *; }
-keep class com.atomicdhruba.calculator.MainActivity { *; }

# ------------------------------------------------------------------------------
# 2. Obfuscation & Repackaging
# ------------------------------------------------------------------------------
# Repackage internal classes to flatten package hierarchy and conceal architecture
-repackageclasses 'com.atomicdhruba.calculator.internal'
-allowaccessmodification

# Optimization settings
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ------------------------------------------------------------------------------
# 3. Anti-Reverse Engineering & Source Concealment
# ------------------------------------------------------------------------------
# Strip source file names and line numbers to hinder decompilation and source mapping
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 4. Strip Debug Logging
# ------------------------------------------------------------------------------
# Remove all verbose, debug, and info logs from release bytecode
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ------------------------------------------------------------------------------
# 5. AndroidX Security Crypto & Google Tink
# ------------------------------------------------------------------------------
# Preserve Tink keyset serialization, protobuf fields, and KeyStore integration
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn androidx.security.crypto.**
-dontwarn com.google.crypto.tink.**

# ------------------------------------------------------------------------------
# 6. Jetpack Compose Runtime & UI
# ------------------------------------------------------------------------------
# Preserve Compose runtime classes and composable functions
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}
-dontwarn androidx.compose.**

# ------------------------------------------------------------------------------
# 7. WebKit & WebView Integration
# ------------------------------------------------------------------------------
# Keep JavascriptInterface annotated methods and WebView client handlers
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    <methods>;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    <methods>;
}
-dontwarn android.webkit.**
-dontwarn androidx.webkit.**

# ------------------------------------------------------------------------------
# 8. Kotlin Coroutines
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
