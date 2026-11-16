// Shared lyrics geometry and karaoke helpers — used by both vertical FullScreen and horizontal embedded
package dev.shephard.player.ui.screens

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
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
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

val LocalLyricsForeground = staticCompositionLocalOf { Color.White }

// ── geometry ──
const val LyricInactiveScale = 18f / 23f
const val LyricMoveDurationMs = 650
const val LyricAnchorFraction = 0.40f
val LyricLineHorizontalPadding = 14.dp
val LyricLineVerticalPadding = 9.dp

/**
 * Draws a lyric line with the existing text layout and lets each grapheme wobble independently.
 * The same 700/800 ms oscillators already used by the lyric views drive the motion; the line
 * layout, wrapping, and karaoke hit ranges therefore remain unchanged.
 */
@Composable
fun SyncedLyricText(
    text: String,
    modifier: Modifier,
    style: TextStyle,
    wobbleEnabled: Boolean,
    wobbleProgress: Float,
    jiggleX: Float,
    jiggleY: Float,
    phaseSeed: Float,
    karaokeLine: SyncedLyricLine?,
    positionMs: Long,
    karaokeColor: Color,
) {
    val textLayout = remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val graphemeRanges = remember(text) { lyricGraphemeRanges(text) }
    val layout = textLayout.value
    val drawWobble = wobbleEnabled && wobbleProgress > 0f
    val glyphPaths = remember(layout, graphemeRanges, drawWobble) {
        if (layout != null && drawWobble) {
            graphemeRanges.map { range -> layout.getPathForRange(range.first, range.last + 1) }
        } else {
            emptyList()
        }
    }

    BasicText(
        text = text,
        onTextLayout = { textLayout.value = it },
        modifier = modifier.drawWithContent {
            if (layout == null || !drawWobble) {
                drawContent()
                if (karaokeLine != null && layout != null) {
                    drawKaraokeHighlight(layout, karaokeLine, positionMs, karaokeColor)
                }
            } else {
                drawWobblingLyricGlyphs(
                    layout = layout,
                    graphemeRanges = graphemeRanges,
                    glyphPaths = glyphPaths,
                    baseColor = style.color,
                    karaokeLine = karaokeLine,
                    positionMs = positionMs,
                    karaokeColor = karaokeColor,
                    progress = wobbleProgress,
                    jiggleX = jiggleX,
                    jiggleY = jiggleY,
                    phaseSeed = phaseSeed,
                )
            }
        },
        style = style,
    )
}

private fun lyricGraphemeRanges(text: String): List<IntRange> {
    if (text.isEmpty()) return emptyList()
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(text)
    val ranges = ArrayList<IntRange>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        if (end > start) ranges += start until end
        start = end
        end = iterator.next()
    }
    return ranges
}

private fun ContentDrawScope.drawWobblingLyricGlyphs(
    layout: TextLayoutResult,
    graphemeRanges: List<IntRange>,
    glyphPaths: List<Path>,
    baseColor: Color,
    karaokeLine: SyncedLyricLine?,
    positionMs: Long,
    karaokeColor: Color,
    progress: Float,
    jiggleX: Float,
    jiggleY: Float,
    phaseSeed: Float,
) {
    val sungCharacterPosition = karaokeLine?.let { sungCharacterPosition(it, positionMs) }
    val xAmplitudePx = 2.dp.toPx()
    val yAmplitudePx = 1.5.dp.toPx()
    val maxAxisScale = 0.70710678f

    graphemeRanges.forEachIndexed { index, range ->
        val start = range.first
        val endExclusive = range.last + 1
        val glyphPath = glyphPaths[index]
        val phase = (index + phaseSeed) * 1.6180339f
        val cosine = cos(phase.toDouble()).toFloat()
        val sine = sin(phase.toDouble()).toFloat()
        val offsetX = (jiggleX * cosine + jiggleY * sine) * maxAxisScale * xAmplitudePx * progress
        val offsetY = (jiggleY * cosine - jiggleX * sine) * maxAxisScale * yAmplitudePx * progress

        withTransform({ translate(left = offsetX, top = offsetY) }) {
            drawPath(glyphPath, color = baseColor)
            val sungThrough = sungCharacterPosition ?: return@withTransform
            if (sungThrough <= start) return@withTransform

            val glyphProgress = ((sungThrough - start) / (endExclusive - start).toFloat()).coerceIn(0f, 1f)
            if (glyphProgress >= 1f) {
                drawPath(glyphPath, color = karaokeColor)
            } else {
                val bounds = layout.getBoundingBox(start)
                if (bounds.width > 0f && bounds.height > 0f) {
                    val isRtl = layout.getParagraphDirection(start) == ResolvedTextDirection.Rtl
                    val revealEdge = if (isRtl) {
                        bounds.right - bounds.width * glyphProgress
                    } else {
                        bounds.left + bounds.width * glyphProgress
                    }
                    clipRect(
                        left = if (isRtl) revealEdge else bounds.left,
                        top = 0f,
                        right = if (isRtl) bounds.right else revealEdge,
                        bottom = size.height,
                    ) {
                        drawPath(glyphPath, color = karaokeColor)
                    }
                }
            }
        }
    }
}

private fun sungCharacterPosition(line: SyncedLyricLine, positionMs: Long): Float {
    var sungChars = 0f
    for (word in line.words) {
        if (positionMs >= word.endMs) {
            sungChars = word.endChar.toFloat()
        } else {
            if (positionMs > word.startMs) {
                val fraction = (positionMs - word.startMs).toFloat() /
                    (word.endMs - word.startMs).coerceAtLeast(1L)
                sungChars = word.startChar + fraction.coerceIn(0f, 1f) * (word.endChar - word.startChar)
            }
            break
        }
    }
    return sungChars.coerceIn(0f, line.text.length.toFloat())
}

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
