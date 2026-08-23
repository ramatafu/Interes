package com.interes.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.interes.shared.db.InteresDatabase
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.BackupPaths
import com.interes.shared.storage.DatabaseDriverFactory
import com.interes.shared.storage.PhotoFileStorage
import com.interes.shared.ui.InteresRoot
import com.interes.shared.ui.NativeWindowController
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    private val repository: BoardRepository by lazy {
        val driver = DatabaseDriverFactory(applicationContext).createDriver()
        val db = InteresDatabase(driver)
        BoardRepository(db, PhotoFileStorage(applicationContext), Dispatchers.IO)
    }
    private val backupPaths: BackupPaths by lazy { BackupPaths(applicationContext) }
    private val nativeWindowController = NativeWindowController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // onExitApp: на Android закрытие приложения кнопкой в UI —
            // моветон (для этого есть системная кнопка "Назад"/жест), но
            // сигнатура InteresRoot общая для обеих платформ — finish()
            // здесь используется только если пользователь всё же явно
            // нажмёт кнопку "✕" в списке досок (см. BoardsListScreen.kt).
            InteresRoot(repository, backupPaths, nativeWindowController, onExitApp = ::finish)
        }
    }
}
