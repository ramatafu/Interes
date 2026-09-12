package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.interes.shared.model.Board
import com.interes.shared.repository.BoardRepository
import kotlinx.coroutines.launch

/**
 * Корзина — доски, удалённые из списка ("⋮ → Удалить" в BoardsListScreen),
 * но ещё не стёртые окончательно (см. BoardRepository.softDeleteBoard /
 * permanentlyDeleteBoard). Открывается кнопкой в SideToolbar.
 *
 * Два тулбара, оба самописные (без Scaffold/TopAppBar — та же причина, что
 * и в BoardsListScreen.kt: TopAppBar сам растягивает/центрирует своё
 * содержимое, а тут нужен точный контроль над положением стрелки "Назад" и
 * заголовка). Верстаются НЕ через Column+weight(1f) — в этом проекте
 * Modifier.weight() недоступен из-за конфликта версий compose-foundation на
 * класспасе (см. тот же обход в AppRoot.kt/SideToolbar.kt: "Cannot access
 * 'val RowColumnParentData?.weight: Float': it is internal in file"),
 * поэтому вместо этого — оверлей в Box:
 * - "остальной" тулбар рисуется ПЕРВЫМ, во весь экран (fillMaxSize()), с
 *   отступом сверху ровно на высоту верхней панели (padding(top =
 *   TopToolbarHeight)) — это и даёт ему все оставшееся место без weight;
 * - верхний тулбар рисуется ВТОРЫМ, поверх него, прижатый к верху
 *   (align(Alignment.TopStart)) и фиксированной высоты TopToolbarHeight —
 *   стрелка "←" и "Корзина" в нём прижаты к левому краю (Arrangement.Start,
 *   без Spacer/weight на заголовке, иначе он бы уехал к центру; у самой
 *   стрелки к тому же срезан лишний дефолтный отступ IconButton — см.
 *   комментарий у неё ниже).
 */
@Composable
fun TrashScreen(
    trashedBoards: List<Board>,
    repository: BoardRepository,
    onBack: () -> Unit,
    nativeWindowController: NativeWindowController
) {
    val scope = rememberCoroutineScope()
    var confirmingDeleteBoard by remember { mutableStateOf<Board?>(null) }
    val language = LocalAppLanguage.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Остальной тулбар — весь экран, с отступом сверху под верхнюю панель.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = TopToolbarHeight)
                .background(SideToolbarColor)
        ) {
            if (trashedBoards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(language.trashIsEmpty(), color = Color.White)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                Text(language.restore())
                            }
                            TextButton(onClick = { confirmingDeleteBoard = board }) {
                                Text(language.deletePermanently(), color = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        // Верхний тулбар — стрелка "Назад" и "Корзина" у самого левого края,
        // без заголовка по центру. Рисуется ПОСЛЕ "остального" тулбара —
        // поверх него, а не перед ним, чтобы визуально лечь сверху.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopToolbarHeight)
                .background(TopToolbarColor)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // size(40.dp) вместо дефолтных 48.dp у IconButton — тот же
            // приём, что и у компактных кнопок тулбара (TopBarGlyph в
            // TopBarIcons.kt): меньше лишнего поля вокруг глифа, стрелка
            // оказывается заметно ближе к истинному левому краю.
            // color = Color.White — по умолчанию Text брал бы тёмный
            // onSurface из темы (AppTheme.kt), почти нечитаемый на тёмно-
            // синем фоне TopToolbarColor.
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Text("\u2190", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            // windowDragHandle — см. тот же приём в BoardsListScreen.kt.
            Text(
                language.trash(),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.windowDragHandle(nativeWindowController)
            )
        }
    }

    confirmingDeleteBoard?.let { board ->
        AlertDialog(
            onDismissRequest = { confirmingDeleteBoard = null },
            title = { Text(language.deletePermanentlyTitle()) },
            text = { Text(language.deletePermanentlyText(board.title)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.permanentlyDeleteBoard(board.id) }
                    confirmingDeleteBoard = null
                }) { Text(language.deletePermanently(), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDeleteBoard = null }) { Text(language.cancel()) }
            }
        )
    }
}
