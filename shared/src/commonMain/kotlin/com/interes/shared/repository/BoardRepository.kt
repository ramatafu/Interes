package com.interes.shared.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.interes.shared.db.InteresDatabase
import com.interes.shared.model.Board
import com.interes.shared.model.BoardSummary
import com.interes.shared.model.Photo
import com.interes.shared.storage.ImportedPhotoFile
import com.interes.shared.storage.PhotoFileStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BoardRepository(
    private val db: InteresDatabase,
    private val photoFileStorage: PhotoFileStorage,
    private val ioDispatcher: CoroutineDispatcher
) {
    fun observeBoards(): Flow<List<Board>> =
        db.boardQueries.selectAllBoards()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { Board(it.id, it.title, it.category, it.createdAt) } }

    /** Для экрана списка досок: доска + путь к первому фото как превью. */
    fun observeBoardSummaries(): Flow<List<BoardSummary>> =
        db.boardQueries.selectBoardsWithThumbnail()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows ->
                rows.map { BoardSummary(it.id, it.title, it.category, it.createdAt, it.thumbnailPath) }
            }

    fun observePhotos(boardId: Long): Flow<List<Photo>> =
        db.photoQueries.selectPhotosByBoard(boardId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows ->
                rows.map { Photo(it.id, it.boardId, it.filePath, it.orderIndex.toInt(), it.width.toInt(), it.height.toInt(), it.addedAt) }
            }

    suspend fun createBoard(title: String, category: String): Long = withContext(ioDispatcher) {
        db.boardQueries.insertBoard(title, category, currentTimeMillis())
        db.boardQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun renameBoard(boardId: Long, newTitle: String) = withContext(ioDispatcher) {
        db.boardQueries.updateBoardTitle(newTitle, boardId)
    }

    /**
     * Удаляет доску вместе со всеми её фото (ON DELETE CASCADE в схеме
     * Photo — при включённом PRAGMA foreign_keys, см. DatabaseDriverFactory).
     * Файлы фото на диске тоже нужно почистить отдельно — каскад в БД
     * удаляет только строки, не сами файлы.
     */
    suspend fun deleteBoard(boardId: Long) = withContext(ioDispatcher) {
        val photos = db.photoQueries.selectPhotosByBoard(boardId).executeAsList()
        db.boardQueries.deleteBoard(boardId)
        photos.forEach { photoFileStorage.deletePhotoFile(it.filePath) }
    }

    /** Импортирует файл (Uri на Android / путь на Desktop) и добавляет его в конец доски. */
    suspend fun addPhotoToBoard(boardId: Long, sourcePath: String): Photo = withContext(ioDispatcher) {
        val imported: ImportedPhotoFile = photoFileStorage.importPhoto(sourcePath)
        // НЕ count(*) — после удаления фото из середины списка это даст
        // задвоенный orderIndex (например: было [0,1,2,3], удалили индекс 2,
        // осталось [0,1,3] — три строки, count()==3, но 3 уже занят).
        // Берём реальный максимум + 1.
        val nextOrder = (db.photoQueries.selectMaxOrderIndex(boardId).executeAsOne() ?: -1L) + 1L
        db.photoQueries.insertPhoto(
            boardId, imported.storedPath, nextOrder,
            imported.width.toLong(), imported.height.toLong(), currentTimeMillis()
        )
        val id = db.photoQueries.lastInsertRowId().executeAsOne()
        Photo(id, boardId, imported.storedPath, nextOrder.toInt(), imported.width, imported.height, currentTimeMillis())
    }

    /**
     * Применяет новый порядок фото в доске — [orderedIds] это ПОЛНЫЙ список
     * id фото доски в желаемой последовательности (его строит PhotoBoardGrid
     * после перетаскивания). Применяем по id, а не по позиции: так реордер
     * устойчив даже если между окончанием жеста и выполнением этой корутины
     * список фото успел измениться (например, доехал параллельный импорт).
     */
    suspend fun reorderPhotos(boardId: Long, orderedIds: List<Long>) = withContext(ioDispatcher) {
        db.transaction {
            orderedIds.forEachIndexed { index, photoId ->
                db.photoQueries.updatePhotoOrder(index.toLong(), photoId)
            }
        }
    }

    /** Удаляет фото с доски: строку в БД и сам файл на диске. */
    suspend fun deletePhoto(photo: Photo) = withContext(ioDispatcher) {
        db.photoQueries.deletePhoto(photo.id)
        photoFileStorage.deletePhotoFile(photo.filePath)
    }
}

internal expect fun currentTimeMillis(): Long
