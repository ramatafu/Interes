package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.interes.shared.model.Photo
import com.interes.shared.util.localFilePathToUri
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Сам просмотрщик — свайп между фото + pinch-to-zoom/пан + двойной тап.
 * НЕ содержит элементов управления (кнопка закрытия/счётчик/ползунок
 * прозрачности) — они рисуются отдельным composable (PhotoViewerControls)
 * поверх этого, вне затухающего слоя всего приложения. Такое разделение
 * специально: прозрачность должна применяться не только к самому
 * просмотрщику, а ко ВСЕМУ содержимому приложения разом (см. AppRoot.kt),
 * а элементы управления должны оставаться видимыми при любом значении
 * прозрачности — иначе нечем было бы вернуть её обратно.
 */
@Composable
fun PhotoViewerContent(
    photos: List<Photo>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    var currentScale by remember { mutableStateOf(1f) }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize().background(Color.Black),
        userScrollEnabled = currentScale <= 1.01f
    ) { page ->
        ZoomableImage(
            photo = photos[page],
            onScaleChanged = { scale ->
                if (pagerState.currentPage == page) currentScale = scale
            }
        )
    }
}

@Composable
private fun ZoomableImage(
    photo: Photo,
    onScaleChanged: (Float) -> Unit
) {
    var scale by remember(photo.id) { mutableStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale) { onScaleChanged(scale) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Тап (двойной) и zoom/pan объединены в ОДИН pointerInput с двумя
            // сопрограммами — два отдельных pointerInput-модификатора на
            // одном узле конкурируют за один поток касаний.
            .pointerInput(photo.id) {
                coroutineScope {
                    launch {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    launch {
                        // ВАЖНО: обычный detectTransformGestures потребляет
                        // ЛЮБОЕ перемещение пальца как pan — включая простой
                        // одиночный свайп влево/вправо, даже без увеличения.
                        // Здесь жест потребляется ТОЛЬКО когда это реально
                        // масштабирование (2+ пальца) или фото уже увеличено
                        // (scale > 1) — иначе одиночный свайп проходит мимо,
                        // к родительскому HorizontalPager.
                        detectZoomAndPanWhenActive(
                            isAlreadyZoomed = { scale > 1f }
                        ) { pan, zoom ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale <= 1f) Offset.Zero else offset + pan
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = localFilePathToUri(photo.filePath),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

/**
 * Как detectTransformGestures, но НЕ потребляет жест, если это одиночный
 * палец без масштабирования и фото ещё не увеличено — тогда событие
 * остаётся непотреблённым и достаётся родительскому HorizontalPager,
 * который интерпретирует его как обычный свайп между страницами.
 */
private suspend fun PointerInputScope.detectZoomAndPanWhenActive(
    isAlreadyZoomed: () -> Boolean,
    onGesture: (pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        var pastTouchSlop = false
        var accumulatedZoom = 1f
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val anyConsumed = event.changes.any { it.isConsumed }
            if (!anyConsumed) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val pointerCount = event.changes.size

                if (!pastTouchSlop) {
                    accumulatedZoom *= zoomChange
                    if (accumulatedZoom != 1f || panChange.getDistance() > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val shouldHandleHere = pointerCount > 1 || isAlreadyZoomed()
                    if (shouldHandleHere) {
                        if (zoomChange != 1f || panChange != Offset.Zero) {
                            onGesture(panChange, zoomChange)
                        }
                        event.changes.forEach { change ->
                            if (change.positionChanged()) change.consume()
                        }
                    }
                    // Иначе (одиночный палец, не увеличено) — намеренно
                    // ничего не consume-им, жест уходит родителю.
                }
            }
        } while (!anyConsumed && event.changes.any { it.pressed })
    }
}
