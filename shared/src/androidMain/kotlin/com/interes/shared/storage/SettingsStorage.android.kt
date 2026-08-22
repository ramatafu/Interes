package com.interes.shared.storage

import android.content.Context
import android.content.SharedPreferences
import com.interes.shared.model.AppSettings
import com.interes.shared.model.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class SettingsStorage(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("interes_settings", Context.MODE_PRIVATE)
    }

    private val state = MutableStateFlow(load())

    private fun load(): AppSettings {
        val themeName = prefs.getString("theme", AppTheme.LIGHT.name) ?: AppTheme.LIGHT.name
        val theme = runCatching { AppTheme.valueOf(themeName) }.getOrDefault(AppTheme.LIGHT)
        val opacity = prefs.getInt("windowOpacityPercent", 100)
        return AppSettings(theme = theme, windowOpacityPercent = opacity.coerceIn(0, 100))
    }

    actual fun observeSettings(): StateFlow<AppSettings> = state

    actual fun setTheme(theme: AppTheme) {
        state.value = state.value.copy(theme = theme)
        prefs.edit().putString("theme", theme.name).apply()
    }

    actual fun setWindowOpacityPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        state.value = state.value.copy(windowOpacityPercent = clamped)
        prefs.edit().putInt("windowOpacityPercent", clamped).apply()
    }
}
