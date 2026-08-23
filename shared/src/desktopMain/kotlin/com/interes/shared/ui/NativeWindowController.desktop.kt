package com.interes.shared.ui

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Frame
import java.awt.GraphicsDevice
import kotlin.math.roundToInt

actual class NativeWindowController(private val window: ComposeWindow) {
    // Проверяем ОДИН раз при создании (не на каждый вызов setOpacityPercent) —
    // window.graphicsConfiguration.device не меняется на лету в этом приложении.
    actual val handlesOpacityNatively: Boolean = runCatching {
        window.graphicsConfiguration.device.isWindowTranslucencySupported(
            GraphicsDevice.WindowTranslucency.TRANSLUCENT
        )
    }.getOrDefault(false)

    actual fun setOpacityPercent(percent: Int) {
        if (!handlesOpacityNatively) return
        // window.opacity — родной java.awt.Window.setOpacity(float), работает,
        // только когда окно undecorated (см. Main.kt) — обязательное условие
        // для значений меньше 1.0f, без него AWT кидает
        // IllegalComponentStateException (задокументировано в Frame/Window
        // javadoc). runCatching — на случай если конкретное железо/драйвер
        // всё-таки откажет уже во время работы, а не только на этапе
        // проверки поддержки выше.
        runCatching { window.opacity = (percent / 100f).coerceIn(0f, 1f) }
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