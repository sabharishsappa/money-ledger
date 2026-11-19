package com.example.data

import kotlinx.coroutines.flow.Flow

interface LedgerDataSource {
    fun observeSettings(userId: String): Flow<UserSettings>
    fun observeAccountGroups(userId: String): Flow<List<AccountGroup>>
    fun observeAccounts(userId: String): Flow<List<Account>>
    fun observeCategories(userId: String): Flow<List<Category>>
    fun observeTransactions(userId: String): Flow<List<LedgerTransaction>>
    fun observeBudgets(userId: String): Flow<List<Budget>>
    fun observeNotes(userId: String): Flow<List<FinanceNote>>

    suspend fun ensureInitialized(userId: String): Result<Unit>

    suspend fun updateSettings(
        userId: String,
        mainCurrencyCode: String,
        mainCurrencySymbol: String,
        subcategoryEnabled: Boolean,
        startScreen: String,
        monthlyStartDate: Long,
        weeklyStartDay: String
    ): Result<Unit>

    suspend fun saveAccountGroup(
        userId: String,
        existingId: String?,
        name: String,
        groupType: AccountNature,
        sortOrder: Long
    ): Result<Unit>

    suspend fun setAccountGroupArchived(userId: String, groupId: String, archived: Boolean): Result<Unit>
    suspend fun deleteAccountGroup(userId: String, groupId: String): Result<Unit>

    suspend fun saveAccount(
        userId: String,
        existingId: String?,
        groupId: String,
        name: String,
        initialBalance: Double,
        accountType: AccountNature,
        note: String?,
        sortOrder: Long,
        includeInNetWorth: Boolean = true
    ): Result<Unit>

    suspend fun setAccountArchived(userId: String, accountId: String, archived: Boolean): Result<Unit>
    suspend fun deleteAccount(userId: String, accountId: String): Result<Unit>

    suspend fun saveCategory(
        userId: String,
        existingId: String?,
        name: String,
        type: CategoryType,
        colorHex: String,
        subcategories: List<String>,
        sortOrder: Long
    ): Result<Unit>

    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit>

    suspend fun saveTransaction(
        userId: String,
        existingId: String?,
        tx: LedgerTransaction
    ): Result<Unit>

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit>

    suspend fun saveBudget(userId: String, categoryId: String, monthlyLimit: Double): Result<Unit>
    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit>

    suspend fun saveNote(
        userId: String,
        existingId: String?,
        title: String,
        content: String,
        pinned: Boolean
    ): Result<Unit>

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit>
}
