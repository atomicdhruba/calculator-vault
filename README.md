<div align="center">

# 📱 Calculator Stealth Vault v2.0

**An authentic, high-precision Android calculator concealing a hardware-encrypted privacy vault, hardened web container, and multi-layered anti-forensic safeguards.**

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024--36)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Security](https://img.shields.io/badge/Security-AES--256%20GCM-00C853?style=for-the-badge&logo=security&logoColor=white)](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
[![Tests](https://img.shields.io/badge/Unit%20Tests-52%20Passing-brightgreen?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/atomicdhruba/calculator-vault)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](https://opensource.org/licenses/MIT)

</div>

---

## 🌟 Overview

**Calculator Stealth Vault** is engineered to provide an uncompromised, authentic calculator experience on the surface while providing a zero-trace, hardware-encrypted private browser beneath.

Ordinary users and casual observers interact with a fully functional, high-precision dark calculator. Entering your secret mathematical formula instantly shifts the application into a hardware-accelerated, anti-forensically hardened private session.

```
┌─────────────────────────────────┐
│       Standard Calculator       │  ◄── Ordinary Day-to-Day Math
│   (Shunting-Yard & BigDecimal)  │
└────────────────┬────────────────┘
                 │
                 ├── [ 7 * 7 = ]  ────►  🔒 Master Vault (Private Web Session)
                 ├── [ 1 1 1 1 = ] ───►  🎭 Decoy Mode (Harmless Fallback)
                 └── [ 0 0 0 0 = ] ───►  🚨 Panic Wipe (Zero-State Storage Purge)
```

---

## ✨ Key Features

### 🧮 Authentic Calculation Engine
- **Full Precision Arithmetic**: Evaluates arithmetic expressions using Dijkstra's Shunting-Yard algorithm and `BigDecimal`, eliminating IEEE 754 floating-point errors (e.g. `0.1 + 0.2 = 0.3`).
- **Complete PEMDAS/BODMAS Support**: Handles complex expressions with parentheses `()`, percentages `%`, unary toggle `+/-`, and multi-tiered operator precedence.
- **Apple/Material Dark Theme**: Authentic circular buttons, reactive font scaling, haptic feedback, and clipboard copy support.

### 🔐 Hardware-Backed Cryptographic Vault
- **Android Keystore (AES-256 GCM)**: Master passcodes, settings, and URLs are stored via `EncryptedSharedPreferences` backed by hardware security modules (TEE / StrongBox).
- **Custom Mathematical Trigger**: Set custom formula passcodes (e.g., `7*7`, `1234=`, or custom expressions).
- **Decoy Mode**: Enter an alternate code (default `1111`) to open a harmless fallback site (e.g. Wikipedia) when under duress.
- **Emergency Panic Wipe Protocol**: Entering `0000` immediately clears all stored preferences, flushes cookies, purges WebStorage, deletes WebView databases/cache, and returns the display to `0`.

### 🛡️ Multi-Tiered Anti-Forensic Safeguards
- **Window Protection (`FLAG_SECURE`)**: Prevents screenshots, screen recorders, and Recent Apps task switcher thumbnails.
- **Flip-to-Lock Sensor**: Placing the device face-down on a surface (`z < -7.0f`) instantly locks the vault and purges the system clipboard.
- **Monotonic Background Auto-Lock**: Tracks background elapsed time monotonically (`System.nanoTime()`), closing the vault after configurable grace periods without wall-clock drift.
- **Clipboard Sanitization**: Purges the clipboard upon lock to ensure credentials and copied content leave zero memory traces.

### 🌐 Hardened Web Engine
- **Fullscreen Video**: Hardware-accelerated custom view container supporting Reels and Stories in native full-screen.
- **Media Upload Pipeline**: Native Android `ActivityResult` file choosers for uploading photos and videos.
- **Multi-Tiered Back Navigation**: Android Back gesture seamlessly navigates WebView history (`goBack()`), exits full-screen videos, or returns to the calculator.
- **Automated DOM Optimization**: Asset-based script injection (`inject_instagram.js`) via `MutationObserver` suppresses app install banners, auto-clicks "Not Now", and enforces OLED dark mode.

### 🔒 Binary Obfuscation & Minimization (R8)
- **Class Repackaging**: All internal packages (`vault`, `calculator`, `webview`) are flattened into `com.stealth.calcbox.internal.*`.
- **Log Stripping**: Strips debug logging (`Log.v`, `Log.d`, `Log.i`) from release bytecode.
- **Resource Shrinking**: Removes unreferenced assets and shrinks final APK to **~3.07 MB**.

---

## 🏗️ Architecture

The project follows modern Android MVVM clean architecture:

```
app/src/main/java/com/stealth/calcbox/
├── calculator/             # Mathematical evaluation engine & ViewModel
│   ├── MathEngine.kt       # Dijkstra Shunting-Yard evaluator with BigDecimal
│   └── CalculatorViewModel # Reactive StateFlow state manager
├── ui/                     # Jetpack Compose presentation layer
│   └── CalculatorScreen.kt # High-fidelity dark calculator UI
├── vault/                  # Cryptography, anti-forensics & passcode evaluation
│   ├── PasscodeConfig.kt   # Passcode normalization & action hierarchies
│   ├── VaultSecurityManager# Hardware-backed EncryptedSharedPreferences & panic wipe
│   └── AntiForensicsManager# FLAG_SECURE, sensor listener & clipboard sanitizer
└── webview/                # Production webview engine & DOM automation
    ├── VaultChromeClient   # File chooser launcher & fullscreen custom view
    ├── VaultWebViewClient  # Scheme filtering & error containment
    ├── VaultWebView        # Lifecycle-aware Compose AndroidView wrapper
    └── ScriptInjector      # In-memory script caching & domain-restricted injection
```

For complete technical specifications, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## 🚀 Getting Started

### Prerequisites
- **JDK 17** or higher
- **Android SDK** (Compile SDK 36, Min SDK 24)

### Clone & Build
```bash
git clone https://github.com/atomicdhruba/calculator-vault.git
cd calculator-vault
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```
*Executes all 52 unit tests covering math evaluation, cryptographic storage, lifecycle auto-lock, and WebKit clients.*

### Build Signed Release APK
Run the automated release build script:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\build_release.ps1
```

The optimized, minified, and signed release APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 🔒 Security & Privacy Policy

- **Zero Telemetry**: No analytics SDKs, crash reporters, or background network beacons.
- **On-Device Cryptography**: Passcodes and vault configurations never leave the device.
- **Direct TLS Communication**: All web traffic communicates directly over official HTTPS protocols with strict mixed content blocking.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Developed with ❤️ by **[Atomic Dhruba](https://github.com/atomicdhruba)**.
