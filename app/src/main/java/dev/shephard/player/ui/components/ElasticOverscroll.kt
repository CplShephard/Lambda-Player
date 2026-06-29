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

/**
 * iOS rubber-band tarzı elastic overscroll.
 *
 * Compose foundation'ın rememberOverscrollEffect() API'si bu BOM versiyonunda
 * internal olduğu için (erişilemiyor), kendi NestedScrollConnection
 * implementasyonumuzu kullanıyoruz. Sadece graphicsLayer translationY
 * kullandığı için scroll performansını etkilemez.
 *
 * KRİTİK: onPreScroll/onPostScroll her zaman Offset.Zero döner —
 * "scroll tükettim" demiyoruz, sadece görsel translationY uyguluyoruz.
 * Bu sayede normal liste scroll'u Compose tarafından bozulmadan yönetilir.
 */
@Composable
fun Modifier.elasticOverscroll(
    listState: LazyListState,
    maxStretchDp: Float = 120f,
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy
): Modifier = elasticOverscrollInternal(
    maxStretchDp = maxStretchDp,
    stiffness = stiffness,
    dampingRatio = dampingRatio,
    isScrollInProgress = { listState.isScrollInProgress }
)

@Composable
fun Modifier.elasticOverscroll(
    gridState: LazyGridState,
    maxStretchDp: Float = 120f,
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy
): Modifier = elasticOverscrollInternal(
    maxStretchDp = maxStretchDp,
    stiffness = stiffness,
    dampingRatio = dampingRatio,
    isScrollInProgress = { gridState.isScrollInProgress }
)

@Composable
fun Modifier.elasticOverscroll(
    scrollState: ScrollState,
    maxStretchDp: Float = 120f,
    stiffness: Float = Spring.StiffnessMediumLow,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy
): Modifier = elasticOverscrollInternal(
    maxStretchDp = maxStretchDp,
    stiffness = stiffness,
    dampingRatio = dampingRatio,
    isScrollInProgress = { scrollState.isScrollInProgress }
)

@Composable
private fun Modifier.elasticOverscrollInternal(
    maxStretchDp: Float,
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
                animationSpec = spring(stiffness = stiffness, dampingRatio = dampingRatio)
            )
        }
    }

    val connection = remember(maxStretchPx) {
        ElasticNestedScrollConnection(
            overscrollY = overscrollY,
            scope = scope,
            maxStretchPx = maxStretchPx
        )
    }

    LaunchedEffect(isScrollInProgress()) {
        if (!isScrollInProgress()) animateBack()
    }

    return this
        .nestedScroll(connection)
        .graphicsLayer { translationY = overscrollY.value }
}

private class ElasticNestedScrollConnection(
    private val overscrollY: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val maxStretchPx: Float
) : NestedScrollConnection {

    // Liste ortasındayken (lastik 0'dayken) hiçbir şeye dokunma —
    // tüm scroll normal şekilde liste tarafından işlenir.
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero

    // Liste bu delta'yı tüketemediyse (available.y != 0, yani sınırdayız)
    // o kalan miktarı rubber-band ile görsel olarak uygula.
    // ÖNEMLİ: Offset.Zero dönüyoruz — "tükettim" demiyoruz, sadece
    // graphicsLayer'a yansıtıyoruz. Compose'un kendi scroll mekaniği bozulmuyor.
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val dy = available.y
        if (dy == 0f) return Offset.Zero

        val next = rubberBand(current = overscrollY.value, delta = dy)
        scope.launch {
            overscrollY.stop()
            overscrollY.snapTo(next)
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (abs(overscrollY.value) > 0.5f || overscrollY.isRunning) {
            scope.launch {
                overscrollY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                )
            }
        }
        return Velocity.Zero
    }

    private fun rubberBand(current: Float, delta: Float): Float {
        val fraction = (abs(current) / maxStretchPx).coerceIn(0f, 0.92f)
        val resistance = 0.55f * (1f - fraction) * (1f - fraction)
        return (current + delta * resistance).coerceIn(-maxStretchPx, maxStretchPx)
    }
}
