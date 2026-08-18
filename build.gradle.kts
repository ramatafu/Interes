// Все версии плагинов теперь берутся из ОДНОГО места — gradle/libs.versions.toml.
// Раньше здесь были хардкоженные версии, которые и вызвали конфликт с
// версией, которую сгенерировал мастер Android Studio (см. README, раздел
// "Исправление ошибки версии плагина").
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
}
