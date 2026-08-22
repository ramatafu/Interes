package com.interes.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.interes.shared.model.AppTheme
import com.interes.shared.model.Photo
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import com.interes.shared.storage.SettingsStorage
import kotlin.math.roundToInt

/**
 * Точка входа UI. Простая навигация без внешней библиотеки — вся Interes
 * умещается в четыре "экрана": список досок -> доска -> полноэкранный
 * просмотр, плюс отдельно — настройки, поэтому обычного mutableState для
 * текущего экрана достаточно.
 *
 * Прозрачность применяется здесь, на самом верхнем уровне, ко ВСЕМУ
 * содержимому приложения разом — списку досок/доске И самому просмотрщику
 * вместе, — а не только к просмотрщику поверх доски.
 * - Android: тема Activity объявлена translucent
 *   (androidApp/src/main/res/values/themes.xml) + Compose graphicsLayer.alpha
 *   ниже — вместе дают реальную прозрачность до системного фона.
 * - Windows: окно undecorated (без системной рамки) — обязательное условие
 *   для реальной прозрачности окна через java.awt.Window.opacity, см.
 *   Main.kt и NativeWindowController.desktop.kt (там же — почему именно так,
 *   со ссылкой на официальную документацию Java). Раз рамки нет — драг за
 *   заголовок и закрытие окна реализованы вручную внутри самого приложения
 *   (перетаскивание заголовка в тулбарах экранов через nativeWindowController,
 *   кнопка "✕" в списке досок через onExitApp).
 *
 * ДВА РАЗНЫХ источника прозрачности перемножаются в один процент:
 * - settings.windowOpacityPercent — постоянная база из раздела "Настройки"
 *   (persist между сессиями, см. SettingsStorage).
 * - appOpacityPercent — временный ползунок ВНУТРИ просмотрщика фото,
 *   сбрасывается на 100% при каждом закрытии просмотрщика/смене фото (не
 *   persist — это заведомо разовая, а не постоянная регулировка "на
 *   посмотреть").
 * Настройки-экран (SettingsScreen) и элементы управления просмотрщика
 * специально вынесены СНАРУЖИ этого затухающего слоя.
 *
 * КУДА именно применяется итоговый процент — зависит от платформы, и решает
 * это NativeWindowController.handlesOpacityNatively (см. NativeWindowController.kt):
 * - Desktop (обычно): true — процент уходит в nativeWindowController, которая
 *   двигает java.awt.Window.opacity САМОГО OS-окна. Тогда Compose-слой ниже
 *   держим на alpha=1 и НЕ гасим его повторно — иначе прозрачность
 *   применилась бы дважды (окно+контент), и ползунок вёл бы себя не
 *   пропорционально тому, что показывает.
 * - Android, и Desktop в редком случае отсутствия поддержки оконной
 *   прозрачности у видеокарты: false — тогда как раньше, тем же процентом
 *   двигаем graphicsLayer.alpha контента. На Android это и есть основной,
 *   изначально рабочий механизм (translucent-тема Activity уже даёт
 *   реальную прозрачность до системного фона под Compose-контентом).
 */
