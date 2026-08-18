plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // Заменяет связку "id("com.android.library") + kotlin("multiplatform")" —
    // та комбинация с AGP 9.0+ больше не поддерживается (см. README).
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    // Раньше было: androidTarget() + отдельный блок android { ... } внизу
    // файла. У нового плагина Android-таргет настраивается прямо здесь,
    // отдельного top-level android-расширения больше нет.
    android {
        namespace = "com.interes.shared"
        compileSdk = 36
        minSdk = 26
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)
                implementation(libs.kotlinx.coroutines.core)

                // UI (доска, сетка, drag-n-drop) — общий код для Android и Desktop
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)

                // Загрузка изображений с локального диска, мультиплатформенно.
                // Сетевой модуль (coil-network-*) намеренно не подключаем —
                // приложение офлайн, грузим только локальные file:// пути.
                implementation(libs.coil.compose)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
                // Нужен для rememberLauncherForActivityResult в системном
                // пикере фото (ImagePicker.android.kt) и для BackHandler
                // (PlatformBackHandler.android.kt).
                implementation(libs.androidx.activity.compose)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("InteresDatabase") {
            packageName.set("com.interes.shared.db")
        }
    }
}
