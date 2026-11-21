package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryType
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.util.LedgerFormatters
import com.example.viewmodel.LedgerDataBundle

@Composable
fun CreateCategoryLedgerDialog(
    bundle: LedgerDataBundle,
    initialType: CategoryType = CategoryType.EXPENSE,
    initialCategoryId: String? = null,
    initialLedgerName: String = "",
    lockType: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, ledgerName: String) -> Unit
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    val categoriesForType = remember(bundle.categories, selectedType) {
        bundle.categories.filter { it.categoryType() == selectedType }
    }
    var selectedCategoryId by remember(initialCategoryId, initialType) {
        val validInitial = categoriesForType.firstOrNull { it.id == initialCategoryId }?.id
        mutableStateOf(validInitial ?: categoriesForType.firstOrNull()?.id.orEmpty())
    }
    var ledgerName by remember(initialLedgerName) { mutableStateOf(initialLedgerName.take(60)) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedType, categoriesForType) {
        if (categoriesForType.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categoriesForType.firstOrNull()?.id.orEmpty()
        }
    }

    val selectedCategory = categoriesForType.firstOrNull { it.id == selectedCategoryId }
    val accentColor by animateColorAsState(
        targetValue = if (selectedType == CategoryType.INCOME) LedgerBlue else LedgerCoral,
        animationSpec = tween(180),
        label = "create_ledger_accent"
    )
    val typeLabel = if (selectedType == CategoryType.INCOME) "Income" else "Expense"
    val dialogTitle = when {
        !lockType -> "Create Expense / Income Ledger"
        selectedType == CategoryType.INCOME -> "Create Income Ledger"
        else -> "Create Expense Ledger"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LedgerSurface,
        title = {
            Text(
                text = dialogTitle,
                color = LedgerTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!lockType) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Ledger Type",
                            style = MaterialTheme.typography.labelMedium,
                            color = LedgerTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                CategoryType.EXPENSE to "Expense",
                                CategoryType.INCOME to "Income"
                            ).forEach { (typeOpt, label) ->
                                val active = selectedType == typeOpt
                                val optAccent = if (typeOpt == CategoryType.INCOME) LedgerBlue else LedgerCoral
                                val optBg by animateColorAsState(
                                    targetValue = if (active) optAccent.copy(alpha = 0.2f) else LedgerSurfaceElevated,
                                    animationSpec = tween(160),
                                    label = "opt_bg_${typeOpt.name}"
                                )
                                val optBorder by animateColorAsState(
                                    targetValue = if (active) optAccent else LedgerBorder,
                                    animationSpec = tween(160),
                                    label = "opt_border_${typeOpt.name}"
                                )
                                val optText by animateColorAsState(
                                    targetValue = if (active) optAccent else LedgerTextSecondary,
                                    animationSpec = tween(160),
                                    label = "opt_text_${typeOpt.name}"
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = optBg,
                                    border = BorderStroke(1.dp, optBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedType = typeOpt }
                                        .testTag("create_ledger_type_${typeOpt.name.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = optText,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "$typeLabel Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = LedgerTextSecondary
                    )
                    if (categoriesForType.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LedgerSurfaceElevated,
                            border = BorderStroke(1.dp, LedgerBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No ${typeLabel.lowercase()} categories found. Please create a category first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LedgerTextMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LedgerSurfaceElevated,
                                border = BorderStroke(1.dp, LedgerBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryMenuExpanded = true }
                                    .testTag("create_ledger_category_selector")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedCategory != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(LedgerFormatters.parseColorHex(selectedCategory.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Text(
                                        text = selectedCategory?.name ?: "Select category",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = LedgerTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Choose category",
                                        tint = LedgerTextSecondary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false },
                                containerColor = LedgerSurfaceElevated,
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                categoriesForType.forEach { cat ->
                                    val isChosen = cat.id == selectedCategoryId
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(LedgerFormatters.parseColorHex(cat.colorHex))
                                                )
                                                Column {
                                                    Text(
                                                        text = cat.name,
                                                        color = if (isChosen) accentColor else LedgerTextPrimary,
                                                        fontWeight = if (isChosen) FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                    if (cat.subcategories.isNotEmpty()) {
                                                        Text(
                                                            text = "${cat.subcategories.size} existing ledger(s)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = LedgerTextMuted
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            categoryMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("create_ledger_cat_opt_${cat.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = ledgerName,
                    onValueChange = { ledgerName = it.take(60) },
                    label = { Text("$typeLabel Ledger Name") },
                    placeholder = {
                        Text(
                            text = if (selectedType == CategoryType.INCOME) {
                                "e.g. Consulting Fees, Dividend"
                            } else {
                                "e.g. Electricity Bill, Office Rent"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_category_ledger_name_input")
                )

                val cleanName = ledgerName.trim()
                var lastPreviewLabel by remember { mutableStateOf("") }
                if (selectedCategory != null && cleanName.isNotEmpty()) {
                    lastPreviewLabel = "${selectedCategory.name} / $cleanName"
                }
                AnimatedVisibility(
                    visible = selectedCategory != null && cleanName.isNotEmpty(),
                    enter = expandVertically(tween(160)) + fadeIn(tween(140)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "Entries will post to:",
                                style = MaterialTheme.typography.labelSmall,
                                color = LedgerTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastPreviewLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val catId = selectedCategory?.id ?: return@Button
                    val clean = ledgerName.trim()
                    if (clean.isNotEmpty()) {
                        onConfirm(catId, clean)
                    }
                },
                enabled = selectedCategory != null && ledgerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("create_category_ledger_save_button")
            ) {
                Text("Create Ledger", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LedgerTextSecondary)
            }
        }
    )
}
