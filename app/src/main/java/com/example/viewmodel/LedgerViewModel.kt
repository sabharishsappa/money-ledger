package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Account
import com.example.data.AccountGroup
import com.example.data.AccountNature
import com.example.data.Budget
import com.example.data.Category
import com.example.data.CategoryType
import com.example.data.FinanceNote
import com.example.data.LedgerDataSource
import com.example.data.LedgerTransaction
import com.example.data.ReportPeriodMode
import com.example.data.StatsPeriodType
import com.example.data.TransactionType
import com.example.data.TransactionsSubTab
import com.example.data.UserSettings
import com.example.util.LedgerExcelExporter
import com.example.util.LedgerFormatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class AccountWithBalance(
    val account: Account,
    val balance: Double,
    val tagBalances: Map<String, Double> = emptyMap(),
    val untaggedBalance: Double = 0.0
)

data class AccountGroupSection(
    val group: AccountGroup,
    val accounts: List<AccountWithBalance>,
    val subtotal: Double,
    val tagSubtotals: Map<String, Double> = emptyMap(),
    val untaggedSubtotal: Double = 0.0
)

data class DayTransactionGroup(
    val epochDay: Long,
    val transactions: List<LedgerTransaction>,
    val dayIncome: Double,
    val dayExpense: Double
)

data class CategoryStatSlice(
    val categoryId: String,
    val categoryName: String,
    val colorHex: String,
    val amount: Double,
    val percentage: Double,
    val transactions: List<LedgerTransaction>
)

