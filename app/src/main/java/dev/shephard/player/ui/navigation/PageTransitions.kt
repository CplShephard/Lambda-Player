package dev.shephard.player.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

/**
 * MADDE 8 — InstallerX / MIUIX "push" sayfa animasyonu.
 *
 * InstallerX, sayfalar arası geçişte miuix-nav'ın `NavTransitions.MiuixDefault`
 * geçişini kullanıyor. Davranışı şu:
 *
 *  - YENİ sayfa ekranın SAĞ KENARININ TAMAMEN DIŞINDAN (offsetX = +fullWidth) içeri
 *    kayar; solmaz (fade YOK), yani "tamamen sağdan gelen" bir kart gibi görünür.
 *  - ESKİ sayfa aynı anda yerinde kalmaz ama tamamen de çıkmaz: sadece genişliğin
 *    ~1/4'ü kadar sola öteler ve hafifçe soluklaşır (paralaks). Bu, iki sayfanın
 *    üst üste bindiği hissini verir — MIUI'nin karakteristik hareketi.
 *  - Geri dönüşte hareket birebir tersine oynar.
 *
 * Zamanlama: folme benzeri, overshoot'suz bir yay. `IntOffset` üzerinde çalıştığı için
 * geçiş sırasında ölçüm/yeniden çizim maliyeti düşük.
 */
object PageTransitions {

    /** Overshoot yapmayan, MIUIX hissiyatına yakın yay. */
    val slideSpec: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 320f,
        visibilityThreshold = IntOffset(1, 1)
    )

    private val fadeSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 320f
    )

    /** Arkada kalan sayfanın ne kadar sola ötelendiği (genişliğe oran). */
    private const val PARALLAX = 4

    /** İleri gidiş — yeni sayfa tamamen sağdan gelir. */
    val enterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { fullWidth -> fullWidth }

    /** İleri gidiş — eski sayfa paralaks ile hafifçe sola kayar ve soluklaşır. */
    val exitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { fullWidth -> -fullWidth / PARALLAX } +
            fadeOut(animationSpec = fadeSpec, targetAlpha = 0.65f)

    /** Geri dönüş — alttaki sayfa paralaks konumundan yerine döner. */
    val popEnterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { fullWidth -> -fullWidth / PARALLAX } +
            fadeIn(animationSpec = fadeSpec, initialAlpha = 0.65f)

    /** Geri dönüş — üstteki sayfa tamamen sağa çıkar. */
    val popExitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { fullWidth -> fullWidth }
}
