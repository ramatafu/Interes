package com.interes.shared.storage

import android.content.Context
import java.io.File

actual class BackupPaths(private val context: Context) {
    // context.getDatabasePath(...) — то самое место, куда AndroidSqliteDriver
    // (см. DatabaseDriverFactory.android.kt) кладёт файл БД по умолчанию.
    actual val databaseFilePath: String = context.getDatabasePath("interes.db").absolutePath

    // context.filesDir/photos — та же папка, что в PhotoFileStorage.android.kt.
    actual val photosDirPath: String = File(context.filesDir, "photos").absolutePath
}
