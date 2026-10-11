package com.atomicdhruba.calculator.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalculationRecord(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CalculatorUiState(
    val expression: String = "",
    val displayText: String = "0",
    val history: List<CalculationRecord> = emptyList(),
    val isEvaluated: Boolean = false
)

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    /**
     * Hook invoked when an expression is evaluated.
     * Returns true if handled (e.g. valid vault passcode entered).
     */
    var onPasscodeEvaluated: ((expression: String, result: String) -> Boolean)? = null

    fun onDigit(digit: String) {
        _uiState.update { state ->
            if (state.isEvaluated || state.displayText == "Error") {
                state.copy(
                    expression = if (state.displayText != "Error") state.displayText else "",
                    displayText = digit,
                    isEvaluated = false
                )
            } else {
                val newDisplay = if (state.displayText == "0") digit else state.displayText + digit
                state.copy(displayText = newDisplay)
            }
        }
    }

    fun onDecimal() {
        _uiState.update { state ->
            if (state.isEvaluated || state.displayText == "Error") {
                state.copy(
                    expression = if (state.displayText != "Error") state.displayText else "",
                    displayText = "0.",
                    isEvaluated = false
                )
            } else {
                val currentSegment = state.displayText.takeLastWhile { it.isDigit() || it == '.' }
                if (currentSegment.contains('.')) {
                    state
                } else if (state.displayText.isEmpty() || !state.displayText.last().isDigit()) {
                    state.copy(displayText = state.displayText + "0.")
                } else {
                    state.copy(displayText = state.displayText + ".")
                }
            }
        }
    }

    fun onOperator(op: String) {
        _uiState.update { state ->
            if (state.displayText == "Error") return@update state

            val formattedOp = when (op) {
                "*" -> "×"
                "/" -> "÷"
                "-" -> "-"
                else -> op
            }

            if (state.displayText.isEmpty() || state.displayText == "0") {
                if (formattedOp == "-") {
                    state.copy(displayText = "-", isEvaluated = false)
                } else {
                    state
                }
            } else {
                val lastChar = state.displayText.lastOrNull()
                val isLastOp = lastChar in listOf('+', '-', '×', '÷', '%', '*', '/')
                val newDisplay = if (isLastOp) {
                    state.displayText.dropLast(1) + formattedOp
                } else {
                    state.displayText + formattedOp
                }
                state.copy(
                    displayText = newDisplay,
                    isEvaluated = false
                )
            }
        }
    }

    fun onToggleSign() {
        _uiState.update { state ->
            if (state.displayText == "0" || state.displayText == "Error") return@update state

            if (state.displayText.startsWith("-") && !state.displayText.drop(1).any { it in "+-×÷*/" }) {
                state.copy(displayText = state.displayText.removePrefix("-"))
            } else if (!state.displayText.any { it in "+-×÷*/" }) {
                state.copy(displayText = "-" + state.displayText)
            } else if (state.displayText.endsWith(")")) {
                val lastOpen = state.displayText.lastIndexOf("(-")
                if (lastOpen != -1) {
                    val prefix = state.displayText.substring(0, lastOpen)
                    val num = state.displayText.substring(lastOpen + 2).removeSuffix(")")
                    state.copy(displayText = "$prefix$num")
                } else {
                    state
                }
            } else {
                val expr = state.displayText
                var i = expr.length - 1
                while (i >= 0 && (expr[i].isDigit() || expr[i] == '.')) {
                    i--
                }
                if (i >= 0 && i < expr.length - 1) {
                    val prefix = expr.substring(0, i + 1)
                    val num = expr.substring(i + 1)
                    if (prefix.endsWith("(-")) {
                        state.copy(displayText = prefix.dropLast(2) + num)
                    } else if (prefix.endsWith("-") && (i == 0 || prefix[i - 1] in "+-×÷*/(")) {
                        state.copy(displayText = prefix.dropLast(1) + num)
                    } else {
                        state.copy(displayText = "$prefix(-$num)")
                    }
                } else {
                    state
                }
            }
        }
    }

    fun onDelete() {
        _uiState.update { state ->
            if (state.isEvaluated || state.displayText == "Error") {
                state.copy(displayText = "0", isEvaluated = false)
            } else {
                val newDisplay = if (state.displayText.length <= 1) "0" else state.displayText.dropLast(1)
                state.copy(displayText = newDisplay)
            }
        }
    }

    fun onClear() {
        _uiState.update { state ->
            state.copy(
                expression = "",
                displayText = "0",
                isEvaluated = false
            )
        }
    }

    fun onEquals() {
        val current = _uiState.value
        if (current.displayText == "Error" || current.isEvaluated) return

        val expr = current.displayText
        val result = MathEngine.evaluate(expr)

        val handled = onPasscodeEvaluated?.invoke(expr, result) == true
        if (handled) {
            return
        }

        if (result != "Error") {
            val record = CalculationRecord(expression = expr, result = result)
            _uiState.update { state ->
                state.copy(
                    expression = expr,
                    displayText = result,
                    history = listOf(record) + state.history,
                    isEvaluated = true
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    expression = expr,
                    displayText = "Error",
                    isEvaluated = true
                )
            }
        }
    }

    fun onClearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun onSelectHistory(record: CalculationRecord) {
        _uiState.update {
            it.copy(
                expression = record.expression,
                displayText = record.result,
                isEvaluated = true
            )
        }
    }
}
