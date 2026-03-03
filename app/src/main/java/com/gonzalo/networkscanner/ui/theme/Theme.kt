package com.gonzalo.networkscanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Cyan500,
    onPrimary = SlateDark900,
    primaryContainer = Cyan700,
    onPrimaryContainer = Cyan400,
    secondary = SlateGray500,
    onSecondary = TextDarkPrimary,
    secondaryContainer = SlateDark700,
    onSecondaryContainer = SlateGray300,
    tertiary = Cyan400,
    onTertiary = SlateDark900,
    tertiaryContainer = Cyan600,
    onTertiaryContainer = Cyan400,
    error = Error500,
    onError = SlateDark900,
    errorContainer = Error500.copy(alpha = 0.2f),
    onErrorContainer = Error400,
    background = SlateDark900,
    onBackground = TextDarkPrimary,
    surface = SlateDark800,
    onSurface = TextDarkPrimary,
    surfaceVariant = SlateDark700,
    onSurfaceVariant = TextDarkSecondary,
    outline = SlateDark600,
    outlineVariant = SlateDark700,
    inverseSurface = SlateLight100,
    inverseOnSurface = SlateDark900,
    inversePrimary = Cyan600,
    surfaceTint = Cyan500
)

private val LightColorScheme = lightColorScheme(
    primary = Cyan600,
    onPrimary = Color.White,
    primaryContainer = Cyan400.copy(alpha = 0.2f),
    onPrimaryContainer = Cyan700,
    secondary = SlateGray500,
    onSecondary = Color.White,
    secondaryContainer = SlateLight200,
    onSecondaryContainer = SlateDark700,
    tertiary = Cyan500,
    onTertiary = Color.White,
    tertiaryContainer = Cyan400.copy(alpha = 0.2f),
    onTertiaryContainer = Cyan700,
    error = Error500,
    onError = Color.White,
    errorContainer = Error500.copy(alpha = 0.1f),
    onErrorContainer = Error500,
    background = SlateLight50,
    onBackground = TextLightPrimary,
    surface = Color.White,
    onSurface = TextLightPrimary,
    surfaceVariant = SlateLight100,
    onSurfaceVariant = TextLightSecondary,
    outline = SlateLight300,
    outlineVariant = SlateLight200,
    inverseSurface = SlateDark800,
    inverseOnSurface = SlateLight50,
    inversePrimary = Cyan400,
    surfaceTint = Cyan600
)

// Slight radius for modern technical aesthetic
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

@Composable
fun NetworkScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
