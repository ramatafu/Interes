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

                // UI (доска, сетка, drag-n-drop) — общий код для Android и Desktop.
                // ВАЖНО: api, а не implementation. desktopApp зависит от
                // :shared через project(":shared") и сам напрямую вызывает
                // Compose-композаблы (AppTitleBar в Main.kt использует Row/
                // Column/Modifier.weight из foundation). При implementation
                // здесь desktopApp эти Compose-зависимости не наследует и
                // резолвит Compose САМ, отдельным графом — на практике версии
                // расходятся (в репозитории есть собранная ранее папка app/ с
                // буквально задвоенными jar'ами: runtime-desktop-1.11.1 И
                // runtime-desktop-1.11.2, savedstate-compose-desktop-1.3.6 И
                // 1.4.0 и т.д. одновременно). Из-за этого при сборке
                // desktopApp падает с "Cannot access ... it is internal in
                // file" (Modifier.weight) и "Unresolved reference
                // WindowDraggableArea" — компилятор просто видит два разных,
                // несовместимых набора классов Compose. api гарантирует, что
                // desktopApp получит ТОТ ЖЕ граф зависимостей, на котором
                // скомпилирован :shared.
                api(compose.runtime)
                api(compose.ui)
                api(compose.foundation)
                api(compose.material3)

                // Загрузка изображений с локального диска, мультиплатформенно.
                // Сетевой модуль (coil-network-*) намеренно не подключаем —
                // приложение офлайн, грузим только локальные file:// пути.
                implementation(libs.coil.compose)

                // Шрифт Inter (см. AppTheme.kt) и иконка приложения (см.
                // BoardsListScreen.kt, desktopApp/Main.kt) — грузятся из
                // commonMain/composeResources/ через сгенерированный
                // Res-класс. api, а не implementation — по той же причине,
                // что и для compose.ui/foundation/material3 выше:
                // desktopApp напрямую использует Res.drawable.app_icon в
                // своём Main.kt, а значит тип DrawableResource должен быть
                // на его собственном compile-classpath, а не только на
                // рантайм-classpath через project(":shared").
                api(compose.components.resources)
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

compose.resources {
    // Явно задаём имя пакета для сгенерированного Res (шрифт Inter, см.
    // AppTheme.kt) — чтобы не зависеть от автоопределения по умолчанию и
    // держать его в одном стиле с остальными пакетами проекта
    // (com.interes.shared.*).
    packageOfResClass = "com.interes.shared.generated.resources"

    // По умолчанию сгенерированный класс Res — internal, виден только
    // внутри :shared. desktopApp обращается к Res.drawable.app_icon
    // напрямую из своего Main.kt (иконка окна/панели задач), а BoardsListScreen
    // здесь же, в :shared, к внутренней видимости и так был бы нечувствителен —
    // проблема именно в межмодульном доступе. Без этого флага сборка падает:
    // "Cannot access 'object Res : Any': it is internal in file".
    publicResClass = true
}
