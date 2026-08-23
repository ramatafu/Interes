package com.interes.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.interes.shared.db.InteresDatabase
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import com.interes.shared.storage.DatabaseDriverFactory
import com.interes.shared.storage.PhotoFileStorage
import com.interes.shared.ui.InteresRoot
import com.interes.shared.ui.NativeWindowController
import com.interes.shared.ui.ViewerKeys
import kotlinx.coroutines.Dispatchers

fun main() = application {
    val driver = DatabaseDriverFactory().createDriver()
    val db = InteresDatabase(driver)
    val repository = BoardRepository(db, PhotoFileStorage(), Dispatchers.IO)
    val backupPaths = BackupPaths()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Interes",
        undecorated = true,
        transparent = true,
        // Пролистывание просмотрщика с клавиатуры: ← / →.
        // Ловим на уровне ОКНА — фокус на фото не нужен.
        // Работает только пока открыт просмотрщик (иначе лямбды null).
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                val handler = when (event.key) {
                    Key.DirectionLeft -> ViewerKeys.onLeft
                    Key.DirectionRight -> ViewerKeys.onRight
                    else -> null
                }
                if (handler != null) {
                    handler()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    ) {
        val nativeWindowController = NativeWindowController(window)
        InteresRoot(repository, backupPaths, nativeWindowController, onExitApp = ::exitApplication)
    }
}