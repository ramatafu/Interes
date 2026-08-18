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

public class PhotoQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectPhotosByBoard(boardId: Long, mapper: (
    id: Long,
    boardId: Long,
    filePath: String,
    orderIndex: Long,
    width: Long,
    height: Long,
    addedAt: Long,
  ) -> T): Query<T> = SelectPhotosByBoardQuery(boardId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!
    )
  }

  public fun selectPhotosByBoard(boardId: Long): Query<Photo> = selectPhotosByBoard(boardId, ::Photo)

  public fun <T : Any> selectPhotoById(id: Long, mapper: (
    id: Long,
    boardId: Long,
    filePath: String,
    orderIndex: Long,
    width: Long,
    height: Long,
    addedAt: Long,
  ) -> T): Query<T> = SelectPhotoByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!
    )
  }

  public fun selectPhotoById(id: Long): Query<Photo> = selectPhotoById(id, ::Photo)

  public fun <T : Any> selectMaxOrderIndex(boardId: Long, mapper: (MAX: Long?) -> T): Query<T> = SelectMaxOrderIndexQuery(boardId) { cursor ->
    mapper(
      cursor.getLong(0)
    )
  }

  public fun selectMaxOrderIndex(boardId: Long): Query<SelectMaxOrderIndex> = selectMaxOrderIndex(boardId, ::SelectMaxOrderIndex)

  public fun lastInsertRowId(): ExecutableQuery<Long> = Query(89_110_282, driver, "Photo.sq", "lastInsertRowId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  /**
   * @return The number of rows updated.
   */
  public fun insertPhoto(
    boardId: Long,
    filePath: String,
    orderIndex: Long,
    width: Long,
    height: Long,
    addedAt: Long,
  ): QueryResult<Long> {
    val result = driver.execute(1_850_095_389, """
        |INSERT INTO Photo (boardId, filePath, orderIndex, width, height, addedAt)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          var parameterIndex = 0
          bindLong(parameterIndex++, boardId)
          bindString(parameterIndex++, filePath)
          bindLong(parameterIndex++, orderIndex)
          bindLong(parameterIndex++, width)
          bindLong(parameterIndex++, height)
          bindLong(parameterIndex++, addedAt)
        }
    notifyQueries(1_850_095_389) { emit ->
      emit("Photo")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun updatePhotoOrder(orderIndex: Long, id: Long): QueryResult<Long> {
    val result = driver.execute(1_150_184_513, """UPDATE Photo SET orderIndex = ? WHERE id = ?""", 2) {
          var parameterIndex = 0
          bindLong(parameterIndex++, orderIndex)
          bindLong(parameterIndex++, id)
        }
    notifyQueries(1_150_184_513) { emit ->
      emit("Photo")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun deletePhoto(id: Long): QueryResult<Long> {
    val result = driver.execute(831_421_291, """DELETE FROM Photo WHERE id = ?""", 1) {
          var parameterIndex = 0
          bindLong(parameterIndex++, id)
        }
    notifyQueries(831_421_291) { emit ->
      emit("Photo")
    }
    return result
  }

  private inner class SelectPhotosByBoardQuery<out T : Any>(
    public val boardId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Photo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Photo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(247_016_854, """SELECT Photo.id, Photo.boardId, Photo.filePath, Photo.orderIndex, Photo.width, Photo.height, Photo.addedAt FROM Photo WHERE boardId = ? ORDER BY orderIndex ASC""", mapper, 1) {
      var parameterIndex = 0
      bindLong(parameterIndex++, boardId)
    }

    override fun toString(): String = "Photo.sq:selectPhotosByBoard"
  }

  private inner class SelectPhotoByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Photo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Photo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(919_968_172, """SELECT Photo.id, Photo.boardId, Photo.filePath, Photo.orderIndex, Photo.width, Photo.height, Photo.addedAt FROM Photo WHERE id = ?""", mapper, 1) {
      var parameterIndex = 0
      bindLong(parameterIndex++, id)
    }

    override fun toString(): String = "Photo.sq:selectPhotoById"
  }

  private inner class SelectMaxOrderIndexQuery<out T : Any>(
    public val boardId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Photo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Photo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> = driver.executeQuery(-783_837_392, """SELECT MAX(orderIndex) FROM Photo WHERE boardId = ?""", mapper, 1) {
      var parameterIndex = 0
      bindLong(parameterIndex++, boardId)
    }

    override fun toString(): String = "Photo.sq:selectMaxOrderIndex"
  }
}
