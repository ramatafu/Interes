package com.interes.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.interes.shared.db.InteresDatabase
import com.interes.shared.generated.resources.Res
import com.interes.shared.generated.resources.app_icon
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import com.interes.shared.storage.DatabaseDriverFactory
import com.interes.shared.storage.PhotoFileStorage
import com.interes.shared.ui.InteresRoot
import com.interes.shared.ui.NativeWindowController
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    val driver = DatabaseDriverFactory().createDriver()
    val db = InteresDatabase(driver)
    val repository = BoardRepository(db, PhotoFileStorage(), Dispatchers.IO)
    val backupPaths = BackupPaths()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Interes",
        // Иконка окна: показывается в панели задач Windows, при Alt+Tab и
        // (если бы у окна была системная рамка) в её левом углу. Иконка
        // самого .exe/ярлыка на рабочем столе после установки задаётся
        // отдельно — см. iconFile в desktopApp/build.gradle.kts.
        icon = painterResource(Res.drawable.app_icon),
        undecorated = true,
        transparent = true
    ) {
        val nativeWindowController = NativeWindowController(window)
        InteresRoot(repository, backupPaths, nativeWindowController, onExitApp = ::exitApplication)
    }
}