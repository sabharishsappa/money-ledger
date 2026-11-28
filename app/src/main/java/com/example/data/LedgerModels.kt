package com.example.data

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER;

    companion object {
        fun safeValueOf(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class AccountNature {
    ASSET,
    LIABILITY;

    companion object {
        fun safeValueOf(raw: String?): AccountNature =
            entries.firstOrNull { it.name == raw } ?: ASSET
    }
}

enum class CategoryType {
    INCOME,
    EXPENSE;

    companion object {
        fun safeValueOf(raw: String?): CategoryType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class CustomRepeatUnit(
    val label: String,
    val singular: String,
    val compactPlural: String,
    val compactSingular: String
) {
    DAYS("Days", "Day", "Days", "Day"),
    WEEKS("Weeks", "Week", "Wks", "Wk"),
    MONTHS("Months", "Month", "Mos", "Mo"),
    YEARS("Years", "Year", "Yrs", "Yr");

    companion object {
        fun safeValueOf(raw: String?): CustomRepeatUnit =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MONTHS
    }
}

enum class RepeatFrequency(
    val label: String,
    val description: String,
    val compactLabel: String = label
) {
    NONE("None", "One-time entry", "Rep/Inst."),
    DAILY("Daily", "Repeats every day", "Daily"),
    WEEKDAYS("Weekdays", "Monday to Friday", "Weekdays"),
    WEEKLY("Weekly", "Repeats every week", "Weekly"),
    BIWEEKLY("Every 2 Weeks", "Bi-weekly / Fortnightly", "2 Weeks"),
    MONTHLY("Monthly", "Same date each month", "Monthly"),
    END_OF_MONTH("End of Month", "Last day of every month", "End of Mo."),
    SPECIFIC_DATE_OF_MONTH("Specific Date of Month", "Choose day 1–31 of each month", "Date / Mo."),
    QUARTERLY("Quarterly", "Every 3 months", "Quarterly"),
    HALF_YEARLY("Half-Yearly", "Every 6 months", "Half-Yr"),
    YEARLY("Yearly", "Repeats once a year", "Yearly"),
    CUSTOM("Custom", "Choose number & unit (Days, Weeks, Months, Years)", "Custom");

    companion object {
        fun safeValueOf(raw: String?): RepeatFrequency {
            if (raw.isNullOrBlank()) return NONE
            val base = raw.substringBefore(':')
            return entries.firstOrNull { it.name == base } ?: NONE
        }

        fun parseDayOfMonth(raw: String?): Int? {
            if (raw.isNullOrBlank() || !raw.startsWith("${SPECIFIC_DATE_OF_MONTH.name}:")) return null
            return raw.substringAfter(':').toIntOrNull()?.coerceIn(1, 31)
        }

        fun parseCustomRepeat(raw: String?): Pair<Int, CustomRepeatUnit>? {
            if (raw.isNullOrBlank() || !raw.startsWith("${CUSTOM.name}:")) return null
            val parts = raw.split(':')
            if (parts.size < 3) return null
            val count = parts[1].toIntOrNull()?.coerceIn(1, 365) ?: return null
            val unit = CustomRepeatUnit.safeValueOf(parts[2])
            return count to unit
        }

        fun encode(
            freq: RepeatFrequency,
            dayOfMonth: Int? = null,
            customCount: Int? = null,
            customUnit: CustomRepeatUnit? = null
        ): String {
            return when (freq) {
                SPECIFIC_DATE_OF_MONTH -> {
                    val day = (dayOfMonth ?: 1).coerceIn(1, 31)
                    "${freq.name}:$day"
                }
                CUSTOM -> {
                    val count = (customCount ?: 1).coerceIn(1, 365)
                    val unit = customUnit ?: CustomRepeatUnit.MONTHS
                    "${freq.name}:$count:${unit.name}"
                }
                else -> freq.name
            }
        }

        fun normalize(raw: String?): String {
            val freq = safeValueOf(raw)
            val custom = parseCustomRepeat(raw)
            return encode(
                freq = freq,
                dayOfMonth = parseDayOfMonth(raw),
                customCount = custom?.first,
                customUnit = custom?.second
            )
        }

        fun ordinalDay(day: Int): String {
            val d = day.coerceIn(1, 31)
            val suffix = when {
                d in 11..13 -> "th"
                d % 10 == 1 -> "st"
                d % 10 == 2 -> "nd"
                d % 10 == 3 -> "rd"
                else -> "th"
            }
            return "$d$suffix"
        }

        fun formatDisplayLabel(raw: String?): String {
            val freq = safeValueOf(raw)
            return when (freq) {
                SPECIFIC_DATE_OF_MONTH -> {
                    val day = parseDayOfMonth(raw) ?: 1
                    "Every ${ordinalDay(day)} of Month"
                }
                CUSTOM -> {
                    val (count, unit) = parseCustomRepeat(raw) ?: (1 to CustomRepeatUnit.MONTHS)
                    val unitWord = if (count == 1) unit.singular else unit.label
                    "Every $count $unitWord"
                }
                else -> freq.label
            }
        }

        fun formatCompactLabel(raw: String?): String {
            val freq = safeValueOf(raw)
            return when (freq) {
                SPECIFIC_DATE_OF_MONTH -> {
                    val day = parseDayOfMonth(raw) ?: 1
                    "${ordinalDay(day)} / Mo."
                }
                CUSTOM -> {
                    val (count, unit) = parseCustomRepeat(raw) ?: (1 to CustomRepeatUnit.MONTHS)
                    val unitShort = if (count == 1) unit.compactSingular else unit.compactPlural
                    "$count $unitShort"
                }
                else -> freq.compactLabel
            }
        }
    }
}

enum class MainTab(val label: String) {
    TRANSACTIONS("Trans."),
    ACCOUNTS("Accounts"),
    REPORTS("Reports"),
    MORE("More")
}

enum class ReportPeriodMode(val label: String) {
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class TransactionsSubTab(val label: String) {
    DAILY("Daily"),
    CALENDAR("Calendar"),
    MONTHLY("Monthly"),
    TOTAL("Total"),
    NOTE("Note")
}

enum class StatsPeriodType(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ANNUALLY("Annually"),
    PERIOD("Period")
}

@Keep
@Immutable
@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mainCurrencyCode: String = "INR",
    val mainCurrencySymbol: String = "₹",
    val subcategoryEnabled: Boolean = true,
    val startScreen: String = "DAILY",
    val monthlyStartDate: Long = 1L,
    val weeklyStartDay: String = "MONDAY",
    val seeded: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "mainCurrencyCode" to mainCurrencyCode,
        "mainCurrencySymbol" to mainCurrencySymbol,
        "subcategoryEnabled" to subcategoryEnabled,
        "startScreen" to startScreen,
        "monthlyStartDate" to monthlyStartDate,
        "weeklyStartDay" to weeklyStartDay,
        "seeded" to seeded,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@Immutable
@IgnoreExtraProperties
data class MainAccountGroup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val nature: String = AccountNature.ASSET.name,
    val sortOrder: Long = 0L,
    val archived: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun accountNature(): AccountNature = AccountNature.safeValueOf(nature)

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(60),
        "nature" to AccountNature.safeValueOf(nature).name,
        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
        "archived" to archived,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        const val DEFAULT_ASSETS_ID = "mg_assets"
        const val DEFAULT_LIABILITIES_ID = "mg_liabilities"

        fun inferNatureFromName(name: String): AccountNature {
            val lower = name.trim().lowercase()
            val isLiability = listOf(
                "liab", "loan", "debt", "credit", "payable", "borrow", "mortgage", "due"
            ).any { lower.contains(it) }
            return if (isLiability) AccountNature.LIABILITY else AccountNature.ASSET
        }
    }
}

@Keep
@Immutable
@IgnoreExtraProperties
data class AccountGroup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mainGroupId: String = "",
    val name: String = "",
    val groupType: String = AccountNature.ASSET.name,
    val sortOrder: Long = 0L,
    val archived: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun nature(): AccountNature = AccountNature.safeValueOf(groupType)

    fun resolvedMainGroupId(): String =
        mainGroupId.takeIf { it.isNotBlank() }
            ?: if (nature() == AccountNature.LIABILITY) {
                MainAccountGroup.DEFAULT_LIABILITIES_ID
            } else {
                MainAccountGroup.DEFAULT_ASSETS_ID
            }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "mainGroupId" to resolvedMainGroupId(),
        "name" to name.trim().take(60),
        "groupType" to AccountNature.safeValueOf(groupType).name,
        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
        "archived" to archived,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@Immutable
@IgnoreExtraProperties
data class Account(
    @DocumentId val id: String = "",
    val userId: String = "",
    val groupId: String = "",
    val name: String = "",
    val initialBalance: Double = 0.0,
    val accountType: String = AccountNature.ASSET.name,
    val note: String? = null,
    val sortOrder: Long = 0L,
    val archived: Boolean = false,
    val includeInNetWorth: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun nature(): AccountNature = AccountNature.safeValueOf(accountType)

    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "groupId" to groupId,
            "name" to name.trim().take(80),
            "initialBalance" to initialBalance,
            "accountType" to AccountNature.safeValueOf(accountType).name,
            "note" to note?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
            "sortOrder" to sortOrder.coerceIn(0L, 10000L),
            "archived" to archived,
            "includeInNetWorth" to includeInNetWorth,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@Immutable
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = CategoryType.EXPENSE.name,
    val colorHex: String = "#F06458",
    val subcategories: List<String> = emptyList(),
    val sortOrder: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun categoryType(): CategoryType = CategoryType.safeValueOf(type)

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(60),
        "type" to CategoryType.safeValueOf(type).name,
        "colorHex" to colorHex,
        "subcategories" to subcategories.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(30),
        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@Immutable
@IgnoreExtraProperties
data class LedgerTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val amount: Double = 0.0,
    val feeAmount: Double = 0.0,
    val epochDay: Long = 0L,
    val timestampMillis: Long = 0L,
    val accountId: String = "",
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val subcategory: String? = null,
    val note: String = "",
    val note2: String = "",
    val tagging: String? = null,
    val toTagging: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val repeatFrequency: String = RepeatFrequency.NONE.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun txType(): TransactionType = TransactionType.safeValueOf(type)
    fun repeat(): RepeatFrequency = RepeatFrequency.safeValueOf(repeatFrequency)
    fun repeatDayOfMonth(): Int? = RepeatFrequency.parseDayOfMonth(repeatFrequency)
    fun customRepeat(): Pair<Int, CustomRepeatUnit>? = RepeatFrequency.parseCustomRepeat(repeatFrequency)
    fun repeatLabel(): String = RepeatFrequency.formatDisplayLabel(repeatFrequency)
    fun normalizedRepeatFrequency(): String = RepeatFrequency.normalize(repeatFrequency)
    fun effectiveTagging(): String? =
        tagging?.trim()?.takeIf { it.isNotEmpty() }
            ?: tags.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    fun effectiveToTagging(): String? =
        toTagging?.trim()?.takeIf { it.isNotEmpty() }

    fun allEffectivePrimaryTaggings(): List<String> =
        listOfNotNull(effectiveTagging(), effectiveToTagging()).distinct()

    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "type" to TransactionType.safeValueOf(type).name,
            "amount" to amount,
            "feeAmount" to feeAmount.coerceAtLeast(0.0),
            "epochDay" to epochDay,
            "timestampMillis" to timestampMillis.coerceAtLeast(0L),
            "accountId" to accountId,
            "toAccountId" to toAccountId?.takeIf { it.isNotBlank() },
            "categoryId" to categoryId?.takeIf { it.isNotBlank() },
            "subcategory" to subcategory?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
            "note" to note.trim().take(140),
            "note2" to note2.trim().take(140),
            "tagging" to tagging?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
            "toTagging" to toTagging?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
            "description" to description?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "tags" to tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(10),
            "favorite" to favorite,
            "repeatFrequency" to normalizedRepeatFrequency(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@Immutable
@IgnoreExtraProperties
data class Budget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val subcategory: String? = null,
    val monthlyLimit: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun normalizedSubcategory(): String? = subcategory?.trim()?.takeIf { it.isNotEmpty() }
    fun isLedgerBudget(): Boolean = normalizedSubcategory() != null

    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "categoryId" to categoryId,
            "subcategory" to normalizedSubcategory()?.take(60),
            "monthlyLimit" to monthlyLimit.coerceAtLeast(0.0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    companion object {
        fun buildDocId(categoryId: String, subcategory: String? = null): String {
            val cleanSub = subcategory?.trim()?.takeIf { it.isNotEmpty() } ?: return "budget_$categoryId"
            val slug = cleanSub.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(40)
            val hash = cleanSub.lowercase().hashCode().toUInt().toString(16)
            return "budget_${categoryId}_${slug}_$hash"
        }
    }
}

@Keep
@Immutable
@IgnoreExtraProperties
data class FinanceNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val pinned: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.trim().take(100),
        "content" to content.trim().take(2000),
        "pinned" to pinned,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
