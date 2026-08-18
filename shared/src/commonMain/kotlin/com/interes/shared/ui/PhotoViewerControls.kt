package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
 * Кнопка закрытия, счётчик "3 / 12" и ползунок прозрачности (0–100%).
 * Рисуется ОТДЕЛЬНО от затухающего слоя всего приложения (см. AppRoot.kt) —
 * эти элементы должны оставаться полностью видимыми при ЛЮБОМ значении
 * ползунка, иначе при значении около 0% нечем было бы вернуть прозрачность
 * обратно или закрыть просмотрщик.
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

        MaterialTheme {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${opacityPercent.toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.width(34.dp)
                )
                Slider(
                    value = opacityPercent,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White
                    ),
                    modifier = Modifier.width(200.dp)
                )
            }
        }
    }
}
