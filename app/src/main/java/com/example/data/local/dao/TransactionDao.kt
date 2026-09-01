package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochMillis BETWEEN :startEpoch AND :endEpoch ORDER BY dateEpochMillis DESC, createdAt DESC")
    fun getTransactionsByDateRange(startEpoch: Long, endEpoch: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR destinationAccountId = :accountId ORDER BY dateEpochMillis DESC, createdAt DESC")
    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY dateEpochMillis DESC, createdAt DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC, createdAt DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transferLinkedId = :linkedId LIMIT 1")
    suspend fun getLinkedTransferTransaction(linkedId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id OR transferLinkedId = :id")
    suspend fun deleteTransactionByIdWithLinked(id: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR destinationAccountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: String)

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: String, newCategoryId: String)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
