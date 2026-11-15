package dev.shephard.player.ui.components.m3

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A scroll-driven header: collapse before scrolling and expand only at the start of the list. */
class ExpressiveTopBarState internal constructor(
    private val rangePx: Float,
    private val scope: CoroutineScope,
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set
    val fraction: Float get() = (-offsetPx / rangePx).coerceIn(0f, 1f)
    private var settling: Job? = null

    internal fun consume(delta: Float): Float {
        settling?.cancel()
        val next = (offsetPx + delta).coerceIn(-rangePx, 0f)
        val consumed = next - offsetPx
        offsetPx = next
        return consumed
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f) Offset(0f, consume(available.y)) else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f) Offset(0f, consume(available.y)) else Offset.Zero

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (offsetPx < 0f && offsetPx > -rangePx) {
                val target = if (fraction >= 0.5f) -rangePx else 0f
                settling?.cancel()
                settling = scope.launch {
                    animate(offsetPx, target, animationSpec = spring(stiffness = Spring.StiffnessMedium)) { value, _ ->
                        offsetPx = value.coerceIn(-rangePx, 0f)
                    }
                }
            }
            return Velocity.Zero
        }
    }
}

@Composable
fun rememberExpressiveTopBarState(): ExpressiveTopBarState {
    val range = with(LocalDensity.current) { 88.dp.toPx() }
    val scope = rememberCoroutineScope()
    return remember(range, scope) { ExpressiveTopBarState(range, scope) }
}
