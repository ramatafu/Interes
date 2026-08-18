package com.interes.shared.ui

import androidx.compose.runtime.Composable

/**
 * Возвращает функцию-триггер: вызов открывает системный выбор изображений.
 * Результат (список путей/Uri в виде строк — на Android это content://,
 * на Desktop обычный абсолютный путь) приходит в [onPicked].
 *
 * Строки, а не java.io.File/Uri напрямую — чтобы не тащить платформенные
 * типы в общий код; конкретный формат разбирает PhotoFileStorage на своей
 * платформе (см. shared/storage).
 */
@Composable
expect fun rememberImagePicker(onPicked: (List<String>) -> Unit): () -> Unit
