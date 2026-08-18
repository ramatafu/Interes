package com.interes.shared.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.interes.shared.model.Photo
import com.interes.shared.repository.BoardRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    boardId: Long,
    boardTitle: String,
    repository: BoardRepository,
    onBack: () -> Unit,
    onPhotoClick: (photos: List<Photo>, index: Int) -> Unit
) {
    val photos by repository.observePhotos(boardId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Фото, для которого долгим нажатием попросили меню действий —
    // сейчас единственное действие тут "удалить", поэтому сразу диалог
    // подтверждения, а не промежуточное меню на одну кнопку.
    var photoPendingDelete by remember { mutableStateOf<Photo?>(null) }

    val pickImages = rememberImagePicker { paths ->
        scope.launch {
            // Импортируем последовательно: каждое фото копируется в приватное
            // хранилище (см. PhotoFileStorage) и получает свой orderIndex.
            paths.forEach { path -> repository.addPhotoToBoard(boardId, path) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(boardTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Без material-icons-extended — просто стрелка текстом.
                        Text("\u2190", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            // Без material-icons-extended ради одной иконки — просто "+".
            FloatingActionButton(onClick = pickImages) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        PhotoBoardGrid(
            photos = photos,
            modifier = Modifier.padding(padding),
            onReorder = { orderedIds ->
                scope.launch { repository.reorderPhotos(boardId, orderedIds) }
            },
            onPhotoClick = { photo ->
                // Индекс ищем в ТОМ ЖЕ списке photos, который уходит в
                // PhotoViewerContent — а не в локальном order сетки. Так тап
                // всегда открывает именно то фото, на которое нажали, вне
                // зависимости от того, сколько их в доске и в каком порядке
                // сетка их сейчас визуально показывает.
                val index = photos.indexOfFirst { it.id == photo.id }
                if (index != -1) onPhotoClick(photos, index)
            },
            onLongPress = { photo -> photoPendingDelete = photo }
        )
    }

    photoPendingDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoPendingDelete = null },
            title = { Text("Удалить фото?") },
            text = { Text("Фото будет удалено с доски безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deletePhoto(photo) }
                    photoPendingDelete = null
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { photoPendingDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
