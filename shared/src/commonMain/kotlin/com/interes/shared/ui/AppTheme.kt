package com.interes.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.interes.shared.generated.resources.Res
import com.interes.shared.generated.resources.inter_bold
import com.interes.shared.generated.resources.inter_medium
import com.interes.shared.generated.resources.inter_regular
import org.jetbrains.compose.resources.Font

/**
 * Единственное место во всём приложении, где задаются фирменный цвет и
 * шрифт — оба по ТЗ фиксированные (без переключения темы).
 */
val AppPrimaryColor = Color(0xFF0088CC)

/**
 * Цвет фона левого и правого тулбаров (SideToolbar.kt, RightToolbar.kt), а
 * также низа/фонов экранов (BottomAppBar, containerColor у Scaffold в
 * BoardsListScreen/BoardScreen/TrashScreen) — взят из образца
 * "Цвет_левого_тулбара.jpg" (см. чат).
 */
val SideToolbarColor = Color(0xFF273540)

/**
 * Цвет верхней панели на всех трёх экранах — чуть темнее SideToolbarColor,
 * взят из отдельного образца "Цвет_верхнего_тулбара.jpg" (см. чат): в
 * рендере верхняя панель и боковая — близкие, но не идентичные оттенки.
 */
val TopToolbarColor = Color(0xFF1C2932)

private val AppPrimaryContainerColor = Color(0xFFB3DBF0)

private val AppColorScheme = lightColorScheme(
    primary = AppPrimaryColor,
    onPrimary = Color.White,
    primaryContainer = AppPrimaryContainerColor,
    onPrimaryContainer = Color(0xFF00344D),
    secondary = AppPrimaryColor,
    onSecondary = Color.White,
    secondaryContainer = AppPrimaryContainerColor,
    onSecondaryContainer = Color(0xFF00344D),
    tertiary = AppPrimaryColor,
    onTertiary = Color.White
)

/**
 * Inter — три начертания, зарегистрированные под своими FontWeight, чтобы
 * bold/medium текст (headline, title, label) не подменялся системным
 * жирным начертанием regular-шрифта, а брал реальный inter_bold/inter_medium.
 */
@Composable
private fun interFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, weight = FontWeight.Normal),
    Font(Res.font.inter_medium, weight = FontWeight.Medium),
    Font(Res.font.inter_bold, weight = FontWeight.Bold)
)

@Composable
private fun appTypography(): Typography {
    val inter = interFontFamily()
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = inter),
        displayMedium = base.displayMedium.copy(fontFamily = inter),
        displaySmall = base.displaySmall.copy(fontFamily = inter),
        headlineLarge = base.headlineLarge.copy(fontFamily = inter),
        headlineMedium = base.headlineMedium.copy(fontFamily = inter),
        headlineSmall = base.headlineSmall.copy(fontFamily = inter),
        titleLarge = base.titleLarge.copy(fontFamily = inter),
        titleMedium = base.titleMedium.copy(fontFamily = inter),
        titleSmall = base.titleSmall.copy(fontFamily = inter),
        bodyLarge = base.bodyLarge.copy(fontFamily = inter),
        bodyMedium = base.bodyMedium.copy(fontFamily = inter),
        bodySmall = base.bodySmall.copy(fontFamily = inter),
        labelLarge = base.labelLarge.copy(fontFamily = inter),
        labelMedium = base.labelMedium.copy(fontFamily = inter),
        labelSmall = base.labelSmall.copy(fontFamily = inter)
    )
}

@Composable
fun InteresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = appTypography(),
        content = content
    )
}
