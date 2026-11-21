package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FootballHubDarkColorScheme =
  darkColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = TextPrimary,
    secondary = GoldTrophy,
    onSecondary = ObsidianBlack,
    secondaryContainer = ObsidianCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldOpen,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    outlineVariant = ObsidianCardElevated,
    error = CrimsonGlow,
    onError = Color.White,
  )

@Composable
fun FootballHubTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = FootballHubDarkColorScheme,
    typography = FootballHubTypography,
    content = content,
  )
}
