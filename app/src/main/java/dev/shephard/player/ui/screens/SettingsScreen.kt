@file:OptIn(ExperimentalFoundationApi::class, dev.shephard.player.ui.miuix.ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.foundation.ExperimentalFoundationApi

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.miuixWidgetClick
import dev.shephard.player.ui.components.overScrollVertical
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import dev.shephard.player.ui.miuix.Card
import dev.shephard.player.ui.miuix.CardDefaults
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.rememberDrawerDismiss
import dev.shephard.player.ui.miuix.Slider
import dev.shephard.player.ui.miuix.SliderDefaults
import dev.shephard.player.ui.miuix.Switch
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.shephard.player.ui.miuix.SwitchDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shephard.player.R
import top.yukonga.miuix.kmp.basic.Card as MiuixNativeCard
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.ThemeModePreference
import dev.shephard.player.ui.components.CustomColorPickerDialog
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.bgeffect.BgEffectBackground
import dev.shephard.player.ui.i18n.AllLanguages
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

/** Preset accent colors. The 7th slot is "Custom" — handled separately. */
private val AccentPalette = listOf(
    0xFF22C55E.toInt(), // green (lambda)
    0xFF3B82F6.toInt(), // blue
    0xFFE11D48.toInt(), // rose
    0xFFF59E0B.toInt(), // amber
    0xFF8B5CF6.toInt(), // violet
    0xFF14B8A6.toInt(), // teal
)

@Composable
fun SettingsScreen(
    playerViewModel: PlayerViewModel = viewModel(),
    onOpenThemeSettings: () -> Unit = {},
    onOpenPlayerSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val strings = LocalStrings.current

    val topBarState = dev.shephard.player.ui.components.rememberCollapsingTopBarState()

    // MADDE 4+ — Settings artık InstallerX tarzı LARGE header kullanıyor (başlık sol üstte büyük,
    // kaydırdıkça app bar'ın ortasında küçülüp belirir).
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            dev.shephard.player.ui.components.InstallerXTopBar(
                title = strings.settings,
                state = topBarState
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = innerPadding.calculateTopPadding() + 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // InstallerX tarzı ana Settings: üstte widget kart, altında üç büyük yönlendirme.
                TotalListeningTimeCard(playerViewModel = playerViewModel)

                SettingsNavigationCard(
                    icon = Icons.Filled.ColorLens,
                    title = strings.themeSettings,
                    summary = "Colors, wallpaper, Liquid Glass, layout and language",
                    onClick = onOpenThemeSettings
                )
                SettingsNavigationCard(
                    icon = Icons.Filled.MusicNote,
                    title = strings.playbackSettings,
                    summary = "Crossfade, gapless playback and audio focus",
                    onClick = onOpenPlayerSettings
                )
                SettingsNavigationCard(
                    icon = Icons.Filled.Info,
                    title = "About Lambda Player",
                    summary = "Version, project links and credits",
                    onClick = onOpenAbout
                )

                Spacer(Modifier.height(110.dp))
            }
        }
    }
}

