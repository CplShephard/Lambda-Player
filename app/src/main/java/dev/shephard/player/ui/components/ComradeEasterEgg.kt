package dev.shephard.player.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.util.TrackOverlayForBack
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The hidden pop-up is only reachable while the app language is Simplified Chinese. */
private const val EASTER_EGG_LANGUAGE = "zh"

/** Taps needed on the About icon, each one at most [TAP_WINDOW_MS] after the previous one. */
private const val REQUIRED_TAPS = 5
private const val TAP_WINDOW_MS = 450L

/** Text is intentionally hard-coded in Chinese: the pop-up only exists in the Chinese UI. */
private const val EASTER_EGG_MESSAGE = "我与你同在，亲爱的同志。中国大陆的朋友们使用这个应用，是我的荣幸 🇨🇳☭"

/**
 * Counts quick consecutive taps on the About screen app icon and calls [onTrigger] after
 * [REQUIRED_TAPS] of them. Does nothing unless the app language is Chinese.
 */
@Composable
fun Modifier.comradeEasterEggTrigger(onTrigger: () -> Unit): Modifier {
    if (LocalStrings.current.code != EASTER_EGG_LANGUAGE) return this
    val haptic = LocalHapticFeedback.current
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    // [0] = tap count, [1] = time of the last tap. Plain array: no recomposition per tap.
    val tapState = remember { longArrayOf(0L, 0L) }
    return this.pointerInput(Unit) {
        detectTapGestures {
            val now = SystemClock.uptimeMillis()
            tapState[0] = if (now - tapState[1] <= TAP_WINDOW_MS) tapState[0] + 1 else 1
            tapState[1] = now
            if (tapState[0] >= REQUIRED_TAPS) {
                tapState[0] = 0
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnTrigger()
            }
        }
    }
}

/**
 * Full-screen hidden pop-up: the message card in the middle and endless heart / ☭ bursts
 * behind it until the pop-up is closed (button, tap outside or back).
 *
 * Performance: one Canvas, a fixed-size particle pool stored in primitive arrays (no
 * allocations per frame), emoji rasterised once into small bitmaps and drawn with
 * drawImage, and the frame clock read only inside the draw lambda so each frame just
 * redraws the Canvas without recomposing anything.
 */
@Composable
fun ComradeEasterEggDialog(useMiuix: Boolean, onDismiss: () -> Unit) {
    TrackOverlayForBack()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val noRipple = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = noRipple, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            CelebrationBursts(modifier = Modifier.fillMaxSize())
            EasterEggCard(useMiuix = useMiuix, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun EasterEggCard(useMiuix: Boolean, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
    }
    val containerColor: Color
    val textColor: Color
    val accent: Color
    val shape: RoundedCornerShape
    if (useMiuix) {
        containerColor = MiuixAppTheme.colorScheme.surface
        textColor = MiuixAppTheme.colorScheme.onSurface
        accent = MiuixAppTheme.colorScheme.primary
        shape = RoundedCornerShape(32.dp)
    } else {
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        textColor = MaterialTheme.colorScheme.onSurface
        accent = MaterialTheme.colorScheme.primary
        shape = RoundedCornerShape(28.dp)
    }
    val cardTapSink = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val p = appear.value
                alpha = p.coerceIn(0f, 1f)
                scaleX = 0.8f + 0.2f * p
                scaleY = 0.8f + 0.2f * p
            }
            .clip(shape)
            .background(containerColor)
            // Taps on the card itself must not close the pop-up.
            .clickable(interactionSource = cardTapSink, indication = null) { }
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Text(
            text = "🇨🇳 ☭ ❤️",
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Text(
            text = EASTER_EGG_MESSAGE,
            color = textColor,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (useMiuix) {
            top.yukonga.miuix.kmp.basic.TextButton(
                text = strings.close,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors(
                    color = accent,
                    textColor = MiuixAppTheme.colorScheme.onPrimary,
                ),
            )
        } else {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = M3ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                androidx.compose.material3.Text(strings.close)
            }
        }
    }
}

private const val MAX_PARTICLES = 96
private const val PARTICLES_PER_BURST = 12
private const val BURST_INTERVAL_SEC = 0.42f
private const val PARTICLE_LIFE_SEC = 1.5f

