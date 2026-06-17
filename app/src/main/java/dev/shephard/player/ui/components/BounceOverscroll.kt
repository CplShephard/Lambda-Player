@file:OptIn(ExperimentalFoundationApi::class)

package dev.shephard.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BounceOverscrollEffect(private val scope: CoroutineScope) : OverscrollEffect {
    private val overscrollY = Animatable(0f)

    @Suppress("OverridingDeprecatedMember")
    override val effectModifier: Modifier = Modifier.graphicsLayer {
        translationY = overscrollY.value
    }

    override val isInProgress: Boolean
        get() = overscrollY.value != 0f || overscrollY.isRunning

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        val consumed = performScroll(delta)
        val remaining = delta - consumed

        if (source == NestedScrollSource.Drag && remaining.y != 0f) {
            scope.launch {
                val resistance = 0.5f
                overscrollY.snapTo(overscrollY.value + remaining.y * resistance)
            }
        }
        return delta
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        performFling(velocity)
        overscrollY.animateTo(
            0f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }
}

@Composable
fun rememberBounceOverscrollEffect(): OverscrollEffect {
    val scope = rememberCoroutineScope()
    return remember { BounceOverscrollEffect(scope) }
}
