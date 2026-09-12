package com.interes.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.interes.shared.generated.resources.Res
import com.interes.shared.generated.resources.board_placeholder
import com.interes.shared.model.BoardSummary
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import com.interes.shared.util.localFilePathToUri
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

// Высота верхнего тулбара — фиксированная, чтобы панель НЕ растягивалась
// на всё окно (старый TopAppBar с Box(fillMaxSize()) в title именно этим
// и болел: кнопки уезжали в середину окна).
/**
 * Высота верхней панели — используется не только здесь, но и в AppRoot.kt
 * (см. заполнители углов, из-за которых верхняя панель визуально ложится
 * поверх боковых тулбаров), поэтому не private.
 */
val TopToolbarHeight: Dp = 56.dp

/**
 * Стартовый экран: карточки всех досок. Долгое нажатие на карточку ИЛИ
 * кнопка "⋮" в её углу открывает меню "Переименовать / Удалить" (кнопка —
 * потому что "долгое нажатие" мышью на десктопе неочевидно и легко
 * пропустить; с ней действие остаётся доступным и по клику). Кнопка "+"
 * создаёт новую доску и сразу в неё переходит. Поле поиска в тулбаре
 * фильтрует доски по названию и категории на лету, без запроса к БД.
 *
 * Верхний тулбар — самописная панель фиксированной высоты (НЕ TopAppBar):
 * слева — пустая перетаскиваемая область (зажатие мышью двигает окно),
 * справа — лупа, затем ровно 60 dp, затем "Свернуть / Развернуть / Закрыть".
 * Кнопки — компактные глифы без внутренних отступов IconButton, поэтому
 * расстояние между лупой и "Свернуть" — честно 60 dp, а не 60 + паддинги.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsListScreen(
    boards: List<BoardSummary>,
    repository: BoardRepository,
    onOpenBoard: (Long) -> Unit,
    onCreateBoard: () -> Unit,
    nativeWindowController: NativeWindowController,
    onExitApp: () -> Unit,
    // Поиск — состояние теперь в AppRoot.kt: там же теперь рисуется САМА
    // верхняя панель (см. её комментарий в AppRoot.kt: она должна доходить
    // до истинных краёв окна, а не только до края отступа под боковые
    // тулбары, поэтому больше не может жить внутри Scaffold этого экрана).
    // Здесь запрос используется только для фильтрации сетки досок.
    searchQuery: String,
    // "Корзина" в нижней панели — только на Android (см. bottomBar ниже):
    // там на главном экране SideToolbar вообще не рисуется (см.
    // hideSideBarsOnHome в AppRoot.kt), и корзина переезжает сюда. На
    // Desktop этот колбэк передаётся, но не используется — там корзина
    // по-прежнему в SideToolbar.kt.
    onOpenTrash: () -> Unit,
    // "Резервная копия" в нижней панели — тоже только на Android, рядом с
    // "Корзиной" (см. bottomBar ниже). На Desktop не используется — там
    // кнопка по-прежнему в SideToolbar.kt.
    backupPaths: BackupPaths
) {
    val scope = rememberCoroutineScope()
    val language = LocalAppLanguage.current

    // Доска, для которой долгим нажатием (или кнопкой "⋮") вызвали меню действий.
    var actionsFor by remember { mutableStateOf<BoardSummary?>(null) }
    var renamingBoard by remember { mutableStateOf<BoardSummary?>(null) }
    var deletingBoard by remember { mutableStateOf<BoardSummary?>(null) }

    val visibleBoards = if (searchQuery.isBlank()) {
        boards
    } else {
        boards.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        // Фон экрана целиком (позади сетки карточек досок/"комнат") — тот
        // же цвет, что и у тулбаров, но с прозрачностью 7% (см. AppTheme.kt).
        containerColor = ToolbarBackgroundColor,
        topBar = {
            // Сама панель (значок/название/поиск/кнопки окна) теперь
            // рисуется в AppRoot.kt — во всю ширину ОКНА, а не только до
            // края отступа под боковые тулбары (как было бы, останься она
            // здесь, внутри Scaffold, который отступает от SideToolbar/
            // RightToolbar). Тут — только резервирование той же высоты,
            // чтобы сетка досок ниже не пряталась под настоящей панелью.
            Spacer(modifier = Modifier.fillMaxWidth().height(TopToolbarHeight))
        },
        // FloatingActionButton "+" убран — на Android "Создать доску"
        // теперь в нижней панели (см. bottomBar ниже), на Desktop — в
        // левом тулбаре (SideToolbar.kt), который вызывает тот же
        // onCreateBoard.
        bottomBar = {
            // Статистика — по ВСЕМ доскам, не только по видимым после
            // поиска: это общая сводка по приложению, а не по результатам
            // фильтра. BottomAppBar, а не просто Text с отступом снизу —
            // тут же и фон/эффект приподнятости, бесплатно, без лишней
            // разметки.
            //
            // Панель рисуется ВСЕГДА, даже когда boards пуст (после
            // удаления последней доски) — раньше здесь было условие
            // if (boards.isNotEmpty()), из-за которого на Android вместе с
            // панелью пропадали "Корзина", "Резервная копия", "О
            // программе" и "Создать доску": пустой список досок не должен
            // отрезать доступ к этим кнопкам, счётчик просто показывает
            // "0 досок • 0 фото".
            val totalPhotos = boards.sumOf { it.photoCount }
            // containerColor = Color.Transparent — а не ToolbarBackgroundColor
            // ещё раз: фон ВСЕГО Scaffold (см. containerColor выше) уже
            // покрывает эту область полупрозрачным цветом. Если покрасить
            // BottomAppBar своим ТАКИМ ЖЕ 93%-прозрачным цветом поверх —
            // два одинаковых полупрозрачных слоя друг на друге почти
            // складываются до непрозрачности (0.93×0.93 ≈ 99.5% непрозрачно)
            // и нижняя панель визуально выглядит сплошной, в отличие от
            // остального фона. Transparent здесь даёт ОДИН слой прозрачности
            // на весь экран — как и должно быть.
            BottomAppBar(containerColor = Color.Transparent) {
                val countText = "${boards.size} ${language.boardsWord(boards.size)} • $totalPhotos ${language.photosWord(totalPhotos)}"
                if (nativeWindowController.primaryActionsInTopBar) {
                    // Android: "Корзина" + "Резервная копия" слева,
                    // счётчик по центру, переключатель языка + "Создать
                    // доску" справа. "О программе" отсюда убрана — теперь
                    // это клик по значку-логотипу "In" в верхнем тулбаре
                    // (см. AppRoot.kt), а не отдельная кнопка здесь.
                    // SideToolbar/RightToolbar на этом экране не рисуются
                    // (см. hideSideBars в AppRoot.kt). "Домой" из верхнего
                    // тулбара убрана совсем — на Android её больше нет
                    // нигде.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        // SpaceEvenly — равные промежутки между всеми
                        // пятью элементами подряд, простое и предсказуемое
                        // распределение вместо ручной расстановки Spacer'ов
                        // под конкретное число кнопок.
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopBarGlyph(onClick = onOpenTrash) { TrashGlyph() }
                        BackupButton(backupPaths = backupPaths, compact = true)
                        Text(countText, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        LanguageButton(compact = true)
                        CreateBoardButton(onCreateBoard = onCreateBoard, compact = true)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(countText, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    ) { padding ->
        if (boards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                // textAlign = Center — Box уже центрирует сам Text как блок,
                // но при переносе на несколько строк каждая строка внутри
                // него по умолчанию прижата к левому краю; textAlign
                // выравнивает уже сам текст внутри строк.
                Text(
                    language.noBoardsYet(),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else if (visibleBoards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(language.noSearchResults(searchQuery))
            }
        } else {
            // На Android (fixedBoardGridColumns != null, см. doc в
            // NativeWindowController.kt) отступы вокруг сетки и МЕЖДУ РЯДАМИ
            // сжаты (2.dp вместо 8.dp — было 4.dp, уменьшено ещё раз по
            // просьбе "комнаты ещё шире") — при фиксированных 2 колонках это
            // способ увеличить сами карточки (пропорционально, без искажения
            // формы). Расстояние МЕЖДУ ДОСКАМИ В РЯДУ (по горизонтали) —
            // отдельное значение, 5.dp, задано явно и не связано с gridGap.
            // На Desktop ничего не трогаем — там всё по 8.dp, как и раньше.
            val gridGap = if (nativeWindowController.fixedBoardGridColumns != null) 2.dp else 8.dp
            val horizontalGap = if (nativeWindowController.fixedBoardGridColumns != null) 5.dp else gridGap

            // BoxWithConstraints — чтобы узнать РЕАЛЬНО видимую высоту (уже
            // за вычетом верхней/нижней панели, см. .padding(padding) здесь)
            // и посчитать высоту карточки так, чтобы влезало ровно 3 ряда
            // без прокрутки — а не как раньше, через aspectRatio (тот высоту
            // экрана вообще не учитывает, 3 ряда могли не влезть или
            // остаться с пустым хвостом). Только на Android
            // (fixedBoardGridColumns != null) — на Desktop cardHeight
            // остаётся null, и BoardCard считает высоту по-старому, через
            // aspectRatio(0.85f). Использует gridGap (вертикальный зазор), а
            // не horizontalGap — горизонтальный зазор на высоту рядов не
            // влияет.
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
                val visibleRows = 3
                val cardHeight = if (nativeWindowController.fixedBoardGridColumns != null) {
                    // Между рядами (visibleRows - 1) зазоров + 2 отступа
                    // contentPadding (сверху и снизу) = (visibleRows + 1)
                    // зазоров суммарно по вертикали.
                    ((maxHeight - gridGap * (visibleRows + 1)) / visibleRows).coerceAtLeast(0.dp)
                } else {
                    null
                }

                LazyVerticalGrid(
                    // На Android nativeWindowController.fixedBoardGridColumns
                    // == 2 — ровно по 2 доски в ряд, всегда (см. doc в
                    // NativeWindowController.kt). На Desktop это null, и сетка
                    // ведёт себя как раньше: AdaptiveMaxColumns сама подбирает
                    // число колонок под ширину окна (не GridCells.Adaptive
                    // напрямую и не GridCells.Fixed(8): на телефоне/узком окне
                    // должно быть меньше 8 колонок, а на широком — вплоть до 8,
                    // но не больше; minSize — минимальная ширина карточки).
                    columns = nativeWindowController.fixedBoardGridColumns?.let { GridCells.Fixed(it) }
                        ?: AdaptiveMaxColumns(minSize = 160.dp, maxColumns = 8),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(gridGap),
                    horizontalArrangement = Arrangement.spacedBy(horizontalGap),
                    verticalArrangement = Arrangement.spacedBy(gridGap)
                ) {
                    items(visibleBoards, key = { it.id }) { board ->
                        BoardCard(
                            board = board,
                            onClick = { onOpenBoard(board.id) },
                            onLongPress = { actionsFor = board },
                            onActionsClick = { actionsFor = board },
                            nativeWindowController = nativeWindowController,
                            heightOverride = cardHeight
                        )
                    }
                }
            }
        }
    }

    // Меню "Переименовать / Удалить" — просто диалог с двумя пунктами вместо
    // отдельной анкорной DropdownMenu, чтобы не завязываться на точную
    // позицию карточки в скроллящейся сетке.
    actionsFor?.let { board ->
        AlertDialog(
            onDismissRequest = { actionsFor = null },
            title = { Text(board.title) },
            text = {
                Column {
                    TextButton(onClick = {
                        renamingBoard = board
                        actionsFor = null
                    }) { Text(language.rename()) }
                    TextButton(onClick = {
                        deletingBoard = board
                        actionsFor = null
                    }) { Text(language.delete(), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionsFor = null }) { Text(language.cancel()) }
            }
        )
    }

    renamingBoard?.let { board ->
        RenameBoardDialog(
            currentTitle = board.title,
            onDismiss = { renamingBoard = null },
            onConfirm = { newTitle ->
                scope.launch { repository.renameBoard(board.id, newTitle) }
                renamingBoard = null
            }
        )
    }

    deletingBoard?.let { board ->
        AlertDialog(
            onDismissRequest = { deletingBoard = null },
            title = { Text(language.deleteBoardTitle()) },
            text = { Text(language.deleteBoardText(board.title)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.softDeleteBoard(board.id) }
                    deletingBoard = null
                }) { Text(language.delete(), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingBoard = null }) { Text(language.cancel()) }
            }
        )
    }
}

@Composable
private fun BoardCard(
    board: BoardSummary,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onActionsClick: () -> Unit,
    nativeWindowController: NativeWindowController,
    // Высота карточки, посчитанная под "ровно 3 ряда видно" (Android, см.
    // BoardsListScreen выше). null — на Desktop, там высота по-прежнему
    // берётся из aspectRatio(0.85f) ниже, как и было всегда.
    heightOverride: Dp? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (heightOverride != null) Modifier.height(heightOverride) else Modifier.aspectRatio(0.85f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(board.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        if (board.thumbnailPaths.isNotEmpty()) {
            // Коллаж "1+2": первое фото — 50% площади (слева), второе и
            // третье делят оставшиеся 50% между собой поровну (сверху и
            // снизу справа). Если фото меньше 3 — компоновка сама
            // подстраивается (см. BoardPhotoCollage ниже). Пересобирается
            // автоматически: thumbnailPaths приходит из реактивного Flow
            // (observeBoardSummaries), любое добавление/удаление фото в БД
            // сразу даёт новый список путей и рекомпозицию.
            BoardPhotoCollage(
                thumbnailPaths = board.thumbnailPaths,
                modifier = Modifier.fillMaxSize()
            )
            // Лёгкий градиент поверх ВСЕГО коллажа (не поверх отдельных
            // плиток) — снизу темнее, сверху прозрачно, чтобы название и
            // счётчик фото читались на любом фоне.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f)
                        )
                    )
            )
        } else {
            // Заглушка для пустой доски — вместо эмодзи-рамки теперь
            // картинка-заглушка (см. board_placeholder.png).
            Image(
                painter = painterResource(Res.drawable.board_placeholder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Кнопка меню действий — видимая альтернатива долгому нажатию
        // (на десктопе с мышью "долгое нажатие" неочевидно). Отдельный
        // pointerInput на самой кнопке не нужен: IconButton сам глотает
        // клик и не даёт ему всплыть до onTap внешнего Box.
        IconButton(
            onClick = onActionsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    // Подсветка — цвет верхнего тулбара (TopToolbarColor),
                    // только на Android (по просьбе). На Desktop оставлен
                    // исходный полупрозрачный чёрный.
                    if (nativeWindowController.fixedBoardGridColumns != null) {
                        TopToolbarColor
                    } else {
                        Color.Black.copy(alpha = 0.35f)
                    }
                )
        ) {
            Text("\u22EE", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }

        // Тёмная подложка снизу + название/категория/счётчик фото поверх —
        // читаемо и на пустой доске (без превью), и поверх светлого фото.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(board.title, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Row {
                    Text(board.category, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    Text(
                        " · ${board.photoCount} ${LocalAppLanguage.current.photosWord(board.photoCount)}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Коллаж превью доски из 1–3 фото, компоновка "1+2":
 * - 1 фото — просто на всю карточку;
 * - 2 фото — пополам по вертикальной линии (слева/справа);
 * - 3 фото — первое занимает левую половину (50% площади), второе и
 *   третье делят правую половину пополам по горизонтали (друг над другом).
 * У каждой миниатюры скруглённые углы 8.dp — отдельно от общего скругления
 * карточки (16.dp у внешнего Box в BoardCard). thumbnailPaths.isEmpty()
 * сюда не приходит — этот случай отсеивается заглушкой ещё в BoardCard.
 */
