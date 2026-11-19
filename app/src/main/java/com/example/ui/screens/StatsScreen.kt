package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerTransaction
import com.example.data.StatsPeriodType
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.ui.theme.TabularAmountStyle
import com.example.util.LedgerFormatters
import com.example.viewmodel.CategoryStatSlice
import com.example.viewmodel.FilterState
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    bundle: LedgerDataBundle,
    filter: FilterState,
    viewModel: LedgerViewModel,
    onEditTransaction: (LedgerTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol
    val (totalIncome, totalExpense, slices) = remember(bundle.transactions, bundle.categories, filter) {
        viewModel.computeStats(bundle, filter)
    }

    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showCustomPeriodDialog by remember { mutableStateOf(false) }
    var inspectedSlice by remember { mutableStateOf<CategoryStatSlice?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
    ) {
        // Top Period Bar: < Nov 2026 >  ...  [Monthly v]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.previousStatsPeriod() },
                modifier = Modifier.testTag("stats_prev_period_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous period",
                    tint = LedgerTextPrimary
                )
            }
            Text(
                text = viewModel.statsPeriodHeaderLabel(filter, bundle.settings),
                style = MaterialTheme.typography.titleMedium,
                color = LedgerTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { viewModel.nextStatsPeriod() },
                modifier = Modifier.testTag("stats_next_period_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next period",
                    tint = LedgerTextPrimary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LedgerSurfaceElevated,
                    border = BorderStroke(1.dp, LedgerBorder),
                    modifier = Modifier
                        .clickable { showPeriodDropdown = true }
                        .testTag("stats_period_dropdown")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filter.statsPeriodType.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = LedgerTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Select period",
                            tint = LedgerTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showPeriodDropdown,
                    onDismissRequest = { showPeriodDropdown = false },
                    containerColor = LedgerSurfaceElevated
                ) {
                    StatsPeriodType.entries.forEach { period ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = period.label,
                                    color = if (filter.statsPeriodType == period) LedgerCoral else LedgerTextPrimary
                                )
                            },
                            onClick = {
                                showPeriodDropdown = false
                                if (period == StatsPeriodType.PERIOD) {
                                    showCustomPeriodDialog = true
                                } else {
                                    viewModel.setStatsPeriodType(period)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Income vs Expenses Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
        ) {
            StatsTypeTab(
                label = "Income ${LedgerFormatters.formatAmount(totalIncome, symbol)}",
                selected = !filter.statsIsExpenseTab,
                onClick = { viewModel.setStatsIsExpenseTab(false) },
                modifier = Modifier.weight(1f).testTag("stats_income_tab")
            )
            StatsTypeTab(
                label = "Expenses ${LedgerFormatters.formatAmount(totalExpense, symbol)}",
                selected = filter.statsIsExpenseTab,
                onClick = { viewModel.setStatsIsExpenseTab(true) },
                modifier = Modifier.weight(1f).testTag("stats_expenses_tab")
            )
        }
        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

        if (slices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PieChart,
                        contentDescription = null,
                        tint = LedgerTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (filter.statsIsExpenseTab) "No expenses recorded in this period" else "No income recorded in this period",
                        style = MaterialTheme.typography.titleMedium,
                        color = LedgerTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add transactions or switch period to visualize your category breakdown.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "pie_chart") {
                    AnimatedLedgerPieChart(
                        slices = slices,
                        animationKey = "${filter.selectedYear}_${filter.selectedMonth}_${filter.statsPeriodType}_${filter.statsIsExpenseTab}_${slices.size}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
                            .background(LedgerBackground)
                    )
                    HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
                }

                items(slices, key = { it.categoryId }) { slice ->
                    val sliceColor = LedgerFormatters.parseColorHex(slice.colorHex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LedgerSurface)
                            .clickable { inspectedSlice = slice }
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                            .testTag("stats_category_row_${slice.categoryId}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(sliceColor)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${slice.percentage.roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = slice.categoryName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = LedgerTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = LedgerFormatters.formatAmount(slice.amount, symbol),
                            style = TabularAmountStyle.copy(fontSize = 14.sp),
                            color = LedgerTextPrimary
                        )
                    }
                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                }
            }
        }
    }

    if (inspectedSlice != null) {
        ModalBottomSheet(
            onDismissRequest = { inspectedSlice = null },
            containerColor = LedgerSurface
        ) {
            val slice = inspectedSlice!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = slice.categoryName,
                        style = MaterialTheme.typography.titleLarge,
                        color = LedgerTextPrimary
                    )
                    Text(
                        text = LedgerFormatters.formatAmount(slice.amount, symbol),
                        style = TabularAmountStyle.copy(fontSize = 16.sp),
                        color = LedgerCoral
                    )
                }
                HorizontalDivider(color = LedgerDivider)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(slice.transactions, key = { it.id }) { tx ->
                        TransactionRowItem(
                            tx = tx,
                            bundle = bundle,
                            symbol = symbol,
                            onClick = {
                                inspectedSlice = null
                                onEditTransaction(tx)
                            }
                        )
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showCustomPeriodDialog) {
        CustomPeriodDialog(
            onDismiss = { showCustomPeriodDialog = false },
            onSelectDays = { days ->
                val today = LedgerFormatters.todayEpochDay()
                viewModel.setCustomPeriodRange(today - (days - 1), today)
                showCustomPeriodDialog = false
            }
        )
    }
}

