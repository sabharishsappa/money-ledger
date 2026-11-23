package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerGreen
import com.example.ui.theme.LedgerMotion
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerSurfaceHighlight
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.ui.theme.LocalAppearancePreferences
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.theme.bodyFontFamilyFor
import com.example.ui.theme.displayFontFamilyFor
import com.example.util.AppTextFormat
import com.example.util.AppTextSize
import com.example.util.AppThemeMode
import com.example.util.AppearanceManager

@Composable
fun AppearanceSettingsScreen(
    currencySymbol: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appearance = LocalAppearancePreferences.current

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("appearance_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LedgerTextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Theme & Display",
                    style = MaterialTheme.typography.titleLarge,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = appearance.summaryLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(color = LedgerDivider)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Preview Card
                AppearanceLivePreviewCard(currencySymbol = currencySymbol)

                // 1. Theme Selector (System / Light / Dark)
                AppearanceSectionHeader(
                    icon = Icons.Filled.Palette,
                    title = "Theme",
                    subtitle = "Choose Light, Dark, or follow your device theme"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = appearance.themeMode == mode
                        ThemeModeCard(
                            mode = mode,
                            icon = when (mode) {
                                AppThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                AppThemeMode.LIGHT -> Icons.Filled.LightMode
                                AppThemeMode.DARK -> Icons.Filled.DarkMode
                            },
                            selected = selected,
                            onClick = { AppearanceManager.setThemeMode(context, mode) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                // 2. Text Size Selector (Small / Medium / Large)
                AppearanceSectionHeader(
                    icon = Icons.Filled.FormatSize,
                    title = "Text Size",
                    subtitle = "Adjust reading size across entries, accounts & reports"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppTextSize.entries.forEach { size ->
                        val selected = appearance.textSize == size
                        TextSizeCard(
                            size = size,
                            selected = selected,
                            onClick = { AppearanceManager.setTextSize(context, size) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_size_${size.name.lowercase()}")
                        )
                    }
                }

                // 3. Text Format (Font Style) Selector
                AppearanceSectionHeader(
                    icon = Icons.Filled.TextFields,
                    title = "Text Format",
                    subtitle = "Pick a typeface for headings, notes & ledger figures"
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextFormat.entries.forEach { format ->
                        val selected = appearance.textFormat == format
                        TextFormatOptionCard(
                            format = format,
                            currencySymbol = currencySymbol,
                            selected = selected,
                            onClick = { AppearanceManager.setTextFormat(context, format) },
                            modifier = Modifier.testTag("text_format_${format.name.lowercase()}")
                        )
                    }
                }

                // Reset Button
                val isDefault = appearance.themeMode == AppThemeMode.DARK &&
                    appearance.textSize == AppTextSize.MEDIUM &&
                    appearance.textFormat == AppTextFormat.MODERN
                if (!isDefault) {
                    OutlinedButton(
                        onClick = { AppearanceManager.resetDefaults(context) },
                        border = BorderStroke(1.dp, LedgerBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("appearance_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "Reset appearance defaults",
                            tint = LedgerTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset to Default Style",
                            style = MaterialTheme.typography.labelLarge,
                            color = LedgerTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AppearanceLivePreviewCard(currencySymbol: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("appearance_live_preview_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerCoral,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LedgerSurfaceHighlight
                ) {
                    Text(
                        text = "Monthly Summary",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextSecondary
                    )
                    Text(
                        text = "${currencySymbol}85,000",
                        style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = LedgerBlue
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextSecondary
                    )
                    Text(
                        text = "${currencySymbol}12,450",
                        style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = LedgerCoral
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextSecondary
                    )
                    Text(
                        text = "${currencySymbol}72,550",
                        style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = LedgerGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = LedgerDivider)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(LedgerAmber)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Groceries & Household",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = LedgerTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "HDFC Bank • #Personal",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${currencySymbol}2,450",
                    style = TabularAmountStyle,
                    color = LedgerCoral
                )
            }
        }
    }
}

@Composable
private fun AppearanceSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LedgerCoral,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LedgerTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTextSecondary
            )
        }
    }
}

@Composable
private fun ThemeModeCard(
    mode: AppThemeMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) LedgerCoral.copy(alpha = 0.14f) else LedgerSurface,
        animationSpec = LedgerMotion.microTween(110),
        label = "theme_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) LedgerCoral else LedgerBorder,
        animationSpec = LedgerMotion.microTween(110),
        label = "theme_card_border"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.label,
                tint = if (selected) LedgerCoral else LedgerTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) LedgerCoral else LedgerTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TextSizeCard(
    size: AppTextSize,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) LedgerBlue.copy(alpha = 0.14f) else LedgerSurface,
        animationSpec = LedgerMotion.microTween(110),
        label = "size_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) LedgerBlue else LedgerBorder,
        animationSpec = LedgerMotion.microTween(110),
        label = "size_card_border"
    )
    val sampleSp = when (size) {
        AppTextSize.SMALL -> 14.sp
        AppTextSize.MEDIUM -> 18.sp
        AppTextSize.LARGE -> 22.sp
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.height(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    fontSize = sampleSp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) LedgerBlue else LedgerTextPrimary
                )
            }
            Text(
                text = size.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) LedgerBlue else LedgerTextPrimary,
                maxLines = 1
            )
            Text(
                text = size.percentLabel,
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TextFormatOptionCard(
    format: AppTextFormat,
    currencySymbol: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) LedgerSurfaceHighlight else LedgerSurface,
        animationSpec = LedgerMotion.microTween(110),
        label = "format_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) LedgerCoral else LedgerDivider,
        animationSpec = LedgerMotion.microTween(110),
        label = "format_card_border"
    )
    val displayFont = displayFontFamilyFor(format)
    val bodyFont = bodyFontFamilyFor(format)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = format.label,
                    fontFamily = displayFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (selected) LedgerCoral else LedgerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = format.subtitle,
                    fontFamily = bodyFont,
                    fontSize = 12.sp,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${currencySymbol}1,25,000",
                fontFamily = bodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = LedgerTextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) LedgerCoral else LedgerSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
