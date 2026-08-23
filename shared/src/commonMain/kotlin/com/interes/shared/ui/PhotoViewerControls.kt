package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Ширина полосы слева, зарезервированной под вертикальный ползунок —
// используется и здесь (для размещения самого ползунка), и в AppRoot.kt
// (там на столько же нужно сдвинуть вправо саму фотографию, чтобы
// ползунок не наезжал на неё, а стоял РЯДОМ, как и просили).
val PhotoViewerSliderAreaWidth: Dp = 64.dp

/**
 * Кнопка закрытия, счётчик "3 / 12" и ползунок прозрачности (0–100%).
 * Рисуется ОТДЕЛЬНО от затухающего слоя всего приложения (см. AppRoot.kt) —
 * эти элементы должны оставаться полностью видимыми при ЛЮБОМ значении
 * ползунка, иначе при значении около 0% нечем было бы вернуть прозрачность
 * обратно или закрыть просмотрщик.
 *
 * Ползунок — СЛЕВА от фотографии (по просьбе), не поверх неё: сама
 * фотография в AppRoot.kt сдвинута вправо на PhotoViewerSliderAreaWidth
 * паддингом, освобождая эту полосу. Вертикальная ориентация — Material3
 * Slider горизонтальный по умолчанию, разворот через graphicsLayer +
 * layout{} (стандартный для Compose приём получить вертикальный слайдер;
 * без сторонних библиотек).
 */
@Composable
fun PhotoViewerControls(
    pageCount: Int,
    currentPage: Int,
    opacityPercent: Float,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2715", color = Color.White, fontSize = 20.sp)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("${currentPage + 1} / $pageCount", color = Color.White, fontSize = 14.sp)
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(PhotoViewerSliderAreaWidth)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${opacityPercent.toInt()}%", color = Color.White, fontSize = 12.sp)
            VerticalSlider(
                value = opacityPercent,
                onValueChange = onOpacityChange,
                valueRange = 0f..100f,
                modifier = Modifier
                    .height(220.dp)
                    .padding(top = 4.dp)
            )
        }
    }
}

/**
 * Обычный Material3 Slider, повёрнутый на 90°. layout{} меняет местами
 * измеренные ширину/высоту ДО поворота — иначе повёрнутый (визуально
 * вертикальный) слайдер занимал бы в разметке место как горизонтальный
 * (широкий и низкий), обрезаясь по факту.
 */
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White
        ),
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(x = -placeable.width, y = 0)
                }
            }
    )
}