data class MonthlySummaryRow(
    val monthZeroBased: Int,
    val monthLabel: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class NetWorthPeriodPoint(
    val key: String,
    val shortLabel: String,
    val fullLabel: String,
    val cutoffEpochDay: Long,
    val assets: Double,
    val liabilities: Double,
    val netWorth: Double
)

data class NetWorthAccountSnapshot(
    val account: Account,
    val groupName: String,
    val balance: Double
)

data class LedgerDataBundle(
    val settings: UserSettings = UserSettings(),
    val accountGroups: List<AccountGroup> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<LedgerTransaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val notes: List<FinanceNote> = emptyList()
) {
    val activeGroups: List<AccountGroup>
        get() = accountGroups.filter { !it.archived }

    val archivedGroups: List<AccountGroup>
        get() = accountGroups.filter { it.archived }

    val activeAccounts: List<Account>
        get() = accounts.filter { !it.archived }

    val archivedAccounts: List<Account>
        get() = accounts.filter { it.archived }

    val netWorthIncludedAccounts: List<Account>
        get() = activeAccounts.filter { it.includeInNetWorth }

    val netWorthExcludedAccounts: List<Account>
        get() = activeAccounts.filter { !it.includeInNetWorth }

    val allTaggings: List<String> by lazy {
        transactions.mapNotNull { it.effectiveTagging() }
            .distinct()
            .sorted()
    }

    val accountBalances: Map<String, Double> by lazy {
        val map = accounts.associate { it.id to it.initialBalance }.toMutableMap()
        transactions.forEach { tx ->
            when (tx.txType()) {
                TransactionType.INCOME -> {
                    map[tx.accountId] = (map[tx.accountId] ?: 0.0) + tx.amount
                }
                TransactionType.EXPENSE -> {
                    map[tx.accountId] = (map[tx.accountId] ?: 0.0) - tx.amount
                }
                TransactionType.TRANSFER -> {
                    map[tx.accountId] = (map[tx.accountId] ?: 0.0) - tx.amount - tx.feeAmount
                    tx.toAccountId?.let { dest ->
                        map[dest] = (map[dest] ?: 0.0) + tx.amount
                    }
                }
            }
        }
        map
    }

    private val perAccountTagSplits: Pair<Map<String, Map<String, Double>>, Map<String, Double>> by lazy {
        val tagged = mutableMapOf<String, MutableMap<String, Double>>()
        val untagged = accounts.associate { it.id to it.initialBalance }.toMutableMap()

        fun addDelta(accId: String, tagKey: String?, delta: Double) {
            if (tagKey.isNullOrBlank()) {
                untagged[accId] = (untagged[accId] ?: 0.0) + delta
            } else {
                val accMap = tagged.getOrPut(accId) { mutableMapOf() }
                accMap[tagKey] = (accMap[tagKey] ?: 0.0) + delta
            }
        }

        transactions.forEach { tx ->
            val tagKey = tx.effectiveTagging()
            when (tx.txType()) {
                TransactionType.INCOME -> addDelta(tx.accountId, tagKey, tx.amount)
                TransactionType.EXPENSE -> addDelta(tx.accountId, tagKey, -tx.amount)
                TransactionType.TRANSFER -> {
                    addDelta(tx.accountId, tagKey, -(tx.amount + tx.feeAmount))
                    tx.toAccountId?.let { dest ->
                        addDelta(dest, tagKey, tx.amount)
                    }
                }
            }
        }

        tagged to untagged
    }

    val accountTagBalances: Map<String, Map<String, Double>>
        get() = perAccountTagSplits.first

    val accountUntaggedBalances: Map<String, Double>
        get() = perAccountTagSplits.second

    val hasAnyUntaggedBalance: Boolean by lazy {
        activeAccounts.any { acc ->
            abs(accountUntaggedBalances[acc.id] ?: 0.0) > 0.004
        }
    }

    val accountSections: List<AccountGroupSection> by lazy {
        val activeByGroup = activeAccounts.groupBy { it.groupId }
        val tags = allTaggings
        activeGroups.map { group ->
            val items = (activeByGroup[group.id] ?: emptyList()).map { acc ->
                val tMap = accountTagBalances[acc.id].orEmpty()
                val uBal = accountUntaggedBalances[acc.id] ?: acc.initialBalance
                AccountWithBalance(
                    account = acc,
                    balance = accountBalances[acc.id] ?: acc.initialBalance,
                    tagBalances = tMap,
                    untaggedBalance = uBal
                )
            }
            val groupTagSubs = tags.associateWith { tag ->
                items.sumOf { it.tagBalances[tag] ?: 0.0 }
            }
            AccountGroupSection(
                group = group,
                accounts = items,
                subtotal = items.sumOf { it.balance },
                tagSubtotals = groupTagSubs,
                untaggedSubtotal = items.sumOf { it.untaggedBalance }
            )
        }
    }

    val grandTagTotals: Map<String, Double> by lazy {
        allTaggings.associateWith { tag ->
            activeAccounts.sumOf { acc -> accountTagBalances[acc.id]?.get(tag) ?: 0.0 }
        }
    }

    val grandUntaggedTotal: Double by lazy {
        activeAccounts.sumOf { acc -> accountUntaggedBalances[acc.id] ?: acc.initialBalance }
    }

    val totalAssets: Double by lazy {
        activeAccounts.sumOf { acc ->
            val bal = accountBalances[acc.id] ?: acc.initialBalance
            if (bal > 0.0) bal else 0.0
        }
    }

    val totalLiabilities: Double by lazy {
        activeAccounts.sumOf { acc ->
            val bal = accountBalances[acc.id] ?: acc.initialBalance
            if (bal < 0.0) abs(bal) else 0.0
        }
    }

    val netTotal: Double
        get() = totalAssets - totalLiabilities

    fun accountName(accountId: String?): String =
        accounts.firstOrNull { it.id == accountId }?.name ?: "Account"

    fun categoryById(categoryId: String?): Category? =
        categories.firstOrNull { it.id == categoryId }
}

data class FilterState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val subTab: TransactionsSubTab = TransactionsSubTab.DAILY,
    val searchQuery: String = "",
    val favoritesOnly: Boolean = false,
    val selectedTagFilter: String? = null,
    val statsPeriodType: StatsPeriodType = StatsPeriodType.MONTHLY,
    val statsIsExpenseTab: Boolean = true,
    val statsWeekOffset: Int = 0,
    val customStartEpochDay: Long = LedgerFormatters.todayEpochDay() - 30L,
    val customEndEpochDay: Long = LedgerFormatters.todayEpochDay()
)

