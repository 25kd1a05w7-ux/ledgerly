package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorKeypad(
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: C, %, /, DEL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(
                text = "C",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).testTag("calc_clear")
            ) { onClear() }

            KeypadButton(
                text = "%",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).testTag("calc_percent")
            ) { onOperatorClick("%") }

            KeypadButton(
                text = "÷",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).testTag("calc_divide")
            ) { onOperatorClick("/") }

            KeypadButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f).testTag("calc_backspace")
            ) { onBackspace() }
        }

        // Row 2: 7, 8, 9, *
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "7", modifier = Modifier.weight(1f).testTag("calc_7")) { onDigitClick("7") }
            KeypadButton(text = "8", modifier = Modifier.weight(1f).testTag("calc_8")) { onDigitClick("8") }
            KeypadButton(text = "9", modifier = Modifier.weight(1f).testTag("calc_9")) { onDigitClick("9") }
            KeypadButton(
                text = "×",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).testTag("calc_multiply")
            ) { onOperatorClick("*") }
        }

        // Row 3: 4, 5, 6, -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "4", modifier = Modifier.weight(1f).testTag("calc_4")) { onDigitClick("4") }
            KeypadButton(text = "5", modifier = Modifier.weight(1f).testTag("calc_5")) { onDigitClick("5") }
            KeypadButton(text = "6", modifier = Modifier.weight(1f).testTag("calc_6")) { onDigitClick("6") }
            KeypadButton(
                text = "−",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).testTag("calc_minus")
            ) { onOperatorClick("-") }
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "1", modifier = Modifier.weight(1f).testTag("calc_1")) { onDigitClick("1") }
            KeypadButton(text = "2", modifier = Modifier.weight(1f).testTag("calc_2")) { onDigitClick("2") }
            KeypadButton(text = "3", modifier = Modifier.weight(1f).testTag("calc_3")) { onDigitClick("3") }
            KeypadButton(
                text = "+",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).testTag("calc_plus")
            ) { onOperatorClick("+") }
        }

        // Row 5: 0, 00, ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "0", modifier = Modifier.weight(1f).testTag("calc_0")) { onDigitClick("0") }
            KeypadButton(text = "00", modifier = Modifier.weight(1f).testTag("calc_00")) { onDigitClick("00") }
            KeypadButton(text = ".", modifier = Modifier.weight(1f).testTag("calc_dot")) { onDigitClick(".") }
            KeypadButton(
                text = "=",
                backgroundColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f).testTag("calc_equals")
            ) { onEquals() }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String? = null,
    icon: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            icon()
        } else if (text != null) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/**
 * Parses and evaluates standard arithmetic expressions like "25+15.5", "100-20*2", "50+10%"
 */
object CalculatorEvaluator {
    fun evaluate(expression: String): Double? {
        val clean = expression.replace("×", "*").replace("÷", "/").replace("−", "-").trim()
        if (clean.isBlank()) return 0.0

        return try {
            // Simple expression evaluator
            if (clean.contains("+")) {
                val parts = clean.split("+")
                parts.sumOf { evaluate(it) ?: 0.0 }
            } else if (clean.contains("-") && !clean.startsWith("-")) {
                val parts = clean.split("-")
                var result = evaluate(parts[0]) ?: 0.0
                for (i in 1 until parts.size) {
                    result -= (evaluate(parts[i]) ?: 0.0)
                }
                result
            } else if (clean.contains("*")) {
                val parts = clean.split("*")
                var result = 1.0
                for (part in parts) {
                    result *= (evaluate(part) ?: 1.0)
                }
                result
            } else if (clean.contains("/")) {
                val parts = clean.split("/")
                var result = evaluate(parts[0]) ?: 0.0
                for (i in 1 until parts.size) {
                    val denom = evaluate(parts[i]) ?: 1.0
                    if (denom != 0.0) result /= denom
                }
                result
            } else if (clean.endsWith("%")) {
                val num = clean.dropLast(1).toDoubleOrNull() ?: 0.0
                num / 100.0
            } else {
                clean.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}
