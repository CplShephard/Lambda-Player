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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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

/**
 * These full-window headers have no padded ancestor. Apply the actual top safe inset once
 * to the entire bar, including custom overlays, then mark it consumed so Miuix's own bar
 * does not insert a second copy. Unlike a hard-coded status bar height this includes cutouts.
 */
@Composable
fun Modifier.miuixHeaderSafeTop(): Modifier {
    val safeTop = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    val padding = safeTop.asPaddingValues().calculateTopPadding()
    return this.padding(top = padding).consumeWindowInsets(safeTop)
}

/**
 * Fixed shade under Library Songs/Playlists/Albums tabs reusing the miuix topBar dim.
 * Library has no collapsing top bar, so this provides the same blur/dim as a fixed bar.
 */
@Composable
fun LibraryTabsFixedShade(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val surface = MiuixAppTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (backdrop != null) Modifier.miuixTopBarBlur(backdrop, tintAlpha = 0.48f, tintColor = surface)
                else Modifier.background(surface)
            )
    ) {
        content()
    }
}

@Composable
fun rememberLibraryTabsBackdrop(): LayerBackdrop? {
    val blurOn = LocalBlurEnabled.current
    return rememberMiuixPageBackdrop(blurOn)
}

/** Translucent dark scrim; when available the captured page is blurred underneath. */
@Composable
private fun BoxScope.TopBarBlurBackground(
    state: CollapsingTopBarState,
    collapseFraction: Float,
    includeStatusBar: Boolean = false,
) {
    val surface = MiuixAppTheme.colorScheme.background
    val statusHeight = if (includeStatusBar) WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding() else 0.dp
    Box(
        modifier = Modifier.fillMaxWidth().height(statusHeight + 64.dp).align(Alignment.TopCenter)
            .graphicsLayer {
                alpha = collapseFraction.coerceIn(0f, 1f)
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(brush = Brush.verticalGradient(0f to Color.White, 0.76f to Color.White, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn)
            }
            .then(if (state.pageBackdrop != null)
                Modifier.miuixTopBarBlur(state.pageBackdrop, tintAlpha = 0.48f, tintColor = surface)
            else Modifier.background(surface))
    )
}

@Composable
private fun BoxScope.MiuixStatusBarBackground() {
    val top = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    val surface = MiuixAppTheme.colorScheme.background
    // Extend the same opaque page/top-bar surface behind the status icons. The
    // scroll-dependent bar/blur overlay above this spans this inset as well.
    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(top).background(surface))
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
        MiuixStatusBarBackground()
        TopBarBlurBackground(state, scrollProgress, includeStatusBar = true)
        Box(Modifier.fillMaxWidth().miuixHeaderSafeTop()) {
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
        MiuixStatusBarBackground()
        TopBarBlurBackground(state, fraction, includeStatusBar = true)
        Box(Modifier.fillMaxWidth().miuixHeaderSafeTop()) {
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
        MovingMiuixTitle(title, fraction)
        }
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
    showStatusBarBackground: Boolean = true,
) {
    val cs = MiuixAppTheme.colorScheme
    val fraction = if (compact) collapseFraction.coerceIn(0f, 1f) else state.collapseFraction
    val backIcon: @Composable () -> Unit = {
        Box(
            modifier = Modifier.padding(start = 12.dp).size(40.dp).clip(CircleShape)
                .background(cs.surfaceVariant.copy(alpha = 0.75f * fraction)).bounceClick { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = LocalStrings.current.backContentDescription, tint = cs.onBackground)
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        if (showStatusBarBackground) MiuixStatusBarBackground()
        TopBarBlurBackground(state, fraction, includeStatusBar = true)
        Box(Modifier.fillMaxWidth().miuixHeaderSafeTop()) {
        if (compact) {
            SmallTopAppBar(title = title, color = Color.Transparent, titleColor = Color.Transparent,
                scrollBehavior = state.scrollBehavior, defaultWindowInsetsPadding = false)
            Box(Modifier.align(Alignment.TopStart).height(56.dp), contentAlignment = Alignment.CenterStart) { backIcon() }
            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(56.dp).graphicsLayer { alpha = fraction },
                contentAlignment = Alignment.Center) {
                Text(title, color = cs.onBackground, style = MiuixAppTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 64.dp))
            }
        } else {
            TopAppBar(title = title, largeTitle = title, titleColor = Color.Transparent,
                largeTitleColor = Color.Transparent, color = Color.Transparent,
                scrollBehavior = state.scrollBehavior, defaultWindowInsetsPadding = false)
            MovingMiuixTitle(title, fraction)
            Box(Modifier.align(Alignment.TopStart).height(56.dp), contentAlignment = Alignment.CenterStart) { backIcon() }
        }
        }
    }
}
