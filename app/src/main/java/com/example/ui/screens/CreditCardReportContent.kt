package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.ReportPeriodMode
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBlue
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
import com.example.viewmodel.CreditCardPeriodPoint
import com.example.viewmodel.CreditCardSummary
import kotlin.math.roundToLong

/**
 * Body of the "Credit Cards" report: totals for the selected period, one card per credit card
 * (spent, paid, outstanding, top categories), and a month/year table to switch periods.
 */
@Composable
fun CreditCardReportContent(
    hasCards: Boolean,
    periods: List<CreditCardPeriodPoint>,
    selectedIndex: Int,
    summaries: List<CreditCardSummary>,
    symbol: String,
    mode: ReportPeriodMode,
    onSelectIndex: (Int) -> Unit,
    onSelectAccount: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!hasCards) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = LedgerTextMuted,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No credit cards yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = LedgerTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add a card under the \"Credit Cards\" group in the Accounts tab to see its spending, payments and outstanding balance here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val selected = periods.getOrNull(selectedIndex)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selected != null) {
            item(key = "cc_totals") {
                CreditCardTotalsCard(point = selected, cardCount = summaries.size, symbol = symbol)
            }
        }

        items(summaries, key = { "cc_card_${it.account.id}" }) { summary ->
            CreditCardSummaryCard(
                summary = summary,
                symbol = symbol,
                onClick = { onSelectAccount(summary.account) }
            )
        }

        item(key = "cc_period_table") {
            CreditCardPeriodTable(
                periods = periods,
                selectedIndex = selectedIndex,
                symbol = symbol,
                mode = mode,
                onSelectIndex = onSelectIndex
            )
        }
    }
}

@Composable
private fun CreditCardTotalsCard(
    point: CreditCardPeriodPoint,
    cardCount: Int,
    symbol: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = Modifier.fillMaxWidth().testTag("cc_totals_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Credit Cards · ${point.fullLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = LedgerTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = LedgerFormatters.formatWholeAmount(point.outstanding, symbol),
                style = TabularAmountStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                color = outstandingColor(point.outstanding)
            )
            Text(
                text = "Total outstanding across $cardCount card${if (cardCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatColumn("Spent", point.spent, symbol, LedgerCoral, Modifier.weight(1f))
                StatColumn("Paid", point.paid, symbol, LedgerGreen, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreditCardSummaryCard(
    summary: CreditCardSummary,
    symbol: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("cc_card_${summary.account.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = LedgerBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.account.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = LedgerTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${summary.transactionCount} transaction${if (summary.transactionCount == 1) "" else "s"} this period",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = LedgerFormatters.formatWholeAmount(summary.outstanding, symbol),
                        style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = outstandingColor(summary.outstanding)
                    )
                    Text(
                        text = if (summary.outstanding.roundToLong() < 0L) "In credit" else "Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatColumn("Spent", summary.spent, symbol, LedgerCoral, Modifier.weight(1f))
                StatColumn("Paid", summary.paid, symbol, LedgerGreen, Modifier.weight(1f))
            }

            if (summary.topCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Top spending",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary
                )
                summary.topCategories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LedgerFormatters.parseColorHex(cat.colorHex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cat.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = LedgerTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = LedgerFormatters.formatWholeAmount(cat.amount, symbol),
                            style = TabularAmountStyle.copy(fontSize = 12.sp),
                            color = LedgerTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditCardPeriodTable(
    periods: List<CreditCardPeriodPoint>,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeader(if (mode == ReportPeriodMode.MONTHLY) "Month" else "Year", LedgerTextSecondary, 0.9f, TextAlign.Start)
                TableHeader("Spent", LedgerCoral, 1f)
                TableHeader("Paid", LedgerGreen, 1f)
                TableHeader("Outstanding", LedgerAmber, 1.15f)
            }
            HorizontalDivider(color = LedgerDivider)

            periods.forEachIndexed { idx, pt ->
                val isSel = idx == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSel) LedgerSurfaceHighlight.copy(alpha = 0.7f) else Color.Transparent)
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("cc_period_row_${pt.key}"),
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
                    TableAmount(pt.spent, symbol, if (pt.spent.roundToLong() > 0L) LedgerCoral else LedgerTextMuted, 1f)
                    TableAmount(pt.paid, symbol, if (pt.paid.roundToLong() > 0L) LedgerGreen else LedgerTextMuted, 1f)
                    TableAmount(pt.outstanding, symbol, outstandingColor(pt.outstanding), 1.15f, bold = isSel)
                }
                if (idx < periods.lastIndex) {
                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, amount: Double, symbol: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = LedgerTextMuted)
        Text(
            text = LedgerFormatters.formatWholeAmount(amount, symbol),
            style = TabularAmountStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = if (amount.roundToLong() > 0L) color else LedgerTextSecondary
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableHeader(
    text: String,
    color: Color,
    weight: Float,
    align: TextAlign = TextAlign.End
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        textAlign = align,
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableAmount(
    amount: Double,
    symbol: String,
    color: Color,
    weight: Float,
    bold: Boolean = false
) {
    Text(
        text = LedgerFormatters.formatWholeAmount(amount, symbol, includeSymbol = false),
        style = TabularAmountStyle.copy(
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        ),
        color = color,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun outstandingColor(amount: Double): Color = when {
    amount.roundToLong() > 0L -> LedgerCoral
    amount.roundToLong() < 0L -> LedgerGreen
    else -> LedgerTextPrimary
}
