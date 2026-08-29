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

private val GoCyan = Color(0xFF00C9DB)
private val GoCyanLight = Color(0xFF33D4E3)
private val GoCyanDark = Color(0xFF009AA8)
private val GoBlue = Color(0xFF0D1B2A)
private val GoBlueMid = Color(0xFF1B2838)
private val GoBlueLight = Color(0xFF243447)
private val GoPurple = Color(0xFF1A1040)
private val GoSurface = Color(0xFF162032)
private val GoSurfaceVariant = Color(0xFF1E2D42)
private val GoOnSurface = Color(0xFFE0E6ED)
private val GoOnSurfaceVariant = Color(0xFF8899AA)
private val GoError = Color(0xFFFF5449)

private val GoConsoleColorScheme = darkColorScheme(
    primary = GoCyan,
    onPrimary = GoBlue,
    primaryContainer = GoCyanDark,
    onPrimaryContainer = GoBlue,
    secondary = GoCyanLight,
    onSecondary = GoBlue,
    secondaryContainer = GoBlueLight,
    onSecondaryContainer = GoOnSurface,
    tertiary = Color(0xFF7B61FF),
    onTertiary = GoBlue,
    tertiaryContainer = GoPurple,
    onTertiaryContainer = GoOnSurface,
    background = GoBlue,
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
    outlineVariant = GoBlueLight,
)

@Composable
fun GoConsoleTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GoBlue.toArgb()
            window.navigationBarColor = GoBlue.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = GoConsoleColorScheme,
        content = content
    )
}
