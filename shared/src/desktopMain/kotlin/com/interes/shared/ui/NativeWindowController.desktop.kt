package com.interes.shared.ui

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
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
        if (window.extendedState == Frame.MAXIMIZED_BOTH) {
            window.extendedState = Frame.NORMAL
        } else {
            // У undecorated-окна (см. Main.kt) MAXIMIZED_BOTH без явного
            // maximizedBounds разворачивает окно на весь экран целиком,
            // включая область под панелью задач Windows — Java/AWT
            // применяет отступ под таскбар автоматически только у окон с
            // системной рамкой. Считаем "полезную" область экрана (той
            // монитор, где сейчас окно) сами через getScreenInsets и
            // разворачиваем только в её границах.
            val gc = window.graphicsConfiguration
            val screenBounds = gc.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
            window.maximizedBounds = Rectangle(
                screenBounds.x + insets.left,
                screenBounds.y + insets.top,
                screenBounds.width - insets.left - insets.right,
                screenBounds.height - insets.top - insets.bottom
            )
            window.extendedState = Frame.MAXIMIZED_BOTH
        }
    }

    actual fun minimize() {
        window.extendedState = Frame.ICONIFIED
    }
}