package com.uyen.launcher.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Ps5Blue,
    secondary = SteamDeckAccent,
    tertiary = AccentGold,
    background = BackgroundDark,
    surface = BackgroundSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = BackgroundDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun UyenTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
