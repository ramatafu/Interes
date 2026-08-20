package com.interes.shared.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.interes.shared.model.Photo
import com.interes.shared.util.localFilePathToUri
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Сетка фото доски в стиле Pinterest (masonry, 2 колонки) с перетаскиванием
 * для смены порядка.
 *
 * Как работает reorder: у каждой карточки через onGloballyPositioned
 * сохраняются реальные экранные границы (bounds). Это специально сделано
 * вместо "индекса по счёту", потому что в masonry-раскладке высоты ячеек
 * разные и соседний по индексу элемент не обязательно ближайший на экране.
 * Во время драга по текущей позиции пальца/курсора ищем, в чьи bounds она
 * попадает — это и есть цель обмена.
 */
@Composable
fun PhotoBoardGrid(
    photos: List<Photo>,
    modifier: Modifier = Modifier,
    onReorder: (orderedIds: List<Long>) -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onLongPress: (Photo) -> Unit
) {
    // Локальная копия — даёт мгновенный визуальный отклик при драге, не
    // дожидаясь, пока запись в БД вернётся новым эмитом Flow из репозитория.
    var order by remember { mutableStateOf(photos) }
    var draggedId by remember { mutableStateOf<Long?>(null) }

    // Пока идёт драг, не даём внешнему списку "переписать" order под ногами.
    LaunchedEffect(photos) {
        if (draggedId == null) order = photos
    }

    val bounds = remember { mutableStateMapOf<Long, Rect>() }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // ВАЖНО (см. комментарий у onGloballyPositioned ниже): компенсация
    // смещения при перестановке не может считаться синхронно сразу после
    // мутации order — LazyStaggeredGrid перекладывает элементы асинхронно,
    // на следующий кадр. Если считать компенсацию сразу, bounds[photo.id]
    // ещё старые, и перетаскиваемая карточка "перебегает" мимо пальца/
    // курсора на кадр позже, когда реальная позиция наконец обновится.
    // Вместо этого запоминаем точку "откуда" и досчитываем компенсацию
    // только когда onGloballyPositioned реально сообщит новую позицию.
    var pendingCompensationAnchor by remember { mutableStateOf<Offset?>(null) }

    val context = LocalPlatformContext.current

    LazyVerticalStaggeredGrid(
        // Adaptive вместо Fixed(2) — та же причина, что и в BoardsListScreen:
        // на широком окне Windows должно быть больше колонок, а не 2
        // растянутые на весь экран.
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // key = { it.id } — стабильная идентичность карточки для Compose
        // при реордере/анимации; сам индекс в списке больше не нужен нигде
        // в теле (тап теперь передаёт сам объект Photo, не позицию — см.
        // комментарий у параметра onPhotoClick).
        items(order, key = { it.id }) { photo ->
            val isDragged = photo.id == draggedId

            Box(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        val newBounds = coords.boundsInRoot()
                        bounds[photo.id] = newBounds
                        if (photo.id == draggedId) {
                            val anchor = pendingCompensationAnchor
                            if (anchor != null) {
                                dragOffset += (anchor - newBounds.center)
                                pendingCompensationAnchor = null
                            }
                        }
                    }
                    // Плавный доезд НЕ перетаскиваемых карточек до новой
                    // позиции при реордере — без этого они мгновенно
                    // "телепортировались" на новое место, что и ощущалось
                    // как дёрганность.
                    .let { if (!isDragged) it.animateItem() else it }
                    .graphicsLayer {
                        if (isDragged) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            shadowElevation = 16f
                            scaleX = 1.03f
                            scaleY = 1.03f
                        }
                    }
                    .zIndex(if (isDragged) 1f else 0f)
                    .clip(RoundedCornerShape(12.dp))
                    // ВАЖНО: тап и долгий драг раньше жили в двух РАЗНЫХ
                    // узлах — Modifier.clickable{} отдельно и
                    // Modifier.pointerInput{detectDragGesturesAfterLongPress}
                    // отдельно. Оба независимо читают один и тот же поток
                    // касаний и периодически "перехватывают" события друг у
                    // друга — из-за этого драг-детектор постоянно сбрасывался
                    // и стартовал заново, что и выглядело как дрожание при
                    // удержании. Одна pointerInput-нода с двумя сопрограммами
                    // внутри — корректный способ Compose обрабатывать
                    // несколько жестов на одном элементе без конфликта.
                    .pointerInput(photo.id) {
                        coroutineScope {
                            launch {
                                detectTapGestures(onTap = { onPhotoClick(photo) })
                            }
                            launch {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedId = photo.id
                                        dragOffset = Offset.Zero
                                        pendingCompensationAnchor = null
                                    },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        dragOffset += delta

                                        val draggedBounds = bounds[photo.id] ?: return@detectDragGesturesAfterLongPress
                                        val pointerNow = draggedBounds.center + dragOffset

                                        val targetId = bounds.entries.firstOrNull { (id, rect) ->
                                            id != photo.id && rect.contains(pointerNow)
                                        }?.key

                                        if (targetId != null) {
                                            val fromIndex = order.indexOfFirst { it.id == photo.id }
                                            val toIndex = order.indexOfFirst { it.id == targetId }
                                            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                                // Запоминаем "откуда" и ждём реального
                                                // онGloballyPositioned с новой позицией —
                                                // см. комментарий выше про асинхронность.
                                                pendingCompensationAnchor = bounds[photo.id]?.center
                                                order = order.toMutableList().apply {
                                                    add(toIndex, removeAt(fromIndex))
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        // Долгое нажатие почти без движения пальца/курсора —
                                        // это не попытка перетащить, а запрос на меню
                                        // действий с фото (удаление). Настоящий reorder
                                        // почти всегда даёт куда большее смещение, чем этот
                                        // небольшой допуск на дрожание руки/мыши.
                                        val totalMovement = kotlin.math.abs(dragOffset.x) + kotlin.math.abs(dragOffset.y)
                                        if (totalMovement < 12f) {
                                            onLongPress(photo)
                                        } else if (order.map { it.id } != photos.map { it.id }) {
                                            // Порядок реально поменялся — передаём готовый
                                            // список id целиком, а не индексы. Индексы были бы
                                            // хрупкими: пока корутина в BoardScreen дойдёт до
                                            // repository.reorderPhotos, список photos мог уже
                                            // измениться (например, параллельно доехал импорт
                                            // другого фото), и старые индексы указывали бы не
                                            // туда. Список id по значению устойчив к этому.
                                            onReorder(order.map { it.id })
                                        }
                                        draggedId = null
                                        dragOffset = Offset.Zero
                                        pendingCompensationAnchor = null
                                    },
                                    onDragCancel = {
                                        order = photos
                                        draggedId = null
                                        dragOffset = Offset.Zero
                                        pendingCompensationAnchor = null
                                    }
                                )
                            }
                        }
                    }
            ) {
                AsyncImage(
                    model = remember(photo.filePath) {
                        ImageRequest.Builder(context)
                            .data(localFilePathToUri(photo.filePath))
                            .memoryCacheKey(photo.filePath)
                            .diskCacheKey(photo.filePath)
                            // Декодируем сразу под размер миниатюры, а не в
                            // полном разрешении камеры (4000×3000 и больше) —
                            // это была главная причина тормозов при скролле:
                            // без этого ограничения Coil декодировал
                            // огромные bitmap'ы под маленькую ячейку сетки.
                            .size(Size(720, 720))
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(
                            if (photo.height > 0) photo.width.toFloat() / photo.height.toFloat() else 1f
                        )
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}
