package dev.shephard.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * WeChat tarzı elastic rubber-band overscroll.
 *
 * - Scroll sınırındayken NestedScrollConnection ile kalan drag mesafesini yakalar.
 * - Mesafe arttıkça direnç artar; içerik parmağa birebir değil sönümlü şekilde gelir.
 * - Parmak bırakıldığında Animatable + spring ile translationY tekrar 0f olur.
 * - Sadece graphicsLayer translationY kullandığı için LazyColumn/LazyVerticalGrid performansını bozmaz.
 */
@Composable
fun Modifier.elasticOverscroll(
    listState: LazyListState,
    maxStretchDp: Float = 132f,
    baseResistance: Float = 0.42f,
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy
): Modifier = elasticOverscrollInternal(
    canScrollBackward = { listState.canScrollBackward },
    canScrollForward = { listState.canScrollForward },
    maxStretchDp = maxStretchDp,
    baseResistance = baseResistance,
    stiffness = stiffness,
    dampingRatio = dampingRatio,
    isScrollInProgress = { listState.isScrollInProgress }
)

@Composable
fun Modifier.elasticOverscroll(
    gridState: LazyGridState,
    maxStretchDp: Float = 132f,
    baseResistance: Float = 0.42f,
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy
): Modifier = elasticOverscrollInternal(
    canScrollBackward = { gridState.canScrollBackward },
    canScrollForward = { gridState.canScrollForward },
    maxStretchDp = maxStretchDp,
    baseResistance = baseResistance,
    stiffness = stiffness,
    dampingRatio = dampingRatio,
    isScrollInProgress = { gridState.isScrollInProgress }
)

@Composable
fun Modifier.elasticOverscroll(
    scrollState: ScrollState,
    maxStretchDp: Float = 132f,
    baseResistance: Float = 0.42f,
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy
): Modifier = elasticOverscrollInternal(
    canScrollBackward = { scrollState.value > 0 },
    canScrollForward = { scrollState.value < scrollState.maxValue },
    maxStretchDp = maxStretchDp,
    baseResistance = baseResistance,
    stiffness = stiffness,
    dampingRatio = dampingRatio,
    isScrollInProgress = { scrollState.isScrollInProgress }
)

@Composable
private fun Modifier.elasticOverscrollInternal(
    canScrollBackward: () -> Boolean,
    canScrollForward: () -> Boolean,
    maxStretchDp: Float,
    baseResistance: Float,
    stiffness: Float,
    dampingRatio: Float,
    isScrollInProgress: () -> Boolean
): Modifier {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxStretchPx = remember(density, maxStretchDp) { with(density) { maxStretchDp.dp.toPx() } }
    val overscrollY = remember { Animatable(0f) }

    fun animateBack() {
        if (abs(overscrollY.value) <= 0.5f && !overscrollY.isRunning) return
        scope.launch {
            overscrollY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    stiffness = stiffness,
                    dampingRatio = dampingRatio
                )
            )
        }
    }

    val connection = remember(maxStretchPx, baseResistance, stiffness, dampingRatio) {
        ElasticNestedScrollConnection(
            overscrollY = overscrollY,
            scope = scope,
            maxStretchPx = maxStretchPx,
            baseResistance = baseResistance,
            animateBack = ::animateBack
        )
    }

    LaunchedEffect(isScrollInProgress()) {
        if (!isScrollInProgress()) animateBack()
    }

    return this
        .nestedScroll(connection)
        .graphicsLayer {
            translationY = overscrollY.value
        }
}

private class ElasticNestedScrollConnection(
    private val overscrollY: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val maxStretchPx: Float,
    private val baseResistance: Float,
    private val animateBack: () -> Unit
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero

        val dy = available.y
        val current = overscrollY.value
        if (dy == 0f || current == 0f) return Offset.Zero

        // Lastik gerilmişken ters yöne gidilirse önce lastigi kapat.
        if (current.sign == dy.sign) return Offset.Zero

        val consumedY = if (abs(dy) > abs(current)) -current else dy
        val newValue = (current + consumedY).coerceIn(-maxStretchPx, maxStretchPx)
        scope.launch {
            overscrollY.stop()
            overscrollY.snapTo(if (abs(newValue) < 0.5f) 0f else newValue)
        }
        return Offset(x = 0f, y = consumedY)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero

        val dy = available.y
        if (dy == 0f) return Offset.Zero

        // Liste tüketemediği kısım (sınırda) → elastic'e ver, gecikme yok.
        val next = rubberBand(current = overscrollY.value, delta = dy)
        scope.launch {
            overscrollY.stop()
            overscrollY.snapTo(next)
        }
        return Offset(x = 0f, y = dy)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        animateBack()
        return Velocity.Zero
    }

    private fun rubberBand(current: Float, delta: Float): Float {
        val fraction = (abs(current) / maxStretchPx).coerceIn(0f, 0.92f)
        val resistance = baseResistance * (1f - fraction) * (1f - fraction)
        return (current + delta * resistance).coerceIn(-maxStretchPx, maxStretchPx)
    }
}
