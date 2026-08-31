import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    // foundation/ui/runtime приходят транзитивно из :shared (там теперь
    // api, см. shared/build.gradle.kts) — тем же графом версий, которым
    // скомпилирован :shared. material3 дублирован намеренно: desktopApp
    // использует MaterialTheme в AppTitleBar напрямую.
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "com.interes.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Interes"
            packageVersion = "0.1.0"

            // ВАЖНО: автоматическое определение нужных JDK-модулей (jdeps),
            // которое Compose Desktop делает под капотом при сборке
            // урезанного jlink-рантайма для .msi/.exe, не видит модули,
            // подключаемые рефлексией/ServiceLoader — а именно так
            // SQLite JDBC (org.sqlite.JDBC регистрируется через
            // java.sql.DriverManager/ServiceLoader) и AWT FileDialog
            // подключают часть своих зависимостей. Итог без этого флага —
            // урезанный рантайм без java.sql/java.naming и т.п., и
            // приложение падает с NoClassDefFoundError / ClassNotFoundException
            // ДО того, как успевает открыть окно — то есть именно то
            // молчаливое закрытие Interes.exe, о котором идёт речь.
            // includeAllModules достаёт полный JDK в комплект — почти не
            // раздувает MSI и полностью убирает этот класс ошибок.
            windows {
                includeAllModules = true
                // console = false (по умолчанию) — окно консоли/логов
                // больше не открывается рядом с приложением. Раньше стояло
                // true, чтобы видеть ошибки запуска (NoClassDefFoundError
                // и т.п.) до открытия окна — но includeAllModules выше уже
                // решает саму причину тех ошибок (полный jlink-рантайм
                // вместо урезанного), так что отдельная консоль для этого
                // больше не нужна.
                // Иконка .exe / ярлыка на рабочем столе / в панели задач
                // после установки (не иконка окна во время работы — та
                // задаётся отдельно через Window(icon = ...) в Main.kt).
                iconFile.set(project.file("src/main/resources/app_icon.ico"))
            }
        }
    }
}
