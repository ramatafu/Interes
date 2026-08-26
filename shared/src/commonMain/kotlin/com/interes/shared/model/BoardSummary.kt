package com.interes.shared.model

/**
 * Доска для списка досок: те же поля, что у Board, плюс пути к первым
 * (по orderIndex) фото доски — для коллажа-превью "1+2" на карточке
 * (см. BoardCard в BoardsListScreen.kt) — и общее количество фото в
 * доске. thumbnailPaths может содержать от 0 до 3 путей: 0 — доска
 * пустая (тогда photoCount тоже 0 и рисуется заглушка), меньше 3 —
 * коллаж показывает только то, что есть.
 */
data class BoardSummary(
    val id: Long,
    val title: String,
    val category: String,
    val createdAt: Long,
    val thumbnailPaths: List<String>,
    val photoCount: Int
)
