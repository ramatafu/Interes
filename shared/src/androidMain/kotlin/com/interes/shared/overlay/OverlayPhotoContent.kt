package com.interes.shared.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private val PHOTO_SIZE = 220.dp

/**
 * ВАЖНО: у корневого Column и у самой картинки нет непрозрачного фона.
 * Любой сплошной фон на корневом уровне — и "прозрачное окно" превращается
 * в обычное окно с рамкой того цвета. Прозрачность окна
 * (PixelFormat.TRANSLUCENT в LayoutParams) даёт только возможность не
 * закрашивать пиксели — фактически прозрачным их делает отсутствие
 * непрозрачного контента поверх.
 *
 * Ползунок и подложка под ним НАМЕРЕННО остаются непрозрачными всегда —
 * иначе при прозрачности фото около 0% сам ползунок и кнопка закрытия
 * стали бы невидимыми и невозможно было бы вернуть окно обратно.
 */
@Composable
internal fun OverlayPhotoContent(
    photoPath: String,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClose: () -> Unit
) {
    var opacityPercent by remember { mutableFloatStateOf(100f) }

    MaterialTheme {
        Column {
            Box {
                AsyncImage(
                    model = "file://$photoPath",
                    contentDescription = null,
                    modifier = Modifier
                        .size(PHOTO_SIZE)
                        .clip(RoundedCornerShape(16.dp))
                        .graphicsLayer { alpha = opacityPercent / 100f }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2715", color = Color.White, fontSize = 14.sp)
                }
            }

            // Подложка под ползунком — своя, непрозрачная, независимая от
            // opacityPercent, чтобы им всегда можно было пользоваться.
            Row(
                modifier = Modifier
                    .width(PHOTO_SIZE)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${opacityPercent.toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.width(30.dp)
                )
                Slider(
                    value = opacityPercent,
                    onValueChange = { opacityPercent = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
