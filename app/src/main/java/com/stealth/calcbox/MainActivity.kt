package com.stealth.calcbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.stealth.calcbox.calculator.CalculatorViewModel
import com.stealth.calcbox.ui.CalculatorScreen

class MainActivity : ComponentActivity() {

    private val calculatorViewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black
                )
            ) {
                CalculatorScreen(viewModel = calculatorViewModel)
            }
        }
    }
}
