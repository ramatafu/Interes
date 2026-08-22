package com.interes.shared.storage

import java.io.File

actual class BackupPaths {
    // Те же пути, что в DatabaseDriverFactory.desktop.kt и
    // PhotoFileStorage.desktop.kt — сознательно продублировано (один
    // File(...) на класс), а не вынесено в общую функцию: три файла и так
    // уже более чем достаточно связаны через одинаковый "APPDATA/Interes"
    // путь, заводить ради этого ещё один общий модуль — лишнее усложнение
    // на ровном месте.
    private val appDataDir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Interes")

    actual val databaseFilePath: String = File(appDataDir, "interes.db").absolutePath
    actual val photosDirPath: String = File(appDataDir, "photos").absolutePath
}
