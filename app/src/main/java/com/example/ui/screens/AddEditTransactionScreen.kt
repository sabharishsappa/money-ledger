package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.data.Category
import com.example.data.CategoryType
import com.example.data.CustomRepeatUnit
import com.example.data.LedgerTransaction
import com.example.data.RepeatFrequency
import com.example.data.TransactionType
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
import com.example.util.IndianNumberVisualTransformation
import com.example.util.LedgerFormatters
import com.example.viewmodel.LedgerDataBundle
import kotlin.math.roundToLong

private enum class ActivePicker {
    NONE,
    ACCOUNT,
    TO_ACCOUNT,
    CATEGORY
}

@Immutable
private data class AccountSuggestionItem(
    val accountId: String,
    val accountName: String,
    val groupName: String,
    val balance: Double
)

@Immutable
private data class CategorySuggestionItem(
    val categoryId: String,
    val categoryName: String,
    val subcategory: String?,
    val colorHex: String,
    val displayLabel: String
)

@Immutable
private data class TextSuggestionItem(
    val text: String,
    val usageCount: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    bundle: LedgerDataBundle,
    existing: LedgerTransaction?,
    draft: LedgerTransaction? = null,
    draftKey: Long = 0L,
    prefillType: TransactionType? = null,
    prefillCategoryId: String? = null,
    prefillSubcategory: String? = null,
    prefillRepeatFrequency: String? = null,
    onBack: () -> Unit,
    onSave: (existingId: String?, tx: LedgerTransaction, keepOpen: Boolean) -> Unit,
    onCopy: ((LedgerTransaction) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    onManageAccounts: () -> Unit,
    onManageCategories: (CategoryType) -> Unit,
    onCreateCategoryLedger: ((categoryId: String, ledgerName: String, onCreated: (String, String) -> Unit) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val symbol = bundle.settings.mainCurrencySymbol
    val seedTx = existing ?: draft
    val isDraftCopy = existing == null && draft != null

    var txType by remember(existing, draft, draftKey, prefillType) {
        mutableStateOf(seedTx?.txType() ?: prefillType ?: TransactionType.EXPENSE)
    }
    var epochDay by remember(existing, draft, draftKey) {
        mutableStateOf(seedTx?.epochDay ?: LedgerFormatters.todayEpochDay())
    }
    var accountId by remember(existing, draft, draftKey) {
        mutableStateOf(seedTx?.accountId.orEmpty())
    }
    var toAccountId by remember(existing, draft, draftKey) {
        mutableStateOf(seedTx?.toAccountId.orEmpty())
    }
    var categoryId by remember(existing, draft, draftKey, prefillCategoryId) {
        mutableStateOf(seedTx?.categoryId ?: prefillCategoryId.orEmpty())
    }
    var subcategory by remember(existing, draft, draftKey, prefillSubcategory) {
        mutableStateOf(seedTx?.subcategory ?: prefillSubcategory.orEmpty())
    }

    // Type-ahead input queries synced with selected account / category
    var accountQuery by remember(existing, draft, draftKey) {
        val initialAccId = seedTx?.accountId.orEmpty()
        mutableStateOf(if (initialAccId.isNotBlank()) bundle.accountName(initialAccId) else "")
    }
    var toAccountQuery by remember(existing, draft, draftKey) {
        val initialToId = seedTx?.toAccountId.orEmpty()
        mutableStateOf(if (initialToId.isNotBlank()) bundle.accountName(initialToId) else "")
    }
    var categoryQuery by remember(existing, draft, draftKey, prefillCategoryId, prefillSubcategory) {
        val initialCatId = seedTx?.categoryId ?: prefillCategoryId.orEmpty()
        val initialSub = seedTx?.subcategory ?: prefillSubcategory.orEmpty()
        val catName = bundle.categoryById(initialCatId)?.name.orEmpty()
        val label = when {
            catName.isBlank() -> ""
            initialSub.isNotBlank() && (bundle.settings.subcategoryEnabled || !prefillSubcategory.isNullOrBlank() || isDraftCopy) -> "$catName / $initialSub"
            else -> catName
        }
        mutableStateOf(label)
    }

    var accountFieldFocused by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var toAccountFieldFocused by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var categoryFieldFocused by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var noteFieldFocused by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var note2FieldFocused by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var taggingFieldFocused by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var toTaggingFieldFocused by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showAccountSuggestions by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showToAccountSuggestions by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showCategorySuggestions by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showNoteSuggestions by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showNote2Suggestions by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showTaggingSuggestions by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showToTaggingSuggestions by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var hasTypedAccountQuery by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var hasTypedToAccountQuery by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var hasTypedCategoryQuery by remember(existing, draft, draftKey) { mutableStateOf(false) }

    var amountText by remember(existing, draft, draftKey) {
        val initial = seedTx?.amount?.let { amt ->
            if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
        }.orEmpty()
        mutableStateOf(initial)
    }
    var showFeeField by remember(existing, draft, draftKey) {
        mutableStateOf((seedTx?.feeAmount ?: 0.0) > 0.0)
    }
    var feeText by remember(existing, draft, draftKey) {
        val initialFee = seedTx?.feeAmount?.takeIf { it > 0.0 }?.let { fee ->
            if (fee % 1.0 == 0.0) fee.toLong().toString() else fee.toString()
        }.orEmpty()
        mutableStateOf(initialFee)
    }
    var noteText by remember(existing, draft, draftKey) { mutableStateOf(seedTx?.note.orEmpty()) }
    var note2Text by remember(existing, draft, draftKey) { mutableStateOf(seedTx?.note2.orEmpty()) }
    var taggingText by remember(existing, draft, draftKey) { mutableStateOf(seedTx?.effectiveTagging().orEmpty()) }
    var toTaggingText by remember(existing, draft, draftKey) { mutableStateOf(seedTx?.effectiveToTagging().orEmpty()) }
    var descriptionText by remember(existing, draft, draftKey) { mutableStateOf(seedTx?.description.orEmpty()) }
    var favorite by remember(existing, draft, draftKey) { mutableStateOf(seedTx?.favorite ?: false) }
    var repeatFreq by remember(existing, draft, draftKey, prefillRepeatFrequency) {
        val raw = seedTx?.repeatFrequency ?: prefillRepeatFrequency
        mutableStateOf(RepeatFrequency.safeValueOf(raw))
    }
    var repeatDayOfMonth by remember(existing, draft, draftKey, prefillRepeatFrequency) {
        val raw = seedTx?.repeatFrequency ?: prefillRepeatFrequency
        val defaultDay = RepeatFrequency.parseDayOfMonth(raw)
            ?: LedgerFormatters.epochDayToCalendar(seedTx?.epochDay ?: LedgerFormatters.todayEpochDay())
                .get(java.util.Calendar.DAY_OF_MONTH)
        mutableStateOf(defaultDay.coerceIn(1, 31))
    }
    var repeatCustomCount by remember(existing, draft, draftKey, prefillRepeatFrequency) {
        val raw = seedTx?.repeatFrequency ?: prefillRepeatFrequency
        mutableStateOf(RepeatFrequency.parseCustomRepeat(raw)?.first ?: 2)
    }
    var repeatCustomUnit by remember(existing, draft, draftKey, prefillRepeatFrequency) {
        val raw = seedTx?.repeatFrequency ?: prefillRepeatFrequency
        mutableStateOf(RepeatFrequency.parseCustomRepeat(raw)?.second ?: CustomRepeatUnit.WEEKS)
    }

    var activePicker by remember(existing, draft, draftKey) { mutableStateOf(ActivePicker.NONE) }
    var lastVisiblePicker by remember(existing, draft, draftKey) { mutableStateOf(ActivePicker.ACCOUNT) }
    if (activePicker != ActivePicker.NONE) {
        lastVisiblePicker = activePicker
    }
    fun dismissAllSuggestions() {
        showAccountSuggestions = false
        showToAccountSuggestions = false
        showCategorySuggestions = false
        showNoteSuggestions = false
        showNote2Suggestions = false
        showTaggingSuggestions = false
        showToTaggingSuggestions = false
    }
    val anySuggestionsOpen = showAccountSuggestions ||
        showToAccountSuggestions ||
        showCategorySuggestions ||
        showNoteSuggestions ||
        showNote2Suggestions ||
        showTaggingSuggestions ||
        showToTaggingSuggestions
    BackHandler {
        when {
            anySuggestionsOpen -> dismissAllSuggestions()
            activePicker != ActivePicker.NONE -> {
                activePicker = ActivePicker.NONE
            }
            else -> onBack()
        }
    }
    var expandedGroupId by remember(existing, draft, draftKey) { mutableStateOf<String?>(null) }
    var expandedCategoryId by remember(existing, draft, draftKey) { mutableStateOf<String?>(null) }

    var showDatePicker by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showCopyOptionsDialog by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showCopyDatePicker by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showRepeatDialog by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var showCreateLedgerDialog by remember(existing, draft, draftKey) { mutableStateOf(false) }
    var draftLedgerNameForDialog by remember(existing, draft, draftKey) { mutableStateOf("") }
    var draftCategoryIdForDialog by remember(existing, draft, draftKey) { mutableStateOf<String?>(null) }
    var validationError by remember(existing, draft, draftKey) { mutableStateOf<String?>(null) }

    val targetAccentColor = when (txType) {
        TransactionType.INCOME -> LedgerBlue
        TransactionType.EXPENSE -> LedgerCoral
        TransactionType.TRANSFER -> LedgerTextPrimary
    }
    val accentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = LedgerMotion.snappyTween(),
        label = "tx_accent_color"
    )

    val saveButtonTextColor = when (txType) {
        TransactionType.TRANSFER -> LedgerBackground
        else -> Color.White
    }

    val filteredCategories = remember(bundle.categories, txType) {
        val catType = if (txType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        bundle.categories.filter { it.categoryType() == catType }
    }

    val allAccountSuggestions = remember(bundle.accountSections) {
        bundle.accountSections.flatMap { sec ->
            sec.accounts.map { item ->
                AccountSuggestionItem(
                    accountId = item.account.id,
                    accountName = item.account.name,
                    groupName = sec.group.name,
                    balance = item.balance
                )
            }
        }
    }

    val allCategorySuggestions = remember(filteredCategories, bundle.settings.subcategoryEnabled) {
        buildList {
            filteredCategories.forEach { cat ->
                add(
                    CategorySuggestionItem(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        subcategory = null,
                        colorHex = cat.colorHex,
                        displayLabel = cat.name
                    )
                )
                if (bundle.settings.subcategoryEnabled) {
                    cat.subcategories.forEach { sub ->
                        add(
                            CategorySuggestionItem(
                                categoryId = cat.id,
                                categoryName = cat.name,
                                subcategory = sub,
                                colorHex = cat.colorHex,
                                displayLabel = "${cat.name} / $sub"
                            )
                        )
                    }
                }
            }
        }
    }

    fun filterAccounts(query: String, activeTyping: Boolean): List<AccountSuggestionItem> {
        val q = query.trim()
        if (!activeTyping || q.isEmpty()) return allAccountSuggestions.take(6)
        val starts = allAccountSuggestions.filter {
            it.accountName.startsWith(q, ignoreCase = true) ||
                it.groupName.startsWith(q, ignoreCase = true)
        }
        val contains = allAccountSuggestions.filter { item ->
            item !in starts && (
                item.accountName.contains(q, ignoreCase = true) ||
                    item.groupName.contains(q, ignoreCase = true)
                )
        }
        return (starts + contains).take(6)
    }

    fun filterCategorySuggestions(query: String, activeTyping: Boolean): List<CategorySuggestionItem> {
        val q = query.trim()
        if (!activeTyping || q.isEmpty()) return allCategorySuggestions.take(6)
        val starts = allCategorySuggestions.filter {
            it.displayLabel.startsWith(q, ignoreCase = true) ||
                it.categoryName.startsWith(q, ignoreCase = true) ||
                (it.subcategory?.startsWith(q, ignoreCase = true) == true)
        }
        val contains = allCategorySuggestions.filter { item ->
            item !in starts && (
                item.displayLabel.contains(q, ignoreCase = true) ||
                    item.categoryName.contains(q, ignoreCase = true) ||
                    (item.subcategory?.contains(q, ignoreCase = true) == true)
                )
        }
        return (starts + contains).take(6)
    }

    val allNoteSuggestions = remember(bundle.transactions) {
        val counts = mutableMapOf<String, Int>()
        val latestLabel = mutableMapOf<String, String>()
        val latestOrder = mutableMapOf<String, Long>()
        bundle.transactions.forEach { tx ->
            val clean = tx.note.trim()
            if (clean.isNotEmpty()) {
                val key = clean.lowercase()
                counts[key] = (counts[key] ?: 0) + 1
                val recency = tx.epochDay * 1_000_000L + (tx.timestampMillis % 1_000_000L)
                if (recency >= (latestOrder[key] ?: Long.MIN_VALUE)) {
                    latestOrder[key] = recency
                    latestLabel[key] = clean
                }
            }
        }
        counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { latestOrder[it.key] ?: 0L }
            )
            .map { (key, count) ->
                TextSuggestionItem(
                    text = latestLabel[key] ?: key,
                    usageCount = count
                )
            }
    }

    val allTaggingSuggestions = remember(bundle.transactions) {
        val counts = mutableMapOf<String, Int>()
        val latestLabel = mutableMapOf<String, String>()
        val latestOrder = mutableMapOf<String, Long>()
        bundle.transactions.forEach { tx ->
            tx.allEffectivePrimaryTaggings().forEach { clean ->
                if (clean.isNotEmpty()) {
                    val key = clean.lowercase()
                    counts[key] = (counts[key] ?: 0) + 1
                    val recency = tx.epochDay * 1_000_000L + (tx.timestampMillis % 1_000_000L)
                    if (recency >= (latestOrder[key] ?: Long.MIN_VALUE)) {
                        latestOrder[key] = recency
                        latestLabel[key] = clean
                    }
                }
            }
        }
        counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { latestOrder[it.key] ?: 0L }
            )
            .map { (key, count) ->
                TextSuggestionItem(
                    text = latestLabel[key] ?: key,
                    usageCount = count
                )
            }
    }

    val allNote2Suggestions = remember(bundle.transactions) {
        val counts = mutableMapOf<String, Int>()
        val latestLabel = mutableMapOf<String, String>()
        val latestOrder = mutableMapOf<String, Long>()
        bundle.transactions.forEach { tx ->
            val clean = tx.note2.trim()
            if (clean.isNotEmpty()) {
                val key = clean.lowercase()
                counts[key] = (counts[key] ?: 0) + 1
                val recency = tx.epochDay * 1_000_000L + (tx.timestampMillis % 1_000_000L)
                if (recency >= (latestOrder[key] ?: Long.MIN_VALUE)) {
                    latestOrder[key] = recency
                    latestLabel[key] = clean
                }
            }
        }
        counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { latestOrder[it.key] ?: 0L }
            )
            .map { (key, count) ->
                TextSuggestionItem(
                    text = latestLabel[key] ?: key,
                    usageCount = count
                )
            }
    }

    fun filterTextSuggestions(
        allItems: List<TextSuggestionItem>,
        query: String
    ): List<TextSuggestionItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val starts = allItems.filter { it.text.startsWith(q, ignoreCase = true) }
        val contains = allItems.filter { item ->
            item !in starts && item.text.contains(q, ignoreCase = true)
        }
        return (starts + contains).take(6)
    }

    fun formatCategoryLabel(catId: String, sub: String): String {
        val name = bundle.categoryById(catId)?.name.orEmpty()
        return when {
            name.isBlank() -> ""
            sub.isNotBlank() && bundle.settings.subcategoryEnabled -> "$name / $sub"
            else -> name
        }
    }

    fun openPicker(picker: ActivePicker) {
        focusManager.clearFocus()
        dismissAllSuggestions()
        activePicker = if (activePicker == picker) ActivePicker.NONE else picker
    }

    fun buildValidatedTransaction(
        targetEpochDay: Long = epochDay,
        clearId: Boolean = false
    ): LedgerTransaction? {
        val amount = amountText.replace(",", "").trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            validationError = "Please enter a valid amount greater than 0"
            return null
        }

        // Resolve exact typed ledger match if the user typed without tapping a suggestion
        var resolvedAccountId = accountId
        if (resolvedAccountId.isBlank() && accountQuery.isNotBlank()) {
            val match = allAccountSuggestions.firstOrNull {
                it.accountName.equals(accountQuery.trim(), ignoreCase = true)
            }
            if (match != null) {
                resolvedAccountId = match.accountId
                accountId = match.accountId
                accountQuery = match.accountName
            }
        }
        if (resolvedAccountId.isBlank()) {
            validationError = "Please select or choose a matching account"
            activePicker = ActivePicker.ACCOUNT
            return null
        }

        var resolvedToAccountId = toAccountId
        var resolvedCategoryId = categoryId
        var resolvedSubcategory = subcategory

        if (txType == TransactionType.TRANSFER) {
            if (resolvedToAccountId.isBlank() && toAccountQuery.isNotBlank()) {
                val match = allAccountSuggestions.firstOrNull {
                    it.accountName.equals(toAccountQuery.trim(), ignoreCase = true)
                }
                if (match != null) {
                    resolvedToAccountId = match.accountId
                    toAccountId = match.accountId
                    toAccountQuery = match.accountName
                }
            }
            if (resolvedToAccountId.isBlank()) {
                validationError = "Please select a destination account"
                activePicker = ActivePicker.TO_ACCOUNT
                return null
            }
            if (resolvedToAccountId == resolvedAccountId) {
                val cleanFromTag = taggingText.trim()
                val cleanToTag = toTaggingText.trim()
                val tagsDiffer = !cleanFromTag.equals(cleanToTag, ignoreCase = true)
                if (!tagsDiffer) {
                    validationError = "Select a different destination account or assign different From/To tags"
                    return null
                }
            }
        } else {
            if (resolvedCategoryId.isBlank() && categoryQuery.isNotBlank()) {
                val match = allCategorySuggestions.firstOrNull {
                    it.displayLabel.equals(categoryQuery.trim(), ignoreCase = true) ||
                        it.categoryName.equals(categoryQuery.trim(), ignoreCase = true) ||
                        (it.subcategory?.equals(categoryQuery.trim(), ignoreCase = true) == true)
                }
                if (match != null) {
                    resolvedCategoryId = match.categoryId
                    resolvedSubcategory = match.subcategory.orEmpty()
                    categoryId = resolvedCategoryId
                    subcategory = resolvedSubcategory
                    categoryQuery = match.displayLabel
                }
            }
            if (resolvedCategoryId.isBlank()) {
                validationError = if (txType == TransactionType.INCOME) {
                    "Please select an income category or ledger"
                } else {
                    "Please select an expense category or ledger"
                }
                activePicker = ActivePicker.CATEGORY
                return null
            }
        }

        val fee = if (txType == TransactionType.TRANSFER && showFeeField) {
            feeText.replace(",", "").trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        } else 0.0

        validationError = null
        return LedgerTransaction(
            id = if (clearId) "" else existing?.id.orEmpty(),
            type = txType.name,
            amount = amount,
            feeAmount = fee,
            epochDay = targetEpochDay,
            timestampMillis = System.currentTimeMillis(),
            accountId = resolvedAccountId,
            toAccountId = if (txType == TransactionType.TRANSFER) resolvedToAccountId else null,
            categoryId = if (txType != TransactionType.TRANSFER) resolvedCategoryId else null,
            subcategory = if (txType != TransactionType.TRANSFER &&
                (bundle.settings.subcategoryEnabled || resolvedSubcategory.isNotBlank())
            ) {
                resolvedSubcategory.takeIf { it.isNotBlank() }
            } else null,
            note = noteText.trim(),
            note2 = note2Text.trim(),
            tagging = taggingText.trim().takeIf { it.isNotBlank() },
            toTagging = toTaggingText.trim().takeIf { it.isNotBlank() },
            description = descriptionText.trim().takeIf { it.isNotBlank() },
            tags = emptyList(),
            favorite = favorite,
            repeatFrequency = RepeatFrequency.encode(
                freq = repeatFreq,
                dayOfMonth = repeatDayOfMonth,
                customCount = repeatCustomCount,
                customUnit = repeatCustomUnit
            )
        )
    }

    fun submit(keepOpen: Boolean) {
        val model = buildValidatedTransaction() ?: return
        onSave(existing?.id, model, keepOpen)
        if (keepOpen) {
            amountText = ""
            feeText = ""
            noteText = ""
            note2Text = ""
            taggingText = ""
            toTaggingText = ""
            descriptionText = ""
        }
    }

    fun startCopyFlow() {
        if (buildValidatedTransaction(clearId = true) != null) {
            focusManager.clearFocus()
            activePicker = ActivePicker.NONE
            showCopyOptionsDialog = true
        }
    }

    fun openCopiedDraft(targetEpochDay: Long) {
        val copiedModel = buildValidatedTransaction(
            targetEpochDay = targetEpochDay,
            clearId = true
        ) ?: return
        showCopyOptionsDialog = false
        showCopyDatePicker = false
        onCopy?.invoke(copiedModel)
    }

    val suggestionEnter = remember { LedgerMotion.expandEnter() }
    val suggestionExit = remember { LedgerMotion.shrinkExit() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
    ) {
        val labelWidth = if (maxWidth < 380.dp) 68.dp else 76.dp
        val pickerHeight = (maxHeight * 0.36f).coerceIn(200.dp, 290.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("tx_editor_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LedgerTextPrimary
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (txType) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.EXPENSE -> "Expense"
                        TransactionType.TRANSFER -> "Transfer"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (isDraftCopy) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LedgerAmber.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, LedgerAmber.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("tx_draft_badge")
                    ) {
                        Text(
                            text = "Draft",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerAmber,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = { favorite = !favorite },
                modifier = Modifier.testTag("tx_editor_favorite_button")
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Toggle bookmark",
                    tint = if (favorite) LedgerAmber else LedgerTextSecondary
                )
            }
        }

        // Scrollable Form Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
            if (isDraftCopy) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LedgerAmber.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, LedgerAmber.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("tx_draft_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = LedgerAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Draft copy — review details and tap Save to add entry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LedgerTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            // 3-Segment Type Switcher (Income | Expense | Transfer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.entries.forEach { option ->
                    val isSelected = txType == option
                    val targetBorder = when {
                        !isSelected -> LedgerBorder
                        option == TransactionType.INCOME -> LedgerBlue
                        option == TransactionType.EXPENSE -> LedgerCoral
                        else -> LedgerTextPrimary
                    }
                    val targetText = when {
                        !isSelected -> LedgerTextSecondary
                        option == TransactionType.INCOME -> LedgerBlue
                        option == TransactionType.EXPENSE -> LedgerCoral
                        else -> LedgerTextPrimary
                    }
                    val borderColor by animateColorAsState(
                        targetValue = targetBorder,
                        animationSpec = LedgerMotion.microTween(100),
                        label = "seg_border_${option.name}"
                    )
                    val textColor by animateColorAsState(
                        targetValue = targetText,
                        animationSpec = LedgerMotion.microTween(100),
                        label = "seg_text_${option.name}"
                    )
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) LedgerSurfaceElevated else LedgerSurface,
                        animationSpec = LedgerMotion.microTween(100),
                        label = "seg_bg_${option.name}"
                    )
                    val label = when (option) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.EXPENSE -> "Expense"
                        TransactionType.TRANSFER -> "Transfer"
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = containerColor,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable {
                                txType = option
                                dismissAllSuggestions()
                                if (option != TransactionType.TRANSFER) {
                                    val validCat = bundle.categories.any {
                                        it.id == categoryId && it.categoryType() == if (option == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                                    }
                                    if (!validCat) {
                                        categoryId = ""
                                        subcategory = ""
                                        categoryQuery = ""
                                    }
                                }
                            }
                            .testTag("tx_type_${option.name.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date + Rep/Inst Row
            FormFieldRow(
                label = "Date",
                labelWidth = labelWidth,
                isActive = false,
                activeAccent = accentColor,
                onClick = { showDatePicker = true },
                trailing = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showRepeatDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("tx_repeat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Repeat frequency",
                            tint = if (repeatFreq != RepeatFrequency.NONE) accentColor else LedgerTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        val encodedRepeat = RepeatFrequency.encode(
                            freq = repeatFreq,
                            dayOfMonth = repeatDayOfMonth,
                            customCount = repeatCustomCount,
                            customUnit = repeatCustomUnit
                        )
                        Text(
                            text = RepeatFrequency.formatCompactLabel(encodedRepeat),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (repeatFreq != RepeatFrequency.NONE) accentColor else LedgerTextMuted,
                            maxLines = 1
                        )
                    }
                }
            ) {
                Text(
                    text = LedgerFormatters.formatFullDateInput(epochDay),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LedgerTextPrimary
                )
            }

            // Tag suggestion lists for the inline 2-alphabet Tag boxes beside Account/From and Category/To
            val matchingTags = remember(allTaggingSuggestions, taggingText, showTaggingSuggestions, taggingFieldFocused) {
                if (showTaggingSuggestions && taggingFieldFocused && taggingText.trim().isNotEmpty()) {
                    filterTextSuggestions(allTaggingSuggestions, taggingText)
                } else {
                    emptyList()
                }
            }
            val matchingToTags = remember(
                allTaggingSuggestions,
                toTaggingText,
                showToTaggingSuggestions,
                toTaggingFieldFocused
            ) {
                if (showToTaggingSuggestions && toTaggingFieldFocused && toTaggingText.trim().isNotEmpty()) {
                    filterTextSuggestions(allTaggingSuggestions, toTaggingText)
                } else {
                    emptyList()
                }
            }

            // Account / From & Category / To Rows (with compact inline 2-alphabet Tag boxes beside each field)
            if (txType == TransactionType.TRANSFER) {
                FieldWithFloatingSuggestions(
                    showSuggestions = showAccountSuggestions && accountQuery.trim().isNotEmpty(),
                    startIndent = labelWidth,
                    suggestionsContent = {
                        AccountSuggestionList(
                            suggestions = filterAccounts(accountQuery, hasTypedAccountQuery),
                            selectedAccountId = accountId,
                            accentColor = accentColor,
                            symbol = symbol,
                            onSelect = { item ->
                                accountId = item.accountId
                                accountQuery = item.accountName
                                hasTypedAccountQuery = false
                                showAccountSuggestions = false
                                focusManager.clearFocus()
                            },
                            onBrowseAll = { openPicker(ActivePicker.ACCOUNT) }
                        )
                    }
                ) {
                    FormFieldRow(
                        label = "From",
                        labelWidth = labelWidth,
                        isActive = activePicker == ActivePicker.ACCOUNT || accountFieldFocused,
                        activeAccent = accentColor,
                        onClick = { openPicker(ActivePicker.ACCOUNT) },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (accountQuery.isNotEmpty() && accountFieldFocused) {
                                    IconButton(
                                        onClick = {
                                            accountQuery = ""
                                            accountId = ""
                                            hasTypedAccountQuery = false
                                            showAccountSuggestions = false
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear source account",
                                            tint = LedgerTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { openPicker(ActivePicker.ACCOUNT) },
                                    modifier = Modifier.size(30.dp).testTag("tx_from_account_picker_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select source account",
                                        tint = if (activePicker == ActivePicker.ACCOUNT) accentColor else LedgerTextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val tmpId = accountId
                                        accountId = toAccountId
                                        toAccountId = tmpId
                                        val tmpQuery = accountQuery
                                        accountQuery = toAccountQuery
                                        toAccountQuery = tmpQuery
                                        val tmpTag = taggingText
                                        taggingText = toTaggingText
                                        toTaggingText = tmpTag
                                    },
                                    modifier = Modifier.size(30.dp).testTag("tx_swap_accounts_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapVert,
                                        contentDescription = "Swap accounts",
                                        tint = LedgerTextSecondary
                                    )
                                }
                                FieldWithFloatingSuggestions(
                                    showSuggestions = matchingTags.isNotEmpty(),
                                    startIndent = 0.dp,
                                    minDropdownWidth = 136.dp,
                                    modifier = Modifier.wrapContentSize(),
                                    suggestionsContent = {
                                        TextSuggestionList(
                                            suggestions = matchingTags,
                                            currentValue = taggingText,
                                            accentColor = LedgerCoral,
                                            listTestTag = "tx_tagging_suggestions_list",
                                            itemTestTagPrefix = "tx_tagging_suggestion_",
                                            onSelect = { selected ->
                                                taggingText = selected.text
                                                showTaggingSuggestions = false
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                ) {
                                    InlineCompactTagBox(
                                        value = taggingText,
                                        onValueChange = { typed ->
                                            taggingText = typed.take(20)
                                            showTaggingSuggestions = typed.trim().isNotEmpty()
                                            activePicker = ActivePicker.NONE
                                        },
                                        isFocused = taggingFieldFocused,
                                        onFocusChanged = { focused ->
                                            taggingFieldFocused = focused
                                            if (focused) {
                                                activePicker = ActivePicker.NONE
                                                dismissAllSuggestions()
                                            } else {
                                                showTaggingSuggestions = false
                                            }
                                        },
                                        accentColor = LedgerCoral,
                                        inputTestTag = "tx_tagging_input"
                                    )
                                }
                            }
                        }
                    ) {
                        BasicTextField(
                            value = accountQuery,
                            onValueChange = { typed ->
                                accountQuery = typed.take(80)
                                hasTypedAccountQuery = typed.trim().isNotEmpty()
                                val exact = allAccountSuggestions.firstOrNull {
                                    it.accountName.equals(typed.trim(), ignoreCase = true)
                                }
                                accountId = exact?.accountId.orEmpty()
                                showAccountSuggestions = typed.trim().isNotEmpty()
                                activePicker = ActivePicker.NONE
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { inner ->
                                if (accountQuery.isEmpty()) {
                                    Text("Source ledger", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                                }
                                inner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { state ->
                                    accountFieldFocused = state.isFocused
                                    if (state.isFocused) {
                                        hasTypedAccountQuery = false
                                        activePicker = ActivePicker.NONE
                                        dismissAllSuggestions()
                                    } else {
                                        showAccountSuggestions = false
                                    }
                                }
                                .testTag("tx_from_account_input")
                        )
                    }
                }

                FieldWithFloatingSuggestions(
                    showSuggestions = showToAccountSuggestions && toAccountQuery.trim().isNotEmpty(),
                    startIndent = labelWidth,
                    suggestionsContent = {
                        AccountSuggestionList(
                            suggestions = filterAccounts(toAccountQuery, hasTypedToAccountQuery),
                            selectedAccountId = toAccountId,
                            accentColor = accentColor,
                            symbol = symbol,
                            onSelect = { item ->
                                toAccountId = item.accountId
                                toAccountQuery = item.accountName
                                hasTypedToAccountQuery = false
                                showToAccountSuggestions = false
                                focusManager.clearFocus()
                            },
                            onBrowseAll = { openPicker(ActivePicker.TO_ACCOUNT) }
                        )
                    }
                ) {
                    FormFieldRow(
                        label = "To",
                        labelWidth = labelWidth,
                        isActive = activePicker == ActivePicker.TO_ACCOUNT || toAccountFieldFocused,
                        activeAccent = accentColor,
                        onClick = { openPicker(ActivePicker.TO_ACCOUNT) },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (toAccountQuery.isNotEmpty() && toAccountFieldFocused) {
                                    IconButton(
                                        onClick = {
                                            toAccountQuery = ""
                                            toAccountId = ""
                                            hasTypedToAccountQuery = false
                                            showToAccountSuggestions = false
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear destination account",
                                            tint = LedgerTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { openPicker(ActivePicker.TO_ACCOUNT) },
                                    modifier = Modifier.size(30.dp).testTag("tx_to_account_picker_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select destination account",
                                        tint = if (activePicker == ActivePicker.TO_ACCOUNT) accentColor else LedgerTextSecondary
                                    )
                                }
                                FieldWithFloatingSuggestions(
                                    showSuggestions = matchingToTags.isNotEmpty(),
                                    startIndent = 0.dp,
                                    minDropdownWidth = 136.dp,
                                    modifier = Modifier.wrapContentSize(),
                                    suggestionsContent = {
                                        TextSuggestionList(
                                            suggestions = matchingToTags,
                                            currentValue = toTaggingText,
                                            accentColor = LedgerBlue,
                                            listTestTag = "tx_to_tagging_suggestions_list",
                                            itemTestTagPrefix = "tx_to_tagging_suggestion_",
                                            onSelect = { selected ->
                                                toTaggingText = selected.text
                                                showToTaggingSuggestions = false
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                ) {
                                    InlineCompactTagBox(
                                        value = toTaggingText,
                                        onValueChange = { typed ->
                                            toTaggingText = typed.take(20)
                                            showToTaggingSuggestions = typed.trim().isNotEmpty()
                                            activePicker = ActivePicker.NONE
                                        },
                                        isFocused = toTaggingFieldFocused,
                                        onFocusChanged = { focused ->
                                            toTaggingFieldFocused = focused
                                            if (focused) {
                                                activePicker = ActivePicker.NONE
                                                dismissAllSuggestions()
                                            } else {
                                                showToTaggingSuggestions = false
                                            }
                                        },
                                        accentColor = LedgerBlue,
                                        inputTestTag = "tx_to_tagging_input"
                                    )
                                }
                            }
                        }
                    ) {
                        BasicTextField(
                            value = toAccountQuery,
                            onValueChange = { typed ->
                                toAccountQuery = typed.take(80)
                                hasTypedToAccountQuery = typed.trim().isNotEmpty()
                                val exact = allAccountSuggestions.firstOrNull {
                                    it.accountName.equals(typed.trim(), ignoreCase = true)
                                }
                                toAccountId = exact?.accountId.orEmpty()
                                showToAccountSuggestions = typed.trim().isNotEmpty()
                                activePicker = ActivePicker.NONE
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { inner ->
                                if (toAccountQuery.isEmpty()) {
                                    Text("Destination ledger", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                                }
                                inner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { state ->
                                    toAccountFieldFocused = state.isFocused
                                    if (state.isFocused) {
                                        hasTypedToAccountQuery = false
                                        activePicker = ActivePicker.NONE
                                        dismissAllSuggestions()
                                    } else {
                                        showToAccountSuggestions = false
                                    }
                                }
                                .testTag("tx_to_account_input")
                        )
                    }
                }
            } else {
                FieldWithFloatingSuggestions(
                    showSuggestions = showAccountSuggestions && accountQuery.trim().isNotEmpty(),
                    startIndent = labelWidth,
                    suggestionsContent = {
                        AccountSuggestionList(
                            suggestions = filterAccounts(accountQuery, hasTypedAccountQuery),
                            selectedAccountId = accountId,
                            accentColor = accentColor,
                            symbol = symbol,
                            onSelect = { item ->
                                accountId = item.accountId
                                accountQuery = item.accountName
                                hasTypedAccountQuery = false
                                showAccountSuggestions = false
                                focusManager.clearFocus()
                            },
                            onBrowseAll = { openPicker(ActivePicker.ACCOUNT) }
                        )
                    }
                ) {
                    FormFieldRow(
                        label = "Account",
                        labelWidth = labelWidth,
                        isActive = activePicker == ActivePicker.ACCOUNT || accountFieldFocused,
                        activeAccent = accentColor,
                        onClick = { openPicker(ActivePicker.ACCOUNT) },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (accountQuery.isNotEmpty() && accountFieldFocused) {
                                    IconButton(
                                        onClick = {
                                            accountQuery = ""
                                            accountId = ""
                                            hasTypedAccountQuery = false
                                            showAccountSuggestions = false
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear account",
                                            tint = LedgerTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { openPicker(ActivePicker.ACCOUNT) },
                                    modifier = Modifier.size(30.dp).testTag("tx_account_picker_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select account",
                                        tint = if (activePicker == ActivePicker.ACCOUNT) accentColor else LedgerTextSecondary
                                    )
                                }
                                FieldWithFloatingSuggestions(
                                    showSuggestions = matchingTags.isNotEmpty(),
                                    startIndent = 0.dp,
                                    minDropdownWidth = 136.dp,
                                    modifier = Modifier.wrapContentSize(),
                                    suggestionsContent = {
                                        TextSuggestionList(
                                            suggestions = matchingTags,
                                            currentValue = taggingText,
                                            accentColor = accentColor,
                                            listTestTag = "tx_tagging_suggestions_list",
                                            itemTestTagPrefix = "tx_tagging_suggestion_",
                                            onSelect = { selected ->
                                                taggingText = selected.text
                                                showTaggingSuggestions = false
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                ) {
                                    InlineCompactTagBox(
                                        value = taggingText,
                                        onValueChange = { typed ->
                                            taggingText = typed.take(20)
                                            showTaggingSuggestions = typed.trim().isNotEmpty()
                                            activePicker = ActivePicker.NONE
                                        },
                                        isFocused = taggingFieldFocused,
                                        onFocusChanged = { focused ->
                                            taggingFieldFocused = focused
                                            if (focused) {
                                                activePicker = ActivePicker.NONE
                                                dismissAllSuggestions()
                                            } else {
                                                showTaggingSuggestions = false
                                            }
                                        },
                                        accentColor = accentColor,
                                        inputTestTag = "tx_tagging_input"
                                    )
                                }
                            }
                        }
                    ) {
                        BasicTextField(
                            value = accountQuery,
                            onValueChange = { typed ->
                                accountQuery = typed.take(80)
                                hasTypedAccountQuery = typed.trim().isNotEmpty()
                                val exact = allAccountSuggestions.firstOrNull {
                                    it.accountName.equals(typed.trim(), ignoreCase = true)
                                }
                                accountId = exact?.accountId.orEmpty()
                                showAccountSuggestions = typed.trim().isNotEmpty()
                                activePicker = ActivePicker.NONE
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { inner ->
                                if (accountQuery.isEmpty()) {
                                    Text("Select ledger", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                                }
                                inner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { state ->
                                    accountFieldFocused = state.isFocused
                                    if (state.isFocused) {
                                        hasTypedAccountQuery = false
                                        activePicker = ActivePicker.NONE
                                        dismissAllSuggestions()
                                    } else {
                                        showAccountSuggestions = false
                                    }
                                }
                                .testTag("tx_account_input")
                        )
                    }
                }

                FieldWithFloatingSuggestions(
                    showSuggestions = showCategorySuggestions && categoryQuery.trim().isNotEmpty(),
                    startIndent = labelWidth,
                    suggestionsContent = {
                        CategorySuggestionList(
                            suggestions = filterCategorySuggestions(categoryQuery, hasTypedCategoryQuery),
                            selectedCategoryId = categoryId,
                            selectedSubcategory = subcategory,
                            queryText = if (hasTypedCategoryQuery) categoryQuery else "",
                            txType = txType,
                            accentColor = accentColor,
                            onSelect = { item ->
                                categoryId = item.categoryId
                                subcategory = item.subcategory.orEmpty()
                                categoryQuery = item.displayLabel
                                hasTypedCategoryQuery = false
                                showCategorySuggestions = false
                                focusManager.clearFocus()
                            },
                            onCreateNewLedger = if (onCreateCategoryLedger != null) {
                                { typedName ->
                                    draftLedgerNameForDialog = typedName
                                    draftCategoryIdForDialog = categoryId.takeIf { it.isNotBlank() }
                                    showCategorySuggestions = false
                                    focusManager.clearFocus()
                                    showCreateLedgerDialog = true
                                }
                            } else null,
                            onBrowseAll = { openPicker(ActivePicker.CATEGORY) }
                        )
                    }
                ) {
                    FormFieldRow(
                        label = "Category",
                        labelWidth = labelWidth,
                        isActive = activePicker == ActivePicker.CATEGORY || categoryFieldFocused,
                        activeAccent = accentColor,
                        onClick = { openPicker(ActivePicker.CATEGORY) },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (categoryQuery.isNotEmpty() && categoryFieldFocused) {
                                    IconButton(
                                        onClick = {
                                            categoryQuery = ""
                                            categoryId = ""
                                            subcategory = ""
                                            hasTypedCategoryQuery = false
                                            showCategorySuggestions = false
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear category",
                                            tint = LedgerTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { openPicker(ActivePicker.CATEGORY) },
                                    modifier = Modifier.size(30.dp).testTag("tx_category_picker_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select category",
                                        tint = if (activePicker == ActivePicker.CATEGORY) accentColor else LedgerTextSecondary
                                    )
                                }
                                FieldWithFloatingSuggestions(
                                    showSuggestions = matchingToTags.isNotEmpty(),
                                    startIndent = 0.dp,
                                    minDropdownWidth = 136.dp,
                                    modifier = Modifier.wrapContentSize(),
                                    suggestionsContent = {
                                        TextSuggestionList(
                                            suggestions = matchingToTags,
                                            currentValue = toTaggingText,
                                            accentColor = accentColor,
                                            listTestTag = "tx_to_tagging_suggestions_list",
                                            itemTestTagPrefix = "tx_to_tagging_suggestion_",
                                            onSelect = { selected ->
                                                toTaggingText = selected.text
                                                showToTaggingSuggestions = false
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                ) {
                                    InlineCompactTagBox(
                                        value = toTaggingText,
                                        onValueChange = { typed ->
                                            toTaggingText = typed.take(20)
                                            showToTaggingSuggestions = typed.trim().isNotEmpty()
                                            activePicker = ActivePicker.NONE
                                        },
                                        isFocused = toTaggingFieldFocused,
                                        onFocusChanged = { focused ->
                                            toTaggingFieldFocused = focused
                                            if (focused) {
                                                activePicker = ActivePicker.NONE
                                                dismissAllSuggestions()
                                            } else {
                                                showToTaggingSuggestions = false
                                            }
                                        },
                                        accentColor = accentColor,
                                        inputTestTag = "tx_to_tagging_input"
                                    )
                                }
                            }
                        }
                    ) {
                        BasicTextField(
                            value = categoryQuery,
                            onValueChange = { typed ->
                                categoryQuery = typed.take(80)
                                hasTypedCategoryQuery = typed.trim().isNotEmpty()
                                val exact = allCategorySuggestions.firstOrNull {
                                    it.displayLabel.equals(typed.trim(), ignoreCase = true) ||
                                        (it.subcategory == null && it.categoryName.equals(typed.trim(), ignoreCase = true))
                                }
                                if (exact != null) {
                                    categoryId = exact.categoryId
                                    subcategory = exact.subcategory.orEmpty()
                                } else {
                                    categoryId = ""
                                    subcategory = ""
                                }
                                showCategorySuggestions = typed.trim().isNotEmpty()
                                activePicker = ActivePicker.NONE
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { inner ->
                                if (categoryQuery.isEmpty()) {
                                    Text(
                                        text = if (txType == TransactionType.INCOME) {
                                            "Income ledger"
                                        } else {
                                            "Expense ledger"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = LedgerTextMuted
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { state ->
                                    categoryFieldFocused = state.isFocused
                                    if (state.isFocused) {
                                        hasTypedCategoryQuery = false
                                        activePicker = ActivePicker.NONE
                                        dismissAllSuggestions()
                                    } else {
                                        showCategorySuggestions = false
                                    }
                                }
                                .testTag("tx_category_input")
                        )
                    }
                }
            }

            // Amount Row (+ Fees pill for Transfer)
            FormFieldRow(
                label = "Amount",
                labelWidth = labelWidth,
                isActive = false,
                activeAccent = accentColor,
                onClick = { activePicker = ActivePicker.NONE },
                trailing = if (txType == TransactionType.TRANSFER) {
                    {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (showFeeField) LedgerCoral.copy(alpha = 0.2f) else LedgerSurfaceElevated,
                            border = BorderStroke(1.dp, if (showFeeField) LedgerCoral else LedgerBorder),
                            modifier = Modifier
                                .clickable { showFeeField = !showFeeField }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("tx_fees_toggle")
                        ) {
                            Text(
                                text = "Fees",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (showFeeField) LedgerCoral else LedgerTextPrimary
                            )
                        }
                    }
                } else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = bundle.settings.mainCurrencySymbol,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LedgerTextSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    BasicTextField(
                        value = amountText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) amountText = filtered.take(14)
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = LedgerTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = IndianNumberVisualTransformation,
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            if (amountText.isEmpty()) {
                                Text("0.00", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                    )
                }
            }

            AnimatedVisibility(
                visible = txType == TransactionType.TRANSFER && showFeeField,
                enter = suggestionEnter,
                exit = suggestionExit
            ) {
                FormFieldRow(
                    label = "Fees",
                    labelWidth = labelWidth,
                    isActive = false,
                    activeAccent = accentColor,
                    onClick = { activePicker = ActivePicker.NONE }
                ) {
                    BasicTextField(
                        value = feeText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) feeText = filtered.take(12)
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = IndianNumberVisualTransformation,
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            if (feeText.isEmpty()) {
                                Text("0.00", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("tx_fee_input")
                    )
                }
            }

            // Note 1 Row (with floating type-ahead matching suggestions from previous entries)
            val matchingNotes = remember(allNoteSuggestions, noteText, showNoteSuggestions, noteFieldFocused) {
                if (showNoteSuggestions && noteFieldFocused && noteText.trim().isNotEmpty()) {
                    filterTextSuggestions(allNoteSuggestions, noteText)
                } else {
                    emptyList()
                }
            }
            FieldWithFloatingSuggestions(
                showSuggestions = matchingNotes.isNotEmpty(),
                startIndent = labelWidth,
                suggestionsContent = {
                    TextSuggestionList(
                        suggestions = matchingNotes,
                        currentValue = noteText,
                        accentColor = accentColor,
                        listTestTag = "tx_note_suggestions_list",
                        itemTestTagPrefix = "tx_note_suggestion_",
                        onSelect = { selected ->
                            noteText = selected.text
                            showNoteSuggestions = false
                            focusManager.clearFocus()
                        }
                    )
                }
            ) {
                FormFieldRow(
                    label = "Note 1",
                    labelWidth = labelWidth,
                    isActive = noteFieldFocused,
                    activeAccent = accentColor,
                    onClick = { activePicker = ActivePicker.NONE },
                    trailing = if (noteText.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = {
                                    noteText = ""
                                    showNoteSuggestions = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear note 1",
                                    tint = LedgerTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null
                ) {
                    BasicTextField(
                        value = noteText,
                        onValueChange = { typed ->
                            noteText = typed.take(140)
                            showNoteSuggestions = typed.trim().isNotEmpty()
                            activePicker = ActivePicker.NONE
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            if (noteText.isEmpty()) {
                                Text("Enter title or note 1", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                noteFieldFocused = state.isFocused
                                if (state.isFocused) {
                                    activePicker = ActivePicker.NONE
                                    dismissAllSuggestions()
                                } else {
                                    showNoteSuggestions = false
                                }
                            }
                            .testTag("tx_note_input")
                    )
                }
            }

            // Note 2 Row (same floating type-ahead matching behavior as Note 1)
            val matchingNotes2 = remember(allNote2Suggestions, note2Text, showNote2Suggestions, note2FieldFocused) {
                if (showNote2Suggestions && note2FieldFocused && note2Text.trim().isNotEmpty()) {
                    filterTextSuggestions(allNote2Suggestions, note2Text)
                } else {
                    emptyList()
                }
            }
            FieldWithFloatingSuggestions(
                showSuggestions = matchingNotes2.isNotEmpty(),
                startIndent = labelWidth,
                suggestionsContent = {
                    TextSuggestionList(
                        suggestions = matchingNotes2,
                        currentValue = note2Text,
                        accentColor = accentColor,
                        listTestTag = "tx_note2_suggestions_list",
                        itemTestTagPrefix = "tx_note2_suggestion_",
                        onSelect = { selected ->
                            note2Text = selected.text
                            showNote2Suggestions = false
                            focusManager.clearFocus()
                        }
                    )
                }
            ) {
                FormFieldRow(
                    label = "Note 2",
                    labelWidth = labelWidth,
                    isActive = note2FieldFocused,
                    activeAccent = accentColor,
                    onClick = { activePicker = ActivePicker.NONE },
                    trailing = if (note2Text.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = {
                                    note2Text = ""
                                    showNote2Suggestions = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear note 2",
                                    tint = LedgerTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null
                ) {
                    BasicTextField(
                        value = note2Text,
                        onValueChange = { typed ->
                            note2Text = typed.take(140)
                            showNote2Suggestions = typed.trim().isNotEmpty()
                            activePicker = ActivePicker.NONE
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            if (note2Text.isEmpty()) {
                                Text("Enter note 2 (optional)", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                note2FieldFocused = state.isFocused
                                if (state.isFocused) {
                                    activePicker = ActivePicker.NONE
                                    dismissAllSuggestions()
                                } else {
                                    showNote2Suggestions = false
                                }
                            }
                            .testTag("tx_note2_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description Field
            BasicTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it.take(500) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = LedgerTextPrimary),
                minLines = 2,
                maxLines = 4,
                cursorBrush = SolidColor(accentColor),
                decorationBox = { inner ->
                    Column {
                        if (descriptionText.isEmpty()) {
                            Text("Description", style = MaterialTheme.typography.bodyMedium, color = LedgerTextMuted)
                        }
                        inner()
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = LedgerDivider)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            AnimatedVisibility(
                visible = validationError != null,
                enter = suggestionEnter,
                exit = suggestionExit
            ) {
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerCoral,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save + Copy + Delete Equal-Size Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { submit(keepOpen = false) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = saveButtonTextColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("tx_save_button")
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onCopy != null) {
                    OutlinedButton(
                        onClick = { startCopyFlow() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        border = BorderStroke(1.dp, LedgerBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerTextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tx_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onDelete != null || existing == null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        border = BorderStroke(1.dp, LedgerCoral.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerCoral),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tx_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            }
        }

        // Bottom Interactive Picker Drawer (Smooth slide-in/out & responsive height)
        AnimatedVisibility(
            visible = activePicker != ActivePicker.NONE,
            enter = slideInVertically(LedgerMotion.snappyTween(140)) { it } + fadeIn(LedgerMotion.microTween(110)),
            exit = slideOutVertically(LedgerMotion.microTween(115)) { it } + fadeOut(LedgerMotion.microTween(90))
        ) {
            val shownPicker = if (activePicker != ActivePicker.NONE) activePicker else lastVisiblePicker
            Surface(
                color = LedgerSurface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pickerHeight)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LedgerSurfaceHighlight)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (shownPicker) {
                                ActivePicker.ACCOUNT -> if (txType == TransactionType.TRANSFER) "From Account" else "Accounts"
                                ActivePicker.TO_ACCOUNT -> "To Account"
                                ActivePicker.CATEGORY -> if (txType == TransactionType.INCOME) {
                                    "Income Categories & Ledgers"
                                } else {
                                    "Expense Categories & Ledgers"
                                }
                                ActivePicker.NONE -> ""
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = LedgerTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (shownPicker == ActivePicker.CATEGORY && onCreateCategoryLedger != null) {
                            Surface(
                                onClick = {
                                    draftLedgerNameForDialog = ""
                                    draftCategoryIdForDialog = expandedCategoryId
                                        ?: categoryId.takeIf { it.isNotBlank() }
                                    showCreateLedgerDialog = true
                                },
                                shape = RoundedCornerShape(7.dp),
                                color = accentColor.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("picker_create_category_ledger_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Create ledger",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Ledger",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                if (shownPicker == ActivePicker.CATEGORY) {
                                    onManageCategories(
                                        if (txType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                                    )
                                } else {
                                    onManageAccounts()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Manage", tint = LedgerTextSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { activePicker = ActivePicker.NONE },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close picker", tint = LedgerTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    when (shownPicker) {
                        ActivePicker.ACCOUNT, ActivePicker.TO_ACCOUNT -> {
                            val sections = remember(bundle.accountSections) {
                                bundle.accountSections.filter { it.accounts.isNotEmpty() }
                            }
                            val selectedGroup = sections.firstOrNull { it.group.id == expandedGroupId }
                                ?: sections.firstOrNull()

                            Row(modifier = Modifier.fillMaxSize()) {
                                // Left pane: Account Groups
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(LedgerBackground)
                                ) {
                                    items(sections, key = { it.group.id }) { sec ->
                                        val isCurrentGroup = selectedGroup?.group?.id == sec.group.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isCurrentGroup) LedgerSurface else Color.Transparent)
                                                .clickable { expandedGroupId = sec.group.id }
                                                .padding(horizontal = 14.dp, vertical = 13.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = sec.group.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isCurrentGroup) LedgerTextPrimary else LedgerTextSecondary,
                                                fontWeight = if (isCurrentGroup) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = LedgerTextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                    }
                                }

                                Box(modifier = Modifier.width(1.dp).fillMaxSize().background(LedgerDivider))

                                // Right pane: Accounts in selected group
                                LazyColumn(modifier = Modifier.weight(1.1f).background(LedgerSurface)) {
                                    val accs = selectedGroup?.accounts.orEmpty()
                                    items(accs, key = { it.account.id }) { item ->
                                        val isSelected = if (shownPicker == ActivePicker.ACCOUNT) {
                                            accountId == item.account.id
                                        } else {
                                            toAccountId == item.account.id
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                                                .clickable {
                                                    if (shownPicker == ActivePicker.ACCOUNT) {
                                                        accountId = item.account.id
                                                        accountQuery = item.account.name
                                                        activePicker = if (txType == TransactionType.TRANSFER) {
                                                            ActivePicker.TO_ACCOUNT
                                                        } else {
                                                            ActivePicker.CATEGORY
                                                        }
                                                    } else {
                                                        toAccountId = item.account.id
                                                        toAccountQuery = item.account.name
                                                        activePicker = ActivePicker.NONE
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.account.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSelected) accentColor else LedgerTextPrimary,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }

                        ActivePicker.CATEGORY -> {
                            val selectedCat: Category? = filteredCategories.firstOrNull { it.id == expandedCategoryId }
                                ?: filteredCategories.firstOrNull { it.id == categoryId }
                                ?: filteredCategories.firstOrNull()

                            Row(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(LedgerBackground)
                                ) {
                                    items(filteredCategories, key = { it.id }) { cat ->
                                        val isCurr = selectedCat?.id == cat.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isCurr) LedgerSurface else Color.Transparent)
                                                .clickable {
                                                    expandedCategoryId = cat.id
                                                    categoryId = cat.id
                                                    if (!bundle.settings.subcategoryEnabled || cat.subcategories.isEmpty()) {
                                                        subcategory = ""
                                                        categoryQuery = cat.name
                                                        activePicker = ActivePicker.NONE
                                                    } else {
                                                        subcategory = ""
                                                        categoryQuery = cat.name
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 13.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(LedgerFormatters.parseColorHex(cat.colorHex))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isCurr) LedgerTextPrimary else LedgerTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (bundle.settings.subcategoryEnabled && cat.subcategories.isNotEmpty()) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = LedgerTextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                    }
                                }

                                if (bundle.settings.subcategoryEnabled && selectedCat != null) {
                                    Box(modifier = Modifier.width(1.dp).fillMaxSize().background(LedgerDivider))
                                    LazyColumn(modifier = Modifier.weight(1f).background(LedgerSurface)) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        categoryId = selectedCat.id
                                                        subcategory = ""
                                                        categoryQuery = formatCategoryLabel(selectedCat.id, "")
                                                        activePicker = ActivePicker.NONE
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 13.dp)
                                            ) {
                                                Text(
                                                    text = "${selectedCat.name} (General)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = LedgerTextSecondary
                                                )
                                            }
                                            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                        }
                                        items(selectedCat.subcategories) { sub ->
                                            val isSubSelected = categoryId == selectedCat.id && subcategory == sub
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (isSubSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                                                    .clickable {
                                                        categoryId = selectedCat.id
                                                        subcategory = sub
                                                        categoryQuery = formatCategoryLabel(selectedCat.id, sub)
                                                        activePicker = ActivePicker.NONE
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 13.dp)
                                            ) {
                                                Text(
                                                    text = sub,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isSubSelected) accentColor else LedgerTextPrimary
                                                )
                                            }
                                            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                        }
                                        if (onCreateCategoryLedger != null) {
                                            item {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(LedgerSurfaceElevated.copy(alpha = 0.6f))
                                                        .clickable {
                                                            draftLedgerNameForDialog = ""
                                                            draftCategoryIdForDialog = selectedCat.id
                                                            showCreateLedgerDialog = true
                                                        }
                                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                                        .testTag("picker_add_ledger_under_${selectedCat.id}"),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Add Ledger",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = accentColor,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ActivePicker.NONE -> Unit
                    }
                }
            }
        }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LedgerFormatters.epochDayToMillis(epochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            epochDay = millis / 86_400_000L
                        }
                        showDatePicker = false
                    }
                ) { Text("OK", color = LedgerCoral) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showCopyOptionsDialog && onCopy != null) {
        val todayDay = LedgerFormatters.todayEpochDay()
        AlertDialog(
            onDismissRequest = { showCopyOptionsDialog = false },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = "Copy Entry",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose a date to open this copy as a draft:",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerTextSecondary
                    )
                    CopyDateOptionRow(
                        icon = Icons.Filled.Event,
                        title = "Same Date",
                        subtitle = LedgerFormatters.formatFullDateInput(epochDay),
                        accentColor = accentColor,
                        testTag = "tx_copy_same_date_option",
                        onClick = { openCopiedDraft(epochDay) }
                    )
                    CopyDateOptionRow(
                        icon = Icons.Filled.Today,
                        title = "Today's Date",
                        subtitle = LedgerFormatters.formatFullDateInput(todayDay),
                        accentColor = LedgerBlue,
                        testTag = "tx_copy_today_option",
                        onClick = { openCopiedDraft(todayDay) }
                    )
                    CopyDateOptionRow(
                        icon = Icons.Filled.DateRange,
                        title = "Choose Date from Calendar",
                        subtitle = "Pick a custom date",
                        accentColor = LedgerAmber,
                        testTag = "tx_copy_calendar_option",
                        onClick = {
                            showCopyOptionsDialog = false
                            showCopyDatePicker = true
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showCopyOptionsDialog = false },
                    modifier = Modifier.testTag("tx_copy_options_cancel_button")
                ) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        )
    }

    if (showCopyDatePicker && onCopy != null) {
        val copyDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LedgerFormatters.epochDayToMillis(LedgerFormatters.todayEpochDay())
        )
        DatePickerDialog(
            onDismissRequest = { showCopyDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosenMillis = copyDatePickerState.selectedDateMillis
                        if (chosenMillis != null) {
                            val chosenEpochDay = chosenMillis / 86_400_000L
                            openCopiedDraft(chosenEpochDay)
                        }
                    },
                    enabled = copyDatePickerState.selectedDateMillis != null,
                    modifier = Modifier.testTag("tx_copy_date_confirm_button")
                ) { Text("Open Draft", color = LedgerCoral, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCopyDatePicker = false },
                    modifier = Modifier.testTag("tx_copy_date_cancel_button")
                ) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        ) {
            DatePicker(
                state = copyDatePickerState,
                title = {
                    Text(
                        text = "Choose date for draft copy",
                        style = MaterialTheme.typography.labelLarge,
                        color = LedgerTextSecondary,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                }
            )
        }
    }

    if (showRepeatDialog) {
        var draftFreq by remember(showRepeatDialog) { mutableStateOf(repeatFreq) }
        var draftDay by remember(showRepeatDialog) {
            val fallbackDay = LedgerFormatters.epochDayToCalendar(epochDay)
                .get(java.util.Calendar.DAY_OF_MONTH)
            mutableStateOf(
                if (repeatFreq == RepeatFrequency.SPECIFIC_DATE_OF_MONTH) repeatDayOfMonth else fallbackDay
            )
        }
        var draftCustomCountText by remember(showRepeatDialog) {
            mutableStateOf(repeatCustomCount.coerceIn(1, 365).toString())
        }
        var draftCustomUnit by remember(showRepeatDialog) {
            mutableStateOf(repeatCustomUnit)
        }
        val parsedCustomCount = draftCustomCountText.trim().toIntOrNull()?.coerceIn(1, 365) ?: 0
        val customPreviewLabel = if (parsedCustomCount > 0) {
            val unitWord = if (parsedCustomCount == 1) draftCustomUnit.singular else draftCustomUnit.label
            "Every $parsedCustomCount $unitWord"
        } else {
            "Enter a number (1–365)"
        }
        val needsExplicitConfirm = draftFreq == RepeatFrequency.SPECIFIC_DATE_OF_MONTH ||
            draftFreq == RepeatFrequency.CUSTOM

        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = "Repeat / Installment",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    RepeatFrequency.entries.forEach { freq ->
                        val isSelected = draftFreq == freq
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    draftFreq = freq
                                    if (freq != RepeatFrequency.SPECIFIC_DATE_OF_MONTH && freq != RepeatFrequency.CUSTOM) {
                                        repeatFreq = freq
                                        showRepeatDialog = false
                                    }
                                }
                                .padding(vertical = 11.dp, horizontal = 8.dp)
                                .testTag("repeat_option_${freq.name.lowercase()}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = freq.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) accentColor else LedgerTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    text = when {
                                        freq == RepeatFrequency.SPECIFIC_DATE_OF_MONTH && isSelected ->
                                            "Repeats on the ${RepeatFrequency.ordinalDay(draftDay)} of every month"
                                        freq == RepeatFrequency.CUSTOM && isSelected ->
                                            if (parsedCustomCount > 0) "Repeats $customPreviewLabel" else freq.description
                                        else -> freq.description
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerTextMuted
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "Selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Inline Day-of-Month selector (1..31) when Specific Date of Month is chosen
                        if (freq == RepeatFrequency.SPECIFIC_DATE_OF_MONTH && isSelected) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LedgerSurfaceElevated,
                                border = BorderStroke(1.dp, LedgerBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Select Day of Month (${RepeatFrequency.ordinalDay(draftDay)})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LedgerTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        for (day in 1..31) {
                                            val daySelected = draftDay == day
                                            Surface(
                                                onClick = { draftDay = day },
                                                shape = RoundedCornerShape(7.dp),
                                                color = if (daySelected) accentColor else LedgerSurface,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (daySelected) accentColor else LedgerBorder
                                                ),
                                                modifier = Modifier
                                                    .size(width = 34.dp, height = 32.dp)
                                                    .testTag("repeat_day_$day")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = day.toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (daySelected) saveButtonTextColor else LedgerTextPrimary,
                                                        fontWeight = if (daySelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = "For shorter months (e.g. Feb), days 29–31 apply on the month's last day.",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = LedgerTextMuted
                                    )
                                }
                            }
                        }

                        // Inline Custom Interval selector (Number + Days/Weeks/Months/Years)
                        if (freq == RepeatFrequency.CUSTOM && isSelected) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LedgerSurfaceElevated,
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Repeat Every",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LedgerTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            onClick = {
                                                val next = ((parsedCustomCount.takeIf { it > 0 } ?: 1) - 1).coerceAtLeast(1)
                                                draftCustomCountText = next.toString()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = LedgerSurface,
                                            border = BorderStroke(1.dp, LedgerBorder),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .testTag("repeat_custom_minus_button")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "−",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = LedgerTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        OutlinedTextField(
                                            value = draftCustomCountText,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }.take(3)
                                                draftCustomCountText = digits
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            placeholder = { Text("e.g. 2", color = LedgerTextMuted) },
                                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                                color = LedgerTextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("repeat_custom_number_input")
                                        )
                                        Surface(
                                            onClick = {
                                                val next = ((parsedCustomCount.takeIf { it > 0 } ?: 0) + 1).coerceAtMost(365)
                                                draftCustomCountText = next.toString()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = LedgerSurface,
                                            border = BorderStroke(1.dp, LedgerBorder),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .testTag("repeat_custom_plus_button")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "+",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = LedgerTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Unit",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LedgerTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CustomRepeatUnit.entries.forEach { unit ->
                                            val unitSelected = draftCustomUnit == unit
                                            Surface(
                                                onClick = { draftCustomUnit = unit },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (unitSelected) accentColor else LedgerSurface,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (unitSelected) accentColor else LedgerBorder
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 40.dp)
                                                    .testTag("repeat_custom_unit_${unit.name.lowercase()}")
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = unit.label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (unitSelected) saveButtonTextColor else LedgerTextPrimary,
                                                        fontWeight = if (unitSelected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            },
            confirmButton = {
                when (draftFreq) {
                    RepeatFrequency.SPECIFIC_DATE_OF_MONTH -> {
                        Button(
                            onClick = {
                                repeatFreq = RepeatFrequency.SPECIFIC_DATE_OF_MONTH
                                repeatDayOfMonth = draftDay
                                showRepeatDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = saveButtonTextColor
                            ),
                            modifier = Modifier.testTag("repeat_apply_specific_day_button")
                        ) {
                            Text("Set ${RepeatFrequency.ordinalDay(draftDay)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    RepeatFrequency.CUSTOM -> {
                        Button(
                            onClick = {
                                if (parsedCustomCount > 0) {
                                    repeatFreq = RepeatFrequency.CUSTOM
                                    repeatCustomCount = parsedCustomCount
                                    repeatCustomUnit = draftCustomUnit
                                    showRepeatDialog = false
                                }
                            },
                            enabled = parsedCustomCount > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = saveButtonTextColor
                            ),
                            modifier = Modifier.testTag("repeat_apply_custom_button")
                        ) {
                            Text(
                                text = if (parsedCustomCount > 0) "Set $customPreviewLabel" else "Apply Custom",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    else -> {
                        TextButton(onClick = { showRepeatDialog = false }) {
                            Text("Close", color = LedgerTextSecondary)
                        }
                    }
                }
            },
            dismissButton = if (needsExplicitConfirm) {
                {
                    TextButton(onClick = { showRepeatDialog = false }) {
                        Text("Cancel", color = LedgerTextSecondary)
                    }
                }
            } else null
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = if (isDraftCopy) "Discard Draft?" else "Delete Entry?",
                    color = LedgerTextPrimary
                )
            },
            text = {
                Text(
                    text = when {
                        existing != null -> "This transaction will be permanently deleted."
                        isDraftCopy -> "Discard this draft copy? The original entry will remain unchanged."
                        else -> "Discard this unsaved entry and close?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        if (existing != null && onDelete != null) {
                            onDelete(existing.id)
                        } else {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LedgerCoral,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("tx_delete_confirm_button")
                ) {
                    Text(if (isDraftCopy) "Discard" else "Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("tx_delete_cancel_button")
                ) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        )
    }

    if (showCreateLedgerDialog && onCreateCategoryLedger != null) {
        CreateCategoryLedgerDialog(
            bundle = bundle,
            initialType = if (txType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE,
            initialCategoryId = draftCategoryIdForDialog ?: categoryId.takeIf { it.isNotBlank() },
            initialLedgerName = draftLedgerNameForDialog,
            lockType = true,
            onDismiss = { showCreateLedgerDialog = false },
            onConfirm = { targetCatId, newLedgerName ->
                showCreateLedgerDialog = false
                onCreateCategoryLedger(targetCatId, newLedgerName) { createdCatId, createdName ->
                    categoryId = createdCatId
                    subcategory = createdName
                    val catName = bundle.categoryById(createdCatId)?.name.orEmpty()
                    categoryQuery = if (catName.isNotBlank()) "$catName / $createdName" else createdName
                    hasTypedCategoryQuery = false
                    showCategorySuggestions = false
                    activePicker = ActivePicker.NONE
                    validationError = null
                }
            }
        )
    }
}

@Composable
private fun CopyDateOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = LedgerTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FormFieldRow(
    label: String,
    labelWidth: Dp = 76.dp,
    isActive: Boolean,
    activeAccent: Color,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val dividerColor by animateColorAsState(
        targetValue = if (isActive) activeAccent else LedgerDivider,
        animationSpec = LedgerMotion.microTween(100),
        label = "field_div_color_$label"
    )
    val dividerThickness by animateDpAsState(
        targetValue = if (isActive) 1.5.dp else 1.dp,
        animationSpec = LedgerMotion.microTween(100),
        label = "field_div_thick_$label"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerTextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(labelWidth)
            )
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(6.dp))
                trailing()
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(labelWidth))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                HorizontalDivider(
                    color = dividerColor,
                    thickness = dividerThickness,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private class FieldDropdownPositionProvider(
    private val startIndentPx: Int,
    private val verticalGapPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.left + startIndentPx).coerceIn(0, maxX)
        val belowY = anchorBounds.bottom + verticalGapPx
        val aboveY = anchorBounds.top - popupContentSize.height - verticalGapPx
        val fitsBelow = belowY + popupContentSize.height <= windowSize.height
        val y = when {
            fitsBelow -> belowY
            aboveY >= 0 -> aboveY
            else -> belowY.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        }
        return IntOffset(x, y)
    }
}

@Composable
private fun InlineCompactTagBox(
    value: String,
    onValueChange: (String) -> Unit,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    accentColor: Color,
    inputTestTag: String
) {
    val hasValue = value.isNotBlank()
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> accentColor
            hasValue -> accentColor.copy(alpha = 0.65f)
            else -> LedgerBorder
        },
        animationSpec = LedgerMotion.microTween(100),
        label = "inline_tag_border_$inputTestTag"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> accentColor.copy(alpha = 0.14f)
            hasValue -> accentColor.copy(alpha = 0.10f)
            else -> LedgerSurfaceElevated
        },
        animationSpec = LedgerMotion.microTween(100),
        label = "inline_tag_bg_$inputTestTag"
    )
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = bgColor,
        border = BorderStroke(if (isFocused) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .width(46.dp)
            .height(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.labelLarge.copy(
                    color = if (hasValue || isFocused) accentColor else LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                singleLine = true,
                cursorBrush = SolidColor(accentColor),
                decorationBox = { inner ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = LedgerTextMuted,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChanged(it.isFocused) }
                    .testTag(inputTestTag)
            )
        }
    }
}

@Composable
private fun FieldWithFloatingSuggestions(
    showSuggestions: Boolean,
    startIndent: Dp,
    minDropdownWidth: Dp = 180.dp,
    modifier: Modifier = Modifier.fillMaxWidth(),
    suggestionsContent: @Composable () -> Unit,
    fieldContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    val startIndentPx = with(density) { startIndent.roundToPx() }
    val verticalGapPx = with(density) { 4.dp.roundToPx() }
    val positionProvider = remember(startIndentPx, verticalGapPx) {
        FieldDropdownPositionProvider(
            startIndentPx = startIndentPx,
            verticalGapPx = verticalGapPx
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged { anchorSize = it }
    ) {
        fieldContent()

        if (showSuggestions && anchorSize.width > 0) {
            val dropdownWidth = with(density) {
                (anchorSize.width.toDp() - startIndent).coerceAtLeast(minDropdownWidth)
            }
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = true
                )
            ) {
                Box(modifier = Modifier.width(dropdownWidth)) {
                    suggestionsContent()
                }
            }
        }
    }
}

@Composable
private fun AccountSuggestionList(
    suggestions: List<AccountSuggestionItem>,
    selectedAccountId: String,
    accentColor: Color,
    symbol: String,
    onSelect: (AccountSuggestionItem) -> Unit,
    onBrowseAll: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LedgerSurfaceElevated,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_account_suggestions_list")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 210.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (suggestions.isEmpty()) {
                    Text(
                        text = "No matching ledgers found",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerTextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                } else {
                    suggestions.forEachIndexed { idx, item ->
                        val isSelected = item.accountId == selectedAccountId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) accentColor.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable { onSelect(item) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("tx_account_suggestion_${item.accountId}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = item.accountName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) accentColor else LedgerTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.groupName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = LedgerFormatters.formatSignedWhole(item.balance, symbol),
                                style = TabularAmountStyle.copy(fontSize = 12.sp),
                                color = if (item.balance.roundToLong() < 0L) LedgerCoral else LedgerBlue
                            )
                        }
                        if (idx < suggestions.lastIndex) {
                            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                        }
                    }
                }
            }
            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurface)
                    .clickable(onClick = onBrowseAll)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("tx_browse_all_accounts_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Browse all accounts by group",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CategorySuggestionList(
    suggestions: List<CategorySuggestionItem>,
    selectedCategoryId: String,
    selectedSubcategory: String,
    queryText: String,
    txType: TransactionType,
    accentColor: Color,
    onSelect: (CategorySuggestionItem) -> Unit,
    onCreateNewLedger: ((String) -> Unit)?,
    onBrowseAll: () -> Unit
) {
    val cleanQuery = queryText.trim()
    val kindLabel = if (txType == TransactionType.INCOME) "Income" else "Expense"
    val hasExactMatch = cleanQuery.isNotEmpty() && suggestions.any {
        it.displayLabel.equals(cleanQuery, ignoreCase = true) ||
            it.categoryName.equals(cleanQuery, ignoreCase = true) ||
            (it.subcategory?.equals(cleanQuery, ignoreCase = true) == true)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LedgerSurfaceElevated,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_category_suggestions_list")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (suggestions.isEmpty()) {
                    Text(
                        text = "No matching ${kindLabel.lowercase()} ledgers found",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerTextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                } else {
                    suggestions.forEachIndexed { idx, item ->
                        val isSelected = item.categoryId == selectedCategoryId &&
                            item.subcategory.orEmpty() == selectedSubcategory
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) accentColor.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable { onSelect(item) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("tx_category_suggestion_${item.categoryId}_${item.subcategory.orEmpty()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(LedgerFormatters.parseColorHex(item.colorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.displayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) accentColor else LedgerTextPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (idx < suggestions.lastIndex) {
                            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                        }
                    }
                }
            }
            if (onCreateNewLedger != null) {
                HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.08f))
                        .clickable { onCreateNewLedger(if (hasExactMatch) "" else cleanQuery) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("tx_create_category_ledger_suggestion_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (cleanQuery.isNotEmpty() && !hasExactMatch) {
                            "Create \"$cleanQuery\" as $kindLabel Ledger"
                        } else {
                            "Create New $kindLabel Ledger"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurface)
                    .clickable(onClick = onBrowseAll)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("tx_browse_all_categories_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Browse all categories & ledgers",
                    style = MaterialTheme.typography.labelMedium,
                    color = LedgerTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = LedgerTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TextSuggestionList(
    suggestions: List<TextSuggestionItem>,
    currentValue: String,
    accentColor: Color,
    listTestTag: String,
    itemTestTagPrefix: String,
    onSelect: (TextSuggestionItem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LedgerSurfaceElevated,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(listTestTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
        ) {
            suggestions.forEachIndexed { idx, item ->
                val isExact = item.text.equals(currentValue.trim(), ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isExact) accentColor.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelect(item) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("$itemTestTagPrefix$idx"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isExact) accentColor else LedgerTextPrimary,
                        fontWeight = if (isExact) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    if (item.usageCount > 1) {
                        Text(
                            text = "${item.usageCount}×",
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerTextMuted
                        )
                    }
                }
                if (idx < suggestions.lastIndex) {
                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                }
            }
        }
    }
}
