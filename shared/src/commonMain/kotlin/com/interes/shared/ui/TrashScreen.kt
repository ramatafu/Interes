package com.interes.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.interes.shared.model.Board
import com.interes.shared.repository.BoardRepository
import kotlinx.coroutines.launch

/**
 * Корзина — доски, удалённые из списка ("⋮ → Удалить" в BoardsListScreen),
 * но ещё не стёртые окончательно (см. BoardRepository.softDeleteBoard /
 * permanentlyDeleteBoard). Открывается кнопкой в SideToolbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashedBoards: List<Board>,
    repository: BoardRepository,
    onBack: () -> Unit,
    nativeWindowController: NativeWindowController
) {
    val scope = rememberCoroutineScope()
    var confirmingDeleteBoard by remember { mutableStateOf<Board?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // windowDragHandle — см. тот же приём в BoardsListScreen.kt.
                    Text("Корзина", modifier = Modifier.windowDragHandle(nativeWindowController))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        if (trashedBoards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Корзина пуста")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(trashedBoards, key = { it.id }) { board ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Без Modifier.weight(1f) — та же причина, что в
                        // AppRoot.kt/SideToolbar.kt (конфликт версий
                        // Compose-foundation). Row уже с
                        // Arrangement.SpaceBetween — этого достаточно, чтобы
                        // прижать кнопки вправо, растягивать саму колонку с
                        // текстом не обязательно, разница не видна (текст
                        // всё равно выровнен по левому краю).
                        Column {
                            Text(board.title, style = MaterialTheme.typography.titleMedium)
                            Text(board.category, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { scope.launch { repository.restoreBoard(board.id) } }) {
                            Text("Восстановить")
                        }
                        TextButton(onClick = { confirmingDeleteBoard = board }) {
                            Text("Удалить навсегда", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    confirmingDeleteBoard?.let { board ->
        AlertDialog(
            onDismissRequest = { confirmingDeleteBoard = null },
            title = { Text("Удалить навсегда?") },
            text = { Text("Доска \"${board.title}\" и все фото на ней будут удалены безвозвратно. Отменить это будет нельзя.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.permanentlyDeleteBoard(board.id) }
                    confirmingDeleteBoard = null
                }) { Text("Удалить навсегда", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDeleteBoard = null }) { Text("Отмена") }
            }
        )
    }
}
