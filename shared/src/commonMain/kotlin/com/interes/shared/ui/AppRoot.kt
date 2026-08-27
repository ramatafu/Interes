package com.interes.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.interes.shared.generated.resources.Res
import com.interes.shared.generated.resources.app_icon
import com.interes.shared.model.Photo
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

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

    // Поиск досок (главный экран). Поднято сюда из BoardsListScreen.kt —
    // сама панель с полем поиска теперь рисуется прямо здесь, во всю
    // ширину ОКНА (см. ниже), а не только в пределах Scaffold того экрана.
    // showSearchField отдельно от searchQuery: значок лупы разворачивает
    // поле поиска, крестик его закрывает и одновременно сбрасывает запрос.
    var showSearchField by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Триггер "выбрать фото" для ТЕКУЩЕЙ открытой доски — поднят сюда из
    // BoardScreen.kt (см. onPickImagesReady там), чтобы кнопка "+" на
    // правом тулбаре (RightToolbar.kt) могла его вызвать. null, когда
    // доска не открыта — тогда RightToolbar кнопку вообще не показывает
    // (см. передачу onAddPhoto ниже).
    var pickImagesForCurrentBoard by remember { mutableStateOf<(() -> Unit)?>(null) }

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
                                    onExitApp = onExitApp,
                                    searchQuery = searchQuery
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
                                        nativeWindowController = nativeWindowController,
                                        onExitApp = onExitApp,
                                        onPickImagesReady = { pickImagesForCurrentBoard = it }
                                    )
                                }
                            }
                        }
                    }

                    if (currentViewerState != null && pagerState != null) {
                        PhotoViewerContent(
                            photos = currentViewerState.first,
                            pagerState = pagerState,
                            // Инсет сверху/снизу — та же высота, что и у
                            // верхнего тулбара (TopToolbarHeight), чтобы
                            // фото ложилось строго МЕЖДУ верхней панелью
                            // (счётчик/кнопки окна) и нижней границей окна,
                            // а не заходило под них (было видно самому
                            // фото под полупрозрачными чипами управления).
                            modifier = Modifier.padding(top = TopToolbarHeight, bottom = TopToolbarHeight)
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

            // Правый тулбар: кнопка "Добавить фото" (только когда открыта
            // доска — и в комнате, и в просмотрщике фото), стрелка ▶ +
            // вертикальный ползунок прозрачности (ползунок виден только
            // при открытом просмотрщике).
            RightToolbar(
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                onNextPhoto = onNextPhoto,
                opacityPercent = if (pagerState != null) appOpacityPercent else null,
                onOpacityChange = if (pagerState != null) {
                    { appOpacityPercent = it }
                } else null,
                onAddPhoto = if (selectedBoardId != null) pickImagesForCurrentBoard else null
            )

            // Верхняя панель — рисуется ЗДЕСЬ, а не внутри Scaffold
            // конкретного экрана: так она по-настоящему тянется от одного
            // края ОКНА до другого (а не только до края отступа под
            // боковые тулбары, как было бы внутри инсетнутого контента
            // выше) и ложится поверх верхних углов SideToolbar/RightToolbar
            // (рисуется последней — то есть поверх них).
            //
            // На главном экране (список досок) — настоящая, полностью
            // рабочая панель: значок + название слева, поиск/кнопки окна
            // справа. На экране доски и в корзине у них СВОЯ верхняя панель
            // внутри Scaffold (back-кнопка + заголовок) — она по-прежнему
            // отступает от боковых тулбаров, а тут для них только два
            // декоративных заполнителя углов, чтобы полоса визуально
            // продолжалась в их сторону.
            val isHomeScreen = !showTrash && selectedBoardId == null
            if (isHomeScreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .height(TopToolbarHeight)
                        .background(TopToolbarColor)
                        .windowDragHandle(nativeWindowController)
                ) {
                    // Значок приложения — на той же вертикальной оси, что и
                    // иконки левого тулбара (SideToolbar.kt): те центрированы
                    // по ширине колонки ToolbarWidth (72.dp), поэтому здесь —
                    // такой же Box шириной ToolbarWidth с центрированием,
                    // а не Row, прижатый к самому левому краю окна. Название
                    // "Interes" рядом с иконкой убрано — верхний тулбар
                    // теперь без текста. Скрыто в режиме поиска — там на
                    // этом месте разворачивается поле поиска.
                    if (!showSearchField) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(ToolbarWidth)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.app_icon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                    }

                    if (showSearchField) {
                        // Режим поиска: строка поиска досок + крестик
                        // закрытия. Форма и заливка — по образцу рендера в
                        // чате: скруглённая "таблетка" полупрозрачным белым
                        // поверх цвета панели, а не стандартный
                        // прямоугольный OutlinedTextField.
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск доски...") },
                            leadingIcon = { SearchGlyph(color = MaterialTheme.colorScheme.onSurface) },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.35f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 48.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(40.dp)
                                .clickable {
                                    showSearchField = false
                                    searchQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            CloseGlyph()
                        }
                    } else {
                        // Группа кнопок справа: лупа, ровно 60 dp, затем
                        // "Свернуть / Развернуть / Закрыть" — прижаты к
                        // самому правому краю окна.
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TopBarGlyph(onClick = { showSearchField = true }) { SearchGlyph() }
                            Spacer(modifier = Modifier.width(60.dp))
                            WindowControlButtons(nativeWindowController = nativeWindowController, onClose = onExitApp)
                        }
                    }
                }
            } else {
                // Заполнители углов — их размер: ширина ровно как у
                // соответствующего тулбара (ToolbarWidth / RightToolbarWidth),
                // высота — как у верхней панели (TopToolbarHeight, одинаковая
                // у всех трёх экранов). Цвет — TopToolbarColor, тот же, что
                // и у собственной верхней панели экрана доски/корзины (см.
                // BoardScreen.kt, TrashScreen.kt).
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