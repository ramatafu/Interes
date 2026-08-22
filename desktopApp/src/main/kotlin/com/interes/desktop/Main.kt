package com.interes.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.interes.shared.db.InteresDatabase
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import com.interes.shared.storage.DatabaseDriverFactory
import com.interes.shared.storage.PhotoFileStorage
import com.interes.shared.storage.SettingsStorage
import com.interes.shared.ui.InteresRoot
import com.interes.shared.ui.NativeWindowController
import kotlinx.coroutines.Dispatchers

/**
 * undecorated = true — ОБЯЗАТЕЛЬНОЕ условие для настоящей прозрачности окна
 * на Windows. java.awt.Window.setOpacity(float) (см. NativeWindowController.desktop.kt)
 * бросает IllegalComponentStateException при значении меньше 1.0f на ОБЫЧНОМ
 * (decorated) окне — задокументированное ограничение самого AWT, не баг
 * этого проекта: https://docs.oracle.com/javase/8/docs/api/java/awt/Frame.html
 * ("IllegalComponentStateException - if undecorated is false, and this frame
 * opacity is less than 1.0f"). Раньше это исключение тихо проглатывалось
 * runCatching{} внутри setOpacityPercent — ползунок двигался, а окно
 * оставалось полностью непрозрачным без единой видимой ошибки.
 *
 * НЕ transparent = true — это другая, ненужная здесь настройка (попиксельная
 * прозрачность конкретных участков окна — например, чтобы вырезать окно
 * фигурной формы). Нам нужна равномерная прозрачность ВСЕГО окна разом —
 * ровно то, что даёт window.opacity без transparent.
 *
 * Раз убрали системную рамку — вместе с ней пропали стандартные
 * возможности Windows двигать/закрывать окно за title bar. Компенсировано
 * в самом приложении: перетаскивание — зажатием и перетаскиванием заголовка
 * "Interes"/"Настройки"/названия доски в верхней панели любого экрана (см.
 * TopAppBar в BoardsListScreen.kt/BoardScreen.kt/SettingsScreen.kt — там
 * Modifier.pointerInput + detectDragGestures двигают nativeWindowController),
 * закрытие — иконка "✕" в тулбаре списка досок (onExitApp) ИЛИ штатное для
 * Windows Alt+F4 (работает независимо от наличия рамки — это системный
 * шорткат уровня ОС, не завязанный на видимую title bar).
 */
fun main() = application {
    val driver = DatabaseDriverFactory().createDriver()
    val db = InteresDatabase(driver)
    val repository = BoardRepository(db, PhotoFileStorage(), Dispatchers.IO)
    val settingsStorage = SettingsStorage()
    val backupPaths = BackupPaths()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Interes",
        undecorated = true
    ) {
        // window — из WindowScope, доступен только внутри этого блока,
        // поэтому NativeWindowController создаётся именно здесь, а не
        // выше вместе с остальными зависимостями.
        val nativeWindowController = NativeWindowController(window)
        InteresRoot(repository, settingsStorage, backupPaths, nativeWindowController, onExitApp = ::exitApplication)
    }
}
