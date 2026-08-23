package com.interes.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Тонкие векторные иконки для кнопок окна и поиска — по образцу рендера
 * (см. чат): минималистичные линии вместо emoji-глифов, которые раньше
 * использовались в TopBarGlyph (крестик "\u2715", минус "\u2212" и т.д.).
 * Рисуются через Canvas, а не через icon-шрифт — в проекте принципиально
 * не используется material-icons-extended (см. комментарии в
 * BoardScreen.kt/BoardsListScreen.kt: "без material-icons-extended ради
 * одной иконки").
 */

@Composable
fun MinimizeGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.4.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val y = this.size.height / 2f
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(this.size.width, y),
            strokeWidth = strokeWidth.toPx()
        )
    }
}

@Composable
fun MaximizeGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.4.dp
) {
    Canvas(modifier = modifier.size(size)) {
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(this.size.width, this.size.height),
            style = Stroke(width = strokeWidth.toPx())
        )
    }
}

@Composable
fun CloseGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.4.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(color = color, start = Offset(0f, 0f), end = Offset(w, h), strokeWidth = strokeWidth.toPx())
        drawLine(color = color, start = Offset(0f, h), end = Offset(w, 0f), strokeWidth = strokeWidth.toPx())
    }
}

/**
 * Значок лупы для поиска — кружок с ручкой, как на рендере, вместо
 * emoji "\uD83D\uDD0D".
 */
@Composable
fun SearchGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.6.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val circleDiameter = this.size.minDimension * 0.68f
        val circleRadius = circleDiameter / 2f
        val center = Offset(circleRadius + stroke / 2f, circleRadius + stroke / 2f)
        drawCircle(color = color, radius = circleRadius, center = center, style = Stroke(width = stroke))

        val handleStart = Offset(
            center.x + circleRadius * 0.75f,
            center.y + circleRadius * 0.75f
        )
        val handleEnd = Offset(this.size.width, this.size.height)
        drawLine(color = color, start = handleStart, end = handleEnd, strokeWidth = stroke)
    }
}
