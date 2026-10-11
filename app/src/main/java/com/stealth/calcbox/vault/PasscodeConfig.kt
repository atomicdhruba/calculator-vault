package com.stealth.calcbox.vault

/**
 * Result action dispatched when user evaluates an expression in the calculator.
 */
sealed interface VaultAction {
    data class UnlockVault(val targetUrl: String = PasscodeSettings.DEFAULT_VAULT_URL) : VaultAction
    data class UnlockDecoy(val decoyUrl: String = PasscodeSettings.DEFAULT_DECOY_URL) : VaultAction
    data object PanicWipe : VaultAction
    data object None : VaultAction
}

/**
 * Normalizes user-entered expressions and passcode configurations for reliable matching.
 * Converts unicode mathematical symbols (× -> *, ÷ -> /, − -> -, dashes), removes whitespace,
 * and strips trailing equals signs.
 */
fun normalizeExpression(input: String): String {
    return input
        .filterNot { it.isWhitespace() }
        .replace("×", "*")
        .replace("✕", "*")
        .replace("÷", "/")
        .replace("−", "-") // Unicode minus \u2212
        .replace("–", "-") // Unicode en-dash \u2013
        .replace("—", "-") // Unicode em-dash \u2014
        .trimEnd('=')
}

/**
 * Security settings configuration for the covert calculator vault.
 */
data class PasscodeSettings(
    val primaryPasscode: String = DEFAULT_PRIMARY_PASSCODE,
    val decoyPasscode: String = DEFAULT_DECOY_PASSCODE,
    val panicPasscode: String = DEFAULT_PANIC_PASSCODE,
    val vaultUrl: String = DEFAULT_VAULT_URL,
    val decoyUrl: String = DEFAULT_DECOY_URL,
    val autoLockTimeoutSeconds: Int = DEFAULT_AUTO_LOCK_TIMEOUT_SECONDS,
    val allowResultMatch: Boolean = false
) {
    companion object {
        const val DEFAULT_PRIMARY_PASSCODE = "7*7"
        const val DEFAULT_DECOY_PASSCODE = "1111"
        const val DEFAULT_PANIC_PASSCODE = "0000"
        const val DEFAULT_VAULT_URL = "https://www.instagram.com/"
        const val DEFAULT_DECOY_URL = "https://en.m.wikipedia.org/"
        const val DEFAULT_AUTO_LOCK_TIMEOUT_SECONDS = 0
    }
}
