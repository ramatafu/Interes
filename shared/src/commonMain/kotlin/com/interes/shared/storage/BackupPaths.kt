package com.interes.shared.storage

/**
 * Расположение файла БД и папки фото на текущей платформе — то, что
 * BackupManager (commonMain) упаковывает в ZIP при резервном копировании и
 * распаковывает при восстановлении. Сами пути платформенные (Android:
 * приватное хранилище приложения; Desktop: %APPDATA%\Interes), поэтому
 * expect/actual, но дальше с этими путями работает обычный java.io/java.util.zip
 * прямо в commonMain — это безопасно ИМЕННО в этом проекте, потому что оба
 * реальных таргета (android { ... } и jvm("desktop") в shared/build.gradle.kts)
 * работают на JVM и одинаково имеют полный java.* — в отличие от
 * ImagePicker/PhotoFileStorage, где platform-типы (content:// Uri на
 * Android vs обычный путь на Desktop) РЕАЛЬНО различались по смыслу, тут
 * разницы нет: и там, и там — простой абсолютный путь к файлу.
 */
expect class BackupPaths {
    val databaseFilePath: String
    val photosDirPath: String
}
