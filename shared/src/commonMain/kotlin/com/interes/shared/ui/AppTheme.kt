package com.interes.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Единственное место во всём приложении, где задаются фирменный цвет и
 * шрифт — оба по ТЗ фиксированные (без переключения темы).
 * 
 * ВНИМАНИЕ: шрифт Inter временно отключён из-за проблем с компиляцией.
 * Будет подключен позже отдельным обновлением.
 */
val AppPrimaryColor = Color(0xFF0088CC)

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

@Composable
fun InteresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}