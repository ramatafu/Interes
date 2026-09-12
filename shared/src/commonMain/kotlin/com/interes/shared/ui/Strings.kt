package com.interes.shared.ui

/**
 * Единый словарь пользовательских строк интерфейса — набор extension-функций
 * над AppLanguage (а не @Composable-функций, читающих LocalAppLanguage
 * напрямую): некоторые из этих строк собираются внутри НЕ-composable
 * колбэков (например, результат восстановления из резервной копии в
 * BackupButton — приходит асинхронно, уже вне композиции), где
 * LocalAppLanguage.current физически недоступен. Явный параметр-получатель
 * работает одинаково в обоих случаях — внутри @Composable вызывается как
 * `LocalAppLanguage.current.trash()`, вне композиции — как
 * `capturedLanguage.trash()`, где capturedLanguage прочитан ЗАРАНЕЕ, ещё
 * находясь в композиции (см. BackupButton в SideToolbar.kt).
 *
 * Имена функций, а не строковые ключи — опечатка в обращении ловится
 * компилятором, а не рантаймом.
 *
 * ЧТО НЕ ПОКРЫТО этим словарём (сознательно):
 * - Сообщения об ошибках, брошенные из PhotoFileStorage.*.kt/
 *   BackupManager.kt (глубоко внутри repository/storage, не рядом с местом
 *   вызова) — остаются на английском как техническая деталь внутри уже
 *   локализованного текста-обёртки (см. backupCreateFailed ниже — обёртка
 *   локализуется, "reason" внутри неё — нет).
 * - Заголовок нативного диалога выбора файла на Desktop (java.awt.FileDialog
 *   в ImagePicker.desktop.kt/BackupActions.desktop.kt) — рисуется самой ОС,
 *   а не Compose.
 * - Название приложения "Interes" — не переводится ни в каком языке.
 */

private fun AppLanguage.pick(ru: String, en: String): String = if (this == AppLanguage.RU) ru else en

// --- Общие кнопки/слова, переиспользуются в нескольких диалогах ---
fun AppLanguage.cancel(): String = pick("Отмена", "Cancel")
fun AppLanguage.delete(): String = pick("Удалить", "Delete")
fun AppLanguage.ok(): String = pick("ОК", "OK")
fun AppLanguage.save(): String = pick("Сохранить", "Save")
fun AppLanguage.create(): String = pick("Создать", "Create")

// --- SideToolbar.kt / RightToolbar.kt: значки и общие кнопки тулбаров ---
fun AppLanguage.trash(): String = pick("Корзина", "Trash")
fun AppLanguage.home(): String = pick("Домой", "Home")
fun AppLanguage.newBoardAction(): String = pick("Создать доску", "New board")
fun AppLanguage.backup(): String = pick("Резервная копия", "Backup")
fun AppLanguage.createBackup(): String = pick("Создать резервную копию", "Create backup")
fun AppLanguage.restoreFromBackup(): String = pick("Восстановить из резервной копии", "Restore from backup")
fun AppLanguage.backupCreated(): String = pick("Резервная копия успешно создана.", "Backup created successfully.")

// reason — уже готовая строка сообщения исключения, английская (см.
// doc-комментарий файла про то, что бросается вне зоны локализации).
fun AppLanguage.backupCreateFailed(reason: String): String =
    pick("Не удалось создать копию: $reason", "Failed to create backup: $reason")

fun AppLanguage.restoreSucceeded(): String = pick(
    "Восстановлено. Перезапустите Interes, чтобы изменения вступили в силу.",
    "Restored. Restart Interes for the changes to take effect."
)

fun AppLanguage.restoreFailed(reason: String): String =
    pick("Не удалось восстановить: $reason", "Failed to restore: $reason")

fun AppLanguage.error(): String = pick("Ошибка", "Error")
fun AppLanguage.done(): String = pick("Готово", "Done")
fun AppLanguage.restartNow(): String = pick("Перезапустить сейчас", "Restart now")
fun AppLanguage.about(): String = pick("О программе", "About")
fun AppLanguage.appVersion(version: String): String = pick("Версия $version", "Version $version")

fun AppLanguage.appDescription(): String = pick(
    "Оффлайн-приложение для досок визуализации: собирайте и организуйте фотографии по темам и категориям.",
    "Offline app for visualization boards: collect and organize photos by theme and category."
)

