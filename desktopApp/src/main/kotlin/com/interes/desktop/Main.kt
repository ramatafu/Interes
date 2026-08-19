package com.interes.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.interes.shared.db.InteresDatabase
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.DatabaseDriverFactory
import com.interes.shared.storage.PhotoFileStorage
import com.interes.shared.ui.InteresRoot
import kotlinx.coroutines.Dispatchers

/**
 * ВРЕМЕННО упрощено до обычного декорированного окна (было undecorated +
 * transparent + своя шторка заголовка с WindowDraggableArea). Причина:
 * desktopApp (обычный kotlin("jvm") модуль) и :shared (Kotlin Multiplatform
 * модуль) в этом проекте резолвят зависимости Compose РАЗНЫМИ путями — на
 * практике это приводило к тому, что на classpath одновременно оказывались
 * две разные версии compose-foundation-desktop/compose-runtime-desktop
 * (см. историю правок в shared/build.gradle.kts). Код, использующий только
 * то же самое подмножество Compose API, что и сам :shared (Row/Column/Box/
 * Text/MaterialTheme — всё это уже успешно компилируется внутри :shared),
 * собирается без проблем. А вот WindowDraggableArea и Modifier.weight()
 * ИМЕННО здесь, в desktopApp/Main.kt, стабильно ловили "Unresolved
 * reference" / "internal in file" — оба симптома несовпадения версий.
 *
 * Кастомную прозрачную рамку с своей шторкой заголовка (см. историю в git —
 * AppTitleBar, undecorated/transparent Window) стоит вернуть отдельным
 * шагом, когда база уже собирается и запускается стабильно.
 */
fun main() = application {
    val driver = DatabaseDriverFactory().createDriver()
    val db = InteresDatabase(driver)
    val repository = BoardRepository(db, PhotoFileStorage(), Dispatchers.IO)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Interes"
    ) {
        InteresRoot(repository)
    }
}
