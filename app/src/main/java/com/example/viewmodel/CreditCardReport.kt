package com.example.viewmodel

import com.example.data.Account
import com.example.data.AccountNature
import com.example.data.ReportPeriodMode
import com.example.data.TransactionType
import com.example.util.LedgerFormatters

/** Spending on one card in one category during the selected period. */
data class CreditCardCategorySpend(
    val categoryName: String,
    val colorHex: String,
    val amount: Double
)

/** One credit card's activity in the selected period plus its balance at the end of it. */
data class CreditCardSummary(
    val account: Account,
    val groupName: String,
    /** Expenses charged to the card, plus transfers/cash withdrawals out of it (including fees). */
    val spent: Double,
    /** Bill payments (transfers into the card) plus refunds/cashback recorded as income on it. */
    val paid: Double,
    /** Amount owed at the end of the period. Negative means the card is in credit. */
    val outstanding: Double,
    val transactionCount: Int,
    val topCategories: List<CreditCardCategorySpend>
)

/** Totals across all credit cards for one month or year, used for the period table. */
data class CreditCardPeriodPoint(
    val key: String,
    val shortLabel: String,
    val fullLabel: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val spent: Double,
    val paid: Double,
    val outstanding: Double
)

data class CreditCardReport(
    val cards: List<Account>,
    val periods: List<CreditCardPeriodPoint>
)

object CreditCardReportCalculator {

    private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /**
     * A credit card is an active liability account whose group or name mentions a card
     * (the default "Credit Cards" group, or anything like "HDFC Card").
     */
    fun creditCardAccounts(bundle: LedgerDataBundle): List<Account> {
        val groupsById = bundle.accountGroups.associateBy { it.id }
        return bundle.activeAccounts.filter { acc ->
            val group = groupsById[acc.groupId]
            val isLiability = acc.nature() == AccountNature.LIABILITY ||
                group?.nature() == AccountNature.LIABILITY
            val mentionsCard = group?.name.orEmpty().contains("card", ignoreCase = true) ||
                acc.name.contains("card", ignoreCase = true)
            isLiability && mentionsCard
        }.sortedWith(compareBy({ groupsById[it.groupId]?.sortOrder ?: 0L }, { it.sortOrder }, { it.name }))
    }

    fun computeSeries(
        bundle: LedgerDataBundle,
        mode: ReportPeriodMode,
        anchorYear: Int
    ): CreditCardReport {
        val cards = creditCardAccounts(bundle)
        val periods = periodRanges(mode, anchorYear).map { (key, labels, range) ->
            val summaries = summarize(bundle, cards, range.first, range.last)
            CreditCardPeriodPoint(
                key = key,
                shortLabel = labels.first,
                fullLabel = labels.second,
                startEpochDay = range.first,
                endEpochDay = range.last,
                spent = summaries.sumOf { it.spent },
                paid = summaries.sumOf { it.paid },
                outstanding = summaries.sumOf { it.outstanding }
            )
        }
        return CreditCardReport(cards, periods)
    }

    fun summarize(
        bundle: LedgerDataBundle,
        cards: List<Account>,
        startEpochDay: Long,
        endEpochDay: Long
    ): List<CreditCardSummary> {
        if (cards.isEmpty()) return emptyList()
        val groupNames = bundle.accountGroups.associate { it.id to it.name }
        val categoriesById = bundle.categories.associateBy { it.id }

        return cards.map { card ->
            // Ledger convention: liabilities carry negative balances, expenses subtract.
            var balance = card.initialBalance
            var spent = 0.0
            var paid = 0.0
            var count = 0
            val byCategory = mutableMapOf<String, Double>()

            bundle.transactions.forEach { tx ->
                if (tx.epochDay > endEpochDay) return@forEach
                val inPeriod = tx.epochDay >= startEpochDay
                val touchesCard = tx.accountId == card.id || tx.toAccountId == card.id
                if (!touchesCard) return@forEach
                if (inPeriod) count++

                when (tx.txType()) {
                    TransactionType.EXPENSE -> if (tx.accountId == card.id) {
                        balance -= tx.amount
                        if (inPeriod) {
                            spent += tx.amount
                            val catKey = tx.categoryId ?: ""
                            byCategory[catKey] = (byCategory[catKey] ?: 0.0) + tx.amount
                        }
                    }
                    TransactionType.INCOME -> if (tx.accountId == card.id) {
                        balance += tx.amount
                        if (inPeriod) paid += tx.amount
                    }
                    TransactionType.TRANSFER -> {
                        if (tx.accountId == card.id) {
                            val out = tx.amount + tx.feeAmount
                            balance -= out
                            if (inPeriod) spent += out
                        }
                        if (tx.toAccountId == card.id) {
                            balance += tx.amount
                            if (inPeriod) paid += tx.amount
                        }
                    }
                }
            }

            val topCategories = byCategory.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { (catId, amount) ->
                    val cat = categoriesById[catId]
                    CreditCardCategorySpend(
                        categoryName = cat?.name ?: "Uncategorized",
                        colorHex = cat?.colorHex ?: "#8A8F98",
                        amount = amount
                    )
                }

            CreditCardSummary(
                account = card,
                groupName = groupNames[card.groupId] ?: "Credit Cards",
                spent = spent,
                paid = paid,
                outstanding = -balance,
                transactionCount = count,
                topCategories = topCategories
            )
        }
    }

    private fun periodRanges(
        mode: ReportPeriodMode,
        anchorYear: Int
    ): List<Triple<String, Pair<String, String>, LongRange>> = when (mode) {
        ReportPeriodMode.MONTHLY -> (0..11).map { m ->
            val start = LedgerFormatters.calendarToEpochDay(anchorYear, m, 1)
            val end = LedgerFormatters.calendarToEpochDay(anchorYear, m, LedgerFormatters.daysInMonth(anchorYear, m))
            Triple("$anchorYear-$m", monthNames[m] to "${monthNames[m]} $anchorYear", start..end)
        }
        ReportPeriodMode.YEARLY -> ((anchorYear - 5)..anchorYear).map { y ->
            val start = LedgerFormatters.calendarToEpochDay(y, 0, 1)
            val end = LedgerFormatters.calendarToEpochDay(y, 11, 31)
            Triple("$y", "$y" to "Year $y", start..end)
        }
    }
}
