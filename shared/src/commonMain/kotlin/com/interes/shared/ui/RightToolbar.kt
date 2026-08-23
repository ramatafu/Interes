package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Ширина правого тулбара — такая же, как у левого (SideToolbar),
// чтобы окно выглядело симметрично. Используется в AppRoot.kt
// (padding(end = RightToolbarWidth)) и здесь.
val RightToolbarWidth: Dp = 72.dp

/**
 * Правый тулбар. ПОКА ПУСТОЙ — просто цветная полоса фиксированной ширины,
 * зарезервированная под будущие инструменты. Видна поверх любого экрана,
 * как и левый SideToolbar, и так же вынесена СНАРУЖИ затухающего слоя
 * прозрачности (см. AppRoot.kt).
 */
@Composable
fun RightToolbar(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(RightToolbarWidth)
            .fillMaxHeight()
            .background(AppPrimaryColor)
    )
}