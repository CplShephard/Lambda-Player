package dev.shephard.player.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * iOS rubber-band tarzı elastic overscroll.
 * Compose'un built-in overscroll effect'ini kullanır — scroll'u bozmaz,
 * performans sorunu yaratmaz, her durumda çalışır.
 */
@Composable
fun Modifier.elasticOverscroll(listState: LazyListState): Modifier {
    val overscroll = rememberOverscrollEffect()
    return this.nestedScroll(overscroll.nestedScrollConnection)
}

@Composable
fun Modifier.elasticOverscroll(gridState: LazyGridState): Modifier {
    val overscroll = rememberOverscrollEffect()
    return this.nestedScroll(overscroll.nestedScrollConnection)
}

@Composable
fun Modifier.elasticOverscroll(scrollState: ScrollState): Modifier {
    val overscroll = rememberOverscrollEffect()
    return this.nestedScroll(overscroll.nestedScrollConnection)
}
