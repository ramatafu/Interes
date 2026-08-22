package com.interes.shared.storage

import com.interes.shared.model.AppSettings
import com.interes.shared.model.AppTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Хранилище настроек приложения — тема, прозрачность окна. Persist между
 * сессиями на обеих платформах, интерфейс единый (expect/actual), как и у
 * DatabaseDriverFactory/PhotoFileStorage.
 *
 * НЕ androidx.datastore.preferences (то, что обычно советуют для Android):
 * это Android-специфичная библиотека, и подключать её ТОЛЬКО ради нескольких
 * простых boolean/int-настроек — лишняя зависимость с собственным графом
 * версий. Учитывая, сколько проблем в этом проекте уже вызвали конфликты
 * версий Compose-зависимостей между :shared и desktopApp (см. историю
 * правок в shared/build.gradle.kts), решили НЕ рисковать так же с ещё одной
 * библиотекой. Вместо неё: SharedPreferences на Android (штатный API
 * платформы, без дополнительных зависимостей) и обычный properties-файл на
 * Desktop (в том же APPDATA/Interes, где уже лежат photos/ и interes.db —
 * см. DatabaseDriverFactory.desktop.kt). Оба варианта одинаково надёжно
 * переживают перезапуск приложения — сути задачи ("сохраняются между
 * сессиями") это не меняет.
 *
 * StateFlow, а не Flow — настройки нужны СРАЗУ при старте экрана (тема
 * оборачивает вообще весь UI в AppRoot.kt), а не только после первой
 * асинхронной эмиссии.
 */
expect class SettingsStorage {
    fun observeSettings(): StateFlow<AppSettings>
    fun setTheme(theme: AppTheme)
    fun setWindowOpacityPercent(percent: Int)
}
