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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.AccountGroup
import com.example.data.AccountNature
import com.example.data.Category
import com.example.data.CategoryType
import com.example.data.LedgerTransaction
import com.example.data.RepeatFrequency
import com.example.ui.theme.ChartColorHexes
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.ui.theme.TabularAmountStyle
import com.example.util.LedgerFormatters
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel

enum class ConfigRoute {
    ROOT,
    ACCOUNT_GROUPS,
    ACCOUNTS_SETTING,
    DELETED_ACCOUNTS,
    INCOME_CATEGORIES,
    EXPENSE_CATEGORIES,
    BUDGET_SETTING,
    TAG_SETTING,
    REPEAT_SETTING
}

@Composable
fun MoreScreen(
    bundle: LedgerDataBundle,
    onOpenConfiguration: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLocalDataDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = LedgerTextPrimary
            )
            Text(
                text = "Local Mode v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted
            )
        }
        HorizontalDivider(color = LedgerDivider)

        // 3x2 Settings Action Grid
        Column(modifier = Modifier.fillMaxWidth().background(LedgerSurface)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SettingsGridTile(
                    icon = Icons.Filled.Settings,
                    label = "Configuration",
                    onClick = onOpenConfiguration,
                    modifier = Modifier.weight(1f).testTag("more_configuration_tile")
                )
                SettingsGridTile(
                    icon = Icons.Filled.FileDownload,
                    label = "Export Excel",
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f).testTag("more_export_tile")
                )
                SettingsGridTile(
                    icon = Icons.Filled.CloudDone,
                    label = "Local Data",
                    onClick = { showLocalDataDialog = true },
                    modifier = Modifier.weight(1f).testTag("more_local_data_tile")
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                SettingsGridTile(
                    icon = Icons.Filled.Palette,
                    label = "Style",
                    onClick = { showStyleDialog = true },
                    modifier = Modifier.weight(1f).testTag("more_style_tile")
                )
                SettingsGridTile(
                    icon = Icons.Filled.AccountBalanceWallet,
                    label = "Budgets",
                    onClick = onOpenBudgets,
                    modifier = Modifier.weight(1f).testTag("more_budgets_tile")
                )
                SettingsGridTile(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    label = "Help",
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.weight(1f).testTag("more_help_tile")
                )
            }
        }
        HorizontalDivider(color = LedgerDivider)

        Spacer(modifier = Modifier.height(20.dp))

        // Local Testing Mode Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LedgerSurface,
            border = BorderStroke(1.dp, LedgerDivider),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = LedgerBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local Testing Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = LedgerTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign-in is temporarily disabled while testing. All entries are saved locally on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${bundle.activeAccounts.size} active accounts • ${bundle.transactions.size} ledger entries • ${bundle.categories.size} categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerTextMuted
                )
            }
        }
    }

    if (showLocalDataDialog) {
        AlertDialog(
            onDismissRequest = { showLocalDataDialog = false },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = "Local Storage Active",
                    color = LedgerTextPrimary
                )
            },
            text = {
                Text(
                    text = "Sign-in is disabled so you can build and test freely without authentication. All accounts, categories, budgets, notes, and entries are persisted locally on this device. Cloud Sync and Google Sign-In can be re-enabled whenever you're ready.",
                    color = LedgerTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showLocalDataDialog = false }) { Text("OK", color = LedgerCoral) }
            }
        )
    }

    if (showStyleDialog) {
        AlertDialog(
            onDismissRequest = { showStyleDialog = false },
            containerColor = LedgerSurface,
            title = { Text("Theme & Display", color = LedgerTextPrimary) },
            text = {
                Text(
                    "Money Ledger uses the dark charcoal ledger theme with color-coded Income/Assets (Blue), Expenses/Liabilities (Coral Red), and Transfers (White), paired with tabular numerals for effortless scanning.",
                    color = LedgerTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showStyleDialog = false }) { Text("Got it", color = LedgerCoral) }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = LedgerSurface,
            title = { Text("Quick Guide", color = LedgerTextPrimary) },
            text = {
                Text(
                    "• Trans.: Browse Daily, Calendar, Monthly, Total, and Notes views. Tap + to add Income, Expense, or Transfer entries.\n\n" +
                        "• Stats: Switch Weekly, Monthly, Annually, or Custom Period and tap any category slice/row to inspect its transactions.\n\n" +
                        "• Accounts: Track Assets, Liabilities, and Net Total across custom account groups. Tap any account for its full statement.",
                    color = LedgerTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Close", color = LedgerCoral) }
            }
        )
    }
}

