package com.interes.shared.storage

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.interes.shared.db.InteresDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(InteresDatabase.Schema, context, "interes.db")
        // SQLite по умолчанию игнорирует внешние ключи, если явно не
        // включить — без этого удаление доски НЕ удалит её фото каскадно,
        // несмотря на "ON DELETE CASCADE" в схеме Photo.sq.
        driver.execute(null, "PRAGMA foreign_keys=ON;", 0)
        return driver
    }
}
