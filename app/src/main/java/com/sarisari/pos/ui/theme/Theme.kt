package com.sarisari.pos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = PinoyBlue,
    onPrimary = Color.White,
    primaryContainer = PinoyBlueLight,
    onPrimaryContainer = Color(0xFF001A52),
    secondary = PinoyRed,
    onSecondary = Color.White,
    secondaryContainer = PinoyRedLight,
    onSecondaryContainer = Color(0xFF410005),
    tertiary = Color(0xFF6B5900),
    onTertiary = Color.White,
    tertiaryContainer = SunYellowLight,
    onTertiaryContainer = Color(0xFF221B00),
    background = SurfaceWarm,
    surface = SurfaceWarm,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF173FA0),
    secondary = Color(0xFFFFB3AE),
    secondaryContainer = Color(0xFF8C0A18),
    tertiary = SunYellow,
    tertiaryContainer = Color(0xFF524400),
    background = DarkSurface,
    surface = DarkSurface,
)

@Composable
fun SariSariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
