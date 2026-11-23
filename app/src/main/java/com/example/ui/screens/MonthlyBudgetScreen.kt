package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Budget
import com.example.data.Category
import com.example.data.CategoryType
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
import com.example.util.IndianNumberVisualTransformation
import com.example.util.LedgerFormatters
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.roundToInt

@Immutable
data class CategoryBudgetProgress(
    val category: Category,
    val budget: Budget,
    val spent: Double,
    val transactions: List<LedgerTransaction>
) {
    val ratio: Float
        get() = if (budget.monthlyLimit > 0.0) (spent / budget.monthlyLimit).toFloat() else 0f

    val percentInt: Int
        get() = (ratio * 100f).roundToInt()

    val remaining: Double
        get() = budget.monthlyLimit - spent

    val isOverBudget: Boolean
        get() = spent > budget.monthlyLimit + 0.004

    val isNearLimit: Boolean
        get() = !isOverBudget && ratio >= 0.80f
}

@Immutable
data class UnbudgetedCategorySpend(
    val category: Category,
    val spent: Double,
    val entryCount: Int
)

@Immutable
data class MonthlyBudgetSummary(
    val totalBudgeted: Double,
    val totalSpentInBudgeted: Double,
    val totalMonthExpense: Double,
    val budgetedItems: List<CategoryBudgetProgress>,
    val unbudgetedItems: List<UnbudgetedCategorySpend>,
    val overBudgetCount: Int,
    val nearLimitCount: Int
) {
    val overallRatio: Float
        get() = if (totalBudgeted > 0.0) (totalSpentInBudgeted / totalBudgeted).toFloat() else 0f

    val overallPercentInt: Int
        get() = (overallRatio * 100f).roundToInt()

    val remaining: Double
        get() = totalBudgeted - totalSpentInBudgeted

    val isOverBudget: Boolean
        get() = totalBudgeted > 0.0 && totalSpentInBudgeted > totalBudgeted + 0.004

    val isNearLimit: Boolean
        get() = totalBudgeted > 0.0 && !isOverBudget && overallRatio >= 0.80f
}

@Composable
fun budgetStatusColor(ratio: Float, isOverBudget: Boolean): Color = when {
    isOverBudget -> LedgerCoral
    ratio >= 0.80f -> LedgerAmber
    else -> LedgerBlue
}

fun computeMonthlyBudgetSummary(
    bundle: LedgerDataBundle,
    year: Int,
    monthZeroBased: Int
): MonthlyBudgetSummary {
    val monthExpenses = bundle.transactions.filter { tx ->
        if (tx.txType() != TransactionType.EXPENSE) return@filter false
        val cal = LedgerFormatters.epochDayToCalendar(tx.epochDay)
        cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == monthZeroBased
    }

    val txByCategory = monthExpenses.groupBy { it.categoryId.orEmpty() }
    val expenseCategories = bundle.categories
        .filter { it.categoryType() == CategoryType.EXPENSE }
        .sortedBy { it.sortOrder }

    val activeBudgetsByCatId = bundle.budgets
        .filter { it.monthlyLimit > 0.0 }
        .associateBy { it.categoryId }

    val budgetedList = mutableListOf<CategoryBudgetProgress>()
    val unbudgetedList = mutableListOf<UnbudgetedCategorySpend>()

    expenseCategories.forEach { cat ->
        val catTxs = txByCategory[cat.id].orEmpty().sortedByDescending { it.epochDay }
        val spent = catTxs.sumOf { it.amount }
        val budget = activeBudgetsByCatId[cat.id]
        if (budget != null) {
            budgetedList.add(
                CategoryBudgetProgress(
                    category = cat,
                    budget = budget,
                    spent = spent,
                    transactions = catTxs
                )
            )
        } else {
            unbudgetedList.add(
                UnbudgetedCategorySpend(
                    category = cat,
                    spent = spent,
                    entryCount = catTxs.size
                )
            )
        }
    }

    // Show highest usage % first so categories near/over limit are immediately visible
    val sortedBudgeted = budgetedList.sortedWith(
        compareByDescending<CategoryBudgetProgress> { it.ratio }
            .thenByDescending { it.spent }
            .thenBy { it.category.name.lowercase() }
    )
    // Show unbudgeted categories with active spend first so users notice where to set limits
    val sortedUnbudgeted = unbudgetedList.sortedWith(
        compareByDescending<UnbudgetedCategorySpend> { it.spent }
            .thenBy { it.category.sortOrder }
    )

    return MonthlyBudgetSummary(
        totalBudgeted = sortedBudgeted.sumOf { it.budget.monthlyLimit },
        totalSpentInBudgeted = sortedBudgeted.sumOf { it.spent },
        totalMonthExpense = monthExpenses.sumOf { it.amount },
        budgetedItems = sortedBudgeted,
        unbudgetedItems = sortedUnbudgeted,
        overBudgetCount = sortedBudgeted.count { it.isOverBudget },
        nearLimitCount = sortedBudgeted.count { it.isNearLimit }
    )
}

