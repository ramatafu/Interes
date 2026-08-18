package com.interes.shared.ui

import androidx.compose.runtime.Composable

/**
 * Перехватывает системную кнопку/жест "Назад".
 *
 * До этого исправления в приложении не было вообще никакой обработки
 * back-press — навигация (список досок / доска / просмотрщик) жила только
 * в mutableState внутри AppRoot и никак не была связана с системной кнопкой
 * "Назад". Из-за этого нажатие "Назад" внутри доски не возвращало к списку
 * досок, а действовало как обычное закрытие Activity — приложение сразу
 * сворачивалось/завершалось, что и воспринималось как "вылет".
 *
 * - Android: делегирует в androidx.activity.compose.BackHandler — тот сам
 *   регистрирует OnBackPressedCallback в системном диспетчере, ничего
 *   дополнительно переопределять в MainActivity не нужно.
 * - Windows: у Compose Desktop нет системной кнопки "Назад" — actual пустой.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
