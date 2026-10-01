package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PdfRedDarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = PdfRedDarkContainer,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF1C252B),
    background = PdfDarkBackground,
    surface = PdfDarkSurface,
    surfaceVariant = PdfDarkSurfaceVariant,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFEEEEEE),
)

private val LightColorScheme = lightColorScheme(
    primary = PdfRedPrimary,
    onPrimary = Color.White,
    primaryContainer = PdfRedLight,
    onPrimaryContainer = PdfRedDark,
    secondary = PdfSecondary,
    onSecondary = Color.White,
    secondaryContainer = PdfSecondaryLight,
    onSecondaryContainer = Color(0xFF102027),
    tertiary = PdfTertiary,
    background = Color(0xFFF9FAFB),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F5),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
