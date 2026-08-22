package com.interes.shared.ui

import androidx.compose.ui.awt.ComposeWindow
import java.awt.GraphicsDevice
import kotlin.math.roundToInt

actual class NativeWindowController(private val window: ComposeWindow) {
    // Проверяем ОДИН раз при создании (не на каждый вызов setOpacityPercent) —
    // window.graphicsConfiguration.device не меняется на лету в этом приложении.
    actual val handlesOpacityNatively: Boolean = run {
        val result = runCatching {
            window.graphicsConfiguration.device.isWindowTranslucencySupported(
                GraphicsDevice.WindowTranslucency.TRANSLUCENT
            )
        }
        println("[Interes] handlesOpacityNatively check: $result")
        result.exceptionOrNull()?.printStackTrace()
        result.getOrDefault(false)
    }

    actual fun setOpacityPercent(percent: Int) {
        println("[Interes] setOpacityPercent($percent), handlesOpacityNatively=$handlesOpacityNatively, window.isUndecorated=${window.isUndecorated}, window.isDisplayable=${window.isDisplayable}")
        if (!handlesOpacityNatively) return
        // window.opacity — родной java.awt.Window.setOpacity(float), теперь
        // работает: окно undecorated (см. Main.kt) — это ОБЯЗАТЕЛЬНОЕ условие
        // для значений меньше 1.0f, без него AWT кидает
        // IllegalComponentStateException (задокументировано в Frame/Window
        // javadoc). runCatching — на случай если конкретное железо/драйвер
        // всё-таки откажет уже во время работы, а не только на этапе
        // проверки поддержки выше. e.printStackTrace() — чтобы не повторить
        // ту же ошибку, что была с добавлением фото: молча проглоченное
        // исключение выглядит точно так же, как "ничего не произошло".
        runCatching {
            window.opacity = (percent / 100f).coerceIn(0f, 1f)
            println("[Interes] window.opacity установлен, фактическое значение теперь: ${window.opacity}")
        }.onFailure { it.printStackTrace() }
    }

    actual fun moveWindowBy(dxPx: Float, dyPx: Float) {
        val loc = window.location
        window.setLocation(
            (loc.x + dxPx).roundToInt(),
            (loc.y + dyPx).roundToInt()
        )
    }

    actual fun toggleMaximize() {
        window.extendedState = if (window.extendedState == java.awt.Frame.MAXIMIZED_BOTH) {
            java.awt.Frame.NORMAL
        } else {
            java.awt.Frame.MAXIMIZED_BOTH
        }
    }
}
