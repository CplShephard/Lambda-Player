package dev.shephard.player.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.theme.PredictiveBackExitDirection
import dev.shephard.player.ui.animation.predictiveback.installerNavTransition
import dev.shephard.player.ui.components.LocalPlaylistDetailOpen
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.entryProvider
import top.yukonga.miuix.kmp.nav.transition.NavTransition

/**
 * The predictive back wiring that NavGraph uses for its submenus, exposed so playlist detail screens
 * get exactly the same transitions (AOSP, Scale, background reveal, ...).
 */
class SubmenuNavConfig(
    val animation: PredictiveBackAnimation,
    val transition: NavTransition,
    val effects: NavDisplayEffects,
)

val LocalSubmenuNavConfig = staticCompositionLocalOf<SubmenuNavConfig?> { null }

private object PlaylistListRoute : NavKey
private object PlaylistDetailRoute : NavKey

/**
 * Hosts a playlist list page plus its detail inside a local NavDisplay, so the detail is a real
 * back-stack entry: the list stays behind it during a back gesture (background reveal) and the
 * transition comes from the same [SubmenuNavConfig] the settings submenus use.
 *
 * [openIndex] is the requested detail (null = list). [onClosed] is called when the detail is popped
 * by a back gesture or back press, so the caller can clear its own state. [content] receives the
 * detail index, or null for the list page.
 */
@Composable
fun PlaylistDetailNavHost(
    openIndex: Int?,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable (Int?) -> Unit,
) {
    val config = LocalSubmenuNavConfig.current

    // Fallback wiring when no NavGraph is above us (e.g. previews). Matches the default MIUIX style.
    val fallbackTransition = remember {
        installerNavTransition(PredictiveBackAnimation.MIUIX, PredictiveBackExitDirection.FOLLOW_GESTURE)
    }
    val fallbackBackdrop = MaterialTheme.colorScheme.background
    val fallbackEffects = remember(fallbackBackdrop) {
        NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = 0.dp,
            cornerClipMode = NavCornerClipMode.Leading,
            dimAmount = 0.5f,
            backdropColor = fallbackBackdrop,
            blockInputDuringTransition = false,
        )
    }
    val animation = config?.animation ?: PredictiveBackAnimation.MIUIX
    val transition = config?.transition ?: fallbackTransition
    val effects = config?.effects ?: fallbackEffects

    val currentContent by rememberUpdatedState(content)
    val currentOnClosed by rememberUpdatedState(onClosed)

    val initialIndex = openIndex
    val backStack = remember {
        mutableStateListOf<NavKey>(PlaylistListRoute).apply {
            if (initialIndex != null) add(PlaylistDetailRoute)
        }
    }
    var detailIndex by remember { mutableIntStateOf(initialIndex ?: -1) }

    // Follow the caller's requested detail. Pushes and programmatic pops animate like a normal back.
    LaunchedEffect(openIndex) {
        if (openIndex != null) {
            if (backStack.size > 1 && detailIndex == openIndex) return@LaunchedEffect
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            // Set the index before the entry exists so the detail never renders with a stale index.
            detailIndex = openIndex
            backStack.add(PlaylistDetailRoute)
        } else if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun popDetail() {
        if (backStack.size <= 1) return
        backStack.removeAt(backStack.lastIndex)
        currentOnClosed()
    }

    val entryProvider = remember(backStack, animation) {
        entryProvider {
            entry<PlaylistListRoute> {
                currentContent(null)
            }
            entry<PlaylistDetailRoute> {
                // NONE: no predictive preview, the entry takes the gesture itself (same as NavGraph submenus).
                NavEntryBackHandler(
                    interceptPredictiveBack = animation == PredictiveBackAnimation.NONE && backStack.size > 1,
                    onBack = ::popDetail,
                ) {
                    currentContent(detailIndex)
                }
            }
        }
    }

    // The bottom stack (global mini + dock) is hidden while a detail is on the stack.
    val detailActive = backStack.size > 1
    val detailOpen = LocalPlaylistDetailOpen.current
    SideEffect { detailOpen.value = detailActive }
    DisposableEffect(detailOpen) {
        onDispose { detailOpen.value = false }
    }

    NavDisplay(
        backStack,
        entryProvider,
        modifier,
        ::popDetail,
        transition,
        effects,
    )
}
