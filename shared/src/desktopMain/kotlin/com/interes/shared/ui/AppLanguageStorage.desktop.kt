package com.interes.shared.ui

import java.io.File

actual class AppLanguageStorage {
    // Тот же "APPDATA/Interes" путь, что и в BackupPaths.desktop.kt/
    // DatabaseDriverFactory.desktop.kt — см. комментарий там же, почему
    // сознательно продублировано, а не вынесено в общий модуль.
    private val file: File = File(
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Interes"),
        "language.txt"
    )

    actual fun load(): AppLanguage {
        if (!file.exists()) return AppLanguage.EN
        return runCatching { AppLanguage.valueOf(file.readText().trim()) }.getOrDefault(AppLanguage.EN)
    }

    actual fun save(language: AppLanguage) {
        file.parentFile?.mkdirs()
        file.writeText(language.name)
    }
}
