package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.interes.shared.model.Photo
import com.interes.shared.util.localFilePathToUri
import kotlin.math.hypot
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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

private suspend fun PointerInputScope.detectZoomAndPanWhenActive(
    isAlreadyZoomed: () -> Boolean,
    onGesture: (pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var prev = mapOf(down.id to down.position)

        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break

            val zoomed = isAlreadyZoomed()
            if (pressed.size >= 2 || zoomed) {
                event.changes.forEach { if (it.pressed) it.consume() }

                if (pressed.size >= 2) {
                    val a = pressed[0]
                    val b = pressed[1]
                    val pa = prev[a.id]
                    val pb = prev[b.id]
                    if (pa != null && pb != null) {
                        val prevDist = hypot(pa.x - pb.x, pa.y - pb.y)
                        val curDist = hypot(a.position.x - b.position.x, a.position.y - b.position.y)
                        val zoom = if (prevDist > 1f) curDist / prevDist else 1f
                        val pan = Offset(
                            ((a.position.x - pa.x) + (b.position.x - pb.x)) / 2f,
                            ((a.position.y - pa.y) + (b.position.y - pb.y)) / 2f
                        )
                        onGesture(pan, zoom)
                    }
                } else {
                    val p = pressed[0]
                    val pp = prev[p.id]
                    if (pp != null) onGesture(p.position - pp, 1f)
                }
            }

            prev = pressed.associate { it.id to it.position }
        }
    }
}