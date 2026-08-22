package com.interes.shared.ui

import androidx.compose.runtime.Composable
import com.interes.shared.storage.BackupPaths

/**
 * В отличие от rememberImagePicker (который просто отдаёт путь/Uri
 * выбранного файла) эти два триггера выполняют ВСЮ операцию целиком —
 * и выбор места, и упаковку/распаковку ZIP. Причина: на Android системный
 * диалог "Сохранить как" (Storage Access Framework) отдаёт content:// Uri,
 * в который можно только писать через contentResolver, а не обычный путь
 * файла на диске — то есть на Android эти два шага (выбор места и запись
 * файла) физически разные API и не разделяются так же чисто, как на
 * Desktop (java.awt.FileDialog сразу даёт обычный путь). Проще и надёжнее
 * держать всю платформенную кухню внутри одной функции, чем протаскивать
 * Uri через общий BackupManager (который работает с простыми путями и
 * должен оставаться платформенно-нейтральным).
 */
@Composable
expect fun rememberBackupCreator(paths: BackupPaths, onResult: (Result<Unit>) -> Unit): () -> Unit

@Composable
expect fun rememberBackupRestorer(paths: BackupPaths, onResult: (Result<Unit>) -> Unit): () -> Unit
