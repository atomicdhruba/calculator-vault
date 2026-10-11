# ProGuard / R8 Rules for Calculator Stealth Vault

# Keep application entry points
-keep class com.stealth.calcbox.CalcApplication { *; }
-keep class com.stealth.calcbox.MainActivity { *; }

# Allow obfuscation of internal packages
-repackageclasses 'com.stealth.calcbox.internal'
-allowaccessmodification

# Optimization settings
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
