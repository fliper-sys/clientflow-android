package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MinPrimaryDark,
    secondary = MinSecondaryDark,
    tertiary = MinTertiaryDark,
    background = MinBgDark,
    surface = MinSurfaceDark,
    onPrimary = MinBgDark,
    onSecondary = MinBgDark,
    onTertiary = MinBgDark,
    onBackground = MinTextDark,
    onSurface = MinTextDark,
    surfaceVariant = MinSurfaceDark,
    onSurfaceVariant = MinMutedDark,
    outline = MinOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = MinPrimaryLight,
    secondary = MinSecondaryLight,
    tertiary = MinTertiaryLight,
    background = MinBgLight,
    surface = MinSurfaceLight,
    onPrimary = MinSurfaceLight,
    onSecondary = MinSurfaceLight,
    onTertiary = clinicalBgLightCheck(),
    onBackground = MinTextLight,
    onSurface = MinTextLight,
    surfaceVariant = MinSurfaceLight,
    onSurfaceVariant = MinMutedLight,
    outline = MinOutlineLight
)

private fun clinicalBgLightCheck() = androidx.compose.ui.graphics.Color.White

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
