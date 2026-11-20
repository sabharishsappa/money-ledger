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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.example.data.AccountNature
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
import com.example.viewmodel.CreditCardReportCalculator
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

/** Reports available in the Reports tab. */
enum class ReportKind(val label: String) {
    NET_WORTH("Net Worth"),
    CREDIT_CARDS("Credit Cards")
}

@Composable
fun ReportsScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onSelectAccount: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol

    var periodMode by remember { mutableStateOf(ReportPeriodMode.MONTHLY) }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var reportKind by remember { mutableStateOf(ReportKind.NET_WORTH) }

    val series = remember(bundle, periodMode) {
        viewModel.computeNetWorthSeries(bundle, periodMode)
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
                    text = when (reportKind) {
                        ReportKind.NET_WORTH ->
                            series.lastOrNull()?.let { "Net Worth • Till ${it.fullLabel}" } ?: "Net Worth Summary"
                        ReportKind.CREDIT_CARDS -> "Credit Card Spending & Dues"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary
                )
            }

            // Monthly / Yearly Dropdown Selector
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
                            color = LedgerTextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Select report period",
                            tint = LedgerTextSecondary,
                            modifier = Modifier.size(18.dp)
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

        // Report picker (Net Worth / Credit Cards)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportKind.entries.forEach { kind ->
                val active = reportKind == kind
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (active) LedgerCoral.copy(alpha = 0.16f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (active) LedgerCoral else LedgerBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { reportKind = kind }
                        .testTag("reports_kind_${kind.name.lowercase()}")
                ) {
                    Text(
                        text = kind.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) LedgerCoral else LedgerTextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

        if (reportKind == ReportKind.CREDIT_CARDS) {
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
        } else if (bundle.activeAccounts.isEmpty()) {
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
                            excludedCount = excludedCount
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
                text = "Latest on right • scroll horizontally for earlier ${if (mode == ReportPeriodMode.MONTHLY) "months" else "years"}",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

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

            val chartHeight = 220.dp
            val scrollState = rememberScrollState()
            val autoScrollKey = "${mode.name}_${series.size}_${series.lastOrNull()?.key}"
            var lastAutoScrolledKey by remember { mutableStateOf("") }

            LaunchedEffect(autoScrollKey, scrollState.maxValue) {
                if (lastAutoScrolledKey != autoScrollKey && scrollState.maxValue < Int.MAX_VALUE) {
                    scrollState.scrollTo(scrollState.maxValue)
                    lastAutoScrolledKey = autoScrollKey
                }
            }

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
                        maxOf(54.dp, maxWidth / series.size.coerceAtLeast(1))
                    } else {
                        maxOf(58.dp, maxWidth / series.size.coerceAtLeast(1))
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

                            // Draw Assets (Blue) & Liabilities (Coral) split bars by ledger group
                            val netPoints = ArrayList<Offset>(series.size)
                            series.forEachIndexed { idx, pt ->
                                val centerX = idx * slotPx + slotPx / 2f
                                val barLeft = centerX - barWidth / 2f

                                val assetVal = pt.assets.toFloat()
                                val liabVal = pt.liabilities.toFloat()
                                val assetPx = abs(assetVal) * pxPerUnit
                                val liabPx = abs(liabVal) * pxPerUnit

                                // Determine if both bars share the same vertical side of zero
                                val bothUp = assetVal > 0f && liabVal < 0f
                                val bothDown = assetVal < 0f && liabVal > 0f
                                val splitSideBySide = (bothUp || bothDown) && assetPx > 0.5f && liabPx > 0.5f
                                val subBarW = if (splitSideBySide) (barWidth - 2.dp.toPx()) / 2f else barWidth
                                val assetLeft = if (splitSideBySide) barLeft else barLeft
                                val liabLeft = if (splitSideBySide) barLeft + subBarW + 2.dp.toPx() else barLeft

                                if (assetPx > 0.5f) {
                                    val topY = if (assetVal >= 0f) zeroY - assetPx else zeroY
                                    drawRoundRect(
                                        color = if (idx == selectedIndex) LedgerBlue else LedgerBlue.copy(alpha = 0.78f),
                                        topLeft = Offset(assetLeft, topY),
                                        size = Size(subBarW, assetPx),
                                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                                    )
                                }

                                if (liabPx > 0.5f) {
                                    val topY = if (liabVal >= 0f) zeroY else zeroY - liabPx
                                    drawRoundRect(
                                        color = if (idx == selectedIndex) LedgerCoral else LedgerCoral.copy(alpha = 0.80f),
                                        topLeft = Offset(liabLeft, topY),
                                        size = Size(subBarW, liabPx),
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

            // Display latest period first so users see current data immediately and scroll down for history
            val displayIndices = series.indices.reversed().toList()
            displayIndices.forEachIndexed { rowPos, idx ->
                val pt = series[idx]
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
                        color = if (pt.liabilities.roundToLong() != 0L) LedgerCoral else LedgerTextMuted,
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
