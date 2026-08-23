package com.interes.shared.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.interes.shared.db.InteresDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val appDataDir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Interes")
        if (!appDataDir.exists()) appDataDir.mkdirs()
        val dbFile = File(appDataDir, "interes.db")

        // ВАЖНО: флаг "это первый запуск" фиксируем ДО создания драйвера и
        // ДО любых driver.execute(...). Само открытие JDBC-соединения по
        // "jdbc:sqlite:..." уже создаёт физический (пустой) файл на диске
        // как побочный эффект — если проверять dbFile.exists() ПОСЛЕ этого
        // (как было раньше), файл уже будет "существовать" даже на первом
        // запуске, Schema.create() никогда не вызовется, и приложение упадёт
        // на первом же запросе к БД с "no such table: Board".
        val isFreshDatabase = !dbFile.exists()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        driver.execute(null, "PRAGMA foreign_keys=ON;", 0)

        if (isFreshDatabase) {
            InteresDatabase.Schema.create(driver)
        } else {
            // У пользователя уже есть база с прошлой версии приложения — в
            // ней нет новой колонки deletedAt (добавлена для Корзины, см.
            // Board.sq). В проекте не настроены версионные .sqm-миграции
            // SQLDelight — ALTER TABLE тут самый простой надёжный способ
            // довести старую БД до актуальной схемы. runCatching — при
            // повторных запусках колонка уже будет на месте, и ALTER TABLE
            // ожидаемо упадёт с "duplicate column name": это не ошибка.
            runCatching { driver.execute(null, "ALTER TABLE Board ADD COLUMN deletedAt INTEGER;", 0) }
        }
        return driver
    }
}