@Composable
private fun SettingsGridTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 22.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = LedgerTextPrimary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LedgerTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConfigurationHostScreen(
    initialRoute: ConfigRoute,
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBackToMain: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit
) {
    var currentRoute by remember(initialRoute) { mutableStateOf(initialRoute) }

    fun navigateUp() {
        if (currentRoute == ConfigRoute.ROOT || currentRoute == initialRoute && initialRoute != ConfigRoute.ROOT) {
            onBackToMain()
        } else {
            currentRoute = ConfigRoute.ROOT
        }
    }

    BackHandler { navigateUp() }

    when (currentRoute) {
        ConfigRoute.ROOT -> ConfigurationMenuScreen(
            bundle = bundle,
            viewModel = viewModel,
            onBack = onBackToMain,
            onNavigate = { currentRoute = it }
        )
        ConfigRoute.ACCOUNT_GROUPS -> AccountGroupSettingScreen(
            bundle = bundle,
            viewModel = viewModel,
            onBack = { navigateUp() }
        )
        ConfigRoute.ACCOUNTS_SETTING -> AccountsSettingScreen(
            bundle = bundle,
            viewModel = viewModel,
            onBack = { navigateUp() }
        )
        ConfigRoute.DELETED_ACCOUNTS -> DeletedAccountsScreen(
            bundle = bundle,
            viewModel = viewModel,
            onBack = { navigateUp() }
        )
        ConfigRoute.INCOME_CATEGORIES -> CategorySettingScreen(
            categoryType = CategoryType.INCOME,
            bundle = bundle,
            viewModel = viewModel,
            onBack = { navigateUp() }
        )
        ConfigRoute.EXPENSE_CATEGORIES -> CategorySettingScreen(
            categoryType = CategoryType.EXPENSE,
            bundle = bundle,
            viewModel = viewModel,
            onBack = { navigateUp() }
        )
        ConfigRoute.BUDGET_SETTING -> BudgetSettingScreen(
            bundle = bundle,
            viewModel = viewModel,
            onBack = { navigateUp() }
        )
        ConfigRoute.TAG_SETTING -> TagSettingScreen(
            bundle = bundle,
            onBack = { navigateUp() }
        )
        ConfigRoute.REPEAT_SETTING -> RepeatSettingScreen(
            bundle = bundle,
            onBack = { navigateUp() },
            onEditTransaction = onEditTransaction
        )
    }
}

