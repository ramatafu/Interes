package com.interes.shared.db

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class BoardQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllBoards(mapper: (
    id: Long,
    title: String,
    category: String,
    createdAt: Long,
  ) -> T): Query<T> = Query(154_130_082, arrayOf("Board"), driver, "Board.sq", "selectAllBoards", "SELECT Board.id, Board.title, Board.category, Board.createdAt FROM Board ORDER BY createdAt DESC") { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!
    )
  }

  public fun selectAllBoards(): Query<Board> = selectAllBoards(::Board)

  public fun <T : Any> selectBoardById(id: Long, mapper: (
    id: Long,
    title: String,
    category: String,
    createdAt: Long,
  ) -> T): Query<T> = SelectBoardByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!
    )
  }

  public fun selectBoardById(id: Long): Query<Board> = selectBoardById(id, ::Board)

  public fun <T : Any> selectBoardsByCategory(category: String, mapper: (
    id: Long,
    title: String,
    category: String,
    createdAt: Long,
  ) -> T): Query<T> = SelectBoardsByCategoryQuery(category) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!
    )
  }

  public fun selectBoardsByCategory(category: String): Query<Board> = selectBoardsByCategory(category, ::Board)

  public fun <T : Any> selectBoardsWithThumbnail(mapper: (
    id: Long,
    title: String,
    category: String,
    createdAt: Long,
    thumbnailPath: String?,
  ) -> T): Query<T> = Query(-1_924_215_859, arrayOf("Board", "Photo"), driver, "Board.sq", "selectBoardsWithThumbnail", """
  |SELECT Board.id, Board.title, Board.category, Board.createdAt, Photo.filePath AS thumbnailPath
  |FROM Board
  |LEFT JOIN Photo ON Photo.boardId = Board.id
  |    AND Photo.orderIndex = (SELECT MIN(p2.orderIndex) FROM Photo p2 WHERE p2.boardId = Board.id)
  |ORDER BY Board.createdAt DESC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)
    )
  }

  public fun selectBoardsWithThumbnail(): Query<SelectBoardsWithThumbnail> = selectBoardsWithThumbnail(::SelectBoardsWithThumbnail)

  public fun lastInsertRowId(): ExecutableQuery<Long> = Query(-601_964_074, driver, "Board.sq", "lastInsertRowId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  /**
   * @return The number of rows updated.
   */
  public fun insertBoard(
    title: String,
    category: String,
    createdAt: Long,
  ): QueryResult<Long> {
    val result = driver.execute(1_337_997_469, """INSERT INTO Board (title, category, createdAt) VALUES (?, ?, ?)""", 3) {
          var parameterIndex = 0
          bindString(parameterIndex++, title)
          bindString(parameterIndex++, category)
          bindLong(parameterIndex++, createdAt)
        }
    notifyQueries(1_337_997_469) { emit ->
      emit("Board")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun deleteBoard(id: Long): QueryResult<Long> {
    val result = driver.execute(319_323_371, """DELETE FROM Board WHERE id = ?""", 1) {
          var parameterIndex = 0
          bindLong(parameterIndex++, id)
        }
    notifyQueries(319_323_371) { emit ->
      emit("Board")
      emit("Photo")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun updateBoardTitle(title: String, id: Long): QueryResult<Long> {
    val result = driver.execute(-824_445_493, """UPDATE Board SET title = ? WHERE id = ?""", 2) {
          var parameterIndex = 0
          bindString(parameterIndex++, title)
          bindLong(parameterIndex++, id)
        }
    notifyQueries(-824_445_493) { emit ->
      emit("Board")
    }
    return result
  }

  private inner class SelectBoardByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Board", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Board", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(-529_343_700, """SELECT Board.id, Board.title, Board.category, Board.createdAt FROM Board WHERE id = ?""", mapper, 1) {
      var parameterIndex = 0
      bindLong(parameterIndex++, id)
    }

    override fun toString(): String = "Board.sq:selectBoardById"
  }

  private inner class SelectBoardsByCategoryQuery<out T : Any>(
    public val category: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Board", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Board", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(-618_672_434, """SELECT Board.id, Board.title, Board.category, Board.createdAt FROM Board WHERE category = ? ORDER BY createdAt DESC""", mapper, 1) {
      var parameterIndex = 0
      bindString(parameterIndex++, category)
    }

    override fun toString(): String = "Board.sq:selectBoardsByCategory"
  }
}
