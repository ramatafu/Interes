package com.interes.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.interes.shared.backup.BackupManager
import com.interes.shared.storage.BackupPaths
import java.awt.FileDialog
import java.awt.Frame
import java.text.SimpleDateFormat
import java.util.Date

@Composable
actual fun rememberBackupCreator(paths: BackupPaths, onResult: (Result<Unit>) -> Unit): () -> Unit {
    // Синхронно, на том же потоке, что и сам клик — как FileDialog.isVisible
    // ниже (тот же паттерн, что уже в ImagePicker.desktop.kt). Для реального
    // объёма фото досок (десятки-сотни, не тысячи) упаковка в ZIP занимает
    // доли секунды — на секунду-другую подвиснуть на клике "Создать копию"
    // приемлемо. Если досок станет действительно много, стоит переносить в
    // корутину с состоянием загрузки — сознательно не усложняем сейчас.
    return remember(paths) {
        {
            val dialog = FileDialog(null as Frame?, "Сохранить резервную копию", FileDialog.SAVE)
            val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm").format(Date())
            dialog.file = "interes-backup-$stamp.zip"
            dialog.isVisible = true // блокирует поток до закрытия — как и в ImagePicker.desktop.kt
            val dir = dialog.directory
            val file = dialog.file
            if (dir != null && file != null) {
                // AWT FileDialog в режиме SAVE не добавляет расширение сам,
                // если пользователь его стёр/не указал — дописываем, иначе
                // архив без .zip будет непонятен человеку в проводнике.
                val name = if (file.endsWith(".zip", ignoreCase = true)) file else "$file.zip"
                onResult(BackupManager.createBackup(paths, dir + name))
            }
            // Если пользователь нажал "Отмена" в диалоге — dir/file будут
            // null, и мы просто ничего не делаем (не считаем это ошибкой:
            // onResult вообще не вызывается, никакого сообщения не покажется).
        }
    }
}

@Composable
actual fun rememberBackupRestorer(paths: BackupPaths, onResult: (Result<Unit>) -> Unit): () -> Unit {
    return remember(paths) {
        {
            val dialog = FileDialog(null as Frame?, "Выберите файл резервной копии", FileDialog.LOAD)
            // См. комментарий в ImagePicker.desktop.kt: setFilenameFilter не
            // работает на Windows, поэтому маска — через dialog.file.
            dialog.file = "*.zip"
            dialog.isVisible = true
            val dir = dialog.directory
            val file = dialog.file
            if (dir != null && file != null) {
                onResult(BackupManager.restoreBackup(paths, dir + file))
            }
        }
    }
}