@Composable
fun InteresRoot(
    repository: BoardRepository,
    settingsStorage: SettingsStorage,
    backupPaths: BackupPaths,
    nativeWindowController: NativeWindowController,
    onExitApp: () -> Unit
) {
    // remember(repository) — ВАЖНО, а не просто repository.observeX().collectAsState()
    // напрямую. repository.observeBoardSummaries()/observeBoards() строят
    // НОВУЮ цепочку Flow при каждом вызове. Без remember каждая перерисовка
    // InteresRoot (а она перерисовывается именно при получении новых данных
    // из этих же Flow — читает boardSummaries/allBoards) создавала бы
    // Flow-объект заново; collectAsState видит "другой" Flow как смену ключа
    // и перезапускает сбор с нуля (сброс на initial = emptyList()) — из-за
    // этого перерисовка догоняла сама себя быстрее, чем БД успевала вернуть
    // актуальные данные, и список визуально никогда не "устаканивался" на
    // новом состоянии. Ровно это и выглядело как "фото не добавляется":
    // на самом деле фото исправно попадало в БД, но экран крутился в этом
    // цикле сброс-пересбор и не показывал итог.
    val boardSummariesFlow = remember(repository) { repository.observeBoardSummaries() }
    val allBoardsFlow = remember(repository) { repository.observeBoards() }
    val boardSummaries by boardSummariesFlow.collectAsState(initial = emptyList())
    val allBoards by allBoardsFlow.collectAsState(initial = emptyList())

    // StateFlow уже содержит текущее значение синхронно (см. SettingsStorage) —
    // initial = ...value, а не пустое/дефолтное значение, чтобы не было
    // видимой вспышки светлой темы на долю секунды при старте с сохранённой
    // тёмной.
    val settingsFlow = remember(settingsStorage) { settingsStorage.observeSettings() }
    val settings by settingsFlow.collectAsState(initial = settingsFlow.value)

    // null = показан список досок; иначе id открытой доски.
    var selectedBoardId by remember { mutableStateOf<Long?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    // Открытая полноэкранная галерея: список фото доски + индекс, с которого
    // начать. null — галерея закрыта.
    var viewerState by remember { mutableStateOf<Pair<List<Photo>, Int>?>(null) }

    // Прозрачность ТОЛЬКО просмотрщика (0–100%), управляется ползунком в
    // PhotoViewerControls. Сбрасывается на 100%, когда закрывается
    // просмотрщик (иначе список/доска рисковали бы остаться прозрачными)
    // и при пролистывании на другое фото (та же логика, что и у зума —
    // каждое фото начинается "с чистого листа"). Постоянная база берётся
    // отдельно из settings.windowOpacityPercent — см. doc-комментарий выше.
    var appOpacityPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(viewerState == null) {
        if (viewerState == null) appOpacityPercent = 100f
    }

    // Порядок важен: если открыты настройки — "Назад" сначала закрывает их;
    // иначе если открыт просмотрщик — закрывает его, а не выкидывает сразу
    // из доски; иначе, если открыта доска — "Назад" возвращает к списку досок.
    PlatformBackHandler(enabled = showSettings) { showSettings = false }
    PlatformBackHandler(enabled = !showSettings && viewerState != null) { viewerState = null }
    PlatformBackHandler(enabled = !showSettings && viewerState == null && selectedBoardId != null) { selectedBoardId = null }

    val colorScheme = if (settings.theme == AppTheme.DARK) darkColorScheme() else lightColorScheme()

    // Итоговый процент — произведение двух источников (см. doc-комментарий
    // класса), в диапазоне 0..100.
    val combinedOpacityPercent = (appOpacityPercent / 100f) * (settings.windowOpacityPercent / 100f) * 100f

    LaunchedEffect(combinedOpacityPercent, nativeWindowController) {
        println("[Interes] AppRoot: combinedOpacityPercent=$combinedOpacityPercent, handlesOpacityNatively=${nativeWindowController.handlesOpacityNatively}")
        if (nativeWindowController.handlesOpacityNatively) {
            nativeWindowController.setOpacityPercent(combinedOpacityPercent.roundToInt())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val currentViewerState = viewerState
        val pagerState = if (currentViewerState != null) {
            rememberPagerState(initialPage = currentViewerState.second) { currentViewerState.first.size }
        } else null

        if (pagerState != null) {
            LaunchedEffect(pagerState.currentPage) { appOpacityPercent = 100f }
        }

        // Единый затухающий слой: список досок/доска И сама галерея (без
        // элементов управления, БЕЗ настроек) вместе, одним alpha на
        // графическом слое — но ТОЛЬКО когда прозрачность окна не
        // обрабатывается нативно (см. doc-комментарий класса). Когда
        // NativeWindowController уже красит само OS-окно, тут держим alpha=1,
        // иначе прозрачность применилась бы дважды.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (nativeWindowController.handlesOpacityNatively) 1f else combinedOpacityPercent / 100f
                }
        ) {
            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val boardId = selectedBoardId
                    if (boardId == null) {
                        BoardsListScreen(
                            boards = boardSummaries,
                            repository = repository,
                            onOpenBoard = { id -> selectedBoardId = id },
                            onOpenSettings = { showSettings = true },
                            nativeWindowController = nativeWindowController,
                            onExitApp = onExitApp
                        )
                    } else {
                        // Доску могли удалить (в т.ч. её же длинным нажатием
                        // со списка) или переименовать, пока она открыта —
                        // актуальное название всегда берём из уже
                        // загруженного списка досок.
                        val currentTitle = allBoards.firstOrNull { it.id == boardId }?.title
                        // Доски больше нет (удалена) — возвращаемся к списку.
                        // LaunchedEffect, а не прямая запись в теле composable —
                        // запись состояния во время композиции нежелательна,
                        // даже когда (как здесь) практически не вызывает
                        // видимых проблем.
                        LaunchedEffect(currentTitle) {
                            if (currentTitle == null) selectedBoardId = null
                        }
                        if (currentTitle != null) {
                            BoardScreen(
                                boardId = boardId,
                                boardTitle = currentTitle,
                                repository = repository,
                                onBack = { selectedBoardId = null },
                                onPhotoClick = { photos, index -> viewerState = photos to index },
                                nativeWindowController = nativeWindowController
                            )
                        }
                    }
                }
            }

            if (currentViewerState != null && pagerState != null) {
                PhotoViewerContent(photos = currentViewerState.first, pagerState = pagerState)
            }
        }

        // Элементы управления просмотрщика — СНАРУЖИ затухающего слоя,
        // всегда полностью непрозрачны.
        if (currentViewerState != null && pagerState != null) {
            PhotoViewerControls(
                pageCount = currentViewerState.first.size,
                currentPage = pagerState.currentPage,
                opacityPercent = appOpacityPercent,
                onOpacityChange = { appOpacityPercent = it },
                onDismiss = { viewerState = null }
            )
        }

        // Настройки — тоже СНАРУЖИ затухающего слоя и своя MaterialTheme
        // (та же colorScheme) поверх всего остального, когда открыты.
        if (showSettings) {
            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        settings = settings,
                        backupPaths = backupPaths,
                        onBack = { showSettings = false },
                        onThemeChange = { theme -> settingsStorage.setTheme(theme) },
                        onOpacityChange = { percent -> settingsStorage.setWindowOpacityPercent(percent) },
                        nativeWindowController = nativeWindowController
                    )
                }
            }
        }
    }
}