@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val accent by prefs.accentColor.collectAsState(initial = AccentPalette.first())
    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = 0.55f)
    val cardAlpha by prefs.cardAlpha.collectAsState(initial = 0.85f)
    val language by prefs.language.collectAsState(initial = "en")
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeModePreference.LIGHT)
    val dynamicColor by prefs.dynamicColor.collectAsState(initial = false)
    val liquidGlassEnabled by prefs.liquidGlassEnabled.collectAsState(initial = false)
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)

    var langMenuOpen by remember { mutableStateOf(false) }
    var customPickerOpen by remember { mutableStateOf(false) }
    // ÖNEMLİ — Wallpaper Brightness slider'ı BAŞTAN yazıldı.
    // Eski kod: `wallpaperBrightnessValue by remember(wallpaperBrightness) { ... }` ile
    // DataStore değerinden başlatılıyordu, AMA `onValueChangeFinished` içinde
    // `prefs.setWallpaperBrightness(1f - wallpaperBrightnessValue)` ile TERSİ kaydediliyordu.
    // Sonuç: kullanıcı slider'ı bıraktığı anda DataStore değeri değişiyor, `remember(wallpaperBrightness)`
    // yeniden tetikleniyor ve slider'ın kendisi TERS değere zıplıyordu — slider'ın her
    // bırakışta konumunun değişmesi ("berbat çalışıyor") sorununun kök nedeni buydu.
    // Ters çevirme kaldırıldı, ama key'siz `remember { }` YENİ bir soruna yol açtı: bu
    // composable'ın composition'ı navigation boyunca KORUNUYOR (route değişince yok
    // edilmiyor), bu yüzden local state SADECE bu ekran hayatı boyunca ilk kez compose
    // edildiği anda DataStore'dan bir kez okunuyordu — Theme Settings'ten çıkıp tekrar
    // girildiğinde her seferinde aynı (ilk) değeri göstermeye devam ediyordu, gerçek
    // DataStore değeri farklı olsa bile. Çözüm: her girişte (composable'ın recompose
    // DEĞİL, gerçekten ekrana gelme anı) LaunchedEffect(Unit) ile local state'i DataStore'daki
    // GÜNCEL değerle bir kez senkronize ediyoruz. Bu, sürüklerken slider'ın DataStore'un ara
    // emisyonlarıyla "zıplamasını" önler (senkron sadece giriş anında olur, her emit'te değil)
    // ve her girişte doğru konumu gösterir.
    var wallpaperBrightnessValue by remember { mutableFloatStateOf(wallpaperBrightness) }
    // İlk yükleme & DataStore değeri değiştiğinde (ör. başka yerden duvar kağıdı
    // silindiğinde/ayarlandığında) local slider state'ini senkronize et. DataStore sadece
    // `onValueChangeFinished`'te (bırakınca) yazıldığı için sürükleme sırasında emisyon olmaz,
    // dolayısıyla slider buradan "zıplamaz".
    LaunchedEffect(wallpaperBrightness) {
        wallpaperBrightnessValue = wallpaperBrightness
    }
    // KRİTİK — Theme Settings'ten çıkıp tekrar girildiğinde composable'ın composition'ı
    // navigation boyunca KORUNUYOR (route değişince yok edilmiyor), bu yüzden `remember` ve
    // `LaunchedEffect(wallpaperBrightness)` yeniden çalışmıyor (değer değişmediyse). Sonuç:
    // local slider state'i ESKİ değerde takılı kalıyor, gerçek DataStore değeri farklı olsa bile
    // ("değer farklı ama hep aynı yerde duruyor" şikayeti). Çözüm: ekran her gerçekten görünür
    // olduğunda (ON_RESUME) local state'i DataStore'daki GÜNCEL değerle bir kez senkronize et.
    // Sürükleme sırasında ekran zaten RESUMED olduğu için bu callback sürüklerken tetiklenmez.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        wallpaperBrightnessValue = wallpaperBrightness
    }

    // MADDE 4 — duvar kağıdı seçerken de kapak seçimindeki gibi sistem cropper
    // (com.android.camera.action.CROP) kullanılsın. Önce görsel seçilir, sonra crop
    // intent'i ile kırpılır; kırpılan çıktı kalıcı depolamaya yazılır.
    var wallpaperCropOutputUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val wallpaperCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = wallpaperCropOutputUri
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            scope.launch { prefs.setWallpaperUri(output.toString()) }
        }
    }

    fun launchWallpaperCrop(sourceUri: android.net.Uri) {
        val dir = java.io.File(context.filesDir, "persisted_wallpaper").apply { mkdirs() }
        val file = java.io.File(dir, "wallpaper_${System.currentTimeMillis()}.jpg")
        val outputUri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        wallpaperCropOutputUri = outputUri

        // Cihazın GERÇEK ekran çözünürlüğünü al — hem outputX/Y hem de aspectX/Y bundan
        // türetiliyor, böylece kırpma kutusu ekranın tam en-boy oranına kilitleniyor.
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val cropIntent = android.content.Intent("com.android.camera.action.CROP").apply {
            setDataAndType(sourceUri, "image/*")
            putExtra("crop", "true")
            putExtra("scale", "true")
            // ÖNEMLİ: aspectX/aspectY eklenince kırpma kutusu ekranın en-boy oranına
            // KİLİTLENİR (serbest/"free scale" değil, "fullscreen scale" davranışı).
            // Önceden sadece outputX/Y veriliyordu ve "scale":"true" ile birlikte
            // kullanıcıya serbestçe herhangi bir orana çekilebilen bir kırpma kutusu
            // sunuluyordu — duvar kağıdı bu yüzden ekranı tam kaplamayabiliyordu.
            putExtra("aspectX", screenWidth)
            putExtra("aspectY", screenHeight)
            putExtra("outputX", screenWidth)
            putExtra("outputY", screenHeight)
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, outputUri)
            putExtra("outputFormat", android.graphics.Bitmap.CompressFormat.JPEG.toString())
            putExtra("return-data", false)
            putExtra("noFaceDetection", true)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "wallpaper", sourceUri)
        }
        val resolved = context.packageManager.queryIntentActivities(cropIntent, 0)
        for (info in resolved) {
            val pkg = info.activityInfo?.packageName ?: continue
            try {
                context.grantUriPermission(
                    pkg, outputUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
        }
        if (resolved.isNotEmpty()) {
            wallpaperCropLauncher.launch(cropIntent)
        } else {
            // Sistem cropper yoksa seçilen görseli olduğu gibi kalıcı depolamaya kopyala.
            scope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistWallpaper(context, sourceUri)
                prefs.setWallpaperUri((persisted ?: sourceUri).toString())
            }
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            launchWallpaperCrop(uri)
        }
    }

    SettingsPageScaffold(title = strings.themeSettings, onBack = onBack) {
        SectionCard {
            Text(strings.themeSettings, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            // MADDE 7 — eski ThemeModeSegmentedSwitch (Material tarzı üç butonlu segment)
            // yerine InstallerX Revived'ın kullandığı GERÇEK Miuix bileşeni:
            // WindowSpinnerPreference. Bu, gerçek Miuix animasyonlu dropdown pop-up'ını
            // (WindowDropdownPopup) kullanıyor — kullanıcının referans gösterdiği
            // "Light theme / Dark theme / Follow system theme" popup'ıyla birebir aynı.
            val themeModeOptions = listOf(
                ThemeModePreference.LIGHT to strings.lightMode,
                ThemeModePreference.DARK to strings.darkMode,
                ThemeModePreference.AUTO to strings.autoMode,
            )
            val themeModeSelectedIndex = themeModeOptions.indexOfFirst { it.first == themeMode }.coerceAtLeast(0)
            top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                title = strings.darkMode,
                items = themeModeOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.second) },
                selectedIndex = themeModeSelectedIndex,
                // ÖNEMLİ: BasicComponent (WindowSpinnerPreference'ın altında kullandığı
                // bileşen) kendi clickable/basma efektini `modifier` parametresine
                // doğrudan uyguluyor, kendi başına clip YAPMIYOR — bu yüzden basılı
                // tutulduğunda ripple/highlight kartın yuvarlak köşelerini yok sayıp
                // dikdörtgen (corner radius 0) görünüyordu. Burada clip ekliyoruz.
                modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setThemeMode(themeModeOptions[index].first) }
                }
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = strings.blurEffect, checked = liquidGlassEnabled) { enabled ->
                scope.launch { prefs.setLiquidGlassEnabled(enabled) }
            }

            Spacer(Modifier.height(12.dp))
            Text(strings.accentColor, style = MiuixAppTheme.typography.bodyMedium, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentPalette.forEach { argb ->
                    val selected = argb == accent
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .bounceClick(enabled = !dynamicColor) { scope.launch { prefs.setAccentColor(argb) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.28f))
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                listOf(
                                    Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFFACC15),
                                    Color(0xFF22C55E), Color(0xFF14B8A6), Color(0xFF3B82F6),
                                    Color(0xFF8B5CF6), Color(0xFFEF4444)
                                )
                            )
                        )
                        .bounceClick(enabled = !dynamicColor) { customPickerOpen = true }
                )
            }
        }

        SectionCard {
            Text(strings.wallpaper, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))
            if (wallpaper.isNotEmpty()) {
                var previewLoaded by remember(wallpaper) { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixAppTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = "Wallpaper preview",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        onState = { previewLoaded = it is AsyncImagePainter.State.Success }
                    )
                    if (!previewLoaded) Icon(Icons.Filled.BrokenImage, null, tint = MiuixAppTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
            }
            SettingsActionRow(
                icon = Icons.Filled.Image,
                title = if (wallpaper.isEmpty()) strings.chooseFromGallery else strings.changeWallpaper,
                onClick = { wallpaperPicker.launch(arrayOf("image/*")) }
            )
            if (wallpaper.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(strings.wallpaperBrightness, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = wallpaperBrightnessValue,
                    onValueChange = { wallpaperBrightnessValue = it },
                    onValueChangeFinished = { scope.launch { prefs.setWallpaperBrightness(wallpaperBrightnessValue) } },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MiuixAppTheme.colorScheme.primary,
                        activeTrackColor = MiuixAppTheme.colorScheme.primary,
                        inactiveTrackColor = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
                Text(
                    text = strings.removeWallpaper,
                    color = MiuixAppTheme.colorScheme.error,
                    modifier = Modifier.bounceClick { scope.launch { prefs.setWallpaperUri("") } }.padding(8.dp)
                )
            }
        }

        SectionCard {
            Text(strings.layout, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))
            // MADDE 7 — grid/list geçişi de artık InstallerX'in kullandığı gerçek Miuix
            // WindowSpinnerPreference (animasyonlu dropdown popup) ile yapılıyor, eski
            // Material tarzı LayoutToggleChip çiftleri yerine.
            val layoutModeOptions = listOf(
                LayoutMode.LIST to strings.list,
                LayoutMode.GRID to strings.grid,
            )
            val musicsLayoutIndex = layoutModeOptions.indexOfFirst { it.first == musicsLayout }.coerceAtLeast(0)
            top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                title = strings.musicsLayout,
                items = layoutModeOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.second) },
                selectedIndex = musicsLayoutIndex,
                modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setMusicsLayout(layoutModeOptions[index].first) }
                }
            )
            Spacer(Modifier.height(4.dp))
            val playlistsLayoutIndex = layoutModeOptions.indexOfFirst { it.first == playlistsLayout }.coerceAtLeast(0)
            top.yukonga.miuix.kmp.preference.WindowSpinnerPreference(
                title = strings.playlistsLayout,
                items = layoutModeOptions.map { top.yukonga.miuix.kmp.basic.DropdownItem(text = it.second) },
                selectedIndex = playlistsLayoutIndex,
                modifier = Modifier.clip(RoundedCornerShape(30.dp)),
                onSelectedIndexChange = { index ->
                    scope.launch { prefs.setPlaylistsLayout(layoutModeOptions[index].first) }
                }
            )
        }

        SectionCard {
            Text(strings.language, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            SettingsActionRow(
                icon = Icons.Filled.ArrowDropDown,
                title = AllLanguages.firstOrNull { it.code == language }?.displayName ?: language,
                onClick = { langMenuOpen = true }
            )
        }

        Spacer(Modifier.height(110.dp))
    }

    if (langMenuOpen) {
        MiuixDrawer(
            onDismissRequest = { langMenuOpen = false },
        ) {
            // Bir dil seçildiğinde bayrağı doğrudan false yapmak yerine drawer'ın kendi
            // kapanma akışını tetikliyoruz; böylece çıkış animasyonu kesilmiyor.
            val dismissDrawer = rememberDrawerDismiss()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .padding(20.dp)
            ) {
                Text(strings.language, style = MiuixAppTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).overScrollVertical(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AllLanguages) { lang ->
                        val selected = lang.code == language
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MiuixAppTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.58f)
                                )
                                .miuixWidgetClick(pressScale = 0.97f, maxTiltDegrees = 3f) {
                                    scope.launch { prefs.setLanguage(lang.code) }
                                    dismissDrawer()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (selected) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onBackground,
                                style = MiuixAppTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selected) {
                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MiuixAppTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (customPickerOpen && !dynamicColor) {
        CustomColorPickerDialog(
            onDismiss = { customPickerOpen = false },
            onColorPicked = { argb ->
                // MADDE 5 — Sadece rengi kaydet; drawer kapanışını onDismissRequest üzerinden
                // (dismissDrawer animasyonu bitince) sağlıyoruz. Böylece Apply'a basınca da
                // kapanma animasyonu oynar.
                scope.launch { prefs.setAccentColor(argb) }
            },
            initialArgb = accent,
            title = strings.customColorTitle,
            hexPlaceholder = strings.hexPlaceholder
        )
    }
}

@Composable
fun PlayerSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val crossfade by prefs.crossfadeEnabled.collectAsState(initial = false)
    val gapless by prefs.gaplessEnabled.collectAsState(initial = true)
    val playWith by prefs.playWithOthers.collectAsState(initial = false)

    SettingsPageScaffold(title = strings.playbackSettings, onBack = onBack) {
        SectionCard {
            Text(strings.playbackSettings, style = MiuixAppTheme.typography.titleMedium, color = MiuixAppTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = strings.crossfade, checked = crossfade) { scope.launch { prefs.setCrossfadeEnabled(it) } }
            ToggleRow(label = strings.gapless, checked = gapless) { scope.launch { prefs.setGaplessEnabled(it) } }
            ToggleRow(label = strings.playWithOthers, checked = playWith) { scope.launch { prefs.setPlayWithOthers(it) } }
        }
        Spacer(Modifier.height(110.dp))
    }
}