@Composable
private fun BoardPhotoCollage(thumbnailPaths: List<String>, modifier: Modifier = Modifier) {
    val context = LocalPlatformContext.current
    // Небольшой зазор между плитками коллажа — иначе скруглённые углы
    // соседних фото визуально сливаются друг с другом.
    val gap = 2.dp

    @Composable
    fun Thumbnail(path: String, thumbnailModifier: Modifier) {
        AsyncImage(
            model = remember(path) {
                ImageRequest.Builder(context)
                    .data(localFilePathToUri(path))
                    .memoryCacheKey(path)
                    .diskCacheKey(path)
                    .size(Size(480, 480))
                    .crossfade(true)
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = thumbnailModifier.clip(RoundedCornerShape(8.dp))
        )
    }

    when (thumbnailPaths.size) {
        1 -> Thumbnail(thumbnailPaths[0], modifier)
        2 -> Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
            Thumbnail(thumbnailPaths[0], Modifier.weight(1f).fillMaxHeight())
            Thumbnail(thumbnailPaths[1], Modifier.weight(1f).fillMaxHeight())
        }
        else -> Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
            // Первое фото — левая половина, 50% площади всей карточки.
            Thumbnail(thumbnailPaths[0], Modifier.weight(1f).fillMaxHeight())
            // Второе и третье — правая половина, поровну друг над другом.
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                Thumbnail(thumbnailPaths[1], Modifier.weight(1f).fillMaxWidth())
                Thumbnail(thumbnailPaths[2], Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

// Не private — используется и здесь (FAB), и из AppRoot.kt (кнопка
// "Создать доску" в SideToolbar, видна поверх любого экрана).
@Composable
fun CreateBoardDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val language = LocalAppLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.newBoardDialogTitle()) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(language.titleLabel()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(language.categoryLabel()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onCreate(title.trim(), category.trim().ifBlank { language.defaultCategory() }) }
            ) { Text(language.create()) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(language.cancel()) }
        }
    )
}

