package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val RightToolbarWidth: Dp = 72.dp

@Composable
fun RightToolbar(
    modifier: Modifier = Modifier,
    onNextPhoto: (() -> Unit)? = null,
    opacityPercent: Float? = null,
    onOpacityChange: ((Float) -> Unit)? = null
) {
    var showOpacityDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(RightToolbarWidth)
            .fillMaxHeight()
            .background(SideToolbarColor)
    ) {
        // Стрелка "следующее фото" — ТОЧНО по центру высоты окна,
        // на том же уровне, что и стрелка ◀ на левом тулбаре.
        if (onNextPhoto != null) {
            IconButton(
                onClick = onNextPhoto,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
            ) {
                ChevronRightGlyph()
            }
        }

        // Ползунок прозрачности + кнопка ручного ввода процента над ним.
        if (opacityPercent != null && onOpacityChange != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Кнопка с текущим процентом — ЧУТЬ ВЫШЕ ползунка.
                // Клик открывает диалог, где можно вручную ввести число 0–100.
                Box(
                    modifier = Modifier
                        .size(56.dp, 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { showOpacityDialog = true }
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${opacityPercent.toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                VerticalSlider(
                    value = opacityPercent,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .height(180.dp)
                        .padding(vertical = 4.dp)
                )
            }

            // Диалог ручного ввода процента прозрачности.
            if (showOpacityDialog) {
                var input by remember { mutableStateOf(opacityPercent.toInt().toString()) }
                AlertDialog(
                    onDismissRequest = { showOpacityDialog = false },
                    title = { Text("Прозрачность") },
                    text = {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            label = { Text("Процент (0–100)") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            // Применяем только если введено число;
                            // значение автоматически зажимается в 0–100.
                            input.toIntOrNull()?.let { v ->
                                onOpacityChange(v.toFloat().coerceIn(0f, 100f))
                            }
                            showOpacityDialog = false
                        }) { Text("Применить") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOpacityDialog = false }) { Text("Отмена") }
                    }
                )
            }
        }
    }
}

/**
 * Обычный Material3 Slider, повёрнутый на 90°. layout{} меняет местами
 * измеренные ширину/высоту ДО поворота — иначе повёрнутый слайдер занимал
 * бы в разметке место как горизонтальный.
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
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
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