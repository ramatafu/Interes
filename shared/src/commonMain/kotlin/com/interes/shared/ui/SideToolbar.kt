package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.interes.shared.storage.BackupPaths

// Ширина панели — используется и здесь, и в AppRoot.kt (там нужен тот же
// отступ слева у контента, чтобы не уезжать под панель — см.
// Modifier.padding(start = ToolbarWidth) в AppRoot.kt). Цвет фона панели —
// AppPrimaryColor из AppTheme.kt (тот же фирменный #0088CC, что и во всей
// остальной цветовой схеме приложения, не отдельная константа).
val ToolbarWidth: Dp = 72.dp

/**
 * Постоянная левая панель инструментов — заменила собой весь экран
 * "Настройки" (см. AppRoot.kt). Видна поверх любого содержимого приложения:
 * список досок, открытая доска, просмотрщик фото, Корзина — везде.
 *
 * Без material-icons-extended (та же причина, что и везде в проекте —
 * см. комментарии в BoardsListScreen.kt/BoardScreen.kt): иконки — обычные
 * Unicode-символы текстом, а не векторная графика.
 */
@Composable
fun SideToolbar(
    modifier: Modifier = Modifier,
    onHome: () -> Unit,
    onCreateBoard: () -> Unit,
    backupPaths: BackupPaths,
    onOpenTrash: () -> Unit
) {
    var backupMenuExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    // Результат последней операции с резервной копией — показываем простым
    // диалогом (не Snackbar: у узкой боковой панели нет своего Scaffold,
    // заводить его тут ради одного сообщения — лишнее усложнение).
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

    Column(
        modifier = modifier
            .width(ToolbarWidth)
            .background(AppPrimaryColor)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        // Arrangement.SpaceBetween вместо Modifier.weight(1f) на спейсере —
        // .weight() (Row/ColumnScope) в этом проекте ловит старый конфликт
        // версий Compose-foundation (та же причина, что и в AppRoot.kt, см.
        // комментарий там). SpaceBetween распределяет свободное место МЕЖДУ
        // двумя дочерними Column (верхняя группа кнопок / нижняя с Корзиной),
        // не используя weight вообще — и даёт ровно тот же визуальный
        // результат: верхняя группа прижата к верху, Корзина — к низу.
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Название приложения — раньше было в заголовке списка досок,
            // перенесено сюда по просьбе (панель видна всегда, а не только
            // на одном экране). Без maxLines/fontSize-подгонки: при 72dp
            // ширины и стандартном labelSmall слово "Interes" переносится
            // на 2 строки само — это ожидаемо и нормально выглядит.
            Text(
                "Interes",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ToolbarIconButton(symbol = "\u2302", contentDescription = "Домой", onClick = onHome)
            ToolbarIconButton(symbol = "+", contentDescription = "Создать доску", onClick = onCreateBoard)

            Box {
                ToolbarIconButton(symbol = "\uD83D\uDCBE", contentDescription = "Резервная копия", onClick = { backupMenuExpanded = true })
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

            ToolbarIconButton(symbol = "\u2139", contentDescription = "О программе", onClick = { showInfoDialog = true })
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            ToolbarIconButton(symbol = "\uD83D\uDDD1", contentDescription = "Корзина", onClick = onOpenTrash)
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Interes") },
            text = {
                Column {
                    // ВАЖНО: версия захардкожена и должна вручную совпадать
                    // с packageVersion в desktopApp/build.gradle.kts —
                    // единого источника версии на весь проект сейчас нет.
                    Text("Версия 0.1.0", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Приложение для досок визуализации: собирайте и организуйте фотографии по темам и категориям.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        // Стандартное короткое уведомление о GPLv3 (то, что
                        // FSF рекомендует показывать в диалогах "О
                        // программе") — не полный текст лицензии на
                        // несколько тысяч слов, для него на это отдельная
                        // ссылка ниже.
                        "Лицензия: GNU General Public License v3.0 (GPLv3). " +
                            "Это свободное ПО: вы можете распространять и/или изменять его на " +
                            "условиях GPLv3. Полный текст лицензии: " +
                            "https://www.gnu.org/licenses/gpl-3.0.html",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        // Просто текст, не кликабельная ссылка — открытие
                        // браузера по клику потребовало бы ещё один
                        // expect/actual только ради этого; текст можно
                        // скопировать вручную.
                        "GitHub: https://github.com/ramatafu/Interes",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
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

@Composable
private fun ToolbarIconButton(symbol: String, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).padding(vertical = 2.dp)) {
        Text(
            symbol,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}
