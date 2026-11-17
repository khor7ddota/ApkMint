package com.apkmint.creator.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

private enum class IconFontStyle(val label: String) {
    BOLD("Bold"),
    REGULAR("Clean"),
    SERIF("Serif"),
    MONO("Mono"),
    ITALIC("Italic")
}

private enum class IconShape(val label: String) {
    SQUARE("Full-Bleed"),
    ROUNDED("Rounded"),
    CIRCLE("Circle")
}

private enum class GradientDirection(val label: String) {
    DIAGONAL("Diagonal"),
    VERTICAL("Top-Down"),
    HORIZONTAL("Left-Right")
}

private val ICON_PALETTE = listOf(
    "#0D9488", "#10B981", "#1565C0", "#4F46E5",
    "#7C3AED", "#EC4899", "#EF4444", "#F59E0B",
    "#111827", "#FFFFFF"
)

@Composable
fun TextIconGeneratorDialog(
    initialAppName: String,
    onDismiss: () -> Unit,
    onApplyIcon: (Uri, Bitmap) -> Unit
) {
    val context = LocalContext.current

    var iconText by remember {
        mutableStateOf(
            initialAppName.trim()
                .take(2)
                .uppercase()
                .ifEmpty { "AM" }
        )
    }
    var fontStyle by remember { mutableStateOf(IconFontStyle.BOLD) }
    var iconShape by remember { mutableStateOf(IconShape.SQUARE) }
    var textScale by remember { mutableFloatStateOf(0.36f) }
    var textColorHex by remember { mutableStateOf("#FFFFFF") }

    var isGradient by remember { mutableStateOf(true) }
    var bgColor1Hex by remember { mutableStateOf("#0D9488") }
    var bgColor2Hex by remember { mutableStateOf("#3B82F6") }
    var gradientDir by remember { mutableStateOf(GradientDirection.DIAGONAL) }

    val previewBitmap = remember(
        iconText, fontStyle, iconShape, textScale, textColorHex,
        isGradient, bgColor1Hex, bgColor2Hex, gradientDir
    ) {
        renderTextIconBitmap(
            sizePx = 512,
            text = iconText.ifEmpty { "A" },
            fontStyle = fontStyle,
            shape = iconShape,
            textScale = textScale,
            textColorHex = textColorHex,
            isGradient = isGradient,
            bg1Hex = bgColor1Hex,
            bg2Hex = bgColor2Hex,
            direction = gradientDir
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Text Icon", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Text Icon Preview",
                        modifier = Modifier.size(104.dp)
                    )
                }

                OutlinedTextField(
                    value = iconText,
                    onValueChange = { if (it.length <= 5) iconText = it },
                    label = { Text("Icon Text (1-5 chars)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Text Size", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = textScale,
                    onValueChange = { textScale = it },
                    valueRange = 0.25f..0.65f
                )

                Text("Font Style", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconFontStyle.entries.take(3).forEach { style ->
                        FilterChip(
                            selected = fontStyle == style,
                            onClick = { fontStyle = style },
                            label = { Text(style.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconFontStyle.entries.drop(3).forEach { style ->
                        FilterChip(
                            selected = fontStyle == style,
                            onClick = { fontStyle = style },
                            label = { Text(style.label) }
                        )
                    }
                }

                Text("Icon Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconShape.entries.forEach { shape ->
                        FilterChip(
                            selected = iconShape == shape,
                            onClick = { iconShape = shape },
                            label = { Text(shape.label) }
                        )
                    }
                }

                MiniColorPicker(
                    label = "Text Color",
                    selectedHex = textColorHex,
                    onSelect = { textColorHex = it }
                )

                Text("Background Style", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isGradient,
                        onClick = { isGradient = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Solid") }
                    SegmentedButton(
                        selected = isGradient,
                        onClick = { isGradient = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Gradient") }
                }

                MiniColorPicker(
                    label = if (isGradient) "Gradient Start Color" else "Background Color",
                    selectedHex = bgColor1Hex,
                    onSelect = { bgColor1Hex = it }
                )

                if (isGradient) {
                    MiniColorPicker(
                        label = "Gradient End Color",
                        selectedHex = bgColor2Hex,
                        onSelect = { bgColor2Hex = it }
                    )
                    Text("Gradient Direction", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GradientDirection.entries.forEach { dir ->
                            FilterChip(
                                selected = gradientDir == dir,
                                onClick = { gradientDir = dir },
                                label = { Text(dir.label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val uri = saveBitmapToCache(context, previewBitmap)
                onApplyIcon(uri, previewBitmap)
            }) {
                Text("Use This Icon")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MiniColorPicker(
    label: String,
    selectedHex: String,
    onSelect: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { showPicker = true }) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(parseComposeColor(selectedHex, Color.Gray))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.size(6.dp))
                Text("Color Picker")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ICON_PALETTE.take(5).forEach { hex ->
                ColorDot(hex, selectedHex, onSelect)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ICON_PALETTE.drop(5).forEach { hex ->
                ColorDot(hex, selectedHex, onSelect)
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            title = label,
            initialHex = selectedHex,
            onDismiss = { showPicker = false },
            onColorSelected = { hex ->
                onSelect(hex)
                showPicker = false
            }
        )
    }
}

@Composable
private fun ColorDot(hex: String, selectedHex: String, onSelect: (String) -> Unit) {
    val color = parseComposeColor(hex, Color.Gray)
    val isSelected = selectedHex.equals(hex, ignoreCase = true)
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onSelect(hex) }
    )
}

private fun renderTextIconBitmap(
    sizePx: Int,
    text: String,
    fontStyle: IconFontStyle,
    shape: IconShape,
    textScale: Float,
    textColorHex: String,
    isGradient: Boolean,
    bg1Hex: String,
    bg2Hex: String,
    direction: GradientDirection
): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val s = sizePx.toFloat()

    val c1 = parseAndroidColor(bg1Hex, android.graphics.Color.parseColor("#0D9488"))
    val c2 = parseAndroidColor(bg2Hex, android.graphics.Color.parseColor("#3B82F6"))
    val tc = parseAndroidColor(textColorHex, android.graphics.Color.WHITE)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        if (isGradient) {
            val (x1, y1, x2, y2) = when (direction) {
                GradientDirection.DIAGONAL -> floatArrayOf(0f, 0f, s, s)
                GradientDirection.VERTICAL -> floatArrayOf(0f, 0f, 0f, s)
                GradientDirection.HORIZONTAL -> floatArrayOf(0f, 0f, s, 0f)
            }.let { listOf(it[0], it[1], it[2], it[3]) }
            shader = LinearGradient(x1, y1, x2, y2, c1, c2, Shader.TileMode.CLAMP)
        } else {
            color = c1
        }
    }

    val rect = RectF(0f, 0f, s, s)
    when (shape) {
        IconShape.CIRCLE -> canvas.drawOval(rect, bgPaint)
        IconShape.ROUNDED -> canvas.drawRoundRect(rect, s * 0.22f, s * 0.22f, bgPaint)
        IconShape.SQUARE -> canvas.drawRect(rect, bgPaint)
    }

    val tf = when (fontStyle) {
        IconFontStyle.BOLD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        IconFontStyle.REGULAR -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        IconFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        IconFontStyle.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        IconFontStyle.ITALIC -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tc
        typeface = tf
        textSize = s * textScale
        textAlign = Paint.Align.CENTER
    }

    val bounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, bounds)
    val y = (s / 2f) - bounds.exactCenterY()
    canvas.drawText(text, s / 2f, y, textPaint)

    return bmp
}

private fun parseAndroidColor(hex: String, fallback: Int): Int {
    return runCatching { android.graphics.Color.parseColor(hex.trim()) }.getOrDefault(fallback)
}

private fun saveBitmapToCache(context: Context, bmp: Bitmap): Uri {
    val file = File(context.cacheDir, "text_icon_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return Uri.fromFile(file)
}
