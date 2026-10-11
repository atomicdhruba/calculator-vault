package com.stealth.calcbox

import com.stealth.calcbox.calculator.CalculatorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculatorViewModelTest {

    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        viewModel = CalculatorViewModel()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals("0", state.displayText)
        assertEquals("", state.expression)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun testDigitInput() {
        viewModel.onDigit("5")
        assertEquals("5", viewModel.uiState.value.displayText)
        viewModel.onDigit("2")
        assertEquals("52", viewModel.uiState.value.displayText)
    }

    @Test
    fun testBasicCalculation() {
        viewModel.onDigit("2")
        viewModel.onOperator("+")
        viewModel.onDigit("3")
        viewModel.onEquals()

        assertEquals("5", viewModel.uiState.value.displayText)
        assertEquals(1, viewModel.uiState.value.history.size)
        assertEquals("2+3", viewModel.uiState.value.history.first().expression)
        assertEquals("5", viewModel.uiState.value.history.first().result)
    }

    @Test
    fun testClear() {
        viewModel.onDigit("9")
        viewModel.onOperator("×")
        viewModel.onDigit("9")
        viewModel.onClear()

        assertEquals("0", viewModel.uiState.value.displayText)
        assertEquals("", viewModel.uiState.value.expression)
    }

    @Test
    fun testDelete() {
        viewModel.onDigit("1")
        viewModel.onDigit("2")
        viewModel.onDigit("3")
        viewModel.onDelete()

        assertEquals("12", viewModel.uiState.value.displayText)
    }

    @Test
    fun testToggleSign() {
        viewModel.onDigit("4")
        viewModel.onDigit("2")
        viewModel.onToggleSign()
        assertEquals("-42", viewModel.uiState.value.displayText)

        viewModel.onToggleSign()
        assertEquals("42", viewModel.uiState.value.displayText)
    }

    @Test
    fun testConsecutiveCalculationAfterEquals() {
        viewModel.onDigit("5")
        viewModel.onOperator("+")
        viewModel.onDigit("5")
        viewModel.onEquals()
        assertEquals("10", viewModel.uiState.value.displayText)

        // Tapping an operator continues with the previous result
        viewModel.onOperator("×")
        viewModel.onDigit("3")
        viewModel.onEquals()
        assertEquals("30", viewModel.uiState.value.displayText)
    }

    @Test
    fun testPasscodeListenerHook() {
        var capturedExpression = ""
        viewModel.onPasscodeEvaluated = { expr, _ ->
            capturedExpression = expr
            false
        }

        viewModel.onDigit("7")
        viewModel.onOperator("×")
        viewModel.onDigit("7")
        viewModel.onEquals()

        assertEquals("7*7", capturedExpression.replace("×", "*"))
    }
}
