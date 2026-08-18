package com.interes.shared.storage

import app.cash.sqldelight.db.SqlDriver

/**
 * На Android и на Desktop база создаётся по-разному (Context vs файловый путь),
 * поэтому это expect/actual: общий код в shared ничего не знает о платформе.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
