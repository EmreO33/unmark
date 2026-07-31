package com.unmark.eraser.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = UnmarkPurple,
    secondary = UnmarkPurpleDark,
    background = UnmarkBackgroundDark,
    surface = UnmarkSurfaceDark
)

private val LightColors = lightColorScheme(
    primary = UnmarkPurple,
    secondary = UnmarkPurpleDark,
    background = UnmarkBackgroundLight,
    surface = UnmarkSurfaceLight
)

@Composable
fun UnmarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = UnmarkTypography,
        content = content
    )
}
