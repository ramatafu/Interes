package com.interes.shared.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    nativeWindowController: NativeWindowController,
    // "Добавить фото" переехала из FloatingActionButton (она "протекала"
    // поверх просмотрщика фото, т.к. Scaffold рисует FAB поверх всего
    // контента доски) на правый тулбар (см. AppRoot.kt/RightToolbar.kt).
    // pickImages создаётся ЗДЕСЬ (rememberImagePicker — платформенный,
    // должен жить в композиции BoardScreen), а наружу отдаётся сама
    // функция-триггер — AppRoot передаёт её в RightToolbar как onAddPhoto.
    onPickImagesReady: (() -> Unit) -> Unit,
    // true на Android, пока не открыт просмотрщик фото (см. hideSideBars в
    // AppRoot.kt) — там RightToolbar с "Добавить фото" не рисуется вовсе, и
    // кнопка добавления фото рисуется прямо здесь, в верхней панели самой
    // доски. false — на Desktop, и на Android во время просмотра фото:
    // там кнопка остаётся в RightToolbar, как и раньше.
    showAddPhotoInTopBar: Boolean = false
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
    // Захвачен здесь (тело composable-функции) — колбэк rememberImagePicker
    // ниже выполняется уже вне композиции (после закрытия системного
    // диалога выбора файлов), там LocalAppLanguage.current недоступен.
    val language = LocalAppLanguage.current

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
                snackbarHostState.showSnackbar(language.addPhotosFailed(failed))
            }
        }
    }

    // Отдаём триггер наверх при каждом изменении boardId/pickImages —
    // именно ДЛЯ ТЕКУЩЕЙ доски, чтобы правый тулбар всегда дёргал
    // addPhotoToBoard(boardId, ...) для той доски, что открыта сейчас,
    // а не для предыдущей.
    LaunchedEffect(boardId, pickImages) {
        onPickImagesReady(pickImages)
    }

    Scaffold(
        // Фон экрана целиком (позади сетки фото внутри доски) — тот же
        // цвет, что и у тулбаров: 92B1B7.
        containerColor = SideToolbarColor,
        topBar = {
            TopAppBar(
                modifier = Modifier.height(TopToolbarHeight),
                // "Свернуть/Развернуть/Закрыть" здесь БОЛЬШЕ НЕ рисуются —
                // эта TopAppBar инсетится по бокам под SideToolbar/RightToolbar
                // (см. padding вокруг контента в AppRoot.kt), поэтому её
                // правый край не совпадает с настоящим краем ОКНА — кнопки
                // оказывались левее, чем на главном экране. Теперь кнопки
                // рисует AppRoot.kt поверх правого угла-заполнителя, у
                // истинного края окна — там же, где и на главном экране.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopToolbarColor,
                    // "Назад" и название доски — белым: раньше брали цвет
                    // по умолчанию из светлой ColorScheme (тёмный, почти
                    // чёрный) и терялись на тёмном фоне панели.
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                ),
                title = {
                    // windowDragHandle — см. тот же приём в BoardsListScreen.kt.
                    Text(boardTitle, modifier = Modifier.windowDragHandle(nativeWindowController))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Без material-icons-extended — просто стрелка текстом.
                        Text("\u2190", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (showAddPhotoInTopBar) {
                        IconButton(onClick = pickImages) {
                            PlusGlyph()
                        }
                    }
                }
            )
        },
        // FloatingActionButton "+" убран — см. onPickImagesReady выше:
        // кнопка добавления фото теперь на правом тулбаре (AppRoot.kt).
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
            title = { Text(language.deletePhotoTitle()) },
            text = { Text(language.deletePhotoText()) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deletePhoto(photo) }
                    photoPendingDelete = null
                }) {
                    Text(language.delete())
                }
            },
            dismissButton = {
                TextButton(onClick = { photoPendingDelete = null }) {
                    Text(language.cancel())
                }
            }
        )
    }
}
