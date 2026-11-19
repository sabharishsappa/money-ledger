package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerCoral,
    onPrimary = LedgerTextPrimary,
    primaryContainer = LedgerCoralDark,
    onPrimaryContainer = LedgerTextPrimary,
    secondary = LedgerBlue,
    onSecondary = LedgerTextPrimary,
    secondaryContainer = LedgerBlueDark,
    onSecondaryContainer = LedgerTextPrimary,
    tertiary = LedgerAmber,
    onTertiary = LedgerBackground,
    background = LedgerBackground,
    onBackground = LedgerTextPrimary,
    surface = LedgerSurface,
    onSurface = LedgerTextPrimary,
    surfaceVariant = LedgerSurfaceElevated,
    onSurfaceVariant = LedgerTextSecondary,
    outline = LedgerBorder,
    outlineVariant = LedgerDivider,
    error = LedgerCoral,
    onError = LedgerTextPrimary
)

@Composable
fun MoneyLedgerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LedgerDarkColorScheme,
        typography = Typography,
        content = content
    )
}
