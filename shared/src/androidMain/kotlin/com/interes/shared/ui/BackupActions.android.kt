package com.interes.shared.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.interes.shared.backup.BackupManager
import com.interes.shared.storage.BackupPaths
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Composable
actual fun rememberBackupCreator(paths: BackupPaths, onResult: (Result<Unit>) -> Unit): () -> Unit {
    val context = LocalContext.current
    // SAF (Storage Access Framework) отдаёт только content:// Uri, писать в
    // который умеет лишь ContentResolver — BackupManager.createBackup такое
    // не понимает (он пишет обычным java.io в обычный путь). Поэтому:
    // 1) собираем ZIP во временный файл в приватном кэше приложения,
    // 2) переливаем его байты в выбранный пользователем content:// Uri,
    // 3) временный файл удаляем в любом случае (finally).
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tempFile = File(context.cacheDir, "interes-backup-temp.zip")
        val result = BackupManager.createBackup(paths, tempFile.absolutePath).mapCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                tempFile.inputStream().use { it.copyTo(out) }
            } ?: error("Не удалось открыть выбранное место для сохранения")
            Unit
        }
        tempFile.delete()
        onResult(result)
    }
    val stamp = remember { SimpleDateFormat("yyyy-MM-dd_HHmm").format(Date()) }
    return { launcher.launch("interes-backup-$stamp.zip") }
}

@Composable
actual fun rememberBackupRestorer(paths: BackupPaths, onResult: (Result<Unit>) -> Unit): () -> Unit {
    val context = LocalContext.current
    // Та же логика в обратную сторону: копируем содержимое выбранного
    // content:// Uri во временный файл, дальше с ним работает уже обычный
    // BackupManager.restoreBackup(...), как и на Desktop.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tempFile = File(context.cacheDir, "interes-restore-temp.zip")
        val result = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { input.copyTo(it) }
            } ?: error("Не удалось открыть выбранный файл резервной копии")
        }.mapCatching {
            BackupManager.restoreBackup(paths, tempFile.absolutePath).getOrThrow()
        }
        tempFile.delete()
        onResult(result)
    }
    return { launcher.launch(arrayOf("application/zip")) }
}
