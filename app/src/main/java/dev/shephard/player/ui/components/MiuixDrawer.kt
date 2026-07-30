package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * Lambda Player'ın TEK drawer/bottom-sheet bileşeni.
 *
 * Eskiden burada `OverlayBottomSheet` tabanlı, kendi elle yazılmış drag handle'ı olan bir
 * "MiuixSheet" vardı. İki büyük sorunu vardı:
 *
 *  1. `OverlayBottomSheet` içeriğini bir `Scaffold`'un `MiuixPopupHost` slotuna kaydeder.
 *     NowPlayingSheet (ve MainActivity'deki güncelleme sheet'i) Scaffold'un DIŞINDA
 *     çizildiği için oradan açılan drawer'lar hiçbir host tarafından render edilmiyordu.
 *  2. Daha önemlisi: miuix'in sheet katmanı `NavigationBackHandler` kullanıyor ve bu
 *     androidx.activity 1.12+ ister — proje 1.9.1'deydi, bu yüzden drawer açılır açılmaz
 *     uygulama çöküyordu. (build.gradle.kts'te sürümler yükseltildi.)
 *
 * Yeni sürüm, InstallerX'in miuix install dialogunda kullandığı drawer ile BİREBİR aynıdır:
 * `top.yukonga.miuix.kmp.window.WindowBottomSheet`. Bu bileşen gerçek bir platform
 * `Dialog` penceresinde çizilir; hiçbir Scaffold'a / popup host'a bağımlı değildir,
 * dolayısıyla uygulamanın HER yerinden (NowPlaying dahil) güvenle açılabilir.
 * Drag handle, köşe yarıçapı, folme spring animasyonu, dim katmanı, nested-scroll ve
 * predictive-back davranışı InstallerX'teki ile aynıdır çünkü aynı miuix katmanıdır.
 *
 * Kullanım: bileşen SADECE gösterilmek istendiğinde composition'a girer
 * (`if (show) { MiuixDrawer(...) }`). Giriş animasyonu otomatik başlar; kapanma isteğinde
 * önce çıkış animasyonu oynatılır, animasyon bitince [onDismissRequest] çağrılır.
 * Böylece çağıran taraf bayrağı `false` yaptığında sheet zaten ekrandan çıkmış olur ve
 * "zıplayarak kaybolma" olmaz.
 */
@Composable
fun MiuixDrawer(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color = MiuixDrawerDefaults.backgroundColor(),
    cornerRadius: Dp = MiuixDrawerDefaults.CornerRadius,
    insideMargin: DpSize = MiuixDrawerDefaults.InsideMargin,
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Sheet composition'a girer girmez açılır; `show` state'i sadece ÇIKIŞ animasyonunu
    // sürebilmek için var (InstallerX'teki `showBottomSheet` + `dismissSheet {}` deseni).
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    WindowBottomSheet(
        show = visible,
        modifier = modifier,
        title = title,
        startAction = startAction,
        endAction = endAction,
        backgroundColor = backgroundColor,
        cornerRadius = cornerRadius,
        insideMargin = insideMargin,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        onDismissRequest = {
            // Sadece çıkış animasyonunu tetikle. Gerçek "kapat" bildirimi
            // onDismissFinished'te gider.
            if (allowDismiss) visible = false
        },
        onDismissFinished = { currentOnDismissRequest() },
        content = content,
    )
}

/**
 * MADDE 4 — Kapak (cover) düzenleme drawer'larının başlık satırı.
 *
 * Eskiden solda "Cancel", sağda "Apply" YAZI butonları vardı. Artık Miuix'in kendi
 * ikon setinden (`miuix-icons`) gelen ✓ (`MiuixIcons.Ok`) ve × (`MiuixIcons.Close`)
 * işaretleri kullanılıyor — InstallerX'in miuix sheet başlıklarındaki ile aynı dil.
 *
 * Renkler tamamen temaya bağlı:
 *  - ✓ : dolgu `primary`, ikon `onPrimary`  → accent rengi değişince beraber değişir.
 *  - × : dolgu `surfaceVariant`, ikon `onSurface` → açık/koyu temayla beraber değişir.
 */
@Composable
fun MiuixDrawerActionHeader(
    title: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixDrawerCircleAction(
            imageVector = MiuixIcons.Close,
            contentDescription = "Cancel",
            containerColor = MiuixTheme.colorScheme.surfaceVariant,
            contentColor = MiuixTheme.colorScheme.onSurface,
            onClick = onCancel,
        )
        Text(
            text = title,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onBackground,
        )
        MiuixDrawerCircleAction(
            imageVector = MiuixIcons.Ok,
            contentDescription = "Apply",
            containerColor = if (confirmEnabled) MiuixTheme.colorScheme.primary
            else MiuixTheme.colorScheme.surfaceVariant,
            contentColor = if (confirmEnabled) MiuixTheme.colorScheme.onPrimary
            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            enabled = confirmEnabled,
            onClick = onConfirm,
        )
    }
}

@Composable
private fun MiuixDrawerCircleAction(
    imageVector: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor)
            .pressScaleClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

object MiuixDrawerDefaults {
    /** InstallerX'in miuix sheet'i ile aynı köşe yarıçapı. */
    val CornerRadius: Dp = BottomSheetDefaults.cornerRadius

    /**
     * Drawer içerikleri kendi padding'lerini yönetiyor, bu yüzden miuix'in varsayılan
     * 24dp yatay iç boşluğunu sıfırlıyoruz (aksi halde her şey iki kez padding alır).
     */
    val InsideMargin: DpSize = DpSize(0.dp, 0.dp)

    @Composable
    fun backgroundColor(): Color = MiuixTheme.colorScheme.surfaceContainer
}

/**
 * Drawer içeriğinden kapanma isteği göndermek için. `WindowBottomSheet` içerideki
 * composable'lara `LocalDismissState` sağlar; bu fonksiyon çağrıldığında önce çıkış
 * animasyonu oynar, sonra `onDismissRequest` tetiklenir.
 *
 * Bir öğe seçildiğinde (örn. dil seçimi) doğrudan `open = false` yapmak yerine bunu
 * kullanın; böylece animasyon kesilmez.
 */
@Composable
fun rememberDrawerDismiss(): () -> Unit {
    val dismiss = LocalDismissState.current
    return remember(dismiss) { { dismiss?.invoke() } }
}

/**
 * `x?.let { MiuixDrawer(...) }` deseninde, x null olunca içerik anında yok olur ve çıkış
 * animasyonu boş bir sheet üstünde oynar. Bu yardımcı son null olmayan değeri tutar.
 */
@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    var last by remember { mutableStateOf(value) }
    if (value != null) last = value
    return last
}
