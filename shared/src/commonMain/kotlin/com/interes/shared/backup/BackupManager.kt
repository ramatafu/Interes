package com.interes.shared.backup

import com.interes.shared.storage.BackupPaths
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Резервное копирование: один ZIP с файлом БД (в корне архива, как
 * "interes.db") и всей папкой фото (под "photos/..."). Общий код для обеих
 * платформ — см. обоснование в BackupPaths.kt (почему тут можно
 * java.io/java.util.zip прямо в commonMain).
 *
 * ОГРАНИЧЕНИЕ, о котором стоит знать: копирование идёт "вживую", пока
 * приложение (и его открытое соединение с БД) работает — без остановки
 * записи и без PRAGMA-чекпоинтов. Для этого приложения это безопасно на
 * практике: SQLite тут используется в режиме журнала по умолчанию
 * (rollback journal, не WAL — ни AndroidSqliteDriver, ни JdbcSqliteDriver
 * в проекте не переключают журнал в WAL), а он не оставляет висящих
 * недописанных файлов между операциями — каждая операция с БД тут и так
 * короткая (одна вставка/обновление). Для более серьёзного продакшена
 * корректнее было бы на время бэкапа закрывать соединение с БД (или хотя
 * бы делать VACUUM INTO) — здесь это осознанно не сделано ради простоты, у
 * personal-use приложения для досок с фото такой риск объективно невелик.
 */
object BackupManager {

    fun createBackup(paths: BackupPaths, destinationZipPath: String): Result<Unit> = runCatching {
        val dbFile = File(paths.databaseFilePath)
        require(dbFile.exists()) { "Файл базы данных не найден: ${paths.databaseFilePath}" }

        val destFile = File(destinationZipPath)
        destFile.parentFile?.mkdirs()

        ZipOutputStream(FileOutputStream(destFile)).use { zip ->
            zip.putNextEntry(ZipEntry("interes.db"))
            FileInputStream(dbFile).use { it.copyTo(zip) }
            zip.closeEntry()

            val photosDir = File(paths.photosDirPath)
            val photoFiles = photosDir.listFiles()
            if (photoFiles != null) {
                for (photo in photoFiles) {
                    if (!photo.isFile) continue
                    zip.putNextEntry(ZipEntry("photos/${photo.name}"))
                    FileInputStream(photo).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /**
     * Восстанавливает БД и фото из ранее созданного ZIP.
     *
     * ВАЖНО: приложение НЕ перезапускается автоматически и не переоткрывает
     * соединение с БД само — файл interes.db на диске подменяется, но уже
     * открытое JDBC/Android-соединение продолжает смотреть на старые данные
     * до перезапуска процесса. Поэтому после успешного восстановления
     * пользователю нужно вручную перезапустить Interes (см. текст в
     * SideToolbar.kt после успешного restoreBackup) — это НЕ забытая
     * недоделка, а осознанное решение: "на лету" подменить живое соединение
     * с SQLite без риска гонки/повреждения БД — заметно более сложная и
     * рискованная задача, для одного пользователя проще и надёжнее попросить
     * перезапустить приложение.
     *
     * Сначала распаковываем во ВРЕМЕННУЮ папку рядом и только при полном
     * успехе переносим на место — чтобы половина фото не могла молча
     * потеряться, если восстановление прервётся посередине (битый архив,
     * закончилось место на диске и т.п.): либо всё, либо ничего не меняется.
     */
    fun restoreBackup(paths: BackupPaths, sourceZipPath: String): Result<Unit> = runCatching {
        val sourceFile = File(sourceZipPath)
        require(sourceFile.exists()) { "Файл резервной копии не найден: $sourceZipPath" }

        val stagingDir = File(File(paths.databaseFilePath).parentFile, "restore_staging").apply {
            deleteRecursively()
            mkdirs()
        }
        var restoredDb: File? = null
        val restoredPhotos = mutableListOf<File>()

        try {
            ZipInputStream(FileInputStream(sourceFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(stagingDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { zip.copyTo(it) }
                        if (entry.name == "interes.db") {
                            restoredDb = outFile
                        } else if (entry.name.startsWith("photos/")) {
                            restoredPhotos.add(outFile)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val db = restoredDb ?: error("В архиве нет файла базы данных (interes.db) — это не резервная копия Interes")

            // Только теперь, когда распаковка ПОЛНОСТЬЮ удалась, заменяем
            // настоящие файлы. db.copyTo(overwrite = true) вместо rename —
            // rename между разными файловыми системами/дисками (например,
            // staging на диске C, а само приложение установлено на другой
            // диск) может просто не сработать.
            db.copyTo(File(paths.databaseFilePath), overwrite = true)

            val photosDir = File(paths.photosDirPath)
            photosDir.mkdirs()
            for (restoredPhoto in restoredPhotos) {
                restoredPhoto.copyTo(File(photosDir, restoredPhoto.name), overwrite = true)
            }
        } finally {
            stagingDir.deleteRecursively()
        }
    }
}
