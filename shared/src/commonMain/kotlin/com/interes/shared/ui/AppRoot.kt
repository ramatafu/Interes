package com.interes.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import kotlin.math.roundToInt
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

    // Отдаём текущий процент прозрачности платформенному контроллеру окна.
    // На Desktop setOpacityPercent — no-op (прозрачность там по-прежнему
    // ведёт только graphicsLayer.alpha ниже). На Android это по-настоящему
    // меняет alpha системного окна Activity — см. NativeWindowController
    // .android.kt.
    LaunchedEffect(appOpacityPercent) {
        nativeWindowController.setOpacityPercent(appOpacityPercent.roundToInt())
    }

    // Сам ползунок прозрачности — показываем/обновляем/прячем через
    // платформенный контроллер, а не рисуем прямо здесь Compose-элементом.
    // На Desktop showOpacitySlider/hideOpacitySlider — no-op (ползунок там
    // по-прежнему в RightToolbar.kt, ниже). На Android это открывает
    // отдельное системное окно (Dialog), которое НЕ гаснет вместе с
    // остальным приложением — см. NativeWindowController.android.kt.
    LaunchedEffect(viewerState != null, appOpacityPercent) {
        if (viewerState != null) {
            nativeWindowController.showOpacitySlider(appOpacityPercent.roundToInt()) { newPercent ->
                appOpacityPercent = newPercent.toFloat()
            }
        } else {
            nativeWindowController.hideOpacitySlider()
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
            // Ширина боковых колонок — 72.dp на Desktop (как и раньше,
            // ToolbarWidth/RightToolbarWidth), 48.dp на Android (см. doc в
            // NativeWindowController.kt: там в колонках теперь только
            // Корзина/О программе внизу, освободившееся место отдаём сетке
            // досок). Используется здесь везде вместо захардкоженных
            // ToolbarWidth/RightToolbarWidth, чтобы контент, сами тулбары и
            // декоративные заполнители углов были одной ширины.
            val toolbarWidth = nativeWindowController.sideToolbarWidth

            // На Android главный экран (список досок), экран самой доски
            // (сетка её фото) И режим просмотра отдельного фото — все
            // получают ДВЕ горизонтальные полосы (верхняя + всё остальное)
            // вместо боковых колонок: SideToolbar/RightToolbar там вообще не
            // рисуются, а инсет контента становится 0 — экран получает
            // ПОЛНУЮ ширину окна, и стрелка "Назад"/заголовок доски
            // (BoardScreen.kt, видна и во время просмотра фото — сам
            // просмотрщик рисуется поверх НИЖЕ её высоты, см. padding в
            // PhotoViewerContent) оказываются ровно у истинного левого края.
            // "Корзина"/"О программе" при этом переехали в нижнюю панель
            // BoardsListScreen.kt (см. её bottomBar); "Добавить фото" на
            // экране доски — в её собственную верхнюю панель (см.
            // showAddPhotoInTopBar в BoardScreen.kt).
            //
            // Стрелки ◀/▶ (пролистать фото) на Android при этом пропадают —
            // SideToolbar/RightToolbar с ними больше не рисуются НИГДЕ на
            // Android. Замена — свайп по фото (HorizontalPager в
            // PhotoViewerContent и так уже поддерживает это). Ползунок
            // прозрачности НЕ ЗАТРОНУТ: на Android он и без того рисуется
            // не здесь, а в отдельном системном окне (см.
            // nativeWindowController.showOpacitySlider ниже) — от
            // SideToolbar/RightToolbar никак не зависит.
            //
            // Боковые тулбары остаются ТОЛЬКО в корзине (её пока не
            // трогали) — единственный экран, где hideSideBars всё ещё
            // false на Android. На Desktop primaryActionsInTopBar всегда
            // false — там hideSideBars всегда false, ничего не меняется ни
            // на одном экране.
            val isHomeScreen = !showTrash && selectedBoardId == null
            val hideSideBars = nativeWindowController.primaryActionsInTopBar && !showTrash


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
                    .padding(
                        start = if (hideSideBars) 0.dp else toolbarWidth,
                        end = if (hideSideBars) 0.dp else toolbarWidth
                    )
            ) {
                // Затухающий слой: прозрачность гасит ТОЛЬКО контент.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // handlesOpacityNatively == true (Android) —
                            // прозрачность уже применена ко ВСЕМУ системному
                            // окну (см. LaunchedEffect выше), этот слой
                            // остаётся полностью непрозрачным, иначе
                            // прозрачность применилась бы дважды. На Desktop
                            // (false) — как и раньше, только этот слой гасит
                            // содержимое.
                            alpha = if (nativeWindowController.handlesOpacityNatively) 1f else appOpacityPercent / 100f
                        }
                ) {
                    // color = Color.Transparent — иначе Surface без явного
                    // цвета берёт MaterialTheme.colorScheme.surface (сплошной
                    // непрозрачный почти-белый) и красит им ВЕСЬ inset-контент
                    // ПОД Scaffold каждого экрана. Из-за этого 93%-прозрачный
                    // фон BoardsListScreen (ToolbarBackgroundColor) смешивался
                    // не с настоящим прозрачным окном (transparent = true в
                    // Main.kt), а с этим скрытым непрозрачным слоем — и
                    // прозрачность была практически незаметна.
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
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
                                    searchQuery = searchQuery,
                                    // "Корзина" в нижней панели — только на
                                    // Android (см. hideSideBars выше и
                                    // bottomBar в BoardsListScreen.kt): там
                                    // SideToolbar на главном экране не
                                    // рисуется, и корзина переезжает сюда.
                                    onOpenTrash = { showTrash = true }
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
                                        // Сначала закрываем просмотрщик фото (если
                                        // он открыт поверх доски), и только вторым
                                        // нажатием выходим из самой доски — та же
                                        // очерёдность, что и у системной кнопки
                                        // "назад" (см. PlatformBackHandler выше).
                                        // Раньше здесь было selectedBoardId = null
                                        // напрямую — стрелка "Назад" на панели
                                        // доски (та остаётся видна и кликабельна
                                        // поверх/под просмотрщиком) сразу выкидывала
                                        // на список досок мимо просмотрщика.
                                        onBack = {
                                            if (viewerState != null) viewerState = null else selectedBoardId = null
                                        },
                                        onPhotoClick = { photos, index -> viewerState = photos to index },
                                        nativeWindowController = nativeWindowController,
                                        onPickImagesReady = { pickImagesForCurrentBoard = it },
                                        // На Android, пока не открыт просмотрщик
                                        // фото, RightToolbar (с "Добавить фото")
                                        // не рисуется вовсе (см. hideSideBars
                                        // выше) — кнопка переезжает в верхнюю
                                        // панель самой доски.
                                        showAddPhotoInTopBar = hideSideBars
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
                        onDismiss = { viewerState = null },
                        // На Android крестик убран — закрытие просмотра
                        // теперь только системной кнопкой/жестом "Назад"
                        // (см. PlatformBackHandler выше). На Desktop
                        // остаётся true по умолчанию — там это единственный
                        // способ выйти.
                        showDismissButton = !nativeWindowController.primaryActionsInTopBar
                    )
                }
            }

            // Левый тулбар со стрелкой ◀. На Android не рисуется, пока
            // hideSideBars (главный экран, экран доски и просмотр фото) —
            // "Корзина" переехала в нижнюю панель BoardsListScreen.kt, а
            // "Домой/+/Резервная копия" и так уже в верхнем тулбаре. Только
            // в корзине — рисуется как обычно.
            if (!hideSideBars) {
                SideToolbar(
                    modifier = Modifier.fillMaxHeight().align(Alignment.CenterStart),
                    onHome = goHome,
                    onCreateBoard = { showCreateBoardDialog = true },
                    backupPaths = backupPaths,
                    onOpenTrash = { showTrash = true },
                    onPrevPhoto = onPrevPhoto,
                    // На Android эта группа кнопок теперь в верхнем тулбаре
                    // (см. ниже, PrimaryActionButtons) — здесь не дублируем.
                    showPrimaryActions = !nativeWindowController.primaryActionsInTopBar,
                    width = toolbarWidth,
                    // compact сжимает кнопку "Корзина" внизу до 40.dp — нужно,
                    // раз сама колонка на Android уже 48.dp, а не 72.dp (см.
                    // toolbarWidth выше).
                    compact = nativeWindowController.primaryActionsInTopBar
                )
            }

            // Правый тулбар: кнопка "Добавить фото" (на Android не нужна —
            // она теперь в верхней панели самой доски, см.
            // showAddPhotoInTopBar в BoardScreen.kt), стрелка ▶ +
            // вертикальный ползунок прозрачности. На Android не рисуется,
            // пока hideSideBars — "О программе" переехала в нижнюю панель
            // BoardsListScreen.kt. На Desktop рисуется как обычно на всех
            // экранах.
            if (!hideSideBars) {
                RightToolbar(
                    modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                    onNextPhoto = onNextPhoto,
                    // Свой (вертикальный) ползунок RightToolbar — только когда
                    // прозрачность НЕ обрабатывается нативно (Desktop). На
                    // Android (handlesOpacityNatively == true) ползунок теперь
                    // рисуется в отдельном системном окне (см.
                    // nativeWindowController.showOpacitySlider выше), здесь
                    // остаётся null.
                    opacityPercent = if (pagerState != null && !nativeWindowController.handlesOpacityNatively) appOpacityPercent else null,
                    onOpacityChange = if (pagerState != null && !nativeWindowController.handlesOpacityNatively) {
                        { appOpacityPercent = it }
                    } else null,
                    onAddPhoto = if (selectedBoardId != null) pickImagesForCurrentBoard else null,
                    // На Android "О программе" переехала сюда, на уровень
                    // корзины (см. SideToolbar.kt) — видна всегда, как и
                    // корзина, а не только на главном экране.
                    showAboutButton = nativeWindowController.primaryActionsInTopBar,
                    width = toolbarWidth,
                    // compact сжимает "Добавить фото" и "О программе" до 40.dp —
                    // нужно при узкой (48.dp) колонке на Android.
                    compact = nativeWindowController.primaryActionsInTopBar
                )
            }

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
                                .width(toolbarWidth)
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
                        // Правая группа: лупа (и, на Desktop, кнопки окна) —
                        // прижаты к самому правому краю окна.
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TopBarGlyph(onClick = { showSearchField = true }) { SearchGlyph() }
                            // "Свернуть/Развернуть/Закрыть" — только там, где
                            // у платформы есть своё окно с рамкой (Desktop).
                            // На Android showWindowControls == false, кнопки
                            // не рисуются вовсе.
                            if (nativeWindowController.showWindowControls) {
                                Spacer(modifier = Modifier.width(60.dp))
                                WindowControlButtons(nativeWindowController = nativeWindowController, onClose = onExitApp)
                            }
                        }

                        // "Домой/Создать доску/Резервная копия" — РАВНОМЕРНО
                        // распределены В ПРОМЕЖУТКЕ между значком приложения
                        // (заканчивается на ToolbarWidth от левого края) и
                        // лупой (TopBarGlyph, 40.dp, у самого правого края) —
                        // отступы start/end этого Row обрезают его ровно до
                        // этого промежутка, а SpaceEvenly делит его на равные
                        // доли (равный зазор до первой кнопки, между кнопками
                        // и после последней). Только на Android
                        // (primaryActionsInTopBar == true); на Desktop эта
                        // группа остаётся в SideToolbar.kt.
                        if (nativeWindowController.primaryActionsInTopBar) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxWidth()
                                    .padding(start = toolbarWidth, end = 40.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PrimaryActionButtons(
                                    onHome = goHome,
                                    onCreateBoard = { showCreateBoardDialog = true },
                                    backupPaths = backupPaths,
                                    compact = true,
                                    // "О программе" сюда не переезжает — она
                                    // в правом тулбаре, на уровне корзины
                                    // (см. RightToolbar, showAboutButton).
                                    includeInfo = false
                                )
                            }
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
                //
                // На Android, пока боковые тулбары скрыты (hideSideBars —
                // сейчас это экран доски и просмотр фото, см. doc выше),
                // заполнители не рисуются вовсе: собственная TopAppBar
                // экрана доски теперь и так во всю ширину окна (инсет
                // контента = 0), рисовать эти два квадрата было бы не рядом
                // с несуществующим боковым тулбаром, а поверх кнопок "Назад"/
                // "+" самой панели.
                if (!hideSideBars) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .width(toolbarWidth)
                            .height(TopToolbarHeight)
                            .background(TopToolbarColor)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .width(toolbarWidth)
                            .height(TopToolbarHeight)
                            .background(TopToolbarColor)
                    )
                }
                // "Свернуть / Развернуть / Закрыть" — рисуются здесь, у
                // настоящего правого края ОКНА (align в этом самом внешнем
                // Box), а не внутри TopAppBar экрана доски/корзины: та
                // инсетится под RightToolbar (см. padding контента выше), и
                // её правый край не совпадает с краем окна — кнопки были бы
                // левее, чем на главном экране. Ряд кнопок (144.dp) шире
                // заполнителя (RightToolbarWidth = 72.dp) и заходит поверх
                // TopAppBar экрана доски/корзины — там тот же TopToolbarColor
                // и (после переноса кнопок сюда) actions больше не рисует,
                // так что стык незаметен.
                if (nativeWindowController.showWindowControls) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .height(TopToolbarHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WindowControlButtons(nativeWindowController = nativeWindowController, onClose = onExitApp)
                    }
                }
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