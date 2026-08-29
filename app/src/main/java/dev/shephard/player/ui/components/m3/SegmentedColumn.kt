// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.shephard.player.ui.components.m3

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.shephard.player.ui.theme.ConnectionRadius
import dev.shephard.player.ui.theme.CornerRadius
import kotlin.math.roundToInt

private const val PADDING_HORIZONTAL = 16
private const val PADDING_VERTICAL = 8

private const val bouncyStiffness = 800f
private const val bouncyDamping = 0.5f

@DslMarker
annotation class SegmentedColumnDsl

@Immutable
data class SegmentedItemData(
    val key: Any?,
    val visible: Boolean,
    val customTopPadding: Dp? = null,
    val forceFlatTop: Boolean = false,
    val forceFlatBottom: Boolean = false,
    val content: @Composable (Shape) -> Unit
)

@SegmentedColumnDsl
class SegmentedColumnScope {
    val items = mutableListOf<SegmentedItemData>()

    fun item(
        key: Any? = null,
        animatedVisibility: Boolean = true,
        topPadding: Dp? = null,
        forceFlatTop: Boolean = false,
        forceFlatBottom: Boolean = false,
        content: @Composable (Shape) -> Unit
    ) {
        items.add(SegmentedItemData(key ?: items.size, animatedVisibility, topPadding, forceFlatTop, forceFlatBottom, content))
    }

    fun expandableItem(
        animatedVisibility: Boolean = true,
        expanded: Boolean,
        topPadding: Dp? = null,
        bottomPadding: Dp = 1.dp,
        topContent: @Composable (Shape) -> Unit,
        bottomContent: @Composable (Shape) -> Unit
    ) {
        item(
            animatedVisibility = animatedVisibility,
            topPadding = topPadding,
            forceFlatBottom = expanded,
            content = topContent
        )
        item(
            animatedVisibility = animatedVisibility && expanded,
            topPadding = bottomPadding,
            forceFlatTop = true,
            content = bottomContent
        )
    }
}

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    contentPadding: PaddingValues = PaddingValues(horizontal = PADDING_HORIZONTAL.dp, vertical = PADDING_VERTICAL.dp),
    content: SegmentedColumnScope.() -> Unit
) {
    val scope = SegmentedColumnScope().apply(content)
    val allItems = scope.items

    if (allItems.isEmpty()) return

    Column(modifier = modifier.padding(contentPadding)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = PADDING_HORIZONTAL.dp, top = PADDING_VERTICAL.dp, bottom = 16.dp)
            )
        }

        val floatSpring = spring<Float>(dampingRatio = bouncyDamping, stiffness = bouncyStiffness)
        val dpSpring = spring<Dp>(dampingRatio = bouncyDamping, stiffness = bouncyStiffness)

        val progresses = allItems.mapIndexed { index, item ->
            key(item.key ?: index) {
                animateFloatAsState(
                    targetValue = if (item.visible) 1f else 0f,
                    animationSpec = floatSpring,
                    label = "progress"
                )
            }
        }

        val firstVisibleIndex = allItems.indexOfFirst { it.visible }
        val lastVisibleIndex = allItems.indexOfLast { it.visible }

        Layout(
            content = {
                allItems.forEachIndexed { index, itemData ->
                    key(itemData.key ?: index) {
                        val isFirst = index == firstVisibleIndex || (index == 0 && !itemData.visible)
                        val isLast = index == lastVisibleIndex || (index == allItems.lastIndex && !itemData.visible)

                        val baseTopRadius = if (isFirst) CornerRadius else ConnectionRadius
                        val baseBottomRadius = if (isLast) CornerRadius else ConnectionRadius

                        val targetTopRadius = if (itemData.forceFlatTop) 0.dp else baseTopRadius
                        val targetBottomRadius = if (itemData.forceFlatBottom) 0.dp else baseBottomRadius

                        val animatedTopRadius by animateDpAsState(
                            targetValue = targetTopRadius,
                            animationSpec = dpSpring,
                            label = "topRadius"
                        )
                        val animatedBottomRadius by animateDpAsState(
                            targetValue = targetBottomRadius,
                            animationSpec = dpSpring,
                            label = "bottomRadius"
                        )

                        val dynamicShape = remember(animatedTopRadius, animatedBottomRadius) {
                            object : Shape {
                                override fun createOutline(
                                    size: androidx.compose.ui.geometry.Size,
                                    layoutDirection: LayoutDirection,
                                    density: Density
                                ): Outline {
                                    return RoundedCornerShape(
                                        topStart = animatedTopRadius,
                                        topEnd = animatedTopRadius,
                                        bottomStart = animatedBottomRadius,
                                        bottomEnd = animatedBottomRadius
                                    ).createOutline(size, layoutDirection, density)
                                }
                            }
                        }

                        val progress = progresses[index].value

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = progress
                                    scaleY = progress
                                    scaleX = 0.92f + (0.08f * progress)
                                    clip = false
                                }
                                .zIndex(if (itemData.visible) 1f else 0f)
                                .then(
                                    if (!itemData.visible) Modifier.semantics { hideFromAccessibility() }
                                    else Modifier
                                )
                        ) {
                            CompositionLocalProvider(LocalSegmentedItemShape provides dynamicShape) {
                                itemData.content(dynamicShape)
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }

            val itemSpacingPx = with(density) { 2.dp.roundToPx() }
            var totalHeight = 0
            val yPositions = IntArray(placeables.size)

            var isFirstItemPlaced = true

            placeables.forEachIndexed { index, placeable ->
                val progress = progresses[index].value
                val customPaddingPx = allItems[index].customTopPadding?.let { with(density) { it.roundToPx() } }

                if (progress > 0f) {
                    val spacing = when {
                        customPaddingPx != null -> (customPaddingPx * progress).roundToInt()
                        !isFirstItemPlaced -> (itemSpacingPx * progress).roundToInt()
                        else -> 0
                    }

                    totalHeight += spacing
                    yPositions[index] = totalHeight

                    val animatedItemHeight = (placeable.height * progress).roundToInt()
                    totalHeight += animatedItemHeight

                    isFirstItemPlaced = false
                } else {
                    yPositions[index] = totalHeight
                }
            }

            layout(constraints.maxWidth, totalHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val progress = progresses[index].value
                    if (progress > 0f) {
                        placeable.placeRelative(0, yPositions[index])
                    }
                }
            }
        }
    }
}
