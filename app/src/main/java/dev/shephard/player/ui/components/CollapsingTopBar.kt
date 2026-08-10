package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.miuixBlurSurface
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.compose.ui.text.font.FontWeight

/**
 * MADDE 4 — Music / Playlists / Settings artık "LAMBDA PLAYER" marka kartı (eski
 * BrandHeader) kullanmıyor. O kart `Scaffold`'un `topBar`'ı olarak SABİT bir yükseklikle
 * render ediliyordu; Theme/Player/About alt sayfalarına geçilince bu yükseklik aniden
 * değişip page transition'ın "kartın altına ışınlanıp kapanma" gibi bozuk görünmesine
 * yol açıyordu — kart page transition'a kendini bir ekran sınırı gibi dayatıyordu.
 *
 * Çözüm: `Scaffold.topBar` tamamen kaldırıldı (bkz. MainContainer.kt). Üç ana sekme de
 * artık Theme/Playback/About ayar sayfalarının kullandığı BİREBİR AYNI başlık desenini
 * kullanıyor — `SmallTopAppBar`, kaydırdıkça küçülüp ortalanan büyük başlık, aynı
 * `MiuixScrollBehavior`. Fark: `SettingsPageScaffold` `Column.verticalScroll` kullanırken,
 * bu bileşen sadece üstteki app bar'ı ve `nestedScrollConnection`'ı sağlıyor — asıl
 * içerik (LazyColumn/LazyVerticalGrid) çağıran ekranın kendi scrollable'ı olarak kalıyor,
 * böylece iç içe iki dikey scrollable olmaktan kaçınılıyor.
 *
 * Kullanım:
 * ```
 * val topBar = rememberCollapsingTopBarState()
 * LazyColumn(modifier = Modifier.nestedScroll(topBar.scrollBehavior.nestedScrollConnection)) {
 *     item { CollapsingPageTitle(title = "Music", state = topBar) }
 *     ...
 * }
 * CollapsingTopBar(title = "Music", state = topBar)
 * ```
 */
@Composable
fun rememberCollapsingTopBarState(): CollapsingTopBarState {
    val scrollBehavior = MiuixScrollBehavior()
    // InstallerX'teki gibi: kaydırınca küçülen top bar'ın gerçek blur alması için, SAYFAYA
    // ÖZEL bir backdrop kuruluyor (bkz. rememberMiuixBlurBackdrop / MiuixThemeSettingsPage.kt
    // referans alındı). Bu, dock/MiniPlayer'ın kullandığı `LocalAppBackdrop` (sadece sabit
    // wallpaper, tüm uygulama için TEK ve ucuz) ile KARIŞTIRILMAMALI — o global backdrop'u
    // burada kullanmıyoruz çünkü top bar'ın arkasında görünmesi gereken şey wallpaper değil,
    // o AN görüntülenen sayfanın kayan içeriğidir (InstallerX'te de böyle). Sadece bu sayfa
    // aktifken var olduğu için (composable dispose olunca kaybolur), performans maliyeti
    // dock'unkinden farklı olarak TEK bir sayfayla sınırlı — tüm navigasyon boyunca sürekli
    // canlı kalan global bir capture değil.
    val liquidGlassOn = LocalBlurEnabled.current
    val pageBackdrop = if (liquidGlassOn) rememberLayerBackdrop() else null
    return remember(scrollBehavior, pageBackdrop) { CollapsingTopBarState(scrollBehavior, pageBackdrop) }
}

class CollapsingTopBarState(
    val scrollBehavior: ScrollBehavior,
    val pageBackdrop: LayerBackdrop? = null,
) {
    /** 0f = tam açık (büyük başlık görünür), 1f = tam kapalı (sadece app bar'daki küçük başlık). */
    val collapseFraction: Float
        get() = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
}

/**
 * Sayfanın kayan içeriğini (LazyColumn/LazyVerticalGrid) top bar'ın blur kaynağına kaydeder.
 * Ekranın kendi scrollable'ına `Modifier.thenIfPageBackdrop(state)` şeklinde eklenmeli —
 * InstallerX'teki `Modifier.layerBackdrop(topBarBackdrop)` çağrısının karşılığı.
 */
fun Modifier.captureForTopBarBlur(state: CollapsingTopBarState): Modifier =
    state.pageBackdrop?.let { this.then(Modifier.layerBackdrop(it)) } ?: this

