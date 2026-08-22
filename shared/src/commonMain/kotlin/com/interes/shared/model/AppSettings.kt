package com.interes.shared.model

/**
 * Тема оформления. Просто enum, а не Boolean isDark — на случай, если
 * позже добавится системная тема ("следовать за ОС"), не ломая формат
 * уже сохранённых настроек (см. SettingsStorage).
 */
enum class AppTheme {
    LIGHT, DARK
}

/**
 * Все настройки приложения одним объектом — то, что реально хранится
 * между сессиями (см. SettingsStorage.observeSettings()).
 *
 * windowOpacityPercent — БАЗОВАЯ прозрачность всего окна (0..100),
 * настраивается в разделе "Настройки" и сохраняется постоянно. Это
 * ОТДЕЛЬНАЯ вещь от ползунка прозрачности внутри просмотрщика фото
 * (appOpacityPercent в AppRoot.kt) — тот временный, сбрасывается на 100%
 * при каждом закрытии просмотрщика/смене фото. AppRoot.kt перемножает
 * оба значения на одном и том же graphicsLayer.alpha.
 */
data class AppSettings(
    val theme: AppTheme = AppTheme.LIGHT,
    val windowOpacityPercent: Int = 100
)
