package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.util.AppThemeMode
import com.example.util.AppearancePreferences

val LocalAppearancePreferences = staticCompositionLocalOf { AppearancePreferences() }

private fun buildDarkScheme(p: LedgerColorPalette) = darkColorScheme(
    primary = p.coral,
    onPrimary = Color.White,
    primaryContainer = p.coralDark,
    onPrimaryContainer = Color.White,
    secondary = p.blue,
    onSecondary = Color.White,
    secondaryContainer = p.blueDark,
    onSecondaryContainer = Color.White,
    tertiary = p.amber,
    onTertiary = p.background,
    background = p.background,
    onBackground = p.textPrimary,
    surface = p.surface,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceElevated,
    onSurfaceVariant = p.textSecondary,
    outline = p.border,
    outlineVariant = p.divider,
    error = p.coral,
    onError = Color.White
)

private fun buildLightScheme(p: LedgerColorPalette) = lightColorScheme(
    primary = p.coral,
    onPrimary = Color.White,
    primaryContainer = p.coral.copy(alpha = 0.14f),
    onPrimaryContainer = p.coralDark,
    secondary = p.blue,
    onSecondary = Color.White,
    secondaryContainer = p.blue.copy(alpha = 0.14f),
    onSecondaryContainer = p.blueDark,
    tertiary = p.amber,
    onTertiary = Color.White,
    background = p.background,
    onBackground = p.textPrimary,
    surface = p.surface,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceElevated,
    onSurfaceVariant = p.textSecondary,
    outline = p.border,
    outlineVariant = p.divider,
    error = p.coral,
    onError = Color.White
)

private val LedgerDarkColorScheme = buildDarkScheme(DarkLedgerPalette)
private val LedgerLightColorScheme = buildLightScheme(LightLedgerPalette)

@Composable
fun MoneyLedgerTheme(
    appearance: AppearancePreferences = AppearancePreferences(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appearance.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }

    val palette = if (useDark) DarkLedgerPalette else LightLedgerPalette
    val colorScheme = if (useDark) LedgerDarkColorScheme else LedgerLightColorScheme
    val typography = remember(appearance.textFormat) {
        buildLedgerTypography(appearance.textFormat)
    }
    val tabularStyle = remember(appearance.textFormat) {
        buildTabularAmountStyle(appearance.textFormat)
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, appearance.textSize) {
        Density(
            density = baseDensity.density,
            fontScale = (baseDensity.fontScale * appearance.textSize.scale).coerceIn(0.75f, 1.8f)
        )
    }

    CompositionLocalProvider(
        LocalAppearancePreferences provides appearance,
        LocalLedgerColors provides palette,
        LocalTabularAmountStyle provides tabularStyle,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
