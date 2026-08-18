package com.interes.shared.ui

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // На Windows нет системной кнопки/жеста "Назад", перехватывать нечего.
}
