package dev.shephard.player.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.miuixTopBarBlur
import dev.shephard.player.ui.glass.rememberMiuixPageBackdrop
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

/** Translucent dark scrim; when available the captured page is blurred underneath. */
@Composable
private fun BoxScope.TopBarBlurBackground(
    state: CollapsingTopBarState,
    collapseFraction: Float,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp).align(Alignment.TopCenter)
            .graphicsLayer { alpha = collapseFraction.coerceIn(0f, 1f) }
            .then(
                if (state.pageBackdrop != null)
                    Modifier.miuixTopBarBlur(state.pageBackdrop, tintAlpha = 0.48f, tintColor = Color.Black)
                else Modifier.background(Color.Black.copy(alpha = 0.48f))
            )
    )
}

/** Single title moves from the large left title to the centered small title. */
@Composable
private fun BoxScope.MovingMiuixTitle(title: String, fraction: Float) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier = Modifier.matchParentSize().padding(horizontal = 20.dp),
        contentAlignment = BiasAlignment(-1f + f, 1f - 2f * f),
    ) {
        Box(modifier = Modifier.height(56.dp).fillMaxWidth(),
            contentAlignment = BiasAlignment(-1f + f, 0f)) {
            Text(
                text = title,
                style = MiuixAppTheme.typography.headlineSmall.copy(
                    fontSize = androidx.compose.ui.unit.lerp(28.sp, 18.sp, f),
                    fontWeight = FontWeight.Bold,
                ),
                color = MiuixAppTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 48.dp * f),
            )
        }
    }
}

@Composable
fun CollapsingTopBar(
    title: String,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
) {
    val scrollProgress = state.collapseFraction
    Box(modifier = modifier.fillMaxWidth()) {
        TopBarBlurBackground(state, scrollProgress)
        SmallTopAppBar(
            title = title,
            color = if (state.pageBackdrop != null)
                androidx.compose.ui.graphics.Color.Transparent
            else
                MiuixAppTheme.colorScheme.background.copy(alpha = scrollProgress),
            titleColor = MiuixAppTheme.colorScheme.onBackground.copy(alpha = scrollProgress),
            scrollBehavior = state.scrollBehavior,
            defaultWindowInsetsPadding = false,
        )
    }
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
        color = MiuixAppTheme.colorScheme.onBackground,

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
    val fraction = state.collapseFraction
    Box(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = title,
            largeTitle = title,
            titleColor = Color.Transparent,
            largeTitleColor = Color.Transparent,
            subtitle = subtitle,
            color = Color.Transparent,
            scrollBehavior = state.scrollBehavior,
            defaultWindowInsetsPadding = false,
        )
        TopBarBlurBackground(state, fraction)
        MovingMiuixTitle(title, fraction)
    }
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
    compact: Boolean = false,
) {
    val cs = MiuixAppTheme.colorScheme
    val fraction = if (compact) collapseFraction.coerceIn(0f, 1f) else state.collapseFraction
    val backIcon: @Composable () -> Unit = {
        Box(
            modifier = Modifier.padding(start = 12.dp).size(40.dp).clip(CircleShape)
                .background(cs.surfaceVariant.copy(alpha = 0.75f)).bounceClick { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = LocalStrings.current.backContentDescription, tint = cs.onBackground)
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        if (compact) {
            SmallTopAppBar(title = title, color = Color.Transparent, titleColor = Color.Transparent,
                scrollBehavior = state.scrollBehavior, defaultWindowInsetsPadding = false)
            TopBarBlurBackground(state, fraction)
            Box(Modifier.align(Alignment.TopStart).height(56.dp), contentAlignment = Alignment.CenterStart) { backIcon() }
            Box(Modifier.matchParentSize().graphicsLayer { alpha = fraction }, contentAlignment = Alignment.Center) {
                Text(title, color = cs.onBackground, style = MiuixAppTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 64.dp))
            }
        } else {
            TopAppBar(title = title, largeTitle = title, titleColor = Color.Transparent,
                largeTitleColor = Color.Transparent, color = Color.Transparent,
                scrollBehavior = state.scrollBehavior, defaultWindowInsetsPadding = false)
            TopBarBlurBackground(state, fraction)
            MovingMiuixTitle(title, fraction)
            Box(Modifier.align(Alignment.TopStart).height(56.dp), contentAlignment = Alignment.CenterStart) { backIcon() }
        }
    }
}
