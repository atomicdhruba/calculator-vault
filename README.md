# Calculator Stealth Vault v2.0

An authentic, high-precision Android calculator application engineered with a built-in cryptographic privacy vault and secure web view.

Built with modern Android standards: **Kotlin 2.1**, **Jetpack Compose (Material 3)**, **AndroidX Security Crypto (AES-256 GCM)**, and **AndroidX WebKit**.

---

## Features

### 1. Authentic Calculation Engine
- **Full Math Precision**: Evaluates arithmetic expressions using Dijkstra's Shunting-Yard algorithm and `BigDecimal` to prevent IEEE 754 floating-point inaccuracies.
- **Operator Precedence**: Correct BODMAS/PEMDAS order of operations with support for parentheses `()`, percentages `%`, unary negation `+/-`, and decimals.
- **Apple/Material Dark UI**: Authentic circular button layout, adaptive font scaling, haptic feedback on touch, and calculation history.

### 2. Cryptographic Privacy Vault
- **Configurable Secret Expression**: Triggered seamlessly during calculation (default `7*7` or `49`).
- **Hardware-Backed Encryption**: Passcodes, settings, and target vault URLs are encrypted using `EncryptedSharedPreferences` backed by Android Keystore (AES-256 GCM / MasterKey).
- **Decoy Mode**: Enter a secondary passcode (default `1111`) to open a harmless fallback site (e.g. Wikipedia).
- **Emergency Panic Wipe**: Entering emergency code (default `0000`) instantly clears all encrypted preferences, flushes cookies, purges WebStorage, clears WebView databases/cache, and returns the display to `0`.

### 3. Anti-Forensics & Stealth Safeguards
- **Screenshot & Recents Protection**: `FLAG_SECURE` prevents screenshots, screen recorders, and Recent Apps task switcher thumbnail leaks.
- **Flip-to-Lock**: Accelerometer orientation detection immediately locks the vault and returns to the calculator when the device is placed face-down on a desk.
- **Monotonic Lifecycle Auto-Lock**: Automatically locks the vault after a configurable background timeout (`System.nanoTime()` monotonic tracking).
- **Clipboard Sanitization**: Purges the system clipboard upon vault lock or panic wipe to prevent sensitive data leaks.

### 4. Production Web Engine
- **Fullscreen Video**: Hardware-accelerated custom view container supporting Instagram Reels and Stories.
- **Media Uploads**: Seamless photo and video uploads using native ActivityResult file choosers.
- **In-WebView Back Navigation**: Android Back gesture navigates history (`goBack()`), exits full screen video, or returns to the calculator when at the root.
- **DOM Automation**: Lightweight, non-intrusive asset injection to suppress app-download nags, auto-dismiss "Not Now" dialogs, and enforce OLED dark mode.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.1
- **Min SDK**: 24 (Android 7.0+)
- **Target SDK**: 36 (Android 15+)
- **UI Framework**: Jetpack Compose BOM (Material 3)
- **Security**: AndroidX Security Crypto (`androidx.security.crypto:security-crypto:1.1.0-alpha06`)
- **Web Engine**: AndroidX WebKit (`androidx.webkit:webkit:1.11.0`)
- **Architecture**: MVVM with reactive StateFlow and lifecycle-aware composables

---

## Project Structure

```
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/             # DOM automation scripts (inject_instagram.js)
│   │   │   ├── java/com/stealth/calcbox/
│   │   │   │   ├── calculator/     # Shunting-yard engine, ViewModel
│   │   │   │   ├── ui/             # Jetpack Compose calculator screen
│   │   │   │   ├── vault/          # Cryptographic security, anti-forensics
│   │   │   │   └── webview/        # Custom WebChromeClient, WebViewClient, VaultWebView
│   │   │   └── res/                # Vector drawables, mipmaps, strings, themes
│   │   └── test/                   # Comprehensive unit test suite (52 tests)
│   ├── proguard-rules.pro          # R8 obfuscation and class repackaging
│   └── build.gradle.kts            # App-level build script
├── docs/plans/                     # Architecture and implementation plans
├── scripts/
│   └── build_release.ps1           # Automated PowerShell release pipeline
├── build.gradle.kts                # Root build script
├── settings.gradle.kts             # Plugin and dependency repositories
└── README.md
```

---

## Building & Verification

### Prerequisites
- JDK 17+
- Android SDK (API 36, Build-Tools 36.0.0+)

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Building Release APK
Run the automated release build script:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\build_release.ps1
```

The minified, obfuscated, and signed APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## Privacy & Security Policy

- **Zero Telemetry**: The application contains no analytics, tracking SDKs, or background exfiltration services.
- **Client-Side Only**: All encryption keys and passcode hashes are generated and stored exclusively on-device within the Android Keystore.
- **Direct Connection**: Web browsing connects directly to official HTTPS endpoints with strict TLS verification (`MIXED_CONTENT_NEVER_ALLOW`).
