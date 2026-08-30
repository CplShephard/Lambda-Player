package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.miuixBlurSurface
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.compose.ui.text.font.FontWeight

@Composable
fun rememberCollapsingTopBarState(): CollapsingTopBarState {
    val scrollBehavior = MiuixScrollBehavior()

    val liquidGlassOn = LocalBlurEnabled.current
    val pageBackdrop = if (liquidGlassOn) rememberLayerBackdrop() else null
    return remember(scrollBehavior, pageBackdrop) { CollapsingTopBarState(scrollBehavior, pageBackdrop) }
}

class CollapsingTopBarState(
    val scrollBehavior: ScrollBehavior,
    val pageBackdrop: LayerBackdrop? = null,
) {

    val collapseFraction: Float
        get() = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
}

fun Modifier.captureForTopBarBlur(state: CollapsingTopBarState): Modifier =
    state.pageBackdrop?.let { this.then(Modifier.layerBackdrop(it)) } ?: this

@Composable
fun CollapsingTopBar(
    title: String,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
) {
    val scrollProgress = state.collapseFraction
    // Use the same blur/tint configuration as the mini player pop-up so the
    // glass surface feels visually consistent across the app.
    SmallTopAppBar(
        title = title,

        modifier = modifier
            .then(
                if (state.pageBackdrop != null) {
                    Modifier.miuixBlurSurface(
                        backdrop = state.pageBackdrop,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        blurRadius = 28f,
                        tintAlpha = 0.58f,
                        fallbackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                } else Modifier
            ),
        color = if (state.pageBackdrop != null)
            androidx.compose.ui.graphics.Color.Transparent
        else
            MiuixAppTheme.colorScheme.background.copy(alpha = scrollProgress),
        titleColor = wallpaperAdaptiveTextColor().copy(alpha = scrollProgress),
        scrollBehavior = state.scrollBehavior,

        defaultWindowInsetsPadding = false,
    )
}

@Composable
fun CollapsingPageTitle(
    title: String,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
) {
    val scrollProgress = state.collapseFraction
    Text(
        text = title,
        style = MiuixAppTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = wallpaperAdaptiveTextColor(),

        modifier = modifier.graphicsLayer { alpha = 1f - scrollProgress }
    )
}

@Composable
fun MiuixTopBar(
    title: String,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
    subtitle: String = "",
) {
    val cs = MiuixAppTheme.colorScheme
    val collapseFraction = state.collapseFraction
    TopAppBar(
        title = title,
        largeTitle = title,
        largeTitleColor = wallpaperAdaptiveTextColor(),
        titleColor = wallpaperAdaptiveTextColor().copy(alpha = collapseFraction),
        subtitle = subtitle,
        subtitleColor = cs.onSurfaceVariant,

        color = if (state.pageBackdrop != null)
            androidx.compose.ui.graphics.Color.Transparent
        else
            cs.background.copy(alpha = collapseFraction),
        scrollBehavior = state.scrollBehavior,
        defaultWindowInsetsPadding = false,
        modifier = modifier
            .then(
                if (state.pageBackdrop != null) {
                    // Match the mini player pop-up's blur/tint values for a
                    // consistent liquid-glass look across the app.
                    Modifier.miuixBlurSurface(
                        backdrop = state.pageBackdrop,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        blurRadius = 28f,
                        tintAlpha = 0.58f,
                        fallbackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                } else Modifier
            ),
    )
}

@Composable
fun SubmenuTopBar(
    title: String,
    onBack: () -> Unit,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
) {
    val cs = MiuixAppTheme.colorScheme
    val collapseFraction = state.collapseFraction
    SmallTopAppBar(
        title = title,
        modifier = modifier
            .then(
                if (state.pageBackdrop != null) {
                    // Match the mini player pop-up's blur/tint values for a
                    // consistent liquid-glass look across the app.
                    Modifier.miuixBlurSurface(
                        backdrop = state.pageBackdrop,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        blurRadius = 28f,
                        tintAlpha = 0.58f,
                        fallbackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                } else Modifier
            ),
        color = if (state.pageBackdrop != null)
            androidx.compose.ui.graphics.Color.Transparent
        else
            cs.background.copy(alpha = collapseFraction),
        titleColor = wallpaperAdaptiveTextColor().copy(alpha = collapseFraction),
        scrollBehavior = state.scrollBehavior,
        defaultWindowInsetsPadding = false,
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceVariant.copy(alpha = 0.75f))
                    .bounceClick { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = cs.onBackground
                )
            }
        }
    )
}
