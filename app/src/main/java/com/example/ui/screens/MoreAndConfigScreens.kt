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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.MainAccountGroup
import com.example.data.RepeatFrequency
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.theme.ChartColorHexes
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerGreen
import com.example.ui.theme.LedgerMotion
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerSurfaceHighlight
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.ui.theme.LocalAppearancePreferences
import com.example.ui.theme.TabularAmountStyle
import com.example.util.LedgerFormatters
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import java.util.Locale
import kotlin.math.roundToLong

enum class ConfigRoute {
    ROOT,
    ACCOUNTS_SETTING,
    DELETED_ACCOUNTS,
    INCOME_CATEGORIES,
    EXPENSE_CATEGORIES,
    BUDGET_SETTING,
    TAG_SETTING,
    REPEAT_SETTING,
    APP_LOCK,
    APPEARANCE
}

@Composable
fun MoreScreen(
    bundle: LedgerDataBundle,
    appLockEnabled: Boolean = false,
    onOpenConfiguration: () -> Unit,
    onOpenAppLock: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onExportCsv: () -> Unit,
    onImportExcel: () -> Unit,
    onMassUploadLedgers: () -> Unit,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = LocalAppearancePreferences.current
    var showLocalDataDialog by remember { mutableStateOf(false) }
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
                        title = "Theme & Display",
                        subtitle = appearance.summaryLabel(),
                        onClick = onOpenAppearance,
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

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = LedgerSurface,
            title = { Text("Quick Guide", color = LedgerTextPrimary) },
            text = {
                Text(
                    "• Trans.: Browse Daily, Calendar, Monthly, Total, and Notes views. Tap + to add Income, Expense, or Transfer entries.\n\n" +
                        "• Accounts: Track Assets, Liabilities, and Net Total across custom account groups. Tap any account for its full statement.\n\n" +
                        "• Reports: Open Stats, Net Worth, Monthly Budget, and Tagging Transactions reports from one unified list.",
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
    onAddRecurringTransaction: () -> Unit = {},
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
            LedgerMotion.pushPopTransform(goingBack = goingBack)
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
                viewModel = viewModel,
                onBack = { navigateUp() }
            )
            ConfigRoute.REPEAT_SETTING -> RepeatSettingScreen(
                bundle = bundle,
                onBack = { navigateUp() },
                onEditTransaction = onEditTransaction,
                onAddRecurringTransaction = onAddRecurringTransaction
            )
            ConfigRoute.APP_LOCK -> AppLockSettingsScreen(
                isEnabled = appLockEnabled,
                onBack = { navigateUp() },
                onLockStateChanged = onAppLockStateChanged,
                onLockNow = onLockNow
            )
            ConfigRoute.APPEARANCE -> AppearanceSettingsScreen(
                currencySymbol = bundle.settings.mainCurrencySymbol,
                onBack = { navigateUp() }
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
                val archivedTotal = bundle.archivedAccounts.size + bundle.archivedGroups.size + bundle.archivedMainGroups.size
                ConfigRow(
                    title = "Account Settings",
                    value = "${bundle.activeAccounts.size} ledgers",
                    valueColor = LedgerBlue,
                    onClick = { onNavigate(ConfigRoute.ACCOUNTS_SETTING) }
                )
                ConfigRow(
                    title = "Mass Upload Ledgers",
                    value = "Excel / CSV",
                    valueColor = LedgerBlue,
                    onClick = onMassUploadLedgers
                )
                ConfigRow(
                    title = "Deleted accounts",
                    value = if (archivedTotal > 0) "$archivedTotal" else null,
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
                val appearance = LocalAppearancePreferences.current
                ConfigRow(
                    title = "Theme & Display",
                    value = "${appearance.themeMode.label} • ${appearance.textSize.label}",
                    valueColor = LedgerAmber,
                    onClick = { onNavigate(ConfigRoute.APPEARANCE) }
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
private fun AccountsSettingScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val symbol = bundle.settings.mainCurrencySymbol
    val hierarchy = bundle.accountHierarchy

    var collapsedMainIds by remember { mutableStateOf(setOf<String>()) }
    var collapsedSubIds by remember { mutableStateOf(setOf<String>()) }

    var showAddChooserDialog by remember { mutableStateOf(false) }

    var editingMainGroup by remember { mutableStateOf<MainAccountGroup?>(null) }
    var showMainGroupDialog by remember { mutableStateOf(false) }

    var editingSubGroup by remember { mutableStateOf<AccountGroup?>(null) }
    var showSubGroupDialog by remember { mutableStateOf(false) }

    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showLedgerDialog by remember { mutableStateOf(false) }

    var pendingArchiveMainGroup by remember { mutableStateOf<MainAccountGroup?>(null) }
    var pendingArchiveSubGroup by remember { mutableStateOf<AccountGroup?>(null) }
    var pendingArchiveLedger by remember { mutableStateOf<Account?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(
            title = "Account Settings",
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = { showAddChooserDialog = true },
                    modifier = Modifier.testTag("accounts_setting_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add main group, sub group, or ledger", tint = LedgerTextPrimary)
                }
            }
        )

        // Hierarchy summary strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurfaceElevated)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Main Group → Sub Group → Ledger",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = LedgerTextSecondary
            )
            Text(
                text = "${bundle.activeMainGroups.size} • ${bundle.activeGroups.size} • ${bundle.activeAccounts.size}",
                style = MaterialTheme.typography.labelSmall,
                color = LedgerTextMuted
            )
        }
        HorizontalDivider(color = LedgerDivider)

        val assetAccentBlue = LedgerBlue
        val liabAccentCoral = LedgerCoral

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            hierarchy.forEach { mainEntry ->
                val mg = mainEntry.mainGroup
                val isMainExpanded = mg.id !in collapsedMainIds
                val mainAccent = if (mg.accountNature() == AccountNature.LIABILITY) liabAccentCoral else assetAccentBlue
                val isDefaultMain = mg.id == MainAccountGroup.DEFAULT_ASSETS_ID || mg.id == MainAccountGroup.DEFAULT_LIABILITIES_ID

                // Level 1: Main Group Row
                item(key = "mg_${mg.id}") {
                    val chevronRot by animateFloatAsState(
                        targetValue = if (isMainExpanded) 90f else 0f,
                        animationSpec = LedgerMotion.quickSpring(),
                        label = "mg_chev_${mg.id}"
                    )
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .background(LedgerSurfaceHighlight)
                                .clickable {
                                    collapsedMainIds = if (isMainExpanded) {
                                        collapsedMainIds + mg.id
                                    } else {
                                        collapsedMainIds - mg.id
                                    }
                                }
                                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                                .testTag("account_mapping_main_${mg.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(mainAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isMainExpanded) "Collapse ${mg.name}" else "Expand ${mg.name}",
                                tint = mainAccent,
                                modifier = Modifier.size(20.dp).rotate(chevronRot)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mg.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LedgerTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${mainEntry.subGroups.size} sub ${if (mainEntry.subGroups.size == 1) "group" else "groups"} • ${mainEntry.totalLedgerCount} ${if (mainEntry.totalLedgerCount == 1) "ledger" else "ledgers"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerTextSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingMainGroup = mg
                                    showMainGroupDialog = true
                                },
                                modifier = Modifier.size(40.dp).testTag("edit_main_group_${mg.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit ${mg.name}",
                                    tint = LedgerTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (!isDefaultMain) {
                                IconButton(
                                    onClick = { pendingArchiveMainGroup = mg },
                                    modifier = Modifier.size(40.dp).testTag("archive_main_group_${mg.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RemoveCircle,
                                        contentDescription = "Archive ${mg.name}",
                                        tint = LedgerCoral,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.75.dp)
                    }
                }

                if (isMainExpanded) {
                    if (mainEntry.subGroups.isEmpty()) {
                        item(key = "mg_empty_${mg.id}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LedgerSurface)
                                    .padding(start = 34.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "No sub groups under ${mg.name} yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LedgerTextMuted
                                )
                            }
                            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                        }
                    } else {
                        mainEntry.subGroups.forEach { subSec ->
                            val sub = subSec.group
                            val isSubExpanded = sub.id !in collapsedSubIds

                            // Level 2: Sub Group Row
                            item(key = "sub_${sub.id}") {
                                val subChevronRot by animateFloatAsState(
                                    targetValue = if (isSubExpanded) 90f else 0f,
                                    animationSpec = LedgerMotion.microTween(),
                                    label = "sub_chev_${sub.id}"
                                )
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 50.dp)
                                            .background(LedgerSurfaceElevated)
                                            .clickable {
                                                collapsedSubIds = if (isSubExpanded) {
                                                    collapsedSubIds + sub.id
                                                } else {
                                                    collapsedSubIds - sub.id
                                                }
                                            }
                                            .padding(start = 22.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                                            .testTag("account_mapping_sub_${sub.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(22.dp)
                                                .background(mainAccent.copy(alpha = 0.45f))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = if (isSubExpanded) "Collapse ${sub.name}" else "Expand ${sub.name}",
                                            tint = LedgerTextSecondary,
                                            modifier = Modifier.size(18.dp).rotate(subChevronRot)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sub.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LedgerTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${subSec.accounts.size} ${if (subSec.accounts.size == 1) "ledger" else "ledgers"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LedgerTextMuted
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                editingSubGroup = sub
                                                showSubGroupDialog = true
                                            },
                                            modifier = Modifier.size(38.dp).testTag("edit_sub_group_${sub.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit ${sub.name}",
                                                tint = LedgerTextSecondary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { pendingArchiveSubGroup = sub },
                                            modifier = Modifier.size(38.dp).testTag("archive_sub_group_${sub.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.RemoveCircle,
                                                contentDescription = "Archive ${sub.name}",
                                                tint = LedgerCoral.copy(alpha = 0.85f),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                }
                            }

                            // Level 3: Ledgers under Sub Group
                            if (isSubExpanded) {
                                if (subSec.accounts.isEmpty()) {
                                    item(key = "sub_empty_${sub.id}") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(LedgerSurface)
                                                .padding(start = 52.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "No ledgers yet",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LedgerTextMuted
                                            )
                                        }
                                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                                    }
                                } else {
                                    items(subSec.accounts, key = { "ledger_${it.account.id}" }) { accItem ->
                                        val balColor = if (accItem.balance.roundToLong() < 0L) LedgerCoral else LedgerBlue
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .background(LedgerSurface)
                                                    .clickable {
                                                        editingAccount = accItem.account
                                                        showLedgerDialog = true
                                                    }
                                                    .padding(start = 46.dp, end = 8.dp, top = 9.dp, bottom = 9.dp)
                                                    .testTag("account_mapping_ledger_${accItem.account.id}"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(mainAccent.copy(alpha = 0.7f))
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                    Text(
                                                        text = accItem.account.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = LedgerTextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (!accItem.account.includeInNetWorth) {
                                                        Text(
                                                            text = "Excluded from Net Worth",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = LedgerTextMuted
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = LedgerFormatters.formatSignedWhole(accItem.balance, symbol),
                                                    style = TabularAmountStyle.copy(fontSize = 13.sp),
                                                    color = balColor,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                                IconButton(
                                                    onClick = { pendingArchiveLedger = accItem.account },
                                                    modifier = Modifier.size(36.dp).testTag("archive_ledger_${accItem.account.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = "Archive ${accItem.account.name}",
                                                        tint = LedgerTextMuted,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(
                                                color = LedgerDivider,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 46.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingArchiveMainGroup?.let { mg ->
        ConfirmDestructiveDialog(
            title = "Archive Main Group?",
            message = "Archive \"${mg.name}\"? You can restore it anytime from Deleted Accounts.",
            confirmLabel = "Archive",
            confirmTestTag = "confirm_archive_main_group_button",
            onConfirm = {
                viewModel.setMainGroupArchived(mg.id, true)
                pendingArchiveMainGroup = null
            },
            onDismiss = { pendingArchiveMainGroup = null }
        )
    }

    pendingArchiveSubGroup?.let { sub ->
        ConfirmDestructiveDialog(
            title = "Archive Sub Group?",
            message = "Archive \"${sub.name}\"? You can restore it anytime from Deleted Accounts.",
            confirmLabel = "Archive",
            confirmTestTag = "confirm_archive_sub_group_button",
            onConfirm = {
                viewModel.setAccountGroupArchived(sub.id, true)
                pendingArchiveSubGroup = null
            },
            onDismiss = { pendingArchiveSubGroup = null }
        )
    }

    pendingArchiveLedger?.let { acc ->
        ConfirmDestructiveDialog(
            title = "Archive Ledger?",
            message = "Archive \"${acc.name}\"? Its past transactions will stay intact and you can restore it from Deleted Accounts.",
            confirmLabel = "Archive",
            confirmTestTag = "confirm_archive_ledger_button",
            onConfirm = {
                viewModel.setAccountArchived(acc.id, true)
                pendingArchiveLedger = null
            },
            onDismiss = { pendingArchiveLedger = null }
        )
    }

    // + Button Chooser Dialog: Main Group | Sub Group | Ledger
    if (showAddChooserDialog) {
        AlertDialog(
            onDismissRequest = { showAddChooserDialog = false },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = "Add Account Structure",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose what you would like to create:",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerTextSecondary
                    )
                    AccountHierarchyCreateOptionRow(
                        icon = Icons.Filled.AccountBalance,
                        iconTint = LedgerBlue,
                        title = "Main Group",
                        subtitle = "Top-level group (e.g. Assets, Liabilities)",
                        testTag = "add_choice_main_group",
                        onClick = {
                            showAddChooserDialog = false
                            editingMainGroup = null
                            showMainGroupDialog = true
                        }
                    )
                    AccountHierarchyCreateOptionRow(
                        icon = Icons.Filled.Folder,
                        iconTint = LedgerAmber,
                        title = "Sub Group",
                        subtitle = "Category under a Main Group (e.g. Cash, Bank)",
                        testTag = "add_choice_sub_group",
                        onClick = {
                            showAddChooserDialog = false
                            editingSubGroup = null
                            showSubGroupDialog = true
                        }
                    )
                    AccountHierarchyCreateOptionRow(
                        icon = Icons.Filled.AccountBalanceWallet,
                        iconTint = LedgerCoral,
                        title = "Ledger",
                        subtitle = "Individual account under a Sub Group",
                        testTag = "add_choice_ledger",
                        onClick = {
                            showAddChooserDialog = false
                            editingAccount = null
                            showLedgerDialog = true
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showAddChooserDialog = false },
                    modifier = Modifier.testTag("add_choice_cancel_button")
                ) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        )
    }

    // Create / Edit Main Group Dialog (Simply asks Name)
    if (showMainGroupDialog) {
        var mainGroupName by remember(editingMainGroup, showMainGroupDialog) {
            mutableStateOf(editingMainGroup?.name.orEmpty())
        }
        AlertDialog(
            onDismissRequest = { showMainGroupDialog = false },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = if (editingMainGroup == null) "New Main Group" else "Edit Main Group",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = mainGroupName,
                    onValueChange = { mainGroupName = it.take(60) },
                    label = { Text("Main Group Name") },
                    placeholder = { Text("e.g. Assets, Liabilities, Capital") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_group_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mainGroupName.isNotBlank()) {
                            viewModel.saveMainGroup(
                                existingId = editingMainGroup?.id,
                                name = mainGroupName,
                                nature = editingMainGroup?.accountNature(),
                                sortOrder = editingMainGroup?.sortOrder ?: bundle.effectiveMainGroups.size.toLong()
                            )
                            showMainGroupDialog = false
                        }
                    },
                    enabled = mainGroupName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral),
                    modifier = Modifier.testTag("main_group_save_button")
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMainGroupDialog = false }) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        )
    }

    // Create / Edit Sub Group Dialog (Asks Name and Main Group)
    if (showSubGroupDialog) {
        val activeMainGroups = bundle.activeMainGroups
        var subGroupName by remember(editingSubGroup, showSubGroupDialog) {
            mutableStateOf(editingSubGroup?.name.orEmpty())
        }
        var selectedMainGroupId by remember(editingSubGroup, showSubGroupDialog, activeMainGroups) {
            mutableStateOf(
                editingSubGroup?.resolvedMainGroupId()
                    ?: activeMainGroups.firstOrNull()?.id
                    ?: MainAccountGroup.DEFAULT_ASSETS_ID
            )
        }
        var mainGroupMenuExpanded by remember { mutableStateOf(false) }
        val selectedMainGroup = activeMainGroups.firstOrNull { it.id == selectedMainGroupId }
            ?: activeMainGroups.firstOrNull()

        AlertDialog(
            onDismissRequest = { showSubGroupDialog = false },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = if (editingSubGroup == null) "New Sub Group" else "Edit Sub Group",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = subGroupName,
                        onValueChange = { subGroupName = it.take(60) },
                        label = { Text("Sub Group Name") },
                        placeholder = { Text("e.g. Cash, Bank Accounts, Credit Cards") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sub_group_name_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Main Group",
                            style = MaterialTheme.typography.labelMedium,
                            color = LedgerTextSecondary
                        )
                        Box {
                            OutlinedButton(
                                onClick = { mainGroupMenuExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LedgerBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("sub_group_main_group_selector")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedMainGroup?.name ?: "Select Main Group",
                                        color = LedgerTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Choose Main Group",
                                        tint = LedgerTextSecondary
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = mainGroupMenuExpanded,
                                onDismissRequest = { mainGroupMenuExpanded = false },
                                containerColor = LedgerSurfaceElevated
                            ) {
                                activeMainGroups.forEach { mg ->
                                    val isChosen = mg.id == selectedMainGroup?.id
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = mg.name,
                                                color = if (isChosen) LedgerCoral else LedgerTextPrimary,
                                                fontWeight = if (isChosen) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedMainGroupId = mg.id
                                            mainGroupMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = selectedMainGroup
                        if (subGroupName.isNotBlank() && chosen != null) {
                            viewModel.saveAccountGroup(
                                existingId = editingSubGroup?.id,
                                name = subGroupName,
                                groupType = chosen.accountNature(),
                                sortOrder = editingSubGroup?.sortOrder ?: bundle.accountGroups.size.toLong(),
                                mainGroupId = chosen.id
                            )
                            showSubGroupDialog = false
                        }
                    },
                    enabled = subGroupName.isNotBlank() && selectedMainGroup != null,
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral),
                    modifier = Modifier.testTag("sub_group_save_button")
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubGroupDialog = false }) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        )
    }

    // Create / Edit Ledger Dialog (Asks Sub Group + Ledger Name & Details)
    if (showLedgerDialog) {
        AccountEditorDialog(
            bundle = bundle,
            existing = editingAccount,
            onDismiss = { showLedgerDialog = false },
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
                showLedgerDialog = false
            }
        )
    }
}

@Composable
private fun AccountHierarchyCreateOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
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
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
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
private fun DeletedAccountsScreen(
    bundle: LedgerDataBundle,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    var pendingPermanentDelete by remember { mutableStateOf<Account?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(title = "Deleted Accounts", onBack = onBack)

        if (bundle.archivedAccounts.isEmpty() && bundle.archivedGroups.isEmpty() && bundle.archivedMainGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No deleted ledgers or groups.", color = LedgerTextMuted)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (bundle.archivedMainGroups.isNotEmpty()) {
                    item { ConfigSectionHeader("Archived Main Groups") }
                    items(bundle.archivedMainGroups, key = { it.id }) { mg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LedgerSurface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mg.name, color = LedgerTextPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.setMainGroupArchived(mg.id, false) }) {
                                Icon(Icons.Filled.Restore, contentDescription = "Restore main group", tint = LedgerBlue)
                            }
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
                if (bundle.archivedGroups.isNotEmpty()) {
                    item { ConfigSectionHeader("Archived Sub Groups") }
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
                                Icon(Icons.Filled.Restore, contentDescription = "Restore sub group", tint = LedgerBlue)
                            }
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
                if (bundle.archivedAccounts.isNotEmpty()) {
                    item { ConfigSectionHeader("Archived Ledgers") }
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
                            IconButton(
                                onClick = { pendingPermanentDelete = acc },
                                modifier = Modifier.testTag("perm_delete_acc_${acc.id}")
                            ) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = "Delete permanently", tint = LedgerCoral)
                            }
                        }
                        HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    pendingPermanentDelete?.let { target ->
        ConfirmDestructiveDialog(
            title = "Delete Permanently?",
            message = "Permanently delete \"${target.name}\"? This cannot be undone.",
            confirmLabel = "Delete Forever",
            confirmTestTag = "confirm_perm_delete_account_button",
            onConfirm = {
                viewModel.deleteAccountPermanently(target.id)
                pendingPermanentDelete = null
            },
            onDismiss = { pendingPermanentDelete = null }
        )
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
    var pendingDeleteCategory by remember { mutableStateOf<Category?>(null) }
    var pendingRemoveLedger by remember { mutableStateOf<Pair<Category, String>?>(null) }

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
                    animationSpec = LedgerMotion.microTween(),
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
                            onClick = { pendingDeleteCategory = cat },
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
                        enter = LedgerMotion.expandEnter(),
                        exit = LedgerMotion.shrinkExit()
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
                                        onClick = { pendingRemoveLedger = cat to ledger },
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

    pendingDeleteCategory?.let { cat ->
        ConfirmDestructiveDialog(
            title = "Delete Category?",
            message = "Delete \"${cat.name}\" and its ledger list? Existing transactions will remain intact.",
            confirmLabel = "Delete",
            confirmTestTag = "confirm_delete_category_button",
            onConfirm = {
                viewModel.deleteCategory(cat.id)
                pendingDeleteCategory = null
            },
            onDismiss = { pendingDeleteCategory = null }
        )
    }

    pendingRemoveLedger?.let { (cat, ledgerName) ->
        ConfirmDestructiveDialog(
            title = "Remove Ledger?",
            message = "Remove \"$ledgerName\" from ${cat.name}?",
            confirmLabel = "Remove",
            confirmTestTag = "confirm_remove_category_ledger_button",
            onConfirm = {
                viewModel.removeCategoryLedger(cat, ledgerName)
                pendingRemoveLedger = null
            },
            onDismiss = { pendingRemoveLedger = null }
        )
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
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val symbol = bundle.settings.mainCurrencySymbol

    val tagStats = remember(bundle.transactions) {
        bundle.transactions
            .flatMap { tx -> tx.allEffectivePrimaryTaggings().map { it to tx } }
            .groupBy({ it.first }, { it.second })
            .entries
            .sortedByDescending { it.value.size }
    }

    var renamingTag by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ConfigTopBar(title = "Tagging Settings", onBack = onBack)

        if (tagStats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Tagging values you add to transactions will appear here. Tap any tag to rename it across all entries.",
                    color = LedgerTextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "tag_rename_hint") {
                    Text(
                        text = "Tap any tagging below to rename it across all matching transactions.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                items(tagStats, key = { it.key }) { (tag, txs) ->
                    val inc = txs.filter { it.txType() == com.example.data.TransactionType.INCOME }.sumOf { it.amount }
                    val exp = txs.filter { it.txType() == com.example.data.TransactionType.EXPENSE }.sumOf { it.amount }
                    val net = inc - exp
                    val netColor = when {
                        net.roundToLong() < 0L -> LedgerCoral
                        net.roundToLong() > 0L -> LedgerGreen
                        else -> LedgerTextPrimary
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LedgerSurface)
                            .clickable { renamingTag = tag }
                            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
                            .testTag("tag_setting_row_$tag"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(LedgerCoral)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = tag,
                                    color = LedgerTextPrimary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${txs.size} ${if (txs.size == 1) "entry" else "entries"}",
                                    color = LedgerTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Text(
                            text = LedgerFormatters.formatSignedWhole(net, symbol),
                            style = TabularAmountStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = netColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = { renamingTag = tag },
                            modifier = Modifier.testTag("rename_tag_button_$tag")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Rename $tag",
                                tint = LedgerTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                }
            }
        }
    }

    renamingTag?.let { oldTag ->
        var newName by remember(oldTag) { mutableStateOf(oldTag) }
        val trimmedNew = newName.trim()
        val matchingCount = tagStats.firstOrNull { it.key == oldTag }?.value?.size ?: 0
        val willMerge = trimmedNew.isNotEmpty() &&
            !trimmedNew.equals(oldTag, ignoreCase = true) &&
            tagStats.any { it.key.equals(trimmedNew, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { renamingTag = null },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = "Rename Tagging",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Update \"$oldTag\" across $matchingCount ${if (matchingCount == 1) "transaction" else "transactions"}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerTextSecondary
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(60) },
                        label = { Text("New Tagging Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_tag_input")
                    )
                    if (willMerge) {
                        Text(
                            text = "\"$trimmedNew\" already exists — saving will merge both tags together.",
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerAmber
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (trimmedNew.isNotEmpty() && trimmedNew != oldTag) {
                            viewModel.renameTagging(oldTag, trimmedNew)
                        }
                        renamingTag = null
                    },
                    enabled = trimmedNew.isNotEmpty() && trimmedNew != oldTag,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LedgerCoral,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("rename_tag_save_button")
                ) {
                    Text("Rename", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingTag = null }) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun RepeatSettingScreen(
    bundle: LedgerDataBundle,
    onBack: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit,
    onAddRecurringTransaction: () -> Unit
) {
    val repeating = remember(bundle.transactions) {
        bundle.transactions.filter { it.repeat() != RepeatFrequency.NONE }
    }
    val symbol = bundle.settings.mainCurrencySymbol

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ConfigTopBar(
                title = "Repeat Setting",
                onBack = onBack,
                trailing = {
                    IconButton(
                        onClick = onAddRecurringTransaction,
                        modifier = Modifier.testTag("repeat_add_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create recurring entry",
                            tint = LedgerCoral
                        )
                    }
                }
            )
            if (repeating.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "No recurring transactions configured yet.",
                            style = MaterialTheme.typography.titleSmall,
                            color = LedgerTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Create a recurring entry directly here, or tap Rep/Inst. when adding any transaction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LedgerTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onAddRecurringTransaction,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LedgerCoral,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("repeat_empty_create_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Recurring Entry", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    items(repeating, key = { it.id }) { tx ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LedgerSurfaceElevated)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.repeatLabel(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LedgerCoral
                                )
                                Text(
                                    text = LedgerFormatters.formatShortDate(tx.epochDay),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerTextMuted
                                )
                            }
                            TransactionRowItem(
                                tx = tx,
                                bundle = bundle,
                                symbol = symbol,
                                onClick = { onEditTransaction(tx) }
                            )
                            HorizontalDivider(color = LedgerDivider, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        if (repeating.isNotEmpty()) {
            androidx.compose.material3.FloatingActionButton(
                onClick = onAddRecurringTransaction,
                containerColor = LedgerCoral,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("repeat_fab_add_entry")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create recurring entry"
                )
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
