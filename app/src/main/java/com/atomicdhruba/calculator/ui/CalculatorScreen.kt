package com.atomicdhruba.calculator.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atomicdhruba.calculator.calculator.CalculationRecord
import com.atomicdhruba.calculator.calculator.CalculatorViewModel

// Authentic iOS/Material Calculator Color Palette
private val ColorBg = Color(0xFF000000)
private val ColorLightGray = Color(0xFFA5A5A5)
private val ColorDarkGray = Color(0xFF333333)
private val ColorOrange = Color(0xFFFF9F0A)
private val ColorTextDark = Color(0xFF000000)
private val ColorTextWhite = Color(0xFFFFFFFF)
private val ColorMuted = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var showHistorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scrollState = rememberScrollState()
    LaunchedEffect(uiState.displayText) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Top Toolbar (History Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHistorySheet = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Calculation History",
                        tint = ColorMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Expression display (secondary preview)
            if (uiState.expression.isNotEmpty()) {
                Text(
                    text = uiState.expression,
                    color = ColorMuted,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Main Display with dynamic font scaling & long press copy
            val dynamicFontSize = when {
                uiState.displayText.length <= 6 -> 68.sp
                uiState.displayText.length <= 9 -> 52.sp
                uiState.displayText.length <= 12 -> 40.sp
                uiState.displayText.length <= 16 -> 32.sp
                else -> 26.sp
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .pointerInput(uiState.displayText) {
                        detectTapGestures(
                            onLongPress = {
                                clipboardManager.setText(AnnotatedString(uiState.displayText))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast
                                    .makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        )
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = uiState.displayText,
                    color = ColorTextWhite,
                    fontSize = dynamicFontSize,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.horizontalScroll(scrollState)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad Grid: 5 Rows x 4 Columns
            val buttonSpacing = 12.dp

            // Row 1: C, +/-, %, ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = if (uiState.displayText != "0" && !uiState.isEvaluated) "C" else "AC",
                    bgColor = ColorLightGray,
                    textColor = ColorTextDark,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onClear() }
                )
                CalcButton(
                    text = "+/-",
                    bgColor = ColorLightGray,
                    textColor = ColorTextDark,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onToggleSign() }
                )
                CalcButton(
                    text = "%",
                    bgColor = ColorLightGray,
                    textColor = ColorTextDark,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onOperator("%") }
                )
                CalcButton(
                    text = "÷",
                    bgColor = ColorOrange,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onOperator("÷") }
                )
            }

            Spacer(modifier = Modifier.height(buttonSpacing))

            // Row 2: 7, 8, 9, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = "7",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("7") }
                )
                CalcButton(
                    text = "8",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("8") }
                )
                CalcButton(
                    text = "9",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("9") }
                )
                CalcButton(
                    text = "×",
                    bgColor = ColorOrange,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onOperator("×") }
                )
            }

            Spacer(modifier = Modifier.height(buttonSpacing))

            // Row 3: 4, 5, 6, -
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = "4",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("4") }
                )
                CalcButton(
                    text = "5",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("5") }
                )
                CalcButton(
                    text = "6",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("6") }
                )
                CalcButton(
                    text = "-",
                    bgColor = ColorOrange,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onOperator("-") }
                )
            }

            Spacer(modifier = Modifier.height(buttonSpacing))

            // Row 4: 1, 2, 3, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = "1",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("1") }
                )
                CalcButton(
                    text = "2",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("2") }
                )
                CalcButton(
                    text = "3",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("3") }
                )
                CalcButton(
                    text = "+",
                    bgColor = ColorOrange,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onOperator("+") }
                )
            }

            Spacer(modifier = Modifier.height(buttonSpacing))

            // Row 5: 0, ., DEL, =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                CalcButton(
                    text = "0",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDigit("0") }
                )
                CalcButton(
                    text = ".",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDecimal() }
                )
                CalcButton(
                    text = "DEL",
                    bgColor = ColorDarkGray,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDelete() }
                )
                CalcButton(
                    text = "=",
                    bgColor = ColorOrange,
                    textColor = ColorTextWhite,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEquals() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Calculation History Bottom Sheet
        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1C1C1E)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            color = ColorTextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.history.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onClearHistory()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = ColorOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Clear", color = ColorOrange)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No calculations yet",
                                color = ColorMuted,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            items(
                                items = uiState.history,
                                key = { it.timestamp }
                            ) { record ->
                                HistoryItem(
                                    record = record,
                                    onSelect = {
                                        viewModel.onSelectHistory(record)
                                        showHistorySheet = false
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF2C2C2E))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    text: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = textColor
        ),
        shape = CircleShape,
        modifier = modifier.aspectRatio(1f)
    ) {
        val fontSize = when {
            text.length > 2 -> 20.sp
            text in listOf("+/-", "DEL") -> 22.sp
            text in listOf("+", "-", "×", "÷", "=") -> 32.sp
            else -> 30.sp
        }

        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = if (text in listOf("+", "-", "×", "÷", "=")) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryItem(
    record: CalculationRecord,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = record.expression,
            color = ColorMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "= ${record.result}",
            color = ColorTextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
