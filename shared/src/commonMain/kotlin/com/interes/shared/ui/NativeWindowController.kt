package com.interes.shared.ui

/**
 * Управляет прозрачностью САМОГО ОКНА на уровне ОС (не только содержимого
 * внутри Compose) — то, что нужно, чтобы сквозь окно Interes реально было
 * видно рабочий стол Windows и другие приложения под ним, а не просто
 * затемнялось содержимое внутри непрозрачного окна.
 *
 * Только Desktop способен на это через java.awt.Window.opacity (см.
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
     * Сдвигает окно на (dxPx, dyPx) пикселей — замена перетаскиванию за
     * системную рамку, которой больше нет (см. undecorated = true в Main.kt).
     * На Android пустая реализация — там нет отдельного плавающего окна.
     */
    fun moveWindowBy(dxPx: Float, dyPx: Float)

    /** Разворачивает окно на весь экран / возвращает обратно. Замена
     * двойного клика по системной title bar, которой больше нет. На Android
     * пустая реализация — там своя модель полноэкранности через Activity. */
    fun toggleMaximize()
}
