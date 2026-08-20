package com.interes.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame

@Composable
actual fun rememberImagePicker(onPicked: (List<String>) -> Unit): () -> Unit {
    return remember {
        {
            val dialog = FileDialog(null as Frame?, "Выберите фото", FileDialog.LOAD)
            dialog.isMultipleMode = true
            // FileDialog.setFilenameFilter официально не работает в
            // Windows-реализации AWT (см. javadoc самого метода: "Filename
            // filters do not function in Sun's reference implementation for
            // Microsoft Windows") — на Windows этот колбэк просто никогда не
            // вызывается, и диалог показывает вообще все файлы независимо от
            // фильтра. Оставляем его для macOS/Linux, где он действительно
            // работает, но на Windows полагаемся на dialog.file — маску,
            // которую нативный диалог Windows понимает сам.
            dialog.file = "*.jpg;*.jpeg;*.png;*.bmp;*.gif"
            dialog.setFilenameFilter { _, name ->
                // .webp сюда намеренно НЕ включён: стандартный javax.imageio.ImageIO
                // (см. PhotoFileStorage.desktop.kt — используется для определения
                // размеров фото при импорте) не умеет декодировать WebP без
                // сторонней библиотеки-плагина, которую мы не подключаем. Если
                // добавить .webp сюда без неё, импорт такого файла будет
                // гарантированно падать с "файл не распознан как изображение".
                val lower = name.lowercase()
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".bmp") || lower.endsWith(".gif")
            }
            dialog.isVisible = true // блокирует поток до закрытия — штатное поведение AWT FileDialog
            val files = dialog.files
            if (files.isNotEmpty()) {
                onPicked(files.map { it.absolutePath })
            }
        }
    }
}
