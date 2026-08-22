package com.interes.shared.storage

import com.interes.shared.model.AppSettings
import com.interes.shared.model.AppTheme
import java.io.File
import java.util.Properties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class SettingsStorage {
    // Та же папка, что и у БД/фото (см. DatabaseDriverFactory.desktop.kt,
    // PhotoFileStorage.desktop.kt) — не размазываем данные приложения по
    // разным местам диска.
    private val file = File(
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Interes"),
        "settings.properties"
    )

    private val state = MutableStateFlow(load())

    private fun load(): AppSettings {
        if (!file.exists()) return AppSettings()
        val props = Properties()
        runCatching { file.inputStream().use { props.load(it) } }
        val theme = runCatching { AppTheme.valueOf(props.getProperty("theme", AppTheme.LIGHT.name)) }
            .getOrDefault(AppTheme.LIGHT)
        val opacity = props.getProperty("windowOpacityPercent", "100").toIntOrNull() ?: 100
        return AppSettings(theme = theme, windowOpacityPercent = opacity.coerceIn(0, 100))
    }

    private fun persist(settings: AppSettings) {
        val props = Properties()
        props.setProperty("theme", settings.theme.name)
        props.setProperty("windowOpacityPercent", settings.windowOpacityPercent.toString())
        file.parentFile?.mkdirs()
        runCatching { file.outputStream().use { props.store(it, "Interes settings") } }
    }

    actual fun observeSettings(): StateFlow<AppSettings> = state

    actual fun setTheme(theme: AppTheme) {
        val updated = state.value.copy(theme = theme)
        state.value = updated
        persist(updated)
    }

    actual fun setWindowOpacityPercent(percent: Int) {
        val updated = state.value.copy(windowOpacityPercent = percent.coerceIn(0, 100))
        state.value = updated
        persist(updated)
    }
}
