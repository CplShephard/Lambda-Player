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
 * MADDE 1 / 8 — InstallerX Revived'ın `NavTransitions.MiuixDefault` geçişi, Lambda'ın
 * mevcut (androidx.navigation.compose) NavHost'una BİREBİR uygulanmış hâli.
 *
 * InstallerX, sayfalar arası geçişte Miuix'in `NavTransitions.MiuixDefault`'ını kullanır.
 * Bu geçiş `navGraphicsTransition` tabanlıdır ve şu davranışa sahiptir (InstallerX
 * kaynağından birebir):
 *
 *  - YENİ sayfa ekranın SAĞ KENARININ TAMAMEN DIŞINDAN (offsetX = +fullWidth) içeri kayar;
 *    solmaz (fade YOK), yani "tamamen sağdan gelen" bir kart gibi görünür.
 *  - ARKADAKİ (örtülen) sayfa aynı anda yerinde kalmaz: genişliğin ~1/4'ü kadar sola ötelenir
 *    (paralaks) ve SADECE %10 soluklaşır (alpha 1.0 → 0.9). Önceki uygulamadaki 0.65'lik
 *    solma "creepy" görünüme yol açıyordu; gerçek MiuixDefault yalnızca 0.9'a iner.
 *  - InstallerX, örtülen sayfanın ÜZERİNE ayrıca koyu bir scrim çizer (dimAmount = 0.5).
 *    Bu scrim Miuix'in NavDisplay host katmanındandır; Compose NavHost'ta ayrı bir katman
 *    olduğu için burada birebir taklit edilmemiştir — ama içerik alfası birebir aynıdır.
 *  - Geri dönüşte hareket birebir tersine oynar.
 *
 * Zamanlama: Miuix'in `NavMotion.Default` yayına yakın, overshoot'suz bir yay.
 */
object PageTransitions {

    /** Overshoot yapmayan, MIUIX hissiyatına yakın yay. */
    val slideSpec: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 400f,
        visibilityThreshold = IntOffset(1, 1)
    )

    private val fadeSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 400f
    )

    /** Arkada kalan sayfanın ne kadar sola ötelendiği (genişliğe oran). MiuixDefault: 1/4. */
    private const val PARALLAX = 4

    /** MiuixDefault: örtülen sayfa yalnızca %10 soluklaşır (1.0 → 0.9). */
    private const val COVERED_ALPHA = 0.9f

    /** İleri gidiş — yeni sayfa tamamen sağdan gelir. */
    val enterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { fullWidth -> fullWidth }

    /** İleri gidiş — eski sayfa paralaks ile hafifçe sola kayar ve çok az soluklaşır. */
    val exitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { fullWidth -> -fullWidth / PARALLAX } +
            fadeOut(animationSpec = fadeSpec, targetAlpha = COVERED_ALPHA)

    /** Geri dönüş — alttaki sayfa paralaks konumundan yerine döner (0.9 → 1.0). */
    val popEnterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { fullWidth -> -fullWidth / PARALLAX } +
            fadeIn(animationSpec = fadeSpec, initialAlpha = COVERED_ALPHA)

    /** Geri dönüş — üstteki sayfa tamamen sağa çıkar. */
    val popExitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { fullWidth -> fullWidth }
}
