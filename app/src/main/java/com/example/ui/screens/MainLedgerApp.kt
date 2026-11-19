package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.CategoryType
import com.example.data.LedgerTransaction
import com.example.data.LocalLedgerDatabase
import com.example.data.LocalRoomLedgerRepository
import com.example.data.MainTab
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerNavBar
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.util.LedgerExcelExporter
import com.example.viewmodel.LedgerDataBundle
import com.example.viewmodel.LedgerViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface ActiveOverlay {
    data object None : ActiveOverlay
    data class AddEditTx(val existing: LedgerTransaction?) : ActiveOverlay
    data class AccountDetail(val accountId: String) : ActiveOverlay
    data class Configuration(val route: ConfigRoute) : ActiveOverlay
}

private const val LOCAL_TESTING_USER_ID = "local_device_user"

@Composable
fun AppNavigation() {
    LocalLedgerShell(currentUserId = LOCAL_TESTING_USER_ID)
}

@Composable
private fun LocalLedgerShell(
    currentUserId: String
) {
    val viewModel: LedgerViewModel = viewModel(
        key = "local_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource = LocalRoomLedgerRepository(
                    LocalLedgerDatabase.getInstance(app).ledgerDao()
                )
                LedgerViewModel(dataSource, currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(MainTab.TRANSACTIONS) }
    var overlay by remember { mutableStateOf<ActiveOverlay>(ActiveOverlay.None) }
    var exportDialogBundle by remember { mutableStateOf<LedgerDataBundle?>(null) }
    var pendingSaveBundle by remember { mutableStateOf<LedgerDataBundle?>(null) }

    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(LedgerExcelExporter.XLSX_MIME_TYPE)
    ) { uri ->
        val targetBundle = pendingSaveBundle
        pendingSaveBundle = null
        if (uri != null && targetBundle != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            LedgerExcelExporter.writeXlsx(targetBundle, out)
                        } ?: error("Unable to open output stream")
                    }.isSuccess
                }
                snackbarHostState.showSnackbar(
                    if (ok) "Excel report (.xlsx) saved" else "Failed to save Excel file"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun shareExcel(bundle: LedgerDataBundle) {
        scope.launch {
            val uri = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                    val file = File(dir, LedgerExcelExporter.defaultFileName())
                    file.outputStream().use { out ->
                        LedgerExcelExporter.writeXlsx(bundle, out)
                    }
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }.getOrNull()
            }
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = LedgerExcelExporter.XLSX_MIME_TYPE
                    putExtra(Intent.EXTRA_SUBJECT, "Money Ledger Excel Report")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Excel Report (.xlsx)"))
            } else {
                snackbarHostState.showSnackbar("Failed to generate Excel file")
            }
        }
    }

    fun shareCsv(bundle: LedgerDataBundle) {
        val csv = viewModel.buildCsvExport(bundle)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Money Ledger Double-Entry Export")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        context.startActivity(Intent.createChooser(intent, "Share Ledger CSV"))
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LedgerBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = LedgerCoral)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading your ledger...", color = LedgerTextSecondary)
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LedgerBackground)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unable to load ledger",
                        style = MaterialTheme.typography.titleLarge,
                        color = LedgerTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        is UiState.Success -> {
            val bundle = state.data

            when (val currentOverlay = overlay) {
                is ActiveOverlay.AddEditTx -> {
                    AddEditTransactionScreen(
                        bundle = bundle,
                        existing = currentOverlay.existing,
                        onBack = { overlay = ActiveOverlay.None },
                        onSave = { existingId, tx, keepOpen ->
                            viewModel.saveTransaction(existingId, tx) { ok ->
                                if (ok && !keepOpen) {
                                    overlay = ActiveOverlay.None
                                }
                            }
                        },
                        onCopy = if (currentOverlay.existing != null) {
                            { copiedTx ->
                                viewModel.copyTransaction(copiedTx) { ok ->
                                    if (ok) overlay = ActiveOverlay.None
                                }
                            }
                        } else null,
                        onDelete = if (currentOverlay.existing != null) {
                            { id ->
                                viewModel.deleteTransaction(id) { ok ->
                                    if (ok) overlay = ActiveOverlay.None
                                }
                            }
                        } else null,
                        onManageAccounts = {
                            overlay = ActiveOverlay.Configuration(ConfigRoute.ACCOUNTS_SETTING)
                        },
                        onManageCategories = { catType ->
                            overlay = ActiveOverlay.Configuration(
                                if (catType == CategoryType.INCOME) ConfigRoute.INCOME_CATEGORIES
                                else ConfigRoute.EXPENSE_CATEGORIES
                            )
                        }
                    )
                }

                is ActiveOverlay.AccountDetail -> {
                    AccountDetailScreen(
                        accountId = currentOverlay.accountId,
                        bundle = bundle,
                        viewModel = viewModel,
                        onBack = { overlay = ActiveOverlay.None },
                        onEditTransaction = { tx ->
                            overlay = ActiveOverlay.AddEditTx(tx)
                        }
                    )
                }

                is ActiveOverlay.Configuration -> {
                    ConfigurationHostScreen(
                        initialRoute = currentOverlay.route,
                        bundle = bundle,
                        viewModel = viewModel,
                        onBackToMain = { overlay = ActiveOverlay.None },
                        onEditTransaction = { tx ->
                            overlay = ActiveOverlay.AddEditTx(tx)
                        }
                    )
                }

                ActiveOverlay.None -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(LedgerBackground)) {
                        val useRail = maxWidth >= 680.dp

                        if (useRail) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationRail(
                                    containerColor = LedgerNavBar,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MainTab.entries.forEach { tab ->
                                        val selected = activeTab == tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { activeTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = tabIcon(tab, selected),
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label, fontSize = 11.sp) },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = LedgerCoral,
                                                selectedTextColor = LedgerCoral,
                                                unselectedIconColor = LedgerTextMuted,
                                                unselectedTextColor = LedgerTextMuted,
                                                indicatorColor = LedgerCoral.copy(alpha = 0.14f)
                                            )
                                        )
                                    }
                                }
                                Scaffold(
                                    containerColor = LedgerBackground,
                                    snackbarHost = { SnackbarHost(snackbarHostState) },
                                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                    modifier = Modifier.weight(1f)
                                ) { innerPadding ->
                                    MainTabContent(
                                        activeTab = activeTab,
                                        bundle = bundle,
                                        filterState = filterState,
                                        viewModel = viewModel,
                                        onAddTransaction = { overlay = ActiveOverlay.AddEditTx(null) },
                                        onEditTransaction = { tx -> overlay = ActiveOverlay.AddEditTx(tx) },
                                        onSelectAccount = { acc -> overlay = ActiveOverlay.AccountDetail(acc.id) },
                                        onOpenConfig = { route -> overlay = ActiveOverlay.Configuration(route) },
                                        onExportCsv = { exportDialogBundle = bundle },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        } else {
                            Scaffold(
                                containerColor = LedgerBackground,
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                contentWindowInsets = WindowInsets.statusBars,
                                bottomBar = {
                                    LedgerBottomBar(
                                        activeTab = activeTab,
                                        onSelectTab = { activeTab = it }
                                    )
                                }
                            ) { innerPadding ->
                                MainTabContent(
                                    activeTab = activeTab,
                                    bundle = bundle,
                                    filterState = filterState,
                                    viewModel = viewModel,
                                    onAddTransaction = { overlay = ActiveOverlay.AddEditTx(null) },
                                    onEditTransaction = { tx -> overlay = ActiveOverlay.AddEditTx(tx) },
                                    onSelectAccount = { acc -> overlay = ActiveOverlay.AccountDetail(acc.id) },
                                    onOpenConfig = { route -> overlay = ActiveOverlay.Configuration(route) },
                                    onExportCsv = { exportDialogBundle = bundle },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }

            exportDialogBundle?.let { targetBundle ->
                val rowCount = remember(targetBundle.transactions) {
                    LedgerExcelExporter.buildDoubleEntryRows(targetBundle).size
                }
                AlertDialog(
                    onDismissRequest = { exportDialogBundle = null },
                    containerColor = LedgerSurface,
                    title = {
                        Text(
                            text = "Export to Excel (.xlsx)",
                            color = LedgerTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "${targetBundle.transactions.size} entries will be exported as $rowCount double-entry lines (Debit + / Credit -).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LedgerTextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LedgerSurfaceElevated,
                                border = BorderStroke(1.dp, LedgerBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Columns: Date • Account • Note • Tag • Description / Remarks • Amount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerTextPrimary
                                    )
                                    Text(
                                        text = "• Expense: Category Debit (+), Account Credit (-)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerTextMuted
                                    )
                                    Text(
                                        text = "• Income: Account Debit (+), Category Credit (-)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerTextMuted
                                    )
                                    Text(
                                        text = "• Transfer: To Account Debit (+), From Account Credit (-)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerTextMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Button(
                                onClick = {
                                    exportDialogBundle = null
                                    pendingSaveBundle = targetBundle
                                    createExcelLauncher.launch(LedgerExcelExporter.defaultFileName())
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LedgerCoral,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("export_save_excel_button")
                            ) {
                                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Excel File (.xlsx)", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    exportDialogBundle = null
                                    shareExcel(targetBundle)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, LedgerBlue.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("export_share_excel_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Excel File (.xlsx)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                exportDialogBundle = null
                                shareCsv(targetBundle)
                            },
                            modifier = Modifier.testTag("export_share_csv_button")
                        ) {
                            Text("Share CSV", color = LedgerTextSecondary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { exportDialogBundle = null }) {
                            Text("Cancel", color = LedgerTextMuted)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainTabContent(
    activeTab: MainTab,
    bundle: LedgerDataBundle,
    filterState: com.example.viewmodel.FilterState,
    viewModel: LedgerViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit,
    onSelectAccount: (com.example.data.Account) -> Unit,
    onOpenConfig: (ConfigRoute) -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (activeTab) {
        MainTab.TRANSACTIONS -> TransactionsScreen(
            bundle = bundle,
            filter = filterState,
            viewModel = viewModel,
            onAddTransaction = onAddTransaction,
            onEditTransaction = onEditTransaction,
            onExportCsv = onExportCsv,
            onOpenBudgets = { onOpenConfig(ConfigRoute.BUDGET_SETTING) },
            modifier = modifier
        )

        MainTab.STATS -> StatsScreen(
            bundle = bundle,
            filter = filterState,
            viewModel = viewModel,
            onEditTransaction = onEditTransaction,
            modifier = modifier
        )

        MainTab.REPORTS -> ReportsScreen(
            bundle = bundle,
            viewModel = viewModel,
            onSelectAccount = onSelectAccount,
            modifier = modifier
        )

        MainTab.ACCOUNTS -> AccountsScreen(
            bundle = bundle,
            viewModel = viewModel,
            onSelectAccount = onSelectAccount,
            onOpenAccountsSetting = { onOpenConfig(ConfigRoute.ACCOUNTS_SETTING) },
            onOpenAccountGroups = { onOpenConfig(ConfigRoute.ACCOUNT_GROUPS) },
            modifier = modifier
        )

        MainTab.MORE -> MoreScreen(
            bundle = bundle,
            onOpenConfiguration = { onOpenConfig(ConfigRoute.ROOT) },
            onExportCsv = onExportCsv,
            onOpenBudgets = { onOpenConfig(ConfigRoute.BUDGET_SETTING) },
            modifier = modifier
        )
    }
}

@Composable
private fun LedgerBottomBar(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LedgerNavBar)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        HorizontalDivider(color = LedgerDivider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val selected = activeTab == tab
                val tint = if (selected) LedgerCoral else LedgerTextMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelectTab(tab) }
                        .testTag("bottom_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tabIcon(tab, selected),
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean) = when (tab) {
    MainTab.TRANSACTIONS -> if (selected) Icons.Filled.Book else Icons.Outlined.Book
    MainTab.STATS -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
    MainTab.REPORTS -> if (selected) Icons.Filled.Assessment else Icons.Outlined.Assessment
    MainTab.ACCOUNTS -> if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
    MainTab.MORE -> if (selected) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz
}
