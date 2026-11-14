// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.CarouselParallaxScrollEffectItemInfo
import androidx.compose.material3.carousel.CarouselParallaxScrollEffectState
import androidx.compose.material3.carousel.carouselParallaxScrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import dev.shephard.player.player.CarouselStyle

/**
 * Album-art pager with the Material 3 parallax carousel effect: pages crossing the edge of the
 * carousel are clipped to a rounded mask and their content slides at a slower rate.
 *
 * [carouselStyle] picks the page width: [CarouselStyle.NO_PEEK] fills the carousel,
 * [CarouselStyle.ONE_PEEK] leaves the next page peeking at the end, and [CarouselStyle.TWO_PEEK]
 * centres a smaller page between both neighbours. Each page is as tall as it is wide when the
 * caller sizes the carousel height to match. In RTL the pages run right to left and the mask follows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParallaxAlbumPager(
    state: PagerState,
    carouselStyle: String,
    carouselWidth: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 0.dp,
    itemCornerRadius: Dp = 16.dp,
    itemKey: ((page: Int) -> Any)? = null,
    content: @Composable (page: Int) -> Unit,
) {
    val layout = parallaxPageLayout(carouselStyle, carouselWidth)
    val layoutDirection = LocalLayoutDirection.current
    val parallaxState = remember(state, layoutDirection) {
        PagerParallaxScrollEffectState(state, isRtl = layoutDirection == LayoutDirection.Rtl)
    }
    val shape = remember(itemCornerRadius) { RoundedCornerShape(itemCornerRadius) }

    HorizontalPager(
        state = state,
        modifier = modifier.semantics { role = Role.Carousel },
        contentPadding = PaddingValues(horizontal = layout.sidePadding),
        pageSize = layout.pageWidth?.let { PageSize.Fixed(it) } ?: PageSize.Fill,
        pageSpacing = itemSpacing,
        beyondViewportPageCount = 1,
        flingBehavior = PagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(1)
        ),
        key = itemKey,
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .carouselParallaxScrollEffect(index = page, state = parallaxState, shape = shape)
        ) {
            content(page)
        }
    }
}

/** Page width and the padding on each side; a null [pageWidth] means pages fill the carousel. */
internal data class ParallaxPageLayout(val pageWidth: Dp?, val sidePadding: Dp)

/**
 * [CarouselStyle.NO_PEEK] pages fill the carousel, [CarouselStyle.ONE_PEEK] pages take 80% and
 * leave the next one peeking at the end, and [CarouselStyle.TWO_PEEK] centres a 60% page between
 * both neighbours.
 */
internal fun parallaxPageLayout(carouselStyle: String, carouselWidth: Dp): ParallaxPageLayout =
    when (carouselStyle) {
        CarouselStyle.NO_PEEK -> ParallaxPageLayout(pageWidth = null, sidePadding = 0.dp)
        CarouselStyle.TWO_PEEK -> ParallaxPageLayout(
            pageWidth = carouselWidth * 0.6f,
            sidePadding = carouselWidth * 0.2f
        )
        else -> ParallaxPageLayout(pageWidth = carouselWidth * 0.8f, sidePadding = 0.dp)
    }

