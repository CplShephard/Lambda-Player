package dev.shephard.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min

/**
 * Modal popup that lets the user pick a custom accent color via:
 *  1) A classic 2D saturation/value gradient palette with a draggable cursor
 *     (hue is selected by a separate slider below).
 *  2) A hex-code text field (#RRGGBB).
 *
 * The selected color is committed via [onColorPicked] as an ARGB int.
 */
@Composable
fun CustomColorPickerDialog(
    onDismiss: () -> Unit,
    onColorPicked: (Int) -> Unit,
    initialArgb: Int = 0xFF22C55E.toInt(),
    title: String = "Pick a custom color",
    hexPlaceholder: String = "#RRGGBB",
    applyLabel: String = "Apply",
    cancelLabel: String = "Cancel"
) {
    val initialHsv = remember(initialArgb) { rgbToHsv(initialArgb) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var v by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember { mutableStateOf(formatHex(initialArgb)) }
    var hexError by remember { mutableStateOf(false) }
    var userEditedHex by remember { mutableStateOf(false) }

    val pickedColor = Color.hsv(hue, sat, v)

    LaunchedEffect(pickedColor) {
        if (!userEditedHex) {
            hexInput = formatHex(pickedColor.toArgb())
            hexError = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    BouncyIconButton(
                        onClick = onDismiss,
                        icon = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconSize = 22.dp
                    )
                }
                Spacer(Modifier.height(8.dp))

                // Preview swatch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pickedColor)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Palette",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                SaturationValueGrid(
                    hue = hue,
                    saturation = sat,
                    value = v,
                    onChange = { newSat, newV ->
                        sat = newSat
                        v = newV
                        userEditedHex = false
                    }
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Hue",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                HueSlider(
                    hue = hue,
                    onChange = {
                        hue = it
                        userEditedHex = false
                    }
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Hex",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { newValue ->
                        hexInput = newValue
                        userEditedHex = true
                        val parsed = parseHex(newValue)
                        if (parsed != null) {
                            val hsv = rgbToHsv(parsed)
                            hue = hsv[0]
                            sat = hsv[1]
                            v = hsv[2]
                            hexError = false
                        } else {
                            hexError = newValue.length > 1
                        }
                    },
                    placeholder = { Text(hexPlaceholder) },
                    singleLine = true,
                    isError = hexError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )
                if (hexError) {
                    Text(
                        text = "Enter a valid hex like #1DB954",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.bounceClick { onDismiss() }
                    ) { Text(cancelLabel) }
                    Spacer(Modifier.size(8.dp))
                    TextButton(
                        onClick = { if (!hexError) onColorPicked(pickedColor.toArgb()) },
                        enabled = !hexError,
                        modifier = Modifier.bounceClick(enabled = !hexError) {
                            onColorPicked(pickedColor.toArgb())
                        }
                    ) {
                        Text(applyLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationValueGrid(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (sat: Float, value: Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(hue) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    onChange(
                        (down.position.x / w).coerceIn(0f, 1f),
                        (1f - down.position.y / h).coerceIn(0f, 1f)
                    )
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        onChange(
                            (change.position.x / w).coerceIn(0f, 1f),
                            (1f - change.position.y / h).coerceIn(0f, 1f)
                        )
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseHue = Color.hsv(hue, 1f, 1f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, baseHue)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )
            // Cursor
            val cx = saturation * size.width
            val cy = (1f - value) * size.height
            drawCircle(color = Color.Black, radius = 13f, center = Offset(cx, cy), style = Stroke(width = 2f))
            drawCircle(color = Color.White, radius = 11f, center = Offset(cx, cy))
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    onChange(((down.position.x / w).coerceIn(0f, 1f)) * 360f)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        onChange(((change.position.x / w).coerceIn(0f, 1f)) * 360f)
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = (0..12).map { i -> Color.hsv(i * 30f, 1f, 1f) }
            drawRect(brush = Brush.horizontalGradient(colors = colors))
            val cursorX = (hue / 360f) * size.width
            drawCircle(
                color = Color.Black,
                radius = 14f,
                center = Offset(cursorX, size.height / 2f),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = Color.White,
                radius = 12f,
                center = Offset(cursorX, size.height / 2f)
            )
        }
    }
}

// ----- helpers -----

private fun rgbToHsv(argb: Int): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val maxC = max(max(r, g), b)
    val minC = min(min(r, g), b)
    val delta = maxC - minC
    val v = maxC
    val s = if (maxC == 0f) 0f else delta / maxC
    val h = when {
        delta == 0f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val hueOut = if (h < 0f) h + 360f else h
    return floatArrayOf(hueOut, s, v)
}

private fun formatHex(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}

private fun parseHex(input: String): Int? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    return try {
        val value = cleaned.toLong(16)
        (0xFF000000.toInt()) or value.toInt()
    } catch (_: NumberFormatException) {
        null
    }
}
