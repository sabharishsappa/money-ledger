package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Category
import com.example.data.CategoryType
import com.example.data.LedgerTransaction
import com.example.data.RepeatFrequency
import com.example.data.TransactionType
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
import com.example.viewmodel.LedgerDataBundle
import kotlin.math.roundToLong

private enum class ActivePicker {
    NONE,
    ACCOUNT,
    TO_ACCOUNT,
    CATEGORY
}

private data class AccountSuggestionItem(
    val accountId: String,
    val accountName: String,
    val groupName: String,
    val balance: Double
)

private data class CategorySuggestionItem(
    val categoryId: String,
    val categoryName: String,
    val subcategory: String?,
    val colorHex: String,
    val displayLabel: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    bundle: LedgerDataBundle,
    existing: LedgerTransaction?,
    prefillType: TransactionType? = null,
    prefillCategoryId: String? = null,
    prefillSubcategory: String? = null,
    onBack: () -> Unit,
    onSave: (existingId: String?, tx: LedgerTransaction, keepOpen: Boolean) -> Unit,
    onCopy: ((LedgerTransaction) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    onManageAccounts: () -> Unit,
    onManageCategories: (CategoryType) -> Unit,
    onCreateCategoryLedger: ((categoryId: String, ledgerName: String, onCreated: (String, String) -> Unit) -> Unit)? = null
) {
    BackHandler { onBack() }

    val focusManager = LocalFocusManager.current
    val symbol = bundle.settings.mainCurrencySymbol

    var txType by remember(existing, prefillType) {
        mutableStateOf(existing?.txType() ?: prefillType ?: TransactionType.EXPENSE)
    }
    var epochDay by remember(existing) {
        mutableStateOf(existing?.epochDay ?: LedgerFormatters.todayEpochDay())
    }
    var accountId by remember(existing) {
        mutableStateOf(existing?.accountId.orEmpty())
    }
    var toAccountId by remember(existing) {
        mutableStateOf(existing?.toAccountId.orEmpty())
    }
    var categoryId by remember(existing, prefillCategoryId) {
        mutableStateOf(existing?.categoryId ?: prefillCategoryId.orEmpty())
    }
    var subcategory by remember(existing, prefillSubcategory) {
        mutableStateOf(existing?.subcategory ?: prefillSubcategory.orEmpty())
    }

    // Type-ahead input queries synced with selected account / category
    var accountQuery by remember(existing) {
        val initialAccId = existing?.accountId.orEmpty()
        mutableStateOf(if (initialAccId.isNotBlank()) bundle.accountName(initialAccId) else "")
    }
    var toAccountQuery by remember(existing) {
        val initialToId = existing?.toAccountId.orEmpty()
        mutableStateOf(if (initialToId.isNotBlank()) bundle.accountName(initialToId) else "")
    }
    var categoryQuery by remember(existing, prefillCategoryId, prefillSubcategory) {
        val initialCatId = existing?.categoryId ?: prefillCategoryId.orEmpty()
        val initialSub = existing?.subcategory ?: prefillSubcategory.orEmpty()
        val catName = bundle.categoryById(initialCatId)?.name.orEmpty()
        val label = when {
            catName.isBlank() -> ""
            initialSub.isNotBlank() && (bundle.settings.subcategoryEnabled || !prefillSubcategory.isNullOrBlank()) -> "$catName / $initialSub"
            else -> catName
        }
        mutableStateOf(label)
    }

    var accountFieldFocused by remember { mutableStateOf(false) }
    var toAccountFieldFocused by remember { mutableStateOf(false) }
    var categoryFieldFocused by remember { mutableStateOf(false) }
    var showAccountSuggestions by remember { mutableStateOf(false) }
    var showToAccountSuggestions by remember { mutableStateOf(false) }
    var showCategorySuggestions by remember { mutableStateOf(false) }
    var hasTypedAccountQuery by remember { mutableStateOf(false) }
    var hasTypedToAccountQuery by remember { mutableStateOf(false) }
    var hasTypedCategoryQuery by remember { mutableStateOf(false) }

    var amountText by remember(existing) {
        mutableStateOf(if (existing != null) existing.amount.toString() else "")
    }
    var showFeeField by remember(existing) {
        mutableStateOf((existing?.feeAmount ?: 0.0) > 0.0)
    }
    var feeText by remember(existing) {
        mutableStateOf(if ((existing?.feeAmount ?: 0.0) > 0.0) existing!!.feeAmount.toString() else "")
    }
    var noteText by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    var taggingText by remember(existing) { mutableStateOf(existing?.effectiveTagging().orEmpty()) }
    var descriptionText by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var favorite by remember(existing) { mutableStateOf(existing?.favorite ?: false) }
    var repeatFreq by remember(existing) { mutableStateOf(existing?.repeat() ?: RepeatFrequency.NONE) }
    var repeatDayOfMonth by remember(existing) {
        val defaultDay = existing?.repeatDayOfMonth()
            ?: LedgerFormatters.epochDayToCalendar(existing?.epochDay ?: LedgerFormatters.todayEpochDay())
                .get(java.util.Calendar.DAY_OF_MONTH)
        mutableStateOf(defaultDay.coerceIn(1, 31))
    }

    var activePicker by remember { mutableStateOf(ActivePicker.NONE) }
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCopyDatePicker by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreateLedgerDialog by remember { mutableStateOf(false) }
    var draftLedgerNameForDialog by remember { mutableStateOf("") }
    var draftCategoryIdForDialog by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val accentColor = when (txType) {
        TransactionType.INCOME -> LedgerBlue
        TransactionType.EXPENSE -> LedgerCoral
        TransactionType.TRANSFER -> LedgerTextPrimary
    }

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
        showAccountSuggestions = false
        showToAccountSuggestions = false
        showCategorySuggestions = false
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
                validationError = "Source and destination accounts must be different"
                return null
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
            tagging = taggingText.trim().takeIf { it.isNotBlank() },
            description = descriptionText.trim().takeIf { it.isNotBlank() },
            tags = emptyList(),
            favorite = favorite,
            repeatFrequency = RepeatFrequency.encode(repeatFreq, repeatDayOfMonth)
        )
    }

    fun submit(keepOpen: Boolean) {
        val model = buildValidatedTransaction() ?: return
        onSave(existing?.id, model, keepOpen)
        if (keepOpen) {
            amountText = ""
            feeText = ""
            noteText = ""
            taggingText = ""
            descriptionText = ""
        }
    }

    fun startCopyFlow() {
        if (buildValidatedTransaction(clearId = true) != null) {
            activePicker = ActivePicker.NONE
            showCopyDatePicker = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("tx_editor_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LedgerTextPrimary
                )
            }
            Text(
                text = when (txType) {
                    TransactionType.INCOME -> "Income"
                    TransactionType.EXPENSE -> "Expense"
                    TransactionType.TRANSFER -> "Transfer"
                },
                style = MaterialTheme.typography.titleLarge,
                color = LedgerTextPrimary,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            IconButton(
                onClick = { favorite = !favorite },
                modifier = Modifier.testTag("tx_editor_favorite_button")
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Toggle favorite",
                    tint = if (favorite) LedgerAmber else LedgerTextSecondary
                )
            }
            if (existing != null && onCopy != null) {
                IconButton(
                    onClick = { startCopyFlow() },
                    modifier = Modifier.testTag("tx_editor_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy entry",
                        tint = LedgerTextPrimary
                    )
                }
            }
            if (existing != null && onDelete != null) {
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.testTag("tx_editor_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete transaction",
                        tint = LedgerCoral
                    )
                }
            }
        }

        // Scrollable Form Area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 3-Segment Type Switcher (Income | Expense | Transfer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.entries.forEach { option ->
                    val isSelected = txType == option
                    val borderColor = when {
                        !isSelected -> LedgerBorder
                        option == TransactionType.INCOME -> LedgerBlue
                        option == TransactionType.EXPENSE -> LedgerCoral
                        else -> LedgerTextPrimary
                    }
                    val textColor = when {
                        !isSelected -> LedgerTextSecondary
                        option == TransactionType.INCOME -> LedgerBlue
                        option == TransactionType.EXPENSE -> LedgerCoral
                        else -> LedgerTextPrimary
                    }
                    val label = when (option) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.EXPENSE -> "Expense"
                        TransactionType.TRANSFER -> "Transfer"
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) LedgerSurfaceElevated else LedgerSurface,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable {
                                txType = option
                                showAccountSuggestions = false
                                showToAccountSuggestions = false
                                showCategorySuggestions = false
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
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date + Rep/Inst Row
            FormFieldRow(
                label = "Date",
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
                        val encodedRepeat = RepeatFrequency.encode(repeatFreq, repeatDayOfMonth)
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

            // Account / From & Category / To Rows (Type-to-suggest + Select Picker)
            if (txType == TransactionType.TRANSFER) {
                FormFieldRow(
                    label = "From",
                    isActive = activePicker == ActivePicker.ACCOUNT || accountFieldFocused,
                    activeAccent = accentColor,
                    onClick = { openPicker(ActivePicker.ACCOUNT) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (accountQuery.isNotEmpty() && accountFieldFocused) {
                                IconButton(
                                    onClick = {
                                        accountQuery = ""
                                        accountId = ""
                                        showAccountSuggestions = true
                                    },
                                    modifier = Modifier.size(30.dp)
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
                                modifier = Modifier.size(32.dp).testTag("tx_from_account_picker_button")
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
                                },
                                modifier = Modifier.size(32.dp).testTag("tx_swap_accounts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SwapVert,
                                    contentDescription = "Swap accounts",
                                    tint = LedgerTextSecondary
                                )
                            }
                        }
                    }
                ) {
                    BasicTextField(
                        value = accountQuery,
                        onValueChange = { typed ->
                            accountQuery = typed.take(80)
                            hasTypedAccountQuery = true
                            val exact = allAccountSuggestions.firstOrNull {
                                it.accountName.equals(typed.trim(), ignoreCase = true)
                            }
                            accountId = exact?.accountId.orEmpty()
                            showAccountSuggestions = true
                            activePicker = ActivePicker.NONE
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            if (accountQuery.isEmpty()) {
                                Text("Type or select source ledger", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
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
                                    showToAccountSuggestions = false
                                    showCategorySuggestions = false
                                    showAccountSuggestions = true
                                } else {
                                    showAccountSuggestions = false
                                }
                            }
                            .testTag("tx_from_account_input")
                    )
                }

                if (showAccountSuggestions) {
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

                FormFieldRow(
                    label = "To",
                    isActive = activePicker == ActivePicker.TO_ACCOUNT || toAccountFieldFocused,
                    activeAccent = accentColor,
                    onClick = { openPicker(ActivePicker.TO_ACCOUNT) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (toAccountQuery.isNotEmpty() && toAccountFieldFocused) {
                                IconButton(
                                    onClick = {
                                        toAccountQuery = ""
                                        toAccountId = ""
                                        hasTypedToAccountQuery = true
                                        showToAccountSuggestions = true
                                    },
                                    modifier = Modifier.size(30.dp)
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
                                modifier = Modifier.size(32.dp).testTag("tx_to_account_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select destination account",
                                    tint = if (activePicker == ActivePicker.TO_ACCOUNT) accentColor else LedgerTextSecondary
                                )
                            }
                        }
                    }
                ) {
                    BasicTextField(
                        value = toAccountQuery,
                        onValueChange = { typed ->
                            toAccountQuery = typed.take(80)
                            hasTypedToAccountQuery = true
                            val exact = allAccountSuggestions.firstOrNull {
                                it.accountName.equals(typed.trim(), ignoreCase = true)
                            }
                            toAccountId = exact?.accountId.orEmpty()
                            showToAccountSuggestions = true
                            activePicker = ActivePicker.NONE
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            if (toAccountQuery.isEmpty()) {
                                Text("Type or select destination ledger", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
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
                                    showAccountSuggestions = false
                                    showCategorySuggestions = false
                                    showToAccountSuggestions = true
                                } else {
                                    showToAccountSuggestions = false
                                }
                            }
                            .testTag("tx_to_account_input")
                    )
                }

                if (showToAccountSuggestions) {
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
            } else {
                FormFieldRow(
                    label = "Account",
                    isActive = activePicker == ActivePicker.ACCOUNT || accountFieldFocused,
                    activeAccent = accentColor,
                    onClick = { openPicker(ActivePicker.ACCOUNT) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (accountQuery.isNotEmpty() && accountFieldFocused) {
                                IconButton(
                                    onClick = {
                                        accountQuery = ""
                                        accountId = ""
                                        hasTypedAccountQuery = true
                                        showAccountSuggestions = true
                                    },
                                    modifier = Modifier.size(30.dp)
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
                                modifier = Modifier.size(32.dp).testTag("tx_account_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select account",
                                    tint = if (activePicker == ActivePicker.ACCOUNT) accentColor else LedgerTextSecondary
                                )
                            }
                        }
                    }
                ) {
                    BasicTextField(
                        value = accountQuery,
                        onValueChange = { typed ->
                            accountQuery = typed.take(80)
                            hasTypedAccountQuery = true
                            val exact = allAccountSuggestions.firstOrNull {
                                it.accountName.equals(typed.trim(), ignoreCase = true)
                            }
                            accountId = exact?.accountId.orEmpty()
                            showAccountSuggestions = true
                            activePicker = ActivePicker.NONE
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            if (accountQuery.isEmpty()) {
                                Text("Type or select ledger", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
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
                                    showCategorySuggestions = false
                                    showAccountSuggestions = true
                                } else {
                                    showAccountSuggestions = false
                                }
                            }
                            .testTag("tx_account_input")
                    )
                }

                if (showAccountSuggestions) {
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

                FormFieldRow(
                    label = "Category",
                    isActive = activePicker == ActivePicker.CATEGORY || categoryFieldFocused,
                    activeAccent = accentColor,
                    onClick = { openPicker(ActivePicker.CATEGORY) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (categoryQuery.isNotEmpty() && categoryFieldFocused) {
                                IconButton(
                                    onClick = {
                                        categoryQuery = ""
                                        categoryId = ""
                                        subcategory = ""
                                        hasTypedCategoryQuery = true
                                        showCategorySuggestions = true
                                    },
                                    modifier = Modifier.size(30.dp)
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
                                modifier = Modifier.size(32.dp).testTag("tx_category_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select category",
                                    tint = if (activePicker == ActivePicker.CATEGORY) accentColor else LedgerTextSecondary
                                )
                            }
                        }
                    }
                ) {
                    BasicTextField(
                        value = categoryQuery,
                        onValueChange = { typed ->
                            categoryQuery = typed.take(80)
                            hasTypedCategoryQuery = true
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
                            showCategorySuggestions = true
                            activePicker = ActivePicker.NONE
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            if (categoryQuery.isEmpty()) {
                                Text(
                                    text = if (txType == TransactionType.INCOME) {
                                        "Type or select income ledger"
                                    } else {
                                        "Type or select expense ledger"
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
                                    showAccountSuggestions = false
                                    showCategorySuggestions = true
                                } else {
                                    showCategorySuggestions = false
                                }
                            }
                            .testTag("tx_category_input")
                    )
                }

                if (showCategorySuggestions) {
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
            }

            // Amount Row (+ Fees pill for Transfer)
            FormFieldRow(
                label = "Amount",
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

            if (txType == TransactionType.TRANSFER && showFeeField) {
                FormFieldRow(
                    label = "Fees",
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

            // Note Row
            FormFieldRow(
                label = "Note",
                isActive = false,
                activeAccent = accentColor,
                onClick = { activePicker = ActivePicker.NONE }
            ) {
                BasicTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(140) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                    singleLine = true,
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { inner ->
                        if (noteText.isEmpty()) {
                            Text("Enter title or note", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("tx_note_input")
                )
            }

            // Tagging Row (Styled identically to Note, right below Note)
            FormFieldRow(
                label = "Tagging",
                isActive = false,
                activeAccent = accentColor,
                onClick = { activePicker = ActivePicker.NONE },
                trailing = if (taggingText.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { taggingText = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear tagging",
                                tint = LedgerTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null
            ) {
                BasicTextField(
                    value = taggingText,
                    onValueChange = { taggingText = it.take(60) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = LedgerTextPrimary),
                    singleLine = true,
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { inner ->
                        if (taggingText.isEmpty()) {
                            Text("e.g. Home, Personal, Business", style = MaterialTheme.typography.bodyLarge, color = LedgerTextMuted)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("tx_tagging_input")
                )
            }

            // Quick-select chips for existing Tagging buckets
            if (bundle.allTaggings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 78.dp)
                ) {
                    bundle.allTaggings.forEach { existingTagging ->
                        val isSelected = taggingText.equals(existingTagging, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) accentColor.copy(alpha = 0.22f) else LedgerSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) accentColor else LedgerBorder),
                            modifier = Modifier
                                .clickable {
                                    taggingText = if (isSelected) "" else existingTagging
                                }
                                .testTag("tx_tagging_chip_$existingTagging")
                        ) {
                            Text(
                                text = existingTagging,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) accentColor else LedgerTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerCoral
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save + Continue (new) OR Save + Copy + Delete (existing) Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { submit(keepOpen = false) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = saveButtonTextColor
                    ),
                    modifier = Modifier
                        .weight(if (existing == null) 2.4f else 1.35f)
                        .height(48.dp)
                        .testTag("tx_save_button")
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                if (existing == null) {
                    OutlinedButton(
                        onClick = { submit(keepOpen = true) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        border = BorderStroke(1.dp, LedgerBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerTextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tx_continue_button")
                    ) {
                        Text("Continue", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                } else {
                    if (onCopy != null) {
                        OutlinedButton(
                            onClick = { startCopyFlow() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Bottom Interactive Picker Drawer (Matches 00:12 - 00:19)
        if (activePicker != ActivePicker.NONE) {
            Surface(
                color = LedgerSurface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LedgerSurfaceHighlight)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (activePicker) {
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
                        if (activePicker == ActivePicker.CATEGORY && onCreateCategoryLedger != null) {
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
                                if (activePicker == ActivePicker.CATEGORY) {
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

                    when (activePicker) {
                        ActivePicker.ACCOUNT, ActivePicker.TO_ACCOUNT -> {
                            val sections = bundle.accountSections.filter { it.accounts.isNotEmpty() }
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
                                        val isSelected = if (activePicker == ActivePicker.ACCOUNT) {
                                            accountId == item.account.id
                                        } else {
                                            toAccountId == item.account.id
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                                                .clickable {
                                                    if (activePicker == ActivePicker.ACCOUNT) {
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
                                                .padding(horizontal = 14.dp, vertical = 13.dp),
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
                            val copiedModel = buildValidatedTransaction(
                                targetEpochDay = chosenEpochDay,
                                clearId = true
                            )
                            showCopyDatePicker = false
                            if (copiedModel != null) {
                                onCopy(copiedModel)
                            }
                        }
                    },
                    enabled = copyDatePickerState.selectedDateMillis != null,
                    modifier = Modifier.testTag("tx_copy_date_confirm_button")
                ) { Text("Copy", color = LedgerCoral, fontWeight = FontWeight.SemiBold) }
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
                        text = "Choose date for copied entry",
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
                                    if (freq != RepeatFrequency.SPECIFIC_DATE_OF_MONTH) {
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
                                    text = if (freq == RepeatFrequency.SPECIFIC_DATE_OF_MONTH && isSelected) {
                                        "Repeats on the ${RepeatFrequency.ordinalDay(draftDay)} of every month"
                                    } else {
                                        freq.description
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

                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            },
            confirmButton = {
                if (draftFreq == RepeatFrequency.SPECIFIC_DATE_OF_MONTH) {
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
                } else {
                    TextButton(onClick = { showRepeatDialog = false }) {
                        Text("Close", color = LedgerTextSecondary)
                    }
                }
            },
            dismissButton = if (draftFreq == RepeatFrequency.SPECIFIC_DATE_OF_MONTH) {
                {
                    TextButton(onClick = { showRepeatDialog = false }) {
                        Text("Cancel", color = LedgerTextSecondary)
                    }
                }
            } else null
        )
    }

    if (showDeleteConfirmDialog && existing != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = LedgerSurface,
            title = { Text("Delete Entry?", color = LedgerTextPrimary) },
            text = {
                Text(
                    text = "This transaction will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(existing.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LedgerCoral,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("tx_delete_confirm_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
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
private fun FormFieldRow(
    label: String,
    isActive: Boolean,
    activeAccent: Color,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerTextSecondary,
                modifier = Modifier.width(78.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(78.dp))
            HorizontalDivider(
                color = if (isActive) activeAccent else LedgerDivider,
                thickness = if (isActive) 1.5.dp else 1.dp,
                modifier = Modifier.weight(1f)
            )
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
        border = BorderStroke(1.dp, LedgerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 78.dp, top = 4.dp, bottom = 6.dp)
            .testTag("tx_account_suggestions_list")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
        border = BorderStroke(1.dp, LedgerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 78.dp, top = 4.dp, bottom = 6.dp)
            .testTag("tx_category_suggestions_list")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
