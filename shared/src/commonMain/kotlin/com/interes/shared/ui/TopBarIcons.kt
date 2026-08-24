package com.interes.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

// ---------------------------------------------------------------------
// Иконки левого тулбара (SideToolbar.kt) — тот же минималистичный
// векторный стиль: тонкие линии, без заливки, скруглённые концы там, где
// это уместно (дом, стрелка, крышка корзины).
// ---------------------------------------------------------------------

@Composable
fun HomeGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.6.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val w = this.size.width
        val h = this.size.height
        val roofY = h * 0.42f
        val path = Path().apply {
            moveTo(0f, roofY)
            lineTo(w / 2f, 0f)
            lineTo(w, roofY)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round))
        // Дверь.
        drawLine(
            color = color,
            start = Offset(w / 2f, h),
            end = Offset(w / 2f, h * 0.62f),
            strokeWidth = stroke
        )
    }
}

@Composable
fun PlusGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.8.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val w = this.size.width
        val h = this.size.height
        drawLine(color = color, start = Offset(w / 2f, 0f), end = Offset(w / 2f, h), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(0f, h / 2f), end = Offset(w, h / 2f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** Значок резервной копии — стрелка "сохранить" в лоток. */
@Composable
fun BackupGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.6.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val w = this.size.width
        val h = this.size.height
        val shaftBottom = h * 0.6f
        // Стрелка вниз.
        drawLine(color = color, start = Offset(w / 2f, 0f), end = Offset(w / 2f, shaftBottom), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.2f, shaftBottom * 0.55f), end = Offset(w / 2f, shaftBottom), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.8f, shaftBottom * 0.55f), end = Offset(w / 2f, shaftBottom), strokeWidth = stroke, cap = StrokeCap.Round)
        // Лоток снизу.
        drawLine(color = color, start = Offset(0f, h), end = Offset(w, h), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(0f, h * 0.78f), end = Offset(0f, h), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w, h * 0.78f), end = Offset(w, h), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun InfoGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.6.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val radius = (this.size.minDimension - stroke) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = stroke))
        // Точка.
        drawCircle(color = color, radius = stroke * 0.65f, center = Offset(center.x, center.y - radius * 0.42f))
        // Ножка.
        drawLine(
            color = color,
            start = Offset(center.x, center.y - radius * 0.05f),
            end = Offset(center.x, center.y + radius * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/** Стрелка "предыдущее фото" (шеврон) — замена символа "\u25C0". */
@Composable
fun ChevronLeftGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.68f, h * 0.12f)
            lineTo(w * 0.28f, h / 2f)
            lineTo(w * 0.68f, h * 0.88f)
        }
        drawPath(path, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

/** Стрелка "следующее фото" (шеврон вправо) — замена символа "\u25B6". */
@Composable
fun ChevronRightGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.32f, h * 0.12f)
            lineTo(w * 0.72f, h / 2f)
            lineTo(w * 0.32f, h * 0.88f)
        }
        drawPath(path, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

@Composable
fun TrashGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 1.6.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val w = this.size.width
        val h = this.size.height
        val lidY = h * 0.24f
        // Крышка.
        drawLine(color = color, start = Offset(w * 0.12f, lidY), end = Offset(w * 0.88f, lidY), strokeWidth = stroke, cap = StrokeCap.Round)
        // Ручка.
        drawLine(color = color, start = Offset(w * 0.38f, lidY), end = Offset(w * 0.38f, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.62f, lidY), end = Offset(w * 0.62f, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.38f, 0f), end = Offset(w * 0.62f, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        // Корпус (трапеция — сверху чуть шире).
        val bodyPath = Path().apply {
            moveTo(w * 0.18f, lidY)
            lineTo(w * 0.22f, h)
            lineTo(w * 0.78f, h)
            lineTo(w * 0.82f, lidY)
        }
        drawPath(bodyPath, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round))
        // Рёбра внутри.
        drawLine(color = color, start = Offset(w * 0.4f, lidY + h * 0.08f), end = Offset(w * 0.41f, h * 0.86f), strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.6f, lidY + h * 0.08f), end = Offset(w * 0.59f, h * 0.86f), strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
    }
}

/**
 * Компактная кнопка верхнего тулбара: размер по глифу + фиксированная
 * высота 40 dp, БЕЗ внутренних горизонтальных отступов (в отличие от
 * IconButton, который добавляет ~12 dp с каждой стороны и раздувает
 * расстояния между иконками). Ширина по содержимому — поэтому Spacer(60.dp)
 * рядом даёт честные 60 dp между лупой и "Свернуть". Используется из
 * AppRoot.kt (верхняя панель главного экрана рисуется там целиком, чтобы
 * по-настоящему доходить до краёв окна, а не только до края отступа под
 * боковые тулбары).
 */
@Composable
fun TopBarGlyph(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
