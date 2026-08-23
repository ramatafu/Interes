package com.interes.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.interes.shared.db.InteresDatabase
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import com.interes.shared.storage.DatabaseDriverFactory
import com.interes.shared.storage.PhotoFileStorage
import com.interes.shared.ui.InteresRoot
import com.interes.shared.ui.NativeWindowController
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
        transparent = true
    ) {
        val nativeWindowController = NativeWindowController(window)
        InteresRoot(repository, backupPaths, nativeWindowController, onExitApp = ::exitApplication)
    }
}