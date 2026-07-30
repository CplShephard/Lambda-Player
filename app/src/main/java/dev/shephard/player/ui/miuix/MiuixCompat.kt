@file:OptIn(ExperimentalFoundationApi::class)

package dev.shephard.player.ui.miuix

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalMaterial3Api

@Stable
class MiuixColorScheme {
    private val c
        @Composable get() = MiuixTheme.colorScheme
    val primary: Color
        @Composable get() = c.primary
    val onPrimary: Color
        @Composable get() = c.onPrimary
    val secondary: Color
        @Composable get() = c.secondary
    val tertiary: Color
        @Composable get() = c.primaryVariant
    val background: Color
        @Composable get() = c.background
    val onBackground: Color
        @Composable get() = c.onBackground
    val surface: Color
        @Composable get() = c.surface
    val onSurface: Color
        @Composable get() = c.onSurface
    val surfaceVariant: Color
        @Composable get() = c.surfaceVariant
    val onSurfaceVariant: Color
        @Composable get() = c.onSurfaceVariantSummary
    val surfaceContainer: Color
        @Composable get() = c.surfaceContainer
    val surfaceContainerLow: Color
        @Composable get() = c.surface
    val surfaceContainerHigh: Color
        @Composable get() = c.surfaceContainerHigh
    val surfaceContainerHighest: Color
        @Composable get() = c.surfaceContainerHighest
    val error: Color
        @Composable get() = c.error
    val onError: Color
        @Composable get() = c.onError
    val outline: Color
        @Composable get() = c.outline
    val outlineVariant: Color
        @Composable get() = c.dividerLine
    val scrim: Color
        @Composable get() = c.windowDimming
}

@Stable
class MiuixTypography {
    val displayMedium: TextStyle
        @Composable get() = MiuixTheme.textStyles.title1
    val headlineMedium: TextStyle
        @Composable get() = MiuixTheme.textStyles.title1
    val headlineSmall: TextStyle
        @Composable get() = MiuixTheme.textStyles.title2
    val titleLarge: TextStyle
        @Composable get() = MiuixTheme.textStyles.title2
    val titleMedium: TextStyle
        @Composable get() = MiuixTheme.textStyles.title3
    val titleSmall: TextStyle
        @Composable get() = MiuixTheme.textStyles.headline1
    val bodyLarge: TextStyle
        @Composable get() = MiuixTheme.textStyles.main
    val bodyMedium: TextStyle
        @Composable get() = MiuixTheme.textStyles.body1
    val bodySmall: TextStyle
        @Composable get() = MiuixTheme.textStyles.body2
    val labelLarge: TextStyle
        @Composable get() = MiuixTheme.textStyles.headline1
    val labelMedium: TextStyle
        @Composable get() = MiuixTheme.textStyles.body2
    val labelSmall: TextStyle
        @Composable get() = MiuixTheme.textStyles.footnote1
}

object MiuixAppTheme {
    val colorScheme = MiuixColorScheme()
    val typography = MiuixTypography()
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MiuixAppTheme.typography.bodyMedium,
    fontWeight: FontWeight? = null,
    // Miuix'in gerçek Text'i bu parametreleri zaten destekliyor; About ekranındaki
    // InstallerX tarzı başlık (35.sp) ve ortalanmış sürüm satırı için geçiriyoruz.
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = top.yukonga.miuix.kmp.basic.Text(
    text = text,
    modifier = modifier,
    color = color,
    style = style,
    fontWeight = fontWeight,
    fontSize = fontSize,
    textAlign = textAlign,
    maxLines = maxLines,
    overflow = overflow,
)

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MiuixAppTheme.colorScheme.onSurface,
) = top.yukonga.miuix.kmp.basic.Icon(
    imageVector = imageVector,
    contentDescription = contentDescription,
    modifier = modifier,
    tint = tint,
)

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = top.yukonga.miuix.kmp.basic.IconButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    content = content,
)

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    top.yukonga.miuix.kmp.basic.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

