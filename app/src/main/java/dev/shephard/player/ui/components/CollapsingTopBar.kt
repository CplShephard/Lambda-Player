package dev.shephard.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Color
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Text
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
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
    return remember(scrollBehavior) { CollapsingTopBarState(scrollBehavior) }
}

class CollapsingTopBarState(val scrollBehavior: ScrollBehavior) {
    /** 0f = tam açık (büyük başlık görünür), 1f = tam kapalı (sadece app bar'daki küçük başlık). */
    val collapseFraction: Float
        get() = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
}

@Composable
fun CollapsingTopBar(
    title: String,
    state: CollapsingTopBarState,
    modifier: Modifier = Modifier,
) {
    val scrollProgress = state.collapseFraction
    SmallTopAppBar(
        title = title,
        modifier = modifier,
        color = Color.Transparent,
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
