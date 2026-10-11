package com.stealth.calcbox.vault

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Manages encrypted security storage, dynamic passcode verification,
 * decoy routing, and anti-forensic emergency panic wipes.
 */
class VaultSecurityManager(
    private val preferences: SharedPreferences
) {

    companion object {
        const val PREFS_FILE = "vault_security_prefs"
        const val FALLBACK_PREFS_FILE = "vault_security_fallback_prefs"

        const val KEY_PRIMARY_PASSCODE = "primary_passcode"
        const val KEY_DECOY_PASSCODE = "decoy_passcode"
        const val KEY_PANIC_PASSCODE = "panic_passcode"
        const val KEY_VAULT_URL = "vault_url"
        const val KEY_DECOY_URL = "decoy_url"
        const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout_seconds"
        const val KEY_ALLOW_RESULT_MATCH = "allow_result_match"

        /**
         * Factory method for Android runtime using hardware-backed [EncryptedSharedPreferences]
         * with AES-256 GCM encryption scheme. Gracefully falls back to private prefs if
         * Keystore provider is unavailable. Uses distinct fallback file to prevent corrupting
         * encrypted keysets.
         */
        fun create(context: Context): VaultSecurityManager {
            val prefs = try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                // Safe fallback with distinct file to avoid Keystore keyset collisions
                context.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
            }
            return VaultSecurityManager(prefs)
        }
    }

    /**
     * Reads current security settings from encrypted preferences.
     */
    fun getSettings(): PasscodeSettings {
        return PasscodeSettings(
            primaryPasscode = preferences.getString(
                KEY_PRIMARY_PASSCODE,
                PasscodeSettings.DEFAULT_PRIMARY_PASSCODE
            ) ?: PasscodeSettings.DEFAULT_PRIMARY_PASSCODE,
            decoyPasscode = preferences.getString(
                KEY_DECOY_PASSCODE,
                PasscodeSettings.DEFAULT_DECOY_PASSCODE
            ) ?: PasscodeSettings.DEFAULT_DECOY_PASSCODE,
            panicPasscode = preferences.getString(
                KEY_PANIC_PASSCODE,
                PasscodeSettings.DEFAULT_PANIC_PASSCODE
            ) ?: PasscodeSettings.DEFAULT_PANIC_PASSCODE,
            vaultUrl = preferences.getString(
                KEY_VAULT_URL,
                PasscodeSettings.DEFAULT_VAULT_URL
            ) ?: PasscodeSettings.DEFAULT_VAULT_URL,
            decoyUrl = preferences.getString(
                KEY_DECOY_URL,
                PasscodeSettings.DEFAULT_DECOY_URL
            ) ?: PasscodeSettings.DEFAULT_DECOY_URL,
            autoLockTimeoutSeconds = preferences.getInt(
                KEY_AUTO_LOCK_TIMEOUT,
                PasscodeSettings.DEFAULT_AUTO_LOCK_TIMEOUT_SECONDS
            ),
            allowResultMatch = preferences.getBoolean(KEY_ALLOW_RESULT_MATCH, false)
        )
    }

    /**
     * Atomically persists updated passcode settings.
     */
    fun updateSettings(settings: PasscodeSettings) {
        preferences.edit()
            .putString(KEY_PRIMARY_PASSCODE, settings.primaryPasscode)
            .putString(KEY_DECOY_PASSCODE, settings.decoyPasscode)
            .putString(KEY_PANIC_PASSCODE, settings.panicPasscode)
            .putString(KEY_VAULT_URL, settings.vaultUrl)
            .putString(KEY_DECOY_URL, settings.decoyUrl)
            .putInt(KEY_AUTO_LOCK_TIMEOUT, settings.autoLockTimeoutSeconds)
            .putBoolean(KEY_ALLOW_RESULT_MATCH, settings.allowResultMatch)
            .apply()
    }

    fun updatePrimaryPasscode(newPasscode: String) {
        preferences.edit().putString(KEY_PRIMARY_PASSCODE, newPasscode).apply()
    }

    fun updateDecoyPasscode(newPasscode: String, newDecoyUrl: String? = null) {
        val editor = preferences.edit().putString(KEY_DECOY_PASSCODE, newPasscode)
        if (newDecoyUrl != null) {
            editor.putString(KEY_DECOY_URL, newDecoyUrl)
        }
        editor.apply()
    }

    fun updatePanicPasscode(newPasscode: String) {
        preferences.edit().putString(KEY_PANIC_PASSCODE, newPasscode).apply()
    }

    fun updateVaultUrl(newUrl: String) {
        preferences.edit().putString(KEY_VAULT_URL, newUrl).apply()
    }

    fun updateDecoyUrl(newUrl: String) {
        preferences.edit().putString(KEY_DECOY_URL, newUrl).apply()
    }

    fun updateAutoLockTimeout(seconds: Int) {
        preferences.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, seconds).apply()
    }

    fun updateAllowResultMatch(allow: Boolean) {
        preferences.edit().putBoolean(KEY_ALLOW_RESULT_MATCH, allow).apply()
    }

    /**
     * Resets all credentials and configuration to factory defaults.
     */
    fun resetToDefaults() {
        updateSettings(PasscodeSettings())
    }

    /**
     * Evaluates a calculator expression and result against panic, decoy, and primary passcodes.
     *
     * Evaluation precedence:
     * 1. Panic Code -> Returns [VaultAction.PanicWipe]
     * 2. Decoy Code -> Returns [VaultAction.UnlockDecoy]
     * 3. Primary Passcode -> Returns [VaultAction.UnlockVault]
     * 4. Otherwise -> Returns [VaultAction.None]
     */
    fun evaluateInput(expression: String, result: String = ""): VaultAction {
        val normExpr = normalizeExpression(expression)
        val normResult = normalizeExpression(result)
        val settings = getSettings()

        // 1. Emergency Panic Wipe takes ultimate precedence
        val normPanic = normalizeExpression(settings.panicPasscode)
        if (matchesPasscode(normExpr, normResult, normPanic, settings.allowResultMatch)) {
            return VaultAction.PanicWipe
        }

        // 2. Decoy Mode check
        val normDecoy = normalizeExpression(settings.decoyPasscode)
        if (matchesPasscode(normExpr, normResult, normDecoy, settings.allowResultMatch)) {
            return VaultAction.UnlockDecoy(settings.decoyUrl)
        }

        // 3. Primary Vault Unlock check
        val normPrimary = normalizeExpression(settings.primaryPasscode)
        if (matchesPasscode(normExpr, normResult, normPrimary, settings.allowResultMatch)) {
            return VaultAction.UnlockVault(settings.vaultUrl)
        }

        return VaultAction.None
    }

    private fun matchesPasscode(
        normExpr: String,
        normResult: String,
        normPasscode: String,
        allowResultMatch: Boolean
    ): Boolean {
        if (normPasscode.isEmpty()) return false
        if (normExpr.isNotEmpty() && normExpr == normPasscode) return true
        if (allowResultMatch && normResult.isNotEmpty() && normResult == normPasscode) return true
        return false
    }

    /**
     * Performs an immediate, destructive anti-forensic purge of all stored web sessions,
     * cookies, WebView cache, databases, local storage, and stored vault secrets on disk.
     */
    fun executePanicWipe(context: Context? = null, onWipeComplete: (() -> Unit)? = null) {
        // 1. Completely wipe stored vault secrets, passcodes, and target URLs from disk
        try {
            preferences.edit().clear().commit()
        } catch (_: Exception) {
        }

        // 2. Purge cookies via CookieManager
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.flush()
            }
        } catch (_: Exception) {
            // Handled when running under JVM unit test or WebView not initialized
        }

        // 3. Purge HTML5 WebStorage
        try {
            WebStorage.getInstance().deleteAllData()
        } catch (_: Exception) {
            // Handled when running under JVM unit test
        }

        // 4. Purge WebView databases, caches, and filesystem directories
        try {
            context?.let { ctx ->
                ctx.cacheDir?.deleteRecursively()
                ctx.getDatabasePath("webview.db")?.delete()
                ctx.getDatabasePath("webviewCache.db")?.delete()
                val webViewDir = File(ctx.dataDir, "app_webview")
                if (webViewDir.exists()) {
                    webViewDir.deleteRecursively()
                }
            }
        } catch (_: Exception) {
        }

        // 5. Notify completion callback (e.g. to clear calculation history and reset UI state)
        try {
            onWipeComplete?.invoke()
        } catch (_: Exception) {
        }
    }
}
