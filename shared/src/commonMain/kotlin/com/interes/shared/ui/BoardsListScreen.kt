package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.interes.shared.model.BoardSummary
import com.interes.shared.repository.BoardRepository
import com.interes.shared.util.localFilePathToUri
import kotlinx.coroutines.launch

/**
 * Стартовый экран: карточки всех досок. Долгое нажатие на карточку ИЛИ
 * кнопка "⋮" в её углу открывает меню "Переименовать / Удалить" (кнопка —
 * потому что "долгое нажатие" мышью на десктопе неочевидно и легко
 * пропустить; с ней действие остаётся доступным и по клику). Кнопка "+"
 * создаёт новую доску и сразу в неё переходит. Поле поиска в тулбаре
 * фильтрует доски по названию и категории на лету, без запроса к БД.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsListScreen(
    boards: List<BoardSummary>,
    repository: BoardRepository,
    onOpenBoard: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    nativeWindowController: NativeWindowController,
    onExitApp: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    // Доска, для которой долгим нажатием (или кнопкой "⋮") вызвали меню действий.
    var actionsFor by remember { mutableStateOf<BoardSummary?>(null) }
    var renamingBoard by remember { mutableStateOf<BoardSummary?>(null) }
    var deletingBoard by remember { mutableStateOf<BoardSummary?>(null) }

    // Поиск досок. showSearchField отдельно от searchQuery: значок лупы
    // разворачивает поле поиска в тулбаре, крестик его закрывает и
    // одновременно сбрасывает запрос — так после закрытия поиска список
    // не остаётся молча отфильтрованным по прошлому запросу.
    var showSearchField by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val visibleBoards = if (searchQuery.isBlank()) {
        boards
    } else {
        boards.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchField) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск досок") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // windowDragHandle — тут, а не на всю TopAppBar: поле
                        // поиска, кнопки лупы/шестерёнки/закрытия должны
                        // оставаться кликабельными, а не пытаться таскать
                        // окно при каждом клике по ним. Захватывать можно
                        // только за сам текст заголовка — как за настоящую
                        // title bar.
                        Text("Interes", modifier = Modifier.windowDragHandle(nativeWindowController))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (showSearchField) {
                            showSearchField = false
                            searchQuery = ""
                        } else {
                            showSearchField = true
                        }
                    }) {
                        // Без material-icons-extended — лупа/крестик текстом,
                        // тем же приёмом, что стрелка "назад" в BoardScreen.
                        Text(
                            if (showSearchField) "\u2715" else "\uD83D\uDD0D",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    // Настройки и закрытие приложения скрываем, пока открыто
                    // поле поиска — иначе в узком окне тулбар начинает
                    // теснить текстовое поле.
                    if (!showSearchField) {
                        IconButton(onClick = onOpenSettings) {
                            Text("\u2699", style = MaterialTheme.typography.titleLarge)
                        }
                        // Замена системной кнопки "развернуть" — двойной
                        // клик по заголовку тоже разворачивает (см.
                        // windowDragHandle), эта кнопка — просто более
                        // очевидная альтернатива для тех, кто не додумается
                        // до двойного клика по надписи "Interes".
                        IconButton(onClick = { nativeWindowController.toggleMaximize() }) {
                            Text("\u2750", style = MaterialTheme.typography.titleLarge)
                        }
                        // Единственный способ закрыть приложение из UI теперь,
                        // когда у окна нет системной рамки (undecorated,
                        // см. Main.kt) — раньше был системный крестик в углу
                        // окна. Alt+F4 по-прежнему тоже работает (это
                        // системный шорткат ОС, не завязанный на видимую
                        // title bar), но кнопка в интерфейсе нужна для тех,
                        // кто про Alt+F4 не вспомнит.
                        IconButton(onClick = onExitApp) {
                            Text("\u2715", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        if (boards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Пока нет ни одной доски — нажмите \"+\", чтобы создать первую")
            }
        } else if (visibleBoards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Ничего не найдено по запросу \"$searchQuery\"")
            }
        } else {
            LazyVerticalGrid(
                // Adaptive вместо Fixed(2): на телефоне это по факту даёт те
                // же 2 колонки, но на широком окне Windows подбирает больше
                // колонок под реальную ширину, а не оставляет половину окна
                // пустой. minSize — минимальная ширина карточки.
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleBoards, key = { it.id }) { board ->
                    BoardCard(
                        board = board,
                        onClick = { onOpenBoard(board.id) },
                        onLongPress = { actionsFor = board },
                        onActionsClick = { actionsFor = board }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateBoardDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, category ->
                showCreateDialog = false
                scope.launch {
                    val id = repository.createBoard(title, category)
                    onOpenBoard(id)
                }
            }
        )
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
                    }) { Text("Переименовать") }
                    TextButton(onClick = {
                        deletingBoard = board
                        actionsFor = null
                    }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionsFor = null }) { Text("Отмена") }
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
            title = { Text("Удалить доску?") },
            text = { Text("Доска \"${board.title}\" и все фото на ней будут удалены безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteBoard(board.id) }
                    deletingBoard = null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingBoard = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun BoardCard(
    board: BoardSummary,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onActionsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(board.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        if (board.thumbnailPath != null) {
            val context = LocalPlatformContext.current
            AsyncImage(
                model = remember(board.thumbnailPath) {
                    ImageRequest.Builder(context)
                        .data(localFilePathToUri(board.thumbnailPath))
                        .memoryCacheKey(board.thumbnailPath)
                        .diskCacheKey(board.thumbnailPath)
                        .size(Size(480, 480))
                        .crossfade(true)
                        .build()
                },
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
                .background(Color.Black.copy(alpha = 0.35f))
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
                        // "фото" в русском не склоняется по числам (1 фото,
                        // 2 фото, 5 фото — везде одна форма), поэтому без
                        // отдельной логики множественного числа.
                        " · ${board.photoCount} фото",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateBoardDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая доска") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Категория") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onCreate(title.trim(), category.trim().ifBlank { "Общее" }) }
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать доску") },
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
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
