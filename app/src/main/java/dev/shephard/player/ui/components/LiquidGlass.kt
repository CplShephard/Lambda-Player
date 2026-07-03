package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Uygulama genelinde "Liquid Glass" (iOS 26 tarzı buzlu cam) görünümünün açık olup
 * olmadığını taşıyan CompositionLocal. MainActivity'de kullanıcı tercihinden set edilir.
 * Ayarlar dışındaki tüm composable'lar bunu okuyarak kart/buton stilini otomatik seçer.
 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }

/**
 * Liquid glass yüzeyinin görsel yoğunluğunu tanımlar. Farklı bileşenler (dock, kartlar,
 * butonlar) hafif farklı opaklık/blur ister, bu yüzden tek bir sabit yerine bir enum var.
 */
enum class GlassTint { SURFACE, ACCENT }

/**
 * Bir composable'a Liquid Glass stilini uygular. `LocalLiquidGlassEnabled` kapalıyken bu
 * modifier hiçbir şey yapmaz (normal Material3 kartına dokunmaz) — yani tüm çağıran yerler
 * hem eski hem yeni görünümü otomatik destekler, ayrı kod yolu yazmaya gerek kalmaz.
 *
 * Cam efekti iki katmandan oluşur:
 *  1. Üstten alta hafifleyen, yarı saydam bir dolgu (frosted/buzlu cam hissi)
 *  2. Üstte ince, parlak bir kenarlık (specular highlight) — camın kenarında ışık kırılıyormuş
 *     hissi verir, iOS 26'daki Liquid Glass'ın imza detayı budur.
 *
 * Not: Gerçek "backdrop blur" (arkadaki içeriği bulanıklaştırma) Compose'da ancak sibling
 * layer capture ile mümkün ve ciddi performans maliyeti getiriyor; onun yerine yarı saydamlık +
 * gradient + specular border kombinasyonu kullanıyoruz — iOS'un kendisi de düşük performans
 * modunda buna yakın bir yaklaşıma düşer.
 */
fun Modifier.liquidGlass(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: GlassTint = GlassTint.SURFACE
): Modifier = composed {
    if (!enabled) return@composed this

    val scheme = MaterialTheme.colorScheme
    val (fillTop, fillBottom) = when (tint) {
        GlassTint.SURFACE -> Pair(
            scheme.surface.copy(alpha = 0.42f),
            scheme.surface.copy(alpha = 0.18f)
        )
        GlassTint.ACCENT -> Pair(
            scheme.primary.copy(alpha = 0.34f),
            scheme.primary.copy(alpha = 0.15f)
        )
    }

    val glassBrush = Brush.verticalGradient(listOf(fillTop, fillBottom))
    val highlightBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.0f)
        )
    )

    this
        .clip(shape)
        .background(glassBrush, shape)
        .border(width = 1.dp, brush = highlightBrush, shape = shape)
}

/**
 * `liquidGlass` ile birebir aynı görsel dil ama varsayılan corner radius'u daha küçük —
 * dock, icon button gibi kompakt öğelerde kullanmak için kısayol.
 */
fun Modifier.liquidGlassLight(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(16.dp),
    tint: GlassTint = GlassTint.SURFACE
): Modifier = liquidGlass(enabled = enabled, shape = shape, tint = tint)
