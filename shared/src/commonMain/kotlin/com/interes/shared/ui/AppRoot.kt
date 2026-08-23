package com.interes.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

/**
 * Мостик для клавиатуры: Main.kt (desktop) ловит ← / → на уровне окна и
 * вызывает эти лямбды. Пока просмотрщик закрыт — они null, стрелки ничего
 * не делают. На Android просто никто их не дёргает — безопасно.
 */
object ViewerKeys {
    var onLeft: (() -> Unit)? = null
    var onRight: (() -> Unit)? = null
}

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

    // Просмотрщик закрыт — отключаем клавиатурные стрелки.
    LaunchedEffect(viewerState) {
        if (viewerState == null) {
            ViewerKeys.onLeft = null
            ViewerKeys.onRight = null
        }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = ToolbarWidth, end = RightToolbarWidth)
            ) {
                val currentViewerState = viewerState
                val pagerState = if (currentViewerState != null) {
                    rememberPagerState(initialPage = currentViewerState.second) { currentViewerState.first.size }
                } else null

                if (pagerState != null) {
                    LaunchedEffect(pagerState.currentPage) { appOpacityPercent = 100f }

                    // Подключаем клавиатурные стрелки к пейджеру, пока он открыт.
                    LaunchedEffect(pagerState) {
                        ViewerKeys.onLeft = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    (pagerState.currentPage - 1).coerceAtLeast(0)
                                )
                            }
                        }
                        ViewerKeys.onRight = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    (pagerState.currentPage + 1).coerceAtMost(pagerState.pageCount - 1)
                                )
                            }
                        }
                    }
                }

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
                        opacityPercent = appOpacityPercent,
                        onOpacityChange = { appOpacityPercent = it },
                        onDismiss = { viewerState = null },
                        onPreviousPage = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    (pagerState.currentPage - 1).coerceAtLeast(0)
                                )
                            }
                        },
                        onNextPage = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    (pagerState.currentPage + 1).coerceAtMost(pagerState.pageCount - 1)
                                )
                            }
                        }
                    )
                }
            }

            SideToolbar(
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterStart),
                onHome = goHome,
                onCreateBoard = { showCreateBoardDialog = true },
                backupPaths = backupPaths,
                onOpenTrash = { showTrash = true }
            )

            RightToolbar(
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd)
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