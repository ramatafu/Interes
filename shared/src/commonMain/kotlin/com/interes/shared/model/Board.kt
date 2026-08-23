package com.interes.shared.model

/**
 * Доска — тематическая коллекция фотографий (аналог доски в Pinterest).
 *
 * @param deletedAt null — доска активна. Не null — доска в Корзине (мягкое
 *                  удаление, см. BoardRepository.softDeleteBoard/TrashScreen.kt) —
 *                  метка времени, когда её туда переместили.
 */
data class Board(
    val id: Long,
    val title: String,
    val category: String,
    val createdAt: Long,
    val deletedAt: Long? = null
)
