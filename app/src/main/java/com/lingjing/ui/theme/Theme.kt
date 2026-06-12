package com.lingjing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 水墨丹青主题
 * 支持浅色/深色主题，Material3 风格
 */
private val LightColorScheme = lightColorScheme(
    primary = FlowerCyan,
    onPrimary = PaperWhite,
    primaryContainer = FlowerCyanLight.copy(alpha = 0.3f),
    onPrimaryContainer = FlowerCyanDark,
    secondary = Ochre,
    onSecondary = PaperWhite,
    tertiary = StoneGreen,
    onTertiary = PaperWhite,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = Color.White.copy(alpha = 0.7f),
    onSurfaceVariant = LightInk,
    outline = Ochre,
    error = ErrorRed,
    onError = PaperWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = FlowerCyanLight,
    onPrimary = DarkBackground,
    primaryContainer = FlowerCyanDark,
    onPrimaryContainer = FlowerCyanLight.copy(alpha = 0.7f),
    secondary = Gamboge,
    onSecondary = DarkBackground,
    tertiary = StoneGreen,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkText.copy(alpha = 0.6f),
    outline = Ochre.copy(alpha = 0.5f),
    error = ErrorRed,
    onError = PaperWhite
)

@Composable
fun LingjingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LingjingTypography,
        shapes = LingjingShapes,
        content = content
    )
}
