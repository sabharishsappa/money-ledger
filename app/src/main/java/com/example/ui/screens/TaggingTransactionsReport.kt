package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerTransaction
import com.example.data.TransactionType
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
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

@Immutable
private data class RawTagHit(
    val tagKey: String,
    val tagDisplayName: String,
    val tx: LedgerTransaction,
    val delta: Double,
    val roleHint: String?
)

@Immutable
private data class TagLedgerEntryRow(
    val rowKey: String,
    val tx: LedgerTransaction,
    val tagKey: String,
    val tagDisplayName: String,
    val signedDelta: Double,
    val accumulatedBalance: Double,
    val roleHint: String?
)

@Immutable
private data class TagQuickSummary(
    val tagKey: String,
    val displayName: String,
    val entryCount: Int,
    val finalAccumulated: Double
)

@Immutable
private data class TagDateGroup(
    val epochDay: Long,
    val entries: List<TagLedgerEntryRow>,
    val dayNet: Double
)

@Composable
fun TaggingTransactionsReportContent(
    bundle: LedgerDataBundle,
    onEditTransaction: (LedgerTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol
    val today = remember { LedgerFormatters.todayEpochDay() }
    val defaultMonthStart = remember(today) {
        val cal = LedgerFormatters.epochDayToCalendar(today)
        LedgerFormatters.calendarToEpochDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1)
    }

    var datePreset by remember { mutableStateOf(TagReportDatePreset.ALL) }
    var customStartEpochDay by remember { mutableStateOf(defaultMonthStart) }
    var customEndEpochDay by remember { mutableStateOf(today) }

    // Empty set means "All Tags" (shows all tagged entries). May include UNTAGGED_KEY when ticked.
    var selectedTagKeys by remember { mutableStateOf(setOf<String>()) }
    // Default to Latest -> Oldest as requested, while running accumulation stays strictly chronological.
    var newestFirst by remember { mutableStateOf(true) }

    var showDateFilterDialog by remember { mutableStateOf(false) }
    var showTagFilterDialog by remember { mutableStateOf(false) }

    val availableTags = bundle.allTaggings
    val (startBound, endBound) = remember(datePreset, customStartEpochDay, customEndEpochDay) {
        resolveDatePresetBounds(datePreset, customStartEpochDay, customEndEpochDay)
    }

    val dateFilteredTransactions = remember(bundle.transactions, startBound, endBound) {
        bundle.transactions.filter { tx ->
            val afterStart = startBound == null || tx.epochDay >= startBound
            val beforeEnd = endBound == null || tx.epochDay <= endBound
            afterStart && beforeEnd
        }
    }

    // Extract chronological tag hits (oldest -> newest) across the active date range
    val allChronologicalHits = remember(dateFilteredTransactions, availableTags) {
        val chronological = dateFilteredTransactions.sortedWith(
            compareBy<LedgerTransaction> { it.epochDay }
                .thenBy { it.timestampMillis }
                .thenBy { it.id }
        )

        val canonicalNameByKey = mutableMapOf<String, String>()
        availableTags.forEach { tag ->
            canonicalNameByKey[tag.lowercase()] = tag
        }

        val hits = ArrayList<RawTagHit>(chronological.size)
        fun addHit(tagDisplay: String, tx: LedgerTransaction, delta: Double, roleHint: String?) {
            val key = if (tagDisplay == UNTAGGED_KEY) UNTAGGED_KEY else tagDisplay.lowercase()
            val display = if (key == UNTAGGED_KEY) {
                "Untagged"
            } else {
                canonicalNameByKey.getOrPut(key) { tagDisplay }
            }
            hits.add(RawTagHit(key, display, tx, delta, roleHint))
        }

        chronological.forEach { tx ->
            val accTag = tx.effectiveTagging()
            val catOrToTag = tx.effectiveToTagging()

            when (tx.txType()) {
                TransactionType.INCOME -> {
                    val sameTag = accTag != null && catOrToTag != null && accTag.equals(catOrToTag, ignoreCase = true)
                    when {
                        accTag == null && catOrToTag == null -> addHit(UNTAGGED_KEY, tx, tx.amount, null)
                        sameTag -> addHit(accTag!!, tx, tx.amount, null)
                        else -> {
                            accTag?.let { addHit(it, tx, tx.amount, "Account") }
                            catOrToTag?.let { addHit(it, tx, tx.amount, "Income") }
                        }
                    }
                }

                TransactionType.EXPENSE -> {
                    val sameTag = accTag != null && catOrToTag != null && accTag.equals(catOrToTag, ignoreCase = true)
                    when {
                        accTag == null && catOrToTag == null -> addHit(UNTAGGED_KEY, tx, -tx.amount, null)
                        sameTag -> addHit(accTag!!, tx, -tx.amount, null)
                        else -> {
                            accTag?.let { addHit(it, tx, -tx.amount, "Account") }
                            catOrToTag?.let { addHit(it, tx, -tx.amount, "Expense") }
                        }
                    }
                }

                TransactionType.TRANSFER -> {
                    val sameTag = accTag != null && catOrToTag != null && accTag.equals(catOrToTag, ignoreCase = true)
                    when {
                        accTag == null && catOrToTag == null -> addHit(UNTAGGED_KEY, tx, -tx.feeAmount, "Internal")
                        sameTag -> addHit(accTag!!, tx, -tx.feeAmount, "Internal")
                        else -> {
                            if (accTag != null) {
                                addHit(accTag, tx, -(tx.amount + tx.feeAmount), "Transfer Out")
                            } else {
                                addHit(UNTAGGED_KEY, tx, -(tx.amount + tx.feeAmount), "Transfer Out")
                            }
                            if (catOrToTag != null) {
                                addHit(catOrToTag, tx, tx.amount, "Transfer In")
                            } else {
                                addHit(UNTAGGED_KEY, tx, tx.amount, "Transfer In")
                            }
                        }
                    }
                }
            }
        }
        hits
    }

    // Per-tag quick summaries for the filter dialog counts and top quick-switch strip
    val tagQuickSummaries = remember(allChronologicalHits) {
        allChronologicalHits
            .groupBy { it.tagKey }
            .map { (key, list) ->
                TagQuickSummary(
                    tagKey = key,
                    displayName = list.first().tagDisplayName,
                    entryCount = list.size,
                    finalAccumulated = list.sumOf { it.delta }
                )
            }
            .sortedWith(
                compareBy<TagQuickSummary> { it.tagKey == UNTAGGED_KEY }
                    .thenBy { it.displayName.lowercase() }
            )
    }

    val tagEntryCounts = remember(tagQuickSummaries) {
        tagQuickSummaries.associate { it.tagKey to it.entryCount }
    }
    val hasUntaggedInRange = (tagEntryCounts[UNTAGGED_KEY] ?: 0) > 0
    val taggedQuickSummaries = remember(tagQuickSummaries) {
        tagQuickSummaries.filter { it.tagKey != UNTAGGED_KEY }
    }

    // Compute chronological running accumulated balance for the active tag filter
    val chronologicalRows = remember(allChronologicalHits, selectedTagKeys) {
        val filtered = if (selectedTagKeys.isEmpty()) {
            allChronologicalHits.filter { it.tagKey != UNTAGGED_KEY }
        } else {
            allChronologicalHits.filter { it.tagKey in selectedTagKeys }
        }

        var running = 0.0
        filtered.mapIndexed { idx, hit ->
            running += hit.delta
            TagLedgerEntryRow(
                rowKey = "${hit.tagKey}_${hit.tx.id}_$idx",
                tx = hit.tx,
                tagKey = hit.tagKey,
                tagDisplayName = hit.tagDisplayName,
                signedDelta = hit.delta,
                accumulatedBalance = running,
                roleHint = hit.roleHint
            )
        }
    }

    // Group rows by date in the chosen display order (Latest -> Oldest by default)
    val dateGroups = remember(chronologicalRows, newestFirst) {
        val ordered = if (newestFirst) chronologicalRows.asReversed() else chronologicalRows
        val grouped = LinkedHashMap<Long, MutableList<TagLedgerEntryRow>>()
        ordered.forEach { row ->
            grouped.getOrPut(row.tx.epochDay) { mutableListOf() }.add(row)
        }
        grouped.entries.map { (epochDay, rows) ->
            TagDateGroup(
                epochDay = epochDay,
                entries = rows,
                dayNet = rows.sumOf { it.signedDelta }
            )
        }
    }

    val totalInflow = remember(chronologicalRows) {
        chronologicalRows.filter { it.signedDelta > 0.0 }.sumOf { it.signedDelta }
    }
    val totalOutflow = remember(chronologicalRows) {
        chronologicalRows.filter { it.signedDelta < 0.0 }.sumOf { abs(it.signedDelta) }
    }
    val totalAccumulated = remember(chronologicalRows) {
        chronologicalRows.lastOrNull()?.accumulatedBalance ?: 0.0
    }

    val hasActiveFilter = datePreset != TagReportDatePreset.ALL || selectedTagKeys.isNotEmpty()
    val tagChipLabel = remember(selectedTagKeys, availableTags) {
        when {
            selectedTagKeys.isEmpty() -> "Tag: All"
            selectedTagKeys.size == 1 -> {
                val single = selectedTagKeys.first()
                val label = if (single == UNTAGGED_KEY) {
                    "Untagged"
                } else {
                    availableTags.firstOrNull { it.equals(single, ignoreCase = true) } ?: single
                }
                "Tag: $label"
            }
            else -> "Tags (${selectedTagKeys.size})"
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
    ) {
        val contentWidth = maxWidth.coerceAtMost(760.dp)
        val valueColWidth = (contentWidth * 0.23f).coerceIn(78.dp, 108.dp)
        val accColWidth = (contentWidth * 0.26f).coerceIn(88.dp, 118.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
            ) {
                // Interactive Filter Bar: Date Filter + Tag Filter + Sort Order + Reset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReportFilterPill(
                        icon = Icons.Filled.DateRange,
                        label = formatDateChipLabel(datePreset, customStartEpochDay, customEndEpochDay),
                        active = datePreset != TagReportDatePreset.ALL,
                        accentColor = LedgerCoral,
                        showDropdownChevron = true,
                        onClick = { showDateFilterDialog = true },
                        modifier = Modifier.testTag("tag_report_date_filter_chip")
                    )

                    ReportFilterPill(
                        icon = Icons.Filled.Label,
                        label = tagChipLabel,
                        active = selectedTagKeys.isNotEmpty(),
                        accentColor = LedgerBlue,
                        showDropdownChevron = true,
                        onClick = { showTagFilterDialog = true },
                        modifier = Modifier.testTag("tag_report_tag_filter_chip")
                    )

                    ReportFilterPill(
                        icon = Icons.Filled.SwapVert,
                        label = if (newestFirst) "Latest" else "Oldest",
                        active = !newestFirst,
                        accentColor = LedgerAmber,
                        showDropdownChevron = false,
                        onClick = { newestFirst = !newestFirst },
                        modifier = Modifier.testTag("tag_report_sort_toggle")
                    )

                    if (hasActiveFilter) {
                        Surface(
                            onClick = {
                                datePreset = TagReportDatePreset.ALL
                                selectedTagKeys = emptySet()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = LedgerSurfaceElevated,
                            border = BorderStroke(1.dp, LedgerBorder),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("tag_report_clear_filters")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Reset filters",
                                    tint = LedgerTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LedgerTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = LedgerDivider)

                // 3-Column Summary Strip: Inflow | Outflow | Accumulated
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryMetricColumn(
                        label = "Inflow",
                        value = LedgerFormatters.formatWholeAmount(totalInflow, symbol),
                        valueColor = LedgerBlue,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricColumn(
                        label = "Outflow",
                        value = LedgerFormatters.formatWholeAmount(totalOutflow, symbol),
                        valueColor = LedgerCoral,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricColumn(
                        label = "Accumulated",
                        value = formatSignedLedgerAmount(totalAccumulated, symbol, showPlus = false),
                        valueColor = when {
                            totalAccumulated.roundToLong() < 0L -> LedgerCoral
                            totalAccumulated.roundToLong() > 0L -> LedgerGreen
                            else -> LedgerTextPrimary
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Tag Summary Pills for 1-tap tag totals & quick filtering
                if (taggedQuickSummaries.isNotEmpty()) {
                    HorizontalDivider(color = LedgerDivider.copy(alpha = 0.7f), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LedgerSurface)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAllSelected = selectedTagKeys.isEmpty()
                        TagSummaryQuickPill(
                            tagName = "All",
                            amountText = "${taggedQuickSummaries.size} ${if (taggedQuickSummaries.size == 1) "tag" else "tags"}",
                            amountColor = if (isAllSelected) LedgerAmber else LedgerTextMuted,
                            selected = isAllSelected,
                            onClick = { selectedTagKeys = emptySet() },
                            modifier = Modifier.testTag("tag_quick_pill_all")
                        )
                        taggedQuickSummaries.forEach { summary ->
                            val selected = selectedTagKeys.size == 1 && summary.tagKey in selectedTagKeys
                            val amtColor = when {
                                summary.finalAccumulated.roundToLong() < 0L -> LedgerCoral
                                summary.finalAccumulated.roundToLong() > 0L -> LedgerGreen
                                else -> LedgerTextSecondary
                            }
                            TagSummaryQuickPill(
                                tagName = summary.displayName,
                                amountText = formatSignedLedgerAmount(summary.finalAccumulated, symbol, showPlus = false),
                                amountColor = amtColor,
                                selected = selected,
                                onClick = {
                                    selectedTagKeys = if (selected) emptySet() else setOf(summary.tagKey)
                                },
                                modifier = Modifier.testTag("tag_quick_pill_${summary.tagKey}")
                            )
                        }
                    }
                }

                HorizontalDivider(color = LedgerDivider)

                // Column Header Strip aligned with the date-grouped transaction rows
                if (dateGroups.isNotEmpty()) {
                    TagDateColumnHeader(
                        entryCount = chronologicalRows.size,
                        valueColWidth = valueColWidth,
                        accColWidth = accColWidth
                    )
                }
            }

            if (dateGroups.isEmpty()) {
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
                            tint = LedgerAmber.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (hasActiveFilter) "No transactions match these filters" else "No tagged transactions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = LedgerTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hasActiveFilter) {
                                "Try expanding the date range or selecting different tags."
                            } else {
                                "Add a 2-letter tag beside Account or Category when recording entries to track tagging-wise statements and accumulated values."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        if (hasActiveFilter) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    datePreset = TagReportDatePreset.ALL
                                    selectedTagKeys = emptySet()
                                },
                                border = BorderStroke(1.dp, LedgerCoral),
                                modifier = Modifier.testTag("tag_report_empty_reset_button")
                            ) {
                                Text("Reset Filters", color = LedgerCoral)
                            }
                        } else if (hasUntaggedInRange) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { selectedTagKeys = setOf(UNTAGGED_KEY) },
                                border = BorderStroke(1.dp, LedgerBlue),
                                modifier = Modifier.testTag("tag_report_show_untagged_button")
                            ) {
                                Text("Show Untagged Entries", color = LedgerBlue)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    dateGroups.forEach { dayGroup ->
                        item(key = "date_hdr_${dayGroup.epochDay}", contentType = "tag_date_hdr") {
                            TagDateGroupHeader(
                                group = dayGroup,
                                symbol = symbol,
                                modifier = Modifier
                                    .widthIn(max = 760.dp)
                                    .fillMaxWidth()
                                    .animateItem()
                            )
                        }

                        items(
                            items = dayGroup.entries,
                            key = { it.rowKey },
                            contentType = { "tag_date_tx_row" }
                        ) { row ->
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 760.dp)
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                TagDateTransactionRow(
                                    row = row,
                                    bundle = bundle,
                                    symbol = symbol,
                                    valueColWidth = valueColWidth,
                                    accColWidth = accColWidth,
                                    onClick = { onEditTransaction(row.tx) }
                                )
                                HorizontalDivider(
                                    color = LedgerDivider.copy(alpha = 0.55f),
                                    thickness = 0.5.dp
                                )
                            }
                        }

                        item(key = "date_gap_${dayGroup.epochDay}", contentType = "date_gap") {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 760.dp)
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(LedgerBackground)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDateFilterDialog) {
        DateRangeFilterDialog(
            currentPreset = datePreset,
            currentCustomStart = customStartEpochDay,
            currentCustomEnd = customEndEpochDay,
            onDismiss = { showDateFilterDialog = false },
            onApply = { preset, startDay, endDay ->
                datePreset = preset
                customStartEpochDay = startDay
                customEndEpochDay = endDay
                showDateFilterDialog = false
            }
        )
    }

    if (showTagFilterDialog) {
        TagMultiSelectFilterDialog(
            availableTags = availableTags,
            hasUntaggedEntries = hasUntaggedInRange,
            entryCountsByKey = tagEntryCounts,
            initialSelectedKeys = selectedTagKeys,
            onDismiss = { showTagFilterDialog = false },
            onApply = { chosen ->
                selectedTagKeys = chosen
                showTagFilterDialog = false
            }
        )
    }
}

@Composable
private fun TagSummaryQuickPill(
    tagName: String,
    amountText: String,
    amountColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) LedgerAmber.copy(alpha = 0.16f) else LedgerSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) LedgerAmber.copy(alpha = 0.75f) else LedgerBorder
        ),
        modifier = modifier.heightIn(min = 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tagName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) LedgerAmber else LedgerTextPrimary,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = amountText,
                style = TabularAmountStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                color = amountColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun TagDateColumnHeader(
    entryCount: Int,
    valueColWidth: Dp,
    accColWidth: Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(LedgerSurfaceElevated)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Note 1 ($entryCount)",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextSecondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            )
            Text(
                text = "Value",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextSecondary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(valueColWidth)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Accumulated",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextSecondary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(accColWidth)
            )
        }
        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
    }
}

