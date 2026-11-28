package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_user_settings")
data class LocalUserSettingsEntity(
    @PrimaryKey val userId: String,
    val mainCurrencyCode: String = "INR",
    val mainCurrencySymbol: String = "₹",
    val subcategoryEnabled: Boolean = true,
    val startScreen: String = "DAILY",
    val monthlyStartDate: Long = 1L,
    val weeklyStartDay: String = "MONDAY",
    val seeded: Boolean = false
)

@Entity(tableName = "local_main_groups")
data class LocalMainGroupEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val nature: String,
    val sortOrder: Long,
    val archived: Boolean
)

@Entity(tableName = "local_account_groups")
data class LocalAccountGroupEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mainGroupId: String = "",
    val name: String,
    val groupType: String,
    val sortOrder: Long,
    val archived: Boolean
)

@Entity(tableName = "local_accounts")
data class LocalAccountEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val groupId: String,
    val name: String,
    val initialBalance: Double,
    val accountType: String,
    val note: String?,
    val sortOrder: Long,
    val archived: Boolean,
    val includeInNetWorth: Boolean
)

@Entity(tableName = "local_categories")
data class LocalCategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val type: String,
    val colorHex: String,
    val subcategoriesSerialized: String,
    val sortOrder: Long
)

@Entity(tableName = "local_transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val amount: Double,
    val feeAmount: Double,
    val epochDay: Long,
    val timestampMillis: Long,
    val accountId: String,
    val toAccountId: String?,
    val categoryId: String?,
    val subcategory: String?,
    val note: String,
    val note2: String = "",
    val tagging: String?,
    val toTagging: String? = null,
    val description: String?,
    val favorite: Boolean,
    val repeatFrequency: String
)

@Entity(tableName = "local_budgets")
data class LocalBudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val categoryId: String,
    val subcategory: String? = null,
    val monthlyLimit: Double
)

@Entity(tableName = "local_finance_notes")
data class LocalFinanceNoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val updatedAtMillis: Long
)

@Dao
interface LocalLedgerDao {
    @Query("SELECT * FROM local_user_settings WHERE userId = :userId LIMIT 1")
    fun observeSettings(userId: String): Flow<LocalUserSettingsEntity?>

