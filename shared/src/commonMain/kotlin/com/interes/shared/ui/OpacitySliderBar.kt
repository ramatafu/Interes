package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Горизонтальная полоска "ползунок + процент" в тёмной скруглённой
 * "таблетке". Вынесена отдельным composable, потому что используется
 * ПО-РАЗНОМУ на двух платформах:
 *
 * - на Android — рисуется ВНУТРИ отдельного системного окна (Dialog), см.
 *   NativeWindowController.android.kt: главное окно Activity целиком гаснет
 *   при уменьшении прозрачности (см. setOpacityPercent в
 *   NativeWindowController.kt), а сам ползунок обязан оставаться полностью
 *   непрозрачным и рабочим независимо от выставленного процента — поэтому
 *   он живёт в отдельном окне с собственной (всегда полной) альфой;
 * - на Desktop этот composable сейчас не используется — там прозрачность
 *   не системная (handlesOpacityNatively == false), ползунок остаётся, как
 *   и был, вертикальным в RightToolbar.kt, вне затухающего слоя.
 *
 * Собственная полупрозрачность фона "таблетки" (Color.Black.copy(alpha =
 * 0.45f) ниже) — чисто декоративная и НЕ связана с процентом прозрачности
 * приложения: она такая всегда, независимо от значения percent.
 */
@Composable
fun OpacitySliderBar(
    percent: Float,
    onPercentChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = percent,
            onValueChange = onPercentChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${percent.toInt()}%",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}
