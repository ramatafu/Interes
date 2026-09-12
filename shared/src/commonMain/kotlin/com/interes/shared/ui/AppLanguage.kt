package com.interes.shared.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * Язык интерфейса — переключается кнопкой (см. LanguageButton в
 * SideToolbar.kt), а не системной локалью. Причина, почему это НЕ через
 * Compose Multiplatform Resources (strings.xml + values-ru/values-en),
 * хотя ресурсы в проекте уже используются для иконки/шрифта: у самого
 * Compose Multiplatform на сентябрь 2026 НЕТ надёжного кросс-платформенного
 * способа переопределить локаль строковых ресурсов в рантайме (см.
 * открытые issue CMP-8376/CMP-4197 в трекере JetBrains) — попытки через
 * CompositionLocalProvider(LocalResourceLocale provides ...) на практике не
 * подхватываются самой библиотекой. Поэтому вместо XML-ресурсов — свой
 * маленький словарь (Strings.kt): каждая строка — обычная (НЕ @Composable)
 * extension-функция над AppLanguage, читающая явно переданный язык, а не
 * CompositionLocal напрямую (нужно и внутри @Composable, и внутри
 * асинхронных колбэков вне композиции — см. подробности в Strings.kt) —
 * просто, предсказуемо, работает одинаково на Android и Desktop.
 */
enum class AppLanguage {
    RU, EN;

    /** Следующий язык при тапе по кнопке-переключателю — простое чередование. */
    fun toggled(): AppLanguage = if (this == RU) EN else RU
}

/**
 * Текущий язык — предоставляется в корне (InteresRoot в AppRoot.kt) через
 * CompositionLocalProvider, читается функциями Strings.kt. Значение по
 * умолчанию (EN) используется только до первой композиции AppRoot, которая
 * сразу подставляет реально загруженное из AppLanguageStorage значение —
 * см. AppRoot.kt.
 */
val LocalAppLanguage = compositionLocalOf { AppLanguage.EN }

/**
 * Функция переключения языка — читается кнопкой-тумблером (LanguageButton
 * в SideToolbar.kt). Отдельный CompositionLocal, а не часть LocalAppLanguage:
 * сам LocalAppLanguage — обычный read-only compositionLocalOf, менять его
 * можно только передав НОВОЕ значение в CompositionLocalProvider выше по
 * дереву (см. InteresRoot в AppRoot.kt) — самой кнопке нужен доступ именно
 * к этому "передать новое значение", а не к текущему значению.
 */
val LocalAppLanguageSetter = compositionLocalOf<(AppLanguage) -> Unit> { {} }

/**
 * Хранилище выбранного языка — платформенное, по той же схеме, что и
 * BackupPaths.kt/DatabaseDriverFactory.kt: Android хранит через
 * SharedPreferences (нужен Context), Desktop — в текстовом файле рядом с
 * БД. Создаётся в MainActivity.kt/Main.kt и передаётся в InteresRoot, как
 * backupPaths.
 */
expect class AppLanguageStorage {
    fun load(): AppLanguage
    fun save(language: AppLanguage)
}
