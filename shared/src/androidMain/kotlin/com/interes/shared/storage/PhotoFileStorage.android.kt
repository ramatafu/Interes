package com.interes.shared.storage

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

actual class PhotoFileStorage(private val context: Context) {

    actual fun importPhoto(sourcePath: String): ImportedPhotoFile {
        val uri = Uri.parse(sourcePath)
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        // Расширение — из реального MIME-типа файла, а не жёсткое ".jpg":
        // SAF-пикер отдаёт content:// Uri без расширения в самом пути,
        // а формат может быть PNG/WEBP (BitmapFactory на Android декодирует
        // WebP нативно, в отличие от Desktop — см. комментарий в
        // ImagePicker.desktop.kt про то, почему там .webp пришлось убрать).
        val mimeType = context.contentResolver.getType(uri)
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "jpg"
        val destFile = File(photosDir, "${UUID.randomUUID()}.$extension")

        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Не удалось открыть выбранное изображение: $sourcePath")

        // Декодируем только границы файла (inJustDecodeBounds), чтобы не
        // грузить всё изображение в память ради размеров.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(destFile.absolutePath, bounds)

        return ImportedPhotoFile(
            storedPath = destFile.absolutePath,
            width = bounds.outWidth,
            height = bounds.outHeight
        )
    }

    actual fun deletePhotoFile(storedPath: String) {
        runCatching { File(storedPath).delete() }
    }
}
