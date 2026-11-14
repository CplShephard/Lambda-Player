// SPDX-License-Identifier: GPL-3.0-only
// Shared Material 3 building blocks for Lambda Player's M3 screens.
package dev.shephard.player.ui.components.m3

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.player.ImagePersistence
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

/**
 * Solid top-bar colours for every M3 page. The container is an opaque theme surface
 * (so it follows the accent colour), matching the solid Miuix small top bars;
 * title/icons use theme on-surface colours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun m3TopBarColors(): TopAppBarColors {
    val cs = MaterialTheme.colorScheme
    return TopAppBarDefaults.topAppBarColors(
        containerColor = cs.surface,
        scrolledContainerColor = cs.surface,
        navigationIconContentColor = cs.onSurface,
        titleContentColor = cs.onSurface,
        actionIconContentColor = cs.onSurfaceVariant,
    )
}

/** One title moves and scales as the page header collapses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3SmallTopBar(
    title: @Composable () -> Unit,
    state: ExpressiveTopBarState,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = m3TopBarColors(),
) {
    val fraction = state.fraction
    val titleStart = androidx.compose.ui.unit.lerp(20.dp, if (navigationIcon != null) 80.dp else 20.dp, fraction)
    val height = androidx.compose.ui.unit.lerp(140.dp, 64.dp, fraction)
    Box(
        modifier = modifier.fillMaxWidth().background(Brush.verticalGradient(
            0f to colors.containerColor, .9f to colors.containerColor, 1f to colors.containerColor.copy(alpha = 0f)))
            // Paint before applying the inset: the same bar surface continues
            // underneath status-bar icons and display cutouts.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).height(height)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(start = titleStart, end = androidx.compose.ui.unit.lerp(24.dp, 64.dp, fraction)),
            contentAlignment = androidx.compose.ui.BiasAlignment(-1f, 1f - 2f * fraction)
        ) {
            Box(modifier = Modifier.height(64.dp), contentAlignment = Alignment.CenterStart) {
                androidx.compose.material3.ProvideTextStyle(
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = androidx.compose.ui.unit.lerp(32.sp, 23.sp, fraction),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                ) { title() }
            }
        }
        if (navigationIcon != null) {
            Box(Modifier.align(Alignment.TopStart).padding(start = 12.dp).height(64.dp), contentAlignment = Alignment.Center) {
                navigationIcon()
            }
        }
        Row(modifier = Modifier.align(Alignment.TopEnd).height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

/**
 * Container colour for list-item cards. Same tone as the item cards on the M3 theme
 * settings page ([BaseWidget]) so the cards stay visible against the page background.
 */
@Composable
fun m3ItemCardColors(): CardColors =
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

/**
 * Returns a launcher that runs the same cover flow as the Miuix screens: pick an image
 * (OpenDocument), crop it to 512x512 through the system CROP intent, and fall back to
 * persisting the picked image as-is for GIFs or when no cropper is installed.
 * [onCover] receives the final, persisted cover Uri.
 */
@Composable
fun rememberCoverPicker(onCover: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnCover by rememberUpdatedState(onCover)
    var cropOutput by remember { mutableStateOf<Uri?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = cropOutput
        if (result.resultCode == Activity.RESULT_OK && output != null) currentOnCover(output)
        cropOutput = null
    }

    fun persistAsIs(source: Uri) {
        scope.launch {
            ImagePersistence.persistCover(context, source)?.let { currentOnCover(it) }
        }
    }

    fun launchCrop(source: Uri) {
        if (dev.shephard.player.player.ImagePersistence.isAnimatedCover(context, source)) {
            persistAsIs(source)
            return
        }
        val dir = java.io.File(context.filesDir, "persisted_covers").apply { mkdirs() }
        val file = java.io.File(dir, "cover_${System.currentTimeMillis()}.jpg")
        val output = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cropOutput = output

        val cropIntent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(source, "image/*")
            putExtra("crop", "true")
            putExtra("scale", true)
            putExtra("outputX", 512)
            putExtra("outputY", 512)
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, output)
            putExtra("outputFormat", android.graphics.Bitmap.CompressFormat.JPEG.toString())
            putExtra("return-data", false)
            putExtra("noFaceDetection", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "cover", source)
        }
        val resolved = context.packageManager.queryIntentActivities(cropIntent, 0)
        for (info in resolved) {
            val pkg = info.activityInfo?.packageName ?: continue
            try {
                context.grantUriPermission(
                    pkg, output,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
        }
        if (resolved.isNotEmpty()) cropLauncher.launch(cropIntent) else persistAsIs(source)
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            launchCrop(uri)
        }
    }

    return remember(picker) { { picker.launch(arrayOf("image/*", "video/mp4")) } }
}

/**
 * M3 twin of the Miuix cover block (create / edit playlist, edit music): a square,
 * tappable cover with an edit badge in the bottom-end corner and, when a custom cover
 * is set, a "Remove cover" button underneath that asks for confirmation.
 *
 * @param model what to show as the cover (custom cover, or a fallback such as the album art).
 * @param hasCustomCover true when [model] is a user-set cover, i.e. removing makes sense.
 * @param onRemove called after the user confirms; pass null to hide the remove button.
 */
@Composable
fun M3CoverEditor(
    model: Any?,
    placeholderIcon: ImageVector,
    hasCustomCover: Boolean,
    onPick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
) {
    val strings = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    var confirmRemove by remember { mutableStateOf(false) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(cs.secondaryContainer)
                .m3CardPress(shape = RoundedCornerShape(16.dp), overlayColor = androidx.compose.ui.graphics.Color.White, onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            var loaded by remember(model) { mutableStateOf(false) }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    onState = { loaded = it is AsyncImagePainter.State.Success }
                )
            }
            if (model == null || !loaded) {
                Icon(placeholderIcon, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(48.dp))
            }
            dev.shephard.player.ui.components.SilentVideoCover(model, Modifier.matchParentSize())
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cs.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, null, tint = cs.onSurface, modifier = Modifier.size(16.dp))
            }
        }
        if (hasCustomCover && onRemove != null) {
            TextButton(onClick = { confirmRemove = true }) {
                Text(strings.removeCover, color = cs.error)
            }
        }
    }

    if (confirmRemove && onRemove != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(strings.removeCover) },
            text = { Text(strings.removeCoverConfirm, color = cs.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove() }) {
                    Text(strings.removeCover, color = cs.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(strings.cancel) }
            },
        )
    }
}
