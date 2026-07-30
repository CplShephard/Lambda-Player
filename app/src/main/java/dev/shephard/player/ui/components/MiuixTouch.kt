package dev.shephard.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.TiltFeedback
import top.yukonga.miuix.kmp.utils.pressable

/**
 * MADDE 7 — InstallerX ekran görüntülerindeki "widget'ın basılan köşesinden gerçekçi
 * bükülmesi" efekti.
 *
 * Önceden burada elle yazılmış bir `graphicsLayer` + `detectTapGestures` çözümü vardı:
 * `transformOrigin`ı parmağın DEĞDİĞİ noktaya alıp aynı anda hem ölçek küçültüyor hem
 * eğiyordu. Sonuç Miuix'inkine benzemiyordu — kart parmağın altına doğru "kaçıyordu".
 *
 * InstallerX'in yeşil durum kartında (MiuixHomePage) kullandığı şey aslında Miuix'in
 * kendi `Card(pressFeedbackType = PressFeedbackType.Tilt)` bileşenidir. O da arka planda
 * `top.yukonga.miuix.kmp.utils.TiltFeedback`'i bir `IndicationNodeFactory` olarak
 * `Modifier.pressable(...)` üzerinden bağlar. Kritik farklar:
 *
 *  - `transformOrigin` parmağın değdiği köşe DEĞİL, onun KARŞI köşesidir
 *    (x < w/2 → pivot 1f). Kağıdı karşı köşesinden tutup basılan köşeyi içeri
 *    bastırmak gibi; bükülme bu yüzden gerçekçi hissettiriyor.
 *  - Ölçek değişmez, sadece `rotationX`/`rotationY` uygulanır ve `cameraDistance`
 *    12 * density gibi çok kısa tutulur (güçlü perspektif = belirgin büküm).
 *  - Yay: `spring(0.6f, 400f)`.
 *
 * Bu yüzden kendi taklidimizi silip DOĞRUDAN Miuix'in `TiltFeedback`'ini kullanıyoruz.
 * Böylece Music/Playlist kartları, Settings kartları ve Total Listening Time kartı
 * InstallerX ile birebir aynı davranışa sahip oluyor.
 *
 * Not: `pressScale` / `maxTiltDegrees` parametreleri geriye dönük çağrı uyumluluğu için
 * duruyor; `maxTiltDegrees` doğrudan Miuix'in `tiltAmount` değeri olarak geçiriliyor.
 */
fun Modifier.miuixWidgetClick(
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") pressScale: Float = 0.94f,
    maxTiltDegrees: Float = 8f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val tiltFeedback = remember(maxTiltDegrees) { TiltFeedback(tiltAmount = maxTiltDegrees) }

    this
        .pressable(
            interactionSource = interactionSource,
            indication = tiltFeedback,
            enabled = enabled,
            delay = null
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Miuix `PressFeedbackType.Sink` karşılığı: basılınca hafifçe içeri çöker, eğilmez.
 * Tilt'in uygun olmadığı (ör. tam ekran genişliğinde ince satırlar) yerler için.
 */
fun Modifier.miuixSinkClick(
    enabled: Boolean = true,
    sinkAmount: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val sinkFeedback = remember(sinkAmount) { SinkFeedback(sinkAmount = sinkAmount) }

    this
        .pressable(
            interactionSource = interactionSource,
            indication = sinkFeedback,
            enabled = enabled,
            delay = null
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/** Simple non-bending press feedback for compact controls such as MiniPlayer buttons. */
fun Modifier.pressScaleClick(
    enabled: Boolean = true,
    pressScale: Float = 0.92f,
    onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 650f
        ),
        label = "pressScaleClick"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled, onClick) {
            detectTapGestures(
                onPress = {
                    if (enabled) {
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) onClick()
                    }
                }
            )
        }
}
