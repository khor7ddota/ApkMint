package com.apkmint.creator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF34D399),
    tertiary = Color(0xFF818CF8),
    background = MintDarkBg,
    surface = MintDarkSurface,
    surfaceVariant = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = MintOnContainer,
    secondary = EmeraldSecondary,
    secondaryContainer = EmeraldContainer,
    tertiary = IndigoTertiary,
    tertiaryContainer = IndigoContainer,
    background = MintLightBg,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9)
)

@Composable
fun ApkMintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
