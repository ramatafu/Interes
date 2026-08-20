package com.interes.shared.util

/**
 * Превращает АБСОЛЮТНЫЙ локальный путь к файлу (как его отдаёт PhotoFileStorage
 * на обеих платформах — см. PhotoFileStorage.desktop.kt/.android.kt) в валидный
 * "file://" URI для Coil (AsyncImage).
 *
 * На Android путь уже unix-style ("/data/user/0/.../photos/x.jpg") — простое
 * приклеивание "file://" спереди случайно даёт корректный URI
 * ("file:///data/user/0/.../x.jpg"), поэтому раньше баг был не виден.
 *
 * На Windows путь содержит обратные слэши и букву диска
 * ("C:\Users\Valuka\AppData\Roaming\Interes\photos\x.jpg") — то же самое
 * приклеивание давало "file://C:\Users\...\x.jpg": обратные слэши вместо
 * прямых и не хватает слэша перед буквой диска. Coil такой URI не понимает и
 * просто не грузит картинку — БЕЗ видимого исключения (проверено: логи при
 * добавлении фото были полностью чистыми, при этом ни превью на карточке
 * доски, ни сама сетка фото ничего не показывали). Отсюда и функция —
 * единая на все три места, где строится "file://" (BoardsListScreen,
 * PhotoBoardGrid, PhotoViewerContent), чтобы не чинить один и тот же баг
 * трижды и по-разному.
 */
fun localFilePathToUri(path: String): String {
    val normalized = path.replace('\\', '/')
    // "C:/Users/..." -> "/C:/Users/..." — на Windows после буквы диска сразу
    // идёт ':', на Unix-путях ("/data/...") такого нет, там просто ничего
    // не меняем.
    val withLeadingSlash = if (normalized.length >= 2 && normalized[1] == ':') "/$normalized" else normalized
    return "file://$withLeadingSlash"
}
