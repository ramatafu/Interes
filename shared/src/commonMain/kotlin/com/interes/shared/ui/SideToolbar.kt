package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
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
                ToolbarIconButton(contentDescription = LocalAppLanguage.current.trash(), onClick = onOpenTrash) { TrashGlyph() }
            }
        }
    }
}

/**
 * Группа кнопок "Домой / Создать доску / Резервная копия / О программе" —
 * используется ТОЛЬКО на Desktop теперь (внутри SideToolbar выше,
 * вертикальной Column, кнопками 64.dp — compact = false). На Android эти
 * четыре кнопки разъехались по разным местам (см. AppRoot.kt/
 * BoardsListScreen.kt): "Домой" убрана совсем, "Создать доску" — в нижнюю
 * панель рядом с "О программе", "Резервная копия" — в нижнюю панель рядом
 * с "Корзиной", "О программе" — в правый тулбар. Поэтому саму эту функцию
 * там больше не вызывают — вместо неё используются CreateBoardButton,
 * BackupButton и AboutButton по отдельности.
 *
 * includeInfo — включает ли эта группа ещё и кнопку "О программе"/языковой
 * тумблер. По умолчанию true (Desktop, без изменений).
 *
 * Сама функция не оборачивает кнопки в Row/Column — это делает вызывающая
 * сторона (см. выше), поэтому её можно вставить и в вертикальный, и в
 * горизонтальный контейнер без изменений.
 */
@Composable
fun PrimaryActionButtons(
    onHome: () -> Unit,
    onCreateBoard: () -> Unit,
    backupPaths: BackupPaths,
    compact: Boolean,
    includeInfo: Boolean = true
) {
    if (compact) {
        TopBarGlyph(onClick = onHome) { HomeGlyph() }
    } else {
        ToolbarIconButton(contentDescription = LocalAppLanguage.current.home(), onClick = onHome) { HomeGlyph() }
    }
    CreateBoardButton(onCreateBoard = onCreateBoard, compact = compact)
    BackupButton(backupPaths = backupPaths, compact = compact)
    if (includeInfo) {
        AboutButton(compact = compact)
        LanguageButton(compact = compact)
    }
}

/**
 * Кнопка "Создать доску" (глиф "+"). Вынесена отдельно от
 * PrimaryActionButtons — на Android она теперь стоит не рядом с Домой/
 * Резервной копией, а в нижней панели рядом с "О программе" (см.
 * BoardsListScreen.kt). На Desktop по-прежнему вызывается изнутри
 * PrimaryActionButtons.
 */
@Composable
fun CreateBoardButton(onCreateBoard: () -> Unit, compact: Boolean) {
    if (compact) {
        TopBarGlyph(onClick = onCreateBoard) { PlusGlyph() }
    } else {
        ToolbarIconButton(contentDescription = LocalAppLanguage.current.newBoardAction(), onClick = onCreateBoard) { PlusGlyph() }
    }
}

/**
 * Кнопка "Резервная копия" (глиф) + её выпадающее меню ("Создать" /
 * "Восстановить") и диалог результата — самодостаточна, со своим
 * состоянием, как AboutButton. Вынесена отдельно от PrimaryActionButtons —
 * на Android она теперь стоит не рядом с Домой/Создать доску, а в нижней
 * панели рядом с "Корзиной" (см. BoardsListScreen.kt). На Desktop
 * по-прежнему вызывается изнутри PrimaryActionButtons.
 */
@Composable
fun BackupButton(backupPaths: BackupPaths, compact: Boolean) {
    var backupMenuExpanded by remember { mutableStateOf(false) }
    var backupResultMessage by remember { mutableStateOf<String?>(null) }
    var backupResultIsError by remember { mutableStateOf(false) }
    // true только после УСПЕШНОГО restoreBackup — тогда в диалоге результата
    // рядом с "OK" появляется вторая кнопка "Restart now" (см. ниже),
    // которая реально убивает и перезапускает процесс (rememberAppRestarter
    // в BackupActions.kt), а не просто просит пользователя сделать это
    // руками (см. подробный doc-комментарий там же, почему одной просьбы
    // недостаточно на Android).
    var showRestartAction by remember { mutableStateOf(false) }

    // Язык захватывается ЗДЕСЬ, в теле composable-функции (доступ к
    // LocalAppLanguage.current есть) — onSuccess/onFailure ниже выполняются
    // асинхронно, уже ВНЕ композиции (после закрытия системного диалога
    // выбора файла), там LocalAppLanguage.current уже недоступен. Strings.kt
    // поэтому и написан как extension-функции с явным параметром-получателем
    // AppLanguage, а не @Composable — capturedLanguage работает в обоих
    // контекстах одинаково.
    val capturedLanguage = LocalAppLanguage.current

    val restartApp = rememberAppRestarter()
    val createBackup = rememberBackupCreator(backupPaths) { result ->
        backupResultIsError = result.isFailure
        showRestartAction = false
        backupResultMessage = result.fold(
            onSuccess = { capturedLanguage.backupCreated() },
            onFailure = { capturedLanguage.backupCreateFailed(it.message ?: "unknown error") }
        )
    }
    val restoreBackup = rememberBackupRestorer(backupPaths) { result ->
        backupResultIsError = result.isFailure
        showRestartAction = result.isSuccess
        backupResultMessage = result.fold(
            onSuccess = { capturedLanguage.restoreSucceeded() },
            onFailure = { capturedLanguage.restoreFailed(it.message ?: "unknown error") }
        )
    }

    Box {
        if (compact) {
            TopBarGlyph(onClick = { backupMenuExpanded = true }) { BackupGlyph() }
        } else {
            ToolbarIconButton(contentDescription = LocalAppLanguage.current.backup(), onClick = { backupMenuExpanded = true }) { BackupGlyph() }
        }
        DropdownMenu(expanded = backupMenuExpanded, onDismissRequest = { backupMenuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(LocalAppLanguage.current.createBackup()) },
                onClick = {
                    backupMenuExpanded = false
                    createBackup()
                }
            )
            DropdownMenuItem(
                text = { Text(LocalAppLanguage.current.restoreFromBackup()) },
                onClick = {
                    backupMenuExpanded = false
                    restoreBackup()
                }
            )
        }
    }

    backupResultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { backupResultMessage = null },
            title = { Text(if (backupResultIsError) LocalAppLanguage.current.error() else LocalAppLanguage.current.done()) },
            text = { Text(message) },
            confirmButton = {
                if (showRestartAction) {
                    TextButton(onClick = { restartApp() }) { Text(LocalAppLanguage.current.restartNow()) }
                } else {
                    TextButton(onClick = { backupResultMessage = null }) { Text(LocalAppLanguage.current.ok()) }
                }
            },
            // "OK" (закрыть без перезапуска) — только когда действительно есть
            // выбор, т.е. когда основная confirmButton занята "Restart now".
            dismissButton = {
                if (showRestartAction) {
                    TextButton(onClick = { backupResultMessage = null }) { Text(LocalAppLanguage.current.ok()) }
                }
            }
        )
    }
}

