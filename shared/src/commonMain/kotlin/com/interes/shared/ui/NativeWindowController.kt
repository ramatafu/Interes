package com.interes.shared.ui

/**
 * Управляет прозрачностью и положением САМОГО ОКНА на уровне ОС (не только
 * содержимого внутри Compose) — то, что нужно, чтобы сквозь окно Interes
 * реально было видно рабочий стол Windows и другие приложения под ним, а не
 * просто затемнялось содержимое внутри непрозрачного окна.
 *
 * Только Desktop способен на это через java.awt.Window (см.
 * NativeWindowController.desktop.kt). На Android аналогичный эффект уже
 * достигается ДРУГИМ путём — через полупрозрачную тему Activity
 * (androidApp/.../themes.xml) + Compose graphicsLayer.alpha в AppRoot.kt —
 * поэтому actual-класс там пустой (см. NativeWindowController.android.kt).
 *
 * handlesOpacityNatively — не просто "какая платформа сейчас": даже на
 * Windows примерно нулевая, но всё же существующая вероятность, что видеокарта
 * / графический стек не поддерживает оконную полупрозрачность
 * (GraphicsDevice.isWindowTranslucencySupported). AppRoot.kt читает этот
 * флаг и в таком случае откатывается на старый способ (Compose
 * graphicsLayer.alpha — просто гасит содержимое, без реального сквозного
 * эффекта) вместо того, чтобы ползунок молча ничего не делал.
 */
expect class NativeWindowController {
    val handlesOpacityNatively: Boolean
    fun setOpacityPercent(percent: Int)

    /**
     * Позиция левого верхнего угла окна в экранных координатах — нужна
     * перетаскиванию окна (windowDragHandle) для абсолютного расчёта
     * смещения: окно должно следовать за мышью 1:1, без "вибрации",
     * которую давал старый инкрементальный алгоритм (moveWindowBy).
     * На Android 0 to 0 — там нет отдельного плавающего окна.
     */
    fun getWindowPosition(): Pair<Int, Int>

    /** Ставит окно левым верхним углом в (x, y) экранных координат.
     * На Android пустая реализация. */
    fun setWindowPosition(x: Int, y: Int)

    /**
     * Сдвигает окно на (dxPx, dyPx) пикселей — оставлено для совместимости,
     * перетаскивание теперь идёт через getWindowPosition/setWindowPosition.
     * На Android пустая реализация — там нет отдельного плавающего окна.
     */
    fun moveWindowBy(dxPx: Float, dyPx: Float)

    /** Разворачивает окно на весь экран / возвращает обратно. На Android
     * пустая реализация — там своя модель полноэкранности через Activity. */
    fun toggleMaximize()

    /** Сворачивает окно в панель задач — замена системной кнопки
     * "свернуть", которой больше нет. На Android пустая реализация. */
    fun minimize()
}