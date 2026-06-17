package dev.shephard.player.player

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

val audioPermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_AUDIO
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}

@Composable
fun rememberAudioPermissionState(
    onGranted: () -> Unit
): AudioPermissionState {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) onGranted()
    }

    return remember {
        AudioPermissionState(
            hasPermissionProvider = { hasPermission },
            requestPermission = { launcher.launch(audioPermission) }
        )
    }
}

class AudioPermissionState(
    private val hasPermissionProvider: () -> Boolean,
    val requestPermission: () -> Unit
) {
    val hasPermission: Boolean get() = hasPermissionProvider()
}
