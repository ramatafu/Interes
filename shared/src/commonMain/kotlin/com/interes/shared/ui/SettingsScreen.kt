package com.interes.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interes.shared.model.AppSettings
import com.interes.shared.model.AppTheme
import com.interes.shared.storage.BackupPaths
import kotlin.math.roundToInt

/**
 * Раздел "Настройки" — открывается с главного экрана (значок шестерёнки в
 * тулбаре BoardsListScreen). Все четыре пункта из ТЗ — по одной карточке.
 *
 * Тему и прозрачность окна применяем СРАЗУ, без отдельной кнопки
 * "Сохранить" — каждое изменение немедленно уходит в SettingsStorage
 * (persist на диск) И немедленно отражается на экране (AppRoot.kt читает
 * тот же settings-поток реактивно). Это и есть "применяются без
 * перезапуска" из ТЗ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    backupPaths: BackupPaths,
    onBack: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onOpacityChange: (Int) -> Unit,
    nativeWindowController: NativeWindowController
) {
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupIsError by remember { mutableStateOf(false) }

    val createBackup = rememberBackupCreator(backupPaths) { result ->
        backupIsError = result.isFailure
        backupMessage = result.fold(
            onSuccess = { "Резервная копия успешно создана." },
            onFailure = { "Не удалось создать копию: ${it.message ?: "неизвестная ошибка"}" }
        )
    }
    val restoreBackup = rememberBackupRestorer(backupPaths) { result ->
        backupIsError = result.isFailure
        backupMessage = result.fold(
            onSuccess = { "Восстановлено. Перезапустите Interes, чтобы изменения вступили в силу." },
            onFailure = { "Не удалось восстановить: ${it.message ?: "неизвестная ошибка"}" }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // windowDragHandle — см. тот же приём в BoardsListScreen.kt.
                    Text("Настройки", modifier = Modifier.windowDragHandle(nativeWindowController))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Тема оформления
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Тема оформления", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (settings.theme == AppTheme.DARK) "Тёмная" else "Светлая")
                        Switch(
                            checked = settings.theme == AppTheme.DARK,
                            onCheckedChange = { checked -> onThemeChange(if (checked) AppTheme.DARK else AppTheme.LIGHT) }
                        )
                    }
                }
            }

            // 2. Прозрачность окна
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Прозрачность окна", style = MaterialTheme.typography.titleMedium)
                        Text("${settings.windowOpacityPercent}%", style = MaterialTheme.typography.titleMedium)
                    }
                    Slider(
                        value = settings.windowOpacityPercent.toFloat(),
                        onValueChange = { onOpacityChange(it.roundToInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // 3. Резервное копирование
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Резервное копирование", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Сохраняет базу данных и все фото со всех досок в один ZIP-файл, из которого их можно потом восстановить.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = createBackup) { Text("Создать копию") }
                        OutlinedButton(onClick = restoreBackup) { Text("Восстановить") }
                    }
                    backupMessage?.let { message ->
                        Text(
                            message,
                            color = if (backupIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 4. О программе
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("О программе", style = MaterialTheme.typography.titleMedium)
                    Text("Interes", style = MaterialTheme.typography.titleLarge)
                    // ВАЖНО: версия захардкожена и должна вручную совпадать с
                    // packageVersion в desktopApp/build.gradle.kts — единого
                    // источника версии на весь проект сейчас нет, заводить
                    // его ради одной строки в UI — лишнее усложнение.
                    Text("Версия 0.1.0", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Приложение для досок визуализации: собирайте и организуйте фотографии по темам и категориям.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Личный проект, без формальной лицензии.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
