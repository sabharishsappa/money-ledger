package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class LedgerColorPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val navBar: Color,
    val divider: Color,
    val border: Color,
    val coral: Color,
    val coralDark: Color,
    val blue: Color,
    val blueDark: Color,
    val green: Color,
    val amber: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val DarkLedgerPalette = LedgerColorPalette(
    isDark = true,
    background = Color(0xFF1C1D21),
    surface = Color(0xFF222428),
    surfaceElevated = Color(0xFF282B31),
    surfaceHighlight = Color(0xFF31353D),
    navBar = Color(0xFF17181C),
    divider = Color(0xFF2D3037),
    border = Color(0xFF3A3E47),
    coral = Color(0xFFF06458),
    coralDark = Color(0xFFD94C40),
    blue = Color(0xFF5C9DF5),
    blueDark = Color(0xFF3F7EDB),
    green = Color(0xFF4CC99B),
    amber = Color(0xFFF4CE47),
    textPrimary = Color(0xFFF3F4F6),
    textSecondary = Color(0xFF9196A1),
    textMuted = Color(0xFF6A6F7A)
)

val LightLedgerPalette = LedgerColorPalette(
    isDark = false,
    background = Color(0xFFF3F5F8),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF6F8FB),
    surfaceHighlight = Color(0xFFE7ECF4),
    navBar = Color(0xFFFFFFFF),
    divider = Color(0xFFE2E6EE),
    border = Color(0xFFCDD4E0),
    coral = Color(0xFFDC4538),
    coralDark = Color(0xFFC2362A),
    blue = Color(0xFF2563EB),
    blueDark = Color(0xFF1D4ED8),
    green = Color(0xFF129468),
    amber = Color(0xFFB87900),
    textPrimary = Color(0xFF141821),
    textSecondary = Color(0xFF525B6C),
    textMuted = Color(0xFF7A8395)
)

// Static fallback color for non-Composable utility defaults
val DefaultLedgerCoral = Color(0xFFF06458)

val LocalLedgerColors = staticCompositionLocalOf { DarkLedgerPalette }

// Theme-reactive palette tokens (automatically switch between Dark & Light modes)
val LedgerBackground: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.background

val LedgerSurface: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.surface

val LedgerSurfaceElevated: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.surfaceElevated

val LedgerSurfaceHighlight: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.surfaceHighlight

val LedgerNavBar: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.navBar

val LedgerDivider: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.divider

val LedgerBorder: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.border

val LedgerCoral: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.coral

val LedgerCoralDark: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.coralDark

val LedgerBlue: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.blue

val LedgerBlueDark: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.blueDark

val LedgerGreen: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.green

val LedgerAmber: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.amber

val LedgerTextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.textPrimary

val LedgerTextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.textSecondary

val LedgerTextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalLedgerColors.current.textMuted

// Pie Chart & Category Badge Palette
val ChartColorPalette = listOf(
    Color(0xFFF06458), // Coral Red
    Color(0xFFF5974E), // Warm Orange
    Color(0xFFF4CE47), // Golden Yellow
    Color(0xFFA8D456), // Fresh Lime
    Color(0xFF4CC9B0), // Teal Mint
    Color(0xFF5C9DF5), // Azure Blue
    Color(0xFF7B8AF7), // Periwinkle Indigo
    Color(0xFFA875F2), // Soft Violet
    Color(0xFFE86B9D), // Rose Pink
    Color(0xFF63B8D6)  // Sky Cyan
)

val ChartColorHexes = listOf(
    "#F06458",
    "#F5974E",
    "#F4CE47",
    "#A8D456",
    "#4CC9B0",
    "#5C9DF5",
    "#7B8AF7",
    "#A875F2",
    "#E86B9D",
    "#63B8D6"
)
