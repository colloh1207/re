package com.sdd.marketplace.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SddPink,
    onPrimary = Color.White,
    primaryContainer = Pink90,
    onPrimaryContainer = Pink10,
    secondary = Rose40,
    onSecondary = Color.White,
    secondaryContainer = Rose90,
    onSecondaryContainer = Color(0xFF3D0026),
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Color(0xFF001F24),
    error = ErrorRed,
    background = SddBackgroundPink,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Pink95,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Pink80,
)

private val DarkColorScheme = darkColorScheme(
    primary = Pink80,
    onPrimary = Pink20,
    primaryContainer = Pink30,
    onPrimaryContainer = Pink90,
    secondary = Rose80,
    onSecondary = Color(0xFF5B1143),
    secondaryContainer = Color(0xFF7B2960),
    onSecondaryContainer = Rose90,
    tertiary = Teal80,
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F57),
    onTertiaryContainer = Teal90,
    error = Color(0xFFFFB4AB),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = SddPink,
)

@Composable
fun SddMarketplaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge mode: keep status bar transparent, only control icon colour
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SddTypography,
        shapes = SddShapes,
        content = content
    )
}