@Composable
private fun ConfigurationMenuScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    onNavigate: (ConfigRoute) -> Unit
) {
    val settings = bundle.settings
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showStartDayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(title = "Configuration", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { ConfigSectionHeader("Accounts") }
            item {
                ConfigRow("Account Group", onClick = { onNavigate(ConfigRoute.ACCOUNT_GROUPS) })
                ConfigRow("Accounts Setting", onClick = { onNavigate(ConfigRoute.ACCOUNTS_SETTING) })
                ConfigRow(
                    title = "Deleted accounts",
                    value = if (bundle.archivedAccounts.isNotEmpty()) "${bundle.archivedAccounts.size}" else null,
                    onClick = { onNavigate(ConfigRoute.DELETED_ACCOUNTS) }
                )
            }

            item { ConfigSectionHeader("Category/Repeat") }
            item {
                ConfigRow("Income Category Setting", onClick = { onNavigate(ConfigRoute.INCOME_CATEGORIES) })
                ConfigRow("Expenses Category Setting", onClick = { onNavigate(ConfigRoute.EXPENSE_CATEGORIES) })
                ConfigRow(
                    title = "Subcategory",
                    value = if (settings.subcategoryEnabled) "ON" else "OFF",
                    valueColor = if (settings.subcategoryEnabled) LedgerCoral else LedgerTextMuted,
                    onClick = {
                        viewModel.updateSettings(settings.copy(subcategoryEnabled = !settings.subcategoryEnabled))
                    }
                )
                ConfigRow("Budget Setting", onClick = { onNavigate(ConfigRoute.BUDGET_SETTING) })
                ConfigRow("Tag Settings", onClick = { onNavigate(ConfigRoute.TAG_SETTING) })
                ConfigRow("Repeat Setting", onClick = { onNavigate(ConfigRoute.REPEAT_SETTING) })
            }

            item { ConfigSectionHeader("Configuration") }
            item {
                ConfigRow(
                    title = "Main Currency Setting",
                    value = "${settings.mainCurrencyCode} (${settings.mainCurrencySymbol})",
                    valueColor = LedgerCoral,
                    onClick = { showCurrencyDialog = true }
                )
                ConfigRow(
                    title = "Start Screen (Daily/Calendar)",
                    value = if (settings.startScreen == "CALENDAR") "Calendar" else "Daily",
                    valueColor = LedgerCoral,
                    onClick = {
                        val next = if (settings.startScreen == "CALENDAR") "DAILY" else "CALENDAR"
                        viewModel.updateSettings(settings.copy(startScreen = next))
                    }
                )
                ConfigRow(
                    title = "Monthly Start Date",
                    value = "Every ${settings.monthlyStartDate}",
                    valueColor = LedgerCoral,
                    onClick = { showStartDayDialog = true }
                )
                ConfigRow(
                    title = "Weekly Start Day",
                    value = if (settings.weeklyStartDay == "SUNDAY") "Sunday" else "Monday",
                    valueColor = LedgerCoral,
                    onClick = {
                        val next = if (settings.weeklyStartDay == "SUNDAY") "MONDAY" else "SUNDAY"
                        viewModel.updateSettings(settings.copy(weeklyStartDay = next))
                    }
                )
            }
        }
    }

    if (showCurrencyDialog) {
        val currencies = listOf("INR" to "₹", "USD" to "$", "EUR" to "€", "GBP" to "£")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            containerColor = LedgerSurface,
            title = { Text("Main Currency", color = LedgerTextPrimary) },
            text = {
                Column {
                    currencies.forEach { (code, sym) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(mainCurrencyCode = code, mainCurrencySymbol = sym))
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$code ($sym)", color = if (settings.mainCurrencyCode == code) LedgerCoral else LedgerTextPrimary)
                            if (settings.mainCurrencyCode == code) Text("Active", color = LedgerCoral, style = MaterialTheme.typography.labelSmall)
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close", color = LedgerTextSecondary) }
            }
        )
    }

    if (showStartDayDialog) {
        val options = listOf(1L, 5L, 10L, 15L, 20L, 25L, 28L)
        AlertDialog(
            onDismissRequest = { showStartDayDialog = false },
            containerColor = LedgerSurface,
            title = { Text("Monthly Start Date", color = LedgerTextPrimary) },
            text = {
                Column {
                    options.forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(monthlyStartDate = day))
                                    showStartDayDialog = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Every $day", color = if (settings.monthlyStartDate == day) LedgerCoral else LedgerTextPrimary)
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStartDayDialog = false }) { Text("Close", color = LedgerTextSecondary) }
            }
        )
    }
}

@Composable
private fun AccountGroupSettingScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    var editingGroup by remember { mutableStateOf<AccountGroup?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(
            title = "Account Group",
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = {
                        editingGroup = null
                        showGroupDialog = true
                    },
                    modifier = Modifier.testTag("add_account_group_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add group", tint = LedgerTextPrimary)
                }
            }
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(bundle.activeGroups, key = { it.id }) { grp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setAccountGroupArchived(grp.id, true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.RemoveCircle, contentDescription = "Archive group", tint = LedgerCoral, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(grp.name, style = MaterialTheme.typography.bodyLarge, color = LedgerTextPrimary)
                        Text(
                            text = if (grp.nature() == AccountNature.LIABILITY) "Liability" else "Asset",
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerTextMuted
                        )
                    }
                    IconButton(
                        onClick = {
                            editingGroup = grp
                            showGroupDialog = true
                        }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit group", tint = LedgerTextSecondary, modifier = Modifier.size(19.dp))
                    }
                }
                HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
            }
        }
    }

    if (showGroupDialog) {
        var name by remember(editingGroup) { mutableStateOf(editingGroup?.name.orEmpty()) }
        var nature by remember(editingGroup) { mutableStateOf(editingGroup?.nature() ?: AccountNature.ASSET) }

        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            containerColor = LedgerSurface,
            title = { Text(if (editingGroup == null) "New Account Group" else "Edit Account Group", color = LedgerTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        AccountNature.entries.forEach { opt ->
                            val selected = nature == opt
                            val tint = if (opt == AccountNature.ASSET) LedgerBlue else LedgerCoral
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) tint.copy(alpha = 0.2f) else LedgerSurfaceElevated,
                                border = BorderStroke(1.dp, if (selected) tint else LedgerBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { nature = opt }
                                    .padding(vertical = 8.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (opt == AccountNature.ASSET) "Asset" else "Liability", color = if (selected) tint else LedgerTextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveAccountGroup(
                                existingId = editingGroup?.id,
                                name = name,
                                groupType = nature,
                                sortOrder = editingGroup?.sortOrder ?: bundle.accountGroups.size.toLong()
                            )
                            showGroupDialog = false
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false }) { Text("Cancel", color = LedgerTextSecondary) }
            }
        )
    }
}

