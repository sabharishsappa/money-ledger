package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LedgerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : LedgerDataSource {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing Firestore." }
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user operation." }
        }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun groupsCol(uid: String) = userDoc(uid).collection("accountGroups")
    private fun accountsCol(uid: String) = userDoc(uid).collection("accounts")
    private fun categoriesCol(uid: String) = userDoc(uid).collection("categories")
    private fun transactionsCol(uid: String) = userDoc(uid).collection("transactions")
    private fun budgetsCol(uid: String) = userDoc(uid).collection("budgets")
    private fun notesCol(uid: String) = userDoc(uid).collection("notes")

    private fun <T> Flow<T>.logAndRethrow(uid: String, operation: OperationType, path: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, operation, path)
            }
            throw error
        }

    // --- Real-Time Streams ---

    override fun observeSettings(userId: String): Flow<UserSettings> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UserSettings(userId = uid)
                }
                .logAndRethrow(uid, OperationType.GET, path)
        )
    }

    override fun observeAccountGroups(userId: String): Flow<List<AccountGroup>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/accountGroups"
        emitAll(
            groupsCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AccountGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .logAndRethrow(uid, OperationType.LIST, path)
        )
    }

    override fun observeAccounts(userId: String): Flow<List<Account>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/accounts"
        emitAll(
            accountsCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Account::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .logAndRethrow(uid, OperationType.LIST, path)
        )
    }

    override fun observeCategories(userId: String): Flow<List<Category>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/categories"
        emitAll(
            categoriesCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .logAndRethrow(uid, OperationType.LIST, path)
        )
    }

    override fun observeTransactions(userId: String): Flow<List<LedgerTransaction>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/transactions"
        emitAll(
            transactionsCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LedgerTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<LedgerTransaction> { it.epochDay }
                                .thenByDescending { it.timestampMillis }
                        )
                }
                .logAndRethrow(uid, OperationType.LIST, path)
        )
    }

    override fun observeBudgets(userId: String): Flow<List<Budget>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/budgets"
        emitAll(
            budgetsCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Budget::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logAndRethrow(uid, OperationType.LIST, path)
        )
    }

    override fun observeNotes(userId: String): Flow<List<FinanceNote>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/notes"
        emitAll(
            notesCol(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FinanceNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<FinanceNote> { it.pinned }
                                .thenByDescending { it.updatedAt?.seconds ?: 0L }
                        )
                }
                .logAndRethrow(uid, OperationType.LIST, path)
        )
    }

    // --- Initial Bootstrap for Brand-New Users ---

    override suspend fun ensureInitialized(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val snap = userDoc(uid).get().await()
            val alreadySeeded = snap.exists() && (snap.getBoolean("seeded") == true)
            if (alreadySeeded) return Result.success(Unit)

            val batch = db.batch()
            if (!snap.exists()) {
                val settings = UserSettings(userId = uid, seeded = true)
                batch.set(userDoc(uid), settings.toCreateMap(uid))
            } else {
                batch.update(
                    userDoc(uid),
                    mapOf(
                        "seeded" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            DefaultLedgerSeed.defaultGroups.forEach { g ->
                val model = AccountGroup(
                    userId = uid,
                    name = g.name,
                    groupType = g.nature.name,
                    sortOrder = g.sortOrder,
                    archived = false
                )
                batch.set(groupsCol(uid).document(g.id), model.toCreateMap(uid))
            }

            DefaultLedgerSeed.defaultAccounts.forEach { a ->
                val model = Account(
                    userId = uid,
                    groupId = a.groupId,
                    name = a.name,
                    initialBalance = 0.0,
                    accountType = a.nature.name,
                    sortOrder = a.sortOrder,
                    archived = false
                )
                batch.set(accountsCol(uid).document(a.id), model.toCreateMap(uid))
            }

            DefaultLedgerSeed.defaultCategories.forEach { c ->
                val model = Category(
                    userId = uid,
                    name = c.name,
                    type = c.type.name,
                    colorHex = c.colorHex,
                    subcategories = c.subcategories,
                    sortOrder = c.sortOrder
                )
                batch.set(categoriesCol(uid).document(c.id), model.toCreateMap(uid))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- User Settings ---

    override suspend fun updateSettings(
        userId: String,
        mainCurrencyCode: String,
        mainCurrencySymbol: String,
        subcategoryEnabled: Boolean,
        startScreen: String,
        monthlyStartDate: Long,
        weeklyStartDay: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val updates = mapOf(
                "mainCurrencyCode" to mainCurrencyCode,
                "mainCurrencySymbol" to mainCurrencySymbol,
                "subcategoryEnabled" to subcategoryEnabled,
                "startScreen" to startScreen,
                "monthlyStartDate" to monthlyStartDate.coerceIn(1L, 28L),
                "weeklyStartDay" to weeklyStartDay,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userDoc(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Account Groups ---

    override suspend fun saveAccountGroup(
        userId: String,
        existingId: String?,
        name: String,
        groupType: AccountNature,
        sortOrder: Long
    ): Result<Unit> {
        val cleanName = name.trim().take(60)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        var path = "users/$userId/accountGroups"
        return try {
            val uid = requireUserId(userId)
            if (existingId.isNullOrBlank()) {
                val docRef = groupsCol(uid).document()
                path = docRef.path
                val model = AccountGroup(
                    userId = uid,
                    name = cleanName,
                    groupType = groupType.name,
                    sortOrder = sortOrder.coerceIn(0L, 10000L),
                    archived = false
                )
                docRef.set(model.toCreateMap(uid)).await()
            } else {
                val docRef = groupsCol(uid).document(existingId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "groupType" to groupType.name,
                        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun setAccountGroupArchived(userId: String, groupId: String, archived: Boolean): Result<Unit> {
        val path = "users/$userId/accountGroups/$groupId"
        return try {
            val uid = requireUserId(userId)
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            groupsCol(uid).document(groupId).update(
                mapOf(
                    "archived" to archived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccountGroup(userId: String, groupId: String): Result<Unit> {
        val path = "users/$userId/accountGroups/$groupId"
        return try {
            val uid = requireUserId(userId)
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            groupsCol(uid).document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Accounts ---

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
        var path = "users/$userId/accounts"
        return try {
            val uid = requireUserId(userId)
            if (existingId.isNullOrBlank()) {
                val docRef = accountsCol(uid).document()
                path = docRef.path
                val model = Account(
                    userId = uid,
                    groupId = groupId,
                    name = cleanName,
                    initialBalance = initialBalance,
                    accountType = accountType.name,
                    note = note?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
                    sortOrder = sortOrder.coerceIn(0L, 10000L),
                    archived = false,
                    includeInNetWorth = includeInNetWorth
                )
                docRef.set(model.toCreateMap(uid)).await()
            } else {
                val docRef = accountsCol(uid).document(existingId)
                path = docRef.path
                val updates = mutableMapOf<String, Any?>(
                    "groupId" to groupId,
                    "name" to cleanName,
                    "initialBalance" to initialBalance,
                    "accountType" to accountType.name,
                    "note" to (note?.trim()?.take(300)?.takeIf { it.isNotEmpty() }),
                    "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                    "includeInNetWorth" to includeInNetWorth,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun setAccountArchived(userId: String, accountId: String, archived: Boolean): Result<Unit> {
        val path = "users/$userId/accounts/$accountId"
        return try {
            val uid = requireUserId(userId)
            require(accountId.isNotBlank()) { "Account ID must not be blank" }
            accountsCol(uid).document(accountId).update(
                mapOf(
                    "archived" to archived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(userId: String, accountId: String): Result<Unit> {
        val path = "users/$userId/accounts/$accountId"
        return try {
            val uid = requireUserId(userId)
            require(accountId.isNotBlank()) { "Account ID must not be blank" }
            accountsCol(uid).document(accountId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Categories ---

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
        val cleanSubs = subcategories.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(30)
        var path = "users/$userId/categories"
        return try {
            val uid = requireUserId(userId)
            if (existingId.isNullOrBlank()) {
                val docRef = categoriesCol(uid).document()
                path = docRef.path
                val model = Category(
                    userId = uid,
                    name = cleanName,
                    type = type.name,
                    colorHex = safeHex,
                    subcategories = cleanSubs,
                    sortOrder = sortOrder.coerceIn(0L, 10000L)
                )
                docRef.set(model.toCreateMap(uid)).await()
            } else {
                val docRef = categoriesCol(uid).document(existingId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "type" to type.name,
                        "colorHex" to safeHex,
                        "subcategories" to cleanSubs,
                        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> {
        val path = "users/$userId/categories/$categoryId"
        return try {
            val uid = requireUserId(userId)
            require(categoryId.isNotBlank()) { "Category ID must not be blank" }
            categoriesCol(uid).document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Transactions ---

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
        var path = "users/$userId/transactions"
        return try {
            val uid = requireUserId(userId)
            if (existingId.isNullOrBlank()) {
                val docRef = transactionsCol(uid).document()
                path = docRef.path
                docRef.set(tx.toCreateMap(uid)).await()
            } else {
                val docRef = transactionsCol(uid).document(existingId)
                path = docRef.path
                val updates = mutableMapOf<String, Any?>(
                    "type" to tx.txType().name,
                    "amount" to tx.amount,
                    "feeAmount" to tx.feeAmount.coerceAtLeast(0.0),
                    "epochDay" to tx.epochDay,
                    "timestampMillis" to tx.timestampMillis.coerceAtLeast(0L),
                    "accountId" to tx.accountId,
                    "toAccountId" to tx.toAccountId?.takeIf { it.isNotBlank() },
                    "categoryId" to tx.categoryId?.takeIf { it.isNotBlank() },
                    "subcategory" to tx.subcategory?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
                    "note" to tx.note.trim().take(140),
                    "tagging" to tx.tagging?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
                    "description" to tx.description?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
                    "tags" to tx.tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(10),
                    "favorite" to tx.favorite,
                    "repeatFrequency" to tx.normalizedRepeatFrequency(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        val path = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireUserId(userId)
            require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }
            transactionsCol(uid).document(transactionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Budgets ---

    override suspend fun saveBudget(userId: String, categoryId: String, monthlyLimit: Double): Result<Unit> {
        if (categoryId.isBlank()) return Result.failure(IllegalArgumentException("Category required"))
        val safeLimit = monthlyLimit.coerceAtLeast(0.0)
        val docId = "budget_$categoryId"
        val path = "users/$userId/budgets/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = budgetsCol(uid).document(docId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "categoryId" to categoryId,
                        "monthlyLimit" to safeLimit,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val model = Budget(userId = uid, categoryId = categoryId, monthlyLimit = safeLimit)
                docRef.set(model.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        val path = "users/$userId/budgets/$budgetId"
        return try {
            val uid = requireUserId(userId)
            require(budgetId.isNotBlank()) { "Budget ID must not be blank" }
            budgetsCol(uid).document(budgetId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Financial Notes ---

    override suspend fun saveNote(
        userId: String,
        existingId: String?,
        title: String,
        content: String,
        pinned: Boolean
    ): Result<Unit> {
        val cleanTitle = title.trim().take(100)
        if (cleanTitle.isEmpty()) return Result.failure(IllegalArgumentException("Note title cannot be empty"))
        var path = "users/$userId/notes"
        return try {
            val uid = requireUserId(userId)
            if (existingId.isNullOrBlank()) {
                val docRef = notesCol(uid).document()
                path = docRef.path
                val model = FinanceNote(
                    userId = uid,
                    title = cleanTitle,
                    content = content.trim().take(2000),
                    pinned = pinned
                )
                docRef.set(model.toCreateMap(uid)).await()
            } else {
                val docRef = notesCol(uid).document(existingId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "content" to content.trim().take(2000),
                        "pinned" to pinned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val path = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId(userId)
            require(noteId.isNotBlank()) { "Note ID must not be blank" }
            notesCol(uid).document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
