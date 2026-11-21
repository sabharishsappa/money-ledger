package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerGreen
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerSurfaceHighlight
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.ui.theme.TabularAmountStyle
import com.example.util.LedgerFormatters
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import java.util.Locale

enum class ConfigRoute {
    ROOT,
    ACCOUNT_GROUPS,
    ACCOUNTS_SETTING,
    DELETED_ACCOUNTS,
    INCOME_CATEGORIES,
    EXPENSE_CATEGORIES,
    BUDGET_SETTING,
    TAG_SETTING,
    REPEAT_SETTING,
    APP_LOCK
}

@Composable
fun MoreScreen(
    bundle: LedgerDataBundle,
    appLockEnabled: Boolean = false,
    onOpenConfiguration: () -> Unit,
    onOpenAppLock: () -> Unit = {},
    onExportCsv: () -> Unit,
    onImportExcel: () -> Unit,
    onMassUploadLedgers: () -> Unit,
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
                .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = LedgerTextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Local Mode v1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTextMuted
                )
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.testTag("more_help_tile")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Help",
                        tint = LedgerTextSecondary
                    )
                }
            }
        }
        HorizontalDivider(color = LedgerDivider)

        // Scrollable Settings Body (fits compact phones, landscape & tablets)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                // Uniformly aligned Settings Options List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                ) {
                    SettingsListRow(
                        icon = Icons.Filled.Settings,
                        iconTint = LedgerTextPrimary,
                        title = "Configuration",
                        subtitle = "Accounts, categories, currency & repeat rules",
                        onClick = onOpenConfiguration,
                        modifier = Modifier.testTag("more_configuration_tile")
                    )
                    HorizontalDivider(
                        color = LedgerDivider,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                    SettingsListRow(
                        icon = Icons.Filled.AccountBalanceWallet,
                        iconTint = LedgerCoral,
                        title = "Budgets",
                        subtitle = "Monthly spending limits by category",
                        onClick = onOpenBudgets,
                        modifier = Modifier.testTag("more_budgets_tile")
                    )
                    HorizontalDivider(
                        color = LedgerDivider,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                    SettingsListRow(
                        icon = Icons.Filled.Lock,
                        iconTint = if (appLockEnabled) LedgerGreen else LedgerCoral,
                        title = "App Lock",
                        subtitle = if (appLockEnabled) {
                            "4-digit PIN lock • Enabled"
                        } else {
                            "Protect app with a 4-digit PIN"
                        },
                        onClick = onOpenAppLock,
                        modifier = Modifier.testTag("more_app_lock_tile")
                    )
                    HorizontalDivider(
                        color = LedgerDivider,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                    SettingsListRow(
                        icon = Icons.Filled.FileDownload,
                        iconTint = LedgerGreen,
                        title = "Export Excel",
                        subtitle = "Download transactions in Excel format",
                        onClick = onExportCsv,
                        modifier = Modifier.testTag("more_export_tile")
                    )
                    HorizontalDivider(
                        color = LedgerDivider,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                    SettingsListRow(
                        icon = Icons.Filled.FileUpload,
                        iconTint = LedgerBlue,
                        title = "Import Excel",
                        subtitle = "Import transactions from Excel / CSV",
                        onClick = onImportExcel,
                        modifier = Modifier.testTag("more_import_tile")
                    )
                    HorizontalDivider(
                        color = LedgerDivider,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                    SettingsListRow(
                        icon = Icons.Filled.FileUpload,
                        iconTint = LedgerBlue,
                        title = "Mass Upload Ledgers",
                        subtitle = "Bulk add accounts & categories via Excel",
                        onClick = onMassUploadLedgers,
                        modifier = Modifier.testTag("more_mass_upload_ledgers_card")
                    )
                    HorizontalDivider(
                        color = LedgerDivider,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                    SettingsListRow(
                        icon = Icons.Filled.Palette,
                        iconTint = LedgerAmber,
                        title = "Style",
                        subtitle = "Dark charcoal theme & color coding",
                        onClick = { showStyleDialog = true },
                        modifier = Modifier.testTag("more_style_tile")
                    )
                    HorizontalDivider(
                        color = LedgerDivider,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                    SettingsListRow(
                        icon = Icons.Filled.CloudDone,
                        iconTint = LedgerBlue,
                        title = "Local Data",
                        subtitle = "${bundle.activeAccounts.size} accounts • ${bundle.transactions.size} entries • ${bundle.categories.size} categories",
                        onClick = { showLocalDataDialog = true },
                        modifier = Modifier.testTag("more_local_data_tile")
                    )
                    HorizontalDivider(
                        color = LedgerDivider,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                    SettingsListRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = LedgerTextSecondary,
                        title = "Quick Guide",
                        subtitle = "How to use Transactions, Accounts & Reports",
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("more_quick_guide_tile")
                    )
                }
                HorizontalDivider(color = LedgerDivider)

                Text(
                    text = "Local Testing Mode • All data is saved locally on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerTextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
                        "• Accounts: Track Assets, Liabilities, and Net Total across custom account groups. Tap any account for its full statement.\n\n" +
                        "• Reports: Open Stats, Net Worth, 6-Month Spending Trends, and Monthly Budget reports from one unified list.",
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
private fun SettingsListRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = LedgerTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LedgerTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ConfigurationHostScreen(
    initialRoute: ConfigRoute,
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    appLockEnabled: Boolean = false,
    onAppLockStateChanged: (enabled: Boolean, message: String) -> Unit = { _, _ -> },
    onLockNow: () -> Unit = {},
    onBackToMain: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit,
    onMassUploadLedgers: () -> Unit,
    onCreateCategoryLedger: (CategoryType, String?) -> Unit,
    onPostEntryForCategoryLedger: (CategoryType, String, String) -> Unit
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

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            val goingBack = targetState == ConfigRoute.ROOT
            if (goingBack) {
                (slideInHorizontally(tween(190)) { -it / 5 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(160)) { it / 5 } + fadeOut(tween(130)))
            } else {
                (slideInHorizontally(tween(190)) { it / 5 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(160)) { -it / 5 } + fadeOut(tween(130)))
            }
        },
        label = "config_route_transition"
    ) { route ->
        when (route) {
            ConfigRoute.ROOT -> ConfigurationMenuScreen(
                bundle = bundle,
                viewModel = viewModel,
                appLockEnabled = appLockEnabled,
                onBack = onBackToMain,
                onNavigate = { currentRoute = it },
                onMassUploadLedgers = onMassUploadLedgers,
                onCreateCategoryLedger = onCreateCategoryLedger
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
                onBack = { navigateUp() },
                onCreateCategoryLedger = onCreateCategoryLedger,
                onPostEntryForCategoryLedger = onPostEntryForCategoryLedger
            )
            ConfigRoute.EXPENSE_CATEGORIES -> CategorySettingScreen(
                categoryType = CategoryType.EXPENSE,
                bundle = bundle,
                viewModel = viewModel,
                onBack = { navigateUp() },
                onCreateCategoryLedger = onCreateCategoryLedger,
                onPostEntryForCategoryLedger = onPostEntryForCategoryLedger
            )
            ConfigRoute.BUDGET_SETTING -> MonthlyBudgetScreen(
                bundle = bundle,
                viewModel = viewModel,
                onBack = { navigateUp() },
                onEditTransaction = onEditTransaction
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
            ConfigRoute.APP_LOCK -> AppLockSettingsScreen(
                isEnabled = appLockEnabled,
                onBack = { navigateUp() },
                onLockStateChanged = onAppLockStateChanged,
                onLockNow = onLockNow
            )
        }
    }
}

@Composable
private fun ConfigurationMenuScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    appLockEnabled: Boolean,
    onBack: () -> Unit,
    onNavigate: (ConfigRoute) -> Unit,
    onMassUploadLedgers: () -> Unit,
    onCreateCategoryLedger: (CategoryType, String?) -> Unit
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
                    title = "Mass Upload Ledgers",
                    value = "Excel / CSV",
                    valueColor = LedgerBlue,
                    onClick = onMassUploadLedgers
                )
                ConfigRow(
                    title = "Deleted accounts",
                    value = if (bundle.archivedAccounts.isNotEmpty()) "${bundle.archivedAccounts.size}" else null,
                    onClick = { onNavigate(ConfigRoute.DELETED_ACCOUNTS) }
                )
            }

            item { ConfigSectionHeader("Category/Repeat") }
            item {
                ConfigRow(
                    title = "Create Expense / Income Ledger",
                    value = "New Ledger",
                    valueColor = LedgerCoral,
                    onClick = { onCreateCategoryLedger(CategoryType.EXPENSE, null) }
                )
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
                ConfigRow(
                    title = "App Lock (4-Digit PIN)",
                    value = if (appLockEnabled) "ON" else "OFF",
                    valueColor = if (appLockEnabled) LedgerGreen else LedgerTextMuted,
                    onClick = { onNavigate(ConfigRoute.APP_LOCK) }
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
    onBack: () -> Unit,
    onCreateCategoryLedger: (CategoryType, String?) -> Unit,
    onPostEntryForCategoryLedger: (CategoryType, String, String) -> Unit
) {
    val cats = remember(bundle.categories, categoryType) {
        bundle.categories.filter { it.categoryType() == categoryType }
    }
    val symbol = bundle.settings.mainCurrencySymbol
    val accentColor = if (categoryType == CategoryType.INCOME) LedgerBlue else LedgerCoral
    val typeWord = if (categoryType == CategoryType.INCOME) "Income" else "Expense"

    // Map of (categoryId to ledgerLower) -> (entryCount to totalAmount)
    val ledgerStats = remember(bundle.transactions, categoryType) {
        val counts = mutableMapOf<Pair<String, String>, Int>()
        val totals = mutableMapOf<Pair<String, String>, Double>()
        bundle.transactions.forEach { tx ->
            val catId = tx.categoryId ?: return@forEach
            val sub = tx.subcategory?.trim().orEmpty()
            if (sub.isNotEmpty()) {
                val key = catId to sub.lowercase(Locale.US)
                counts[key] = (counts[key] ?: 0) + 1
                totals[key] = (totals[key] ?: 0.0) + tx.amount
            }
        }
        counts to totals
    }

    var collapsedCategoryIds by remember(categoryType) { mutableStateOf(setOf<String>()) }
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

        // Prominent Create Expense / Income Ledger Action Banner
        Surface(
            color = LedgerSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onCreateCategoryLedger(categoryType, null) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("create_category_ledger_banner_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Create $typeWord Ledger",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(cats, key = { it.id }, contentType = { "category_group" }) { cat ->
                val isExpanded = cat.id !in collapsedCategoryIds
                val chevronRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 90f else 0f,
                    animationSpec = tween(180),
                    label = "cat_chevron_${cat.id}"
                )
                val ledgerCountLabel = when (cat.subcategories.size) {
                    0 -> "No $typeWord ledgers yet"
                    1 -> "1 $typeWord ledger"
                    else -> "${cat.subcategories.size} $typeWord ledgers"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                ) {
                    // Category Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LedgerSurface)
                            .clickable {
                                collapsedCategoryIds = if (isExpanded) {
                                    collapsedCategoryIds + cat.id
                                } else {
                                    collapsedCategoryIds - cat.id
                                }
                            }
                            .padding(start = 14.dp, end = 6.dp, top = 11.dp, bottom = 11.dp)
                            .testTag("category_row_${cat.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isExpanded) "Collapse ${cat.name}" else "Expand ${cat.name}",
                            tint = LedgerTextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(LedgerFormatters.parseColorHex(cat.colorHex))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = LedgerTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ledgerCountLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = LedgerTextMuted
                            )
                        }
                        IconButton(
                            onClick = {
                                editingCat = cat
                                showDialog = true
                            },
                            modifier = Modifier.testTag("edit_category_${cat.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit category",
                                tint = LedgerTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteCategory(cat.id) },
                            modifier = Modifier.testTag("delete_category_${cat.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete category",
                                tint = LedgerTextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Nested Expense / Income Ledgers under this Category
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(tween(180)) + fadeIn(tween(150)),
                        exit = shrinkVertically(tween(150)) + fadeOut(tween(110))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            cat.subcategories.forEach { ledger ->
                                val statKey = cat.id to ledger.trim().lowercase(Locale.US)
                                val entryCount = ledgerStats.first[statKey] ?: 0
                                val totalAmt = ledgerStats.second[statKey] ?: 0.0

                                HorizontalDivider(color = LedgerDivider.copy(alpha = 0.6f), thickness = 0.5.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LedgerBackground)
                                        .clickable {
                                            onPostEntryForCategoryLedger(categoryType, cat.id, ledger)
                                        }
                                        .padding(start = 34.dp, end = 8.dp, top = 9.dp, bottom = 9.dp)
                                        .testTag("category_ledger_row_${cat.id}_$ledger"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(22.dp)
                                            .background(accentColor.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = ledger,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = LedgerTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (entryCount > 0) {
                                            Text(
                                                text = "$entryCount ${if (entryCount == 1) "entry" else "entries"} • ${LedgerFormatters.formatWholeAmount(totalAmt, symbol)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LedgerTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            onPostEntryForCategoryLedger(categoryType, cat.id, ledger)
                                        },
                                        shape = RoundedCornerShape(7.dp),
                                        color = accentColor.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.testTag("post_entry_ledger_${cat.id}_$ledger")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Post entry to $ledger",
                                                tint = accentColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Post",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = { viewModel.removeCategoryLedger(cat, ledger) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("remove_ledger_${cat.id}_$ledger")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove $ledger",
                                            tint = LedgerTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Inline "+ Add Ledger" row inside this Category
                            HorizontalDivider(color = LedgerDivider.copy(alpha = 0.5f), thickness = 0.5.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LedgerSurfaceElevated.copy(alpha = 0.5f))
                                    .clickable { onCreateCategoryLedger(categoryType, cat.id) }
                                    .padding(start = 34.dp, end = 16.dp, top = 9.dp, bottom = 9.dp)
                                    .testTag("add_ledger_to_cat_${cat.id}"),
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
                                    text = "Add $typeWord Ledger under ${cat.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
                }
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
            title = { Text(if (editingCat == null) "Add $typeWord Category" else "Edit $typeWord Category", color = LedgerTextPrimary) },
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
                        onValueChange = { subsText = it.take(800) },
                        label = { Text("$typeWord Ledgers (comma-separated)") },
                        placeholder = {
                            Text(
                                if (categoryType == CategoryType.INCOME) {
                                    "e.g. Monthly Salary, Bonus, Freelance"
                                } else {
                                    "e.g. Electricity, Fuel, Rent"
                                }
                            )
                        },
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
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel", color = LedgerTextSecondary) }
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