@Composable
private fun AccountsSettingScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(
            title = "Accounts Setting",
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = {
                        editingAccount = null
                        showDialog = true
                    },
                    modifier = Modifier.testTag("accounts_setting_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add account", tint = LedgerTextPrimary)
                }
            }
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            bundle.accountSections.forEach { sec ->
                if (sec.accounts.isNotEmpty()) {
                    item(key = "sec_${sec.group.id}") {
                        ConfigSectionHeader(sec.group.name)
                    }
                    items(sec.accounts, key = { it.account.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LedgerSurface)
                                .clickable {
                                    editingAccount = item.account
                                    showDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.account.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = LedgerTextPrimary
                                )
                                if (!item.account.includeInNetWorth) {
                                    Text(
                                        text = "Excluded from Net Worth",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerTextMuted
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = LedgerTextMuted
                            )
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AccountEditorDialog(
            bundle = bundle,
            existing = editingAccount,
            onDismiss = { showDialog = false },
            onSave = { groupId, name, initialBalance, nature, note, includeInNetWorth ->
                viewModel.saveAccount(
                    existingId = editingAccount?.id,
                    groupId = groupId,
                    name = name,
                    initialBalance = initialBalance,
                    accountType = nature,
                    note = note,
                    sortOrder = editingAccount?.sortOrder ?: bundle.accounts.size.toLong(),
                    includeInNetWorth = includeInNetWorth
                )
                showDialog = false
            }
        )
    }
}

@Composable
private fun DeletedAccountsScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(title = "Deleted Accounts", onBack = onBack)

        if (bundle.archivedAccounts.isEmpty() && bundle.archivedGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No deleted accounts or groups.", color = LedgerTextMuted)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (bundle.archivedAccounts.isNotEmpty()) {
                    item { ConfigSectionHeader("Archived Accounts") }
                    items(bundle.archivedAccounts, key = { it.id }) { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LedgerSurface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(acc.name, color = LedgerTextPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.setAccountArchived(acc.id, false) }) {
                                Icon(Icons.Filled.Restore, contentDescription = "Restore", tint = LedgerBlue)
                            }
                            IconButton(onClick = { viewModel.deleteAccountPermanently(acc.id) }) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = "Delete permanently", tint = LedgerCoral)
                            }
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
                if (bundle.archivedGroups.isNotEmpty()) {
                    item { ConfigSectionHeader("Archived Account Groups") }
                    items(bundle.archivedGroups, key = { it.id }) { grp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LedgerSurface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(grp.name, color = LedgerTextPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.setAccountGroupArchived(grp.id, false) }) {
                                Icon(Icons.Filled.Restore, contentDescription = "Restore group", tint = LedgerBlue)
                            }
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySettingScreen(
    categoryType: CategoryType,
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val cats = remember(bundle.categories, categoryType) {
        bundle.categories.filter { it.categoryType() == categoryType }
    }
    var editingCat by remember { mutableStateOf<Category?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(
            title = if (categoryType == CategoryType.INCOME) "Income Categories" else "Expense Categories",
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = {
                        editingCat = null
                        showDialog = true
                    },
                    modifier = Modifier.testTag("add_category_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add category", tint = LedgerTextPrimary)
                }
            }
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(cats, key = { it.id }) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .clickable {
                            editingCat = cat
                            showDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(LedgerFormatters.parseColorHex(cat.colorHex))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.name, style = MaterialTheme.typography.bodyLarge, color = LedgerTextPrimary)
                        if (cat.subcategories.isNotEmpty()) {
                            Text(
                                text = cat.subcategories.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = LedgerTextMuted,
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete category", tint = LedgerTextMuted, modifier = Modifier.size(19.dp))
                    }
                }
                HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
            }
        }
    }

    if (showDialog) {
        var name by remember(editingCat) { mutableStateOf(editingCat?.name.orEmpty()) }
        var colorHex by remember(editingCat) {
            mutableStateOf(editingCat?.colorHex ?: ChartColorHexes[cats.size % ChartColorHexes.size])
        }
        var subsText by remember(editingCat) {
            mutableStateOf(editingCat?.subcategories?.joinToString(", ").orEmpty())
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = LedgerSurface,
            title = { Text(if (editingCat == null) "Add Category" else "Edit Category", color = LedgerTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subsText,
                        onValueChange = { subsText = it.take(400) },
                        label = { Text("Subcategories (comma-separated)") },
                        placeholder = { Text("e.g. Fuel, Insurance, Service") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChartColorHexes.forEach { hex ->
                            val selected = hex.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(LedgerFormatters.parseColorHex(hex))
                                    .border(
                                        width = if (selected) 2.5.dp else 0.dp,
                                        color = if (selected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedSubs = subsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (name.isNotBlank()) {
                            viewModel.saveCategory(
                                existingId = editingCat?.id,
                                name = name,
                                type = categoryType,
                                colorHex = colorHex,
                                subcategories = parsedSubs,
                                sortOrder = editingCat?.sortOrder ?: cats.size.toLong()
                            )
                            showDialog = false
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel", color = LedgerTextSecondary) }
            }
        )
    }
}

@Composable
private fun BudgetSettingScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val expenseCats = remember(bundle.categories) {
        bundle.categories.filter { it.categoryType() == CategoryType.EXPENSE }
    }
    val symbol = bundle.settings.mainCurrencySymbol
    var selectedCat by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(title = "Budget Setting", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(expenseCats, key = { it.id }) { cat ->
                val budget = bundle.budgets.firstOrNull { it.categoryId == cat.id }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .clickable { selectedCat = cat }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(LedgerFormatters.parseColorHex(cat.colorHex))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(cat.name, style = MaterialTheme.typography.bodyLarge, color = LedgerTextPrimary, modifier = Modifier.weight(1f))
                    Text(
                        text = if (budget != null && budget.monthlyLimit > 0.0) {
                            LedgerFormatters.formatAmount(budget.monthlyLimit, symbol)
                        } else "Not set",
                        style = TabularAmountStyle,
                        color = if (budget != null && budget.monthlyLimit > 0.0) LedgerCoral else LedgerTextMuted
                    )
                }
                HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
            }
        }
    }

    if (selectedCat != null) {
        val cat = selectedCat!!
        val existingBudget = bundle.budgets.firstOrNull { it.categoryId == cat.id }
        var limitText by remember(cat) {
            mutableStateOf(existingBudget?.monthlyLimit?.toString().orEmpty())
        }

        AlertDialog(
            onDismissRequest = { selectedCat = null },
            containerColor = LedgerSurface,
            title = { Text("${cat.name} Monthly Budget", color = LedgerTextPrimary) },
            text = {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) limitText = filtered.take(12)
                    },
                    label = { Text("Monthly Limit ($symbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = limitText.trim().toDoubleOrNull() ?: 0.0
                        if (value > 0.0) {
                            viewModel.saveBudget(cat.id, value)
                        } else if (existingBudget != null) {
                            viewModel.deleteBudget(existingBudget.id)
                        }
                        selectedCat = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { selectedCat = null }) { Text("Cancel", color = LedgerTextSecondary) }
            }
        )
    }
}

@Composable
private fun TagSettingScreen(
    bundle: LedgerDataBundle,
    onBack: () -> Unit
) {
    val tagCounts = remember(bundle.transactions) {
        bundle.transactions.mapNotNull { it.effectiveTagging() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(title = "Tagging Settings", onBack = onBack)
        if (tagCounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Taggings you add to transactions will appear here.", color = LedgerTextMuted)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tagCounts, key = { it.key }) { (tag, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LedgerSurface)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tag, color = LedgerTextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text("$count entries", color = LedgerTextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun RepeatSettingScreen(
    bundle: LedgerDataBundle,
    onBack: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit
) {
    val repeating = remember(bundle.transactions) {
        bundle.transactions.filter { it.repeat() != RepeatFrequency.NONE }
    }
    val symbol = bundle.settings.mainCurrencySymbol

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(title = "Repeat Setting", onBack = onBack)
        if (repeating.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No recurring transactions configured yet. Tap Rep/Inst. while adding a transaction.", color = LedgerTextMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(repeating, key = { it.id }) { tx ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LedgerSurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tx.repeatLabel(), style = MaterialTheme.typography.labelSmall, color = LedgerCoral)
                            Text(LedgerFormatters.formatShortDate(tx.epochDay), style = MaterialTheme.typography.labelSmall, color = LedgerTextMuted)
                        }
                        TransactionRowItem(tx = tx, bundle = bundle, symbol = symbol, onClick = { onEditTransaction(tx) })
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigTopBar(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LedgerSurface)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("config_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LedgerTextPrimary)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = LedgerTextPrimary,
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
        if (trailing != null) trailing()
    }
    HorizontalDivider(color = LedgerDivider)
}

@Composable
private fun ConfigSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = LedgerTextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .background(LedgerBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun ConfigRow(
    title: String,
    value: String? = null,
    valueColor: Color = LedgerTextSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LedgerSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = LedgerTextPrimary
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
}
