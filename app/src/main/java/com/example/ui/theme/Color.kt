package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Minimalism Colors (M3 Pastel & Clean)
val MinPrimaryLight = Color(0xFF6750A4)
val MinSecondaryLight = Color(0xFF625B71)
val MinTertiaryLight = Color(0xFF7D5260)
val MinBgLight = Color(0xFFFDF7FF)
val MinSurfaceLight = Color(0xFFFFFFFF)
val MinTextLight = Color(0xFF1D1B20)
val MinMutedLight = Color(0xFF49454F)
val MinOutlineLight = Color(0xFFCAC4D0)
val MinContainerLight = Color(0xFFE8DEF8)
val MinOnContainerLight = Color(0xFF21005D)

// Dark Minimalism Colors (M3 Cozy Dark)
val MinPrimaryDark = Color(0xFFD0BCFF)
val MinSecondaryDark = Color(0xFFCCC2DC)
val MinTertiaryDark = Color(0xFFEFB8C8)
val MinBgDark = Color(0xFF141218)
val MinSurfaceDark = Color(0xFF1D1B20)
val MinTextDark = Color(0xFFE6E1E5)
val MinMutedDark = Color(0xFFCAC4D0)
val MinOutlineDark = Color(0xFF938F99)
val MinContainerDark = Color(0xFF4F378B)

// Special Diagnostic Aura Weights (Text colors)
val AuraPositive = Color(0xFF1B5E20)       // Emerald: Positive / Productive
val AuraCalm = Color(0xFF0F766E)           // Teal: Serene / Calm
val AuraNeutral = Color(0xFF374151)        // Amber/Grey: Stable baseline
val AuraReflective = Color(0xFF1A237E)     // Indigo: Introspective
val AuraAnxious = Color(0xFF7A5C00)        // Orange: Tense / Elevated distress
val AuraOverwhelmed = Color(0xFFB71C1C)    // Rose: Acute pressure

// Soft Minimalist Mood Background colors (Dynamic pastels)
val AuraPositiveBg = Color(0xFFD1EEDC)
val AuraCalmBg = Color(0xFFE7F3F1)
val AuraReflectiveBg = Color(0xFFE0E2FF)
val AuraNeutralBg = Color(0xFFF0F0F0)
val AuraAnxiousBg = Color(0xFFFFF1D1)
val AuraOverwhelmedBg = Color(0xFFFFE5E5)

// The Personalization Accent Theme definitions matching user image palettes
enum class AccentTheme(
    val displayName: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
    val tertiaryLight: Color,
    val tertiaryDark: Color,
    val description: String
) {
    IVORY_MEMENTO(
        displayName = "Ivory Notebook",
        primaryLight = Color(0xFF1D1B20),    // Solid graphite black
        primaryDark = Color(0xFFE5E5E5),     // Cream-white graphite
        secondaryLight = Color(0xFFE17D5F),   // Terracotta orange clay
        secondaryDark = Color(0xFFE17D5F),    // Terracotta orange clay
        tertiaryLight = Color(0xFFE6C843),    // Amber honey yellow
        tertiaryDark = Color(0xFFE6C843),     // Amber honey yellow
        description = "Beige-sand notebook style from mockups, with hand-sketched boundaries and tactile cards."
    ),
    COSMIC_GLOW(
        displayName = "Cosmic Glow",
        primaryLight = Color(0xFFC2185B),   // Dark hot pink
        primaryDark = Color(0xFFFF4081),    // Saturated neon pink
        secondaryLight = Color(0xFF7B1FA2),  // Premium purple
        secondaryDark = Color(0xFFE040FB),   // Neon glowing magenta-purple
        tertiaryLight = Color(0xFF0097A7),   // Medical marine cyan
        tertiaryDark = Color(0xFF00E5FF),    // Neon glowing cyan
        description = "Deep dark space-inspired synthwave gradients and high-frequency neon glow overlays."
    ),
    CLINICAL_PASTEL(
        displayName = "Clinical Pastel",
        primaryLight = Color(0xFF3F51B5),   // Dynamic blue
        primaryDark = Color(0xFF9FA8DA),    // Light pastel indigo
        secondaryLight = Color(0xFF009688),  // Serene mint
        secondaryDark = Color(0xFF80CBC4),   // Soft sea mint
        tertiaryLight = Color(0xFFE91E63),   // Active warning contrast pink
        tertiaryDark = Color(0xFFF48FB1),    // Calm contrast pink
        description = "Clean, highly visible, crisp contrast palette designed for medical workspace scans."
    ),
    OCEAN_MINT(
        displayName = "Ocean Mint",
        primaryLight = Color(0xFF00796B),   // Cool dark teal
        primaryDark = Color(0xFF4DB6AC),    // Restorative glowing sea green
        secondaryLight = Color(0xFF0288D1),  // Serene ocean sky blue
        secondaryDark = Color(0xFF4FC3F7),   // Bright sky blue
        tertiaryLight = Color(0xFFE65100),   // Warm safety orange
        tertiaryDark = Color(0xFFFFB74D),    // Soft pastel orange
        description = "Coastal sea-mint hues, balanced dark marine greens, and calm water-well accents."
    ),
    VIBRANT_ORCHID(
        displayName = "Vibrant Orchid",
        primaryLight = Color(0xFF6750A4),   // Modern classic M3 rich purple
        primaryDark = Color(0xFFD0BCFF),    // M3 light lavender
        secondaryLight = Color(0xFF625B71),  // Restrained neutral grey-slate
        secondaryDark = Color(0xFFCCC2DC),   // Cozy slate purple-grey
        tertiaryLight = Color(0xFF7D5260),   // Soft classic redwood
        tertiaryDark = Color(0xFFEFB8C8),    // M3 pastel pinkish-gold
        description = "Standard dynamic orchid violet designed to maintain ultimate balanced digital health."
    );
}

