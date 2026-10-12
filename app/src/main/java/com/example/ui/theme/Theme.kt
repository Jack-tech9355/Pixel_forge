package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ForgePurple,
    onPrimary = ForgeTextPrimary,
    primaryContainer = ForgePurpleDark,
    onPrimaryContainer = ForgePurpleLight,
    secondary = ForgeCyan,
    onSecondary = ForgeBg,
    secondaryContainer = ForgeCyanDark,
    onSecondaryContainer = ForgeCyanLight,
    tertiary = ForgeEmerald,
    onTertiary = ForgeBg,
    background = ForgeBg,
    onBackground = ForgeTextPrimary,
    surface = ForgeSurface,
    onSurface = ForgeTextPrimary,
    surfaceVariant = ForgeSurfaceElevated,
    onSurfaceVariant = ForgeTextSecondary,
    outline = ForgeBorder,
    outlineVariant = ForgeSurfaceHighlight,
    error = ForgeRose,
    onError = ForgeTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ForgePurple,
    onPrimary = ForgeLightSurface,
    primaryContainer = ForgePurpleLight,
    onPrimaryContainer = ForgePurpleDark,
    secondary = ForgeCyanDark,
    onSecondary = ForgeLightSurface,
    secondaryContainer = ForgeCyanLight,
    onSecondaryContainer = ForgeCyanDark,
    tertiary = ForgeEmerald,
    onTertiary = ForgeLightSurface,
    background = ForgeLightBg,
    onBackground = ForgeLightTextPrimary,
    surface = ForgeLightSurface,
    onSurface = ForgeLightTextPrimary,
    surfaceVariant = ForgeLightSurfaceElevated,
    onSurfaceVariant = ForgeLightTextSecondary,
    outline = ForgeLightBorder,
    error = ForgeRose,
    onError = ForgeLightSurface
)

@Composable
fun PixelForgeTheme(
    darkTheme: Boolean = true, // Default to retro dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                it.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
