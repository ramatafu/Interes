package com.interes.shared.model

/**
 * Доска — тематическая коллекция фотографий (аналог доски в Pinterest).
 */
data class Board(
    val id: Long,
    val title: String,
    val category: String,
    val createdAt: Long
)
