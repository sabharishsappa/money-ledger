package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceNote
import com.example.data.LedgerTransaction
import com.example.data.RepeatFrequency
import com.example.data.TransactionType
import com.example.data.TransactionsSubTab
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerSurfaceHighlight
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.ui.theme.TabularAmountStyle
import com.example.util.LedgerFormatters
import com.example.viewmodel.DayTransactionGroup
import com.example.viewmodel.FilterState
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import java.util.Calendar

@Composable
fun TransactionsScreen(
    bundle: LedgerDataBundle,
    filter: FilterState,
    viewModel: LedgerViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit,
    onExportCsv: () -> Unit,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol
    val monthTransactions = remember(bundle.transactions, filter) {
        viewModel.transactionsForSelectedMonth(bundle, filter)
    }
    val dayGroups = remember(monthTransactions) { viewModel.groupByDay(monthTransactions) }
    val monthIncome = remember(monthTransactions) {
        monthTransactions.filter { it.txType() == TransactionType.INCOME }.sumOf { it.amount }
    }
    val monthExpense = remember(monthTransactions) {
        monthTransactions.filter { it.txType() == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val monthTotal = monthIncome - monthExpense

    var showSearchBar by remember { mutableStateOf(false) }
    var showTagFilterRow by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<FinanceNote?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(LedgerBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Month Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.previousMonth() },
                    modifier = Modifier.testTag("tx_prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = LedgerTextPrimary
                    )
                }
                Text(
                    text = LedgerFormatters.formatMonthYear(filter.selectedYear, filter.selectedMonth),
                    style = MaterialTheme.typography.titleMedium,
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { viewModel.nextMonth() },
                    modifier = Modifier.testTag("tx_next_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = LedgerTextPrimary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { viewModel.toggleFavoritesOnly() },
                    modifier = Modifier.testTag("tx_favorites_filter_button")
                ) {
                    Icon(
                        imageVector = if (filter.favoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Filter favorites",
                        tint = if (filter.favoritesOnly) LedgerAmber else LedgerTextSecondary
                    )
                }
                IconButton(
                    onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) viewModel.setSearchQuery("")
                    },
                    modifier = Modifier.testTag("tx_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search transactions",
                        tint = if (showSearchBar || filter.searchQuery.isNotEmpty()) LedgerCoral else LedgerTextSecondary
                    )
                }
                IconButton(
                    onClick = { showTagFilterRow = !showTagFilterRow },
                    modifier = Modifier.testTag("tx_filter_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Filter by tag",
                        tint = if (filter.selectedTagFilter != null) LedgerCoral else LedgerTextSecondary
                    )
                }
            }

            // Expandable Search Bar
            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = filter.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search note, category, account, tag...", color = LedgerTextMuted) },
                    singleLine = true,
                    trailingIcon = {
                        if (filter.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = LedgerTextSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LedgerSurfaceElevated,
                        unfocusedContainerColor = LedgerSurfaceElevated,
                        focusedBorderColor = LedgerCoral,
                        unfocusedBorderColor = LedgerBorder,
                        focusedTextColor = LedgerTextPrimary,
                        unfocusedTextColor = LedgerTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("tx_search_input")
                )
            }

            // Expandable Tagging Filter Row
            AnimatedVisibility(visible = showTagFilterRow && bundle.allTaggings.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filter.selectedTagFilter == null,
                            onClick = { viewModel.setTagFilter(null) },
                            label = { Text("All Taggings") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LedgerCoral.copy(alpha = 0.22f),
                                selectedLabelColor = LedgerCoral
                            )
                        )
                    }
                    items(bundle.allTaggings) { tagging ->
                        FilterChip(
                            selected = filter.selectedTagFilter == tagging,
                            onClick = {
                                viewModel.setTagFilter(if (filter.selectedTagFilter == tagging) null else tagging)
                            },
                            label = { Text(tagging) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LedgerCoral.copy(alpha = 0.22f),
                                selectedLabelColor = LedgerCoral
                            )
                        )
                    }
                }
            }

            // 5 Sub-Tabs Row: Daily | Calendar | Monthly | Total | Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurface)
            ) {
                TransactionsSubTab.entries.forEach { tab ->
                    val selected = filter.subTab == tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setSubTab(tab) }
                            .testTag("tx_subtab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) LedgerTextPrimary else LedgerTextSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (selected) LedgerCoral else Color.Transparent)
                        )
                    }
                }
            }

            HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

            // 3-Column Income / Expenses / Total Summary Strip
            if (filter.subTab != TransactionsSubTab.NOTE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryMetricColumn(
                        label = "Income",
                        value = LedgerFormatters.formatAmount(monthIncome, symbol, includeSymbol = false),
                        valueColor = LedgerBlue,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricColumn(
                        label = "Expenses",
                        value = LedgerFormatters.formatAmount(monthExpense, symbol, includeSymbol = false),
                        valueColor = LedgerCoral,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricColumn(
                        label = "Total",
                        value = LedgerFormatters.formatAmount(monthTotal, symbol, includeSymbol = false),
                        valueColor = LedgerTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
            }

            // Sub-tab Content
            when (filter.subTab) {
                TransactionsSubTab.DAILY -> {
                    DailyTransactionsList(
                        bundle = bundle,
                        dayGroups = dayGroups,
                        symbol = symbol,
                        onEditTransaction = onEditTransaction,
                        onAddTransaction = onAddTransaction
                    )
                }
                TransactionsSubTab.CALENDAR -> {
                    CalendarTransactionsView(
                        bundle = bundle,
                        filter = filter,
                        monthTransactions = monthTransactions,
                        symbol = symbol,
                        onEditTransaction = onEditTransaction
                    )
                }
                TransactionsSubTab.MONTHLY -> {
                    MonthlySummaryList(
                        bundle = bundle,
                        year = filter.selectedYear,
                        symbol = symbol,
                        viewModel = viewModel,
                        onSelectMonth = { monthZero ->
                            viewModel.selectMonth(filter.selectedYear, monthZero)
                            viewModel.setSubTab(TransactionsSubTab.DAILY)
                        }
                    )
                }
                TransactionsSubTab.TOTAL -> {
                    TotalOverviewTab(
                        bundle = bundle,
                        monthTransactions = monthTransactions,
                        monthIncome = monthIncome,
                        monthExpense = monthExpense,
                        symbol = symbol,
                        onExportCsv = onExportCsv,
                        onOpenBudgets = onOpenBudgets
                    )
                }
                TransactionsSubTab.NOTE -> {
                    NotesListTab(
                        notes = bundle.notes,
                        onAddNote = {
                            editingNote = null
                            showNoteDialog = true
                        },
                        onEditNote = { note ->
                            editingNote = note
                            showNoteDialog = true
                        },
                        onTogglePin = { note ->
                            viewModel.saveNote(note.id, note.title, note.content, !note.pinned)
                        },
                        onDeleteNote = { note ->
                            viewModel.deleteNote(note.id)
                        }
                    )
                }
            }
        }

        // Coral Floating Action Button (+)
        FloatingActionButton(
            onClick = {
                if (filter.subTab == TransactionsSubTab.NOTE) {
                    editingNote = null
                    showNoteDialog = true
                } else {
                    onAddTransaction()
                }
            },
            shape = CircleShape,
            containerColor = LedgerCoral,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .size(56.dp)
                .testTag("add_transaction_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = if (filter.subTab == TransactionsSubTab.NOTE) "Add Note" else "Add Transaction",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showNoteDialog) {
        NoteEditorDialog(
            existing = editingNote,
            onDismiss = { showNoteDialog = false },
            onSave = { title, content, pinned ->
                viewModel.saveNote(editingNote?.id, title, content, pinned)
                showNoteDialog = false
            }
        )
    }
}

@Composable
fun SummaryMetricColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LedgerTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = TabularAmountStyle.copy(fontSize = 14.sp),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyTransactionsList(
    bundle: LedgerDataBundle,
    dayGroups: List<DayTransactionGroup>,
    symbol: String,
    onEditTransaction: (LedgerTransaction) -> Unit,
    onAddTransaction: () -> Unit
) {
    val repeating = remember(bundle.transactions) {
        bundle.transactions.filter { it.repeat() != RepeatFrequency.NONE }
    }

    if (dayGroups.isEmpty() && repeating.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = LedgerTextMuted,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No transactions for this month",
                    style = MaterialTheme.typography.titleMedium,
                    color = LedgerTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap the + button to log an expense, income, or account transfer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAddTransaction,
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("empty_add_transaction_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Transaction")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        if (repeating.isNotEmpty()) {
            item(key = "repeat_banner") {
                val repeatSum = repeating.sumOf { it.amount }
                val previewNames = repeating.take(2).joinToString(", ") {
                    it.note.ifBlank { bundle.categoryById(it.categoryId)?.name ?: "Recurring" }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurfaceElevated.copy(alpha = 0.65f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(88.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = LedgerTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.bodySmall,
                            color = LedgerTextSecondary
                        )
                    }
                    Text(
                        text = previewNames,
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = LedgerFormatters.formatAmount(repeatSum, symbol),
                        style = TabularAmountStyle.copy(fontSize = 13.sp),
                        color = LedgerTextPrimary
                    )
                }
                HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
            }
        }

        dayGroups.forEach { group ->
            item(key = "header_${group.epochDay}") {
                DayGroupHeader(group = group, symbol = symbol)
            }
            items(group.transactions, key = { it.id }) { tx ->
                TransactionRowItem(
                    tx = tx,
                    bundle = bundle,
                    symbol = symbol,
                    onClick = { onEditTransaction(tx) }
                )
                HorizontalDivider(color = LedgerDivider.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
            item(key = "spacer_${group.epochDay}") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(LedgerBackground)
                )
            }
        }
    }
}

@Composable
private fun DayGroupHeader(
    group: DayTransactionGroup,
    symbol: String
) {
    val dayNum = LedgerFormatters.dayOfMonthPadded(group.epochDay)
    val weekday = LedgerFormatters.weekdayShort(group.epochDay)
    val monthYear = LedgerFormatters.monthYearDot(group.epochDay)
    val dow = LedgerFormatters.dayOfWeekIndex(group.epochDay)
    val badgeBg = when (dow) {
        Calendar.SATURDAY -> LedgerBlue
        Calendar.SUNDAY -> LedgerCoral
        else -> LedgerSurfaceHighlight
    }

    Column(modifier = Modifier.fillMaxWidth().background(LedgerSurface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayNum,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = LedgerTextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = monthYear,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTextMuted
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = LedgerFormatters.formatAmount(group.dayIncome, symbol),
                style = TabularAmountStyle.copy(fontSize = 13.sp),
                color = LedgerBlue,
                modifier = Modifier.padding(end = 14.dp)
            )
            Text(
                text = LedgerFormatters.formatAmount(group.dayExpense, symbol),
                style = TabularAmountStyle.copy(fontSize = 13.sp),
                color = LedgerCoral
            )
        }
        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
    }
}

@Composable
fun TransactionRowItem(
    tx: LedgerTransaction,
    bundle: LedgerDataBundle,
    symbol: String,
    onClick: () -> Unit
) {
    val txType = tx.txType()
    val category = bundle.categoryById(tx.categoryId)
    val leftPrimary = when (txType) {
        TransactionType.TRANSFER -> "Transfer"
        else -> category?.name ?: "Uncategorized"
    }
    val leftSecondary = if (txType != TransactionType.TRANSFER) tx.subcategory else null

    val accountRoute = when (txType) {
        TransactionType.TRANSFER -> {
            val from = bundle.accountName(tx.accountId)
            val to = bundle.accountName(tx.toAccountId)
            "$from → $to"
        }
        else -> bundle.accountName(tx.accountId)
    }

    val amountColor = when (txType) {
        TransactionType.INCOME -> LedgerBlue
        TransactionType.EXPENSE -> LedgerCoral
        TransactionType.TRANSFER -> LedgerTextPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LedgerSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("tx_row_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Category + Subcategory
        Column(modifier = Modifier.width(96.dp).padding(end = 8.dp)) {
            Text(
                text = leftPrimary,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!leftSecondary.isNullOrBlank()) {
                Text(
                    text = leftSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Middle Column: Note / Description + Account Route
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tx.note.ifBlank { leftPrimary },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (tx.favorite) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = LedgerAmber,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = accountRoute,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val displayTagging = tx.effectiveTagging()
                if (displayTagging != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LedgerSurfaceHighlight)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = displayTagging,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = LedgerTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Right Column: Amount
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = LedgerFormatters.formatAmount(tx.amount, symbol),
                style = TabularAmountStyle.copy(fontSize = 14.sp),
                color = amountColor,
                maxLines = 1
            )
            if (txType == TransactionType.TRANSFER && tx.feeAmount > 0.0) {
                Text(
                    text = "Fee ${LedgerFormatters.formatAmount(tx.feeAmount, symbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted
                )
            }
        }
    }
}

@Composable
private fun CalendarTransactionsView(
    bundle: LedgerDataBundle,
    filter: FilterState,
    monthTransactions: List<LedgerTransaction>,
    symbol: String,
    onEditTransaction: (LedgerTransaction) -> Unit
) {
    val daysCount = LedgerFormatters.daysInMonth(filter.selectedYear, filter.selectedMonth)
    val firstDow = LedgerFormatters.firstDayOfWeekInMonth(filter.selectedYear, filter.selectedMonth) // 1=Sun..7=Sat
    val leadingBlanks = firstDow - 1

    val byDay = remember(monthTransactions) { monthTransactions.groupBy { it.epochDay } }
    var selectedDayOfMonth by remember(filter.selectedYear, filter.selectedMonth) {
        mutableStateOf<Int?>(null)
    }

    val selectedEpochDay = selectedDayOfMonth?.let {
        LedgerFormatters.calendarToEpochDay(filter.selectedYear, filter.selectedMonth, it)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurfaceElevated)
                    .padding(vertical = 8.dp)
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEachIndexed { i, name ->
                    val colColor = when (i) {
                        0 -> LedgerCoral
                        6 -> LedgerBlue
                        else -> LedgerTextSecondary
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = colColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val totalCells = leadingBlanks + daysCount
        val rows = (totalCells + 6) / 7
        items(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth().background(LedgerSurface)) {
                for (col in 0..6) {
                    val cellIdx = rowIndex * 7 + col
                    val dayNum = cellIdx - leadingBlanks + 1
                    if (dayNum in 1..daysCount) {
                        val epochDay = LedgerFormatters.calendarToEpochDay(
                            filter.selectedYear,
                            filter.selectedMonth,
                            dayNum
                        )
                        val txs = byDay[epochDay].orEmpty()
                        val inc = txs.filter { it.txType() == TransactionType.INCOME }.sumOf { it.amount }
                        val exp = txs.filter { it.txType() == TransactionType.EXPENSE }.sumOf { it.amount }
                        val isSelected = selectedDayOfMonth == dayNum

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) LedgerCoral else LedgerDivider
                                )
                                .background(
                                    if (isSelected) LedgerCoral.copy(alpha = 0.14f) else LedgerSurface
                                )
                                .clickable {
                                    selectedDayOfMonth = if (isSelected) null else dayNum
                                }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayNum.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (col) {
                                    0 -> LedgerCoral
                                    6 -> LedgerBlue
                                    else -> LedgerTextPrimary
                                },
                                fontWeight = if (txs.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (inc > 0.0) {
                                Text(
                                    text = LedgerFormatters.formatAmount(inc, symbol, false),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = LedgerBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                            if (exp > 0.0) {
                                Text(
                                    text = LedgerFormatters.formatAmount(exp, symbol, false),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = LedgerCoral,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .border(0.5.dp, LedgerDivider)
                                .background(LedgerBackground.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }

        if (selectedEpochDay != null) {
            val selectedTxs = byDay[selectedEpochDay].orEmpty()
            item {
                Text(
                    text = LedgerFormatters.formatFullDateInput(selectedEpochDay),
                    style = MaterialTheme.typography.titleSmall,
                    color = LedgerTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            if (selectedTxs.isEmpty()) {
                item {
                    Text(
                        text = "No entries on this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerTextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(selectedTxs, key = { it.id }) { tx ->
                    TransactionRowItem(
                        tx = tx,
                        bundle = bundle,
                        symbol = symbol,
                        onClick = { onEditTransaction(tx) }
                    )
                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryList(
    bundle: LedgerDataBundle,
    year: Int,
    symbol: String,
    viewModel: LedgerViewModel,
    onSelectMonth: (Int) -> Unit
) {
    val rows = remember(bundle.transactions, year) {
        viewModel.monthlySummariesForYear(bundle, year)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(rows, key = { it.monthZeroBased }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurface)
                    .clickable { onSelectMonth(row.monthZeroBased) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.monthLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = LedgerTextPrimary,
                    modifier = Modifier.width(92.dp)
                )
                Text(
                    text = LedgerFormatters.formatAmount(row.income, symbol),
                    style = TabularAmountStyle.copy(fontSize = 13.sp),
                    color = LedgerBlue,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = LedgerFormatters.formatAmount(row.expense, symbol),
                    style = TabularAmountStyle.copy(fontSize = 13.sp),
                    color = LedgerCoral,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = LedgerFormatters.formatAmount(row.net, symbol),
                    style = TabularAmountStyle.copy(fontSize = 13.sp),
                    color = LedgerTextPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun TotalOverviewTab(
    bundle: LedgerDataBundle,
    monthTransactions: List<LedgerTransaction>,
    monthIncome: Double,
    monthExpense: Double,
    symbol: String,
    onExportCsv: () -> Unit,
    onOpenBudgets: () -> Unit
) {
    val transferTotal = remember(monthTransactions) {
        monthTransactions.filter { it.txType() == TransactionType.TRANSFER }.sumOf { it.amount }
    }
    val expenseByCat = remember(monthTransactions) {
        monthTransactions.filter { it.txType() == TransactionType.EXPENSE }
            .groupBy { it.categoryId ?: "" }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Budget Summary Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LedgerSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LedgerDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monthly Budgets", style = MaterialTheme.typography.titleMedium, color = LedgerTextPrimary)
                        TextButton(onClick = onOpenBudgets) {
                            Text("Manage", color = LedgerCoral)
                        }
                    }
                    if (bundle.budgets.isEmpty()) {
                        Text(
                            text = "Set monthly spending targets per category to monitor budget pacing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LedgerTextSecondary
                        )
                    } else {
                        bundle.budgets.forEach { budget ->
                            val cat = bundle.categoryById(budget.categoryId)
                            val spent = expenseByCat[budget.categoryId] ?: 0.0
                            val progress = if (budget.monthlyLimit > 0.0) (spent / budget.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat?.name ?: "Category", style = MaterialTheme.typography.bodyMedium, color = LedgerTextPrimary)
                                Text(
                                    text = "${LedgerFormatters.formatAmount(spent, symbol)} / ${LedgerFormatters.formatAmount(budget.monthlyLimit, symbol)}",
                                    style = TabularAmountStyle.copy(fontSize = 12.sp),
                                    color = if (spent > budget.monthlyLimit) LedgerCoral else LedgerTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                color = if (spent > budget.monthlyLimit) LedgerCoral else LedgerBlue,
                                trackColor = LedgerSurfaceHighlight,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }

        // Period Cash Flow Breakdown
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LedgerSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LedgerDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Period Summary", style = MaterialTheme.typography.titleMedium, color = LedgerTextPrimary)
                    TotalKeyValueRow("Total Entries", "${monthTransactions.size}", LedgerTextPrimary)
                    TotalKeyValueRow("Total Income", LedgerFormatters.formatAmount(monthIncome, symbol), LedgerBlue)
                    TotalKeyValueRow("Total Expenses", LedgerFormatters.formatAmount(monthExpense, symbol), LedgerCoral)
                    TotalKeyValueRow("Internal Transfers", LedgerFormatters.formatAmount(transferTotal, symbol), LedgerTextPrimary)
                    HorizontalDivider(color = LedgerDivider)
                    TotalKeyValueRow("Net Savings", LedgerFormatters.formatAmount(monthIncome - monthExpense, symbol), LedgerTextPrimary)
                }
            }
        }

        // Export Excel Action
        item {
            Button(
                onClick = onExportCsv,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LedgerSurfaceElevated, contentColor = LedgerTextPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("total_export_csv_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, tint = LedgerCoral, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Ledger to Excel (.xlsx)")
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun TotalKeyValueRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LedgerTextSecondary)
        Text(value, style = TabularAmountStyle, color = color)
    }
}

@Composable
private fun NotesListTab(
    notes: List<FinanceNote>,
    onAddNote: () -> Unit,
    onEditNote: (FinanceNote) -> Unit,
    onTogglePin: (FinanceNote) -> Unit,
    onDeleteNote: (FinanceNote) -> Unit
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No financial notes yet", style = MaterialTheme.typography.titleMedium, color = LedgerTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Keep bill reminders, renewal dates, or tax memos alongside your ledger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddNote,
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Note")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LedgerSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (note.pinned) LedgerCoral.copy(alpha = 0.5f) else LedgerDivider
                ),
                modifier = Modifier.fillMaxWidth().clickable { onEditNote(note) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = LedgerTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onTogglePin(note) }) {
                            Icon(
                                imageVector = if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin note",
                                tint = if (note.pinned) LedgerCoral else LedgerTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { onDeleteNote(note) }) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete note",
                                tint = LedgerTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (note.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerTextSecondary
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(76.dp)) }
    }
}

@Composable
private fun NoteEditorDialog(
    existing: FinanceNote?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var content by remember(existing) { mutableStateOf(existing?.content.orEmpty()) }
    var pinned by remember(existing) { mutableStateOf(existing?.pinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LedgerSurface,
        title = {
            Text(
                text = if (existing == null) "New Financial Note" else "Edit Note",
                color = LedgerTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it.take(2000) },
                    label = { Text("Memo / Details") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, content, pinned) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral)
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
}
