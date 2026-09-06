package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.interes.shared.storage.BackupPaths

val ToolbarWidth: Dp = 72.dp

@Composable
fun SideToolbar(
    modifier: Modifier = Modifier,
    onHome: () -> Unit,
    onCreateBoard: () -> Unit,
    backupPaths: BackupPaths,
    onOpenTrash: () -> Unit,
    // Стрелка "предыдущее фото" — видна только когда можно листнуть влево.
    onPrevPhoto: (() -> Unit)? = null,
    // false на Android — там "Домой/Создать доску/Резервная копия/О
    // программе" рисуются в верхнем тулбаре (см. AppRoot.kt,
    // PrimaryActionButtons ниже и NativeWindowController
    // .primaryActionsInTopBar). Корзина (и стрелка "предыдущее фото") при
    // этом остаются здесь — они НЕ переезжают ни на одной платформе.
    showPrimaryActions: Boolean = true,
    // Ширина самой колонки — берётся из nativeWindowController
    // .sideToolbarWidth (AppRoot.kt). По умолчанию ToolbarWidth (72.dp) —
    // это ровно то, что было раньше, если кто-то вызовет без этого
    // параметра.
    width: Dp = ToolbarWidth,
    // true на Android — кнопка "Корзина" внизу сжата до 40.dp (TopBarGlyph),
    // а не 64.dp (ToolbarIconButton): при width = 56.dp (Android) 64.dp
    // кнопка попросту не поместилась бы в колонку. На Desktop (width =
    // 72.dp, compact = false по умолчанию) кнопка остаётся прежнего
    // размера — там места достаточно.
    compact: Boolean = false
) {
    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(ToolbarBackgroundColor)
    ) {
        if (showPrimaryActions) {
            // Верхняя группа — опущена на 110 dp от верха,
            // расстояние между кнопками +15 dp (spacedBy(15.dp)).
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .offset(y = 110.dp)
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                PrimaryActionButtons(
                    onHome = onHome,
                    onCreateBoard = onCreateBoard,
                    backupPaths = backupPaths,
                    compact = false
                )
            }
        }

        // Стрелка "предыдущее фото" — ТОЧНО по центру высоты окна.
        if (onPrevPhoto != null) {
            IconButton(
                onClick = onPrevPhoto,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            ) {
                ChevronLeftGlyph()
            }
        }

        // Нижняя группа — прижата к низу. Корзина остаётся здесь ВСЕГДА,
        // независимо от showPrimaryActions (см. её doc-комментарий выше).
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            if (compact) {
                TopBarGlyph(onClick = onOpenTrash) { TrashGlyph() }
            } else {
                ToolbarIconButton(contentDescription = "Корзина", onClick = onOpenTrash) { TrashGlyph() }
            }
        }
    }
}

/**
 * Группа кнопок "Домой / Создать доску / Резервная копия / О программе" —
 * вынесена отдельным composable, потому что рисуется в ДВУХ разных местах
 * на разных платформах:
 * - Desktop: внутри SideToolbar выше, вертикальной Column, кнопками
 *   64.dp (ToolbarIconButton) — compact = false;
 * - Android: в верхнем тулбаре (AppRoot.kt), горизонтальным Row, компактными
 *   глифами 40.dp (TopBarGlyph, тот же стиль, что у значка поиска рядом) —
 *   compact = true.
 *
 * includeInfo — включает ли эта группа ещё и кнопку "О программе" (см.
 * AboutButton ниже). По умолчанию true — это сохраняет Desktop БЕЗ
 * изменений (там "О программе" как была четвёртой кнопкой в этой же
 * колонке, так и осталась). На Android теперь false — "О программе"
 * переехала отдельно в правый тулбар, на уровень корзины (см.
 * RightToolbar.kt, AboutButton вызывается там напрямую), поэтому здесь,
 * в верхнем тулбаре, её быть не должно — иначе была бы в двух местах сразу.
 *
 * Сама функция не оборачивает кнопки в Row/Column — это делает вызывающая
 * сторона (см. выше и AppRoot.kt), поэтому её можно вставить и в
 * вертикальный, и в горизонтальный контейнер без изменений.
 */
