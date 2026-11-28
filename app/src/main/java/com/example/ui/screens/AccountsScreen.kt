package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.AccountNature
import com.example.data.LedgerTransaction
import com.example.data.TransactionType
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerMotion
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerSurfaceHighlight
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.ui.theme.TabularAmountStyle
import com.example.util.IndianNumberVisualTransformation
import com.example.util.LedgerFormatters
import com.example.viewmodel.AccountWithBalance
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

private data class AccountsTableWidths(
    val nameColWidth: Dp,
    val tagColWidth: Dp,
    val totalColWidth: Dp
)

private data class NatureGroupSection(
    val id: String,
    val nature: AccountNature,
    val title: String,
    val sections: List<com.example.viewmodel.AccountGroupSection>,
    val subtotal: Double,
    val tagSubtotals: Map<String, Double>,
    val untaggedSubtotal: Double
)

@Composable
fun AccountsScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onSelectAccount: (Account) -> Unit,
    onOpenAccountsSetting: () -> Unit,
    onMassUploadLedgers: () -> Unit,
    onCreateCategoryLedger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol
    var showAddDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSummaryBreakdown by remember { mutableStateOf(false) }

    // Level 1 (Main Groups) are expanded by default; Level 2 (Sub Groups) are collapsed by default
    // so individual ledgers are hidden until the user taps a group or its expand button.
    var collapsedMainGroupIds by remember { mutableStateOf(setOf<String>()) }
    var expandedGroupIds by remember { mutableStateOf(setOf<String>()) }
    // Income & Expenses columnar P&L sections (collapsed by default; tap header to inspect categories/ledgers)
    var expandedPnlSectionIds by remember { mutableStateOf(setOf<String>()) }
    var expandedPnlCategoryKeys by remember { mutableStateOf(setOf<String>()) }

    val taggings = bundle.allTaggings
    val showTagSplitColumns = taggings.isNotEmpty()
    val includeUntaggedCol = showTagSplitColumns && (bundle.hasAnyUntaggedBalance || bundle.hasAnyCategoryUntaggedAmount)
    val grandTagTotals = bundle.grandTagTotals
    val grandUntaggedTotal = bundle.grandUntaggedTotal
    val sharedHScroll = rememberScrollState()

    val pnlSections = remember(bundle.incomeTagSplitSection, bundle.expenseTagSplitSection) {
        listOf(bundle.incomeTagSplitSection, bundle.expenseTagSplitSection).filter { it.rows.isNotEmpty() }
    }

    val nonEmptySections = remember(bundle.accountSections) {
        bundle.accountSections.filter { it.accounts.isNotEmpty() }
    }

    val natureSections = remember(bundle.accountHierarchy, taggings) {
        bundle.accountHierarchy.mapNotNull { entry ->
            val matching = entry.subGroups.filter { it.accounts.isNotEmpty() }
            if (matching.isEmpty()) {
                null
            } else {
                NatureGroupSection(
                    id = entry.mainGroup.id,
                    nature = entry.mainGroup.accountNature(),
                    title = entry.mainGroup.name,
                    sections = matching,
                    subtotal = matching.sumOf { it.subtotal },
                    tagSubtotals = taggings.associateWith { tag ->
                        matching.sumOf { sec -> sec.tagSubtotals[tag] ?: 0.0 }
                    },
                    untaggedSubtotal = matching.sumOf { sec -> sec.untaggedSubtotal }
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleLarge,
                color = LedgerTextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showSummaryBreakdown = !showSummaryBreakdown },
                modifier = Modifier.testTag("accounts_trend_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ShowChart,
                    contentDescription = "Net worth ratio",
                    tint = if (showSummaryBreakdown) LedgerBlue else LedgerTextSecondary
                )
            }
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("accounts_add_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add account",
                    tint = LedgerTextPrimary
                )
            }
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.testTag("accounts_more_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Account options",
                        tint = LedgerTextSecondary
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    containerColor = LedgerSurfaceElevated
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (expandedGroupIds.size < nonEmptySections.size) {
                                    "Expand All Groups"
                                } else {
                                    "Collapse All Groups"
                                },
                                color = LedgerTextPrimary
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            if (expandedGroupIds.size < nonEmptySections.size) {
                                collapsedMainGroupIds = emptySet()
                                expandedGroupIds = nonEmptySections.map { it.group.id }.toSet()
                            } else {
                                expandedGroupIds = emptySet()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Account Settings", color = LedgerTextPrimary) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenAccountsSetting()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Create Expense / Income Ledger",
                                color = LedgerCoral,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onCreateCategoryLedger()
                        },
                        modifier = Modifier.testTag("accounts_create_category_ledger_item")
                    )
                    DropdownMenuItem(
                        text = { Text("Mass Upload Ledgers", color = LedgerBlue, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showOverflowMenu = false
                            onMassUploadLedgers()
                        },
                        modifier = Modifier.testTag("accounts_mass_upload_menu_item")
                    )
                }
            }
        }

        // 3-Column Assets / Liabilities / Total Strip (No decimals)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryMetricColumn(
                label = "Assets",
                value = LedgerFormatters.formatWholeAmount(bundle.totalAssets, symbol, includeSymbol = false),
                valueColor = if (bundle.totalAssets.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricColumn(
                label = "Liabilities",
                value = LedgerFormatters.formatWholeAmount(bundle.totalLiabilities, symbol, includeSymbol = false),
                valueColor = LedgerCoral,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricColumn(
                label = "Total",
                value = LedgerFormatters.formatWholeAmount(bundle.netTotal, symbol, includeSymbol = false),
                valueColor = if (bundle.netTotal.roundToLong() < 0L) LedgerCoral else LedgerTextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(
            visible = showSummaryBreakdown,
            enter = LedgerMotion.expandEnter(),
            exit = LedgerMotion.shrinkExit()
        ) {
            val posAssets = bundle.totalAssets.coerceAtLeast(0.0)
            val posLiabilities = bundle.totalLiabilities.coerceAtLeast(0.0)
            val totalMagnitude = (posAssets + posLiabilities).coerceAtLeast(1.0)
            val targetAssetFraction = (posAssets / totalMagnitude).toFloat().coerceIn(0f, 1f)
            val animatedAssetFraction by animateFloatAsState(
                targetValue = targetAssetFraction,
                animationSpec = LedgerMotion.smoothSpring(),
                label = "accounts_asset_ratio"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(LedgerCoral, RoundedCornerShape(4.dp))
                ) {
                    if (animatedAssetFraction > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedAssetFraction)
                                .height(8.dp)
                                .background(LedgerBlue, RoundedCornerShape(4.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Assets ${(targetAssetFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerBlue
                    )
                    Text(
                        text = "Liabilities ${((1f - targetAssetFraction) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerCoral
                    )
                }
            }
        }

        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

        // Hierarchical Group-Level Accounts List (Assets/Liabilities -> Account Groups -> Expandable Ledgers)
        if (natureSections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = LedgerTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No accounts yet", style = MaterialTheme.typography.titleMedium, color = LedgerTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Add bank accounts, credit cards, loans, or investments to track your net worth.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral)
                    ) {
                        Text("Add Account")
                    }
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val baseNameW = (maxWidth * 0.39f).coerceIn(124.dp, 210.dp)
                val availRightW = (maxWidth - baseNameW - 1.dp).coerceAtLeast(160.dp)
                val tagColCount = taggings.size + if (includeUntaggedCol) 1 else 0
                val minTagW = (maxWidth * 0.24f).coerceIn(88.dp, 124.dp)
                val minTotalW = (maxWidth * 0.27f).coerceIn(96.dp, 134.dp)
                val naturalRightW = minTagW * tagColCount + minTotalW
                val widths = if (showTagSplitColumns && naturalRightW < availRightW && tagColCount > 0) {
                    val scale = (availRightW.value / naturalRightW.value)
                    AccountsTableWidths(
                        nameColWidth = baseNameW,
                        tagColWidth = (minTagW.value * scale).dp,
                        totalColWidth = (minTotalW.value * scale).dp
                    )
                } else {
                    AccountsTableWidths(
                        nameColWidth = baseNameW,
                        tagColWidth = minTagW,
                        totalColWidth = minTotalW
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Sticky Column Header + Tagging-Wise Totals Row on Top
                    if (showTagSplitColumns) {
                        TagSplitHeaderRow(
                            taggings = taggings,
                            includeUntagged = includeUntaggedCol,
                            scrollState = sharedHScroll,
                            widths = widths
                        )
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                        TagSplitDataRow(
                            label = "Total",
                            subtitle = "Tagging-wise",
                            taggings = taggings,
                            tagValues = grandTagTotals,
                            includeUntagged = includeUntaggedCol,
                            untaggedValue = grandUntaggedTotal,
                            totalValue = bundle.netTotal,
                            symbol = symbol,
                            scrollState = sharedHScroll,
                            widths = widths,
                            rowBackground = LedgerSurfaceHighlight.copy(alpha = 0.55f),
                            rowTier = AccountRowTier.GRAND_TOTAL,
                            modifier = Modifier.testTag("accounts_grand_total_row"),
                            onClick = null
                        )
                        HorizontalDivider(color = LedgerBorder, thickness = 1.dp)
                    }

                    val assetAccentBlue = LedgerBlue
                    val liabAccentCoral = LedgerCoral

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        natureSections.forEach { natureSection ->
                            val isNatureExpanded = natureSection.id !in collapsedMainGroupIds
                            val accentColor = if (natureSection.nature == AccountNature.ASSET) assetAccentBlue else liabAccentCoral
                            val toggleNature = {
                                collapsedMainGroupIds = if (isNatureExpanded) {
                                    collapsedMainGroupIds + natureSection.id
                                } else {
                                    collapsedMainGroupIds - natureSection.id
                                }
                            }

                            // Level 1: Main Group Header
                            item(key = "nature_hdr_${natureSection.id}", contentType = "nature_hdr") {
                                val groupCountText = if (natureSection.sections.size == 1) "1 sub group" else "${natureSection.sections.size} sub groups"
                                Column(modifier = Modifier.animateItem()) {
                                    if (showTagSplitColumns) {
                                        TagSplitDataRow(
                                            label = natureSection.title,
                                            subtitle = groupCountText,
                                            taggings = taggings,
                                            tagValues = natureSection.tagSubtotals,
                                            includeUntagged = includeUntaggedCol,
                                            untaggedValue = natureSection.untaggedSubtotal,
                                            totalValue = natureSection.subtotal,
                                            symbol = symbol,
                                            scrollState = sharedHScroll,
                                            widths = widths,
                                            rowBackground = LedgerSurfaceElevated,
                                            rowTier = AccountRowTier.NATURE_HEADER,
                                            isExpanded = isNatureExpanded,
                                            accentColor = accentColor,
                                            modifier = Modifier.testTag("account_nature_header_${natureSection.id}"),
                                            onClick = toggleNature
                                        )
                                    } else {
                                        val chevronRot by animateFloatAsState(
                                            targetValue = if (isNatureExpanded) 90f else 0f,
                                            animationSpec = LedgerMotion.microTween(),
                                            label = "nature_chevron_${natureSection.id}"
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .background(LedgerSurfaceElevated)
                                                .clickable(onClick = toggleNature)
                                                .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                                                .testTag("account_nature_header_${natureSection.id}"),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(20.dp)
                                                        .background(accentColor, RoundedCornerShape(2.dp))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = if (isNatureExpanded) "Collapse ${natureSection.title}" else "Expand ${natureSection.title}",
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp).rotate(chevronRot)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        text = natureSection.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = LedgerTextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = groupCountText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = LedgerTextMuted
                                                    )
                                                }
                                            }
                                            Text(
                                                text = LedgerFormatters.formatSignedWhole(natureSection.subtotal, symbol),
                                                style = TabularAmountStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
                                                color = if (natureSection.subtotal.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
                                }
                            }

                            // Level 2: Account Groups (Bank Accounts, Cash, Credit Cards, Loans, etc.)
                            if (isNatureExpanded) {
                                natureSection.sections.forEach { section ->
                                    val isGroupExpanded = section.group.id in expandedGroupIds
                                    val ledgerCountText = if (section.accounts.size == 1) "1 ledger" else "${section.accounts.size} ledgers"
                                    val toggleGroup = {
                                        expandedGroupIds = if (isGroupExpanded) {
                                            expandedGroupIds - section.group.id
                                        } else {
                                            expandedGroupIds + section.group.id
                                        }
                                    }

                                    item(key = "grp_hdr_${section.group.id}", contentType = "group_hdr") {
                                        Column(modifier = Modifier.animateItem()) {
                                            if (showTagSplitColumns) {
                                                TagSplitDataRow(
                                                    label = section.group.name,
                                                    subtitle = ledgerCountText,
                                                    taggings = taggings,
                                                    tagValues = section.tagSubtotals,
                                                    includeUntagged = includeUntaggedCol,
                                                    untaggedValue = section.untaggedSubtotal,
                                                    totalValue = section.subtotal,
                                                    symbol = symbol,
                                                    scrollState = sharedHScroll,
                                                    widths = widths,
                                                    rowBackground = LedgerSurface,
                                                    rowTier = AccountRowTier.ACCOUNT_GROUP,
                                                    isExpanded = isGroupExpanded,
                                                    modifier = Modifier.testTag("account_group_header_${section.group.id}"),
                                                    onClick = toggleGroup
                                                )
                                            } else {
                                                val groupChevronRot by animateFloatAsState(
                                                    targetValue = if (isGroupExpanded) 90f else 0f,
                                                    animationSpec = LedgerMotion.microTween(),
                                                    label = "grp_chevron_${section.group.id}"
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 48.dp)
                                                        .background(LedgerSurface)
                                                        .clickable(onClick = toggleGroup)
                                                        .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                                                        .testTag("account_group_header_${section.group.id}"),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f).padding(end = 10.dp)
                                                    ) {
                                                        Surface(
                                                            onClick = toggleGroup,
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isGroupExpanded) LedgerSurfaceHighlight else LedgerSurfaceElevated,
                                                            border = BorderStroke(0.5.dp, LedgerBorder),
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .testTag("account_group_expand_${section.group.id}")
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                                    contentDescription = if (isGroupExpanded) {
                                                                        "Collapse ${section.group.name}"
                                                                    } else {
                                                                        "Expand ${section.group.name}"
                                                                    },
                                                                    tint = if (isGroupExpanded) LedgerTextPrimary else LedgerTextSecondary,
                                                                    modifier = Modifier.size(18.dp).rotate(groupChevronRot)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(
                                                                text = section.group.name,
                                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                                                color = LedgerTextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = ledgerCountText,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = LedgerTextMuted
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = LedgerFormatters.formatSignedWhole(section.subtotal, symbol),
                                                        style = TabularAmountStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                                        color = if (section.subtotal.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                        }
                                    }

                                    // Level 3: Individual Ledgers (Hidden by default; revealed only when group is expanded)
                                    if (isGroupExpanded) {
                                        items(
                                            items = section.accounts,
                                            key = { it.account.id },
                                            contentType = { "ledger_item" }
                                        ) { item: AccountWithBalance ->
                                            val accountSubtitle = remember(item.account.includeInNetWorth, item.account.note) {
                                                buildString {
                                                    if (!item.account.includeInNetWorth) append("Excl. Net Worth")
                                                    if (!item.account.note.isNullOrBlank()) {
                                                        if (isNotEmpty()) append(" • ")
                                                        append(item.account.note)
                                                    }
                                                }.takeIf { it.isNotEmpty() }
                                            }

                                            Column(modifier = Modifier.animateItem()) {
                                                if (showTagSplitColumns) {
                                                    TagSplitDataRow(
                                                        label = item.account.name,
                                                        subtitle = accountSubtitle,
                                                        taggings = taggings,
                                                        tagValues = item.tagBalances,
                                                        includeUntagged = includeUntaggedCol,
                                                        untaggedValue = item.untaggedBalance,
                                                        totalValue = item.balance,
                                                        symbol = symbol,
                                                        scrollState = sharedHScroll,
                                                        widths = widths,
                                                        rowBackground = LedgerBackground,
                                                        rowTier = AccountRowTier.LEDGER_ITEM,
                                                        modifier = Modifier.testTag("account_row_${item.account.id}"),
                                                        onClick = { onSelectAccount(item.account) }
                                                    )
                                                } else {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(min = 48.dp)
                                                            .background(LedgerBackground)
                                                            .clickable { onSelectAccount(item.account) }
                                                            .padding(start = 38.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                                                            .testTag("account_row_${item.account.id}"),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(2.dp)
                                                                    .height(16.dp)
                                                                    .background(LedgerDivider)
                                                            )
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Column {
                                                                Text(
                                                                    text = item.account.name,
                                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                                    color = LedgerTextSecondary,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                if (accountSubtitle != null) {
                                                                    Text(
                                                                        text = accountSubtitle,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = LedgerTextMuted,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = LedgerFormatters.formatSignedWhole(item.balance, symbol),
                                                            style = TabularAmountStyle.copy(fontSize = 13.5.sp),
                                                            color = if (item.balance.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(color = LedgerDivider.copy(alpha = 0.6f), thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Income & Expenses Tag-Split Sections (uses Expense/Income type tags in the same columnar table)
                        if (pnlSections.isNotEmpty()) {
                            item(key = "pnl_section_spacer", contentType = "pnl_spacer") {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = LedgerBorder, thickness = 1.dp)
                            }
                            pnlSections.forEach { pnlSection ->
                                val isPnlExpanded = pnlSection.id in expandedPnlSectionIds
                                val pnlAccent = if (pnlSection.isIncome) assetAccentBlue else liabAccentCoral
                                val togglePnl = {
                                    expandedPnlSectionIds = if (isPnlExpanded) {
                                        expandedPnlSectionIds - pnlSection.id
                                    } else {
                                        expandedPnlSectionIds + pnlSection.id
                                    }
                                }
                                val catCountText = if (pnlSection.rows.size == 1) "1 category" else "${pnlSection.rows.size} categories"

                                item(key = "pnl_hdr_${pnlSection.id}", contentType = "nature_hdr") {
                                    Column(modifier = Modifier.animateItem()) {
                                        if (showTagSplitColumns) {
                                            TagSplitDataRow(
                                                label = pnlSection.title,
                                                subtitle = catCountText,
                                                taggings = taggings,
                                                tagValues = pnlSection.tagSubtotals,
                                                includeUntagged = includeUntaggedCol,
                                                untaggedValue = pnlSection.untaggedSubtotal,
                                                totalValue = pnlSection.subtotal,
                                                symbol = symbol,
                                                scrollState = sharedHScroll,
                                                widths = widths,
                                                rowBackground = LedgerSurfaceElevated,
                                                rowTier = AccountRowTier.NATURE_HEADER,
                                                isExpanded = isPnlExpanded,
                                                accentColor = pnlAccent,
                                                modifier = Modifier.testTag("accounts_pnl_header_${pnlSection.id}"),
                                                onClick = togglePnl
                                            )
                                        } else {
                                            val chevronRot by animateFloatAsState(
                                                targetValue = if (isPnlExpanded) 90f else 0f,
                                                animationSpec = LedgerMotion.microTween(),
                                                label = "pnl_chevron_${pnlSection.id}"
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .background(LedgerSurfaceElevated)
                                                    .clickable(onClick = togglePnl)
                                                    .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                                                    .testTag("accounts_pnl_header_${pnlSection.id}"),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(3.dp)
                                                            .height(20.dp)
                                                            .background(pnlAccent, RoundedCornerShape(2.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = if (isPnlExpanded) "Collapse ${pnlSection.title}" else "Expand ${pnlSection.title}",
                                                        tint = pnlAccent,
                                                        modifier = Modifier.size(20.dp).rotate(chevronRot)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text(
                                                            text = pnlSection.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = LedgerTextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = catCountText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = LedgerTextMuted
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = LedgerFormatters.formatSignedWhole(pnlSection.subtotal, symbol),
                                                    style = TabularAmountStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
                                                    color = if (pnlSection.subtotal.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
                                    }
                                }

                                if (isPnlExpanded) {
                                    pnlSection.rows.forEach { catRow ->
                                        val catKey = "${pnlSection.id}_${catRow.categoryId}"
                                        val hasSubLedgers = catRow.ledgers.isNotEmpty()
                                        val isCatExpanded = hasSubLedgers && catKey in expandedPnlCategoryKeys
                                        val toggleCat: (() -> Unit)? = if (hasSubLedgers) {
                                            {
                                                expandedPnlCategoryKeys = if (isCatExpanded) {
                                                    expandedPnlCategoryKeys - catKey
                                                } else {
                                                    expandedPnlCategoryKeys + catKey
                                                }
                                            }
                                        } else null
                                        val catSubText = if (hasSubLedgers) {
                                            if (catRow.ledgers.size == 1) "1 ledger" else "${catRow.ledgers.size} ledgers"
                                        } else null

                                        item(key = "pnl_cat_$catKey", contentType = "group_hdr") {
                                            Column(modifier = Modifier.animateItem()) {
                                                if (showTagSplitColumns) {
                                                    TagSplitDataRow(
                                                        label = catRow.name,
                                                        subtitle = catSubText,
                                                        taggings = taggings,
                                                        tagValues = catRow.tagValues,
                                                        includeUntagged = includeUntaggedCol,
                                                        untaggedValue = catRow.untaggedValue,
                                                        totalValue = catRow.total,
                                                        symbol = symbol,
                                                        scrollState = sharedHScroll,
                                                        widths = widths,
                                                        rowBackground = LedgerSurface,
                                                        rowTier = if (hasSubLedgers) AccountRowTier.ACCOUNT_GROUP else AccountRowTier.LEDGER_ITEM,
                                                        isExpanded = isCatExpanded,
                                                        modifier = Modifier.testTag("accounts_pnl_cat_$catKey"),
                                                        onClick = toggleCat
                                                    )
                                                } else {
                                                    val catChevronRot by animateFloatAsState(
                                                        targetValue = if (isCatExpanded) 90f else 0f,
                                                        animationSpec = LedgerMotion.microTween(),
                                                        label = "pnl_cat_chev_$catKey"
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(min = 48.dp)
                                                            .background(LedgerSurface)
                                                            .let { m -> if (toggleCat != null) m.clickable(onClick = toggleCat) else m }
                                                            .padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                                                            .testTag("accounts_pnl_cat_$catKey"),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                                                        ) {
                                                            if (hasSubLedgers) {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                                    contentDescription = if (isCatExpanded) "Collapse ${catRow.name}" else "Expand ${catRow.name}",
                                                                    tint = LedgerTextSecondary,
                                                                    modifier = Modifier.size(18.dp).rotate(catChevronRot)
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                            }
                                                            Column {
                                                                Text(
                                                                    text = catRow.name,
                                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                                    color = LedgerTextPrimary,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                if (catSubText != null) {
                                                                    Text(
                                                                        text = catSubText,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = LedgerTextMuted
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = LedgerFormatters.formatSignedWhole(catRow.total, symbol),
                                                            style = TabularAmountStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                                                            color = if (catRow.total.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                            }
                                        }

                                        if (isCatExpanded) {
                                            items(
                                                items = catRow.ledgers,
                                                key = { "pnl_sub_${it.key}" },
                                                contentType = { "ledger_item" }
                                            ) { ledgerRow ->
                                                Column(modifier = Modifier.animateItem()) {
                                                    if (showTagSplitColumns) {
                                                        TagSplitDataRow(
                                                            label = ledgerRow.name,
                                                            subtitle = null,
                                                            taggings = taggings,
                                                            tagValues = ledgerRow.tagValues,
                                                            includeUntagged = includeUntaggedCol,
                                                            untaggedValue = ledgerRow.untaggedValue,
                                                            totalValue = ledgerRow.total,
                                                            symbol = symbol,
                                                            scrollState = sharedHScroll,
                                                            widths = widths,
                                                            rowBackground = LedgerBackground,
                                                            rowTier = AccountRowTier.LEDGER_ITEM,
                                                            modifier = Modifier.testTag("accounts_pnl_sub_${ledgerRow.key}"),
                                                            onClick = null
                                                        )
                                                    } else {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(min = 44.dp)
                                                                .background(LedgerBackground)
                                                                .padding(start = 38.dp, end = 14.dp, top = 9.dp, bottom = 9.dp)
                                                                .testTag("accounts_pnl_sub_${ledgerRow.key}"),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = ledgerRow.name,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = LedgerTextSecondary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                                                            )
                                                            Text(
                                                                text = LedgerFormatters.formatSignedWhole(ledgerRow.total, symbol),
                                                                style = TabularAmountStyle.copy(fontSize = 13.5.sp),
                                                                color = if (ledgerRow.total.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }
                                                    HorizontalDivider(color = LedgerDivider.copy(alpha = 0.6f), thickness = 0.5.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AccountEditorDialog(
            bundle = bundle,
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = { groupId, name, initialBalance, nature, note, includeInNetWorth ->
                viewModel.saveAccount(
                    existingId = null,
                    groupId = groupId,
                    name = name,
                    initialBalance = initialBalance,
                    accountType = nature,
                    note = note,
                    sortOrder = bundle.accounts.size.toLong(),
                    includeInNetWorth = includeInNetWorth
                )
                showAddDialog = false
            }
        )
    }
}

private enum class AccountRowTier {
    NATURE_HEADER,
    ACCOUNT_GROUP,
    LEDGER_ITEM,
    GRAND_TOTAL
}

@Composable
private fun TagSplitHeaderRow(
    taggings: List<String>,
    includeUntagged: Boolean,
    scrollState: ScrollState,
    widths: AccountsTableWidths
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LedgerSurfaceElevated)
            .testTag("accounts_tagging_header_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Group / Account",
            style = MaterialTheme.typography.labelMedium,
            color = LedgerTextSecondary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(widths.nameColWidth)
                .padding(start = 12.dp, end = 6.dp, top = 9.dp, bottom = 9.dp)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(LedgerDivider)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            taggings.forEach { tag ->
                HeaderAmountCell(
                    title = tag,
                    width = widths.tagColWidth,
                    textColor = LedgerTextPrimary
                )
            }
            if (includeUntagged) {
                HeaderAmountCell(
                    title = "Untagged",
                    width = widths.tagColWidth,
                    textColor = LedgerTextSecondary
                )
            }
            HeaderAmountCell(
                title = "Total",
                width = widths.totalColWidth,
                textColor = LedgerCoral,
                highlight = true
            )
        }
    }
}

@Composable
private fun HeaderAmountCell(
    title: String,
    width: Dp,
    textColor: Color,
    highlight: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .background(if (highlight) LedgerSurfaceHighlight.copy(alpha = 0.6f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TagSplitDataRow(
    label: String,
    subtitle: String?,
    taggings: List<String>,
    tagValues: Map<String, Double>,
    includeUntagged: Boolean,
    untaggedValue: Double,
    totalValue: Double,
    symbol: String,
    scrollState: ScrollState,
    widths: AccountsTableWidths,
    rowBackground: Color,
    rowTier: AccountRowTier,
    isExpanded: Boolean = false,
    accentColor: Color = LedgerTextSecondary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val vPad = when (rowTier) {
        AccountRowTier.NATURE_HEADER -> 9.dp
        AccountRowTier.ACCOUNT_GROUP -> 10.dp
        AccountRowTier.LEDGER_ITEM -> 9.dp
        AccountRowTier.GRAND_TOTAL -> 10.dp
    }
    val startPad = when (rowTier) {
        AccountRowTier.NATURE_HEADER -> 8.dp
        AccountRowTier.ACCOUNT_GROUP -> 10.dp
        AccountRowTier.LEDGER_ITEM -> 22.dp
        AccountRowTier.GRAND_TOTAL -> 12.dp
    }
    val isBoldAmounts = rowTier == AccountRowTier.NATURE_HEADER || rowTier == AccountRowTier.GRAND_TOTAL
    val chevronRot by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = LedgerMotion.microTween(),
        label = "tag_row_chevron_$label"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .background(rowBackground)
            .then(clickMod),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Frozen Left Group / Account Name Column
        Row(
            modifier = Modifier
                .width(widths.nameColWidth)
                .padding(start = startPad, end = 6.dp, top = vPad, bottom = vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (rowTier) {
                AccountRowTier.NATURE_HEADER -> {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .background(accentColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse $label" else "Expand $label",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp).rotate(chevronRot)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                AccountRowTier.ACCOUNT_GROUP -> {
                    Surface(
                        onClick = { onClick?.invoke() },
                        shape = RoundedCornerShape(5.dp),
                        color = if (isExpanded) LedgerSurfaceHighlight else LedgerSurfaceElevated,
                        border = BorderStroke(0.5.dp, LedgerBorder),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isExpanded) "Collapse $label" else "Expand $label",
                                tint = if (isExpanded) LedgerTextPrimary else LedgerTextSecondary,
                                modifier = Modifier.size(15.dp).rotate(chevronRot)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                AccountRowTier.LEDGER_ITEM -> {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(15.dp)
                            .background(LedgerDivider)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                AccountRowTier.GRAND_TOTAL -> Unit
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = when (rowTier) {
                        AccountRowTier.GRAND_TOTAL -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        AccountRowTier.NATURE_HEADER -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        AccountRowTier.ACCOUNT_GROUP -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        AccountRowTier.LEDGER_ITEM -> MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    },
                    color = when (rowTier) {
                        AccountRowTier.LEDGER_ITEM -> LedgerTextSecondary
                        else -> LedgerTextPrimary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = LedgerTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(if (rowTier == AccountRowTier.LEDGER_ITEM) 22.dp else 26.dp)
                .background(LedgerDivider.copy(alpha = 0.7f))
        )

        // Synchronized Horizontally Scrollable Tagging Columns + Total Column
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            taggings.forEach { tag ->
                val v = tagValues[tag] ?: 0.0
                AmountSplitCell(
                    amount = v,
                    symbol = symbol,
                    width = widths.tagColWidth,
                    vPad = vPad,
                    isBold = isBoldAmounts,
                    showDashForZero = rowTier != AccountRowTier.GRAND_TOTAL
                )
            }
            if (includeUntagged) {
                AmountSplitCell(
                    amount = untaggedValue,
                    symbol = symbol,
                    width = widths.tagColWidth,
                    vPad = vPad,
                    isBold = isBoldAmounts,
                    showDashForZero = rowTier != AccountRowTier.GRAND_TOTAL
                )
            }
            AmountSplitCell(
                amount = totalValue,
                symbol = symbol,
                width = widths.totalColWidth,
                vPad = vPad,
                isBold = rowTier != AccountRowTier.LEDGER_ITEM,
                showDashForZero = false,
                highlightBg = LedgerSurfaceHighlight.copy(
                    alpha = when (rowTier) {
                        AccountRowTier.NATURE_HEADER, AccountRowTier.GRAND_TOTAL -> 0.42f
                        AccountRowTier.ACCOUNT_GROUP -> 0.28f
                        AccountRowTier.LEDGER_ITEM -> 0.16f
                    }
                )
            )
        }
    }
}

@Composable
private fun AmountSplitCell(
    amount: Double,
    symbol: String,
    width: Dp,
    vPad: Dp,
    isBold: Boolean,
    showDashForZero: Boolean,
    highlightBg: Color = Color.Transparent
) {
    val rounded = amount.roundToLong()
    val isZero = rounded == 0L
    val text = if (isZero && showDashForZero) {
        "—"
    } else {
        LedgerFormatters.formatSignedWhole(amount, symbol)
    }
    val textColor = when {
        isZero && showDashForZero -> LedgerTextMuted
        rounded < 0L -> LedgerCoral
        else -> LedgerBlue
    }

    Box(
        modifier = Modifier
            .width(width)
            .background(highlightBg)
            .padding(horizontal = 8.dp, vertical = vPad),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            style = TabularAmountStyle.copy(
                fontSize = 12.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

private data class AccountStatementEntry(
    val tx: LedgerTransaction,
    val signedDelta: Double,
    val cumulativeBalance: Double,
    val sideTag: String?,
    val typeLabel: String,
    val primaryTitle: String,
    val secondarySubtitle: String?
)

private data class AccountStatementDateGroup(
    val epochDay: Long,
    val entries: List<AccountStatementEntry>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit
) {
    BackHandler { onBack() }

    val account = bundle.accounts.firstOrNull { it.id == accountId }
    if (account == null) {
        onBack()
        return
    }
    val symbol = bundle.settings.mainCurrencySymbol
    val liveBalance = bundle.accountBalances[account.id] ?: account.initialBalance
    val tagSplits = bundle.accountTagBalances[account.id].orEmpty()
    val untaggedBal = bundle.accountUntaggedBalances[account.id] ?: account.initialBalance
    val groupName = bundle.accountGroups.firstOrNull { it.id == account.groupId }?.name ?: "Account"

    val todayEpoch = remember { LedgerFormatters.todayEpochDay() }
    val defaultRange = remember(todayEpoch) {
        val cal = LedgerFormatters.epochDayToCalendar(todayEpoch)
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        LedgerFormatters.calendarToEpochDay(y, m, 1) to
            LedgerFormatters.calendarToEpochDay(y, m, LedgerFormatters.daysInMonth(y, m))
    }

    var datePreset by remember { mutableStateOf(TagReportDatePreset.ALL) }
    var customStartDay by remember { mutableStateOf(defaultRange.first) }
    var customEndDay by remember { mutableStateOf(defaultRange.second) }
    var selectedTagKeys by remember { mutableStateOf(emptySet<String>()) }
    var sortLatestFirst by remember { mutableStateOf(true) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showTagBreakdown by remember { mutableStateOf(false) }

    val rawAccountTxs = remember(bundle.transactions, account.id) {
        bundle.transactions.filter { it.accountId == account.id || it.toAccountId == account.id }
    }

    // Determine available tags and counts specifically on this account's side of each transaction
    val tagMeta = remember(rawAccountTxs, account.id) {
        val canonicalByLower = linkedMapOf<String, String>()
        val counts = mutableMapOf<String, Int>()
        var hasUntagged = false
        rawAccountTxs.forEach { tx ->
            val tag = when {
                tx.txType() == TransactionType.TRANSFER && tx.toAccountId == account.id && tx.accountId != account.id ->
                    tx.effectiveToTagging()
                else -> tx.effectiveTagging()
            }
            if (tag.isNullOrBlank()) {
                hasUntagged = true
                counts[UNTAGGED_KEY] = (counts[UNTAGGED_KEY] ?: 0) + 1
            } else {
                val lower = tag.lowercase()
                canonicalByLower.putIfAbsent(lower, tag)
                counts[lower] = (counts[lower] ?: 0) + 1
            }
        }
        Triple(canonicalByLower.values.sortedBy { it.lowercase() }, hasUntagged, counts.toMap())
    }
    val availableTags = tagMeta.first
    val hasUntaggedEntries = tagMeta.second
    val entryCountsByKey = tagMeta.third

    val dateBounds = remember(datePreset, customStartDay, customEndDay) {
        resolveDatePresetBounds(datePreset, customStartDay, customEndDay)
    }

    // Build chronological running cumulative statement, then apply date filter and display ordering
    val statementResult = remember(
        rawAccountTxs,
        account.id,
        account.initialBalance,
        selectedTagKeys,
        dateBounds,
        sortLatestFirst,
        bundle.categories,
        bundle.accounts
    ) {
        val chronological = rawAccountTxs.sortedWith(
            compareBy<LedgerTransaction> { it.epochDay }
                .thenBy { it.timestampMillis }
                .thenBy { it.id }
        )

        val tagFiltered = if (selectedTagKeys.isEmpty()) {
            chronological
        } else {
            chronological.filter { tx ->
                val sideTag = when {
                    tx.txType() == TransactionType.TRANSFER && tx.toAccountId == account.id && tx.accountId != account.id ->
                        tx.effectiveToTagging()
                    else -> tx.effectiveTagging()
                }
                if (sideTag.isNullOrBlank()) UNTAGGED_KEY in selectedTagKeys
                else sideTag.lowercase() in selectedTagKeys
            }
        }

        // Opening balance belongs to the full account (and untagged bucket)
        var running = if (selectedTagKeys.isEmpty() || UNTAGGED_KEY in selectedTagKeys) {
            account.initialBalance
        } else {
            0.0
        }

        val withCumulative = tagFiltered.map { tx ->
            val type = tx.txType()
            val signedDelta: Double
            val sideTag: String?
            val typeLabel: String
            val counterpartText: String?

            when (type) {
                TransactionType.INCOME -> {
                    signedDelta = tx.amount
                    sideTag = tx.effectiveTagging()
                    typeLabel = "Income"
                    counterpartText = bundle.categories
                        .firstOrNull { it.id == tx.categoryId }
                        ?.name
                        ?.takeIf { it.isNotBlank() && it != "Uncategorized" }
                }
                TransactionType.EXPENSE -> {
                    signedDelta = -tx.amount
                    sideTag = tx.effectiveTagging()
                    typeLabel = "Expense"
                    counterpartText = tx.subcategory?.takeIf { it.isNotBlank() }
                        ?: bundle.categories
                            .firstOrNull { it.id == tx.categoryId }
                            ?.name
                            ?.takeIf { it.isNotBlank() && it != "Uncategorized" }
                }
                TransactionType.TRANSFER -> {
                    typeLabel = "Transfer"
                    when {
                        tx.accountId == account.id && tx.toAccountId == account.id -> {
                            signedDelta = -tx.feeAmount
                            sideTag = tx.effectiveTagging() ?: tx.effectiveToTagging()
                            counterpartText = "Self Transfer"
                        }
                        tx.accountId == account.id -> {
                            signedDelta = -(tx.amount + tx.feeAmount)
                            sideTag = tx.effectiveTagging()
                            val toName = bundle.accountName(tx.toAccountId)
                            counterpartText = if (toName.isNotBlank()) "To $toName" else "Outgoing Transfer"
                        }
                        else -> {
                            signedDelta = tx.amount
                            sideTag = tx.effectiveToTagging()
                            val fromName = bundle.accountName(tx.accountId)
                            counterpartText = if (fromName.isNotBlank()) "From $fromName" else "Incoming Transfer"
                        }
                    }
                }
            }

            running += signedDelta

            val primaryTitle = tx.note.trim().ifBlank {
                counterpartText ?: typeLabel
            }
            val secondarySubtitle = buildString {
                append(typeLabel)
                if (!counterpartText.isNullOrBlank() && !counterpartText.equals(primaryTitle, ignoreCase = true)) {
                    append(" • ")
                    append(counterpartText)
                }
                val note2 = tx.note2?.trim().orEmpty()
                if (note2.isNotEmpty()) {
                    append(" • ")
                    append(note2)
                }
            }.takeIf { it.isNotBlank() }

            AccountStatementEntry(
                tx = tx,
                signedDelta = signedDelta,
                cumulativeBalance = running,
                sideTag = sideTag,
                typeLabel = typeLabel,
                primaryTitle = primaryTitle,
                secondarySubtitle = secondarySubtitle
            )
        }

        val startDay = dateBounds.first
        val endDay = dateBounds.second
        val dateFiltered = withCumulative.filter { entry ->
            (startDay == null || entry.tx.epochDay >= startDay) &&
                (endDay == null || entry.tx.epochDay <= endDay)
        }

        val periodInflow = dateFiltered.filter { it.signedDelta > 0.0 }.sumOf { it.signedDelta }
        val periodOutflow = dateFiltered.filter { it.signedDelta < 0.0 }.sumOf { abs(it.signedDelta) }
        val closingCumulative = dateFiltered.lastOrNull()?.cumulativeBalance ?: running

        val ordered = if (sortLatestFirst) dateFiltered.asReversed() else dateFiltered
        val grouped = ordered
            .groupBy { it.tx.epochDay }
            .entries
            .map { (day, list) -> AccountStatementDateGroup(epochDay = day, entries = list) }

        QuadStatementMetrics(
            groups = grouped,
            filteredCount = dateFiltered.size,
            periodInflow = periodInflow,
            periodOutflow = periodOutflow,
            closingCumulative = closingCumulative
        )
    }

    val hasActiveFilters = datePreset != TagReportDatePreset.ALL || selectedTagKeys.isNotEmpty()
    val datePillLabel = remember(datePreset, customStartDay, customEndDay) {
        formatDateChipLabel(datePreset, customStartDay, customEndDay)
    }
    val tagPillLabel = remember(selectedTagKeys, availableTags) {
        when {
            selectedTagKeys.isEmpty() -> "All Tags"
            selectedTagKeys.size == 1 -> {
                val only = selectedTagKeys.first()
                if (only == UNTAGGED_KEY) "Untagged"
                else availableTags.firstOrNull { it.equals(only, ignoreCase = true) } ?: only
            }
            else -> "${selectedTagKeys.size} Tags"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header Bar (Delete removed from top bar — moved inside Edit Account dialog)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(start = 4.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("account_detail_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LedgerTextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.testTag("account_detail_edit")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit account",
                    tint = LedgerTextPrimary
                )
            }
        }
        HorizontalDivider(color = LedgerDivider)

        // Balance Summary Card
        Surface(
            color = LedgerSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, LedgerDivider)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasActiveFilters) "Filtered Closing Balance" else "Current Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = LedgerTextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (account.includeInNetWorth) LedgerBlue.copy(alpha = 0.16f) else LedgerSurfaceHighlight
                    ) {
                        Text(
                            text = if (account.includeInNetWorth) "In Net Worth" else "Excl. Net Worth",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (account.includeInNetWorth) LedgerBlue else LedgerTextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val heroBalance = if (hasActiveFilters) statementResult.closingCumulative else liveBalance
                Text(
                    text = LedgerFormatters.formatSignedWhole(heroBalance, symbol),
                    style = TabularAmountStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    color = if (heroBalance.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag("account_detail_hero_balance")
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = LedgerDivider.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))

                // Compact Inflow / Outflow / Opening Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccountMetricMini(
                        label = "Opening",
                        value = LedgerFormatters.formatSignedWhole(account.initialBalance, symbol),
                        valueColor = if (account.initialBalance.roundToLong() < 0L) LedgerCoral else LedgerTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    AccountMetricMini(
                        label = "Inflow",
                        value = "+${LedgerFormatters.formatWholeAmount(statementResult.periodInflow, symbol)}",
                        valueColor = LedgerBlue,
                        alignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    )
                    AccountMetricMini(
                        label = "Outflow",
                        value = "-${LedgerFormatters.formatWholeAmount(statementResult.periodOutflow, symbol)}",
                        valueColor = LedgerCoral,
                        alignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (tagSplits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTagBreakdown = !showTagBreakdown }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tag Split (${tagSplits.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerTextSecondary
                        )
                        val splitArrowRot by animateFloatAsState(
                            targetValue = if (showTagBreakdown) 180f else 0f,
                            animationSpec = LedgerMotion.microTween(),
                            label = "account_tag_split_rot"
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (showTagBreakdown) "Hide tag split" else "Show tag split",
                            tint = LedgerTextSecondary,
                            modifier = Modifier.size(18.dp).rotate(splitArrowRot)
                        )
                    }
                    AnimatedVisibility(
                        visible = showTagBreakdown,
                        enter = LedgerMotion.expandEnter(),
                        exit = LedgerMotion.shrinkExit()
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tagSplits.entries.sortedBy { it.key.lowercase() }.forEach { (tagName, tagBal) ->
                                val key = tagName.lowercase()
                                val selected = key in selectedTagKeys
                                Surface(
                                    onClick = {
                                        selectedTagKeys = if (selected) selectedTagKeys - key else setOf(key)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) LedgerBlue.copy(alpha = 0.16f) else LedgerSurface,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) LedgerBlue else LedgerDivider
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = tagName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) LedgerBlue else LedgerTextPrimary
                                        )
                                        Text(
                                            text = LedgerFormatters.formatSignedWhole(tagBal, symbol),
                                            style = TabularAmountStyle.copy(fontSize = 11.5.sp),
                                            color = if (tagBal.roundToLong() < 0L) LedgerCoral else LedgerBlue
                                        )
                                    }
                                }
                            }
                            if (untaggedBal.roundToLong() != 0L) {
                                val selected = UNTAGGED_KEY in selectedTagKeys
                                Surface(
                                    onClick = {
                                        selectedTagKeys = if (selected) selectedTagKeys - UNTAGGED_KEY else setOf(UNTAGGED_KEY)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) LedgerBlue.copy(alpha = 0.16f) else LedgerSurface,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) LedgerBlue else LedgerDivider
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Untagged",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LedgerTextMuted
                                        )
                                        Text(
                                            text = LedgerFormatters.formatSignedWhole(untaggedBal, symbol),
                                            style = TabularAmountStyle.copy(fontSize = 11.5.sp),
                                            color = if (untaggedBal.roundToLong() < 0L) LedgerCoral else LedgerBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Bar: Date Range | Tagging | Sort | Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReportFilterPill(
                icon = Icons.Filled.DateRange,
                label = datePillLabel,
                active = datePreset != TagReportDatePreset.ALL,
                accentColor = LedgerCoral,
                showDropdownChevron = true,
                onClick = { showDateDialog = true },
                modifier = Modifier.testTag("account_detail_date_filter")
            )
            ReportFilterPill(
                icon = Icons.Filled.Label,
                label = tagPillLabel,
                active = selectedTagKeys.isNotEmpty(),
                accentColor = LedgerBlue,
                showDropdownChevron = true,
                onClick = { showTagDialog = true },
                modifier = Modifier.testTag("account_detail_tag_filter")
            )
            ReportFilterPill(
                icon = Icons.Filled.SwapVert,
                label = if (sortLatestFirst) "Latest" else "Oldest",
                active = false,
                accentColor = LedgerTextSecondary,
                showDropdownChevron = false,
                onClick = { sortLatestFirst = !sortLatestFirst },
                modifier = Modifier.testTag("account_detail_sort_toggle")
            )
            if (hasActiveFilters) {
                Surface(
                    onClick = {
                        datePreset = TagReportDatePreset.ALL
                        selectedTagKeys = emptySet()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = LedgerCoral.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, LedgerCoral.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .testTag("account_detail_reset_filters")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Reset filters",
                            tint = LedgerCoral,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerCoral
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Column Header Strip: Statement Details | Value | Cumulative
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Statement (${statementResult.filteredCount})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = LedgerTextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Value",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = LedgerTextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(88.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cumulative",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = LedgerTextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(94.dp)
            )
        }
        HorizontalDivider(color = LedgerDivider)

        // Date-Grouped Statement List with Prominent Date Headers + Running Cumulative Column
        if (statementResult.groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (hasActiveFilters) {
                            "No transactions match your filters."
                        } else {
                            "No transactions recorded for this account yet."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerTextMuted,
                        textAlign = TextAlign.Center
                    )
                    if (hasActiveFilters) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                datePreset = TagReportDatePreset.ALL
                                selectedTagKeys = emptySet()
                            }
                        ) {
                            Text("Clear Filters", color = LedgerCoral, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                statementResult.groups.forEach { group ->
                    item(key = "acc_date_${group.epochDay}", contentType = "acc_date_hdr") {
                        AccountStatementDateHeader(
                            epochDay = group.epochDay,
                            entryCount = group.entries.size
                        )
                    }
                    items(
                        items = group.entries,
                        key = { "acc_tx_${it.tx.id}" },
                        contentType = { "acc_tx_row" }
                    ) { entry ->
                        AccountStatementTransactionRow(
                            entry = entry,
                            symbol = symbol,
                            onClick = { onEditTransaction(entry.tx) }
                        )
                        HorizontalDivider(color = LedgerDivider.copy(alpha = 0.65f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showDateDialog) {
        DateRangeFilterDialog(
            currentPreset = datePreset,
            currentCustomStart = customStartDay,
            currentCustomEnd = customEndDay,
            onDismiss = { showDateDialog = false },
            onApply = { preset, start, end ->
                datePreset = preset
                customStartDay = start
                customEndDay = end
                showDateDialog = false
            }
        )
    }

    if (showTagDialog) {
        TagMultiSelectFilterDialog(
            availableTags = availableTags,
            hasUntaggedEntries = hasUntaggedEntries,
            entryCountsByKey = entryCountsByKey,
            initialSelectedKeys = selectedTagKeys,
            onDismiss = { showTagDialog = false },
            onApply = { keys ->
                selectedTagKeys = keys
                showTagDialog = false
            }
        )
    }

    if (showEditDialog) {
        AccountEditorDialog(
            bundle = bundle,
            existing = account,
            onDismiss = { showEditDialog = false },
            onSave = { groupId, name, initialBalance, nature, note, includeInNetWorth ->
                viewModel.saveAccount(
                    existingId = account.id,
                    groupId = groupId,
                    name = name,
                    initialBalance = initialBalance,
                    accountType = nature,
                    note = note,
                    sortOrder = account.sortOrder,
                    includeInNetWorth = includeInNetWorth
                )
                showEditDialog = false
            },
            onDelete = {
                viewModel.setAccountArchived(account.id, true)
                showEditDialog = false
                onBack()
            }
        )
    }
}

private data class QuadStatementMetrics(
    val groups: List<AccountStatementDateGroup>,
    val filteredCount: Int,
    val periodInflow: Double,
    val periodOutflow: Double,
    val closingCumulative: Double
)

@Composable
private fun AccountMetricMini(
    label: String,
    value: String,
    valueColor: Color,
    alignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LedgerTextMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = TabularAmountStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AccountStatementDateHeader(
    epochDay: Long,
    entryCount: Int
) {
    val dayOfMonth = remember(epochDay) { LedgerFormatters.dayOfMonthPadded(epochDay) }
    val dayOfWeek = remember(epochDay) { LedgerFormatters.weekdayShort(epochDay) }
    val monthYear = remember(epochDay) { LedgerFormatters.monthYearDot(epochDay) }
    val isSunday = remember(epochDay) {
        LedgerFormatters.dayOfWeekIndex(epochDay) == Calendar.SUNDAY
    }

    Column(modifier = Modifier.fillMaxWidth().background(LedgerBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayOfMonth,
                style = TabularAmountStyle.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold),
                color = LedgerTextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = if (isSunday) LedgerCoral else LedgerSurfaceHighlight
            ) {
                Text(
                    text = dayOfWeek,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSunday) Color.White else LedgerTextPrimary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = monthYear,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = LedgerTextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted
            )
        }
        HorizontalDivider(color = LedgerDivider.copy(alpha = 0.8f), thickness = 0.6.dp)
    }
}

@Composable
private fun AccountStatementTransactionRow(
    entry: AccountStatementEntry,
    symbol: String,
    onClick: () -> Unit
) {
    val valueRounded = entry.signedDelta.roundToLong()
    val valueColor = when {
        valueRounded > 0L -> LedgerBlue
        valueRounded < 0L -> LedgerCoral
        else -> LedgerTextSecondary
    }
    val cumRounded = entry.cumulativeBalance.roundToLong()
    val cumColor = if (cumRounded < 0L) LedgerCoral else LedgerTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(LedgerSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("account_stmt_row_${entry.tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.primaryTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!entry.sideTag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LedgerSurfaceHighlight
                    ) {
                        Text(
                            text = entry.sideTag,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerBlue,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (!entry.secondarySubtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.secondarySubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Value column
        Text(
            text = formatSignedLedgerAmount(entry.signedDelta, symbol, showPlus = true),
            style = TabularAmountStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
            modifier = Modifier.width(88.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Cumulative balance column
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = LedgerSurfaceElevated,
            modifier = Modifier.width(94.dp)
        ) {
            Text(
                text = LedgerFormatters.formatSignedWhole(entry.cumulativeBalance, symbol),
                style = TabularAmountStyle.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                color = cumColor,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun AccountEditorDialog(
    bundle: LedgerDataBundle,
    existing: Account?,
    onDismiss: () -> Unit,
    onSave: (
        groupId: String,
        name: String,
        initialBalance: Double,
        nature: AccountNature,
        note: String?,
        includeInNetWorth: Boolean
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val activeGroups = bundle.activeGroups
    var selectedGroupId by remember(existing, activeGroups) {
        mutableStateOf(existing?.groupId ?: activeGroups.firstOrNull()?.id.orEmpty())
    }
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var balanceText by remember(existing) {
        mutableStateOf(if (existing != null) existing.initialBalance.roundToLong().toString() else "0")
    }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    var includeInNetWorth by remember(existing) { mutableStateOf(existing?.includeInNetWorth ?: true) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selectedGroup = activeGroups.firstOrNull { it.id == selectedGroupId } ?: activeGroups.firstOrNull()
    val selectedGroupName = selectedGroup?.name ?: "Select Sub Group"
    val selectedMainGroupName = selectedGroup?.let { bundle.mainGroupNameFor(it) }
    val nature = selectedGroup?.let { bundle.groupNatureById[it.id] ?: it.nature() }
        ?: existing?.nature()
        ?: AccountNature.ASSET

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LedgerSurface,
        title = {
            Text(
                text = if (existing == null) "New Ledger" else "Edit Ledger",
                color = LedgerTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Sub Group",
                        style = MaterialTheme.typography.labelMedium,
                        color = LedgerTextSecondary
                    )
                    Box {
                        OutlinedButton(
                            onClick = { groupMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("ledger_sub_group_selector"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LedgerBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedGroupName,
                                        color = LedgerTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!selectedMainGroupName.isNullOrBlank()) {
                                        Text(
                                            text = "Main Group: $selectedMainGroupName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LedgerTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Choose Sub Group",
                                    tint = LedgerTextSecondary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = groupMenuExpanded,
                            onDismissRequest = { groupMenuExpanded = false },
                            containerColor = LedgerSurfaceElevated
                        ) {
                            activeGroups.forEach { grp ->
                                val parentName = bundle.mainGroupNameFor(grp)
                                val isChosen = grp.id == selectedGroup?.id
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = grp.name,
                                                color = if (isChosen) LedgerCoral else LedgerTextPrimary,
                                                fontWeight = if (isChosen) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                            Text(
                                                text = parentName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LedgerTextMuted
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedGroupId = grp.id
                                        groupMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Ledger Name") },
                    placeholder = { Text("e.g. HDFC Savings, Wallet, Credit Card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' || it == '-' }
                        balanceText = filtered.take(15)
                    },
                    label = { Text("Opening Balance (negative for liabilities)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = IndianNumberVisualTransformation,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_opening_balance_input")
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LedgerSurfaceElevated,
                    border = BorderStroke(1.dp, LedgerDivider),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeInNetWorth = !includeInNetWorth }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = "Include in Net Worth",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = LedgerTextPrimary
                            )
                            Text(
                                text = "Count balance in Net Worth report",
                                style = MaterialTheme.typography.labelSmall,
                                color = LedgerTextMuted
                            )
                        }
                        Switch(
                            checked = includeInNetWorth,
                            onCheckedChange = { includeInNetWorth = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LedgerBlue,
                                uncheckedThumbColor = LedgerTextSecondary,
                                uncheckedTrackColor = LedgerBackground
                            ),
                            modifier = Modifier.testTag("account_include_net_worth_switch")
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (existing != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LedgerCoral.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("account_edit_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Account", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.trim().toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && selectedGroupId.isNotBlank()) {
                        onSave(
                            selectedGroupId,
                            name,
                            bal,
                            nature,
                            note.takeIf { it.isNotBlank() },
                            includeInNetWorth
                        )
                    }
                },
                enabled = name.isNotBlank() && selectedGroupId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral),
                modifier = Modifier.testTag("account_dialog_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LedgerTextSecondary)
            }
        }
    )

    if (showDeleteConfirm && existing != null && onDelete != null) {
        ConfirmDestructiveDialog(
            title = "Delete Account?",
            message = "Archive \"${existing.name}\"? Its past transactions will remain intact and you can restore it anytime from Deleted Accounts.",
            confirmLabel = "Delete",
            confirmTestTag = "confirm_delete_account_button",
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
