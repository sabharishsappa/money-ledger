package com.example

import com.example.data.Account
import com.example.data.AccountGroup
import com.example.data.AccountNature
import com.example.data.Category
import com.example.data.LedgerTransaction
import com.example.data.ReportPeriodMode
import com.example.data.TransactionType
import com.example.util.LedgerFormatters
import com.example.viewmodel.CreditCardReportCalculator
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.NetWorthPeriodPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class CreditCardReportTest {

    private val cardsGroup = AccountGroup(id = "grp_cards", name = "Credit Cards", groupType = AccountNature.LIABILITY.name)
    private val bankGroup = AccountGroup(id = "grp_bank", name = "Bank Accounts", groupType = AccountNature.ASSET.name)
    private val loanGroup = AccountGroup(id = "grp_loans", name = "Loans", groupType = AccountNature.LIABILITY.name)

    private val card = Account(id = "card", groupId = "grp_cards", name = "HDFC Card", accountType = AccountNature.LIABILITY.name, initialBalance = -1000.0)
    private val bank = Account(id = "bank", groupId = "grp_bank", name = "Savings", accountType = AccountNature.ASSET.name, initialBalance = 50000.0)
    private val loan = Account(id = "loan", groupId = "grp_loans", name = "Car Loan", accountType = AccountNature.LIABILITY.name)

    private val food = Category(id = "food", name = "Food", colorHex = "#F06458")

    private fun day(month: Int, d: Int) = LedgerFormatters.calendarToEpochDay(2026, month, d)

    private fun tx(id: String, type: TransactionType, amount: Double, epochDay: Long, from: String, to: String? = null, cat: String? = null, fee: Double = 0.0) =
        LedgerTransaction(id = id, type = type.name, amount = amount, feeAmount = fee, epochDay = epochDay, accountId = from, toAccountId = to, categoryId = cat)

    private val bundle = LedgerDataBundle(
        accountGroups = listOf(cardsGroup, bankGroup, loanGroup),
        accounts = listOf(card, bank, loan),
        categories = listOf(food),
        transactions = listOf(
            tx("t1", TransactionType.EXPENSE, 500.0, day(0, 10), "card", cat = "food"),        // Jan spend
            tx("t2", TransactionType.EXPENSE, 300.0, day(1, 5), "card", cat = "food"),         // Feb spend
            tx("t3", TransactionType.TRANSFER, 1200.0, day(1, 20), "bank", to = "card"),       // Feb bill payment
            tx("t4", TransactionType.TRANSFER, 200.0, day(1, 22), "card", to = "bank", fee = 10.0), // Feb cash withdrawal
            tx("t5", TransactionType.EXPENSE, 999.0, day(1, 6), "bank", cat = "food")          // not on the card
        )
    )

    @Test
    fun onlyCardLiabilitiesAreTreatedAsCreditCards() {
        val cards = CreditCardReportCalculator.creditCardAccounts(bundle)
        assertEquals(listOf("card"), cards.map { it.id })
    }

    @Test
    fun februarySummaryCountsSpendPaymentsAndOutstanding() {
        val series = listOf(
            NetWorthPeriodPoint("2026-0", "Jan '26", "Jan 2026", day(0, 31), 0.0, 0.0, 0.0),
            NetWorthPeriodPoint("2026-1", "Feb '26", "Feb 2026", day(1, 28), 0.0, 0.0, 0.0)
        )
        val report = CreditCardReportCalculator.computeSeries(bundle, ReportPeriodMode.MONTHLY, series)
        val feb = report.periods[1]
        assertEquals(510.0, feb.spent, 0.001)   // 300 expense + 200 withdrawal + 10 fee
        assertEquals(1200.0, feb.paid, 0.001)
        // Opening owed 1000, +500 Jan, +300 +210 Feb, -1200 paid = 810
        assertEquals(810.0, feb.outstanding, 0.001)

        val jan = report.periods[0]
        assertEquals(500.0, jan.spent, 0.001)
        assertEquals(1500.0, jan.outstanding, 0.001)

        val summary = CreditCardReportCalculator.summarize(bundle, report.cards, feb.startEpochDay, feb.endEpochDay).single()
        assertEquals(3, summary.transactionCount)
        assertEquals("Food", summary.topCategories.single().categoryName)
        assertEquals(300.0, summary.topCategories.single().amount, 0.001)
    }
}
