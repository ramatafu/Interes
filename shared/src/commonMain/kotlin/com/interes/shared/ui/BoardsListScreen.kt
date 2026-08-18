package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import kotlinx.coroutines.launch

/**
 * Стартовый экран: карточки всех досок. Долгое нажатие на карточку
 * открывает меню "Переименовать / Удалить", кнопка "+" создаёт новую доску
 * и сразу в неё переходит.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsListScreen(
    boards: List<BoardSummary>,
    repository: BoardRepository,
    onOpenBoard: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    // Доска, для которой долгим нажатием вызвали меню действий.
    var actionsFor by remember { mutableStateOf<BoardSummary?>(null) }
    var renamingBoard by remember { mutableStateOf<BoardSummary?>(null) }
    var deletingBoard by remember { mutableStateOf<BoardSummary?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Interes") }) },
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
                items(boards, key = { it.id }) { board ->
                    BoardCard(
                        board = board,
                        onClick = { onOpenBoard(board.id) },
                        onLongPress = { actionsFor = board }
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
    onLongPress: () -> Unit
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
                        .data("file://${board.thumbnailPath}")
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

        // Тёмная подложка снизу + название/категория поверх — читаемо
        // и на пустой доске (без превью), и поверх светлого фото.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(board.title, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(board.category, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
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