class LedgerViewModel(
    private val repository: LedgerDataSource,
    private val currentUserId: String
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var appliedInitialStartScreen = false

    init {
        viewModelScope.launch {
            repository.ensureInitialized(currentUserId)
                .onFailure { err ->
                    Log.w(TAG, "Initial seed skipped or failed", err)
                }
        }
    }

    private val baseStream = combine(
        repository.observeSettings(currentUserId),
        repository.observeAccountGroups(currentUserId),
        repository.observeAccounts(currentUserId),
        repository.observeCategories(currentUserId),
        repository.observeTransactions(currentUserId)
    ) { settings, groups, accounts, categories, transactions ->
        if (!appliedInitialStartScreen && settings.seeded) {
            appliedInitialStartScreen = true
            if (settings.startScreen == "CALENDAR") {
                _filterState.value = _filterState.value.copy(subTab = TransactionsSubTab.CALENDAR)
            }
        }
        LedgerDataBundle(
            settings = settings,
            accountGroups = groups,
            accounts = accounts,
            categories = categories,
            transactions = transactions
        )
    }

    val uiState: StateFlow<UiState<LedgerDataBundle>> = combine(
        baseStream,
        repository.observeBudgets(currentUserId),
        repository.observeNotes(currentUserId)
    ) { bundle, budgets, notes ->
        UiState.Success(bundle.copy(budgets = budgets, notes = notes)) as UiState<LedgerDataBundle>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing ledger streams", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load ledger data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // --- Navigation / Period Filter Actions ---

    fun previousMonth() {
        val f = _filterState.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, f.selectedYear)
            set(Calendar.MONTH, f.selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        _filterState.value = f.copy(
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH)
        )
    }

    fun nextMonth() {
        val f = _filterState.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, f.selectedYear)
            set(Calendar.MONTH, f.selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
        }
        _filterState.value = f.copy(
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH)
        )
    }

    fun selectMonth(year: Int, monthZeroBased: Int) {
        _filterState.value = _filterState.value.copy(
            selectedYear = year,
            selectedMonth = monthZeroBased
        )
    }

    fun previousStatsPeriod() {
        val f = _filterState.value
        when (f.statsPeriodType) {
            StatsPeriodType.MONTHLY -> previousMonth()
            StatsPeriodType.ANNUALLY -> _filterState.value = f.copy(selectedYear = f.selectedYear - 1)
            StatsPeriodType.WEEKLY -> _filterState.value = f.copy(statsWeekOffset = f.statsWeekOffset - 1)
            StatsPeriodType.PERIOD -> {
                val span = (f.customEndEpochDay - f.customStartEpochDay).coerceAtLeast(1L) + 1L
                _filterState.value = f.copy(
                    customStartEpochDay = f.customStartEpochDay - span,
                    customEndEpochDay = f.customEndEpochDay - span
                )
            }
        }
    }

    fun nextStatsPeriod() {
        val f = _filterState.value
        when (f.statsPeriodType) {
            StatsPeriodType.MONTHLY -> nextMonth()
            StatsPeriodType.ANNUALLY -> _filterState.value = f.copy(selectedYear = f.selectedYear + 1)
            StatsPeriodType.WEEKLY -> _filterState.value = f.copy(statsWeekOffset = f.statsWeekOffset + 1)
            StatsPeriodType.PERIOD -> {
                val span = (f.customEndEpochDay - f.customStartEpochDay).coerceAtLeast(1L) + 1L
                _filterState.value = f.copy(
                    customStartEpochDay = f.customStartEpochDay + span,
                    customEndEpochDay = f.customEndEpochDay + span
                )
            }
        }
    }

    fun setSubTab(tab: TransactionsSubTab) {
        _filterState.value = _filterState.value.copy(subTab = tab)
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun toggleFavoritesOnly() {
        _filterState.value = _filterState.value.copy(favoritesOnly = !_filterState.value.favoritesOnly)
    }

    fun setTagFilter(tag: String?) {
        _filterState.value = _filterState.value.copy(selectedTagFilter = tag)
    }

    fun setStatsPeriodType(type: StatsPeriodType) {
        _filterState.value = _filterState.value.copy(statsPeriodType = type)
    }

    fun setStatsIsExpenseTab(isExpense: Boolean) {
        _filterState.value = _filterState.value.copy(statsIsExpenseTab = isExpense)
    }

    fun setCustomPeriodRange(startEpochDay: Long, endEpochDay: Long) {
        val s = minOf(startEpochDay, endEpochDay)
        val e = maxOf(startEpochDay, endEpochDay)
        _filterState.value = _filterState.value.copy(
            statsPeriodType = StatsPeriodType.PERIOD,
            customStartEpochDay = s,
            customEndEpochDay = e
        )
    }

    // --- Derived Computations ---

    fun transactionsForSelectedMonth(bundle: LedgerDataBundle, filter: FilterState): List<LedgerTransaction> {
        val query = filter.searchQuery.trim().lowercase()
        return bundle.transactions.filter { tx ->
            val cal = LedgerFormatters.epochDayToCalendar(tx.epochDay)
            val sameMonth = cal.get(Calendar.YEAR) == filter.selectedYear &&
                cal.get(Calendar.MONTH) == filter.selectedMonth
            if (!sameMonth) return@filter false
            if (filter.favoritesOnly && !tx.favorite) return@filter false
            val txTagging = tx.effectiveTagging()
            if (filter.selectedTagFilter != null && !txTagging.equals(filter.selectedTagFilter, ignoreCase = true)) {
                return@filter false
            }
            if (query.isNotEmpty()) {
                val catName = bundle.categoryById(tx.categoryId)?.name.orEmpty().lowercase()
                val accName = bundle.accountName(tx.accountId).lowercase()
                val destName = tx.toAccountId?.let { bundle.accountName(it).lowercase() }.orEmpty()
                val match = tx.note.lowercase().contains(query) ||
                    txTagging.orEmpty().lowercase().contains(query) ||
                    tx.description.orEmpty().lowercase().contains(query) ||
                    tx.subcategory.orEmpty().lowercase().contains(query) ||
                    catName.contains(query) ||
                    accName.contains(query) ||
                    destName.contains(query)
                if (!match) return@filter false
            }
            true
        }
    }

    fun groupByDay(txs: List<LedgerTransaction>): List<DayTransactionGroup> {
        return txs.groupBy { it.epochDay }
            .entries
            .sortedByDescending { it.key }
            .map { (day, list) ->
                val dayIncome = list.filter { it.txType() == TransactionType.INCOME }.sumOf { it.amount }
                val dayExpense = list.filter { it.txType() == TransactionType.EXPENSE }.sumOf { it.amount }
                DayTransactionGroup(day, list, dayIncome, dayExpense)
            }
    }

    fun monthlySummariesForYear(bundle: LedgerDataBundle, year: Int): List<MonthlySummaryRow> {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val yearTxs = bundle.transactions.filter {
            LedgerFormatters.epochDayToCalendar(it.epochDay).get(Calendar.YEAR) == year
        }
        return (11 downTo 0).map { m ->
            val mTxs = yearTxs.filter {
                LedgerFormatters.epochDayToCalendar(it.epochDay).get(Calendar.MONTH) == m
            }
            val inc = mTxs.filter { it.txType() == TransactionType.INCOME }.sumOf { it.amount }
            val exp = mTxs.filter { it.txType() == TransactionType.EXPENSE }.sumOf { it.amount }
            MonthlySummaryRow(
                monthZeroBased = m,
                monthLabel = "${monthNames[m]} $year",
                income = inc,
                expense = exp,
                net = inc - exp
            )
        }
    }

    fun statsDateRange(filter: FilterState, settings: UserSettings): Pair<Long, Long> {
        return when (filter.statsPeriodType) {
            StatsPeriodType.MONTHLY -> {
                val start = LedgerFormatters.calendarToEpochDay(filter.selectedYear, filter.selectedMonth, 1)
                val lastDay = LedgerFormatters.daysInMonth(filter.selectedYear, filter.selectedMonth)
                val end = LedgerFormatters.calendarToEpochDay(filter.selectedYear, filter.selectedMonth, lastDay)
                start to end
            }
            StatsPeriodType.ANNUALLY -> {
                val start = LedgerFormatters.calendarToEpochDay(filter.selectedYear, Calendar.JANUARY, 1)
                val end = LedgerFormatters.calendarToEpochDay(filter.selectedYear, Calendar.DECEMBER, 31)
                start to end
            }
            StatsPeriodType.WEEKLY -> {
                val today = LedgerFormatters.todayEpochDay() + (filter.statsWeekOffset * 7L)
                val dow = LedgerFormatters.dayOfWeekIndex(today) // Sun=1..Sat=7
                val diffToStart = if (settings.weeklyStartDay == "SUNDAY") {
                    (dow - Calendar.SUNDAY).toLong()
                } else {
                    ((dow - Calendar.MONDAY + 7) % 7).toLong()
                }
                val start = today - diffToStart
                start to (start + 6L)
            }
            StatsPeriodType.PERIOD -> {
                filter.customStartEpochDay to filter.customEndEpochDay
            }
        }
    }

    fun statsPeriodHeaderLabel(filter: FilterState, settings: UserSettings): String {
        return when (filter.statsPeriodType) {
            StatsPeriodType.MONTHLY -> LedgerFormatters.formatMonthYear(filter.selectedYear, filter.selectedMonth)
            StatsPeriodType.ANNUALLY -> filter.selectedYear.toString()
            StatsPeriodType.WEEKLY, StatsPeriodType.PERIOD -> {
                val (s, e) = statsDateRange(filter, settings)
                LedgerFormatters.weekRangeLabel(s, e)
            }
        }
    }

    fun computeStats(
        bundle: LedgerDataBundle,
        filter: FilterState
    ): Triple<Double, Double, List<CategoryStatSlice>> {
        val (startDay, endDay) = statsDateRange(filter, bundle.settings)
        val periodTxs = bundle.transactions.filter { it.epochDay in startDay..endDay }
        val totalIncome = periodTxs.filter { it.txType() == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = periodTxs.filter { it.txType() == TransactionType.EXPENSE }.sumOf { it.amount }

        val targetType = if (filter.statsIsExpenseTab) TransactionType.EXPENSE else TransactionType.INCOME
        val relevant = periodTxs.filter { it.txType() == targetType }
        val grandTotal = relevant.sumOf { it.amount }

        val palette = com.example.ui.theme.ChartColorHexes
        val slices = relevant.groupBy { it.categoryId ?: "uncategorized" }
            .entries
            .map { (catId, list) ->
                val cat = bundle.categoryById(catId)
                val sum = list.sumOf { it.amount }
                val pct = if (grandTotal > 0.0) (sum / grandTotal) * 100.0 else 0.0
                CategoryStatSlice(
                    categoryId = catId,
                    categoryName = cat?.name ?: "Uncategorized",
                    colorHex = cat?.colorHex ?: palette[0],
                    amount = sum,
                    percentage = pct,
                    transactions = list
                )
            }
            .sortedByDescending { it.amount }
            .mapIndexed { idx, slice ->
                // Ensure distinct slice colors when categories share defaults
                slice.copy(colorHex = palette[idx % palette.size])
            }

        return Triple(totalIncome, totalExpense, slices)
    }

    fun computeNetWorthSeries(
        bundle: LedgerDataBundle,
        mode: ReportPeriodMode,
        anchorYear: Int
    ): List<NetWorthPeriodPoint> {
        val includedAccounts = bundle.netWorthIncludedAccounts
        val includedIds = includedAccounts.map { it.id }.toSet()
        val runningBalances = includedAccounts.associate { it.id to it.initialBalance }.toMutableMap()
        val sortedTxs = bundle.transactions.sortedBy { it.epochDay }

        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val periods: List<Triple<String, Pair<String, String>, Long>> = when (mode) {
            ReportPeriodMode.MONTHLY -> (0..11).map { m ->
                val lastDay = LedgerFormatters.daysInMonth(anchorYear, m)
                val cutoff = LedgerFormatters.calendarToEpochDay(anchorYear, m, lastDay)
                Triple(
                    "$anchorYear-$m",
                    monthNames[m] to "${monthNames[m]} $anchorYear",
                    cutoff
                )
            }
            ReportPeriodMode.YEARLY -> ((anchorYear - 5)..anchorYear).map { y ->
                val cutoff = LedgerFormatters.calendarToEpochDay(y, 11, 31)
                Triple(
                    "$y",
                    "$y" to "Year $y",
                    cutoff
                )
            }
        }

        var txIdx = 0
        return periods.map { (key, labels, cutoff) ->
            while (txIdx < sortedTxs.size && sortedTxs[txIdx].epochDay <= cutoff) {
                val tx = sortedTxs[txIdx]
                when (tx.txType()) {
                    TransactionType.INCOME -> {
                        if (tx.accountId in includedIds) {
                            runningBalances[tx.accountId] = (runningBalances[tx.accountId] ?: 0.0) + tx.amount
                        }
                    }
                    TransactionType.EXPENSE -> {
                        if (tx.accountId in includedIds) {
                            runningBalances[tx.accountId] = (runningBalances[tx.accountId] ?: 0.0) - tx.amount
                        }
                    }
                    TransactionType.TRANSFER -> {
                        if (tx.accountId in includedIds) {
                            runningBalances[tx.accountId] = (runningBalances[tx.accountId] ?: 0.0) - tx.amount - tx.feeAmount
                        }
                        tx.toAccountId?.let { dest ->
                            if (dest in includedIds) {
                                runningBalances[dest] = (runningBalances[dest] ?: 0.0) + tx.amount
                            }
                        }
                    }
                }
                txIdx++
            }

            var assets = 0.0
            var liabilities = 0.0
            runningBalances.values.forEach { bal ->
                if (bal >= 0.0) {
                    assets += bal
                } else {
                    liabilities += abs(bal)
                }
            }
            NetWorthPeriodPoint(
                key = key,
                shortLabel = labels.first,
                fullLabel = labels.second,
                cutoffEpochDay = cutoff,
                assets = assets,
                liabilities = liabilities,
                netWorth = assets - liabilities
            )
        }
    }

    fun computeNetWorthAccountSnapshots(
        bundle: LedgerDataBundle,
        cutoffEpochDay: Long
    ): List<NetWorthAccountSnapshot> {
        val includedAccounts = bundle.netWorthIncludedAccounts
        val includedIds = includedAccounts.map { it.id }.toSet()
        val balances = includedAccounts.associate { it.id to it.initialBalance }.toMutableMap()
        val groupNames = bundle.accountGroups.associate { it.id to it.name }

        bundle.transactions.forEach { tx ->
            if (tx.epochDay > cutoffEpochDay) return@forEach
            when (tx.txType()) {
                TransactionType.INCOME -> {
                    if (tx.accountId in includedIds) {
                        balances[tx.accountId] = (balances[tx.accountId] ?: 0.0) + tx.amount
                    }
                }
                TransactionType.EXPENSE -> {
                    if (tx.accountId in includedIds) {
                        balances[tx.accountId] = (balances[tx.accountId] ?: 0.0) - tx.amount
                    }
                }
                TransactionType.TRANSFER -> {
                    if (tx.accountId in includedIds) {
                        balances[tx.accountId] = (balances[tx.accountId] ?: 0.0) - tx.amount - tx.feeAmount
                    }
                    tx.toAccountId?.let { dest ->
                        if (dest in includedIds) {
                            balances[dest] = (balances[dest] ?: 0.0) + tx.amount
                        }
                    }
                }
            }
        }

        return includedAccounts.map { acc ->
            NetWorthAccountSnapshot(
                account = acc,
                groupName = groupNames[acc.groupId] ?: "Account",
                balance = balances[acc.id] ?: acc.initialBalance
            )
        }.sortedByDescending { abs(it.balance) }
    }

    fun buildCsvExport(bundle: LedgerDataBundle): String {
        return LedgerExcelExporter.buildDoubleEntryCsv(bundle)
    }

    // --- Mutations ---

    fun saveTransaction(
        existingId: String?,
        tx: LedgerTransaction,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveTransaction(currentUserId, existingId, tx)
            result.onSuccess {
                _toastMessage.tryEmit(if (existingId == null) "Transaction saved" else "Transaction updated")
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Could not save transaction")
                onComplete(false)
            }
        }
    }

    fun copyTransaction(
        tx: LedgerTransaction,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveTransaction(currentUserId, null, tx.copy(id = ""))
            result.onSuccess {
                _toastMessage.tryEmit("Entry copied to ${LedgerFormatters.formatShortDate(tx.epochDay)}")
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Could not copy transaction")
                onComplete(false)
            }
        }
    }

    fun deleteTransaction(txId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(currentUserId, txId)
            result.onSuccess {
                _toastMessage.tryEmit("Transaction deleted")
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.localizedMessage ?: "Could not delete transaction")
                onComplete(false)
            }
        }
    }

    fun saveAccountGroup(
        existingId: String?,
        name: String,
        groupType: AccountNature,
        sortOrder: Long
    ) {
        viewModelScope.launch {
            repository.saveAccountGroup(currentUserId, existingId, name, groupType, sortOrder)
                .onSuccess { _toastMessage.tryEmit("Account group saved") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save group") }
        }
    }

    fun setAccountGroupArchived(groupId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.setAccountGroupArchived(currentUserId, groupId, archived)
                .onSuccess { _toastMessage.tryEmit(if (archived) "Group archived" else "Group restored") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to update group") }
        }
    }

    fun saveAccount(
        existingId: String?,
        groupId: String,
        name: String,
        initialBalance: Double,
        accountType: AccountNature,
        note: String?,
        sortOrder: Long,
        includeInNetWorth: Boolean = true
    ) {
        viewModelScope.launch {
            repository.saveAccount(
                userId = currentUserId,
                existingId = existingId,
                groupId = groupId,
                name = name,
                initialBalance = initialBalance,
                accountType = accountType,
                note = note,
                sortOrder = sortOrder,
                includeInNetWorth = includeInNetWorth
            )
                .onSuccess { _toastMessage.tryEmit("Account saved") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save account") }
        }
    }

    fun setAccountArchived(accountId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.setAccountArchived(currentUserId, accountId, archived)
                .onSuccess { _toastMessage.tryEmit(if (archived) "Account moved to deleted" else "Account restored") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to update account") }
        }
    }

    fun deleteAccountPermanently(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccount(currentUserId, accountId)
                .onSuccess { _toastMessage.tryEmit("Account permanently deleted") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to delete account") }
        }
    }

    fun saveCategory(
        existingId: String?,
        name: String,
        type: CategoryType,
        colorHex: String,
        subcategories: List<String>,
        sortOrder: Long
    ) {
        viewModelScope.launch {
            repository.saveCategory(currentUserId, existingId, name, type, colorHex, subcategories, sortOrder)
                .onSuccess { _toastMessage.tryEmit("Category saved") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save category") }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(currentUserId, categoryId)
                .onSuccess { _toastMessage.tryEmit("Category deleted") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to delete category") }
        }
    }

    fun saveBudget(categoryId: String, monthlyLimit: Double) {
        viewModelScope.launch {
            repository.saveBudget(currentUserId, categoryId, monthlyLimit)
                .onSuccess { _toastMessage.tryEmit("Budget updated") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save budget") }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudget(currentUserId, budgetId)
                .onSuccess { _toastMessage.tryEmit("Budget removed") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to remove budget") }
        }
    }

    fun saveNote(existingId: String?, title: String, content: String, pinned: Boolean) {
        viewModelScope.launch {
            repository.saveNote(currentUserId, existingId, title, content, pinned)
                .onSuccess { _toastMessage.tryEmit("Note saved") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save note") }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(currentUserId, noteId)
                .onSuccess { _toastMessage.tryEmit("Note deleted") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to delete note") }
        }
    }

    fun updateSettings(updated: UserSettings) {
        viewModelScope.launch {
            repository.updateSettings(
                userId = currentUserId,
                mainCurrencyCode = updated.mainCurrencyCode,
                mainCurrencySymbol = updated.mainCurrencySymbol,
                subcategoryEnabled = updated.subcategoryEnabled,
                startScreen = updated.startScreen,
                monthlyStartDate = updated.monthlyStartDate,
                weeklyStartDay = updated.weeklyStartDay
            ).onSuccess {
                _toastMessage.tryEmit("Settings saved")
            }.onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "Failed to update settings")
            }
        }
    }

    private companion object {
        const val TAG = "LedgerViewModel"
    }
}
