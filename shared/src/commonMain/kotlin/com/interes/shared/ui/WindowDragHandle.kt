package com.interes.shared.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Вешается на заголовок в тулбаре экрана (см. BoardsListScreen.kt,
 * BoardScreen.kt, SettingsScreen.kt) — замена перетаскиванию/двойному клику
 * за системную рамку окна, которой больше нет (см. undecorated = true в
 * Main.kt и doc-комментарий в AppRoot.kt): перетаскивание двигает окно,
 * двойной клик — как за настоящую title bar Windows — разворачивает его на
 * весь экран/возвращает обратно. На Android — no-op (методы
 * NativeWindowController там пустые), просто ничего не произойдёт.
 *
 * Два отдельных pointerInput (а не один комбинированный жест) — стандартный
 * рабочий паттерн Compose: detectDragGestures потребляет событие, только
 * когда движение реально превысило порог "это драг, а не клик" (touch
 * slop), так что быстрый двойной клик без сдвига мыши беспрепятственно
 * долетает и до detectTapGestures ниже.
 */
fun Modifier.windowDragHandle(controller: NativeWindowController): Modifier =
    this
        .pointerInput(controller) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                controller.moveWindowBy(dragAmount.x, dragAmount.y)
            }
        }
        .pointerInput(controller) {
            detectTapGestures(onDoubleTap = { controller.toggleMaximize() })
        }
