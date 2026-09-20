package dev.shephard.player.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.miuixTopBarBlur
import dev.shephard.player.ui.glass.rememberMiuixPageBackdrop
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
import androidx.compose.ui.text.font.FontWeight
import dev.shephard.player.ui.i18n.LocalStrings

@Composable
fun rememberSolidCollapsingTopBarState(): CollapsingTopBarState {
    val scrollBehavior = MiuixScrollBehavior()
    // Solid submenu: no backdrop, no blur smear, solid background
    return remember(scrollBehavior) { CollapsingTopBarState(scrollBehavior, null) }
}

@Composable
fun rememberCollapsingTopBarState(): CollapsingTopBarState {
    val scrollBehavior = MiuixScrollBehavior()

    val liquidGlassOn = LocalBlurEnabled.current
    // InstallerX-style page backdrop: solid surface base + captured content,
    // so small top bars blur cleanly instead of smearing the page.
    val pageBackdrop = rememberMiuixPageBackdrop(liquidGlassOn)
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
    // InstallerX Revived Miuix values: 25dp blur radius blended with the
    // theme surface at 80% opacity.
    SmallTopAppBar(
        title = title,

        modifier = modifier.then(
            if (state.pageBackdrop != null) {
                Modifier.miuixTopBarBlur(backdrop = state.pageBackdrop)
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
        modifier = modifier.then(
            if (state.pageBackdrop != null) {
                Modifier.miuixTopBarBlur(backdrop = state.pageBackdrop)
            } else Modifier
        ),
    )
}

/**
 * 0f..1f "how far the in-content page title has scrolled away" for a page that
 * scrolls with [scrollState]. Drives [SubmenuTopBar] so its title/background fade
 * in on scroll, exactly like the main pages' collapsing bar, instead of being
 * permanently visible.
 */
@Composable
fun rememberScrollFraction(scrollState: ScrollState, thresholdDp: Dp = 44.dp): State<Float> {
    val thresholdPx = with(LocalDensity.current) { thresholdDp.toPx() }.coerceAtLeast(1f)
    return remember(scrollState, thresholdPx) {
        derivedStateOf { (scrollState.value / thresholdPx).coerceIn(0f, 1f) }
    }
}

/** LazyColumn variant of [rememberScrollFraction]; the title must be item 0. */
@Composable
fun rememberScrollFraction(listState: LazyListState, thresholdDp: Dp = 44.dp): State<Float> {
    val thresholdPx = with(LocalDensity.current) { thresholdDp.toPx() }.coerceAtLeast(1f)
    return remember(listState, thresholdPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / thresholdPx).coerceIn(0f, 1f)
        }
    }
}

/**
 * Small top bar for sub-pages (theme / playback / about / stats / playlist detail).
 *
 * Behaves like the main pages' bar: at the top of the page only the back button is
 * shown (the page title lives in the content); as the content scrolls, [collapseFraction]
 * fades the solid background and the small title in. It must be driven by a real Miuix
 * bar (not a hand-rolled Box) so the attached scroll behavior gets initialised — an
 * un-initialised behavior swallows every upward scroll and freezes the list.
 */
@Composable
fun SubmenuTopBar(
    title: String,
    onBack: () -> Unit,
    state: CollapsingTopBarState,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
) {
    val cs = MiuixAppTheme.colorScheme
    val fraction = collapseFraction.coerceIn(0f, 1f)
    SmallTopAppBar(
        title = title,
        modifier = modifier,
        color = cs.background.copy(alpha = fraction),
        titleColor = cs.onBackground.copy(alpha = fraction),
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
                    contentDescription = LocalStrings.current.backContentDescription,
                    tint = cs.onBackground
                )
            }
        }
    )
}