fun AppLanguage.developmentLabel(): String = pick("Разработка:", "Development:")
fun AppLanguage.creditsIdea(): String = pick("Идея, дизайн, тестирование: ram", "Idea, design, testing: ram")
fun AppLanguage.creditsCode(): String = pick(
    "Генерация кода: Claude Code (Anthropic)",
    "Code generation: Claude Code (Anthropic)"
)
fun AppLanguage.creditsConsulting(): String = pick("Консультация: DeepSeek", "Consulting: DeepSeek")
fun AppLanguage.license(): String = pick(
    "Лицензия: GNU General Public License v3.0 (GPLv3).",
    "License: GNU General Public License v3.0 (GPLv3)."
)
fun AppLanguage.close(): String = pick("Закрыть", "Close")

// Подпись кнопки-переключателя языка — сама кнопка показывает язык, НА
// КОТОРЫЙ переключит, а не текущий (как в большинстве приложений).
fun AppLanguage.switchToLanguageLabel(): String = pick("English", "Русский")

// --- RightToolbar.kt ---
fun AppLanguage.addPhoto(): String = pick("Добавить фото", "Add photo")
fun AppLanguage.opacity(): String = pick("Прозрачность", "Opacity")
fun AppLanguage.percentLabel(): String = pick("Процент (0–100)", "Percent (0–100)")
fun AppLanguage.apply(): String = pick("Применить", "Apply")

// --- BoardsListScreen.kt ---
fun AppLanguage.noBoardsYet(): String = pick(
    "Пока нет ни одной доски — нажмите \"+\", чтобы создать первую",
    "No boards yet — tap \"+\" to create your first one"
)

fun AppLanguage.noSearchResults(query: String): String =
    pick("Ничего не найдено по запросу \"$query\"", "No results for \"$query\"")

fun AppLanguage.searchPlaceholder(): String = pick("Поиск доски...", "Search boards...")
fun AppLanguage.rename(): String = pick("Переименовать", "Rename")
fun AppLanguage.deleteBoardTitle(): String = pick("Удалить доску?", "Delete board?")

fun AppLanguage.deleteBoardText(boardTitle: String): String = pick(
    "Доска \"$boardTitle\" переместится в Корзину. Оттуда её можно будет восстановить или удалить навсегда.",
    "Board \"$boardTitle\" will move to Trash. From there it can be restored or deleted permanently."
)

fun AppLanguage.newBoardDialogTitle(): String = pick("Новая доска", "New board")
fun AppLanguage.titleLabel(): String = pick("Название", "Title")
fun AppLanguage.categoryLabel(): String = pick("Категория", "Category")
fun AppLanguage.defaultCategory(): String = pick("Общее", "General")
fun AppLanguage.renameBoardTitle(): String = pick("Переименовать доску", "Rename board")

// "доска" склоняется по числам в русском (1 доска, 2 доски, 5 досок), в
// английском — только единственное/множественное (1 board/N boards).
fun AppLanguage.boardsWord(count: Int): String {
    if (this == AppLanguage.EN) return if (count == 1) "board" else "boards"
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "досок"
        mod10 == 1 -> "доска"
        mod10 in 2..4 -> "доски"
        else -> "досок"
    }
}

// "фото" не склоняется в русском ни при каком числе; в английском —
// обычное единственное/множественное.
fun AppLanguage.photosWord(count: Int): String {
    if (this == AppLanguage.EN) return if (count == 1) "photo" else "photos"
    return "фото"
}

// --- TrashScreen.kt ---
fun AppLanguage.trashIsEmpty(): String = pick("Корзина пуста", "Trash is empty")
fun AppLanguage.restore(): String = pick("Восстановить", "Restore")
fun AppLanguage.deletePermanently(): String = pick("Удалить навсегда", "Delete permanently")
fun AppLanguage.deletePermanentlyTitle(): String = pick("Удалить навсегда?", "Delete permanently?")

fun AppLanguage.deletePermanentlyText(boardTitle: String): String = pick(
    "Доска \"$boardTitle\" и все фото на ней будут удалены безвозвратно. Отменить это будет нельзя.",
    "Board \"$boardTitle\" and all its photos will be deleted permanently. This cannot be undone."
)

// --- BoardScreen.kt ---
fun AppLanguage.addPhotosFailed(failedCount: Int): String {
    val word = if (this == AppLanguage.RU) {
        if (failedCount == 1) "файл" else "файла"
    } else {
        if (failedCount == 1) "file" else "files"
    }
    return pick(
        "Не удалось добавить $failedCount $word — подробности в консоли",
        "Failed to add $failedCount $word — see console for details"
    )
}

fun AppLanguage.deletePhotoTitle(): String = pick("Удалить фото?", "Delete photo?")
fun AppLanguage.deletePhotoText(): String = pick(
    "Фото будет удалено с доски безвозвратно.",
    "The photo will be permanently removed from the board."
)
