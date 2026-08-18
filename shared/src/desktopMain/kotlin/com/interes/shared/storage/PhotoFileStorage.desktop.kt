package com.interes.shared.storage

import java.io.File
import java.util.UUID
import javax.imageio.ImageIO

actual class PhotoFileStorage {

    actual fun importPhoto(sourcePath: String): ImportedPhotoFile {
        val sourceFile = File(sourcePath)
        require(sourceFile.exists()) { "Файл не найден: $sourcePath" }

        val appDataDir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Interes/photos")
        appDataDir.mkdirs()
        // Расширение исходного файла, а не жёсткое ".jpg" — источник может
        // быть PNG/WEBP (диалог выбора в ImagePicker.desktop.kt разрешает
        // все три формата).
        val extension = sourceFile.extension.ifBlank { "jpg" }
        val destFile = File(appDataDir, "${UUID.randomUUID()}.$extension")
        sourceFile.copyTo(destFile, overwrite = true)

        val image = ImageIO.read(destFile)
            ?: error("Файл не распознан как изображение: $sourcePath")

        return ImportedPhotoFile(
            storedPath = destFile.absolutePath,
            width = image.width,
            height = image.height
        )
    }

    actual fun deletePhotoFile(storedPath: String) {
        runCatching { File(storedPath).delete() }
    }
}
