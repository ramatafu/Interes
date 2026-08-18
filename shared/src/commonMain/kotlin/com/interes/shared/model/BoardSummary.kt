package com.interes.shared.model

/**
 * Доска для списка досок: те же поля, что у Board, плюс путь к первому
 * фото доски (для превью на карточке). thumbnailPath = null, если доска
 * ещё пустая.
 */
data class BoardSummary(
    val id: Long,
    val title: String,
    val category: String,
    val createdAt: Long,
    val thumbnailPath: String?
)
