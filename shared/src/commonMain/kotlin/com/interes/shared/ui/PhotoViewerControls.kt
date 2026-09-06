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
 * "Свернуть/Развернуть/Закрыть" здесь СПЕЦИАЛЬНО не дублируются — у самой
 * доски (BoardScreen.kt) уже есть свой верхний тулбар с этими кнопками, и
 * он остаётся видимым поверх фото (см. отступ top/bottom = TopToolbarHeight
 * у PhotoViewerContent в AppRoot.kt — фото не заходит под эту полосу).
 * Поэтому счётчик и кнопка "выйти из просмотра" здесь размещены НИЖЕ этой
 * полосы (padding сверху = TopToolbarHeight + 16.dp), а не поверх неё —
 * иначе они перекрывали бы стрелку "←" и название доски.
 *
 * Раскладка:
 * - счётчик "1 / 2" — слева, чуть ниже верхнего тулбара доски;
 * - кнопка "выйти из просмотра" (✕, возврат к доске, НЕ закрытие окна) —
 *   справа, на той же высоте. Управляется showDismissButton: на Android
 *   её нет вовсе (выход — системная кнопка/жест "Назад", см.
 *   PlatformBackHandler.android.kt), а вот на Desktop эта кнопка —
 *   ЕДИНСТВЕННЫЙ способ закрыть просмотрщик (у Desktop нет аналога системной
 *   кнопки "Назад" — PlatformBackHandler.desktop.kt пустой), поэтому там
 *   showDismissButton остаётся true (значение по умолчанию).
 *
 * Ползунок прозрачности здесь НЕ рисуется — см. OpacitySliderBar.kt:
 * - на Desktop он в RightToolbar.kt (вертикальный, справа);
 * - на Android — в отдельном системном окне (NativeWindowController
 *   .android.kt, showOpacitySlider), а не в этом Compose-дереве: главное
 *   окно Activity целиком гаснет вместе с прозрачностью, и если бы ползунок
 *   рисовался здесь же, он гас бы вместе с фото — то есть был бы неудобен
 *   именно тогда, когда нужнее всего (на низкой прозрачности).
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
    modifier: Modifier = Modifier,
    showDismissButton: Boolean = true
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Счётчик страниц — слева, ниже верхнего тулбара доски.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = TopToolbarHeight + 16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("${currentPage + 1} / $pageCount", color = Color.White, fontSize = 14.sp)
        }

        // Кнопка "выйти из просмотра" (вернуться к доске) — справа, на той
        // же высоте, что и счётчик. Отдельная функция от закрытия окна —
        // это НЕ дублирует кнопки доски, а именно возврат к сетке фото.
        if (showDismissButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = TopToolbarHeight + 16.dp)
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
}
