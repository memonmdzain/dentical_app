package com.dentical.staff.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dentical Brand Colors
val DenticalBlue = Color(0xFF1565C0)
val DenticalLightBlue = Color(0xFF42A5F5)
val DenticalTeal = Color(0xFF00897B)
val DenticalWhite = Color(0xFFFFFFFF)
val DenticalGrey = Color(0xFFF5F5F5)
val DenticalDarkGrey = Color(0xFF424242)

private val LightColorScheme = lightColorScheme(
    primary = DenticalBlue,
    secondary = DenticalTeal,
    tertiary = DenticalLightBlue,
    background = DenticalGrey,
    surface = DenticalWhite,
    onPrimary = DenticalWhite,
    onSecondary = DenticalWhite,
    onBackground = DenticalDarkGrey,
    onSurface = DenticalDarkGrey,
)

@Composable
fun DenticalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
