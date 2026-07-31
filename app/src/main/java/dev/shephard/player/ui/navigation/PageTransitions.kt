package dev.shephard.player.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

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

    /** MiuixDefault: entering page slides in from 28% of width. */
    private const val ENTER_OFFSET = 0.28f
    /** MiuixDefault: covered page slides out 12% (parallax). */
    private const val EXIT_OFFSET = 0.12f
    /** MiuixDefault: covered page only dips to 0.9 alpha. */
    private const val COVERED_ALPHA = 0.9f
    /** MiuixDefault: entering page scales from 0.92. */
    private const val ENTER_SCALE = 0.92f
    /** MiuixDefault: covered page scales to 1.08. */
    private const val EXIT_SCALE = 1.08f

    /** İleri gidiş — yeni sayfa %28 sağdan kayar, %0.9 alfaya yükselir, %0.92'den ölçeklenir. */
    val enterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { (it * ENTER_OFFSET).toInt() } +
            fadeIn(animationSpec = fadeSpec, initialAlpha = COVERED_ALPHA) +
            scaleIn(animationSpec = fadeSpec, initialScale = ENTER_SCALE)

    /** İleri gidiş — eski sayfa %12 sola paralaks kayar, %0.9'a soluklaşır, %1.08'e ölçeklenir. */
    val exitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { (it * -EXIT_OFFSET).toInt() } +
            fadeOut(animationSpec = fadeSpec, targetAlpha = COVERED_ALPHA) +
            scaleOut(animationSpec = fadeSpec, targetScale = EXIT_SCALE)

    /** Geri dönüş — alttaki sayfa %12 sağdan yerine döner (0.9 → 1.0). */
    val popEnterPush: EnterTransition =
        slideInHorizontally(animationSpec = slideSpec) { (it * -EXIT_OFFSET).toInt() } +
            fadeIn(animationSpec = fadeSpec, initialAlpha = COVERED_ALPHA) +
            scaleIn(animationSpec = fadeSpec, initialScale = ENTER_SCALE)

    /** Geri dönüş — üstteki sayfa %28 sağa çıkar. */
    val popExitPush: ExitTransition =
        slideOutHorizontally(animationSpec = slideSpec) { (it * ENTER_OFFSET).toInt() } +
            fadeOut(animationSpec = fadeSpec, targetAlpha = COVERED_ALPHA) +
            scaleOut(animationSpec = fadeSpec, targetScale = EXIT_SCALE)
}
