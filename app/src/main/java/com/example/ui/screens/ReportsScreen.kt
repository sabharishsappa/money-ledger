package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.ReportPeriodMode
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
import com.example.viewmodel.NetWorthPeriodPoint
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

@Composable
fun ReportsScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onSelectAccount: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol
    val todayCal = remember { Calendar.getInstance() }
    val currentYear = remember { todayCal.get(Calendar.YEAR) }
    val currentMonth = remember { todayCal.get(Calendar.MONTH) }

    var periodMode by remember { mutableStateOf(ReportPeriodMode.MONTHLY) }
    var anchorYear by remember { mutableIntStateOf(currentYear) }
    var selectedIndex by remember { mutableIntStateOf(currentMonth) }
    var accountsExpanded by remember { mutableStateOf(true) }

    val series = remember(bundle, periodMode, anchorYear) {
        viewModel.computeNetWorthSeries(bundle, periodMode, anchorYear)
    }

    LaunchedEffect(periodMode, anchorYear) {
        selectedIndex = when (periodMode) {
            ReportPeriodMode.MONTHLY -> if (anchorYear == currentYear) currentMonth else 11
            ReportPeriodMode.YEARLY -> (series.size - 1).coerceAtLeast(0)
        }
    }

    val safeSelectedIdx = selectedIndex.coerceIn(0, (series.size - 1).coerceAtLeast(0))
    val selectedPoint = series.getOrNull(safeSelectedIdx)

    val accountSnapshots = remember(bundle, selectedPoint?.cutoffEpochDay) {
        selectedPoint?.let { viewModel.computeNetWorthAccountSnapshots(bundle, it.cutoffEpochDay) }.orEmpty()
    }
    val includedCount = bundle.netWorthIncludedAccounts.size
    val excludedAccounts = bundle.netWorthExcludedAccounts

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Reports",
                    style = MaterialTheme.typography.titleLarge,
                    color = LedgerTextPrimary
                )
                Text(
                    text = "Net Worth Summary",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary
                )
            }

            // Monthly / Yearly Segmented Switcher
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LedgerBackground,
                border = BorderStroke(1.dp, LedgerBorder)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    ReportPeriodMode.entries.forEach { mode ->
                        val active = periodMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (active) LedgerCoral else Color.Transparent)
                                .clickable { periodMode = mode }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("reports_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (active) Color.White else LedgerTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Period Range Navigator (< 2026 > or < 2021 – 2026 >)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    anchorYear -= if (periodMode == ReportPeriodMode.MONTHLY) 1 else 6
                },
                modifier = Modifier.testTag("reports_prev_period")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous period",
                    tint = LedgerTextPrimary
                )
            }
            Text(
                text = if (periodMode == ReportPeriodMode.MONTHLY) {
                    "$anchorYear"
                } else {
                    "${anchorYear - 5} – $anchorYear"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LedgerTextPrimary
            )
            IconButton(
                onClick = {
                    anchorYear += if (periodMode == ReportPeriodMode.MONTHLY) 1 else 6
                },
                modifier = Modifier.testTag("reports_next_period")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next period",
                    tint = LedgerTextPrimary
                )
            }
        }

        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

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
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Net Worth KPI Card for Selected Period
                if (selectedPoint != null) {
                    item(key = "net_worth_kpi") {
                        NetWorthHeroCard(
                            point = selectedPoint,
                            symbol = symbol,
                            includedCount = includedCount,
                            excludedCount = excludedAccounts.size
                        )
                    }
                }

                // Dual-Color Split Bar Chart (Assets vs Liabilities + Net Worth Line)
                item(key = "net_worth_bar_chart") {
                    NetWorthChartCard(
                        series = series,
                        selectedIndex = safeSelectedIdx,
                        symbol = symbol,
                        mode = periodMode,
                        onSelectIndex = { idx -> selectedIndex = idx }
                    )
                }

                // Period Breakdown Table (Monthly or Yearly rows)
                item(key = "period_table_card") {
                    NetWorthPeriodTableCard(
                        series = series,
                        selectedIndex = safeSelectedIdx,
                        symbol = symbol,
                        mode = periodMode,
                        onSelectIndex = { idx -> selectedIndex = idx }
                    )
                }

                // Included Accounts Snapshot Card (Collapsible)
                item(key = "included_accounts_header") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LedgerSurface,
                        border = BorderStroke(1.dp, LedgerDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { accountsExpanded = !accountsExpanded }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("reports_accounts_toggle"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (accountsExpanded) {
                                            Icons.Filled.KeyboardArrowDown
                                        } else {
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                                        },
                                        contentDescription = if (accountsExpanded) "Collapse accounts" else "Expand accounts",
                                        tint = LedgerTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Accounts Included in Net Worth ($includedCount)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = LedgerTextPrimary
                                        )
                                        selectedPoint?.let { pt ->
                                            Text(
                                                text = "Balances as of ${pt.fullLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LedgerTextMuted
                                            )
                                        }
                                    }
                                }
                                if (selectedPoint != null) {
                                    Text(
                                        text = LedgerFormatters.formatSignedWhole(selectedPoint.netWorth, symbol),
                                        style = TabularAmountStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                        color = if (selectedPoint.netWorth.roundToLong() < 0L) LedgerCoral else LedgerAmber
                                    )
                                }
                            }

                            if (accountsExpanded) {
                                HorizontalDivider(color = LedgerDivider)
                                if (accountSnapshots.isEmpty()) {
                                    Text(
                                        text = "No accounts are currently included in Net Worth. Edit an account to include it.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LedgerTextMuted,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                } else {
                                    accountSnapshots.forEachIndexed { index, snap ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectAccount(snap.account) }
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                                Text(
                                                    text = snap.account.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = LedgerTextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = snap.groupName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LedgerTextMuted
                                                )
                                            }
                                            Text(
                                                text = LedgerFormatters.formatSignedWhole(snap.balance, symbol),
                                                style = TabularAmountStyle.copy(fontSize = 13.5.sp),
                                                color = if (snap.balance.roundToLong() < 0L) LedgerCoral else LedgerBlue
                                            )
                                        }
                                        if (index < accountSnapshots.lastIndex) {
                                            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                        }
                                    }
                                }

                                if (excludedAccounts.isNotEmpty()) {
                                    HorizontalDivider(color = LedgerDivider)
                                    Text(
                                        text = "Excluded from Net Worth: ${excludedAccounts.joinToString(", ") { it.name }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerTextMuted,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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

@Composable
private fun NetWorthHeroCard(
    point: NetWorthPeriodPoint,
    symbol: String,
    includedCount: Int,
    excludedCount: Int
) {
    val netColor = when {
        point.netWorth.roundToLong() < 0L -> LedgerCoral
        point.netWorth.roundToLong() > 0L -> LedgerGreen
        else -> LedgerTextPrimary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reports_net_worth_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Worth • ${point.fullLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    color = LedgerTextSecondary
                )
                Text(
                    text = if (excludedCount > 0) {
                        "$includedCount incl. • $excludedCount excl."
                    } else {
                        "$includedCount accounts"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = LedgerFormatters.formatSignedWhole(point.netWorth, symbol),
                style = TabularAmountStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                color = netColor,
                modifier = Modifier.testTag("reports_net_worth_value")
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = LedgerDivider)
            Spacer(modifier = Modifier.height(10.dp))

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
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = LedgerFormatters.formatWholeAmount(point.assets, symbol),
                        style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = LedgerBlue
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
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = LedgerFormatters.formatWholeAmount(point.liabilities, symbol),
                        style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = LedgerCoral
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
    onSelectIndex: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assets vs Liabilities",
                    style = MaterialTheme.typography.titleSmall,
                    color = LedgerTextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendPill(color = LedgerBlue, label = "Assets", isCircle = false)
                    LegendPill(color = LedgerCoral, label = "Liab.", isCircle = false)
                    LegendPill(color = LedgerAmber, label = "Net", isCircle = true)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap a bar to inspect that ${if (mode == ReportPeriodMode.MONTHLY) "month" else "year"}",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            val maxPosRaw = series.maxOfOrNull { max(it.assets, max(it.netWorth, 0.0)) } ?: 0.0
            val maxNegRaw = series.maxOfOrNull { max(it.liabilities, max(-it.netWorth, 0.0)) } ?: 0.0
            val nicePos = niceCeiling(maxPosRaw)
            val niceNeg = if (maxNegRaw > 0.5) niceCeiling(maxNegRaw) else 0.0
            val totalSpan = (nicePos + niceNeg).coerceAtLeast(1.0)
            val zeroRatioFromTop = (nicePos / totalSpan).toFloat().coerceIn(0.08f, 0.96f)

            val chartHeight = 220.dp
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 28.dp)
            ) {
                // Fixed Left Y-Axis Labels
                Box(
                    modifier = Modifier
                        .width(56.dp)
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
                            color = LedgerTextMuted,
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
                        maxOf(42.dp, maxWidth / series.size.coerceAtLeast(1))
                    } else {
                        maxOf(54.dp, maxWidth / series.size.coerceAtLeast(1))
                    }
                    val totalChartWidth = slotWidthDp * series.size.coerceAtLeast(1)

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
                            val topPad = 10.dp.toPx()
                            val bottomPad = 10.dp.toPx()
                            val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                            val zeroY = topPad + plotHeight * zeroRatioFromTop
                            val pxPerUnit = plotHeight / totalSpan.toFloat()
                            val slotPx = size.width / series.size
                            val barWidth = (slotPx * 0.48f).coerceIn(10.dp.toPx(), 28.dp.toPx())
                            val cornerPx = 4.dp.toPx()

                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                            // Horizontal Gridlines
                            drawLine(
                                color = LedgerDivider.copy(alpha = 0.6f),
                                start = Offset(0f, topPad),
                                end = Offset(size.width, topPad),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dashEffect
                            )
                            if (niceNeg <= 0.0) {
                                val midY = topPad + plotHeight * 0.5f
                                drawLine(
                                    color = LedgerDivider.copy(alpha = 0.5f),
                                    start = Offset(0f, midY),
                                    end = Offset(size.width, midY),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            } else {
                                drawLine(
                                    color = LedgerDivider.copy(alpha = 0.6f),
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
                                    color = LedgerSurfaceHighlight.copy(alpha = 0.55f),
                                    topLeft = Offset(selX + 2.dp.toPx(), 0f),
                                    size = Size(slotPx - 4.dp.toPx(), size.height),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }

                            // Draw Assets (Blue upward) & Liabilities (Coral downward) split bars
                            val netPoints = ArrayList<Offset>(series.size)
                            series.forEachIndexed { idx, pt ->
                                val centerX = idx * slotPx + slotPx / 2f
                                val barLeft = centerX - barWidth / 2f

                                val assetH = (pt.assets.toFloat() * pxPerUnit).coerceAtLeast(0f)
                                if (assetH > 0.5f) {
                                    drawRoundRect(
                                        color = if (idx == selectedIndex) LedgerBlue else LedgerBlue.copy(alpha = 0.78f),
                                        topLeft = Offset(barLeft, zeroY - assetH),
                                        size = Size(barWidth, assetH),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                                    )
                                }

                                val liabH = (pt.liabilities.toFloat() * pxPerUnit).coerceAtLeast(0f)
                                if (liabH > 0.5f) {
                                    drawRoundRect(
                                        color = if (idx == selectedIndex) LedgerCoral else LedgerCoral.copy(alpha = 0.80f),
                                        topLeft = Offset(barLeft, zeroY),
                                        size = Size(barWidth, liabH),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                                    )
                                }

                                val netY = (zeroY - pt.netWorth.toFloat() * pxPerUnit)
                                    .coerceIn(topPad, topPad + plotHeight)
                                netPoints.add(Offset(centerX, netY))
                            }

                            // Zero Baseline
                            drawLine(
                                color = LedgerBorder,
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
                                    color = LedgerAmber,
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
                                drawCircle(color = LedgerSurface, radius = outerR, center = pt)
                                drawCircle(color = LedgerAmber, radius = innerR, center = pt)
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
    onSelectIndex: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == ReportPeriodMode.MONTHLY) "Month" else "Year",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerTextSecondary,
                    modifier = Modifier.weight(0.9f)
                )
                Text(
                    text = "Assets",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerBlue,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Liabilities",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerCoral,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Net Worth",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerAmber,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.1f)
                )
            }
            HorizontalDivider(color = LedgerDivider)

            series.forEachIndexed { idx, pt ->
                val isSel = idx == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSel) LedgerSurfaceHighlight.copy(alpha = 0.7f) else Color.Transparent)
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("reports_period_row_${pt.key}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSel) LedgerAmber else LedgerTextPrimary,
                        modifier = Modifier.weight(0.9f)
                    )
                    Text(
                        text = LedgerFormatters.formatWholeAmount(pt.assets, symbol, includeSymbol = false),
                        style = TabularAmountStyle.copy(fontSize = 12.sp),
                        color = LedgerBlue,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = LedgerFormatters.formatWholeAmount(pt.liabilities, symbol, includeSymbol = false),
                        style = TabularAmountStyle.copy(fontSize = 12.sp),
                        color = if (pt.liabilities.roundToLong() > 0L) LedgerCoral else LedgerTextMuted,
                        textAlign = TextAlign.End,
                        maxLines = 1,
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.1f)
                    )
                }
                if (idx < series.lastIndex) {
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
