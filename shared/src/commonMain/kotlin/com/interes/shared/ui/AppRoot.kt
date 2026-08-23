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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Точка входа UI. Простая навигация без внешней библиотеки — вся Interes
 * умещается в несколько "экранов" (список досок -> доска -> полноэкранный
 * просмотр, плюс отдельно Корзина), поэтому обычного mutableState для
 * текущего экрана достаточно. Слева — постоянный SideToolbar (см.
 * SideToolbar.kt), виден поверх любого из этих экранов; раздела "Настройки"
 * в приложении больше нет — то немногое, что в нём было (резервное
 * копирование, информация о программе), переехало в сам SideToolbar.
 *
 * Прозрачность применяется здесь, на самом верхнем уровне, ко ВСЕМУ
 * содержимому приложения разом — списку досок/доске И самому просмотрщику
 * вместе, — а не только к просмотрщику поверх доски.
 * - Android: тема Activity объявлена translucent
 *   (androidApp/src/main/res/values/themes.xml) + Compose graphicsLayer.alpha
 *   ниже — вместе дают реальную прозрачность до системного фона.
 * - Windows: окно undecorated (без системной рамки) — обязательное условие
 *   для реальной прозрачности окна через java.awt.Window.opacity, см.
 *   Main.kt и NativeWindowController.desktop.kt (там же — почему именно так,
 *   со ссылкой на официальную документацию Java). Раз рамки нет — драг за
 *   заголовок и закрытие окна реализованы вручную внутри самого приложения
 *   (перетаскивание заголовка в тулбарах экранов через nativeWindowController,
 *   кнопка "✕" в списке досок через onExitApp).
 *
 * appOpacityPercent — ползунок ВНУТРИ просмотрщика фото (см.
 * PhotoViewerControls), сбрасывается на 100% при каждом закрытии
 * просмотрщика/смене фото. Больше НЕТ отдельной постоянной "базовой"
 * прозрачности из настроек, которая раньше на него множилась — вместе с
 * разделом "Настройки" убрали и её, теперь этот процент — единственный
 * источник прозрачности окна.
 *
 * КУДА именно применяется этот процент — зависит от платформы, и решает
 * это NativeWindowController.handlesOpacityNatively (см. NativeWindowController.kt):
 * - Desktop (обычно): true — процент уходит в nativeWindowController, которая
 *   двигает java.awt.Window.opacity САМОГО OS-окна. Тогда Compose-слой ниже
 *   держим на alpha=1 и НЕ гасим его повторно — иначе прозрачность
 *   применилась бы дважды (окно+контент).
 * - Android, и Desktop в редком случае отсутствия поддержки оконной
 *   прозрачности у видеокарты: false — тогда как раньше, тем же процентом
 *   двигаем graphicsLayer.alpha контента. На Android это и есть основной,
 *   изначально рабочий механизм (translucent-тема Activity уже даёт
 *   реальную прозрачность до системного фона под Compose-контентом).
 *
 * SideToolbar и элементы управления просмотрщика специально вынесены
 * СНАРУЖИ этого затухающего слоя — панель инструментов должна оставаться
 * видимой и кликабельной, даже когда окно почти полностью прозрачно.
 */
