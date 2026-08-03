package dev.shephard.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import dev.shephard.player.ui.miuix.ExperimentalMaterial3Api
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.MiuixDrawerActionHeader
import dev.shephard.player.ui.components.rememberDrawerDismiss
import dev.shephard.player.ui.miuix.OutlinedTextField
import dev.shephard.player.ui.miuix.Text
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
import kotlin.math.max
import dev.shephard.player.ui.glass.LocalBlurEnabled
import kotlin.math.min

/**
 * Modal popup that lets the user pick a custom accent color via:
 *  1) A classic 2D saturation/value gradient palette with a draggable cursor
 *     (hue is selected by a separate slider below).
 *  2) A hex-code text field (#RRGGBB).
 *
 * The selected color is committed via [onColorPicked] as an ARGB int.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorPickerDialog(
    onDismiss: () -> Unit,
    onColorPicked: (Int) -> Unit,
    initialArgb: Int = 0xFF22C55E.toInt(),
    title: String = "Pick a custom color",
    hexPlaceholder: String = "#RRGGBB",
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

    val liquidGlassOn = LocalBlurEnabled.current
    MiuixDrawer(
        onDismissRequest = onDismiss,
    ) {
        // MADDE — diğer drawer'lar gibi: sol tarafta × (cancel), sağ tarafta ✓ (apply)
        // ikonları + dismiss animasyonu. `rememberDrawerDismiss()` kapanma animasyonunu
        // kesilmeden oynatır. Ayrıca yükseklik 0.74 → 0.82'ye çıkarıldı ki hex alanı
        // ve buton alttan kesilmesin.
        val dismissDrawer = rememberDrawerDismiss()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
                MiuixDrawerActionHeader(
                    title = title,
                    onCancel = dismissDrawer,
                    onConfirm = { if (!hexError) onColorPicked(pickedColor.toArgb()) },
                    confirmEnabled = !hexError,
                )
                Spacer(Modifier.height(16.dp))

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
                    style = MiuixAppTheme.typography.labelMedium,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
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
                    style = MiuixAppTheme.typography.labelMedium,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
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
                    style = MiuixAppTheme.typography.labelMedium,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
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
                        style = MiuixAppTheme.typography.bodySmall,
                        color = MiuixAppTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))
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
