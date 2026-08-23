package com.interes.shared.ui

actual class NativeWindowController {
    // На Android настоящая прозрачность до системного фона уже достигается
    // ИНАЧЕ — через translucent-тему Activity (androidApp/.../themes.xml) +
    // обычный Compose graphicsLayer.alpha (см. AppRoot.kt) — отдельного
    // системного API окна для этого не нужно, класс-заглушка.
    actual val handlesOpacityNatively: Boolean = false
    actual fun setOpacityPercent(percent: Int) {}
    actual fun moveWindowBy(dxPx: Float, dyPx: Float) {}
    actual fun toggleMaximize() {}
    actual fun minimize() {}
}