/**
 * Кнопка-тумблер языка интерфейса (RU ⇄ EN) — глиф-глобус (LanguageGlyph в
 * TopBarIcons.kt), подпись под ним показывает язык, НА КОТОРЫЙ переключит
 * клик (см. switchToLanguageLabel в Strings.kt). Живёт рядом с AboutButton
 * везде, где та появляется (PrimaryActionButtons на Desktop, нижняя панель
 * BoardsListScreen.kt и RightToolbar.kt на Android) — та же логика
 * размещения, что и у AboutButton, просто ещё одна кнопка в тех же местах.
 */
@Composable
fun LanguageButton(compact: Boolean) {
    val currentLanguage = LocalAppLanguage.current
    val setLanguage = LocalAppLanguageSetter.current
    val onClick = { setLanguage(currentLanguage.toggled()) }

    if (compact) {
        TopBarGlyph(onClick = onClick) { LanguageGlyph() }
    } else {
        ToolbarIconButton(contentDescription = currentLanguage.switchToLanguageLabel(), onClick = onClick) {
            LanguageGlyph()
        }
    }
}

/**
 * Кнопка "О программе" (глиф "!") — сама кнопка отдельно от диалога
 * (AboutDialog ниже), потому что диалог теперь открывается ещё и с одного
 * места без этой кнопки: с логотипа "In" в верхнем тулбаре главного экрана
 * (см. AppRoot.kt) — там значок-восклицательный-знак больше не нужен
 * отдельно, но сам диалог тот же самый.
 *
 * Кнопка вынесена ОТДЕЛЬНО от PrimaryActionButtons (Домой/+/Резервная
 * копия), потому что на Android она стоит не рядом с ними, а в правом
 * тулбаре на уровне корзины (см. RightToolbar.kt, актуально только для
 * экрана доски — на главном экране Android кнопки уже нет вообще, см.
 * BoardsListScreen.kt) — на Desktop же по-прежнему вызывается ИЗНУТРИ
 * PrimaryActionButtons (includeInfo = true по умолчанию), то есть остаётся
 * четвёртой кнопкой в той же колонке, что и раньше.
 */
@Composable
fun AboutButton(compact: Boolean) {
    var showInfoDialog by remember { mutableStateOf(false) }
    val language = LocalAppLanguage.current

    if (compact) {
        TopBarGlyph(onClick = { showInfoDialog = true }) { InfoGlyph() }
    } else {
        ToolbarIconButton(contentDescription = language.about(), onClick = { showInfoDialog = true }) { InfoGlyph() }
    }

    AboutDialog(show = showInfoDialog, onDismiss = { showInfoDialog = false })
}

/**
 * Сам диалог "О программе" (версия, описание, авторы, ссылка, лицензия) —
 * без кнопки-триггера, чтобы им мог управлять кто угодно (AboutButton выше
 * и логотип "In" в AppRoot.kt) через собственное show-состояние.
 */
@Composable
fun AboutDialog(show: Boolean, onDismiss: () -> Unit) {
    if (!show) return

    val language = LocalAppLanguage.current
    val uriHandler = LocalUriHandler.current
    val repoUrl = "https://github.com/ramatafu/Interes"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Interes") },
        text = {
            Column {
                Text(language.appVersion("0.2.7"), style = MaterialTheme.typography.bodyMedium)
                Text(
                    language.appDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    language.developmentLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    language.creditsIdea(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    language.creditsCode(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    language.creditsConsulting(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // Кликабельная ссылка на репозиторий — LocalUriHandler
                // общий для Compose Multiplatform (свой actual на
                // Android/Desktop), открывает системный браузер. URL не
                // переводится ни в каком языке.
                Text(
                    repoUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { uriHandler.openUri(repoUrl) }
                )
                Text(
                    language.license(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(language.close()) }
        }
    )
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
