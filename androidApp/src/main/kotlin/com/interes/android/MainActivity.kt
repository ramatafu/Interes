package com.interes.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.interes.shared.db.InteresDatabase
import com.interes.shared.repository.BoardRepository
import com.interes.shared.storage.DatabaseDriverFactory
import com.interes.shared.storage.PhotoFileStorage
import com.interes.shared.ui.InteresRoot
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    private val repository: BoardRepository by lazy {
        val driver = DatabaseDriverFactory(applicationContext).createDriver()
        val db = InteresDatabase(driver)
        BoardRepository(db, PhotoFileStorage(applicationContext), Dispatchers.IO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InteresRoot(repository)
        }
    }
}