@Composable
private fun TagDateGroupHeader(
    group: TagDateGroup,
    symbol: String,
    modifier: Modifier = Modifier
) {
    val dayNum = remember(group.epochDay) { LedgerFormatters.dayOfMonthPadded(group.epochDay) }
    val weekday = remember(group.epochDay) { LedgerFormatters.weekdayShort(group.epochDay) }
    val monthYear = remember(group.epochDay) { LedgerFormatters.monthYearDot(group.epochDay) }
    val dow = remember(group.epochDay) { LedgerFormatters.dayOfWeekIndex(group.epochDay) }
    val badgeBg = when (dow) {
        Calendar.SATURDAY -> LedgerBlue
        Calendar.SUNDAY -> LedgerCoral
        else -> LedgerSurfaceHighlight
    }

    Column(modifier = modifier.background(LedgerSurface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayNum,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = LedgerTextPrimary,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = monthYear,
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted,
                maxLines = 1,
                softWrap = false
            )
        }
        HorizontalDivider(color = LedgerDivider, thickness = 0.75.dp)
    }
}

@Composable
private fun TagDateTransactionRow(
    row: TagLedgerEntryRow,
    bundle: LedgerDataBundle,
    symbol: String,
    valueColWidth: Dp,
    accColWidth: Dp,
    onClick: () -> Unit
) {
    val tx = row.tx
    val txType = tx.txType()
    val category = bundle.categoryById(tx.categoryId)
    val hasNote1 = tx.note.isNotBlank()

    // Focus Note 1 prominently as the primary line; gracefully fall back only if Note 1 is blank
    val primaryFocusText = remember(tx.id, tx.note, tx.note2, tx.subcategory, category?.name, txType) {
        when {
            tx.note.isNotBlank() -> tx.note.trim()
            tx.note2.isNotBlank() -> tx.note2.trim()
            txType == TransactionType.TRANSFER -> "Transfer"
            !tx.subcategory.isNullOrBlank() -> tx.subcategory
            else -> category?.name ?: txType.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    val contextMeta = remember(tx.id, txType, tx.accountId, tx.toAccountId, tx.note, tx.note2, category?.name, bundle.accounts) {
        val parts = ArrayList<String>(3)
        if (hasNote1 && tx.note2.isNotBlank()) {
            parts.add(tx.note2.trim())
        }
        when (txType) {
            TransactionType.TRANSFER -> {
                parts.add("${bundle.accountName(tx.accountId)} → ${bundle.accountName(tx.toAccountId)}")
            }
            else -> {
                parts.add(bundle.accountName(tx.accountId))
                val catName = category?.name
                if (!catName.isNullOrBlank() && !catName.equals(primaryFocusText, ignoreCase = true)) {
                    parts.add(catName)
                }
            }
        }
        parts.joinToString(" • ")
    }

    val deltaRounded = row.signedDelta.roundToLong()
    val deltaColor = when {
        deltaRounded > 0L -> LedgerBlue
        deltaRounded < 0L -> LedgerCoral
        else -> LedgerTextSecondary
    }
    val accRounded = row.accumulatedBalance.roundToLong()
    val accColor = when {
        accRounded < 0L -> LedgerCoral
        accRounded > 0L -> LedgerGreen
        else -> LedgerTextPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .background(LedgerSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("tag_report_row_${row.rowKey}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Focused Note 1 + compact Tag & account/context metadata
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = primaryFocusText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = if (hasNote1) 15.sp else 14.sp,
                    fontWeight = if (hasNote1) FontWeight.Bold else FontWeight.Medium,
                    lineHeight = 19.sp
                ),
                color = if (hasNote1) LedgerTextPrimary else LedgerTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (row.tagKey == UNTAGGED_KEY) LedgerSurfaceHighlight
                            else LedgerAmber.copy(alpha = 0.16f)
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = row.tagDisplayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (row.tagKey == UNTAGGED_KEY) LedgerTextSecondary else LedgerAmber,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (row.roleHint != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LedgerSurfaceHighlight)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = row.roleHint,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = LedgerTextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (contextMeta.isNotBlank()) {
                    Text(
                        text = contextMeta,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = LedgerTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        // Middle-Right Column: Signed Transaction Value
        Text(
            text = formatSignedLedgerAmount(row.signedDelta, symbol, showPlus = true),
            style = TabularAmountStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
            color = deltaColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(valueColWidth)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Far-Right Column: Running Accumulated Value
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = LedgerSurfaceElevated,
            border = BorderStroke(0.75.dp, LedgerBorder.copy(alpha = 0.75f)),
            modifier = Modifier.width(accColWidth)
        ) {
            Text(
                text = formatSignedLedgerAmount(row.accumulatedBalance, symbol, showPlus = false),
                style = TabularAmountStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                color = accColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
}
