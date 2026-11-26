package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoxDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBg,
    primaryContainer = Color(0xFF0E3A4A),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = ElectricIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF272959),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = EmeraldOk,
    onTertiary = ObsidianBg,
    error = CoralStop,
    onError = Color.White,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = SlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = SlateElevated,
    outline = SlateBorder
)

private val VoxLightColorScheme = lightColorScheme(
    primary = DeepCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF083344),
    secondary = ElectricIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = EmeraldOk,
    onTertiary = Color.White,
    error = CoralStop,
    onError = Color.White,
    background = CloudBg,
    onBackground = TextPrimaryLight,
    surface = CloudSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CloudCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = CloudCard,
    outline = CloudBorder
)

@Composable
fun VoxDeckTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VoxDarkColorScheme else VoxLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VoxTypography,
        content = content
    )
}