@Composable
fun PrimaryActionButtons(
    onHome: () -> Unit,
    onCreateBoard: () -> Unit,
    backupPaths: BackupPaths,
    compact: Boolean,
    includeInfo: Boolean = true
) {
    var backupMenuExpanded by remember { mutableStateOf(false) }
    var backupResultMessage by remember { mutableStateOf<String?>(null) }
    var backupResultIsError by remember { mutableStateOf(false) }

    val createBackup = rememberBackupCreator(backupPaths) { result ->
        backupResultIsError = result.isFailure
        backupResultMessage = result.fold(
            onSuccess = { "Резервная копия успешно создана." },
            onFailure = { "Не удалось создать копию: ${it.message ?: "неизвестная ошибка"}" }
        )
    }
    val restoreBackup = rememberBackupRestorer(backupPaths) { result ->
        backupResultIsError = result.isFailure
        backupResultMessage = result.fold(
            onSuccess = { "Восстановлено. Перезапустите Interes, чтобы изменения вступили в силу." },
            onFailure = { "Не удалось восстановить: ${it.message ?: "неизвестная ошибка"}" }
        )
    }

    @Composable
    fun ActionButton(contentDescription: String, onClick: () -> Unit, glyph: @Composable () -> Unit) {
        if (compact) {
            TopBarGlyph(onClick = onClick, content = glyph)
        } else {
            ToolbarIconButton(contentDescription = contentDescription, onClick = onClick, content = glyph)
        }
    }

    ActionButton("Домой", onHome) { HomeGlyph() }
    ActionButton("Создать доску", onCreateBoard) { PlusGlyph() }

    Box {
        ActionButton("Резервная копия", { backupMenuExpanded = true }) { BackupGlyph() }
        DropdownMenu(expanded = backupMenuExpanded, onDismissRequest = { backupMenuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Создать резервную копию") },
                onClick = {
                    backupMenuExpanded = false
                    createBackup()
                }
            )
            DropdownMenuItem(
                text = { Text("Восстановить из резервной копии") },
                onClick = {
                    backupMenuExpanded = false
                    restoreBackup()
                }
            )
        }
    }

    if (includeInfo) {
        AboutButton(compact = compact)
    }

    backupResultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { backupResultMessage = null },
            title = { Text(if (backupResultIsError) "Ошибка" else "Готово") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { backupResultMessage = null }) { Text("ОК") }
            }
        )
    }
}

/**
 * Кнопка "О программе" (глиф "!") + сам диалог с информацией о версии и
 * лицензии. Вынесена ОТДЕЛЬНО от PrimaryActionButtons (Домой/+/Резервная
 * копия), потому что на Android она теперь стоит не рядом с ними, а в
 * правом тулбаре на уровень корзины (см. RightToolbar.kt) — на Desktop же
 * по-прежнему вызывается ИЗНУТРИ PrimaryActionButtons (includeInfo = true
 * по умолчанию), то есть остаётся четвёртой кнопкой в той же колонке, что
 * и раньше.
 */
@Composable
fun AboutButton(compact: Boolean) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (compact) {
        TopBarGlyph(onClick = { showInfoDialog = true }) { InfoGlyph() }
    } else {
        ToolbarIconButton(contentDescription = "О программе", onClick = { showInfoDialog = true }) { InfoGlyph() }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Interes") },
            text = {
                Column {
                    Text("Версия 0.1.0", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Приложение для досок визуализации: собирайте и организуйте фотографии по темам и категориям.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Лицензия: GNU General Public License v3.0 (GPLv3).",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Закрыть") }
            }
        )
    }
}

/**
 * Кнопка левой панели — КРУПНЕЕ, чем раньше: 64 dp вместо 48 dp
 * и шрифт headlineLarge вместо headlineSmall.
 * Не private — переиспользуется из RightToolbar.kt (кнопка "Добавить
 * фото"), чтобы она была оформлена ТОЧНО так же, как значки слева
 * (тот же размер/отступы), а не как отдельный самодельный стиль.
 */
@Composable
fun ToolbarIconButton(contentDescription: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(64.dp).padding(vertical = 2.dp)) {
        content()
    }
}
