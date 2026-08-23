package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhotoViewerControls(
    pageCount: Int,
    currentPage: Int,
    opacityPercent: Float,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Ползунок прозрачности — ВНИЗУ фотографии ПО ЦЕНТРУ.
        // Нарисован СНАРУЖИ затухающего слоя (AppRoot.kt), поэтому
        // НИКОГДА не становится прозрачным при перемещении.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${opacityPercent.toInt()}%", color = Color.White, fontSize = 12.sp)
                Text(
                    "${currentPage + 1} / $pageCount",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Slider(
                value = opacityPercent,
                onValueChange = onOpacityChange,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White
                ),
                modifier = Modifier
                    .width(320.dp)
                    .padding(top = 4.dp)
            )
        }

        // Кнопка "Закрыть" — СПРАВА от фотографии, на той же высоте, что и ползунок.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 20.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2715", color = Color.White, fontSize = 20.sp)
        }

        // Стрелка "предыдущее" — СЛЕВА от фотографии, по центру высоты.
        if (currentPage > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(12.dp)
                    .size(56.dp)
                    .clickable(onClick = onPreviousPage),
                contentAlignment = Alignment.Center
            ) {
                Text("\u25C0", color = Color.White.copy(alpha = 0.6f), fontSize = 40.sp)
            }
        }

        // Стрелка "следующее" — СПРАВА от фотографии, по центру высоты.
        if (currentPage < pageCount - 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(12.dp)
                    .size(56.dp)
                    .clickable(onClick = onNextPage),
                contentAlignment = Alignment.Center
            ) {
                Text("\u25B6", color = Color.White.copy(alpha = 0.6f), fontSize = 40.sp)
            }
        }
    }
}