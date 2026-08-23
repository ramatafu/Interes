package com.interes.shared.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Вешается на заголовок в тулбаре экрана (см. BoardScreen.kt, TrashScreen.kt)
 * — замена перетаскиванию за системную рамку окна, которой больше нет (см.
 * undecorated = true в Main.kt и doc-комментарий в AppRoot.kt). На Android —
 * no-op (NativeWindowController.moveWindowBy там пустой).
 *
 * РАНЬШЕ здесь были ДВА pointerInput на одном модификаторе — этот (drag) и
 * ещё один с detectTapGestures(onDoubleTap = ...) для разворачивания окна.
 * Похоже, именно это и ломало перетаскивание целиком: два независимых
 * распознавателя жестов на одном узле конкурируют за один и тот же поток
 * касаний, и в некоторых случаях detectTapGestures "съедал" начало жеста
 * раньше, чем detectDragGestures успевал распознать его как драг. Двойной
 * клик для разворачивания убран отсюда — разворачивание окна теперь только
 * через отдельную кнопку в SideToolbar/BoardsListScreen (toggleMaximize
 * остался в NativeWindowController, просто вызывается по-другому).
 */
fun Modifier.windowDragHandle(controller: NativeWindowController): Modifier =
    this.pointerInput(controller) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            controller.moveWindowBy(dragAmount.x, dragAmount.y)
        }
    }
