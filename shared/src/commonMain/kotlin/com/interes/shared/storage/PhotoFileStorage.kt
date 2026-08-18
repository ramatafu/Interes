package com.interes.shared.storage

/**
 * Копирует выбранное пользователем изображение в приватную папку приложения
 * и возвращает локальный путь + реальные размеры (нужны для staggered-grid).
 *
 * Реализация платформозависима: на Android источник — Uri из SAF-пикера,
 * на Desktop — java.io.File из системного FileDialog. Общий код (Repository)
 * работает только с результатом — путём и размерами, не заботясь о том,
 * как файл туда попал.
 */
expect class PhotoFileStorage {
    fun importPhoto(sourcePath: String): ImportedPhotoFile

    /** Удаляет скопированный файл фото с диска (при удалении фото с доски). */
    fun deletePhotoFile(storedPath: String)
}

data class ImportedPhotoFile(
    val storedPath: String,
    val width: Int,
    val height: Int
)
