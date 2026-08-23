package com.interes.shared.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
    onPhotoClick: (photos: List<Photo>, index: Int) -> Unit,
    nativeWindowController: NativeWindowController
) {
    // remember(boardId) — та же причина, что и в AppRoot.kt: без него
    // каждая перерисовка BoardScreen (а она перерисовывается именно когда
    // приходят новые фото — читает photos) создавала бы Flow заново,
    // collectAsState сбрасывал бы список на пустой и начинал сбор с нуля,
    // и экран никогда не успевал показать только что добавленное фото —
    // хотя в БД оно уже лежало (см. лог: "[Interes] добавлено: ...").
    val photosFlow = remember(boardId) { repository.observePhotos(boardId) }
    val photos by photosFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Фото, для которого долгим нажатием попросили меню действий —
    // сейчас единственное действие тут "удалить", поэтому сразу диалог
    // подтверждения, а не промежуточное меню на одну кнопку.
    var photoPendingDelete by remember { mutableStateOf<Photo?>(null) }

    val pickImages = rememberImagePicker { paths ->
        scope.launch {
            // Импортируем последовательно, но КАЖДЫЙ файл — в своём
            // try/catch. Раньше исключение при импорте одного файла
            // (неподдерживаемый формат, битый файл и т.п.) вылетало из
            // forEach необработанным и молча обрывало ВЕСЬ пакет — то есть
            // из 10 выбранных фото не добавлялось ни одного, без единого
            // сообщения пользователю. e.printStackTrace() — чтобы при
            // реальном сбое текст ошибки был виден в консоли
            // (console = true в desktopApp/build.gradle.kts), а не терялся.
            var failed = 0
            paths.forEach { path ->
                try {
                    repository.addPhotoToBoard(boardId, path)
                } catch (e: Exception) {
                    failed++
                    e.printStackTrace()
                }
            }
            if (failed > 0) {
                val word = if (failed == 1) "файл" else "файла"
                snackbarHostState.showSnackbar("Не удалось добавить $failed $word — подробности в консоли")
            }
        }
    }

    Scaffold(
        // Фон экрана целиком (позади сетки фото внутри доски) — тот же
        // цвет, что и у тулбаров: 92B1B7.
        containerColor = SideToolbarColor,
        topBar = {
            TopAppBar(
                modifier = Modifier.height(TopToolbarHeight),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TopToolbarColor),
                title = {
                    // windowDragHandle — см. тот же приём в BoardsListScreen.kt.
                    Text(boardTitle, modifier = Modifier.windowDragHandle(nativeWindowController))
                },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
