// Shared lyrics geometry and karaoke helpers — used by both vertical FullScreen and horizontal embedded
package dev.shephard.player.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shephard.player.player.SyncedLyricLine
import kotlinx.coroutines.delay

val LocalLyricsForeground = staticCompositionLocalOf { Color.White }

// ── geometry ──
const val LyricInactiveScale = 18f / 23f
const val LyricMoveDurationMs = 650
const val LyricAnchorFraction = 0.40f
val LyricLineHorizontalPadding = 14.dp
val LyricLineVerticalPadding = 9.dp

fun lyricLineLayoutStyle(base: TextStyle): TextStyle = base.copy(
    fontSize = 23.sp,
    lineHeight = 31.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Left,
)

@Composable
fun rememberSmoothLyricsPosition(
    positionMs: Long,
    isPlaying: Boolean,
    enabled: Boolean,
    frameAccurate: Boolean,
    trackId: Long?,
): MutableLongState {
    // Each track starts a fresh interpolation clock so the previous song's position never
    // lingers as the active line is calculated for newly loaded lyrics.
    val smooth = remember(trackId) { mutableLongStateOf(positionMs) }
    LaunchedEffect(trackId, positionMs, isPlaying, enabled, frameAccurate) {
        val current = smooth.longValue
        val base = if (isPlaying && positionMs <= current && current - positionMs < 400L) current else positionMs
        smooth.longValue = base
        if (!isPlaying || !enabled) return@LaunchedEffect
        if (frameAccurate) {
            var startNanos = -1L
            while (true) {
                withFrameNanos { now ->
                    if (startNanos < 0L) startNanos = now
                    smooth.longValue = base + (now - startNanos) / 1_000_000L
                }
            }
        } else {
            val start = android.os.SystemClock.elapsedRealtime()
            while (true) {
                delay(40L)
                smooth.longValue = base + (android.os.SystemClock.elapsedRealtime() - start)
            }
        }
    }
    return smooth
}

fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.drawKaraokeHighlight(
    layout: TextLayoutResult,
    line: SyncedLyricLine,
    positionMs: Long,
    color: Color,
) {
    drawKaraokeHighlightInternal(layout, line, positionMs, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawKaraokeHighlightInternal(
    layout: TextLayoutResult,
    line: SyncedLyricLine,
    positionMs: Long,
    color: Color,
) {
    val textLength = layout.layoutInput.text.length
    if (textLength == 0) return
    var sungChars = 0f
    for (word in line.words) {
        if (positionMs >= word.endMs) {
            sungChars = word.endChar.toFloat()
        } else {
            if (positionMs > word.startMs) {
                val fraction = (positionMs - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1L)
                sungChars = word.startChar + fraction.coerceIn(0f, 1f) * (word.endChar - word.startChar)
            }
            break
        }
    }
    sungChars = sungChars.coerceIn(0f, textLength.toFloat())
    if (sungChars <= 0f) return
    for (lineIndex in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(lineIndex)
        if (sungChars <= lineStart) break
        val lineEnd = layout.getLineEnd(lineIndex, visibleEnd = true)
        val top = layout.getLineTop(lineIndex)
        val bottom = layout.getLineBottom(lineIndex)
        val rtl = layout.getParagraphDirection(lineStart) == ResolvedTextDirection.Rtl
        val edge = if (sungChars >= lineEnd || lineEnd <= lineStart) {
            if (rtl) layout.getLineLeft(lineIndex) else layout.getLineRight(lineIndex)
        } else {
            val charIndex = sungChars.toInt().coerceIn(lineStart, lineEnd - 1)
            val box = layout.getBoundingBox(charIndex)
            val within = sungChars - charIndex
            if (rtl) box.right - box.width * within else box.left + box.width * within
        }
        val feather = 2.dp.toPx()
        val full = sungChars >= lineEnd || lineEnd <= lineStart
        val brush = if (full) SolidColor(color.copy(alpha = 1f)) else Brush.horizontalGradient(
            colors = if (rtl) listOf(Color.Transparent, color.copy(alpha = 1f)) else listOf(color.copy(alpha = 1f), Color.Transparent),
            startX = if (rtl) edge else edge - feather,
            endX = if (rtl) edge + feather else edge,
        )
        clipRect(left = 0f, top = top, right = size.width, bottom = bottom) {
            drawText(layout, brush = brush, alpha = 1f, shadow = Shadow(color.copy(alpha = .22f), blurRadius = 3.dp.toPx()))
        }
    }
}

fun SyncedLyricLine.toLrcLineShared(): String {
    val builder = StringBuilder("[${formatLrcTimeShared(timeMs)}]")
    if (words.isEmpty()) builder.append(text)
    else {
        builder.append(text, 0, words.first().startChar)
        words.forEachIndexed { index, word ->
            builder.append('<').append(formatLrcTimeShared(word.startMs)).append('>')
            val next = words.getOrNull(index + 1)?.startChar ?: text.length
            builder.append(text, word.startChar, next.coerceAtLeast(word.startChar))
        }
        builder.append('<').append(formatLrcTimeShared(words.last().endMs)).append('>')
    }
    translation?.takeIf { it.isNotBlank() }?.let {
        builder.append('\n').append('[').append(formatLrcTimeShared(timeMs)).append(']').append(it.replace('\n', ' '))
    }
    return builder.toString()
}

fun formatLrcTimeShared(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val centiseconds = (safe % 1_000L) / 10L
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}

fun isWordByWordLyricsShared(synced: String): Boolean =
    Regex("<\\d{1,3}:\\d{2}[.:]\\d{1,3}>").containsMatchIn(synced)

fun formatLyricsDurationShared(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1_000L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

// ── Compatibility aliases so existing call sites in FullScreenLyricsExperience keep compiling ──
fun SyncedLyricLine.toLrcLine(): String = toLrcLineShared()
fun formatLrcTime(ms: Long): String = formatLrcTimeShared(ms)
fun isWordByWordLyrics(synced: String): Boolean = isWordByWordLyricsShared(synced)
fun formatLyricsDuration(ms: Long): String = formatLyricsDurationShared(ms)
