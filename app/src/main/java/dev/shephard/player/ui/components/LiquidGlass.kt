package dev.shephard.player.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
 * Cam efekti dört katmandan oluşur:
 *  1. Üstten alta hafifleyen, yarı saydam bir dolgu (frosted/buzlu cam hissi) — bu katman
 *     API 31+ cihazlarda gerçek bir blur (RenderEffect) ile yumuşatılarak sert gradient
 *     bantları yerine gerçekten "buzlu" bir dağılım verir.
 *  2. Camın üzerinden geçen diyagonal, hareketsiz bir "specular sweep" — iOS'taki gibi
 *     ışığın cam yüzeyinden yansıyormuş hissi verir.
 *  3. Kenarlarda ince, parlak bir highlight border (kırılma hissi).
 *  4. Altta hafif bir koyu gölge tonu (derinlik/iç gölge hissi).
 *
 * Not: Bu, ARKADAKİ içeriği (kartın altındaki scroll eden liste vs.) bulanıklaştıran gerçek
 * "backdrop blur" değildir — Compose'da bu ancak sibling layer capture ile mümkün ve ciddi
 * performans maliyeti getiriyor. Bunun yerine kartın KENDİ dolgu katmanını blur'luyoruz; bu,
 * düşük/orta performans modunda iOS'un kendisinin de yaptığı yaklaşıma yakın bir sonuç verir.
 */
fun Modifier.liquidGlass(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: GlassTint = GlassTint.SURFACE,
    topEdgeHighlight: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    val scheme = MaterialTheme.colorScheme
    val (fillTop, fillBottom, specular) = when (tint) {
        GlassTint.SURFACE -> Triple(
            scheme.surface.copy(alpha = 0.46f),
            scheme.surface.copy(alpha = 0.16f),
            Color.White
        )
        GlassTint.ACCENT -> Triple(
            scheme.primary.copy(alpha = 0.38f),
            scheme.primary.copy(alpha = 0.14f),
            Color.White
        )
    }

    val glassBrush = Brush.verticalGradient(listOf(fillTop, fillBottom))

    var result = this
        .clip(shape)
        // Dolgu katmanını AYRI bir offscreen grafik katmanında çiziyoruz ki API 31+'da
        // sadece bu katmana (specular sweep'e değil) gerçek bir blur (RenderEffect)
        // uygulayabilelim — sert gradient yerine gerçekten "buzlu camdan sızan ışık" hissi
        // veren yumuşak bir dağılım oluşur, üstteki ışık şeridi ise net kalır.
        .then(
            Modifier
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect
                            .createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                }
                .background(glassBrush, shape)
        )
        // Camın içinden ışık geçiyormuş hissi veren diyagonal specular sweep. Sabit (statik)
        // bir açıda, üst-sol köşeden hafif parlak bir şerit halinde geçer — iOS 26 Liquid
        // Glass'ın karakteristik "ışık kırılması" detayı. Bu katman blurlanmaz, net kalır.
        .drawWithContent {
            drawContent()
            rotate(degrees = -20f, pivot = Offset(size.width * 0.3f, size.height * 0.3f)) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            specular.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        start = Offset(-size.width * 0.2f, 0f),
                        end = Offset(size.width * 0.55f, 0f)
                    ),
                    topLeft = Offset(-size.width * 0.5f, -size.height * 0.5f),
                    size = androidx.compose.ui.geometry.Size(size.width * 2f, size.height * 2f)
                )
            }
            // Alt kenara hafif bir iç gölge — cama derinlik/kalınlık hissi katar.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.06f)),
                    startY = size.height * 0.7f,
                    endY = size.height
                )
            )
        }

    result = if (topEdgeHighlight) {
        // Standart cam kenarlığı: tüm kenarlar boyunca ince, parlak bir highlight.
        val highlightBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.6f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.0f)
            )
        )
        result.border(width = 1.dp, brush = highlightBrush, shape = shape)
    } else {
        // Üst kenarı düz (köşesiz) kartlarda -- ör. status bar'a yaslanan header -- üstte
        // yapay bir beyaz çizgi oluşmaması için highlight'ı yalnızca sol/sağ/alt kenarlara
        // uygularız; üst kenar tamamen çizilmeden bırakılır.
        result.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.0f),
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.06f)
                )
            ),
            shape = shape
        )
    }
    result
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

/**
 * ModalBottomSheet / AlertDialog gibi kendi opak `containerColor`'ı olan sistem
 * bileşenlerinde kullanmak için: sheet'in KENDİSİNİ şeffaf bırakıp bu modifier'ı sheet'in
 * içindeki kök Column/Box'a uygulayarak camı orada çiziyoruz. Böylece popup/drawer'lar da
 * diğer kartlarla aynı Liquid Glass diline sahip olur.
 */
fun Modifier.liquidGlassSheetSurface(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
): Modifier = liquidGlass(enabled = enabled, shape = shape, tint = GlassTint.SURFACE, topEdgeHighlight = false)
