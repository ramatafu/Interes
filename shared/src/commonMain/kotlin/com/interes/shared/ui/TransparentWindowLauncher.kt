package com.interes.shared.ui

import androidx.compose.runtime.Composable
import com.interes.shared.model.Photo

/**
 * Возвращает функцию: вызов открывает "прозрачное окно" с конкретным фото —
 * обязательное условие приложения.
 *
 * - Android: плавающее окно поверх других приложений (floating overlay),
 *   требует разрешение "Отображение поверх других приложений"; если его
 *   ещё нет, сама функция откроет системный экран запроса, а окно покажет
 *   сразу после того, как пользователь его выдаст.
 * - Windows: отдельное окно Compose Desktop без рамки и с прозрачным фоном.
 */
@Composable
expect fun rememberTransparentWindowLauncher(): (Photo) -> Unit