/**
 * InstallerX Revived'ın MiuixAboutPage'i ile birebir aynı yapı:
 *
 * - Sabit (sticky) header: uygulama ikonu (Lambda Player'ın ORİJİNAL launcher ikonu,
 *   30dp corner radius ile), 35sp uygulama adı ve sürüm bilgisi. Liste kaydırıldıkça
 *   header InstallerX'teki eşiklerle (icon 0.35→0.50, ad 0.20→0.35, sürüm 0.05→0.20)
 *   sırayla küçülüp kaybolur.
 * - Üstte `SmallTopAppBar`: "About" başlığı, kaydırma ilerledikçe action bar'ın
 *   ORTASINDA küçük şekilde belirir (Miuix davranışı).
 * - İçerik: `SmallTitle` + Miuix `Card` içinde `ArrowPreference` satırları
 *   (GitHub / CplShephard ve Miuix — InstallerX bağlantısı kaldırıldı).
 */
@Composable
fun AboutSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
        catch (_: PackageManager.NameNotFoundException) { "" }
    }

    val density = LocalDensity.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val appBarHeight = 56.dp
    val headerTopPadding = statusBarPadding + appBarHeight + 40.dp

    val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    var logoHeightPx by remember { mutableIntStateOf(0) }
    var headerHeightDp by remember { mutableStateOf(190.dp) }
    var appIconBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sizePx = with(density) { 88.dp.roundToPx() }
                val shrink = context.applicationInfo.loadIcon(context.packageManager) is android.graphics.drawable.AdaptiveIconDrawable
                val loader = me.zhanghai.android.appiconloader.AppIconLoader(sizePx, shrink, context)
                appIconBitmap = loader.loadIcon(context.applicationInfo, false)
            } catch (e: Exception) {
                appIconBitmap = null
            }
        }
    }

    // InstallerX MiuixAboutPage'teki scrollProgress hesabının birebir aynısı.
    val scrollProgress by remember {
        derivedStateOf {
            if (logoHeightPx <= 0) {
                0f
            } else {
                val index = lazyListState.firstVisibleItemIndex
                val offset = lazyListState.firstVisibleItemScrollOffset
                if (index > 0) 1f else (offset.toFloat() / logoHeightPx).coerceIn(0f, 1f)
            }
        }
    }

    // MADDE 9 — About sayfasına özel dinamik ışık (InstallerX'in BgEffect'i).
    // Renkler morumsu/mavimsi yerine YEŞİLİMSİ / AÇIK MAVİMSİ (bkz. BgEffectConfig).
    // Sayfa aşağı kaydırıldıkça (header solarken) ışık da yumuşakça sönüyor — InstallerX
    // About'ta olduğu gibi.
    val isDarkTheme = MiuixAppTheme.colorScheme.background.luminance() < 0.5f
    BgEffectBackground(
        isDarkTheme = isDarkTheme,
        modifier = Modifier.fillMaxSize(),
        isFullSize = true,
        // MADDE 2 — About arka planı SIMSIYAH olmalı; duvar kağıdı About ekranında
        // etkili olmamalı. Bu yüzden opak siyah zemin çizilir, dinamik ışık (MADDE 9)
        // onun ÜZERİNE biniyor.
        surface = Color.Black,
        alpha = { 1f - scrollProgress * 0.85f }
    ) {
        // Sticky animated header (liste bunun ÜZERİNDEN kayar)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = headerTopPadding)
                .onSizeChanged { size ->
                    with(density) { headerHeightDp = size.height.toDp() }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - iconProgress
                        scaleX = 1 - (iconProgress * 0.05f)
                        scaleY = 1 - (iconProgress * 0.05f)
                    }
                    .clip(RoundedCornerShape(30.dp))
            ) {
                val bitmap = appIconBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = strings.appName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = strings.appName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Text(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 5.dp)
                    .graphicsLayer {
                        val projectNameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - projectNameProgress
                        scaleX = 1 - (projectNameProgress * 0.05f)
                        scaleY = 1 - (projectNameProgress * 0.05f)
                    },
                text = strings.appName,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
                color = MiuixAppTheme.colorScheme.onBackground
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val versionProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                        alpha = 1 - versionProgress
                        scaleX = 1 - (versionProgress * 0.05f)
                        scaleY = 1 - (versionProgress * 0.05f)
                    },
                text = "${strings.version} $versionName",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant
            )
        }

        // Kaydırılabilir içerik
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
        ) {
            // Header yüksekliğinde şeffaf boşluk (InstallerX'teki "logoSpacer")
            item(key = "logoSpacer") {
                // LazyColumn ekranın en üstünden başladığı için spacer, header'ın
                // kapladığı tüm alanı (status bar + app bar + 40dp + header + 48dp) doldurur.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(headerTopPadding + headerHeightDp + 48.dp)
                        .onSizeChanged { size -> logoHeightPx = size.height }
                )
            }
            item(key = "about_content") {
                Column(modifier = Modifier.fillParentMaxHeight()) {
                    SmallTitle("About")
                    MiuixNativeCard(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        ArrowPreference(
                            title = "GitHub",
                            summary = "CplShephard",
                            onClick = { uriHandler.openUri("https://github.com/CplShephard") }
                        )
                        // MADDE 9 — InstallerX About'taki "Open Source Licenses" satırının
                        // yerinde artık Lambda Player'ın kendi kaynak kodu bağlantısı var.
                        ArrowPreference(
                            title = "Lambda Player Source Code",
                            summary = "github.com/CplShephard/Lambda-Player",
                            onClick = { uriHandler.openUri("https://github.com/CplShephard/Lambda-Player") }
                        )
                        ArrowPreference(
                            title = "Miuix",
                            summary = "A UI library for Compose MultiPlatform",
                            onClick = { uriHandler.openUri("https://github.com/miuix-project/miuix") }
                        )
                    }
                    Spacer(Modifier.height(110.dp))
                }
            }
        }

        // Üstte sabit app bar: "About" başlığı kaydırdıkça ORTADA belirir
        SmallTopAppBar(
            title = "About",
            modifier = Modifier.align(Alignment.TopCenter),
            color = MiuixAppTheme.colorScheme.background.copy(alpha = scrollProgress),
            titleColor = MiuixAppTheme.colorScheme.onBackground.copy(alpha = scrollProgress),
            scrollBehavior = topAppBarScrollBehavior,
            defaultWindowInsetsPadding = false,
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .bounceClick { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MiuixAppTheme.colorScheme.onBackground
                    )
                }
            }
        )
    }
}


