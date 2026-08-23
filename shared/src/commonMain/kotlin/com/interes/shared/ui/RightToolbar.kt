package com.interes.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val RightToolbarWidth: Dp = 72.dp

/**
 * Правый тулбар. Снаружи затухающего слоя — никогда не прозрачнеет.
 * onNextPhoto != null — значит открыт просмотрщик и есть следующее фото;
 * тогда появляется большая стрелка ▶ для пролистывания.
 */
@Composable
fun RightToolbar(
    modifier: Modifier = Modifier,
    onNextPhoto: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .width(RightToolbarWidth)
            .fillMaxHeight()
            .background(AppPrimaryColor)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (onNextPhoto != null) {
            IconButton(
                onClick = onNextPhoto,
                modifier = Modifier.size(64.dp)
            ) {
                Text(
                    "\u25B6",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}