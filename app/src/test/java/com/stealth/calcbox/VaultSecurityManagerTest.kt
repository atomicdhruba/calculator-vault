package com.stealth.calcbox

import com.stealth.calcbox.vault.PasscodeSettings
import com.stealth.calcbox.vault.VaultAction
import com.stealth.calcbox.vault.VaultSecurityManager
import com.stealth.calcbox.vault.normalizeExpression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultSecurityManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var vaultSecurityManager: VaultSecurityManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        vaultSecurityManager = VaultSecurityManager(fakePrefs)
    }

    @Test
    fun testDefaultPrimaryPasscodeUnlock() {
        // Standard expression "7*7"
        val action1 = vaultSecurityManager.evaluateInput("7*7", "49")
        assertTrue(action1 is VaultAction.UnlockVault)
        assertEquals("https://www.instagram.com/", (action1 as VaultAction.UnlockVault).targetUrl)

        // Unicode multiplication sign "7×7"
        val action2 = vaultSecurityManager.evaluateInput("7×7", "49")
        assertTrue(action2 is VaultAction.UnlockVault)
        assertEquals("https://www.instagram.com/", (action2 as VaultAction.UnlockVault).targetUrl)

        // With spaces and trailing equals
        val action3 = vaultSecurityManager.evaluateInput(" 7 * 7 = ", "49")
        assertTrue(action3 is VaultAction.UnlockVault)

        val action4 = vaultSecurityManager.evaluateInput(" 7 × 7 = ", "49")
        assertTrue(action4 is VaultAction.UnlockVault)
    }

    @Test
    fun testPanicWipeCode() {
        // Default panic code "0000"
        val action1 = vaultSecurityManager.evaluateInput("0000", "0")
        assertEquals(VaultAction.PanicWipe, action1)

        // With trailing equals
        val action2 = vaultSecurityManager.evaluateInput("0000=", "0")
        assertEquals(VaultAction.PanicWipe, action2)

        // With spaces and trailing equals
        val action3 = vaultSecurityManager.evaluateInput("  0000 =  ", "0")
        assertEquals(VaultAction.PanicWipe, action3)
    }

    @Test
    fun testDecoyCode() {
        // Default decoy code "1111"
        val action1 = vaultSecurityManager.evaluateInput("1111", "1111")
        assertTrue(action1 is VaultAction.UnlockDecoy)
        assertEquals("https://en.m.wikipedia.org/", (action1 as VaultAction.UnlockDecoy).decoyUrl)

        // With trailing equals
        val action2 = vaultSecurityManager.evaluateInput("1111=", "1111")
        assertTrue(action2 is VaultAction.UnlockDecoy)

        // With spaces
        val action3 = vaultSecurityManager.evaluateInput(" 1 1 1 1 = ", "1111")
        assertTrue(action3 is VaultAction.UnlockDecoy)
    }

    @Test
    fun testChangingPrimaryPasscode() {
        vaultSecurityManager.updatePrimaryPasscode("1234")
        val settings = vaultSecurityManager.getSettings()
        assertEquals("1234", settings.primaryPasscode)

        // New passcode unlocks
        val actionNew = vaultSecurityManager.evaluateInput("1234", "1234")
        assertTrue(actionNew is VaultAction.UnlockVault)

        // Old passcode no longer works
        val actionOld = vaultSecurityManager.evaluateInput("7*7", "49")
        assertEquals(VaultAction.None, actionOld)
    }

    @Test
    fun testChangingDecoyPasscodeAndUrl() {
        vaultSecurityManager.updateDecoyPasscode("9999", "https://duckduckgo.com/")
        val settings = vaultSecurityManager.getSettings()
        assertEquals("9999", settings.decoyPasscode)
        assertEquals("https://duckduckgo.com/", settings.decoyUrl)

        // New decoy code unlocks with new decoy URL
        val actionNew = vaultSecurityManager.evaluateInput("9999", "9999")
        assertTrue(actionNew is VaultAction.UnlockDecoy)
        assertEquals("https://duckduckgo.com/", (actionNew as VaultAction.UnlockDecoy).decoyUrl)

        // Old decoy code returns None
        val actionOld = vaultSecurityManager.evaluateInput("1111", "1111")
        assertEquals(VaultAction.None, actionOld)
    }

    @Test
    fun testChangingPanicCode() {
        vaultSecurityManager.updatePanicPasscode("911")
        val settings = vaultSecurityManager.getSettings()
        assertEquals("911", settings.panicPasscode)

        // New panic code triggers wipe
        val actionNew = vaultSecurityManager.evaluateInput("911", "911")
        assertEquals(VaultAction.PanicWipe, actionNew)

        // Old panic code no longer triggers wipe
        val actionOld = vaultSecurityManager.evaluateInput("0000", "0")
        assertEquals(VaultAction.None, actionOld)
    }

    @Test
    fun testOrdinaryCalculationsReturnNone() {
        val testCases = listOf(
            Pair("2+3", "5"),
            Pair("5*5", "25"),
            Pair("5×5", "25"),
            Pair("100/2", "50"),
            Pair("100÷2", "50"),
            Pair("10-4", "6"),
            Pair("10−4", "6"),
            Pair("0", "0"),
            Pair("42", "42")
        )

        for ((expr, res) in testCases) {
            val action = vaultSecurityManager.evaluateInput(expr, res)
            assertEquals("Expected VaultAction.None for $expr = $res", VaultAction.None, action)
        }
    }

    @Test
    fun testExpressionNormalization() {
        // Whitespace removal
        assertEquals("7*7", normalizeExpression("  7 * 7  "))
        assertEquals("1234", normalizeExpression(" 1 2 3 4 "))

        // Trailing equals removal
        assertEquals("7*7", normalizeExpression("7*7="))
        assertEquals("0000", normalizeExpression("0000=="))

        // Unicode multiplication operators
        assertEquals("7*7", normalizeExpression("7×7"))
        assertEquals("8*8", normalizeExpression("8✕8"))

        // Unicode division operator
        assertEquals("100/2", normalizeExpression("100÷2"))

        // Unicode minus operators
        assertEquals("10-4", normalizeExpression("10−4")) // \u2212 minus
        assertEquals("10-4", normalizeExpression("10–4")) // \u2013 en-dash
        assertEquals("10-4", normalizeExpression("10—4")) // \u2014 em-dash

        // Complex combinations
        assertEquals("100/2*3-5", normalizeExpression("  100 ÷ 2 × 3 − 5 = "))
    }

    @Test
    fun testAllowResultMatchSetting() {
        // Configure passcode to "49"
        vaultSecurityManager.updatePrimaryPasscode("49")

        // By default, allowResultMatch is false: entering "7*7" produces result "49" but doesn't match "49" expression
        val actionDefault = vaultSecurityManager.evaluateInput("7*7", "49")
        assertEquals(VaultAction.None, actionDefault)

        // Typing "49" directly as expression matches
        val actionDirect = vaultSecurityManager.evaluateInput("49", "49")
        assertTrue(actionDirect is VaultAction.UnlockVault)

        // When allowResultMatch is true, evaluating "7*7" with result "49" unlocks vault
        vaultSecurityManager.updateAllowResultMatch(true)
        val actionWithResultMatch = vaultSecurityManager.evaluateInput("7*7", "49")
        assertTrue(actionWithResultMatch is VaultAction.UnlockVault)
    }

    @Test
    fun testResetToDefaults() {
        vaultSecurityManager.updatePrimaryPasscode("999")
        vaultSecurityManager.updateDecoyPasscode("888", "https://custom.org/")
        vaultSecurityManager.updatePanicPasscode("777")
        vaultSecurityManager.updateVaultUrl("https://custom-vault.org/")
        vaultSecurityManager.updateAutoLockTimeout(30)
        vaultSecurityManager.updateAllowResultMatch(true)

        vaultSecurityManager.resetToDefaults()

        val settings = vaultSecurityManager.getSettings()
        assertEquals(PasscodeSettings.DEFAULT_PRIMARY_PASSCODE, settings.primaryPasscode)
        assertEquals(PasscodeSettings.DEFAULT_DECOY_PASSCODE, settings.decoyPasscode)
        assertEquals(PasscodeSettings.DEFAULT_PANIC_PASSCODE, settings.panicPasscode)
        assertEquals(PasscodeSettings.DEFAULT_VAULT_URL, settings.vaultUrl)
        assertEquals(PasscodeSettings.DEFAULT_DECOY_URL, settings.decoyUrl)
        assertEquals(PasscodeSettings.DEFAULT_AUTO_LOCK_TIMEOUT_SECONDS, settings.autoLockTimeoutSeconds)
        assertFalse(settings.allowResultMatch)

        // Default unlock works again
        val action = vaultSecurityManager.evaluateInput("7*7", "49")
        assertTrue(action is VaultAction.UnlockVault)
    }

    @Test
    fun testPanicWipeCallbackExecution() {
        var callbackInvoked = false
        // Even with null or test context, executePanicWipe handles it gracefully and triggers callback
        vaultSecurityManager.executePanicWipe(context = null, onWipeComplete = {
            callbackInvoked = true
        })
        assertTrue(callbackInvoked)
    }
}
