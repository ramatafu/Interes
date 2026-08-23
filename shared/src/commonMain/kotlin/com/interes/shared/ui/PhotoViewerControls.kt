package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Элементы управления просмотрщика. Нарисованы СНАРУЖИ затухающего слоя
 * (AppRoot.kt), поэтому всегда полностью непрозрачны.
 *
 * Раскладка:
 * - счётчик "1 / 2" — сверху СЛЕВА;
 * - кнопка закрытия ✕ — сверху СПРАВА.
 *
 * ВАЖНО: Box обязан быть fillMaxSize(), иначе он сожмётся до размера
 * кнопок и оба элемента прилипнут к левому верхнему углу (align работает
 * ВНУТРИ границ этого Box).
 */
@Composable
fun PhotoViewerControls(
    pageCount: Int,
    currentPage: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Счётчик страниц — сверху слева.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("${currentPage + 1} / $pageCount", color = Color.White, fontSize = 14.sp)
        }

        // Кнопка "Закрыть" — сверху СПРАВА.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2715", color = Color.White, fontSize = 20.sp)
        }
    }
}