package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ArrowBlue,
    onPrimary = Color.White,
    primaryContainer = ArrowBlue.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    secondary = StarGold,
    onSecondary = Color.Black,
    error = HeartRed,
    onError = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = ArrowBlue,
    onPrimary = Color.White,
    primaryContainer = ArrowBlueLight,
    onPrimaryContainer = ArrowBlue,
    secondary = StarGold,
    onSecondary = Color.Black,
    error = HeartRed,
    onError = Color.White,
    background = GameBackground,
    onBackground = TextPrimary,
    surface = GameSurface,
    onSurface = TextPrimary,
    surfaceVariant = GameSurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = GameOutline
)

@Composable
fun ArrowPuzzleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional game palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = ArrowPuzzleTheme(darkTheme, dynamicColor, content)
