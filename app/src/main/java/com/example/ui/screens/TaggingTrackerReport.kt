package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerTransaction
import com.example.data.TransactionType
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerMotion
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
import java.util.Calendar
import kotlin.math.roundToLong

private enum class TaggingPeriodScope(val label: String) {
    ALL_TIME("All Time"),
    THIS_YEAR("This Year"),
    THIS_MONTH("This Month")
}

private data class TaggingSummaryItem(
    val tagName: String,
    val income: Double,
    val expense: Double,
    val transferTotal: Double,
    val transactions: List<LedgerTransaction>
) {
    val net: Double get() = income - expense
    val volume: Double get() = income + expense + transferTotal
}

@Composable
fun TaggingTrackerContent(
    bundle: LedgerDataBundle,
    onEditTransaction: (LedgerTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol
    val hasPrimary = bundle.allTaggings.isNotEmpty()
    val hasSecondary = bundle.allSecondaryTaggings.isNotEmpty()

    var selectedDimensionSecondary by remember(hasPrimary, hasSecondary) {
        mutableStateOf(!hasPrimary && hasSecondary)
    }
    var periodScope by remember { mutableStateOf(TaggingPeriodScope.ALL_TIME) }
    var expandedTagName by remember(selectedDimensionSecondary, periodScope) { mutableStateOf<String?>(null) }

    val activeAccent = if (selectedDimensionSecondary) LedgerAmber else LedgerCoral

    val filteredByPeriod = remember(bundle.transactions, periodScope) {
        if (periodScope == TaggingPeriodScope.ALL_TIME) {
            bundle.transactions
        } else {
            val now = Calendar.getInstance()
            val curYear = now.get(Calendar.YEAR)
            val curMonth = now.get(Calendar.MONTH)
            bundle.transactions.filter { tx ->
                val cal = LedgerFormatters.epochDayToCalendar(tx.epochDay)
                val sameYear = cal.get(Calendar.YEAR) == curYear
                if (periodScope == TaggingPeriodScope.THIS_YEAR) {
                    sameYear
                } else {
                    sameYear && cal.get(Calendar.MONTH) == curMonth
                }
            }
        }
    }

    val tagSummaries = remember(filteredByPeriod, selectedDimensionSecondary) {
        val grouped = mutableMapOf<String, MutableList<LedgerTransaction>>()
        filteredByPeriod.forEach { tx ->
            val raw = if (selectedDimensionSecondary) tx.effectiveSecondaryTagging() else tx.effectiveTagging()
            if (!raw.isNullOrBlank()) {
                grouped.getOrPut(raw) { mutableListOf() }.add(tx)
            }
        }
        grouped.entries
            .map { (tagName, list) ->
                var inc = 0.0
                var exp = 0.0
                var trf = 0.0
                list.forEach { tx ->
                    when (tx.txType()) {
                        TransactionType.INCOME -> inc += tx.amount
                        TransactionType.EXPENSE -> exp += tx.amount
                        TransactionType.TRANSFER -> trf += tx.amount
                    }
                }
                TaggingSummaryItem(
                    tagName = tagName,
                    income = inc,
                    expense = exp,
                    transferTotal = trf,
                    transactions = list.sortedWith(
                        compareByDescending<LedgerTransaction> { it.epochDay }
                            .thenByDescending { it.timestampMillis }
                    )
                )
            }
            .sortedByDescending { it.volume }
    }

    val totalTaggedIncome = remember(tagSummaries) { tagSummaries.sumOf { it.income } }
    val totalTaggedExpense = remember(tagSummaries) { tagSummaries.sumOf { it.expense } }
    val totalTaggedNet = totalTaggedIncome - totalTaggedExpense

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
        ) {
            // Dimension Switcher: Tagging 1 vs Tagging 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaggingDimensionPill(
                    title = "Tagging 1",
                    count = bundle.allTaggings.size,
                    selected = !selectedDimensionSecondary,
                    accentColor = LedgerCoral,
                    onClick = { selectedDimensionSecondary = false },
                    modifier = Modifier.weight(1f).testTag("tagging_report_tab_1")
                )
                TaggingDimensionPill(
                    title = "Tagging 2",
                    count = bundle.allSecondaryTaggings.size,
                    selected = selectedDimensionSecondary,
                    accentColor = LedgerAmber,
                    onClick = { selectedDimensionSecondary = true },
                    modifier = Modifier.weight(1f).testTag("tagging_report_tab_2")
                )
            }

            // Period Scope Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurface)
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaggingPeriodScope.entries.forEach { scope ->
                    val active = periodScope == scope
                    Surface(
                        onClick = { periodScope = scope },
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) activeAccent.copy(alpha = 0.16f) else LedgerSurfaceElevated,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (active) activeAccent.copy(alpha = 0.65f) else LedgerDivider
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tagging_report_period_${scope.name.lowercase()}")
                    ) {
                        Text(
                            text = scope.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) activeAccent else LedgerTextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = LedgerDivider)

            // 3-Column Income / Expenses / Net Summary Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurface)
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetricColumn(
                    label = "Income",
                    value = LedgerFormatters.formatWholeAmount(totalTaggedIncome, symbol),
                    valueColor = LedgerBlue,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricColumn(
                    label = "Expenses",
                    value = LedgerFormatters.formatWholeAmount(totalTaggedExpense, symbol),
                    valueColor = LedgerCoral,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricColumn(
                    label = "Net",
                    value = LedgerFormatters.formatSignedWhole(totalTaggedNet, symbol),
                    valueColor = when {
                        totalTaggedNet.roundToLong() < 0L -> LedgerCoral
                        totalTaggedNet.roundToLong() > 0L -> LedgerGreen
                        else -> LedgerTextPrimary
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(color = LedgerDivider)
        }

        if (tagSummaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Label,
                        contentDescription = null,
                        tint = activeAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedDimensionSecondary) {
                            "No Tagging 2 entries yet"
                        } else {
                            "No Tagging 1 entries yet"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = LedgerTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedDimensionSecondary) {
                            "Add a Tagging 2 value while recording entries (e.g. Project, Trip, Tax) to track them separately here."
                        } else {
                            "Add a Tagging value while recording entries (e.g. Home, Personal, Business) to track them here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(tagSummaries, key = { it.tagName }) { item ->
                    val isExpanded = expandedTagName == item.tagName
                    val chevronRot by animateFloatAsState(
                        targetValue = if (isExpanded) 90f else 0f,
                        animationSpec = LedgerMotion.quickSpring(),
                        label = "tag_summary_chev_${item.tagName}"
                    )
                    val netColor = when {
                        item.net.roundToLong() < 0L -> LedgerCoral
                        item.net.roundToLong() > 0L -> LedgerGreen
                        else -> LedgerTextPrimary
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 58.dp)
                                .background(LedgerSurface)
                                .clickable {
                                    expandedTagName = if (isExpanded) null else item.tagName
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                                .testTag("tagging_report_row_${item.tagName}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(activeAccent)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isExpanded) "Collapse ${item.tagName}" else "Expand ${item.tagName}",
                                tint = LedgerTextSecondary,
                                modifier = Modifier.size(18.dp).rotate(chevronRot)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = item.tagName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LedgerTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val detailMeta = buildString {
                                    append("${item.transactions.size} ${if (item.transactions.size == 1) "entry" else "entries"}")
                                    if (item.income > 0.0) {
                                        append(" • In ${LedgerFormatters.formatWholeAmount(item.income, symbol)}")
                                    }
                                    if (item.expense > 0.0) {
                                        append(" • Out ${LedgerFormatters.formatWholeAmount(item.expense, symbol)}")
                                    }
                                    if (item.transferTotal > 0.0) {
                                        append(" • Trf ${LedgerFormatters.formatWholeAmount(item.transferTotal, symbol)}")
                                    }
                                }
                                Text(
                                    text = detailMeta,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerTextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = LedgerFormatters.formatSignedWhole(item.net, symbol),
                                    style = TabularAmountStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = netColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Net",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = LedgerTextMuted
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = LedgerMotion.expandEnter(),
                            exit = LedgerMotion.shrinkExit()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LedgerSurfaceElevated.copy(alpha = 0.45f))
                            ) {
                                item.transactions.forEach { tx ->
                                    HorizontalDivider(color = LedgerDivider.copy(alpha = 0.6f), thickness = 0.5.dp)
                                    Text(
                                        text = LedgerFormatters.formatShortDate(tx.epochDay),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerTextMuted,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(LedgerBackground)
                                            .padding(horizontal = 14.dp, vertical = 4.dp)
                                    )
                                    TransactionRowItem(
                                        tx = tx,
                                        bundle = bundle,
                                        symbol = symbol,
                                        onClick = { onEditTransaction(tx) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.75.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaggingDimensionPill(
    title: String,
    count: Int,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accentColor.copy(alpha = 0.18f) else LedgerSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accentColor else LedgerBorder
        ),
        modifier = modifier.heightIn(min = 44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else LedgerTextPrimary,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) accentColor.copy(alpha = 0.25f) else LedgerSurfaceHighlight)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) accentColor else LedgerTextSecondary
                )
            }
        }
    }
}
