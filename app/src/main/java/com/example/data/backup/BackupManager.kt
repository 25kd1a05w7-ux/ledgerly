package com.example.data.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LedgerlyBackupData(
    val version: Int = 1,
    val app: String = "Ledgerly",
    val exportedAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val recurring: List<RecurringTransactionEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList()
)

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val backupAdapter = moshi.adapter(LedgerlyBackupData::class.java)

    suspend fun generateJsonBackup(): String = withContext(Dispatchers.IO) {
        val accounts = database.accountDao().getAllAccounts().first()
        val categories = database.categoryDao().getAllCategories().first()
        val transactions = database.transactionDao().getAllTransactions().first()
        val budgets = database.budgetDao().getAllBudgets().first()
        val recurring = database.recurringDao().getAllRecurring().first()
        val bookmarks = database.bookmarkDao().getAllBookmarks().first()

        val data = LedgerlyBackupData(
            version = 1,
            app = "Ledgerly",
            exportedAt = System.currentTimeMillis(),
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            recurring = recurring,
            bookmarks = bookmarks
        )
        backupAdapter.indent("  ").toJson(data)
    }

    suspend fun restoreFromJson(
        jsonString: String,
        replaceExisting: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val backup = backupAdapter.fromJson(jsonString)
                ?: throw IllegalArgumentException("Invalid backup JSON file format.")

            database.withTransaction {
                if (replaceExisting) {
                    database.transactionDao().deleteAllTransactions()
                    // Re-insert or replace all
                    database.accountDao().insertAccounts(backup.accounts)
                    database.categoryDao().insertCategories(backup.categories)
                    database.budgetDao().insertBudgets(backup.budgets)
                    database.recurringDao().insertRecurringList(backup.recurring)
                    database.bookmarkDao().insertBookmarks(backup.bookmarks)
                    database.transactionDao().insertTransactions(backup.transactions)
                } else {
                    // Merge mode
                    database.accountDao().insertAccounts(backup.accounts)
                    database.categoryDao().insertCategories(backup.categories)
                    database.budgetDao().insertBudgets(backup.budgets)
                    database.recurringDao().insertRecurringList(backup.recurring)
                    database.bookmarkDao().insertBookmarks(backup.bookmarks)
                    database.transactionDao().insertTransactions(backup.transactions)
                }
            }
            backup.transactions.size
        }
    }

    suspend fun generateCsvExport(): String = withContext(Dispatchers.IO) {
        val transactions = database.transactionDao().getAllTransactions().first()
        val categories = database.categoryDao().getAllCategories().first().associateBy { it.id }
        val accounts = database.accountDao().getAllAccounts().first().associateBy { it.id }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append("ID,Date,Type,Amount,Currency,Category,Account,DestinationAccount,Merchant,Note,Tags\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.dateEpochMillis))
            val categoryName = categories[tx.categoryId]?.name ?: "Uncategorized"
            val accountName = accounts[tx.accountId]?.name ?: "Unknown"
            val destName = tx.destinationAccountId?.let { accounts[it]?.name } ?: ""
            val tagsStr = tx.tags.joinToString(";")

            sb.append("\"${escapeCsv(tx.id)}\",")
            sb.append("\"$dateStr\",")
            sb.append("\"${tx.type.name}\",")
            sb.append("${tx.amount},")
            sb.append("\"${tx.currency}\",")
            sb.append("\"${escapeCsv(categoryName)}\",")
            sb.append("\"${escapeCsv(accountName)}\",")
            sb.append("\"${escapeCsv(destName)}\",")
            sb.append("\"${escapeCsv(tx.merchant)}\",")
            sb.append("\"${escapeCsv(tx.note)}\",")
            sb.append("\"${escapeCsv(tagsStr)}\"\n")
        }
        sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    suspend fun shareExportFile(content: String, fileName: String, mimeType: String): Intent = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { it.write(content.toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ledgerly Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