@Composable
fun MonthlyBudgetScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("budget_screen_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LedgerTextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 2.dp)) {
                Text(
                    text = "Monthly Budget",
                    style = MaterialTheme.typography.titleLarge,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Set category spending limits & track usage",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(color = LedgerDivider)

        MonthlyBudgetContent(
            bundle = bundle,
            viewModel = viewModel,
            onEditTransaction = onEditTransaction,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyBudgetContent(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onEditTransaction: (LedgerTransaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val symbol = bundle.settings.mainCurrencySymbol
    val todayCal = remember { Calendar.getInstance() }
    val currentYear = todayCal.get(Calendar.YEAR)
    val currentMonth = todayCal.get(Calendar.MONTH)
    val currentDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    val isCurrentMonth = selectedYear == currentYear && selectedMonth == currentMonth
    val monthLabel = remember(selectedYear, selectedMonth) {
        LedgerFormatters.formatMonthYear(selectedYear, selectedMonth)
    }

    val summary = remember(bundle.transactions, bundle.categories, bundle.budgets, selectedYear, selectedMonth) {
        computeMonthlyBudgetSummary(bundle, selectedYear, selectedMonth)
    }

    val daysInSelectedMonth = remember(selectedYear, selectedMonth) {
        LedgerFormatters.daysInMonth(selectedYear, selectedMonth)
    }
    val remainingDaysInMonth = if (isCurrentMonth) {
        (daysInSelectedMonth - currentDayOfMonth + 1).coerceAtLeast(1)
    } else {
        0
    }

    fun shiftMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
    ) {
        // Month Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { shiftMonth(-1) },
                modifier = Modifier.testTag("budget_prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = LedgerTextPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LedgerTextPrimary,
                    modifier = Modifier.testTag("budget_month_label")
                )
                if (!isCurrentMonth) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LedgerCoral.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, LedgerCoral.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedYear = currentYear
                                selectedMonth = currentMonth
                            }
                            .testTag("budget_jump_current_month")
                    ) {
                        Text(
                            text = "This Month",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerCoral,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = { shiftMonth(1) },
                modifier = Modifier.testTag("budget_next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = LedgerTextPrimary
                )
            }
        }
        HorizontalDivider(color = LedgerDivider)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overall Monthly Budget Summary Card or Empty Onboarding Hero
            item(key = "budget_overview_card") {
                if (summary.budgetedItems.isEmpty()) {
                    EmptyBudgetHeroCard(
                        monthLabel = monthLabel,
                        totalMonthExpense = summary.totalMonthExpense,
                        symbol = symbol,
                        canQuickAdd = summary.unbudgetedItems.isNotEmpty(),
                        onQuickAdd = {
                            editingCategory = summary.unbudgetedItems.firstOrNull()?.category
                        },
                        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
                    )
                } else {
                    OverallBudgetHeroCard(
                        monthLabel = monthLabel,
                        summary = summary,
                        symbol = symbol,
                        isCurrentMonth = isCurrentMonth,
                        remainingDaysInMonth = remainingDaysInMonth,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
                    )
                }
            }

            // Budgeted Categories Section
            if (summary.budgetedItems.isNotEmpty()) {
                item(key = "budgeted_section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Budgets (${summary.budgetedItems.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerTextSecondary
                        )
                        if (summary.overBudgetCount > 0) {
                            Text(
                                text = "${summary.overBudgetCount} over limit",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LedgerCoral
                            )
                        } else if (summary.nearLimitCount > 0) {
                            Text(
                                text = "${summary.nearLimitCount} near limit",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LedgerAmber
                            )
                        } else {
                            Text(
                                text = "All on track",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LedgerGreen
                            )
                        }
                    }
                }

                items(summary.budgetedItems, key = { it.category.id }) { item ->
                    val isExpanded = expandedCategoryId == item.category.id
                    CategoryBudgetProgressCard(
                        item = item,
                        bundle = bundle,
                        symbol = symbol,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedCategoryId = if (isExpanded) null else item.category.id
                        },
                        onEditLimit = { editingCategory = item.category },
                        onEditTransaction = onEditTransaction,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
                    )
                }
            }

            // Unbudgeted Expense Categories Section
            if (summary.unbudgetedItems.isNotEmpty()) {
                item(key = "unbudgeted_section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .padding(top = 4.dp, start = 2.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (summary.budgetedItems.isEmpty()) {
                                "Expense Categories"
                            } else {
                                "Add Limits to Other Categories"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerTextSecondary
                        )
                        Text(
                            text = "${summary.unbudgetedItems.size} available",
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerTextMuted
                        )
                    }
                }

                item(key = "unbudgeted_categories_list") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LedgerSurface,
                        border = BorderStroke(1.dp, LedgerDivider),
                        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            summary.unbudgetedItems.forEachIndexed { index, unbudgeted ->
                                UnbudgetedCategoryRow(
                                    item = unbudgeted,
                                    symbol = symbol,
                                    monthLabel = monthLabel,
                                    onSetLimit = { editingCategory = unbudgeted.category }
                                )
                                if (index < summary.unbudgetedItems.lastIndex) {
                                    HorizontalDivider(
                                        color = LedgerDivider,
                                        modifier = Modifier.padding(start = 52.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "budget_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Set / Edit Monthly Budget Limit Dialog
    editingCategory?.let { cat ->
        val existingBudget = bundle.budgets.firstOrNull { it.categoryId == cat.id }
        val spentThisMonth = summary.budgetedItems
            .firstOrNull { it.category.id == cat.id }?.spent
            ?: summary.unbudgetedItems.firstOrNull { it.category.id == cat.id }?.spent
            ?: 0.0

        SetBudgetLimitDialog(
            category = cat,
            existingBudget = existingBudget,
            spentThisMonth = spentThisMonth,
            monthLabel = monthLabel,
            symbol = symbol,
            onDismiss = { editingCategory = null },
            onSave = { newLimit ->
                viewModel.saveBudget(cat.id, newLimit)
                editingCategory = null
            },
            onRemove = {
                if (existingBudget != null) {
                    viewModel.deleteBudget(existingBudget.id)
                }
                editingCategory = null
            }
        )
    }
}

@Composable
private fun EmptyBudgetHeroCard(
    monthLabel: String,
    totalMonthExpense: Double,
    symbol: String,
    canQuickAdd: Boolean,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier.testTag("budget_empty_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LedgerCoral.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = LedgerCoral,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Plan Your Monthly Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LedgerTextPrimary
                    )
                    Text(
                        text = if (totalMonthExpense > 0.0) {
                            "Spent ${LedgerFormatters.formatWholeAmount(totalMonthExpense, symbol)} in $monthLabel"
                        } else {
                            "No spending limits configured yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Set monthly limits for your expense categories to monitor progress bars and spot overspending early.",
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTextSecondary
            )
            if (canQuickAdd) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onQuickAdd,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LedgerCoral,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("budget_set_first_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set First Budget", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun OverallBudgetHeroCard(
    monthLabel: String,
    summary: MonthlyBudgetSummary,
    symbol: String,
    isCurrentMonth: Boolean,
    remainingDaysInMonth: Int,
    modifier: Modifier = Modifier
) {
    val targetColor = budgetStatusColor(summary.overallRatio, summary.isOverBudget)
    val statusColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = LedgerMotion.microTween(120),
        label = "overall_budget_color"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = summary.overallRatio.coerceIn(0f, 1f),
        animationSpec = LedgerMotion.smoothSpring(),
        label = "overall_budget_progress"
    )
    val statusText = when {
        summary.isOverBudget -> "Over Budget"
        summary.isNearLimit -> "Approaching Limit"
        else -> "On Track"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerDivider),
        modifier = modifier.testTag("budget_overall_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Budget • $monthLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "$statusText • ${summary.overallPercentInt}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = LedgerFormatters.formatWholeAmount(summary.totalSpentInBudgeted, symbol),
                    style = TabularAmountStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag("budget_overall_spent")
                )
                Text(
                    text = "of ${LedgerFormatters.formatWholeAmount(summary.totalBudgeted, symbol)}",
                    style = TabularAmountStyle.copy(fontSize = 14.sp),
                    color = LedgerTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                color = statusColor,
                trackColor = LedgerSurfaceHighlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .testTag("budget_overall_progress_bar")
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = LedgerDivider)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (summary.isOverBudget) "Over Limit By" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = LedgerFormatters.formatWholeAmount(
                            kotlin.math.abs(summary.remaining),
                            symbol
                        ),
                        style = TabularAmountStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                        color = if (summary.isOverBudget) LedgerCoral else LedgerGreen,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCurrentMonth && !summary.isOverBudget) {
                            "Safe / Day (${remainingDaysInMonth}d)"
                        } else {
                            "Month Total Spend"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val secondaryVal = if (isCurrentMonth && !summary.isOverBudget) {
                        summary.remaining.coerceAtLeast(0.0) / remainingDaysInMonth.coerceAtLeast(1)
                    } else {
                        summary.totalMonthExpense
                    }
                    Text(
                        text = LedgerFormatters.formatWholeAmount(secondaryVal, symbol),
                        style = TabularAmountStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                        color = LedgerTextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetProgressCard(
    item: CategoryBudgetProgress,
    bundle: LedgerDataBundle,
    symbol: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditLimit: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryDotColor = LedgerFormatters.parseColorHex(item.category.colorHex)
    val barColor by animateColorAsState(
        targetValue = budgetStatusColor(item.ratio, item.isOverBudget),
        animationSpec = LedgerMotion.microTween(120),
        label = "cat_budget_color_${item.category.id}"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = item.ratio.coerceIn(0f, 1f),
        animationSpec = LedgerMotion.smoothSpring(),
        label = "cat_budget_progress_${item.category.id}"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LedgerSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isOverBudget) LedgerCoral.copy(alpha = 0.45f) else LedgerDivider
        ),
        modifier = modifier.testTag("budget_category_card_${item.category.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditLimit)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(categoryDotColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LedgerTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = barColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (item.isOverBudget) "${item.percentInt}% • Over" else "${item.percentInt}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = barColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = barColor,
                    trackColor = LedgerSurfaceHighlight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .testTag("budget_progress_bar_${item.category.id}")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${LedgerFormatters.formatWholeAmount(item.spent, symbol)} / ${LedgerFormatters.formatWholeAmount(item.budget.monthlyLimit, symbol)}",
                        style = TabularAmountStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = LedgerTextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = if (item.isOverBudget) {
                            "Over by ${LedgerFormatters.formatWholeAmount(-item.remaining, symbol)}"
                        } else {
                            "${LedgerFormatters.formatWholeAmount(item.remaining, symbol)} left"
                        },
                        style = TabularAmountStyle.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                        color = if (item.isOverBudget) LedgerCoral else LedgerTextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            HorizontalDivider(color = LedgerDivider.copy(alpha = 0.7f))

            // Footer Actions: Edit Limit | Entries Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LedgerSurfaceElevated.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEditLimit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("budget_edit_btn_${item.category.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = LedgerTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Edit Limit",
                        style = MaterialTheme.typography.labelMedium,
                        color = LedgerTextSecondary
                    )
                }

                TextButton(
                    onClick = onToggleExpand,
                    enabled = item.transactions.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("budget_entries_toggle_${item.category.id}")
                ) {
                    Text(
                        text = if (item.transactions.isEmpty()) {
                            "No entries"
                        } else {
                            "${item.transactions.size} ${if (item.transactions.size == 1) "entry" else "entries"}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.transactions.isEmpty()) LedgerTextMuted else LedgerBlue
                    )
                    if (item.transactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Hide entries" else "Show entries",
                            tint = LedgerBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expandable Category Entries List
            AnimatedVisibility(
                visible = isExpanded && item.transactions.isNotEmpty(),
                enter = LedgerMotion.expandEnter(),
                exit = LedgerMotion.shrinkExit()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    item.transactions.forEach { tx ->
                        HorizontalDivider(color = LedgerDivider.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditTransaction(tx) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = tx.note.ifBlank { tx.subcategory ?: item.category.name },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LedgerTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sub = buildString {
                                    append(LedgerFormatters.formatShortDate(tx.epochDay))
                                    val accName = bundle.accountName(tx.accountId)
                                    if (accName.isNotBlank()) {
                                        append(" • ")
                                        append(accName)
                                    }
                                    if (!tx.subcategory.isNullOrBlank() && tx.note.isNotBlank()) {
                                        append(" • ")
                                        append(tx.subcategory)
                                    }
                                }
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = LedgerFormatters.formatWholeAmount(tx.amount, symbol),
                                style = TabularAmountStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                                color = LedgerCoral,
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

@Composable
private fun UnbudgetedCategoryRow(
    item: UnbudgetedCategorySpend,
    symbol: String,
    monthLabel: String,
    onSetLimit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onSetLimit)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("budget_set_limit_${item.category.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LedgerFormatters.parseColorHex(item.category.colorHex).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(LedgerFormatters.parseColorHex(item.category.colorHex))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = item.category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = LedgerTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.spent > 0.0) {
                    "Spent ${LedgerFormatters.formatWholeAmount(item.spent, symbol)} in $monthLabel"
                } else {
                    "No expenses in $monthLabel"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (item.spent > 0.0) LedgerCoral.copy(alpha = 0.85f) else LedgerTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = LedgerSurfaceElevated,
            border = BorderStroke(1.dp, LedgerBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = LedgerCoral,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Set Limit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LedgerTextPrimary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetBudgetLimitDialog(
    category: Category,
    existingBudget: Budget?,
    spentThisMonth: Double,
    monthLabel: String,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onRemove: () -> Unit
) {
    var limitText by remember(category.id, existingBudget) {
        val initial = existingBudget?.monthlyLimit?.takeIf { it > 0.0 }
        mutableStateOf(
            if (initial != null) {
                if (initial % 1.0 == 0.0) initial.toLong().toString() else initial.toString()
            } else ""
        )
    }

    val quickPresets = remember(spentThisMonth) {
        val base = mutableListOf<Long>()
        if (spentThisMonth > 0.0) {
            val roundedUp = (ceil(spentThisMonth / 500.0) * 500.0).toLong().coerceAtLeast(500L)
            base.add(roundedUp)
        }
        listOf(1000L, 2500L, 5000L, 10000L, 25000L).forEach { v ->
            if (v !in base && base.size < 4) base.add(v)
        }
        base.sorted()
    }

    val parsedAmount = limitText.trim().toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LedgerSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(LedgerFormatters.parseColorHex(category.colorHex))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${category.name} Budget",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Spent in $monthLabel: ${LedgerFormatters.formatWholeAmount(spentThisMonth, symbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerTextSecondary
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) limitText = filtered.take(12)
                    },
                    label = { Text("Monthly Spending Limit ($symbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = IndianNumberVisualTransformation,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LedgerSurfaceElevated,
                            border = BorderStroke(1.dp, LedgerBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { limitText = preset.toString() }
                        ) {
                            Text(
                                text = LedgerFormatters.formatWholeAmount(preset.toDouble(), symbol),
                                style = MaterialTheme.typography.labelMedium,
                                color = LedgerTextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                if (existingBudget != null) {
                    OutlinedButton(
                        onClick = onRemove,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LedgerCoral.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("budget_remove_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Budget Limit", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(parsedAmount) },
                enabled = parsedAmount > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LedgerCoral,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("budget_save_button")
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LedgerTextSecondary)
            }
        }
    )
}
