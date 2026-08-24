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
    onPrevPhoto: (() -> Unit)? = null
) {
    var backupMenuExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
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

    Box(
        modifier = modifier
            .width(ToolbarWidth)
            .fillMaxHeight()
            .background(SideToolbarColor)
    ) {
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
            ToolbarIconButton(contentDescription = "Домой", onClick = onHome) { HomeGlyph() }
            ToolbarIconButton(contentDescription = "Создать доску", onClick = onCreateBoard) { PlusGlyph() }

            Box {
                ToolbarIconButton(contentDescription = "Резервная копия", onClick = { backupMenuExpanded = true }) { BackupGlyph() }
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

            ToolbarIconButton(contentDescription = "О программе", onClick = { showInfoDialog = true }) { InfoGlyph() }
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

        // Нижняя группа — прижата к низу.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            ToolbarIconButton(contentDescription = "Корзина", onClick = onOpenTrash) { TrashGlyph() }
        }
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
 * Кнопка левой панели — КРУПНЕЕ, чем раньше: 64 dp вместо 48 dp
 * и шрифт headlineLarge вместо headlineSmall.
 */
@Composable
private fun ToolbarIconButton(contentDescription: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(64.dp).padding(vertical = 2.dp)) {
        content()
    }
}