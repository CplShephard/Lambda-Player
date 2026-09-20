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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.player.ImagePersistence
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

/**
 * Solid top-bar colours for every M3 page. The container is an opaque theme surface
 * (so it follows the accent colour) instead of being transparent over the wallpaper,
 * matching the solid Miuix small top bars; title/icons use theme on-surface colours
 * so they stay readable regardless of the wallpaper.
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
        if (context.contentResolver.getType(source) == "image/gif") {
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

    return remember(picker) { { picker.launch(arrayOf("image/*")) } }
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
                .clickable(onClick = onPick),
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
