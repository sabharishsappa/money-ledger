package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.horizontalPeriodSwipe
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerMotion
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
    onImportExcel: () -> Unit,
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
    var pendingDeleteNote by remember { mutableStateOf<FinanceNote?>(null) }

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
                        contentDescription = "Filter by tagging",
                        tint = if (filter.selectedTagFilter != null) {
                            LedgerCoral
                        } else {
                            LedgerTextSecondary
                        }
                    )
                }
            }

            // Expandable Search Bar
            AnimatedVisibility(
                visible = showSearchBar,
                enter = LedgerMotion.expandEnter(),
                exit = LedgerMotion.shrinkExit()
            ) {
                OutlinedTextField(
                    value = filter.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search note 1, note 2, category, account, tag...", color = LedgerTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            AnimatedVisibility(
                visible = showTagFilterRow && bundle.allTaggings.isNotEmpty(),
                enter = LedgerMotion.expandEnter(),
                exit = LedgerMotion.shrinkExit()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = filter.selectedTagFilter == null,
                            onClick = { viewModel.setTagFilter(null) },
                            label = { Text("All Taggings") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LedgerCoral.copy(alpha = 0.22f),
                                selectedLabelColor = LedgerCoral
                            ),
                            modifier = Modifier.testTag("tx_tag1_filter_all")
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
                    val textColor by animateColorAsState(
                        targetValue = if (selected) LedgerTextPrimary else LedgerTextSecondary,
                        animationSpec = LedgerMotion.microTween(100),
                        label = "subtab_text_${tab.name}"
                    )
                    val indicatorColor by animateColorAsState(
                        targetValue = if (selected) LedgerCoral else Color.Transparent,
                        animationSpec = LedgerMotion.microTween(100),
                        label = "subtab_indicator_${tab.name}"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setSubTab(tab) }
                            .testTag("tx_subtab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                            color = textColor,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(indicatorColor)
                        )
                    }
                }
            }

            HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

            // 3-Column Income / Expenses / Total Summary Strip
            AnimatedVisibility(
                visible = filter.subTab != TransactionsSubTab.NOTE,
                enter = LedgerMotion.expandEnter(),
                exit = LedgerMotion.shrinkExit()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LedgerSurface)
                            .padding(vertical = 8.dp, horizontal = 8.dp),
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
                            valueColor = if (monthTotal < -0.004) LedgerCoral else LedgerTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
                }
            }

            // Sub-tab Content (Swipe left/right changes view period)
            AnimatedContent(
                targetState = filter.subTab,
                transitionSpec = {
                    LedgerMotion.tabSwitchTransform(forward = targetState.ordinal >= initialState.ordinal)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalPeriodSwipe(
                        enabled = filter.subTab != TransactionsSubTab.NOTE,
                        onSwipeNext = {
                            if (filter.subTab == TransactionsSubTab.MONTHLY) {
                                viewModel.shiftYear(1)
                            } else {
                                viewModel.nextMonth()
                            }
                        },
                        onSwipePrevious = {
                            if (filter.subTab == TransactionsSubTab.MONTHLY) {
                                viewModel.shiftYear(-1)
                            } else {
                                viewModel.previousMonth()
                            }
                        }
                    ),
                label = "tx_subtab_content"
            ) { currentSubTab ->
                when (currentSubTab) {
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
                            onImportExcel = onImportExcel,
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
                                pendingDeleteNote = note
                            }
                        )
                    }
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

    pendingDeleteNote?.let { note ->
        ConfirmDestructiveDialog(
            title = "Delete Note?",
            message = "Delete \"${note.title.ifBlank { "this note" }}\"? This cannot be undone.",
            confirmLabel = "Delete",
            confirmTestTag = "confirm_delete_note_button",
            onConfirm = {
                viewModel.deleteNote(note.id)
                pendingDeleteNote = null
            },
            onDismiss = { pendingDeleteNote = null }
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
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LedgerTextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = TabularAmountStyle.copy(fontSize = 13.5.sp),
            color = valueColor,
            maxLines = 1,
            softWrap = false,
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
            item(key = "header_${group.epochDay}", contentType = "day_header") {
                DayGroupHeader(
                    group = group,
                    symbol = symbol,
                    modifier = Modifier.animateItem()
                )
            }
            items(
                items = group.transactions,
                key = { it.id },
                contentType = { "tx_row" }
            ) { tx ->
                Column(modifier = Modifier.animateItem()) {
                    TransactionRowItem(
                        tx = tx,
                        bundle = bundle,
                        symbol = symbol,
                        onClick = { onEditTransaction(tx) }
                    )
                    HorizontalDivider(color = LedgerDivider.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
            item(key = "spacer_${group.epochDay}", contentType = "day_spacer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(LedgerBackground)
                )
            }
        }
    }
}

@Composable
private fun DayGroupHeader(
    group: DayTransactionGroup,
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

    Column(modifier = modifier.fillMaxWidth().background(LedgerSurface)) {
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

            Spacer(modifier = Modifier.weight(1f).widthIn(min = 6.dp))

            Text(
                text = LedgerFormatters.formatAmount(group.dayIncome, symbol),
                style = TabularAmountStyle.copy(fontSize = 12.5.sp),
                color = LedgerBlue,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(
                text = LedgerFormatters.formatAmount(group.dayExpense, symbol),
                style = TabularAmountStyle.copy(fontSize = 12.5.sp),
                color = LedgerCoral,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }
    val middleTitle = when {
        tx.note.isNotBlank() -> tx.note
        tx.note2.isNotBlank() -> tx.note2
        txType == TransactionType.TRANSFER -> "Transfer"
        !tx.subcategory.isNullOrBlank() -> tx.subcategory
        else -> category?.name ?: leftPrimary
    }
    val secondaryNoteLine = if (tx.note.isNotBlank() && tx.note2.isNotBlank()) tx.note2 else null

    val accountRoute = remember(tx.id, txType, tx.accountId, tx.toAccountId, bundle.accounts) {
        when (txType) {
            TransactionType.TRANSFER -> {
                val from = bundle.accountName(tx.accountId)
                val to = bundle.accountName(tx.toAccountId)
                "$from → $to"
            }
            else -> bundle.accountName(tx.accountId)
        }
    }

    val amountColor = when (txType) {
        TransactionType.INCOME -> LedgerBlue
        TransactionType.EXPENSE -> LedgerCoral
        TransactionType.TRANSFER -> LedgerTextPrimary
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(LedgerSurface)
            .clickable(onClick = onClick)
            .testTag("tx_row_${tx.id}")
    ) {
        val leftColWidth = (maxWidth * 0.21f).coerceIn(66.dp, 88.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Transaction Type (Income / Expense / Transfer)
            Box(
                modifier = Modifier
                    .width(leftColWidth)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = leftPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Middle Column: Note / Ledger Title + Account Route
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = middleTitle,
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
                if (secondaryNoteLine != null) {
                    Text(
                        text = secondaryNoteLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = accountRoute,
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val fromTag1 = tx.effectiveTagging()
                    val toTag1 = tx.effectiveToTagging()
                    val displayTagging = when {
                        fromTag1 == null && toTag1 == null -> null
                        fromTag1 != null && toTag1 == null -> fromTag1
                        fromTag1 == null && toTag1 != null -> if (txType == TransactionType.TRANSFER) "— → $toTag1" else toTag1
                        fromTag1.equals(toTag1, ignoreCase = true) -> fromTag1
                        else -> "$fromTag1 → $toTag1"
                    }

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
                                softWrap = false,
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
                    style = TabularAmountStyle.copy(fontSize = 13.5.sp),
                    color = amountColor,
                    maxLines = 1,
                    softWrap = false
                )
                if (txType == TransactionType.TRANSFER && tx.feeAmount > 0.0) {
                    Text(
                        text = "Fee ${LedgerFormatters.formatAmount(tx.feeAmount, symbol)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = LedgerTextMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
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
    val daysCount = remember(filter.selectedYear, filter.selectedMonth) {
        LedgerFormatters.daysInMonth(filter.selectedYear, filter.selectedMonth)
    }
    val firstDow = remember(filter.selectedYear, filter.selectedMonth) {
        LedgerFormatters.firstDayOfWeekInMonth(filter.selectedYear, filter.selectedMonth)
    }
    val leadingBlanks = firstDow - 1

    val byDay = remember(monthTransactions) { monthTransactions.groupBy { it.epochDay } }
    val dayTotals = remember(byDay) {
        byDay.mapValues { (_, list) ->
            val inc = list.filter { it.txType() == TransactionType.INCOME }.sumOf { it.amount }
            val exp = list.filter { it.txType() == TransactionType.EXPENSE }.sumOf { it.amount }
            inc to exp
        }
    }
    var selectedDayOfMonth by remember(filter.selectedYear, filter.selectedMonth) {
        mutableStateOf<Int?>(null)
    }

    val selectedEpochDay = selectedDayOfMonth?.let {
        LedgerFormatters.calendarToEpochDay(filter.selectedYear, filter.selectedMonth, it)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cellHeight = (maxWidth / 7f).coerceIn(50.dp, 68.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item(key = "cal_weekdays", contentType = "cal_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurfaceElevated)
                        .padding(vertical = 7.dp)
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
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val totalCells = leadingBlanks + daysCount
            val rows = (totalCells + 6) / 7
            items(rows, key = { "cal_row_$it" }, contentType = { "cal_row" }) { rowIndex ->
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
                            val totals = dayTotals[epochDay]
                            val inc = totals?.first ?: 0.0
                            val exp = totals?.second ?: 0.0
                            val hasEntries = totals != null
                            val isSelected = selectedDayOfMonth == dayNum
                            val cellBg by animateColorAsState(
                                targetValue = if (isSelected) LedgerCoral.copy(alpha = 0.16f) else LedgerSurface,
                                animationSpec = tween(150),
                                label = "cal_cell_$dayNum"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cellHeight)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) LedgerCoral else LedgerDivider
                                    )
                                    .background(cellBg)
                                    .clickable {
                                        selectedDayOfMonth = if (isSelected) null else dayNum
                                    }
                                    .padding(horizontal = 2.dp, vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = when (col) {
                                        0 -> LedgerCoral
                                        6 -> LedgerBlue
                                        else -> LedgerTextPrimary
                                    },
                                    fontWeight = if (hasEntries) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                if (inc > 0.0) {
                                    Text(
                                        text = LedgerFormatters.formatWholeAmount(inc, symbol, false),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = LedgerBlue,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                                if (exp > 0.0) {
                                    Text(
                                        text = LedgerFormatters.formatWholeAmount(exp, symbol, false),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = LedgerCoral,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cellHeight)
                                    .border(0.5.dp, LedgerDivider)
                                    .background(LedgerBackground.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }

            if (selectedEpochDay != null) {
                val selectedTxs = byDay[selectedEpochDay].orEmpty()
                item(key = "selected_day_hdr_$selectedEpochDay") {
                    Text(
                        text = LedgerFormatters.formatFullDateInput(selectedEpochDay),
                        style = MaterialTheme.typography.titleSmall,
                        color = LedgerTextSecondary,
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                if (selectedTxs.isEmpty()) {
                    item(key = "selected_day_empty_$selectedEpochDay") {
                        Text(
                            text = "No entries on this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerTextMuted,
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(selectedTxs, key = { it.id }, contentType = { "tx_row" }) { tx ->
                        Column(modifier = Modifier.animateItem()) {
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
        items(rows, key = { it.monthZeroBased }, contentType = { "monthly_row" }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(LedgerSurface)
                    .clickable { onSelectMonth(row.monthZeroBased) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.monthLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.85f)
                )
                Text(
                    text = LedgerFormatters.formatWholeAmount(row.income, symbol),
                    style = TabularAmountStyle.copy(fontSize = 12.5.sp),
                    color = LedgerBlue,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = LedgerFormatters.formatWholeAmount(row.expense, symbol),
                    style = TabularAmountStyle.copy(fontSize = 12.5.sp),
                    color = LedgerCoral,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = LedgerFormatters.formatSignedWhole(row.net, symbol),
                    style = TabularAmountStyle.copy(fontSize = 12.5.sp),
                    color = if (row.net <= -0.5) LedgerCoral else LedgerTextPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
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
    onImportExcel: () -> Unit,
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
        contentPadding = PaddingValues(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Budget Summary Card
        item(key = "total_budgets") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LedgerSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LedgerDivider),
                modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                        val activeBudgets = bundle.budgets.filter { it.monthlyLimit > 0.0 }
                        if (activeBudgets.isEmpty()) {
                            Text(
                                text = "Set monthly spending targets per category to monitor budget pacing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LedgerTextSecondary
                            )
                        } else {
                            activeBudgets.forEach { budget ->
                                val cat = bundle.categoryById(budget.categoryId)
                                val spent = expenseByCat[budget.categoryId] ?: 0.0
                                val rawRatio = if (budget.monthlyLimit > 0.0) (spent / budget.monthlyLimit).toFloat() else 0f
                                val isOver = spent > budget.monthlyLimit + 0.004
                                val pctInt = (rawRatio * 100f).toInt()
                                val barColor = budgetStatusColor(rawRatio, isOver)
                                val animatedProgress by animateFloatAsState(
                                    targetValue = rawRatio.coerceIn(0f, 1f),
                                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                                    label = "budget_progress_${budget.id}"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat?.name ?: "Category",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = LedgerTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "$pctInt% • ${LedgerFormatters.formatWholeAmount(spent, symbol)} / ${LedgerFormatters.formatWholeAmount(budget.monthlyLimit, symbol)}",
                                        style = TabularAmountStyle.copy(fontSize = 12.sp),
                                        color = if (isOver) LedgerCoral else LedgerTextSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    color = barColor,
                                    trackColor = LedgerSurfaceHighlight,
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                }
            }
        }

        // Period Cash Flow Breakdown
        item(key = "total_period_summary") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LedgerSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LedgerDivider),
                modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Period Summary", style = MaterialTheme.typography.titleMedium, color = LedgerTextPrimary)
                    TotalKeyValueRow("Total Entries", "${monthTransactions.size}", LedgerTextPrimary)
                    TotalKeyValueRow("Total Income", LedgerFormatters.formatAmount(monthIncome, symbol), LedgerBlue)
                    TotalKeyValueRow("Total Expenses", LedgerFormatters.formatAmount(monthExpense, symbol), LedgerCoral)
                    TotalKeyValueRow("Internal Transfers", LedgerFormatters.formatAmount(transferTotal, symbol), LedgerTextPrimary)
                    HorizontalDivider(color = LedgerDivider)
                    val netSavings = monthIncome - monthExpense
                    TotalKeyValueRow(
                        label = "Net Savings",
                        value = LedgerFormatters.formatAmount(netSavings, symbol),
                        color = if (netSavings < -0.004) LedgerCoral else LedgerTextPrimary
                    )
                }
            }
        }

        // Export & Import Excel Actions
        item(key = "total_excel_actions") {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerSurfaceElevated, contentColor = LedgerTextPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("total_export_csv_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = LedgerCoral, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Ledger to Excel (.xlsx)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onImportExcel,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LedgerBlue.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("total_import_excel_button")
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, tint = LedgerBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import from Excel (.xlsx)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        item(key = "total_bottom_spacer") { Spacer(modifier = Modifier.height(72.dp)) }
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notes, key = { it.id }, contentType = { "note_card" }) { note ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LedgerSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (note.pinned) LedgerCoral.copy(alpha = 0.5f) else LedgerDivider
                ),
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .clickable { onEditNote(note) }
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
