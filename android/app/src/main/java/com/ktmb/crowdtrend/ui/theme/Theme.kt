package com.ktmb.crowdtrend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = TealOnTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = TealOnTertiaryContainer,
    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = SurfaceLight,
    onBackground = Color(0xFF0C1A1F),
    surface = SurfaceContainerLight,
    onSurface = Color(0xFF0C1A1F),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3F4948),
    outline = OutlineLight,
    outlineVariant = Color(0xFFC4CFCD),
    inverseSurface = Color(0xFF2E3332),
    inverseOnSurface = Color(0xFFF0F2F1),
    inversePrimary = Teal80,
)

@Composable
fun KtmbTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = KtmbTypography,
        content = content,
    )
}