@Composable
private fun StatsTypeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) LedgerTextPrimary else LedgerTextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) LedgerCoral else Color.Transparent)
        )
    }
}

@Composable
private fun AnimatedLedgerPieChart(
    slices: List<CategoryStatSlice>,
    animationKey: String,
    modifier: Modifier = Modifier
) {
    val sweepProgress = remember { Animatable(0f) }
    LaunchedEffect(animationKey) {
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
        )
    }

    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 10.5.sp.toPx() }
    val subTextSizePx = with(density) { 9.5.sp.toPx() }

    Canvas(modifier = modifier) {
        val total = slices.sumOf { it.amount }.takeIf { it > 0.0 } ?: return@Canvas
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(size.width * 0.25f, size.height * 0.36f)
        val topLeft = Offset(centerX - radius, centerY - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        var startAngle = -90f
        val maxSweep = 360f * sweepProgress.value

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#F3F4F6")
            textSize = labelTextSizePx
            isFakeBoldText = true
        }
        val pctPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#B8BDC8")
            textSize = subTextSizePx
        }

        val visibleLabels = mutableListOf<Triple<Float, CategoryStatSlice, Color>>()

        slices.forEach { slice ->
            val rawSweep = ((slice.amount / total) * 360.0).toFloat()
            val drawnSweep = (startAngle + rawSweep - (-90f)).coerceAtMost(maxSweep) - (startAngle - (-90f))
            if (drawnSweep > 0.2f) {
                val sliceColor = LedgerFormatters.parseColorHex(slice.colorHex)
                drawArc(
                    color = sliceColor,
                    startAngle = startAngle,
                    sweepAngle = drawnSweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
                drawArc(
                    color = LedgerBackground,
                    startAngle = startAngle,
                    sweepAngle = drawnSweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 2.2f)
                )
                if (sweepProgress.value > 0.85f && slice.percentage >= 3.0) {
                    visibleLabels.add(Triple(startAngle + rawSweep / 2f, slice, sliceColor))
                }
            }
            startAngle += rawSweep
        }

        // Draw crisp radial callout leader lines + labels (up to 6 top slices to avoid overlap)
        val topLabeled = visibleLabels.take(6)
        topLabeled.forEachIndexed { index, (midDeg, slice, _) ->
            val rad = Math.toRadians(midDeg.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()

            val p1 = Offset(centerX + cosA * (radius * 0.98f), centerY + sinA * (radius * 0.98f))
            val elbowRadius = radius + 18.dp.toPx() + (index % 2) * 8.dp.toPx()
            val p2 = Offset(centerX + cosA * elbowRadius, centerY + sinA * elbowRadius)
            val isRight = cosA >= 0f
            val horizontalEnd = Offset(p2.x + if (isRight) 18.dp.toPx() else -18.dp.toPx(), p2.y)

            drawLine(
                color = LedgerTextSecondary.copy(alpha = 0.7f),
                start = p1,
                end = p2,
                strokeWidth = 1.6f
            )
            drawLine(
                color = LedgerTextSecondary.copy(alpha = 0.7f),
                start = p2,
                end = horizontalEnd,
                strokeWidth = 1.6f
            )

            titlePaint.textAlign = if (isRight) Paint.Align.LEFT else Paint.Align.RIGHT
            pctPaint.textAlign = if (isRight) Paint.Align.LEFT else Paint.Align.RIGHT

            val textX = horizontalEnd.x + if (isRight) 5.dp.toPx() else -5.dp.toPx()
            val shortName = if (slice.categoryName.length > 14) {
                slice.categoryName.take(13) + "…"
            } else {
                slice.categoryName
            }
            val pctText = String.format(Locale.US, "%.1f %%", slice.percentage)

            drawContext.canvas.nativeCanvas.drawText(shortName, textX, horizontalEnd.y - 2f, titlePaint)
            drawContext.canvas.nativeCanvas.drawText(pctText, textX, horizontalEnd.y + subTextSizePx + 2f, pctPaint)
        }
    }
}

@Composable
private fun CustomPeriodDialog(
    onDismiss: () -> Unit,
    onSelectDays: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LedgerSurface,
        title = { Text("Select Custom Period", color = LedgerTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    7L to "Last 7 Days",
                    30L to "Last 30 Days",
                    90L to "Last 90 Days",
                    180L to "Last 6 Months",
                    365L to "Last 365 Days"
                ).forEach { (days, label) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LedgerSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDays(days) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = LedgerTextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LedgerTextSecondary)
            }
        }
    )
}
