package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDateEpochMillis ASC")
    fun getAllRecurring(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDateEpochMillis <= :timestamp")
    suspend fun getDueRecurring(timestamp: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getRecurringById(id: String): RecurringTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(item: RecurringTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringList(items: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurring(item: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurring(item: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringById(id: String)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAllRecurring()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY title ASC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun getBookmarkById(id: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
}
