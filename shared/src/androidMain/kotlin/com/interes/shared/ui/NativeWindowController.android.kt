package com.interes.shared.ui

import android.app.Dialog
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * На Android, в отличие от Desktop (см. NativeWindowController.desktop.kt),
 * окном управляет сама ОС — нет своего Frame с рамкой, которую можно было бы
 * двигать/сворачивать/разворачивать вручную:
 *
 * - handlesOpacityNatively = true — прозрачность применяется не Compose-
 *   слоем поверх непрозрачного окна, а по-настоящему меняет alpha самого
 *   системного окна Activity (WindowManager.LayoutParams.alpha).
 *   Translucent-тема Activity (androidApp/.../themes.xml,
 *   android:windowIsTranslucent = true + прозрачный windowBackground)
 *   делает эту системную альфу видимой сквозь ВСЁ окно целиком — вплоть до
 *   рабочего стола Android.
 *
 * - ИМЕННО ПОЭТОМУ ползунок прозрачности (showOpacitySlider/
 *   hideOpacitySlider ниже) не может рисоваться внутри обычного
 *   Compose-дерева Activity — он бы гас вместе со всем остальным. Он
 *   рисуется в ОТДЕЛЬНОМ системном окне (android.app.Dialog), у которого
 *   своя, всегда полная альфа — не связанная с alpha окна Activity.
 *
 * - showWindowControls = false — своих кнопок "Свернуть/Развернуть/Закрыть"
 *   на Android не рисуем: для этого есть системная кнопка "Домой"/жест.
 */
actual class NativeWindowController(private val activity: ComponentActivity) {
    actual val handlesOpacityNatively: Boolean = true

    actual fun setOpacityPercent(percent: Int) {
        val window = activity.window
        window.attributes = window.attributes.apply {
            alpha = percent.coerceIn(0, 100) / 100f
        }
    }

    actual val showWindowControls: Boolean = false

    // Домой/+/Резервная копия/О программе переезжают в верхний тулбар — см.
    // doc-комментарий свойства в NativeWindowController.kt и AppRoot.kt.
    actual val primaryActionsInTopBar: Boolean = true

    // Ровно 2 доски в ряд на главном экране — см. doc-комментарий свойства
    // в NativeWindowController.kt.
    actual val fixedBoardGridColumns: Int? = 2

    // 48.dp — минимум, при котором ещё нормально помещается компактная
    // 40.dp-кнопка (TopBarGlyph) с небольшим полем по бокам. Было 56.dp;
    // уменьшено ещё раз по просьбе "комнаты ещё шире" — см. doc в
    // NativeWindowController.kt.
    actual val sideToolbarWidth: Dp = 48.dp

    actual fun getWindowPosition(): Pair<Int, Int> = 0 to 0
    actual fun setWindowPosition(x: Int, y: Int) {}
    actual fun moveWindowBy(dxPx: Float, dyPx: Float) {}
    actual fun toggleMaximize() {}
    actual fun minimize() {}

    // --- Ползунок прозрачности в отдельном системном окне (см. doc выше) ---

    private var sliderDialog: Dialog? = null
    private val sliderPercent = mutableFloatStateOf(100f)
    private var onSliderChange: ((Int) -> Unit)? = null

    actual fun showOpacitySlider(percent: Int, onChange: (Int) -> Unit) {
        sliderPercent.floatValue = percent.toFloat()
        onSliderChange = onChange
        if (sliderDialog == null) {
            val dialog = buildSliderDialog()
            sliderDialog = dialog
            // Activity могла уже начать закрываться между рекомпозициями —
            // show() на неприкреплённом/уничтоженном окне бросает
            // BadTokenException, а не молча игнорирует вызов.
            runCatching { dialog.show() }
        }
    }

    actual fun hideOpacitySlider() {
        runCatching { sliderDialog?.dismiss() }
        sliderDialog = null
    }

    private fun buildSliderDialog(): Dialog {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val composeView = ComposeView(activity).apply {
            // ComposeView в Dialog не наследует ViewTree-владельцев Activity
            // автоматически (это ДРУГОЕ окно со своей View-иерархией) — без
            // этих трёх строк он падает с "ViewTreeLifecycleOwner not found"
            // при попытке отрисоваться.
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)

            setContent {
                val percent by sliderPercent
                OpacitySliderBar(
                    percent = percent,
                    onPercentChange = { newValue ->
                        sliderPercent.floatValue = newValue
                        onSliderChange?.invoke(newValue.toInt())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                )
            }
        }
        dialog.setContentView(composeView)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0f)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            // FLAG_NOT_FOCUSABLE: это окно не должно перехватывать системную
            // кнопку "Назад" у главного окна Activity (иначе PlatformBack
            // Handler.android.kt перестал бы работать, пока ползунок виден).
            // FLAG_NOT_TOUCH_MODAL: тап мимо ползунка проходит насквозь к
            // Activity под ним, а не проглатывается этим окном.
            // alpha этого окна НЕ трогаем — остаётся 1f, независимо от
            // setOpacityPercent() главного окна выше.
            addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
        }
        return dialog
    }
}