@Composable
fun CollapsingTopBar(
    title: String,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
) {
    val scrollProgress = state.collapseFraction
    SmallTopAppBar(
        title = title,
        // InstallerX'teki gibi: backdrop varsa app bar arka planı TRANSPARENT bırakılır,
        // gerçek blur aşağıdaki .miuixBlurSurface() ile geliyor. Backdrop yoksa (blur kapalı
        // ya da desteklenmiyor) eski düz-renk-fade davranışına düşülür.
        modifier = modifier
            .then(
                if (state.pageBackdrop != null) {
                    Modifier.miuixBlurSurface(
                        backdrop = state.pageBackdrop,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        blurRadius = 70f,
                        tintAlpha = if (scrollProgress > 0.01f) (0.68f + scrollProgress * 0.27f).coerceIn(0f, 0.95f) else 0f,
                        fallbackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                } else Modifier
            ),
        color = if (state.pageBackdrop != null)
            androidx.compose.ui.graphics.Color.Transparent
        else
            MiuixAppTheme.colorScheme.background.copy(alpha = scrollProgress),
        titleColor = MiuixAppTheme.colorScheme.onBackground.copy(alpha = scrollProgress),
        scrollBehavior = state.scrollBehavior,
        // SmallTopAppBar zaten WindowInsets.systemBars(Top)'u koşulsuz kendi uyguluyor;
        // bu parametre sadece yatay (notch/nav bar) insets'i kontrol ediyor.
        defaultWindowInsetsPadding = false,
    )
}

/**
 * Büyük, kaydırdıkça solup küçülen sayfa başlığı — Theme/Playback/About'taki ile aynı.
 * Ekranın kendi LazyColumn/LazyVerticalGrid'inin İLK item'ı olarak eklenmeli.
 */
@Composable
fun CollapsingPageTitle(
    title: String,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
) {
    val scrollProgress = state.collapseFraction
    Text(
        text = title,
        style = MiuixAppTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MiuixAppTheme.colorScheme.onBackground,
        // ÖNEMLİ: bu satır eksikti. scrollProgress hesaplanıyordu ama hiçbir yerde
        // kullanılmıyordu, bu yüzden büyük başlık kaydırırken hiç solmuyor/küçülmüyordu —
        // sürekli tam opak kalıp app bar'daki küçük başlıkla çakışıyordu. SettingsPageScaffold'daki
        // (Theme/Playback/About) çalışan davranışla birebir aynı: kaydırdıkça solup kayboluyor,
        // yerini üstteki ortalanmış küçük başlığa bırakıyor.
        modifier = modifier.graphicsLayer { alpha = 1f - scrollProgress }
    )
}

/**
 * InstallerX'in Theme/Installer/Uninstaller ayar sayfalarındaki "large header" deseninin
 * Miuix karşılığı.
 *
 * InstallerX bu sayfalarda Material3 `LargeFlexibleTopAppBar` + `exitUntilCollapsedScrollBehavior`
 * kullanıyor: sayfa başlığı SOL ÜSTTE büyük (headline) olarak durur, kaydırdıkça küçülüp app bar'ın
 * ORTASINDA küçük başlık olarak belirir. Miuix kütüphanesinin `TopAppBar`'ı `largeTitle` parametresiyle
 * bu davranışı birebir sağlıyor — bu bileşen, ana sekmelerin (Music/Playlists/Settings) başlık desenini
 * bu InstallerX tarzına taşır.
 *
 * Kullanım: içerik bir Miuix `Scaffold`'un `topBar`'ına verilir; içerik `state.scrollBehavior`'ın
 * `nestedScrollConnection`'ı ile `nestedScroll` edilir.
 */
@Composable
fun InstallerXTopBar(
    title: String,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
    subtitle: String = "",
) {
    val cs = MiuixAppTheme.colorScheme
    val collapseFraction = state.collapseFraction
    TopAppBar(
        title = title,
        largeTitle = title,
        largeTitleColor = cs.onBackground,
        titleColor = cs.onBackground.copy(alpha = collapseFraction),
        subtitle = subtitle,
        subtitleColor = cs.onSurfaceVariant,
        // InstallerX'teki gibi: backdrop varsa TRANSPARENT + gerçek blur (.miuixBlurSurface),
        // yoksa (blur kapalı/desteklenmiyor) eski düz-renk-fade davranışı.
        color = if (state.pageBackdrop != null)
            androidx.compose.ui.graphics.Color.Transparent
        else
            cs.background.copy(alpha = collapseFraction),
        scrollBehavior = state.scrollBehavior,
        defaultWindowInsetsPadding = false,
        modifier = modifier
            .then(
                if (state.pageBackdrop != null) {
                    Modifier.miuixBlurSurface(
                        backdrop = state.pageBackdrop,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        blurRadius = 70f,
                        tintAlpha = if (collapseFraction > 0.01f) (0.68f + collapseFraction * 0.27f).coerceIn(0f, 0.95f) else 0f,
                        fallbackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                } else Modifier
            ),
    )
}

/**
 * Theme/Playback/About ayar sayfalarının (SettingsPageScaffold) kullandığı header'ın aynısı:
 * `SmallTopAppBar` + geri tuşu (navigationIcon) + kaydırdıkça ortada beliren küçük başlık.
 *
 * playlist detail ekranının (PlaylistDetailView) başlığını bu desene taşımak için kullanılıyor —
 * böylece submenu olan playlist detail, Theme/Playback/About ile birebir aynı header'a sahip olur.
 */
@Composable
fun SubmenuTopBar(
    title: String,
    onBack: () -> Unit,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
) {
    val cs = MiuixAppTheme.colorScheme
    val collapseFraction = state.collapseFraction
    SmallTopAppBar(
        title = title,
        modifier = modifier
            .then(
                if (state.pageBackdrop != null) {
                    Modifier.miuixBlurSurface(
                        backdrop = state.pageBackdrop,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        blurRadius = 70f,
                        tintAlpha = if (collapseFraction > 0.01f) (0.68f + collapseFraction * 0.27f).coerceIn(0f, 0.95f) else 0f,
                        fallbackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                } else Modifier
            ),
        color = if (state.pageBackdrop != null)
            androidx.compose.ui.graphics.Color.Transparent
        else
            cs.background.copy(alpha = collapseFraction),
        titleColor = cs.onBackground.copy(alpha = collapseFraction),
        scrollBehavior = state.scrollBehavior,
        defaultWindowInsetsPadding = false,
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceVariant.copy(alpha = 0.75f))
                    .bounceClick { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = cs.onBackground
                )
            }
        }
    )
}