/**
 * Ayar alt sayfaları için Miuix/InstallerX tarzı iskelet:
 * içerikteki büyük başlık kaydırıldıkça kaybolur ve `SmallTopAppBar`'ın ORTASINDA
 * küçük şekilde belirmeye devam eder (Miuix'in daralan başlık davranışı).
 */
@Composable
private fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    // Büyük başlık ~44dp'lik kaydırma aralığında kaybolur; aynı aralıkta app bar'daki
    // ortalanmış küçük başlık belirir.
    val collapseRangePx = with(density) { 44.dp.toPx() }
    val scrollProgress by remember {
        derivedStateOf { (scrollState.value / collapseRangePx).coerceIn(0f, 1f) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MiuixAppTheme.colorScheme.background)) {
        SmallTopAppBar(
            title = title,
            color = MiuixAppTheme.colorScheme.background.copy(alpha = scrollProgress),
            titleColor = MiuixAppTheme.colorScheme.onBackground.copy(alpha = scrollProgress),
            scrollBehavior = topAppBarScrollBehavior,
            // NOT: SmallTopAppBar zaten WindowInsets.systemBars(Top)'u KOŞULSUZ kendi
            // uyguluyor (defaultWindowInsetsPadding sadece yatay/notch insets'i kontrol
            // ediyor) — yani status bar padding'i buradan değil, Scaffold'un
            // contentWindowInsets'inden geliyordu. Asıl düzeltme orada (bkz. MainContainer).
            defaultWindowInsetsPadding = false,
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                        .bounceClick { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MiuixAppTheme.colorScheme.onBackground)
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                .overScrollVertical()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Büyük sayfa başlığı — kaydırdıkça küçülüp solar, yerini üstteki
            // ortalanmış küçük başlığa bırakır.
            Text(
                text = title,
                style = MiuixAppTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MiuixAppTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .graphicsLayer {
                        alpha = 1f - scrollProgress
                        scaleX = 1f - scrollProgress * 0.05f
                        scaleY = 1f - scrollProgress * 0.05f
                    }
            )
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    SectionCard(
        modifier = Modifier.miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixAppTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MiuixAppTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MiuixAppTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MiuixAppTheme.colorScheme.onBackground)
                Text(summary, style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixAppTheme.colorScheme.background.copy(alpha = 0.72f))
            .miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MiuixAppTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, color = MiuixAppTheme.colorScheme.onBackground, style = MiuixAppTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

// İzole edilmiş: totalListeningMs PlayerViewModel'in observePosition() döngüsü tarafından
// artık 10 saniyelik paketler halinde güncelleniyor. Bu değer buradaki KENDİ collectAsState'inde toplandığı için
// recompose sadece bu küçük composable ile sınırlı kalıyor — SettingsScreen'in geri kalanı
// (toggle'lar, slider'lar) her tick'te yeniden çizilmiyor.
@Composable
private fun TotalListeningTimeCard(playerViewModel: PlayerViewModel) {
    val strings = LocalStrings.current
    val totalMs by playerViewModel.totalListeningMsLive.collectAsState()
    SectionCard(modifier = Modifier.miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { }) {
        Text(
            text = strings.totalListeningTime,
            style = MiuixAppTheme.typography.titleMedium,
            color = MiuixAppTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatListeningTime(totalMs, strings),
            style = MiuixAppTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MiuixAppTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MiuixAppTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // weight(1f): uzun (örn. Türkçe) etiketler Switch'i sıkıştırıp deforme etmesin.
        // Etiket gerekirse birden fazla satıra sarar, toggle her zaman sabit boyutta kalır.
        Text(
            text = label,
            color = MiuixAppTheme.colorScheme.onBackground,
            style = MiuixAppTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                checkedTrackColor = MiuixAppTheme.colorScheme.primary,
                uncheckedTrackColor = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        )
    }
}

private fun formatListeningTime(
    ms: Long,
    strings: dev.shephard.player.ui.i18n.Strings
): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d%s %02d%s %02d%s".format(
        h, strings.hourShort,
        m, strings.minuteShort,
        s, strings.secondShort
    ) else "%d%s %02d%s".format(
        m, strings.minuteShort,
        s, strings.secondShort
    )
}
