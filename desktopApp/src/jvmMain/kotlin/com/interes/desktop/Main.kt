package com.interes.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDraggableArea
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.interes.shared.db.InteresDatabase
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.DatabaseDriverFactory
import com.interes.shared.storage.PhotoFileStorage
import com.interes.shared.ui.InteresRoot
import kotlinx.coroutines.Dispatchers

private val TITLE_BAR_HEIGHT = 36.dp

/**
 * Главное окно сделано undecorated + transparent намеренно — это плата за
 * реальную прозрачность до уровня рабочего стола Windows (ползунок в
 * просмотрщике фото, см. AppRoot.kt в shared-модуле). У Compose Desktop
 * окно не может на лету переключаться между "обычное" и "прозрачное" —
 * настройка фиксируется один раз при создании, поэтому всё окно приложения
 * такое с самого начала, а не только пока открыт конкретный экран.
 *
 * Раз системной рамки больше нет — сделал свою тонкую панель сверху
 * (заголовок + сворачивание + закрытие), она ВСЕГДА непрозрачна и не
 * участвует в затухании остального контента.
 */
fun main() = application {
    val driver = DatabaseDriverFactory().createDriver()
    val db = InteresDatabase(driver)
    val repository = BoardRepository(db, PhotoFileStorage(), Dispatchers.IO)

    // Явный начальный размер — без него поведение undecorated-окна по
    // умолчанию менее предсказуемо, чем у обычного окна с рамкой.
    val windowState = rememberWindowState(width = 1100.dp, height = 750.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Interes",
        undecorated = true,
        transparent = true,
        // ПРОВЕРИТЬ НА РЕАЛЬНОЙ МАШИНЕ: изменение размера undecorated-окна
        // перетаскиванием за край в Compose Desktop не всегда работает
        // "из коробки" так же надёжно, как у окна с системной рамкой —
        // поведение зависит от версии Compose Multiplatform/ОС. Если
        // потянуть за край не получается, понадобятся свои resize-хендлы
        // по краям (аналогично AppTitleBar ниже, только на drag с изменением
        // windowState.size вместо перемещения) — дайте знать, добавлю.
        resizable = true,
        state = windowState
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTitleBar(
                onMinimize = { windowState.isMinimized = true },
                onClose = ::exitApplication
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                InteresRoot(repository)
            }
        }
    }
}

@Composable
private fun AppTitleBar(onMinimize: () -> Unit, onClose: () -> Unit) {
    MaterialTheme {
        WindowDraggableArea {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TITLE_BAR_HEIGHT)
                    .background(Color(0xFF1A1A2E)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Interes",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp).weight(1f)
                )
                TitleBarButton(symbol = "\u2013", onClick = onMinimize)
                TitleBarButton(symbol = "\u2715", onClick = onClose)
            }
        }
    }
}

@Composable
private fun TitleBarButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TITLE_BAR_HEIGHT)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color.White, fontSize = 14.sp)
    }
}
