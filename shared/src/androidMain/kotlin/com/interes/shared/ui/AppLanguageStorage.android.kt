package com.interes.shared.ui

import android.content.Context

actual class AppLanguageStorage(private val context: Context) {
    private val prefs by lazy { context.getSharedPreferences("interes_prefs", Context.MODE_PRIVATE) }

    actual fun load(): AppLanguage {
        val stored = prefs.getString(KEY, null) ?: return AppLanguage.EN
        return runCatching { AppLanguage.valueOf(stored) }.getOrDefault(AppLanguage.EN)
    }

    actual fun save(language: AppLanguage) {
        prefs.edit().putString(KEY, language.name).apply()
    }

    private companion object {
        const val KEY = "language"
    }
}
