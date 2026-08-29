# MiuiX Hata Düzeltmeleri — Özet

Bu pakette yapılan tüm değişiklikler MiuiX UI engine'ini ilgilendiren 5
sorunu hedefler. Material 3 düzeltmeleri sonraki turda yapılacaktır.

## 1) Home / Playlists sayfalarında siyah arka plan → wallpaper gizleniyordu

**Sorun:** `MiuixAppTheme.colorScheme.background` karanlık temada siyah
olduğundan, `HomeScreen` ve `PlaylistScreen` kök `Box`'ları bu renkle
doluyor ve alttaki duvar kağıdını tamamen kapatıyordu.

**Çözüm:**
- `MainContainer`'da wallpaper varsa `containerBackground` artık şeffaf
  yapılıyor; aksi halde temaya geri dönülüyor (`Material 3` ve
  `MiuixAppTheme` renkleri korunuyor).
- `HomeScreen.kt` ve `PlaylistScreen.kt` içindeki kök
  `Box(modifier = … .background(MiuixAppTheme.colorScheme.background))`
  satırları kaldırıldı. Yalnızca **PlaylistDetailView** içindeki
  `Column` hâlâ `MiuixAppTheme.colorScheme.background` ile sarılı —
  kullanıcının istediği şekilde detail view siyah arka planını koruyor.
- `MainContainer`'da wallpaper artık kendi kutusunda
  `Modifier.layerBackdrop(wallpaperBackdrop)` ile sarılı; bu sayede tüm
  liquid-glass yüzeyler wallpaper'ı görebiliyor.

## 2) SmallTopAppBar'ların değişik blur değerleri → kötü render

**Sorun:** `SmallTopAppBar` / `TopAppBar` çağrılarında `blurRadius=70f`
ve scroll ilerledikçe `0.68 + 0.27*scroll` formülünde tint alpha
kullanılıyordu. Bu, mini player pop-up'ından (`blurRadius=28f,
tintAlpha=0.58f`) gözle görülür biçimde farklıydı ve kötü render
ediliyordu.

**Çözüm:** Tüm `miuixBlurSurface` çağrılarında `blurRadius=28f` ve
`tintAlpha=0.58f` kullanılıyor. Değiştirilen yerler:
- `CollapsingTopBar.kt` — `CollapsingTopBar`, `InstallerXTopBar`,
  `SubmenuTopBar`.
- `SettingsScreen.kt` — `SettingsPageScaffold`.
- `StatsScreen.kt` — `StatsScreen`.
- `PlaylistScreen.kt` — `PlaylistDetailTopBar`.

## 3) Settings → Playlists geçişinde hafif kasma

**Sorun:** `MainContainer`, `lastMainPage`'i sürekli state olarak
topluyordu. `setLastMainPage` her pager settledPage değişiminde disk
I/O tetikliyor, `rememberPagerState(initialPage=…)` recomposition
sırasında yeniden hesaplanıyor ve `LaunchedEffect(currentPage)
{ syncPage() }` recomposition döngüsü oluşturuyordu.

**Çözüm:**
- `lastMainPage` artık `state` olarak toplanmıyor; sadece ilk
  composition'da `runBlocking { prefs.lastMainPage.first() }` ile bir
  kez okunup `remember`'da tutuluyor. Bu, `rememberPagerState`'in
  `initialPage`'inin de yalnız ilk açılışta hesaplanmasını sağlıyor.
- Sayfa kaydedilirken 250ms'lik bir debounce eklendi; kullanıcı hızlı
  swipe yaparken disk'e ara yazma yapılmıyor.
- `LaunchedEffect(currentPage) { mainPagerState.syncPage() }` satırı
  kaldırıldı. `animateToPage` zaten `selectedPage`'i güncellediği için
  dock senkronizasyonu bozulmuyor.

## 4) Total Listening Time sayfasında yeterince müzik yokken miuix overscroll çalışmıyor

**Sorun:** `StatsScreen` dikey kaydırma için `verticalScroll` kullanıyor
ancak `overScrollVertical` modifier'ı uygulanmamıştı. Üstelik içerik
ekranı dolduracak kadar uzun değilse, modifier olmadan üst kenardan
overscroll tetiklenmiyordu.

**Çözüm:**
- `StatsScreen` scroll bölümüne `.overScrollVertical()` modifier'ı
  `verticalScroll` modifier'ından **önce** uygulandı (doğru sıra:
  önce `clipToBounds` + nested scroll node, sonra iç scrollable).
- Sayfanın en üstüne 40dp'lik `Spacer` eklendi, böylece içerik az
  olduğunda bile üst kenardan overscroll tetiklenebiliyor.

## 5) Apple floating dock blur açıkken wallpaper renderlanamıyor

**Sorun:** Apple dock, `FloatingBottomBar` parametresi olarak
`effectiveBackdrop = contentBackdrop ?: dummyBackdrop` kullanıyordu.
`contentBackdrop` yalnızca sayfa içeriğini yakalıyordu, wallpaper'ı
yakalamıyordu. `dummyBackdrop` ise boş bir `LayerBackdrop` olduğundan
içinde hiçbir şey yoktu. Bu yüzden dock "liquid glass" moduna geçince
arkasında yalnızca boş / yarı saydam katman görünüyordu.

**Çözüm:**
- `MiuixBlur.kt` içine yeni `rememberWallpaperBlurBackdrop(enableBlur)`
  fonksiyonu eklendi. Bu backdrop, minimal bir draw block
  (`drawRect(surfaceColor) + drawContent()`) ile sadece miuix layer'ı
  oluşturur.
- `MainContainer` artık wallpaper'ı çizdiği `Box` üzerinde
  `Modifier.layerBackdrop(wallpaperBackdrop)` kullanıyor — yani
  miuix'in layer capture'ı wallpaper'ı ve karartma katmanını
  otomatik olarak topluyor.
- `AppleFloatingDock` parametre olarak `wallpaperBackdrop` alıyor;
  `effectiveBackdrop = wallpaperBackdrop ?: contentBackdrop ?: …` ile
  öncelik wallpaper'ı gören backdrop'a veriliyor.

## Ek: Duvar kağıdı olan temalar için container şeffaflığı

`MainContainer` artık `containerBackground`'ı şu mantıkla seçiyor:
- Eğer wallpaper ayarlıysa → `Color.Transparent` (wallpaper zaten
  arka planda çiziliyor, tema renginin tekrar basılması gereksiz).
- Aksi halde → ilgili tema rengi (`MiuixAppTheme.colorScheme.background`
  veya `MaterialTheme.colorScheme.surfaceContainer`).

Böylece wallpaper ayarlı olduğunda `NavGraph`'ın arka planı transparan
kalıyor, kullanıcının seçtiği duvar kağıdı tüm sayfalarda
görünür kalıyor.

## Değiştirilen Dosyalar

- `app/src/main/java/dev/shephard/player/ui/glass/MiuixBlur.kt`
- `app/src/main/java/dev/shephard/player/ui/components/CollapsingTopBar.kt`
- `app/src/main/java/dev/shephard/player/ui/navigation/MainContainer.kt`
- `app/src/main/java/dev/shephard/player/ui/screens/HomeScreen.kt`
- `app/src/main/java/dev/shephard/player/ui/screens/PlaylistScreen.kt`
- `app/src/main/java/dev/shephard/player/ui/screens/SettingsScreen.kt`
- `app/src/main/java/dev/shephard/player/ui/screens/StatsScreen.kt`
