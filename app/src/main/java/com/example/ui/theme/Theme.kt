package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InstagramDarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color.White,
    primaryContainer = InstaGradientMiddle,
    onPrimaryContainer = Color.White,
    secondary = InstaGradientEnd,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = InstaGradientStart,
    onTertiary = Color.White,
    background = DarkBackground,       // bg-black
    onBackground = DarkTextPrimary,    // text-white
    surface = DarkSurface,             // bg-[#121212]
    onSurface = DarkTextPrimary,       // text-white
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,              // border-zinc-800
    outlineVariant = DarkBorder,
    error = InstaLikeRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Strictly Instagram pure dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = InstagramDarkColorScheme,
        typography = Typography,
        content = content
    )
}
