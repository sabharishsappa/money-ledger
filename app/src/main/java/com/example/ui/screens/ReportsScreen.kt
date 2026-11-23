package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.AccountNature
import com.example.data.LedgerTransaction
import com.example.data.ReportPeriodMode
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
import com.example.ui.theme.TabularAmountStyle
import com.example.util.LedgerFormatters
import com.example.viewmodel.CreditCardReportCalculator
import com.example.viewmodel.FilterState
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import com.example.viewmodel.NetWorthAccountSnapshot
import com.example.viewmodel.NetWorthPeriodPoint
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

private enum class AvailableReport(val title: String) {
    STATS("Stats"),
    NET_WORTH("Net Worth"),
    CREDIT_CARDS("Credit Cards"),
    SPENDING_TRENDS("Spending Trends"),
    MONTHLY_BUDGET("Monthly Budget"),
    TAGGING_TRACKER("Tagging Tracker")
}

@Composable
fun ReportsScreen(
    bundle: LedgerDataBundle,
    filter: FilterState,
    viewModel: LedgerViewModel,
    onSelectAccount: (Account) -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit = {},
    onAddExpense: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol

    var openedReport by remember { mutableStateOf<AvailableReport?>(null) }
    var periodMode by remember { mutableStateOf(ReportPeriodMode.MONTHLY) }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    BackHandler(enabled = openedReport != null) {
        openedReport = null
    }

    val series = remember(bundle, periodMode) {
        viewModel.computeNetWorthSeries(bundle, periodMode)
    }
    val statsPreview = remember(bundle.transactions, bundle.categories, filter) {
        viewModel.computeStats(bundle, filter)
    }
    val trendsPreview = remember(bundle.transactions, bundle.categories) {
        viewModel.computeSpendingTrends(bundle, monthsCount = 6)
    }
    val budgetPreview = remember(bundle.transactions, bundle.categories, bundle.budgets) {
        val now = Calendar.getInstance()
        computeMonthlyBudgetSummary(bundle, now.get(Calendar.YEAR), now.get(Calendar.MONTH))
    }

    LaunchedEffect(periodMode, series.size) {
        if (series.isNotEmpty()) {
            selectedIndex = series.lastIndex
        }
    }

    val safeSelectedIdx = if (selectedIndex in series.indices) {
        selectedIndex
    } else {
        (series.size - 1).coerceAtLeast(0)
    }
    val selectedPoint = series.getOrNull(safeSelectedIdx)
    val creditCardReport = remember(bundle, periodMode, series) {
        CreditCardReportCalculator.computeSeries(bundle, periodMode, series)
    }
    val creditCardSummaries = remember(bundle, creditCardReport, safeSelectedIdx) {
        val period = creditCardReport.periods.getOrNull(safeSelectedIdx)
        if (period == null) emptyList() else CreditCardReportCalculator.summarize(
            bundle, creditCardReport.cards, period.startEpochDay, period.endEpochDay
        )
    }
    val includedCount = bundle.netWorthIncludedAccounts.size
    val excludedCount = bundle.netWorthExcludedAccounts.size

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
                .padding(
                    start = if (openedReport != null) 4.dp else 16.dp,
                    end = 16.dp,
                    top = if (openedReport != null) 6.dp else 10.dp,
                    bottom = if (openedReport != null) 6.dp else 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (openedReport != null) {
                IconButton(
                    onClick = { openedReport = null },
                    modifier = Modifier.testTag("reports_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to reports list",
                        tint = LedgerTextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (openedReport != null) 2.dp else 0.dp, end = 8.dp)
            ) {
                Text(
                    text = openedReport?.title ?: "Reports",
                    style = MaterialTheme.typography.titleLarge,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (openedReport) {
                        null -> "${AvailableReport.entries.size} reports available"
                        AvailableReport.STATS -> "Income & expense breakdown by category"
                        AvailableReport.NET_WORTH ->
                            series.lastOrNull()?.let { "Net Worth • Till ${it.fullLabel}" } ?: "Net Worth Summary"
                        AvailableReport.CREDIT_CARDS -> "Credit card spending, payments & dues"
                        AvailableReport.SPENDING_TRENDS -> "Past 6 months by expense category"
                        AvailableReport.MONTHLY_BUDGET -> "Category spending limits & usage progress"
                        AvailableReport.TAGGING_TRACKER -> "Track Tagging 1 & Tagging 2 separately"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Monthly / Yearly Dropdown Selector (shown inside Net Worth report)
            if (openedReport == AvailableReport.NET_WORTH || openedReport == AvailableReport.CREDIT_CARDS) {
                val chevronRot by animateFloatAsState(
                    targetValue = if (showPeriodDropdown) 180f else 0f,
                    animationSpec = LedgerMotion.quickSpring(),
                    label = "reports_dropdown_chevron"
                )
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LedgerSurfaceElevated,
                        border = BorderStroke(1.dp, LedgerBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showPeriodDropdown = true }
                            .testTag("reports_period_dropdown")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = periodMode.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = LedgerTextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Select report period",
                                tint = LedgerTextSecondary,
                                modifier = Modifier.size(18.dp).rotate(chevronRot)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showPeriodDropdown,
                        onDismissRequest = { showPeriodDropdown = false },
                        containerColor = LedgerSurfaceElevated
                    ) {
                        ReportPeriodMode.entries.forEach { mode ->
                            val active = periodMode == mode
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.label,
                                        color = if (active) LedgerCoral else LedgerTextPrimary,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                trailingIcon = if (active) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = LedgerCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    periodMode = mode
                                    showPeriodDropdown = false
                                },
                                modifier = Modifier.testTag("reports_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

        AnimatedContent(
            targetState = openedReport,
            transitionSpec = {
                val goingBack = targetState == null
                LedgerMotion.pushPopTransform(goingBack = goingBack)
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "reports_navigation_content"
        ) { report ->
            when (report) {
                null -> {
                    val latestNetWorth = series.lastOrNull()?.netWorth
                    val latestNetColor = when {
                        latestNetWorth == null -> LedgerTextSecondary
                        latestNetWorth.roundToLong() < 0L -> LedgerCoral
                        latestNetWorth.roundToLong() > 0L -> LedgerGreen
                        else -> LedgerTextPrimary
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item(key = "available_reports_list") {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 680.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LedgerSurface)
                                ) {
                                    val statsPreviewAmount = if (filter.statsIsExpenseTab) {
                                        statsPreview.second
                                    } else {
                                        statsPreview.first
                                    }
                                    ReportListItemRow(
                                        icon = Icons.Filled.PieChart,
                                        iconTint = LedgerAmber,
                                        title = "Stats",
                                        subtitle = "Income & expense pie chart by category",
                                        previewValue = if (statsPreviewAmount > 0.0) {
                                            LedgerFormatters.formatWholeAmount(statsPreviewAmount, symbol)
                                        } else null,
                                        previewColor = if (filter.statsIsExpenseTab) LedgerCoral else LedgerBlue,
                                        onClick = { openedReport = AvailableReport.STATS },
                                        modifier = Modifier.testTag("reports_list_item_stats")
                                    )
                                    HorizontalDivider(
                                        color = LedgerDivider,
                                        modifier = Modifier.padding(start = 66.dp)
                                    )
                                    ReportListItemRow(
                                        icon = Icons.Filled.AccountBalance,
                                        iconTint = LedgerBlue,
                                        title = "Net Worth Report",
                                        subtitle = "Assets vs Liabilities • Monthly & Yearly",
                                        previewValue = if (bundle.activeAccounts.isNotEmpty() && latestNetWorth != null) {
                                            LedgerFormatters.formatSignedWhole(latestNetWorth, symbol)
                                        } else null,
                                        previewColor = latestNetColor,
                                        onClick = { openedReport = AvailableReport.NET_WORTH },
                                        modifier = Modifier.testTag("reports_list_item_net_worth")
                                    )
                                    HorizontalDivider(
                                        color = LedgerDivider,
                                        modifier = Modifier.padding(start = 66.dp)
                                    )
                                    val latestCardsOutstanding = creditCardReport.periods.lastOrNull()?.outstanding
                                    ReportListItemRow(
                                        icon = Icons.Filled.CreditCard,
                                        iconTint = LedgerAmber,
                                        title = "Credit Cards",
                                        subtitle = "Spending, payments & outstanding per card",
                                        previewValue = if (creditCardReport.cards.isNotEmpty() && latestCardsOutstanding != null) {
                                            LedgerFormatters.formatWholeAmount(latestCardsOutstanding, symbol)
                                        } else null,
                                        previewColor = if ((latestCardsOutstanding ?: 0.0).roundToLong() > 0L) LedgerCoral else LedgerGreen,
                                        onClick = { openedReport = AvailableReport.CREDIT_CARDS },
                                        modifier = Modifier.testTag("reports_list_item_credit_cards")
                                    )
                                    HorizontalDivider(
                                        color = LedgerDivider,
                                        modifier = Modifier.padding(start = 66.dp)
                                    )
                                    ReportListItemRow(
                                        icon = Icons.Filled.BarChart,
                                        iconTint = LedgerCoral,
                                        title = "Spending Trends (6 Months)",
                                        subtitle = "Bar chart of expenses by category",
                                        previewValue = if (trendsPreview.sixMonthTotal > 0.0) {
                                            LedgerFormatters.formatWholeAmount(trendsPreview.sixMonthTotal, symbol)
                                        } else null,
                                        previewColor = LedgerCoral,
                                        onClick = { openedReport = AvailableReport.SPENDING_TRENDS },
                                        modifier = Modifier.testTag("reports_list_item_spending_trends")
                                    )
                                    HorizontalDivider(
                                        color = LedgerDivider,
                                        modifier = Modifier.padding(start = 66.dp)
                                    )
                                    ReportListItemRow(
                                        icon = Icons.Filled.AccountBalanceWallet,
                                        iconTint = if (budgetPreview.totalBudgeted > 0.0) {
                                            budgetStatusColor(budgetPreview.overallRatio, budgetPreview.isOverBudget)
                                        } else LedgerGreen,
                                        title = "Monthly Budget",
                                        subtitle = "Category spending limits & progress bars",
                                        previewValue = if (budgetPreview.totalBudgeted > 0.0) {
                                            "${budgetPreview.overallPercentInt}% used"
                                        } else "Set limits",
                                        previewColor = if (budgetPreview.totalBudgeted > 0.0) {
                                            budgetStatusColor(budgetPreview.overallRatio, budgetPreview.isOverBudget)
                                        } else LedgerTextSecondary,
                                        onClick = { openedReport = AvailableReport.MONTHLY_BUDGET },
                                        modifier = Modifier.testTag("reports_list_item_monthly_budget")
                                    )
                                    HorizontalDivider(
                                        color = LedgerDivider,
                                        modifier = Modifier.padding(start = 66.dp)
                                    )
                                    val tag1Count = bundle.allTaggings.size
                                    val tag2Count = bundle.allSecondaryTaggings.size
                                    ReportListItemRow(
                                        icon = Icons.Filled.Label,
                                        iconTint = LedgerAmber,
                                        title = "Tagging Tracker",
                                        subtitle = "Track Tagging 1 & Tagging 2 separately",
                                        previewValue = if (tag1Count > 0 || tag2Count > 0) {
                                            "T1: $tag1Count • T2: $tag2Count"
                                        } else null,
                                        previewColor = LedgerAmber,
                                        onClick = { openedReport = AvailableReport.TAGGING_TRACKER },
                                        modifier = Modifier.testTag("reports_list_item_tagging_tracker")
                                    )
                                }
                                HorizontalDivider(color = LedgerDivider)

                                Text(
                                    text = "Select a report to view charts and detailed breakdowns.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LedgerTextMuted,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }

                AvailableReport.STATS -> {
                    StatsScreen(
                        bundle = bundle,
                        filter = filter,
                        viewModel = viewModel,
                        onEditTransaction = onEditTransaction,
                        onOpenSpendingTrends = { openedReport = AvailableReport.SPENDING_TRENDS },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AvailableReport.MONTHLY_BUDGET -> {
                    MonthlyBudgetContent(
                        bundle = bundle,
                        viewModel = viewModel,
                        onEditTransaction = onEditTransaction,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AvailableReport.CREDIT_CARDS -> {
                    CreditCardReportContent(
                        hasCards = creditCardReport.cards.isNotEmpty(),
                        periods = creditCardReport.periods,
                        selectedIndex = safeSelectedIdx,
                        summaries = creditCardSummaries,
                        symbol = symbol,
                        mode = periodMode,
                        onSelectIndex = { idx -> selectedIndex = idx },
                        onSelectAccount = onSelectAccount
                    )
                }

                AvailableReport.SPENDING_TRENDS -> {
                    SpendingTrendsContent(
                        bundle = bundle,
                        viewModel = viewModel,
                        onEditTransaction = onEditTransaction,
                        onAddExpense = onAddExpense,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AvailableReport.TAGGING_TRACKER -> {
                    TaggingTrackerContent(
                        bundle = bundle,
                        onEditTransaction = onEditTransaction,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AvailableReport.NET_WORTH -> {
                    if (bundle.activeAccounts.isEmpty()) {
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
                                Text(
                                    text = "No accounts for Net Worth",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LedgerTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Create accounts in the Accounts tab and enable \"Include in Net Worth\" to view your balance history.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LedgerTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Hero Net Worth KPI Card for Selected Period
                            if (selectedPoint != null) {
                                item(key = "net_worth_kpi", contentType = "kpi_card") {
                                    NetWorthHeroCard(
                                        point = selectedPoint,
                                        symbol = symbol,
                                        includedCount = includedCount,
                                        excludedCount = excludedCount,
                                        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                                    )
                                }
                            }

                            // Dual-Color Split Bar Chart (Assets vs Liabilities + Net Worth Line)
                            item(key = "net_worth_bar_chart", contentType = "chart_card") {
                                NetWorthChartCard(
                                    series = series,
                                    selectedIndex = safeSelectedIdx,
                                    symbol = symbol,
                                    mode = periodMode,
                                    onSelectIndex = { idx -> selectedIndex = idx },
                                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                                )
                            }

                            // Period Breakdown Table (Monthly or Yearly rows)
                            item(key = "period_table_card", contentType = "table_card") {
                                NetWorthPeriodTableCard(
                                    series = series,
                                    selectedIndex = safeSelectedIdx,
                                    symbol = symbol,
                                    mode = periodMode,
                                    onSelectIndex = { idx -> selectedIndex = idx },
                                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportListItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    previewValue: String?,
    previewColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = LedgerTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (previewValue != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = previewValue,
                style = TabularAmountStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = previewColor,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LedgerTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun NetWorthHeroCard(
    point: NetWorthPeriodPoint,
    symbol: String,
    includedCount: Int,
    excludedCount: Int,
    modifier: Modifier = Modifier
) {
    val targetNetColor = when {
        point.netWorth.roundToLong() < 0L -> LedgerCoral
        point.netWorth.roundToLong() > 0L -> LedgerGreen
        else -> LedgerTextPrimary
    }
    val netColor by animateColorAsState(
        targetValue = targetNetColor,
        animationSpec = LedgerMotion.microTween(120),
        label = "net_worth_color"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier.testTag("reports_net_worth_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Worth • ${point.fullLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = if (excludedCount > 0) {
                        "$includedCount incl. • $excludedCount excl."
                    } else {
                        "$includedCount accounts"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = LedgerFormatters.formatSignedWhole(point.netWorth, symbol),
                transitionSpec = { LedgerMotion.fastFadeTransform() },
                label = "net_worth_value_anim"
            ) { formattedNet ->
                Text(
                    text = formattedNet,
                    style = TabularAmountStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    color = netColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("reports_net_worth_value")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = LedgerDivider)
            Spacer(modifier = Modifier.height(9.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LedgerBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assets", style = MaterialTheme.typography.labelSmall, color = LedgerTextSecondary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = LedgerFormatters.formatWholeAmount(point.assets, symbol),
                        style = TabularAmountStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                        color = if (point.assets.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LedgerCoral)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Liabilities", style = MaterialTheme.typography.labelSmall, color = LedgerTextSecondary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = LedgerFormatters.formatWholeAmount(point.liabilities, symbol),
                        style = TabularAmountStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                        color = LedgerCoral,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NetWorthChartCard(
    series: List<NetWorthPeriodPoint>,
    selectedIndex: Int,
    symbol: String,
    mode: ReportPeriodMode,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Memoize axis scale calculation so recompositions don't scan series repeatedly
    val axisMetrics = remember(series) {
        val maxPosRaw = series.maxOfOrNull {
            maxOf(it.assets, -it.liabilities, it.netWorth, 0.0)
        } ?: 0.0
        val maxNegRaw = series.maxOfOrNull {
            maxOf(it.liabilities, -it.assets, -it.netWorth, 0.0)
        } ?: 0.0
        val nicePos = niceCeiling(maxPosRaw)
        val niceNeg = if (maxNegRaw > 0.5) niceCeiling(maxNegRaw) else 0.0
        val totalSpan = (nicePos + niceNeg).coerceAtLeast(1.0)
        val zeroRatioFromTop = (nicePos / totalSpan).toFloat().coerceIn(0.08f, 0.96f)
        listOf(nicePos, niceNeg, totalSpan, zeroRatioFromTop.toDouble())
    }
    val nicePos = axisMetrics[0]
    val niceNeg = axisMetrics[1]
    val totalSpan = axisMetrics[2]
    val zeroRatioFromTop = axisMetrics[3].toFloat()

    // Smooth chart growth animation when switching Monthly / Yearly
    val animProgress = remember { Animatable(0f) }
    val autoScrollKey = "${mode.name}_${series.size}_${series.lastOrNull()?.key}"
    LaunchedEffect(autoScrollKey) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = LedgerMotion.chartTween(260)
        )
    }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            val chartHeight = (maxWidth * 0.52f).coerceIn(176.dp, 232.dp)
            val yAxisWidth = if (maxWidth < 360.dp) 48.dp else 54.dp
            val scrollState = rememberScrollState()
            var lastAutoScrolledKey by remember { mutableStateOf("") }

            LaunchedEffect(autoScrollKey, scrollState.maxValue) {
                if (lastAutoScrolledKey != autoScrollKey && scrollState.maxValue < Int.MAX_VALUE) {
                    scrollState.scrollTo(scrollState.maxValue)
                    lastAutoScrolledKey = autoScrollKey
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header & Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assets vs Liabilities",
                        style = MaterialTheme.typography.titleSmall,
                        color = LedgerTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendPill(color = LedgerBlue, label = "Assets", isCircle = false)
                        LegendPill(color = LedgerCoral, label = "Liab.", isCircle = false)
                        LegendPill(color = LedgerAmber, label = "Net", isCircle = true)
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Latest on right • scroll horizontally for earlier ${if (mode == ReportPeriodMode.MONTHLY) "months" else "years"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight + 26.dp)
                ) {
                    // Fixed Left Y-Axis Labels
                    Box(
                        modifier = Modifier
                            .width(yAxisWidth)
                            .height(chartHeight)
                            .padding(end = 6.dp)
                    ) {
                    Text(
                        text = LedgerFormatters.formatAxisWhole(nicePos, symbol),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = LedgerTextMuted,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    if (niceNeg > 0.0) {
                        Text(
                            text = "${symbol}0",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = LedgerTextSecondary,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = (chartHeight * zeroRatioFromTop - 7.dp).coerceAtLeast(0.dp))
                        )
                        Text(
                            text = LedgerFormatters.formatAxisWhole(-niceNeg, symbol),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = LedgerCoral,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    } else {
                        Text(
                            text = LedgerFormatters.formatAxisWhole(nicePos / 2.0, symbol),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = LedgerTextMuted,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                        Text(
                            text = "${symbol}0",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = LedgerTextSecondary,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }

                // Bar Chart Canvas + X-Axis Labels
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val slotWidthDp = if (mode == ReportPeriodMode.MONTHLY) {
                        maxOf(54.dp, maxWidth / series.size.coerceAtLeast(1))
                    } else {
                        maxOf(58.dp, maxWidth / series.size.coerceAtLeast(1))
                    }
                    val totalChartWidth = slotWidthDp * series.size.coerceAtLeast(1)

                    val chartDividerColor = LedgerDivider
                    val chartHighlightColor = LedgerSurfaceHighlight
                    val chartBlueColor = LedgerBlue
                    val chartCoralColor = LedgerCoral
                    val chartBorderColor = LedgerBorder
                    val chartAmberColor = LedgerAmber
                    val chartSurfaceColor = LedgerSurface

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .horizontalScroll(scrollState)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .width(totalChartWidth)
                                .height(chartHeight)
                                .testTag("reports_net_worth_bar_canvas")
                                .pointerInput(series) {
                                    detectTapGestures { offset ->
                                        if (series.isNotEmpty()) {
                                            val slotPx = size.width.toFloat() / series.size
                                            val idx = (offset.x / slotPx).toInt().coerceIn(0, series.lastIndex)
                                            onSelectIndex(idx)
                                        }
                                    }
                                }
                        ) {
                            if (series.isEmpty()) return@Canvas
                            val progress = animProgress.value
                            val topPad = 10.dp.toPx()
                            val bottomPad = 10.dp.toPx()
                            val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                            val zeroY = topPad + plotHeight * zeroRatioFromTop
                            val pxPerUnit = plotHeight / totalSpan.toFloat()
                            val slotPx = size.width / series.size
                            val barWidth = (slotPx * 0.48f).coerceIn(10.dp.toPx(), 28.dp.toPx())
                            val cornerPx = 4.dp.toPx()

                            // Horizontal Gridlines
                            drawLine(
                                color = chartDividerColor.copy(alpha = 0.6f),
                                start = Offset(0f, topPad),
                                end = Offset(size.width, topPad),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dashEffect
                            )
                            if (niceNeg <= 0.0) {
                                val midY = topPad + plotHeight * 0.5f
                                drawLine(
                                    color = chartDividerColor.copy(alpha = 0.5f),
                                    start = Offset(0f, midY),
                                    end = Offset(size.width, midY),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            } else {
                                drawLine(
                                    color = chartDividerColor.copy(alpha = 0.6f),
                                    start = Offset(0f, topPad + plotHeight),
                                    end = Offset(size.width, topPad + plotHeight),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }

                            // Selected Period Column Highlight
                            if (selectedIndex in series.indices) {
                                val selX = selectedIndex * slotPx
                                drawRoundRect(
                                    color = chartHighlightColor.copy(alpha = 0.55f),
                                    topLeft = Offset(selX + 2.dp.toPx(), 0f),
                                    size = Size(slotPx - 4.dp.toPx(), size.height),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }

                            // Draw Assets (Blue) & Liabilities (Coral) split bars with smooth growth reveal
                            val netPoints = ArrayList<Offset>(series.size)
                            series.forEachIndexed { idx, pt ->
                                val centerX = idx * slotPx + slotPx / 2f
                                val barLeft = centerX - barWidth / 2f

                                val assetVal = pt.assets.toFloat() * progress
                                val liabVal = pt.liabilities.toFloat() * progress
                                val assetPx = abs(assetVal) * pxPerUnit
                                val liabPx = abs(liabVal) * pxPerUnit

                                val bothUp = assetVal > 0f && liabVal < 0f
                                val bothDown = assetVal < 0f && liabVal > 0f
                                val splitSideBySide = (bothUp || bothDown) && assetPx > 0.5f && liabPx > 0.5f
                                val subBarW = if (splitSideBySide) (barWidth - 2.dp.toPx()) / 2f else barWidth
                                val assetLeft = barLeft
                                val liabLeft = if (splitSideBySide) barLeft + subBarW + 2.dp.toPx() else barLeft

                                if (assetPx > 0.5f) {
                                    val topY = if (assetVal >= 0f) zeroY - assetPx else zeroY
                                    drawRoundRect(
                                        color = if (idx == selectedIndex) chartBlueColor else chartBlueColor.copy(alpha = 0.78f),
                                        topLeft = Offset(assetLeft, topY),
                                        size = Size(subBarW, assetPx),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                                    )
                                }

                                if (liabPx > 0.5f) {
                                    val topY = if (liabVal >= 0f) zeroY else zeroY - liabPx
                                    drawRoundRect(
                                        color = if (idx == selectedIndex) chartCoralColor else chartCoralColor.copy(alpha = 0.80f),
                                        topLeft = Offset(liabLeft, topY),
                                        size = Size(subBarW, liabPx),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                                    )
                                }

                                val netY = (zeroY - pt.netWorth.toFloat() * progress * pxPerUnit)
                                    .coerceIn(topPad, topPad + plotHeight)
                                netPoints.add(Offset(centerX, netY))
                            }

                            // Zero Baseline
                            drawLine(
                                color = chartBorderColor,
                                start = Offset(0f, zeroY),
                                end = Offset(size.width, zeroY),
                                strokeWidth = 1.4.dp.toPx()
                            )

                            // Net Worth Connecting Trend Line (Amber)
                            if (netPoints.size >= 2) {
                                val path = Path().apply {
                                    moveTo(netPoints.first().x, netPoints.first().y)
                                    for (i in 1 until netPoints.size) {
                                        lineTo(netPoints[i].x, netPoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = chartAmberColor,
                                    style = Stroke(
                                        width = 2.2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // Net Worth Data Point Dots
                            netPoints.forEachIndexed { idx, pt ->
                                val isSel = idx == selectedIndex
                                val outerR = if (isSel) 5.5.dp.toPx() else 3.8.dp.toPx()
                                val innerR = if (isSel) 3.4.dp.toPx() else 2.2.dp.toPx()
                                drawCircle(color = chartSurfaceColor, radius = outerR, center = pt)
                                drawCircle(color = chartAmberColor, radius = innerR, center = pt)
                            }
                        }

                        // X-Axis Labels Row
                        Row(modifier = Modifier.width(totalChartWidth).height(26.dp)) {
                            series.forEachIndexed { idx, pt ->
                                val isSel = idx == selectedIndex
                                Box(
                                    modifier = Modifier
                                        .width(slotWidthDp)
                                        .fillMaxHeight()
                                        .clickable { onSelectIndex(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pt.shortLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSel) LedgerAmber else LedgerTextSecondary,
                                        maxLines = 1
                                    )
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

@Composable
private fun LegendPill(
    color: Color,
    label: String,
    isCircle: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(if (isCircle) CircleShape else RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = LedgerTextSecondary
        )
    }
}

@Composable
private fun NetWorthPeriodTableCard(
    series: List<NetWorthPeriodPoint>,
    selectedIndex: Int,
    symbol: String,
    mode: ReportPeriodMode,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayIndices = remember(series.size) { series.indices.reversed().toList() }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == ReportPeriodMode.MONTHLY) "Month" else "Year",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(0.85f)
                )
                Text(
                    text = "Assets",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerBlue,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Liabilities",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerCoral,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Net Worth",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerAmber,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1.1f)
                )
            }
            HorizontalDivider(color = LedgerDivider)

            // Display latest period first so users see current data immediately and scroll down for history
            displayIndices.forEachIndexed { rowPos, idx ->
                val pt = series[idx]
                val isSel = idx == selectedIndex
                val rowBg by animateColorAsState(
                    targetValue = if (isSel) LedgerSurfaceHighlight.copy(alpha = 0.7f) else Color.Transparent,
                    animationSpec = LedgerMotion.microTween(100),
                    label = "report_row_bg_${pt.key}"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .background(rowBg)
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("reports_period_row_${pt.key}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSel) LedgerAmber else LedgerTextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.85f)
                    )
                    Text(
                        text = LedgerFormatters.formatWholeAmount(pt.assets, symbol, includeSymbol = false),
                        style = TabularAmountStyle.copy(fontSize = 12.sp),
                        color = if (pt.assets.roundToLong() < 0L) LedgerCoral else LedgerBlue,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = LedgerFormatters.formatWholeAmount(pt.liabilities, symbol, includeSymbol = false),
                        style = TabularAmountStyle.copy(fontSize = 12.sp),
                        color = if (pt.liabilities.roundToLong() != 0L) LedgerCoral else LedgerTextMuted,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = LedgerFormatters.formatSignedWhole(pt.netWorth, symbol),
                        style = TabularAmountStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (pt.netWorth.roundToLong() < 0L) LedgerCoral else LedgerTextPrimary,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.1f)
                    )
                }
                if (rowPos < displayIndices.lastIndex) {
                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun niceCeiling(value: Double): Double {
    if (value <= 0.0) return 1000.0
    val exp = kotlin.math.floor(kotlin.math.log10(value))
    val base = 10.0.pow(exp)
    val fraction = value / base
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return ceil(niceFraction * base)
}
