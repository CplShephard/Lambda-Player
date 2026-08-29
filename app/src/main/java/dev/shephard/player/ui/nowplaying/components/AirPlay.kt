// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.nowplaying.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import dev.shephard.player.R

/**
 * Compact AirPlay-style row used in the now-playing sheet.
 *
 * When a paired Bluetooth audio device is connected, the icon morphs
 * into a headphone glyph and the device's name is shown underneath. Tapping
 * the row opens the system media-output picker (Android 11+) or the
 * sound settings screen as a fallback.
 */
@Composable
fun RowScope.AirPlay() {
    val context = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? = remember { BluetoothAdapter.getDefaultAdapter() }
    var hasBtPermission by remember { mutableStateOf(hasBluetoothPermission(context)) }
    var connectedDeviceName by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasBtPermission = granted
        if (granted) connectedDeviceName = queryConnectedAudioDevice(bluetoothAdapter)
    }

    DisposableEffect(hasBtPermission) {
        if (!hasBtPermission) {
            onDispose { }
        } else {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                    connectedDeviceName = queryConnectedAudioDevice(bluetoothAdapter)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            connectedDeviceName = queryConnectedAudioDevice(bluetoothAdapter)
            onDispose { runCatching { context.unregisterReceiver(receiver) } }
        }
    }

    Column(
        modifier = Modifier
            .heightIn(min = 53.dp)
            .padding(bottom = 48.dp)
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!hasBtPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    openSystemMediaPicker(context)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.height(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = connectedDeviceName != null,
                transitionSpec = {
                    (scaleIn(initialScale = 0.3f) + fadeIn())
                        .togetherWith(scaleOut(targetScale = 0.3f) + fadeOut())
                },
                contentAlignment = Alignment.Center,
            ) { showName ->
                Icon(
                    painter = painterResource(
                        id = if (showName) R.drawable.ic_earphone
                        else R.drawable.ic_nowplaying_airplay,
                    ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(if (showName) 27.dp else 21.5.dp),
                )
            }
        }
        AnimatedVisibility(
            visible = connectedDeviceName != null,
            enter = scaleIn(initialScale = 0.3f) + fadeIn(),
            exit = scaleOut(targetScale = 0.3f) + fadeOut(),
        ) {
            Text(
                text = connectedDeviceName.orEmpty(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
        }
    }
}

private fun hasBluetoothPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return ActivityCompat.checkSelfPermission(
        context, Manifest.permission.BLUETOOTH_CONNECT,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun queryConnectedAudioDevice(adapter: BluetoothAdapter?): String? {
    if (adapter == null) return null
    val device = runCatching {
        adapter.bondedDevices?.firstOrNull {
            it.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
        }
    }.getOrNull() ?: return null
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias else device.name
    }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
}

private fun openSystemMediaPicker(context: Context) {
    runCatching {
        val intent = android.content.Intent("android.settings.MEDIA_CONTROLS_SETTINGS")
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure {
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