class CardColors(val containerColor: Color)
object CardDefaults {
    @Composable fun cardColors(containerColor: Color = MiuixAppTheme.colorScheme.surfaceVariant) = CardColors(containerColor)
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.containerColor)
            .padding(0.dp),
        content = content,
    )
}

class SliderColors
object SliderDefaults {
    @Composable fun colors(
        thumbColor: Color = Color.White,
        activeTrackColor: Color = MiuixAppTheme.colorScheme.primary,
        inactiveTrackColor: Color = MiuixAppTheme.colorScheme.onSurfaceVariant,
    ) = SliderColors()
}

/**
 * Miuix Slider.
 *
 * Miuix'in kendi varsayılanı `thumbColor = colorScheme.onPrimary`. Lambda'da `onPrimary`,
 * accent rengin parlaklığına göre siyaha düşebiliyor (ör. varsayılan yeşil accent'te
 * onPrimary = #111111) — bu yüzden tema ayarlarındaki slider'ların ucundaki yuvarlak
 * simge SİYAH görünüyordu. Orijinal Miuix'te bu tutamak her zaman beyazdır, o yüzden
 * burada thumb rengini accent'ten bağımsız olarak sabit beyaza kilitliyoruz.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    colors: SliderColors = SliderDefaults.colors(),
    onValueChangeFinished: (() -> Unit)? = null,
) = top.yukonga.miuix.kmp.basic.Slider(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    valueRange = valueRange,
    onValueChangeFinished = onValueChangeFinished,
    colors = top.yukonga.miuix.kmp.basic.SliderDefaults.sliderColors(
        thumbColor = Color.White,
        disabledThumbColor = Color.White.copy(alpha = 0.5f),
    ),
)

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = Box(
    modifier = modifier
        .size(28.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(if (checked) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.surfaceContainerHigh)
        .clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
    contentAlignment = Alignment.Center,
) {
    if (checked) Box(Modifier.size(12.dp).clip(CircleShape).background(MiuixAppTheme.colorScheme.onPrimary))
}

class SwitchColors
object SwitchDefaults {
    @Composable fun colors(
        checkedThumbColor: Color = MiuixAppTheme.colorScheme.onPrimary,
        checkedTrackColor: Color = MiuixAppTheme.colorScheme.primary,
        uncheckedThumbColor: Color = MiuixAppTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor: Color = MiuixAppTheme.colorScheme.surfaceVariant,
    ) = SwitchColors()
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    colors: SwitchColors = SwitchDefaults.colors(),
    enabled: Boolean = true,
) = top.yukonga.miuix.kmp.basic.Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
)

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier) {
        label?.invoke()
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = MiuixAppTheme.typography.bodyLarge.copy(color = MiuixAppTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isError) MiuixAppTheme.colorScheme.error.copy(alpha = 0.12f) else MiuixAppTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder != null) placeholder()
                inner()
            }
        )
    }
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = MiuixAppTheme.colorScheme.outlineVariant,
    thickness: Dp = 1.dp,
) = Box(modifier.fillMaxWidth().height(thickness).background(color))

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MiuixAppTheme.colorScheme.primary,
) = top.yukonga.miuix.kmp.basic.CircularProgressIndicator(modifier = modifier)

@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = MiuixAppTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            .clip(shape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) = Button(onClick = onClick, modifier = modifier, content = content)

// Eski `ModalBottomSheet` / `SheetState` / `rememberModalBottomSheetState` uyumluluk
// katmanı TAMAMEN kaldırıldı. `OverlayBottomSheet` tabanlıydı, Scaffold'un popup host'una
// bağımlıydı ve NowPlaying gibi Scaffold dışı yerlerden açılamıyordu.
// Yerine: dev.shephard.player.ui.components.MiuixDrawer (WindowBottomSheet tabanlı,
// InstallerX'in miuix install dialogundaki drawer ile birebir aynı).



