package com.example.data

import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

class LocalRoomLedgerRepository(
    private val dao: LocalLedgerDao
) : LedgerDataSource {

    private companion object {
        const val SUBCAT_DELIMITER = "\u001F"
    }

    override fun observeSettings(userId: String): Flow<UserSettings> =
        dao.observeSettings(userId).map { entity ->
            if (entity == null) {
                UserSettings(id = userId, userId = userId)
            } else {
                UserSettings(
                    id = entity.userId,
                    userId = entity.userId,
                    mainCurrencyCode = entity.mainCurrencyCode,
                    mainCurrencySymbol = entity.mainCurrencySymbol,
                    subcategoryEnabled = entity.subcategoryEnabled,
                    startScreen = entity.startScreen,
                    monthlyStartDate = entity.monthlyStartDate,
                    weeklyStartDay = entity.weeklyStartDay,
                    seeded = entity.seeded
                )
            }
        }

    override fun observeMainGroups(userId: String): Flow<List<MainAccountGroup>> =
        dao.observeMainGroups(userId).map { list ->
            list.map {
                MainAccountGroup(
                    id = it.id,
                    userId = it.userId,
                    name = it.name,
                    nature = it.nature,
                    sortOrder = it.sortOrder,
                    archived = it.archived
                )
            }
        }

    override fun observeAccountGroups(userId: String): Flow<List<AccountGroup>> =
        dao.observeAccountGroups(userId).map { list ->
            list.map {
                AccountGroup(
                    id = it.id,
                    userId = it.userId,
                    mainGroupId = it.mainGroupId,
                    name = it.name,
                    groupType = it.groupType,
                    sortOrder = it.sortOrder,
                    archived = it.archived
                )
            }
        }

    override fun observeAccounts(userId: String): Flow<List<Account>> =
        dao.observeAccounts(userId).map { list ->
            list.map {
                Account(
                    id = it.id,
                    userId = it.userId,
                    groupId = it.groupId,
                    name = it.name,
                    initialBalance = it.initialBalance,
                    accountType = it.accountType,
                    note = it.note,
                    sortOrder = it.sortOrder,
                    archived = it.archived,
                    includeInNetWorth = it.includeInNetWorth
                )
            }
        }

    override fun observeCategories(userId: String): Flow<List<Category>> =
        dao.observeCategories(userId).map { list ->
            list.map {
                Category(
                    id = it.id,
                    userId = it.userId,
                    name = it.name,
                    type = it.type,
                    colorHex = it.colorHex,
                    subcategories = if (it.subcategoriesSerialized.isEmpty()) {
                        emptyList()
                    } else {
                        it.subcategoriesSerialized.split(SUBCAT_DELIMITER).filter { s -> s.isNotEmpty() }
                    },
                    sortOrder = it.sortOrder
                )
            }
        }

    override fun observeTransactions(userId: String): Flow<List<LedgerTransaction>> =
        dao.observeTransactions(userId).map { list ->
            list.map {
                LedgerTransaction(
                    id = it.id,
                    userId = it.userId,
                    type = it.type,
                    amount = it.amount,
                    feeAmount = it.feeAmount,
                    epochDay = it.epochDay,
                    timestampMillis = it.timestampMillis,
                    accountId = it.accountId,
                    toAccountId = it.toAccountId,
                    categoryId = it.categoryId,
                    subcategory = it.subcategory,
                    note = it.note,
                    note2 = it.note2,
                    tagging = it.tagging,
                    toTagging = it.toTagging,
                    description = it.description,
                    tags = emptyList(),
                    favorite = it.favorite,
                    repeatFrequency = it.repeatFrequency
                )
            }
        }

    override fun observeBudgets(userId: String): Flow<List<Budget>> =
        dao.observeBudgets(userId).map { list ->
            list.map {
                Budget(
                    id = it.id,
                    userId = it.userId,
                    categoryId = it.categoryId,
                    subcategory = it.subcategory,
                    monthlyLimit = it.monthlyLimit
                )
            }
        }

    override fun observeNotes(userId: String): Flow<List<FinanceNote>> =
        dao.observeNotes(userId).map { list ->
            list.map {
                FinanceNote(
                    id = it.id,
                    userId = it.userId,
                    title = it.title,
                    content = it.content,
                    pinned = it.pinned,
                    updatedAt = Timestamp(Date(it.updatedAtMillis))
                )
            }
        }

    override suspend fun ensureInitialized(userId: String): Result<Unit> {
        return try {
            val existingMainGroups = dao.getMainGroups(userId)
            if (existingMainGroups.isEmpty()) {
                dao.upsertMainGroups(
                    DefaultLedgerSeed.defaultMainGroups.map { mg ->
                        LocalMainGroupEntity(
                            id = mg.id,
                            userId = userId,
                            name = mg.name,
                            nature = mg.nature.name,
                            sortOrder = mg.sortOrder,
                            archived = false
                        )
                    }
                )
            }
            val existing = dao.getSettings(userId)
            if (existing != null && existing.seeded) {
                return Result.success(Unit)
            }
            dao.upsertSettings(
                LocalUserSettingsEntity(
                    userId = userId,
                    mainCurrencyCode = existing?.mainCurrencyCode ?: "INR",
                    mainCurrencySymbol = existing?.mainCurrencySymbol ?: "₹",
                    subcategoryEnabled = existing?.subcategoryEnabled ?: true,
                    startScreen = existing?.startScreen ?: "DAILY",
                    monthlyStartDate = existing?.monthlyStartDate ?: 1L,
                    weeklyStartDay = existing?.weeklyStartDay ?: "MONDAY",
                    seeded = true
                )
            )
            dao.upsertAccountGroups(
                DefaultLedgerSeed.defaultGroups.map { g ->
                    LocalAccountGroupEntity(
                        id = g.id,
                        userId = userId,
                        mainGroupId = g.mainGroupId,
                        name = g.name,
                        groupType = g.nature.name,
                        sortOrder = g.sortOrder,
                        archived = false
                    )
                }
            )
            dao.upsertAccounts(
                DefaultLedgerSeed.defaultAccounts.map { a ->
                    LocalAccountEntity(
                        id = a.id,
                        userId = userId,
                        groupId = a.groupId,
                        name = a.name,
                        initialBalance = 0.0,
                        accountType = a.nature.name,
                        note = null,
                        sortOrder = a.sortOrder,
                        archived = false,
                        includeInNetWorth = true
                    )
                }
            )
            dao.upsertCategories(
                DefaultLedgerSeed.defaultCategories.map { c ->
                    LocalCategoryEntity(
                        id = c.id,
                        userId = userId,
                        name = c.name,
                        type = c.type.name,
                        colorHex = c.colorHex,
                        subcategoriesSerialized = c.subcategories.joinToString(SUBCAT_DELIMITER),
                        sortOrder = c.sortOrder
                    )
                }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSettings(
        userId: String,
        mainCurrencyCode: String,
        mainCurrencySymbol: String,
        subcategoryEnabled: Boolean,
        startScreen: String,
        monthlyStartDate: Long,
        weeklyStartDay: String
    ): Result<Unit> = runSafely {
        val current = dao.getSettings(userId)
        dao.upsertSettings(
            LocalUserSettingsEntity(
                userId = userId,
                mainCurrencyCode = mainCurrencyCode,
                mainCurrencySymbol = mainCurrencySymbol,
                subcategoryEnabled = subcategoryEnabled,
                startScreen = startScreen,
                monthlyStartDate = monthlyStartDate.coerceIn(1L, 28L),
                weeklyStartDay = weeklyStartDay,
                seeded = current?.seeded ?: true
            )
        )
    }

    override suspend fun saveMainGroup(
        userId: String,
        existingId: String?,
        name: String,
        nature: AccountNature,
        sortOrder: Long
    ): Result<Unit> {
        val cleanName = name.trim().take(60)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Main group name cannot be empty"))
        return runSafely {
            val id = existingId?.takeIf { it.isNotBlank() } ?: "mg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val existing = dao.getMainGroup(id)
            dao.upsertMainGroup(
                LocalMainGroupEntity(
                    id = id,
                    userId = userId,
                    name = cleanName,
                    nature = nature.name,
                    sortOrder = sortOrder.coerceIn(0L, 10000L),
                    archived = existing?.archived ?: false
                )
            )
        }
    }

    override suspend fun setMainGroupArchived(userId: String, mainGroupId: String, archived: Boolean): Result<Unit> =
        runSafely { dao.setMainGroupArchived(userId, mainGroupId, archived) }

    override suspend fun deleteMainGroup(userId: String, mainGroupId: String): Result<Unit> =
        runSafely { dao.deleteMainGroup(userId, mainGroupId) }

    override suspend fun saveAccountGroup(
        userId: String,
        existingId: String?,
        name: String,
        groupType: AccountNature,
        sortOrder: Long,
        mainGroupId: String
    ): Result<Unit> {
        val cleanName = name.trim().take(60)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Sub group name cannot be empty"))
        return runSafely {
            val id = existingId?.takeIf { it.isNotBlank() } ?: "grp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val existing = dao.getAccountGroup(id)
            val resolvedMainId = mainGroupId.takeIf { it.isNotBlank() }
                ?: existing?.mainGroupId?.takeIf { it.isNotBlank() }
                ?: if (groupType == AccountNature.LIABILITY) {
                    MainAccountGroup.DEFAULT_LIABILITIES_ID
                } else {
                    MainAccountGroup.DEFAULT_ASSETS_ID
                }
            dao.upsertAccountGroup(
                LocalAccountGroupEntity(
                    id = id,
                    userId = userId,
                    mainGroupId = resolvedMainId,
                    name = cleanName,
                    groupType = groupType.name,
                    sortOrder = sortOrder.coerceIn(0L, 10000L),
                    archived = existing?.archived ?: false
                )
            )
        }
    }

    override suspend fun setAccountGroupArchived(userId: String, groupId: String, archived: Boolean): Result<Unit> =
        runSafely { dao.setAccountGroupArchived(userId, groupId, archived) }

    override suspend fun deleteAccountGroup(userId: String, groupId: String): Result<Unit> =
        runSafely { dao.deleteAccountGroup(userId, groupId) }

    override suspend fun saveAccount(
        userId: String,
        existingId: String?,
        groupId: String,
        name: String,
        initialBalance: Double,
        accountType: AccountNature,
        note: String?,
        sortOrder: Long,
        includeInNetWorth: Boolean
    ): Result<Unit> {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Account name cannot be empty"))
        if (groupId.isBlank()) return Result.failure(IllegalArgumentException("Please select an account group"))
        return runSafely {
            val id = existingId?.takeIf { it.isNotBlank() } ?: "acc_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val existing = dao.getAccount(id)
            dao.upsertAccount(
                LocalAccountEntity(
                    id = id,
                    userId = userId,
                    groupId = groupId,
                    name = cleanName,
                    initialBalance = initialBalance,
                    accountType = accountType.name,
                    note = note?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
                    sortOrder = sortOrder.coerceIn(0L, 10000L),
                    archived = existing?.archived ?: false,
                    includeInNetWorth = includeInNetWorth
                )
            )
        }
    }

    override suspend fun setAccountArchived(userId: String, accountId: String, archived: Boolean): Result<Unit> =
        runSafely { dao.setAccountArchived(userId, accountId, archived) }

    override suspend fun deleteAccount(userId: String, accountId: String): Result<Unit> =
        runSafely { dao.deleteAccount(userId, accountId) }

    override suspend fun saveCategory(
        userId: String,
        existingId: String?,
        name: String,
        type: CategoryType,
        colorHex: String,
        subcategories: List<String>,
        sortOrder: Long
    ): Result<Unit> {
        val cleanName = name.trim().take(60)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        val safeHex = if (Regex("^#[0-9A-Fa-f]{6}$").matches(colorHex)) colorHex else "#F06458"
        val cleanSubs = subcategories.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(200)
        return runSafely {
            val id = existingId?.takeIf { it.isNotBlank() } ?: "cat_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            dao.upsertCategory(
                LocalCategoryEntity(
                    id = id,
                    userId = userId,
                    name = cleanName,
                    type = type.name,
                    colorHex = safeHex,
                    subcategoriesSerialized = cleanSubs.joinToString(SUBCAT_DELIMITER),
                    sortOrder = sortOrder.coerceIn(0L, 10000L)
                )
            )
        }
    }

    override suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> =
        runSafely { dao.deleteCategory(userId, categoryId) }

    override suspend fun saveTransaction(
        userId: String,
        existingId: String?,
        tx: LedgerTransaction
    ): Result<Unit> {
        if (tx.amount <= 0.0) return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        if (tx.accountId.isBlank()) return Result.failure(IllegalArgumentException("Please select an account"))
        if (tx.txType() == TransactionType.TRANSFER && tx.toAccountId.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Please select a destination account"))
        }
        return runSafely {
            val id = existingId?.takeIf { it.isNotBlank() } ?: "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            dao.upsertTransaction(
                LocalTransactionEntity(
                    id = id,
                    userId = userId,
                    type = tx.txType().name,
                    amount = tx.amount,
                    feeAmount = tx.feeAmount.coerceAtLeast(0.0),
                    epochDay = tx.epochDay,
                    timestampMillis = tx.timestampMillis.coerceAtLeast(0L),
                    accountId = tx.accountId,
                    toAccountId = tx.toAccountId?.takeIf { it.isNotBlank() },
                    categoryId = tx.categoryId?.takeIf { it.isNotBlank() },
                    subcategory = tx.subcategory?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
                    note = tx.note.trim().take(140),
                    note2 = tx.note2.trim().take(140),
                    tagging = tx.tagging?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
                    toTagging = tx.toTagging?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
                    description = tx.description?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
                    favorite = tx.favorite,
                    repeatFrequency = tx.normalizedRepeatFrequency()
                )
            )
        }
    }

    override suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> =
        runSafely { dao.deleteTransaction(userId, transactionId) }

    override suspend fun renameTagging(userId: String, oldTag: String, newTag: String): Result<Int> {
        val cleanOld = oldTag.trim()
        val cleanNew = newTag.trim().take(60)
        if (cleanOld.isEmpty() || cleanNew.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tagging name cannot be empty"))
        }
        return try {
            val primaryCount = dao.renameTransactionTagging(userId, cleanOld, cleanNew)
            val toCount = dao.renameTransactionToTagging(userId, cleanOld, cleanNew)
            Result.success(primaryCount + toCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveBudget(
        userId: String,
        categoryId: String,
        monthlyLimit: Double,
        subcategory: String?
    ): Result<Unit> {
        if (categoryId.isBlank()) return Result.failure(IllegalArgumentException("Category required"))
        val cleanSub = subcategory?.trim()?.take(60)?.takeIf { it.isNotEmpty() }
        return runSafely {
            dao.upsertBudget(
                LocalBudgetEntity(
                    id = Budget.buildDocId(categoryId, cleanSub),
                    userId = userId,
                    categoryId = categoryId,
                    subcategory = cleanSub,
                    monthlyLimit = monthlyLimit.coerceAtLeast(0.0)
                )
            )
        }
    }

    override suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> =
        runSafely { dao.deleteBudget(userId, budgetId) }

    override suspend fun saveNote(
        userId: String,
        existingId: String?,
        title: String,
        content: String,
        pinned: Boolean
    ): Result<Unit> {
        val cleanTitle = title.trim().take(100)
        if (cleanTitle.isEmpty()) return Result.failure(IllegalArgumentException("Note title cannot be empty"))
        return runSafely {
            val id = existingId?.takeIf { it.isNotBlank() } ?: "note_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            dao.upsertNote(
                LocalFinanceNoteEntity(
                    id = id,
                    userId = userId,
                    title = cleanTitle,
                    content = content.trim().take(2000),
                    pinned = pinned,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteNote(userId: String, noteId: String): Result<Unit> =
        runSafely { dao.deleteNote(userId, noteId) }

    private suspend inline fun runSafely(block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
