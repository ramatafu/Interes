package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.rememberWindowState
import coil3.compose.AsyncImage
import com.interes.shared.model.Photo
import java.io.File

private val CONTROL_BAR_HEIGHT = 40.dp
private val IMAGE_HEIGHT = 320.dp
private val MAX_WINDOW_WIDTH = 800.dp

/**
 * Окно для отображения фотографии поверх других приложений.
 *
 * Особенности:
 * - окно без стандартной рамки;
 * - прозрачный фон окна;
 * - окно всегда поверх других окон;
 * - фотографию можно перетаскивать за любую область изображения;
 * - кнопка закрытия не входит в область перетаскивания;
 * - снизу находится панель управления прозрачностью;
 * - панель управления всегда остаётся видимой;
 * - прозрачность применяется только к фотографии;
 * - локальные изображения загружаются через File.toURI().
 */
@Composable
fun ApplicationScope.TransparentPhotoWindow(
    photo: Photo,
    onClose: () -> Unit
) {
    val aspectRatio =
        if (photo.height > 0) {
            photo.width.toFloat() / photo.height.toFloat()
        } else {
            1f
        }

    val calculatedWidth = IMAGE_HEIGHT * aspectRatio
    val windowWidth = calculatedWidth.coerceAtMost(MAX_WINDOW_WIDTH)

    var opacityPercent by remember {
        mutableFloatStateOf(100f)
    }

    Window(
        onCloseRequest = onClose,
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        state = rememberWindowState(
            size = DpSize(
                width = windowWidth,
                height = IMAGE_HEIGHT + CONTROL_BAR_HEIGHT
            )
        ),
        title = "Interes"
    ) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    /*
                     * Область фотографии.
                     *
                     * Только изображение находится внутри
                     * WindowDraggableArea. Поэтому нажатие на кнопку
                     * закрытия не пытается одновременно перемещать окно.
                     */
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IMAGE_HEIGHT)
                    ) {

                        WindowDraggableArea {
                            AsyncImage(
                                model = File(photo.filePath).toURI(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp
                                        )
                                    )
                                    .graphicsLayer {
                                        alpha = opacityPercent / 100f
                                    }
                            )
                        }

                        /*
                         * Кнопка закрытия.
                         *
                         * Она находится вне WindowDraggableArea,
                         * поэтому её можно нормально нажать.
                         */
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    Color.Black.copy(alpha = 0.55f)
                                )
                                .clickable {
                                    onClose()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "×",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    /*
                     * Панель управления прозрачностью.
                     *
                     * Она намеренно НЕ находится внутри
                     * WindowDraggableArea, поэтому перемещение
                     * ползунка не двигает окно.
                     */
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CONTROL_BAR_HEIGHT)
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                            .background(
                                Color.Black.copy(alpha = 0.55f)
                            )
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "${opacityPercent.toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.width(34.dp)
                        )

                        Slider(
                            value = opacityPercent,
                            onValueChange = {
                                opacityPercent = it
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(
                                    alpha = 0.35f
                                )
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}