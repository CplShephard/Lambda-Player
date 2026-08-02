package dev.shephard.player.ui.navigation

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * `androidx.navigation3.animation.NavTransitionEasing`'in birebir kopyası.
 *
 * Orijinali `internal` olduğu için (miuix-navigation3-ui modülü, paket dışından erişilemez)
 * Lambda Player'ın kendi paketine taşındı. Theme/Playback/About sayfaları (herhangi bir
 * özel `transitionSpec` VERMEDİKLERİ için) bu easing'i kullanan `NavDisplay`'in kendi
 * dahili varsayılan geçişini kullanıyor — playlist detayına girme animasyonunu da bununla
 * BİREBİR AYNI yapmak için bu sınıfa ihtiyaç var.
 *
 * Matematiksel olarak kritik-sönümlü (critically damped) bir yay eğrisidir: [response]
 * saniye cinsinden karakteristik süreyi, [damping] sönümleme oranını belirler. Gerçek
 * Miuix NavDisplay'de `NavTransitionEasing(0.8f, 0.95f)` olarak kullanılıyor.
 */
@Immutable
internal class NavTransitionEasing(
    response: Float,
    damping: Float,
) : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response

        w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = exp(r * t)
        return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }

    fun inverseTransform(fraction: Float, tolerance: Float = 1e-6f): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f

        var low = 0f
        var high = 1f
        var mid = 0f

        repeat(16) {
            mid = (low + high) / 2f
            val value = transform(mid)
            if (abs(value - fraction) < tolerance) return mid

            if (value < fraction) {
                low = mid
            } else {
                high = mid
            }
        }
        return mid
    }
}

/** Gerçek Miuix NavDisplay'in kullandığı sabit değer: `NavTransitionEasing(0.8f, 0.95f)`. */
internal val NavAnimationEasing = NavTransitionEasing(0.8f, 0.95f)
