package com.apkmint.creator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

@Composable
fun ColorPickerDialog(
    title: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initialColor = runCatching { android.graphics.Color.parseColor(initialHex.trim()) }
        .getOrDefault(android.graphics.Color.parseColor("#0D9488"))
    val initialHsv = remember {
        FloatArray(3).also { ColorUtils.colorToHSL(initialColor, it) }
    }
    val initialAndroidHsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) }
    }

    var hue by remember { mutableFloatStateOf(initialAndroidHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialAndroidHsv[1]) }
    var value by remember { mutableFloatStateOf(initialAndroidHsv[2]) }

    val currentColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    val currentHex = String.format("#%06X", 0xFFFFFF and currentColorInt)
    var hexInput by remember(currentHex) { mutableStateOf(currentHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 2D Saturation-Value Panel
                val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                sat = (offset.x / size.width).coerceIn(0f, 1f)
                                value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                    drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

                    val cx = sat * size.width
                    val cy = (1f - value) * size.height
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.4f),
                        radius = 11.5.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Hue Rainbow Bar
                val rainbow = remember {
                    listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(99.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = ((offset.x / size.width).coerceIn(0f, 1f)) * 360f
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                hue = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
                            }
                        }
                ) {
                    drawRect(brush = Brush.horizontalGradient(rainbow))
                    val hx = (hue / 360f) * size.width
                    drawCircle(
                        color = Color.White,
                        radius = 12.dp.toPx(),
                        center = Offset(hx, size.height / 2f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Preview + Hex Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(currentColorInt))
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { raw ->
                            val cleaned = if (raw.startsWith("#")) raw.take(7) else "#${raw.take(6)}"
                            hexInput = cleaned.uppercase()
                            runCatching {
                                if (cleaned.length == 7) {
                                    val c = android.graphics.Color.parseColor(cleaned)
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(c, hsv)
                                    hue = hsv[0]
                                    sat = hsv[1]
                                    value = hsv[2]
                                }
                            }
                        },
                        label = { Text("HEX Color") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(currentHex) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
