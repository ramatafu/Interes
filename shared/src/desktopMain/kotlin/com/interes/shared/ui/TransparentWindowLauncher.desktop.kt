package com.interes.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.interes.shared.model.Photo

/**
 * Список сейчас открытых прозрачных окон на Desktop. Читается из
 * application { } в точке входа (Main.kt), т.к. новое Window нельзя
 * "запустить" императивно откуда угодно — оно должно быть частью дерева
 * композиции внутри ApplicationScope.
 */
object DesktopTransparentWindows {
    val open = mutableStateListOf<Photo>()
}

@Composable
actual fun rememberTransparentWindowLauncher(): (Photo) -> Unit {
    return { photo ->
        if (DesktopTransparentWindows.open.none { it.id == photo.id }) {
            DesktopTransparentWindows.open.add(photo)
        }
    }
}
