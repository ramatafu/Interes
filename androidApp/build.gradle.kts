import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // kotlin-android НЕ подключаем: начиная с AGP 9.0 поддержка Kotlin
    // встроена в сам com.android.application и включена по умолчанию.
    // Отдельное применение org.jetbrains.kotlin.android теперь конфликтует
    // с этим встроенным режимом ("no longer required for Kotlin support
    // since AGP 9.0" — именно эта ошибка была в логе).
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.interes.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.interes.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Раньше здесь был android.kotlinOptions { jvmTarget = "17" } — со
    // встроенным Kotlin этот блок больше не существует. JVM target для
    // Kotlin теперь задаётся отдельным top-level kotlin{} блоком ниже.
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    // ВАЖНО: material3/foundation/core-ktx/lifecycle-viewmodel-compose
    // сюда намеренно НЕ добавлены. Весь UI приходит из :shared через
    // Compose Multiplatform 1.11.1 (compose.material3/compose.foundation
    // и т.д. в shared/build.gradle.kts) — если здесь же захардкодить
    // ДРУГИЕ версии тех же артефактов androidx.compose.*, это создаёт
    // риск конфликта версий на класспасе Android-таргета. MainActivity.kt
    // использует только ComponentActivity/setContent — для этого нужен
    // только activity-compose, версия берётся из того же каталога, что и
    // в shared/build.gradle.kts (androidMain), чтобы не разъезжались.
    implementation(libs.androidx.activity.compose)
}