@Composable
private fun CelebrationBursts(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    // Rasterised once: two heart emoji and the hammer & sickle in red and in gold.
    val sprites: List<ImageBitmap> = remember(density) {
        val px = with(density) { 30.dp.toPx() }
        listOf(
            rasterizeGlyph("❤️", px, Color.Red.toArgb()),
            rasterizeGlyph("💖", px, Color.Red.toArgb()),
            rasterizeGlyph("☭", px * 1.1f, Color(0xFFDE2910).toArgb()),
            rasterizeGlyph("☭", px * 1.1f, Color(0xFFFFDE00).toArgb()),
        )
    }
    val pool = remember { ParticlePool(MAX_PARTICLES) }
    var frameTime by remember { mutableLongStateOf(0L) }
    val sizeHolder = remember { FloatArray(2) }

    LaunchedEffect(pool) {
        var last = 0L
        var untilNextBurst = 0f
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                val w = sizeHolder[0]
                val h = sizeHolder[1]
                if (w > 0f && h > 0f) {
                    untilNextBurst -= dt
                    if (untilNextBurst <= 0f) {
                        untilNextBurst = BURST_INTERVAL_SEC
                        pool.burst(
                            x = w * (0.12f + Random.nextFloat() * 0.76f),
                            y = h * (0.10f + Random.nextFloat() * 0.80f),
                            speedScale = w / 3f,
                            spriteCount = sprites.size,
                        )
                    }
                    pool.step(dt, gravity = h * 0.55f)
                }
                frameTime = now
            }
        }
    }

    Canvas(modifier = modifier) {
        sizeHolder[0] = size.width
        sizeHolder[1] = size.height
        // Reading the frame clock here invalidates only this draw pass every frame.
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTime
        for (i in 0 until pool.capacity) {
            val life = pool.life[i]
            if (life <= 0f) continue
            val sprite = sprites[pool.sprite[i]]
            val progress = 1f - life / PARTICLE_LIFE_SEC
            // Pop in quickly, then shrink slightly and fade out towards the end of the life.
            val scale = pool.scale[i] * (if (progress < 0.15f) progress / 0.15f else 1f - 0.25f * progress)
            val alpha = if (progress > 0.6f) ((1f - progress) / 0.4f).coerceIn(0f, 1f) else 1f
            val cx = pool.x[i]
            val cy = pool.y[i]
            rotate(degrees = pool.rotation[i], pivot = Offset(cx, cy)) {
                scale(scale = scale, pivot = Offset(cx, cy)) {
                    drawImage(
                        image = sprite,
                        topLeft = Offset(cx - sprite.width / 2f, cy - sprite.height / 2f),
                        alpha = alpha,
                    )
                }
            }
        }
    }
}

/** Fixed-size particle storage; dead slots (life <= 0) are reused by new bursts. */
private class ParticlePool(val capacity: Int) {
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val vx = FloatArray(capacity)
    val vy = FloatArray(capacity)
    val life = FloatArray(capacity)
    val rotation = FloatArray(capacity)
    val spin = FloatArray(capacity)
    val scale = FloatArray(capacity)
    val sprite = IntArray(capacity)
    private var cursor = 0

    fun burst(x: Float, y: Float, speedScale: Float, spriteCount: Int) {
        val angleOffset = Random.nextFloat() * (2f * PI.toFloat())
        repeat(PARTICLES_PER_BURST) { n ->
            val i = cursor
            cursor = (cursor + 1) % capacity
            val angle = angleOffset + n * (2f * PI.toFloat() / PARTICLES_PER_BURST) +
                (Random.nextFloat() - 0.5f) * 0.4f
            val speed = speedScale * (0.55f + Random.nextFloat() * 0.6f)
            this.x[i] = x
            this.y[i] = y
            vx[i] = cos(angle) * speed
            vy[i] = sin(angle) * speed - speedScale * 0.35f
            life[i] = PARTICLE_LIFE_SEC * (0.8f + Random.nextFloat() * 0.2f)
            rotation[i] = (Random.nextFloat() - 0.5f) * 40f
            spin[i] = (Random.nextFloat() - 0.5f) * 220f
            scale[i] = 0.7f + Random.nextFloat() * 0.6f
            sprite[i] = Random.nextInt(spriteCount)
        }
    }

    fun step(dt: Float, gravity: Float) {
        if (dt <= 0f) return
        val drag = 1f - 1.6f * dt
        for (i in 0 until capacity) {
            if (life[i] <= 0f) continue
            life[i] -= dt
            vx[i] *= drag
            vy[i] = vy[i] * drag + gravity * dt
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            rotation[i] += spin[i] * dt
        }
    }
}

private fun rasterizeGlyph(text: String, textSizePx: Float, color: Int): ImageBitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        this.color = color
        textAlign = Paint.Align.CENTER
        isFakeBoldText = text == "☭"
    }
    val metrics = paint.fontMetrics
    val pad = 4
    val width = ceil(paint.measureText(text)).toInt().coerceAtLeast(1) + pad * 2
    val height = ceil(metrics.descent - metrics.ascent).toInt().coerceAtLeast(1) + pad * 2
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(bitmap).drawText(text, width / 2f, pad - metrics.ascent, paint)
    return bitmap.asImageBitmap()
}