@Composable
private fun RenameBoardDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    val language = LocalAppLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.renameBoardTitle()) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onConfirm(title.trim()) }
            ) { Text(language.save()) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(language.cancel()) }
        }
    )
}

/**
 * Как GridCells.Adaptive, но с потолком в maxColumns: подбирает число
 * колонок под реальную ширину экрана (минимум minSize на карточку), но
 * никогда не превышает maxColumns, даже на очень широком окне.
 * calculateCrossAxisCellSizes — официальный способ написать свою стратегию
 * колонок для LazyVerticalGrid (тот же метод, который под капотом
 * реализуют сами GridCells.Adaptive/GridCells.Fixed).
 */
private class AdaptiveMaxColumns(private val minSize: Dp, private val maxColumns: Int) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val minSizePx = minSize.roundToPx()
        val columns = ((availableSize + spacing).toFloat() / (minSizePx + spacing).toFloat())
            .toInt()
            .coerceIn(1, maxColumns)
        val cellSize = (availableSize - spacing * (columns - 1)) / columns
        // Остаток пикселей от целочисленного деления раздаём по одному
        // первым колонкам — иначе между последней карточкой и правым краем
        // мог бы оставаться заметный зазор в несколько пикселей.
        val remainder = (availableSize - spacing * (columns - 1)) - cellSize * columns
        return List(columns) { index -> if (index < remainder) cellSize + 1 else cellSize }
    }
}