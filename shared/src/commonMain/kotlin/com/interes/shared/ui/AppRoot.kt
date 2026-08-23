package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.interes.shared.model.Photo
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import kotlinx.coroutines.launch

@Composable
fun InteresRoot(
    repository: BoardRepository,
    backupPaths: BackupPaths,
    nativeWindowController: NativeWindowController,
    onExitApp: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val boardSummariesFlow = remember(repository) { repository.observeBoardSummaries() }
    val allBoardsFlow = remember(repository) { repository.observeBoards() }
    val trashedBoardsFlow = remember(repository) { repository.observeTrashedBoards() }
    val boardSummaries by boardSummariesFlow.collectAsState(initial = emptyList())
    val allBoards by allBoardsFlow.collectAsState(initial = emptyList())
    val trashedBoards by trashedBoardsFlow.collectAsState(initial = emptyList())

    var selectedBoardId by remember { mutableStateOf<Long?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    var showCreateBoardDialog by remember { mutableStateOf(false) }
    var viewerState by remember { mutableStateOf<Pair<List<Photo>, Int>?>(null) }
    var appOpacityPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(viewerState == null) {
        if (viewerState == null) appOpacityPercent = 100f
    }

    PlatformBackHandler(enabled = showTrash) { showTrash = false }
    PlatformBackHandler(enabled = !showTrash && viewerState != null) { viewerState = null }
    PlatformBackHandler(enabled = !showTrash && viewerState == null && selectedBoardId != null) { selectedBoardId = null }

    val goHome: () -> Unit = {
        viewerState = null
        showTrash = false
        selectedBoardId = null
    }

    InteresTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // pagerState поднят сюда: доступен и контенту, и лямбдам тулбаров.
            val currentViewerState = viewerState
            val pagerState = if (currentViewerState != null) {
                rememberPagerState(initialPage = currentViewerState.second) { currentViewerState.first.size }
            } else null

            if (pagerState != null) {
                LaunchedEffect(pagerState.currentPage) { appOpacityPercent = 100f }
            }

            // Стрелки листания: null, когда листнуть нельзя или просмотрщик
            // закрыт — тогда стрелка на тулбаре не рисуется.
            val onPrevPhoto: (() -> Unit)? = if (pagerState != null && pagerState.currentPage > 0) {
                {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage - 1).coerceAtLeast(0)
                        )
                    }
                }
            } else null

            val onNextPhoto: (() -> Unit)? = if (pagerState != null && pagerState.currentPage < pagerState.pageCount - 1) {
                {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(pagerState.pageCount - 1)
                        )
                    }
                }
            } else null

            // Контент — с тем же горизонтальным паддингом, что и раньше:
            // тулбары должны оставаться видимыми на всю свою высоту, а не
            // только ниже верхней панели, поэтому тело каждого экрана
            // по-прежнему инсетится здесь целиком (а не по кускам внутри
            // самих экранов, как было в промежуточной версии — та версия
            // ошибочно растягивала Surface на всё окно и закрывала тулбары
            // целиком, а не только сверху).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = ToolbarWidth, end = RightToolbarWidth)
            ) {
                // Затухающий слой: прозрачность гасит ТОЛЬКО контент.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = appOpacityPercent / 100f
                        }
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (showTrash) {
                            TrashScreen(
                                trashedBoards = trashedBoards,
                                repository = repository,
                                onBack = { showTrash = false },
                                nativeWindowController = nativeWindowController
                            )
                        } else {
                            val boardId = selectedBoardId
                            if (boardId == null) {
                                BoardsListScreen(
                                    boards = boardSummaries,
                                    repository = repository,
                                    onOpenBoard = { id -> selectedBoardId = id },
                                    onCreateBoard = { showCreateBoardDialog = true },
                                    nativeWindowController = nativeWindowController,
                                    onExitApp = onExitApp
                                )
                            } else {
                                val currentTitle = allBoards.firstOrNull { it.id == boardId }?.title
                                LaunchedEffect(currentTitle) {
                                    if (currentTitle == null) selectedBoardId = null
                                }
                                if (currentTitle != null) {
                                    BoardScreen(
                                        boardId = boardId,
                                        boardTitle = currentTitle,
                                        repository = repository,
                                        onBack = { selectedBoardId = null },
                                        onPhotoClick = { photos, index -> viewerState = photos to index },
                                        nativeWindowController = nativeWindowController
                                    )
                                }
                            }
                        }
                    }

                    if (currentViewerState != null && pagerState != null) {
                        PhotoViewerContent(
                            photos = currentViewerState.first,
                            pagerState = pagerState
                        )
                    }
                }

                if (currentViewerState != null && pagerState != null) {
                    PhotoViewerControls(
                        pageCount = currentViewerState.first.size,
                        currentPage = pagerState.currentPage,
                        onDismiss = { viewerState = null }
                    )
                }
            }

            // Левый тулбар со стрелкой ◀.
            SideToolbar(
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterStart),
                onHome = goHome,
                onCreateBoard = { showCreateBoardDialog = true },
                backupPaths = backupPaths,
                onOpenTrash = { showTrash = true },
                onPrevPhoto = onPrevPhoto
            )

            // Правый тулбар: стрелка ▶ + вертикальный ползунок прозрачности
            // (ползунок виден только при открытом просмотрщике).
            RightToolbar(
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                onNextPhoto = onNextPhoto,
                opacityPercent = if (pagerState != null) appOpacityPercent else null,
                onOpacityChange = if (pagerState != null) {
                    { appOpacityPercent = it }
                } else null
            )

            // Заполнители углов — рисуются ПОСЛЕДНИМИ, то есть поверх и
            // тулбаров, и всего остального. Их размер: ширина ровно как у
            // соответствующего тулбара (ToolbarWidth / RightToolbarWidth),
            // высота — как у верхней панели (TopToolbarHeight, одинаковая у
            // всех трёх экранов). Цвет — TopToolbarColor, тот же,
            // что и у самой верхней панели (см. BoardsListScreen.kt,
            // BoardScreen.kt, TrashScreen.kt).
            //
            // Так верхняя панель визуально "дотягивается" до самых краёв
            // окна и ложится поверх верхних углов боковых тулбаров, а сами
            // тулбары остаются полностью рабочими и видимыми ниже этой
            // полосы — в отличие от предыдущей попытки, где Surface с
            // контентом занимал всё окно целиком и закрывал тулбары
            // полностью, а не только сверху.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(ToolbarWidth)
                    .height(TopToolbarHeight)
                    .background(TopToolbarColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(RightToolbarWidth)
                    .height(TopToolbarHeight)
                    .background(TopToolbarColor)
            )
        }

        if (showCreateBoardDialog) {
            CreateBoardDialog(
                onDismiss = { showCreateBoardDialog = false },
                onCreate = { title, category ->
                    showCreateBoardDialog = false
                    scope.launch {
                        val id = repository.createBoard(title, category)
                        viewerState = null
                        showTrash = false
                        selectedBoardId = id
                    }
                }
            )
        }
    }
}