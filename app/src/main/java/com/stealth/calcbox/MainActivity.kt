package com.stealth.calcbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stealth.calcbox.calculator.CalculatorViewModel
import com.stealth.calcbox.ui.CalculatorScreen
import com.stealth.calcbox.vault.AntiForensicsManager
import com.stealth.calcbox.vault.VaultAction
import com.stealth.calcbox.vault.VaultSecurityManager

class MainActivity : ComponentActivity() {

    private val calculatorViewModel: CalculatorViewModel by viewModels()
    private lateinit var vaultSecurityManager: VaultSecurityManager
    private val antiForensicsManager = AntiForensicsManager()

    var isVaultUnlocked: Boolean by mutableStateOf(false)
        private set
    var currentVaultUrl: String by mutableStateOf("")
        private set
    var isDecoyActive: Boolean by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        antiForensicsManager.enableSecureWindow(this)
        enableEdgeToEdge()

        vaultSecurityManager = VaultSecurityManager.create(this)

        calculatorViewModel.onPasscodeEvaluated = { expression, result ->
            val action = vaultSecurityManager.evaluateInput(expression, result)
            when (action) {
                is VaultAction.PanicWipe -> {
                    vaultSecurityManager.executePanicWipe(this) {
                        calculatorViewModel.onClear()
                    }
                    calculatorViewModel.onClear()
                    antiForensicsManager.purgeClipboard(this)
                    lockVault()
                    true
                }
                is VaultAction.UnlockVault -> {
                    unlockVault(action.targetUrl, isDecoy = false)
                    true
                }
                is VaultAction.UnlockDecoy -> {
                    unlockVault(action.decoyUrl, isDecoy = true)
                    true
                }
                VaultAction.None -> false
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black
                )
            ) {
                if (isVaultUnlocked) {
                    // Task 5 WebView engine placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                } else {
                    CalculatorScreen(viewModel = calculatorViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val timeoutSeconds = if (::vaultSecurityManager.isInitialized) {
            vaultSecurityManager.getSettings().autoLockTimeoutSeconds
        } else {
            0
        }

        if (isVaultUnlocked && antiForensicsManager.shouldAutoLock(timeoutSeconds = timeoutSeconds)) {
            lockVault()
            antiForensicsManager.purgeClipboard(this)
        }
        antiForensicsManager.resetPauseTime()

        antiForensicsManager.registerFlipSensor(this) {
            runOnUiThread {
                if (isVaultUnlocked) {
                    lockVault()
                    antiForensicsManager.purgeClipboard(this)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        antiForensicsManager.recordPauseTime()
        antiForensicsManager.unregisterFlipSensor()
    }

    fun lockVault() {
        isVaultUnlocked = false
        currentVaultUrl = ""
        isDecoyActive = false
        calculatorViewModel.onClear()
    }

    fun unlockVault(url: String, isDecoy: Boolean = false) {
        currentVaultUrl = url
        isDecoyActive = isDecoy
        isVaultUnlocked = true
    }
}
