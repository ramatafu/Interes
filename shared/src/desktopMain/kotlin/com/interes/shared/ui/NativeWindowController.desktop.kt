package com.interes.shared.ui

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Frame
import kotlin.math.roundToInt

actual class NativeWindowController(private val window: ComposeWindow) {
    // Прозрачность теперь послойная (graphicsLayer.alpha в AppRoot.kt),
    // window.opacity не трогаем — иначе гасло бы ВСЁ окно вместе с ползунком.
    actual val handlesOpacityNatively: Boolean = false

    actual fun setOpacityPercent(percent: Int) {
        // No-op: прозрачность ведёт graphicsLayer.alpha в AppRoot.kt.
    }

    actual fun getWindowPosition(): Pair<Int, Int> {
        val loc = window.location
        return loc.x to loc.y
    }

    actual fun setWindowPosition(x: Int, y: Int) {
        window.setLocation(x, y)
    }

    actual fun moveWindowBy(dxPx: Float, dyPx: Float) {
        val loc = window.location
        window.setLocation(
            (loc.x + dxPx).roundToInt(),
            (loc.y + dyPx).roundToInt()
        )
    }

    actual fun toggleMaximize() {
        window.extendedState = if (window.extendedState == Frame.MAXIMIZED_BOTH) {
            Frame.NORMAL
        } else {
            Frame.MAXIMIZED_BOTH
        }
    }

    actual fun minimize() {
        window.extendedState = Frame.ICONIFIED
    }
}