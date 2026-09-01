package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AccountType
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.CategoryType
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import java.util.UUID

@Entity(
    tableName = "accounts",
    indices = [Index("name"), Index("type")]
)
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AccountType,
    val currency: String = "USD",
    val openingBalance: Double = 0.0,
    val colorHex: String = "#10B981",
    val iconName: String = "account_balance",
    val creditLimit: Double = 0.0,
    val billingDay: Int = 1,
    val dueDay: Int = 20,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index("name"), Index("type"), Index("parentId")]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: CategoryType,
    val iconName: String = "category",
    val colorHex: String = "#3B82F6",
    val parentId: String? = null,
    val displayOrder: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("dateEpochMillis"),
        Index("accountId"),
        Index("destinationAccountId"),
        Index("categoryId"),
        Index("type"),
        Index("merchant")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String = "USD",
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val type: TransactionType,
    val categoryId: String,
    val accountId: String,
    val destinationAccountId: String? = null,
    val transferLinkedId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val tags: List<String> = emptyList(),
    val attachmentPath: String? = null,
    val recurringId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("period")]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val categoryId: String? = null, // null means overall budget
    val amountLimit: Double,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDateEpochMillis: Long = 0L,
    val endDateEpochMillis: Long = 0L,
    val rollover: Boolean = false,
    val alert50: Boolean = true,
    val alert75: Boolean = true,
    val alert90: Boolean = true,
    val alert100: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recurring_transactions",
    indices = [Index("accountId"), Index("categoryId"), Index("nextDueDateEpochMillis")]
)
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String = "USD",
    val type: TransactionType,
    val categoryId: String,
    val accountId: String,
    val destinationAccountId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long? = null,
    val lastExecutedDateEpochMillis: Long? = null,
    val nextDueDateEpochMillis: Long,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: String,
    val accountId: String,
    val merchant: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
