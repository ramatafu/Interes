package com.interes.shared.db.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.interes.shared.db.BoardQueries
import com.interes.shared.db.InteresDatabase
import com.interes.shared.db.PhotoQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<InteresDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = InteresDatabaseImpl.Schema

internal fun KClass<InteresDatabase>.newInstance(driver: SqlDriver): InteresDatabase = InteresDatabaseImpl(driver)

private class InteresDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver),
    InteresDatabase {
  override val boardQueries: BoardQueries = BoardQueries(driver)

  override val photoQueries: PhotoQueries = PhotoQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE Board (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    title TEXT NOT NULL,
          |    category TEXT NOT NULL,
          |    createdAt INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Photo (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    boardId INTEGER NOT NULL,
          |    filePath TEXT NOT NULL,
          |    orderIndex INTEGER NOT NULL,
          |    width INTEGER NOT NULL,
          |    height INTEGER NOT NULL,
          |    addedAt INTEGER NOT NULL,
          |    FOREIGN KEY (boardId) REFERENCES Board(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, "CREATE INDEX photo_board_order ON Photo(boardId, orderIndex)", 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
