package com.gostudios.console.cloud

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GoBlue = Color(0xFF0066FF)
private val GoBlueLight = Color(0xFF3385FF)
private val GoBlueDark = Color(0xFF0044CC)
private val GoNavy = Color(0xFF0D1B2A)
private val GoNavyMid = Color(0xFF1B2838)
private val GoNavyLight = Color(0xFF243447)
private val GoPurple = Color(0xFF1A1040)
private val GoSurface = Color(0xFF162032)
private val GoSurfaceVariant = Color(0xFF1E2D42)
private val GoOnSurface = Color(0xFFE0E6ED)
private val GoOnSurfaceVariant = Color(0xFF8899AA)
private val GoError = Color(0xFFFF5449)

private val GoConsoleColorScheme = darkColorScheme(
    primary = GoBlue,
    onPrimary = Color.White,
    primaryContainer = GoBlueDark,
    onPrimaryContainer = GoNavy,
    secondary = GoBlueLight,
    onSecondary = GoNavy,
    secondaryContainer = GoNavyLight,
    onSecondaryContainer = GoOnSurface,
    tertiary = Color(0xFF7B61FF),
    onTertiary = Color.White,
    tertiaryContainer = GoPurple,
    onTertiaryContainer = GoOnSurface,
    background = GoNavy,
    onBackground = GoOnSurface,
    surface = GoSurface,
    onSurface = GoOnSurface,
    surfaceVariant = GoSurfaceVariant,
    onSurfaceVariant = GoOnSurfaceVariant,
    error = GoError,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = GoOnSurfaceVariant,
    outlineVariant = GoNavyLight,
)

@Composable
fun GoConsoleTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GoNavy.toArgb()
            window.navigationBarColor = GoNavy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = GoConsoleColorScheme,
        content = content
    )
}