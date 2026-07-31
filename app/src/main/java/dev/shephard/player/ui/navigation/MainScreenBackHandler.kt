// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
fun MainScreenBackHandler(
    mainPagerState: MainPagerState,
    navigator: Navigator,
) {
    val isPagerBackHandlerEnabled = navigator.current() is Route.Main && navigator.backStackSize() == 1 && mainPagerState.selectedPage != 0

    BackHandler(enabled = isPagerBackHandlerEnabled) {
        mainPagerState.animateToPage(0)
    }
}