@Composable
fun InteresRoot(
    repository: BoardRepository,
    backupPaths: BackupPaths,
    nativeWindowController: NativeWindowController,
    onExitApp: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // remember(repository) — ВАЖНО, а не просто repository.observeX().collectAsState()
    // напрямую. repository.observeBoardSummaries()/observeBoards() строят
    // НОВУЮ цепочку Flow при каждом вызове. Без remember каждая перерисовка
    // InteresRoot (а она перерисовывается именно при получении новых данных
    // из этих же Flow — читает boardSummaries/allBoards) создавала бы
    // Flow-объект заново; collectAsState видит "другой" Flow как смену ключа
    // и перезапускает сбор с нуля (сброс на initial = emptyList()) — из-за
    // этого перерисовка догоняла сама себя быстрее, чем БД успевала вернуть
    // актуальные данные, и список визуально никогда не "устаканивался" на
    // новом состоянии.
    val boardSummariesFlow = remember(repository) { repository.observeBoardSummaries() }
    val allBoardsFlow = remember(repository) { repository.observeBoards() }
    val trashedBoardsFlow = remember(repository) { repository.observeTrashedBoards() }
    val boardSummaries by boardSummariesFlow.collectAsState(initial = emptyList())
    val allBoards by allBoardsFlow.collectAsState(initial = emptyList())
    val trashedBoards by trashedBoardsFlow.collectAsState(initial = emptyList())

    // null = показан список досок; иначе id открытой доски.
    var selectedBoardId by remember { mutableStateOf<Long?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    // Диалог создания доски — поднят сюда (раньше жил в BoardsListScreen),
    // потому что теперь его открывает и кнопка "Создать доску" в SideToolbar,
    // а тот виден поверх ЛЮБОГО экрана, не только списка досок.
    var showCreateBoardDialog by remember { mutableStateOf(false) }

    // Открытая полноэкранная галерея: список фото доски + индекс, с которого
    // начать. null — галерея закрыта.
    var viewerState by remember { mutableStateOf<Pair<List<Photo>, Int>?>(null) }

    // Прозрачность просмотрщика (0–100%), управляется ползунком в
    // PhotoViewerControls. Сбрасывается на 100%, когда закрывается
    // просмотрщик (иначе список/доска рисковали бы остаться прозрачными)
    // и при пролистывании на другое фото (каждое фото начинается "с чистого
    // листа").
    var appOpacityPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(viewerState == null) {
        if (viewerState == null) appOpacityPercent = 100f
    }

    // Порядок важен: если открыта Корзина — "Назад" сначала закрывает её;
    // иначе если открыт просмотрщик — закрывает его, а не выкидывает сразу
    // из доски; иначе, если открыта доска — "Назад" возвращает к списку досок.
    PlatformBackHandler(enabled = showTrash) { showTrash = false }
    PlatformBackHandler(enabled = !showTrash && viewerState != null) { viewerState = null }
    PlatformBackHandler(enabled = !showTrash && viewerState == null && selectedBoardId != null) { selectedBoardId = null }

    LaunchedEffect(appOpacityPercent, nativeWindowController) {
        if (nativeWindowController.handlesOpacityNatively) {
            nativeWindowController.setOpacityPercent(appOpacityPercent.roundToInt())
        }
    }

    // "Домой" в SideToolbar — из ЛЮБОГО состояния (доска, просмотрщик,
    // Корзина) возвращает ровно к списку досок.
    val goHome: () -> Unit = {
        viewerState = null
        showTrash = false
        selectedBoardId = null
    }

    InteresTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Modifier.align()/.padding() — а не Row + Modifier.weight(1f).
            // .weight() (Row/ColumnScope) в этом проекте на практике ловит тот
            // же старый конфликт версий Compose-foundation, что раньше был у
            // WindowDraggableArea ("Cannot access ... it is internal in file"):
            // тот факт, что раньше это нигде не проявлялось, объясняется просто
            // тем, что .weight() до сих пор нигде не использовался, а не тем,
            // что конфликт был реально устранён. Box + align + padding даёт тот
            // же визуальный результат (панель слева фиксированной ширины,
            // контент занимает оставшееся) совершенно другим, гарантированно
            // рабочим в этом проекте механизмом (Box-выравнивание уже
            // используется, например, в BoardCard).
            Box(modifier = Modifier.fillMaxSize().padding(start = ToolbarWidth)) {
                val currentViewerState = viewerState
                val pagerState = if (currentViewerState != null) {
                    rememberPagerState(initialPage = currentViewerState.second) { currentViewerState.first.size }
                } else null

                if (pagerState != null) {
                    LaunchedEffect(pagerState.currentPage) { appOpacityPercent = 100f }
                }

                // Единый затухающий слой: список досок/доска/Корзина И сама
                // галерея (без элементов управления) вместе, одним alpha на
                // графическом слое — но ТОЛЬКО когда прозрачность окна не
                // обрабатывается нативно (см. doc-комментарий класса).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (nativeWindowController.handlesOpacityNatively) 1f else appOpacityPercent / 100f
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
                                // Доску могли удалить (в т.ч. её же длинным
                                // нажатием со списка) или переименовать, пока
                                // она открыта — актуальное название всегда
                                // берём из уже загруженного списка досок.
                                val currentTitle = allBoards.firstOrNull { it.id == boardId }?.title
                                // Доски больше нет (удалена/в Корзине) —
                                // возвращаемся к списку. LaunchedEffect, а не
                                // прямая запись в теле composable — запись
                                // состояния во время композиции нежелательна.
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
                            pagerState = pagerState,
                            // Отступ слева — под вертикальный ползунок
                            // прозрачности (см. PhotoViewerControls.kt): он
                            // должен стоять РЯДОМ с фото, а не поверх него.
                            modifier = Modifier.padding(start = PhotoViewerSliderAreaWidth)
                        )
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

            // Сама панель — поверх содержимого (которое сдвинуто под неё
            // отступом padding(start = ToolbarWidth) выше), прижата к левому
            // краю на всю высоту окна.
            SideToolbar(
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterStart),
                onHome = goHome,
                onCreateBoard = { showCreateBoardDialog = true },
                backupPaths = backupPaths,
                onOpenTrash = { showTrash = true }
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
