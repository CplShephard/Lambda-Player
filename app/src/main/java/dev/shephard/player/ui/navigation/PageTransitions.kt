// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

/**
 * MADDE 1 / 8 — InstallerX Revived'ın `NavTransitions.MiuixDefault` geçişinin
 * resmi ve BİREBİR uygulanmış hâli.
 *
 *  - YENİ sayfa ekranın SAĞ KENARININ TAMAMEN DIŞINDAN (offsetX = +fullWidth, yani { it })
 *    içeri kayar; solmaz (fade YOK), yani "tamamen sağdan gelen" bir kart gibi görünür.
 *  - ARKADAKİ (örtülen) sayfa aynı anda yerinde kalmaz: genişliğin 1/4'ü kadar sola ötelenir
 *    (targetOffsetX = { -it / 4 }) ve alfası 0.9f olur.
 *  - Geri dönüşte birebir tersine oynar.
 */
object PageTransitions {

    val slideSpec: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = 450,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    )

    private val fadeSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = 450,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    )

    /** İleri gidiş — yeni menü sayfası %100 sağdan kayar, 0 fade. */
    val enterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { it }

    /** İleri gidiş — eski sayfa 1/4 sola paralaks kayar, %0.9 alfaya iner. */
    val exitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { -it / 4 } +
            fadeOut(animationSpec = fadeSpec, targetAlpha = 0.9f)

    /** Geri dönüş — alttaki sayfa 1/4 soldan yerine döner (0.9 -> 1.0). */
    val popEnterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { -it / 4 } +
            fadeIn(animationSpec = fadeSpec, initialAlpha = 0.9f)

    /** Geri dönüş — üstteki menü sayfası %100 sağa çıkar, 0 fade. */
    val popExitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { it }
}
