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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerTransaction
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerGreen
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerSurfaceHighlight
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.ui.theme.TabularAmountStyle
import com.example.util.LedgerFormatters
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import com.example.viewmodel.MonthlySpendingPoint
import com.example.viewmodel.SpendingCategoryMeta
import com.example.viewmodel.SpendingTrendsData
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class BarChartLayoutMode(val label: String) {
    STACKED("Stacked"),
    GROUPED("Grouped")
}

@Composable
fun SpendingTrendsScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("spending_trends_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LedgerTextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 2.dp)) {
                Text(
                    text = "Spending Trends",
                    style = MaterialTheme.typography.titleLarge,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Past 6 months by expense category",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

        SpendingTrendsContent(
            bundle = bundle,
            viewModel = viewModel,
            onEditTransaction = onEditTransaction,
            onAddExpense = onAddExpense,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpendingTrendsContent(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onEditTransaction: (LedgerTransaction) -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol
    val trendsData = remember(bundle.transactions, bundle.categories) {
        viewModel.computeSpendingTrends(bundle, monthsCount = 6)
    }

    var chartMode by remember { mutableStateOf(BarChartLayoutMode.STACKED) }
    var hiddenCategoryIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedMonthIndex by remember { mutableIntStateOf(-1) }
    var inspectedCategory by remember { mutableStateOf<SpendingCategoryMeta?>(null) }

    LaunchedEffect(trendsData.months.size, trendsData.sixMonthTotal) {
        if (trendsData.months.isNotEmpty() && selectedMonthIndex !in trendsData.months.indices) {
            val latestWithData = trendsData.months.indexOfLast { it.totalExpense > 0.0 }
            selectedMonthIndex = if (latestWithData >= 0) latestWithData else trendsData.months.lastIndex
        }
    }

    val safeSelectedIdx = if (selectedMonthIndex in trendsData.months.indices) {
        selectedMonthIndex
    } else {
        (trendsData.months.size - 1).coerceAtLeast(0)
    }

    val visibleCategories = remember(trendsData.categories, hiddenCategoryIds) {
        trendsData.categories.filter { it.categoryId !in hiddenCategoryIds }
    }
    val visibleCategoryIdSet = remember(visibleCategories) {
        visibleCategories.map { it.categoryId }.toSet()
    }

    val filteredMonthTotals = remember(trendsData.months, visibleCategoryIdSet) {
        trendsData.months.map { month ->
            month.byCategory.entries
                .filter { it.key in visibleCategoryIdSet }
                .sumOf { it.value }
        }
    }
    val filteredSixMonthTotal = remember(filteredMonthTotals) { filteredMonthTotals.sum() }
    val filteredAvg = if (filteredMonthTotals.isNotEmpty()) filteredSixMonthTotal / filteredMonthTotals.size else 0.0
    val filteredMomPct = remember(filteredMonthTotals) {
        val latest = filteredMonthTotals.lastOrNull() ?: 0.0
        val prev = filteredMonthTotals.getOrNull(filteredMonthTotals.lastIndex - 1) ?: 0.0
        if (prev > 0.0) ((latest - prev) / prev) * 100.0 else null
    }

    if (trendsData.sixMonthTotal <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 360.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = LedgerSurfaceElevated,
                    border = BorderStroke(1.dp, LedgerBorder),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            tint = LedgerCoral,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Expenses in the Last 6 Months",
                    style = MaterialTheme.typography.titleMedium,
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Record expense entries to visualize your 6-month spending trends categorized by expense type.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAddExpense,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LedgerCoral,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("spending_trends_empty_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Expense", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 6-Month Summary KPI Strip
        item(key = "spending_kpis", contentType = "kpi_strip") {
            SpendingKpiSummaryCard(
                sixMonthTotal = filteredSixMonthTotal,
                monthlyAvg = filteredAvg,
                momChangePct = filteredMomPct,
                dateRangeLabel = "${trendsData.months.first().shortLabel} – ${trendsData.months.last().fullLabel}",
                activeCategoryCount = visibleCategories.size,
                totalCategoryCount = trendsData.categories.size,
                symbol = symbol,
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
            )
        }

        // 2. Interactive Stacked / Grouped Bar Chart + Tooltip + Category Filter Legend
        item(key = "spending_bar_chart", contentType = "bar_chart_card") {
            SpendingBarChartCard(
                trendsData = trendsData,
                visibleCategories = visibleCategories,
                hiddenCategoryIds = hiddenCategoryIds,
                selectedMonthIndex = safeSelectedIdx,
                chartMode = chartMode,
                symbol = symbol,
                onChangeMode = { chartMode = it },
                onSelectMonth = { selectedMonthIndex = it },
                onToggleCategory = { catId ->
                    hiddenCategoryIds = if (catId in hiddenCategoryIds) {
                        hiddenCategoryIds - catId
                    } else if (visibleCategories.size > 1) {
                        hiddenCategoryIds + catId
                    } else {
                        // Tapping the sole remaining visible category resets all back to visible
                        emptySet()
                    }
                },
                onResetFilters = { hiddenCategoryIds = emptySet() },
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
            )
        }

        // 3. 6-Month Category Breakdown List
        item(key = "spending_category_breakdown", contentType = "breakdown_card") {
            SpendingCategoryBreakdownCard(
                trendsData = trendsData,
                hiddenCategoryIds = hiddenCategoryIds,
                symbol = symbol,
                onInspectCategory = { inspectedCategory = it },
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
            )
        }
    }

    // Drill-down Bottom Sheet for inspecting entries in a category across the 6 months
    inspectedCategory?.let { catMeta ->
        val catTransactions = remember(trendsData.months, catMeta.categoryId) {
            trendsData.months.flatMap { month ->
                month.transactions.filter { (it.categoryId ?: "uncategorized") == catMeta.categoryId }
            }.sortedByDescending { it.epochDay }
        }
        val catColor = remember(catMeta.colorHex) { LedgerFormatters.parseColorHex(catMeta.colorHex) }

        ModalBottomSheet(
            onDismissRequest = { inspectedCategory = null },
            containerColor = LedgerSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(catColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = catMeta.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = LedgerTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${catTransactions.size} ${if (catTransactions.size == 1) "expense" else "expenses"} in past 6 months",
                                style = MaterialTheme.typography.labelSmall,
                                color = LedgerTextMuted
                            )
                        }
                    }
                    Text(
                        text = LedgerFormatters.formatWholeAmount(catMeta.sixMonthTotal, symbol),
                        style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = LedgerCoral,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                HorizontalDivider(color = LedgerDivider)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(catTransactions, key = { it.id }) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inspectedCategory = null
                                    onEditTransaction(tx)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = tx.note.ifBlank { tx.subcategory ?: catMeta.name },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LedgerTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${LedgerFormatters.formatShortDate(tx.epochDay)} • ${bundle.accountName(tx.accountId)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = LedgerFormatters.formatAmount(tx.amount, symbol),
                                style = TabularAmountStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                color = LedgerCoral,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingKpiSummaryCard(
    sixMonthTotal: Double,
    monthlyAvg: Double,
    momChangePct: Double?,
    dateRangeLabel: String,
    activeCategoryCount: Int,
    totalCategoryCount: Int,
    symbol: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6-Month Spending",
                    style = MaterialTheme.typography.labelLarge,
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LedgerSurfaceElevated,
                    border = BorderStroke(1.dp, LedgerBorder)
                ) {
                    Text(
                        text = if (activeCategoryCount == totalCategoryCount) {
                            dateRangeLabel
                        } else {
                            "$activeCategoryCount/$totalCategoryCount types"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpendingMetricBox(
                    label = "6-Mo Total",
                    value = LedgerFormatters.formatWholeAmount(sixMonthTotal, symbol),
                    valueColor = LedgerCoral,
                    modifier = Modifier.weight(1.1f)
                )
                SpendingMetricBox(
                    label = "Monthly Avg",
                    value = LedgerFormatters.formatWholeAmount(monthlyAvg, symbol),
                    valueColor = LedgerTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                val momColor = when {
                    momChangePct == null -> LedgerTextMuted
                    momChangePct > 0.5 -> LedgerCoral
                    momChangePct < -0.5 -> LedgerGreen
                    else -> LedgerTextSecondary
                }
                val momText = if (momChangePct != null) {
                    val sign = if (momChangePct >= 0.0) "+" else "-"
                    "$sign${String.format(Locale.US, "%.0f", abs(momChangePct))}%"
                } else {
                    "—"
                }
                SpendingMetricBox(
                    label = "MoM Trend",
                    value = momText,
                    valueColor = momColor,
                    trendUp = momChangePct?.let { it > 0.5 },
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

@Composable
private fun SpendingMetricBox(
    label: String,
    value: String,
    valueColor: Color,
    trendUp: Boolean? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trendUp != null) {
                    Icon(
                        imageVector = if (trendUp) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = null,
                        tint = valueColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = value,
                    style = TabularAmountStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = valueColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendingBarChartCard(
    trendsData: SpendingTrendsData,
    visibleCategories: List<SpendingCategoryMeta>,
    hiddenCategoryIds: Set<String>,
    selectedMonthIndex: Int,
    chartMode: BarChartLayoutMode,
    symbol: String,
    onChangeMode: (BarChartLayoutMode) -> Unit,
    onSelectMonth: (Int) -> Unit,
    onToggleCategory: (String) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val months = trendsData.months
    val visibleIdSet = remember(visibleCategories) { visibleCategories.map { it.categoryId }.toSet() }
    val colorMap = remember(trendsData.categories) {
        trendsData.categories.associate { it.categoryId to LedgerFormatters.parseColorHex(it.colorHex) }
    }

    // In Grouped mode, show top 4 visible categories as side-by-side bars to keep every bar crisp and legible
    val groupedCategories = remember(visibleCategories) { visibleCategories.take(4) }

    val maxBarValue = remember(months, visibleCategories, groupedCategories, chartMode) {
        val rawMax = when (chartMode) {
            BarChartLayoutMode.STACKED -> months.maxOfOrNull { month ->
                month.byCategory.entries.filter { it.key in visibleIdSet }.sumOf { it.value }
            } ?: 0.0
            BarChartLayoutMode.GROUPED -> months.maxOfOrNull { month ->
                groupedCategories.maxOfOrNull { cat -> month.byCategory[cat.categoryId] ?: 0.0 } ?: 0.0
            } ?: 0.0
        }
        niceSpendingCeiling(rawMax)
    }

    val animProgress = remember { Animatable(0f) }
    val animKey = "${chartMode.name}_${visibleCategories.size}_${hiddenCategoryIds.hashCode()}_${months.sumOf { it.totalExpense }}"
    LaunchedEffect(animKey) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
    }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }
    val selectedMonth = months.getOrNull(selectedMonthIndex)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            val chartHeight = (maxWidth * 0.56f).coerceIn(188.dp, 244.dp)
            val yAxisWidth = if (maxWidth < 360.dp) 46.dp else 52.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header + Stacked / Grouped Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Monthly Spending by Type",
                            style = MaterialTheme.typography.titleSmall,
                            color = LedgerTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap any month bar to inspect breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LedgerBackground,
                        border = BorderStroke(1.dp, LedgerBorder)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            BarChartLayoutMode.entries.forEach { mode ->
                                val active = chartMode == mode
                                val bg by animateColorAsState(
                                    targetValue = if (active) LedgerCoral else Color.Transparent,
                                    animationSpec = tween(160),
                                    label = "chart_mode_bg_${mode.name}"
                                )
                                val fg by animateColorAsState(
                                    targetValue = if (active) Color.White else LedgerTextSecondary,
                                    animationSpec = tween(160),
                                    label = "chart_mode_fg_${mode.name}"
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .clickable { onChangeMode(mode) }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                        .testTag("spending_chart_mode_${mode.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = fg,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bar Chart Plot Area (Y-Axis + Canvas + X-Axis Labels)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight + 26.dp)
                ) {
                    // Left Y-Axis Labels
                    Box(
                        modifier = Modifier
                            .width(yAxisWidth)
                            .height(chartHeight)
                            .padding(end = 6.dp)
                    ) {
                        Text(
                            text = LedgerFormatters.formatAxisWhole(maxBarValue, symbol),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = LedgerTextMuted,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        Text(
                            text = LedgerFormatters.formatAxisWhole(maxBarValue / 2.0, symbol),
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

                    // Bars Canvas + Month X-Axis
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight)
                                .testTag("spending_trends_bar_canvas")
                                .pointerInput(months) {
                                    detectTapGestures { offset ->
                                        if (months.isNotEmpty()) {
                                            val slotPx = size.width.toFloat() / months.size
                                            val idx = (offset.x / slotPx).toInt().coerceIn(0, months.lastIndex)
                                            onSelectMonth(idx)
                                        }
                                    }
                                }
                        ) {
                            if (months.isEmpty()) return@Canvas
                            val progress = animProgress.value
                            val topPad = 8.dp.toPx()
                            val bottomPad = 4.dp.toPx()
                            val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                            val baselineY = topPad + plotHeight
                            val slotPx = size.width / months.size
                            val cornerPx = 4.dp.toPx()

                            // Horizontal dashed gridlines (100%, 75%, 50%, 25%)
                            for (step in 0..3) {
                                val y = topPad + plotHeight * (step / 4f)
                                drawLine(
                                    color = LedgerDivider.copy(alpha = if (step % 2 == 0) 0.65f else 0.35f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }

                            // Selected Month Highlight Column
                            if (selectedMonthIndex in months.indices) {
                                val selX = selectedMonthIndex * slotPx
                                drawRoundRect(
                                    color = LedgerSurfaceHighlight.copy(alpha = 0.55f),
                                    topLeft = Offset(selX + 2.dp.toPx(), 0f),
                                    size = Size((slotPx - 4.dp.toPx()).coerceAtLeast(1f), size.height),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }

                            when (chartMode) {
                                BarChartLayoutMode.STACKED -> {
                                    val barWidth = (slotPx * 0.54f).coerceIn(14.dp.toPx(), 34.dp.toPx())
                                    months.forEachIndexed { idx, month ->
                                        val isSelected = idx == selectedMonthIndex
                                        val barLeft = idx * slotPx + (slotPx - barWidth) / 2f
                                        val alpha = if (isSelected) 1f else 0.84f
                                        var currentBottomY = baselineY

                                        val activeSlices = visibleCategories.mapNotNull { cat ->
                                            val amount = month.byCategory[cat.categoryId] ?: 0.0
                                            if (amount > 0.0) cat to amount else null
                                        }

                                        activeSlices.forEachIndexed { sliceIdx, (cat, amount) ->
                                            val segHeight = ((amount / maxBarValue).toFloat() * plotHeight * progress)
                                                .coerceAtLeast(0f)
                                            if (segHeight > 0.5f) {
                                                val segTop = (currentBottomY - segHeight).coerceAtLeast(topPad)
                                                val actualH = (currentBottomY - segTop).coerceAtLeast(0.5f)
                                                val fillColor = (colorMap[cat.categoryId] ?: LedgerCoral).copy(alpha = alpha)
                                                val isTopSegment = sliceIdx == activeSlices.lastIndex

                                                if (isTopSegment) {
                                                    drawRoundRect(
                                                        color = fillColor,
                                                        topLeft = Offset(barLeft, segTop),
                                                        size = Size(barWidth, actualH),
                                                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                                                    )
                                                    // Flatten bottom corners when stacked on top of another segment
                                                    if (sliceIdx > 0 && actualH > cornerPx) {
                                                        drawRect(
                                                            color = fillColor,
                                                            topLeft = Offset(barLeft, currentBottomY - cornerPx),
                                                            size = Size(barWidth, cornerPx)
                                                        )
                                                    }
                                                } else {
                                                    drawRect(
                                                        color = fillColor,
                                                        topLeft = Offset(barLeft, segTop),
                                                        size = Size(barWidth, actualH)
                                                    )
                                                }
                                                currentBottomY = segTop
                                            }
                                        }
                                    }
                                }

                                BarChartLayoutMode.GROUPED -> {
                                    val groupCount = groupedCategories.size.coerceAtLeast(1)
                                    val groupAreaWidth = (slotPx * 0.78f).coerceAtLeast(12.dp.toPx())
                                    val gap = 2.dp.toPx()
                                    val singleBarWidth = ((groupAreaWidth - gap * (groupCount - 1)) / groupCount)
                                        .coerceIn(4.dp.toPx(), 16.dp.toPx())
                                    val totalUsed = singleBarWidth * groupCount + gap * (groupCount - 1)

                                    months.forEachIndexed { idx, month ->
                                        val isSelected = idx == selectedMonthIndex
                                        val alpha = if (isSelected) 1f else 0.82f
                                        val startX = idx * slotPx + (slotPx - totalUsed) / 2f

                                        groupedCategories.forEachIndexed { cIdx, cat ->
                                            val amount = month.byCategory[cat.categoryId] ?: 0.0
                                            val barH = ((amount / maxBarValue).toFloat() * plotHeight * progress)
                                                .coerceAtLeast(0f)
                                            if (barH > 0.5f) {
                                                val x = startX + cIdx * (singleBarWidth + gap)
                                                val y = (baselineY - barH).coerceAtLeast(topPad)
                                                val fillColor = (colorMap[cat.categoryId] ?: LedgerCoral).copy(alpha = alpha)
                                                drawRoundRect(
                                                    color = fillColor,
                                                    topLeft = Offset(x, y),
                                                    size = Size(singleBarWidth, (baselineY - y).coerceAtLeast(0.5f)),
                                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Crisp baseline axis
                            drawLine(
                                color = LedgerBorder,
                                start = Offset(0f, baselineY),
                                end = Offset(size.width, baselineY),
                                strokeWidth = 1.4.dp.toPx()
                            )
                        }

                        // X-Axis Month Labels
                        Row(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                            months.forEachIndexed { idx, month ->
                                val isSelected = idx == selectedMonthIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onSelectMonth(idx) }
                                        .testTag("spending_month_axis_${month.key}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = month.shortLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) LedgerAmber else LedgerTextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Recharts-style Tooltip Panel for the Selected Month
                if (selectedMonth != null) {
                    AnimatedContent(
                        targetState = selectedMonth to visibleIdSet,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(110)) },
                        label = "spending_month_tooltip"
                    ) { (month, activeIds) ->
                        SelectedMonthTooltipCard(
                            month = month,
                            categories = visibleCategories,
                            activeIds = activeIds,
                            colorMap = colorMap,
                            symbol = symbol
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Expense Category Legend / Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (chartMode == BarChartLayoutMode.GROUPED && visibleCategories.size > 4) {
                            "Filter Expense Types (top 4 shown grouped)"
                        } else {
                            "Filter by Expense Type"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted
                    )
                    if (hiddenCategoryIds.isNotEmpty()) {
                        Text(
                            text = "Show All",
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onResetFilters)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("spending_trends_reset_filter")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    trendsData.categories.forEach { cat ->
                        val isVisible = cat.categoryId !in hiddenCategoryIds
                        val catColor = colorMap[cat.categoryId] ?: LedgerCoral
                        val chipBg by animateColorAsState(
                            targetValue = if (isVisible) catColor.copy(alpha = 0.16f) else LedgerBackground,
                            animationSpec = tween(150),
                            label = "legend_bg_${cat.categoryId}"
                        )
                        val chipBorder by animateColorAsState(
                            targetValue = if (isVisible) catColor.copy(alpha = 0.65f) else LedgerBorder,
                            animationSpec = tween(150),
                            label = "legend_border_${cat.categoryId}"
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, chipBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onToggleCategory(cat.categoryId) }
                                .testTag("spending_legend_${cat.categoryId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isVisible) catColor else LedgerTextMuted.copy(alpha = 0.4f))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isVisible) LedgerTextPrimary else LedgerTextMuted,
                                    fontWeight = if (isVisible) FontWeight.Medium else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
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
private fun SelectedMonthTooltipCard(
    month: MonthlySpendingPoint,
    categories: List<SpendingCategoryMeta>,
    activeIds: Set<String>,
    colorMap: Map<String, Color>,
    symbol: String
) {
    val breakdown = remember(month, categories, activeIds) {
        categories.mapNotNull { cat ->
            val amt = month.byCategory[cat.categoryId] ?: 0.0
            if (cat.categoryId in activeIds && amt > 0.0) cat to amt else null
        }.sortedByDescending { it.second }
    }
    val monthTotal = breakdown.sumOf { it.second }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerBorder),
        modifier = Modifier.fillMaxWidth().testTag("spending_month_tooltip")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month.fullLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = LedgerAmber,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = LedgerFormatters.formatWholeAmount(monthTotal, symbol),
                    style = TabularAmountStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (breakdown.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No matching expenses recorded in ${month.fullLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))

                breakdown.take(6).forEach { (cat, amt) ->
                    val pct = if (monthTotal > 0.0) (amt / monthTotal * 100.0).roundToInt() else 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colorMap[cat.categoryId] ?: LedgerCoral)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = LedgerTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = "$pct%",
                            style = TabularAmountStyle.copy(fontSize = 11.sp),
                            color = LedgerTextMuted,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = LedgerFormatters.formatWholeAmount(amt, symbol),
                            style = TabularAmountStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = LedgerTextPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                if (breakdown.size > 6) {
                    Text(
                        text = "+ ${breakdown.size - 6} more expense categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingCategoryBreakdownCard(
    trendsData: SpendingTrendsData,
    hiddenCategoryIds: Set<String>,
    symbol: String,
    onInspectCategory: (SpendingCategoryMeta) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expense Types (6-Month Total)",
                    style = MaterialTheme.typography.titleSmall,
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Tap to view entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)

            trendsData.categories.forEachIndexed { idx, cat ->
                val isMuted = cat.categoryId in hiddenCategoryIds
                val catColor = remember(cat.colorHex) { LedgerFormatters.parseColorHex(cat.colorHex) }
                val share = if (trendsData.sixMonthTotal > 0.0) {
                    (cat.sixMonthTotal / trendsData.sixMonthTotal).toFloat().coerceIn(0f, 1f)
                } else 0f
                val animatedShare by animateFloatAsState(
                    targetValue = if (isMuted) 0f else share,
                    animationSpec = tween(320),
                    label = "cat_share_${cat.categoryId}"
                )
                val pctInt = (share * 100f).roundToInt()
                val avgMonthly = cat.sixMonthTotal / trendsData.months.size.coerceAtLeast(1)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInspectCategory(cat) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("spending_category_row_${cat.categoryId}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isMuted) LedgerTextMuted.copy(alpha = 0.4f) else catColor)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMuted) LedgerTextMuted else LedgerTextPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Avg ${LedgerFormatters.formatWholeAmount(avgMonthly, symbol)}/mo • ${cat.transactionCount} ${if (cat.transactionCount == 1) "entry" else "entries"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LedgerTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = LedgerFormatters.formatWholeAmount(cat.sixMonthTotal, symbol),
                                style = TabularAmountStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                                color = if (isMuted) LedgerTextMuted else LedgerTextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "$pctInt%",
                                style = TabularAmountStyle.copy(fontSize = 11.sp),
                                color = if (isMuted) LedgerTextMuted else catColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LedgerBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedShare)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(catColor)
                        )
                    }
                }
                if (idx < trendsData.categories.lastIndex) {
                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun niceSpendingCeiling(rawMax: Double): Double {
    if (rawMax <= 0.0) return 100.0
    val exp = kotlin.math.floor(kotlin.math.log10(rawMax))
    val base = 10.0.pow(exp)
    val fraction = rawMax / base
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 1.5 -> 1.5
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 3.0 -> 3.0
        fraction <= 4.0 -> 4.0
        fraction <= 5.0 -> 5.0
        fraction <= 6.0 -> 6.0
        fraction <= 8.0 -> 8.0
        else -> 10.0
    }
    return ceil(niceFraction * base)
}