    @Query("SELECT * FROM local_user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettings(userId: String): LocalUserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(entity: LocalUserSettingsEntity)

    @Query("SELECT * FROM local_main_groups WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeMainGroups(userId: String): Flow<List<LocalMainGroupEntity>>

    @Query("SELECT * FROM local_main_groups WHERE userId = :userId ORDER BY sortOrder ASC")
    suspend fun getMainGroups(userId: String): List<LocalMainGroupEntity>

    @Query("SELECT * FROM local_main_groups WHERE id = :id LIMIT 1")
    suspend fun getMainGroup(id: String): LocalMainGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMainGroup(entity: LocalMainGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMainGroups(entities: List<LocalMainGroupEntity>)

    @Query("UPDATE local_main_groups SET archived = :archived WHERE id = :id AND userId = :userId")
    suspend fun setMainGroupArchived(userId: String, id: String, archived: Boolean)

    @Query("DELETE FROM local_main_groups WHERE id = :id AND userId = :userId")
    suspend fun deleteMainGroup(userId: String, id: String)

    @Query("SELECT * FROM local_account_groups WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeAccountGroups(userId: String): Flow<List<LocalAccountGroupEntity>>

    @Query("SELECT * FROM local_account_groups WHERE id = :id LIMIT 1")
    suspend fun getAccountGroup(id: String): LocalAccountGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccountGroup(entity: LocalAccountGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccountGroups(entities: List<LocalAccountGroupEntity>)

    @Query("UPDATE local_account_groups SET archived = :archived WHERE id = :id AND userId = :userId")
    suspend fun setAccountGroupArchived(userId: String, id: String, archived: Boolean)

    @Query("DELETE FROM local_account_groups WHERE id = :id AND userId = :userId")
    suspend fun deleteAccountGroup(userId: String, id: String)

    @Query("SELECT * FROM local_accounts WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeAccounts(userId: String): Flow<List<LocalAccountEntity>>

    @Query("SELECT * FROM local_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccount(id: String): LocalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(entity: LocalAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(entities: List<LocalAccountEntity>)

    @Query("UPDATE local_accounts SET archived = :archived WHERE id = :id AND userId = :userId")
    suspend fun setAccountArchived(userId: String, id: String, archived: Boolean)

    @Query("DELETE FROM local_accounts WHERE id = :id AND userId = :userId")
    suspend fun deleteAccount(userId: String, id: String)

    @Query("SELECT * FROM local_categories WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeCategories(userId: String): Flow<List<LocalCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(entity: LocalCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(entities: List<LocalCategoryEntity>)

    @Query("DELETE FROM local_categories WHERE id = :id AND userId = :userId")
    suspend fun deleteCategory(userId: String, id: String)

    @Query("SELECT * FROM local_transactions WHERE userId = :userId ORDER BY epochDay DESC, timestampMillis DESC")
    fun observeTransactions(userId: String): Flow<List<LocalTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(entity: LocalTransactionEntity)

    @Query("DELETE FROM local_transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteTransaction(userId: String, id: String)

    @Query(
        "UPDATE local_transactions SET tagging = :newTag " +
            "WHERE userId = :userId AND tagging IS NOT NULL AND LOWER(TRIM(tagging)) = LOWER(TRIM(:oldTag))"
    )
    suspend fun renameTransactionTagging(userId: String, oldTag: String, newTag: String): Int

    @Query(
        "UPDATE local_transactions SET toTagging = :newTag " +
            "WHERE userId = :userId AND toTagging IS NOT NULL AND LOWER(TRIM(toTagging)) = LOWER(TRIM(:oldTag))"
    )
    suspend fun renameTransactionToTagging(userId: String, oldTag: String, newTag: String): Int

    @Query("SELECT * FROM local_budgets WHERE userId = :userId")
    fun observeBudgets(userId: String): Flow<List<LocalBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(entity: LocalBudgetEntity)

    @Query("DELETE FROM local_budgets WHERE id = :id AND userId = :userId")
    suspend fun deleteBudget(userId: String, id: String)

    @Query("SELECT * FROM local_finance_notes WHERE userId = :userId ORDER BY pinned DESC, updatedAtMillis DESC")
    fun observeNotes(userId: String): Flow<List<LocalFinanceNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(entity: LocalFinanceNoteEntity)

    @Query("DELETE FROM local_finance_notes WHERE id = :id AND userId = :userId")
    suspend fun deleteNote(userId: String, id: String)
}

@Database(
    entities = [
        LocalUserSettingsEntity::class,
        LocalMainGroupEntity::class,
        LocalAccountGroupEntity::class,
        LocalAccountEntity::class,
        LocalCategoryEntity::class,
        LocalTransactionEntity::class,
        LocalBudgetEntity::class,
        LocalFinanceNoteEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class LocalLedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LocalLedgerDao

    companion object {
        @Volatile
        private var INSTANCE: LocalLedgerDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_main_groups` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `nature` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `archived` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE `local_account_groups` ADD COLUMN `mainGroupId` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `local_transactions` ADD COLUMN `secondaryTagging` TEXT"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `local_transactions` ADD COLUMN `toTagging` TEXT")
                db.execSQL("ALTER TABLE `local_transactions` ADD COLUMN `toSecondaryTagging` TEXT")
                db.execSQL(
                    "UPDATE `local_transactions` SET `toTagging` = `tagging`, `toSecondaryTagging` = `secondaryTagging` WHERE `type` = 'TRANSFER'"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_transactions_new` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `feeAmount` REAL NOT NULL,
                        `epochDay` INTEGER NOT NULL,
                        `timestampMillis` INTEGER NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `toAccountId` TEXT,
                        `categoryId` TEXT,
                        `subcategory` TEXT,
                        `note` TEXT NOT NULL,
                        `note2` TEXT NOT NULL,
                        `tagging` TEXT,
                        `toTagging` TEXT,
                        `description` TEXT,
                        `favorite` INTEGER NOT NULL,
                        `repeatFrequency` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `local_transactions_new` (
                        `id`, `userId`, `type`, `amount`, `feeAmount`, `epochDay`, `timestampMillis`,
                        `accountId`, `toAccountId`, `categoryId`, `subcategory`, `note`, `note2`,
                        `tagging`, `toTagging`, `description`, `favorite`, `repeatFrequency`
                    )
                    SELECT
                        `id`, `userId`, `type`, `amount`, `feeAmount`, `epochDay`, `timestampMillis`,
                        `accountId`, `toAccountId`, `categoryId`, `subcategory`, `note`, '',
                        `tagging`, `toTagging`, `description`, `favorite`, `repeatFrequency`
                    FROM `local_transactions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `local_transactions`")
                db.execSQL("ALTER TABLE `local_transactions_new` RENAME TO `local_transactions`")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `local_budgets` ADD COLUMN `subcategory` TEXT")
            }
        }

        fun getInstance(context: Context): LocalLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalLedgerDatabase::class.java,
                    "money_ledger_local.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
