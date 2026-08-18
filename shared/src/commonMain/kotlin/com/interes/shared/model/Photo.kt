package com.interes.shared.model

/**
 * Фотография внутри доски.
 *
 * @param filePath абсолютный путь к файлу в локальном хранилище приложения
 *                 (никогда не URL — приложение офлайн).
 * @param orderIndex позиция внутри доски; меняется при drag-n-drop
 *                   и используется для сортировки в сетке.
 * @param width/height исходные размеры изображения — нужны, чтобы
 *                     staggered-grid мог сразу посчитать высоту ячейки
 *                     без декодирования файла на каждый кадр.
 */
data class Photo(
    val id: Long,
    val boardId: Long,
    val filePath: String,
    val orderIndex: Int,
    val width: Int,
    val height: Int,
    val addedAt: Long
)
