package com.example.domain.model

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity

data class AccountWithBalance(
    val account: AccountEntity,
    val balance: Double,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalTransfersIn: Double = 0.0,
    val totalTransfersOut: Double = 0.0,
    val transactionCount: Int = 0
) {
    val currentBalance: Double get() = balance
}

data class TransactionDetail(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val account: AccountEntity?,
    val destinationAccount: AccountEntity? = null
)

data class BudgetWithProgress(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val spent: Double,
    val remaining: Double,
    val percentageUsed: Float,
    val isOverspent: Boolean,
    val projectedSpending: Double,
    val dailySafeSpend: Double = 0.0
)

data class CategorySpending(
    val category: CategoryEntity,
    val amount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class DailySummary(
    val dateEpochMillis: Long,
    val dayOfMonth: Int,
    val dayOfWeek: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class MonthlySummary(
    val monthYear: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class CashFlowSummary(
    val totalBalance: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val income: Double,
    val expenses: Double,
    val netCashFlow: Double,
    val savingsRate: Float
)

data class FinancialInsight(
    val id: String,
    val title: String,
    val message: String,
    val type: InsightType,
    val actionText: String? = null
)

enum class InsightType {
    INFO,
    WARNING,
    SUCCESS,
    BUDGET
}
