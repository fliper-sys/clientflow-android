package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentTheme: AccentTheme = AccentTheme.COSMIC_GLOW,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (accentTheme == AccentTheme.IVORY_MEMENTO) {
        if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFF6F0E5),       // Cream off-white
                secondary = Color(0xFFE17D5F),     // Clay orange
                tertiary = Color(0xFFE6C843),      // Honey Yellow
                background = Color(0xFF0F0F11),    // Warm dark charcoal
                surface = Color(0xFF18181D),       // Muted charcoal card surface
                onPrimary = Color(0xFF0F0F11),
                onSecondary = Color(0xFF1D1B20),
                onTertiary = Color(0xFF1D1B20),
                onBackground = Color(0xFFF6F0E5),
                onSurface = Color(0xFFF6F0E5),
                surfaceVariant = Color(0xFF24242B),
                onSurfaceVariant = Color(0xFFD4CFC7),
                outline = Color(0xFF383842)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF1D1B20),       // Solid graphite black
                secondary = Color(0xFFE17D5F),     // Terracotta orange
                tertiary = Color(0xFFE6C843),      // Honey yellow
                background = Color(0xFFF6F0E5),    // Warm ivory mockup beige background
                surface = Color(0xFFFAF6EE),       // Warm card container beige
                onPrimary = Color(0xFFFAF6EE),
                onSecondary = Color(0xFFFAF6EE),
                onTertiary = Color(0xFFFAF6EE),
                onBackground = Color(0xFF1D1B20),
                onSurface = Color(0xFF1D1B20),
                surfaceVariant = Color(0xFFEFE8DA),
                onSurfaceVariant = Color(0xFF1D1B20),
                outline = Color(0xFF1D1B20)         // Bold black outline matching sketch art
            )
        }
    } else if (darkTheme) {
        darkColorScheme(
            primary = accentTheme.primaryDark,
            secondary = accentTheme.secondaryDark,
            tertiary = accentTheme.tertiaryDark,
            background = Color(0xFF0C0913), // Cosmic premium dark matching image 1
            surface = Color(0xFF15111F),    // Custom layered card dark gray
            onPrimary = Color(0xFF0C0913),
            onSecondary = Color(0xFF0C0913),
            onTertiary = Color(0xFF0C0913),
            onBackground = Color(0xFFF3EDF7),
            onSurface = Color(0xFFF3EDF7),
            surfaceVariant = Color(0xFF221A30),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF322E3D)
        )
    } else {
        lightColorScheme(
            primary = accentTheme.primaryLight,
            secondary = accentTheme.secondaryLight,
            tertiary = accentTheme.tertiaryLight,
            background = Color(0xFFFDF7FF), // Elegant clinical white cream matching image 2
            surface = Color(0xFFFFFFFF),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onTertiary = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1D1B20),
            onSurface = Color(0xFF1D1B20),
            surfaceVariant = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFFCAC4D0)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

