package com.interes.shared.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.interes.shared.model.Photo
import com.interes.shared.overlay.PhotoOverlayService

@Composable
actual fun rememberTransparentWindowLauncher(): (Photo) -> Unit {
    val context = LocalContext.current
    // Фото, ожидающее показа после того, как пользователь вернётся с экрана
    // разрешений — если разрешения ещё не было.
    var pendingPhoto by remember { mutableStateOf<Photo?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Системный экран не возвращает точный результат в data — надёжнее
        // просто перепроверить Settings.canDrawOverlays после возврата.
        if (Settings.canDrawOverlays(context)) {
            pendingPhoto?.let { PhotoOverlayService.start(context, it.filePath) }
        }
        pendingPhoto = null
    }

    return { photo ->
        if (Settings.canDrawOverlays(context)) {
            PhotoOverlayService.start(context, photo.filePath)
        } else {
            pendingPhoto = photo
            permissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }
}
