package com.interes.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.interes.shared.model.Photo
import com.interes.shared.repository.BoardRepository

/**
 * Точка входа UI. Простая навигация без внешней библиотеки — вся Interes
 * умещается в три "экрана": список досок -> доска -> полноэкранный
 * просмотр, поэтому обычного mutableState для текущего экрана достаточно.
 *
 * Прозрачность (ползунок в просмотрщике фото) применяется здесь, на самом
 * верхнем уровне, ко ВСЕМУ содержимому приложения разом — списку досок/
 * доске И самому просмотрщику вместе, — а не только к просмотрщику поверх
 * доски. Это принципиально: чтобы прозрачность реально доходила до уровня
 * рабочего стола ОС (а не просто открывала доску/чёрный фон позади), нужно,
 * чтобы AБСОЛЮТНО ничего непрозрачного не оставалось между "пустым" окном
 * и тем, что физически находится позади него. Плюс само окно на уровне ОС
 * должно поддерживать реальную прозрачность:
 * - Android: тема Activity объявлена translucent
 *   (androidApp/src/main/res/values/themes.xml).
 * - Windows: главное окно создаётся как undecorated+transparent
 *   (desktopApp/.../Main.kt).
 * Без этих платформенных настроек Compose-часть (alpha ниже) сама по себе
 * ничего не даст — система всё равно не будет смешивать пиксели с тем, что
 * позади окна.
 */
@Composable
fun InteresRoot(repository: BoardRepository) {
    // remember(repository) — ВАЖНО, а не просто repository.observeX().collectAsState()
    // напрямую. repository.observeBoardSummaries()/observeBoards() строят
    // НОВУЮ цепочку Flow при каждом вызове. Без remember каждая перерисовка
    // InteresRoot (а она перерисовывается именно при получении новых данных
    // из этих же Flow — читает boardSummaries/allBoards) создавала бы
    // Flow-объект заново; collectAsState видит "другой" Flow как смену ключа
    // и перезапускает сбор с нуля (сброс на initial = emptyList()) — из-за
    // этого перерисовка догоняла сама себя быстрее, чем БД успевала вернуть
    // актуальные данные, и список визуально никогда не "устаканивался" на
    // новом состоянии. Ровно это и выглядело как "фото не добавляется":
    // на самом деле фото исправно попадало в БД, но экран крутился в этом
    // цикле сброс-пересбор и не показывал итог.
    val boardSummariesFlow = remember(repository) { repository.observeBoardSummaries() }
    val allBoardsFlow = remember(repository) { repository.observeBoards() }
    val boardSummaries by boardSummariesFlow.collectAsState(initial = emptyList())
    val allBoards by allBoardsFlow.collectAsState(initial = emptyList())

    // null = показан список досок; иначе id открытой доски.
    var selectedBoardId by remember { mutableStateOf<Long?>(null) }

    // Открытая полноэкранная галерея: список фото доски + индекс, с которого
    // начать. null — галерея закрыта.
    var viewerState by remember { mutableStateOf<Pair<List<Photo>, Int>?>(null) }

    // Прозрачность ВСЕГО приложения (0–100%), управляется ползунком в
    // PhotoViewerControls. Сбрасывается на 100%, когда закрывается
    // просмотрщик (иначе список/доска рисковали бы остаться прозрачными)
    // и при пролистывании на другое фото (та же логика, что и у зума —
    // каждое фото начинается "с чистого листа").
    var appOpacityPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(viewerState == null) {
        if (viewerState == null) appOpacityPercent = 100f
    }

    // Порядок важен: если открыт просмотрщик — "Назад" сначала закрывает
    // именно его, а не выкидывает сразу из доски. Если просмотрщик закрыт,
    // но открыта доска — "Назад" возвращает к списку досок.
    PlatformBackHandler(enabled = viewerState != null) { viewerState = null }
    PlatformBackHandler(enabled = viewerState == null && selectedBoardId != null) { selectedBoardId = null }

    Box(modifier = Modifier.fillMaxSize()) {
        val currentViewerState = viewerState
        val pagerState = if (currentViewerState != null) {
            rememberPagerState(initialPage = currentViewerState.second) { currentViewerState.first.size }
        } else null

        if (pagerState != null) {
            LaunchedEffect(pagerState.currentPage) { appOpacityPercent = 100f }
        }

        // Единый затухающий слой: список досок/доска И сама галерея (без
        // элементов управления) вместе, одним alpha на графическом слое.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = appOpacityPercent / 100f }
        ) {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val boardId = selectedBoardId
                    if (boardId == null) {
                        BoardsListScreen(
                            boards = boardSummaries,
                            repository = repository,
                            onOpenBoard = { id -> selectedBoardId = id }
                        )
                    } else {
                        // Доску могли удалить (в т.ч. её же длинным нажатием
                        // со списка) или переименовать, пока она открыта —
                        // актуальное название всегда берём из уже
                        // загруженного списка досок.
                        val currentTitle = allBoards.firstOrNull { it.id == boardId }?.title
                        // Доски больше нет (удалена) — возвращаемся к списку.
                        // LaunchedEffect, а не прямая запись в теле composable —
                        // запись состояния во время композиции нежелательна,
                        // даже когда (как здесь) практически не вызывает
                        // видимых проблем.
                        LaunchedEffect(currentTitle) {
                            if (currentTitle == null) selectedBoardId = null
                        }
                        if (currentTitle != null) {
                            BoardScreen(
                                boardId = boardId,
                                boardTitle = currentTitle,
                                repository = repository,
                                onBack = { selectedBoardId = null },
                                onPhotoClick = { photos, index -> viewerState = photos to index }
                            )
                        }
                    }
                }
            }

            if (currentViewerState != null && pagerState != null) {
                PhotoViewerContent(photos = currentViewerState.first, pagerState = pagerState)
            }
        }

        // Элементы управления просмотрщика — СНАРУЖИ затухающего слоя,
        // всегда полностью непрозрачны.
        if (currentViewerState != null && pagerState != null) {
            PhotoViewerControls(
                pageCount = currentViewerState.first.size,
                currentPage = pagerState.currentPage,
                opacityPercent = appOpacityPercent,
                onOpacityChange = { appOpacityPercent = it },
                onDismiss = { viewerState = null }
            )
        }
    }
}