/**
 * Feeds [carouselParallaxScrollEffect] from a [PagerState], which Material 3 has no state for.
 * See [parallaxItemInfo] for how it differs from Material's lazy-list state.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class PagerParallaxScrollEffectState(
    private val pagerState: PagerState,
    private val isRtl: Boolean,
) : CarouselParallaxScrollEffectState {

    override val orientation: Orientation
        get() = pagerState.layoutInfo.orientation

    override fun calculateItemInfo(index: Int): CarouselParallaxScrollEffectItemInfo {
        val layoutInfo = pagerState.layoutInfo
        val page = layoutInfo.visiblePagesInfo.fastFirstOrNull { it.index == index }
            ?: return UnmaskedItemInfo
        return parallaxItemInfo(
            pageOffset = page.offset.toFloat(),
            pageSize = layoutInfo.pageSize.toFloat(),
            crossAxisSize = layoutInfo.viewportSize.height.toFloat(),
            viewportStart = layoutInfo.viewportStartOffset.toFloat(),
            viewportEnd = layoutInfo.viewportEndOffset.toFloat(),
            isRtl = isRtl,
        )
    }
}

/**
 * Mask and parallax for a page at [pageOffset] from the start edge, the way lazy layouts measure
 * it. The parallax distance follows Material's lazy-list state. The mask differs: Material leaves
 * its rounded edge just past the viewport, which suits an edge-to-edge row, but the player's
 * carousel is inset, so the mask edge tracks the visible edge and pages keep their rounded corners
 * as they slide out.
 *
 * Offsets count from the start edge, but the mask and parallax are drawn left to right, so in RTL
 * the result is mirrored onto the page's physical sides.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun parallaxItemInfo(
    pageOffset: Float,
    pageSize: Float,
    crossAxisSize: Float,
    viewportStart: Float,
    viewportEnd: Float,
    isRtl: Boolean,
): CarouselParallaxScrollEffectItemInfo {
    if (pageSize == 0f) return UnmaskedItemInfo
    val parallaxDistance = pageSize * maskIntensity(pageSize, crossAxisSize)

    // Start-relative: how much of the page is past the start or end edge.
    val hiddenAtStart = (viewportStart - pageOffset).coerceIn(0f, pageSize)
    val hiddenAtEnd = (pageOffset + pageSize - viewportEnd).coerceIn(0f, pageSize)
    val maskStart: Float
    val maskEnd: Float
    val parallax: Float
    val progress: Float
    when {
        hiddenAtStart > 0f -> {
            progress = hiddenAtStart / pageSize
            maskStart = hiddenAtStart
            maskEnd = pageSize
            parallax = lerp(0f, parallaxDistance, progress)
        }
        hiddenAtEnd > 0f -> {
            progress = hiddenAtEnd / pageSize
            maskStart = 0f
            maskEnd = pageSize - hiddenAtEnd
            parallax = -lerp(0f, parallaxDistance, progress)
        }
        else -> {
            progress = 0f
            maskStart = 0f
            maskEnd = pageSize
            parallax = 0f
        }
    }

    return if (isRtl) {
        CarouselParallaxScrollEffectItemInfo(
            maskStart = pageSize - maskEnd,
            maskEnd = pageSize - maskStart,
            parallax = -parallax,
            maskProgress = progress,
        )
    } else {
        CarouselParallaxScrollEffectItemInfo(
            maskStart = maskStart,
            maskEnd = maskEnd,
            parallax = parallax,
            maskProgress = progress,
        )
    }
}

/** Material's "no item info" value: an empty mask tells the effect to leave the page unclipped. */
@OptIn(ExperimentalMaterial3Api::class)
internal val UnmaskedItemInfo = CarouselParallaxScrollEffectItemInfo(
    maskStart = 0f,
    maskEnd = 0f,
    parallax = 0f,
    maskProgress = 0f,
)

/** Wider pages are masked and shifted further: from 1/4 of the page when tall to 1/2 when 16:9. */
private fun maskIntensity(mainAxisSize: Float, crossAxisSize: Float): Float {
    if (crossAxisSize == 0f) return 1 / 4f
    val aspectRatio = mainAxisSize / crossAxisSize
    return when {
        aspectRatio >= 16 / 9f -> 1 / 2f
        aspectRatio > 1f -> lerp(1 / 3f, 1 / 2f, (aspectRatio - 1f) / (16 / 9f - 1f))
        aspectRatio > 9 / 16f -> lerp(1 / 4f, 1 / 3f, (aspectRatio - 9 / 16f) / (1f - 9 / 16f))
        else -> 1 / 4f
    }
}
