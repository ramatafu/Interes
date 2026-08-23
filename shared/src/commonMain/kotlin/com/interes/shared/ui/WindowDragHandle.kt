package com.interes.shared.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Вешается на заголовок в тулбаре экрана (см. BoardsListScreen.kt,
 * BoardScreen.kt, TrashScreen.kt) — замена перетаскиванию за системную рамку
 * окна, которой больше нет (см. undecorated = true в Main.kt и doc-комментарий
 * в AppRoot.kt). На Android — no-op (NativeWindowController там заглушка).
 *
 * ПОЧЕМУ НЕ инкрементально (moveWindowBy на каждый dragAmount): окно движется
 * ВМЕСТЕ с курсором, поэтому локальные координаты указателя "уплывают" под
 * ним — дельты то удваиваются, то обнуляются, окно вибрирует, вырывается
 * из-под мыши, и отпускание кнопки случается где попало (в т.ч. над кнопкой
 * "Свернуть" или над панелью задач) — окно вело себя как будто само
 * сворачивается вместо перемещения.
 *
 * Здесь расчёт АБСОЛЮТНЫЙ: запоминаем позицию окна и локальную координату
 * нажатия в момент зажатия, а на каждое движение ставим окно в
 * startWindowPos + реальное_смещение_мыши_по_экрану. Реальное смещение =
 * (текущая позиция окна − стартовая) + (локальная координата − координата
 * нажатия): локальный "прыжок", вызванный сдвигом самого окна, в точности
 * компенсируется прибавкой сдвига окна, и сумма всегда равна истинному
 * перемещению курсора по экрану. Окно следует за мышью 1:1, без вибрации.
 */
fun Modifier.windowDragHandle(controller: NativeWindowController): Modifier =
    this.pointerInput(controller) {
        // Позиция окна (экранные координаты) и координата нажатия (локальные
        // координаты) в момент начала жеста.
        var originX = 0
        var originY = 0
        var pressX = 0f
        var pressY = 0f

        detectDragGestures(
            onDragStart = { offset ->
                val pos = controller.getWindowPosition()
                originX = pos.first
                originY = pos.second
                pressX = offset.x
                pressY = offset.y
            }
        ) { change, _ ->
            change.consume()
            val cur = controller.getWindowPosition()
            val totalX = (cur.first - originX) + (change.position.x - pressX)
            val totalY = (cur.second - originY) + (change.position.y - pressY)
            controller.setWindowPosition(
                originX + totalX.toInt(),
                originY + totalY.toInt()
            )
        }
    }