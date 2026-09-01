package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.AccountType
import com.example.domain.model.AccountWithBalance
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.BudgetWithProgress
import com.example.domain.model.CashFlowSummary
import com.example.domain.model.CategorySpending
import com.example.domain.model.CategoryType
import com.example.domain.model.DailySummary
import com.example.domain.model.FinancialInsight
import com.example.domain.model.InsightType
import com.example.domain.model.MonthlySummary
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepository(private val database: AppDatabase) {

    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val recurringDao = database.recurringDao()
    private val bookmarkDao = database.bookmarkDao()

    // ----------------------------------------------------
    // ACCOUNTS & BALANCE COMPUTATION (LEDGER ENGINE)
    // ----------------------------------------------------

    val allAccountsWithBalanceFlow: Flow<List<AccountWithBalance>> =
        combine(accountDao.getAllAccounts(), transactionDao.getAllTransactions()) { accounts, transactions ->
            computeAccountBalances(accounts, transactions)
        }

    val activeAccountsWithBalanceFlow: Flow<List<AccountWithBalance>> =
        combine(accountDao.getActiveAccounts(), transactionDao.getAllTransactions()) { accounts, transactions ->
            computeAccountBalances(accounts, transactions)
        }

    private fun computeAccountBalances(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): List<AccountWithBalance> {
        return accounts.map { account ->
            var incomeSum = 0.0
            var expenseSum = 0.0
            var transfersIn = 0.0
            var transfersOut = 0.0
            var count = 0

            for (tx in transactions) {
                if (tx.accountId == account.id) {
                    count++
                    when (tx.type) {
                        TransactionType.INCOME, TransactionType.REFUND -> incomeSum += tx.amount
                        TransactionType.EXPENSE -> expenseSum += tx.amount
                        TransactionType.TRANSFER -> transfersOut += tx.amount
                        TransactionType.ADJUSTMENT -> {
                            // Positive adjustment acts as income, negative as expense
                            if (tx.amount >= 0) incomeSum += tx.amount else expenseSum += Math.abs(tx.amount)
                        }
                    }
                }
                // Check if account is receiving a transfer
                if (tx.type == TransactionType.TRANSFER && tx.destinationAccountId == account.id) {
                    count++
                    transfersIn += tx.amount
                }
            }

            val computedBalance = if (account.type == AccountType.CREDIT_CARD) {
                // For credit cards: Opening balance (usually 0 or past balance) - purchases + payments
                account.openingBalance - expenseSum + incomeSum + transfersIn - transfersOut
            } else {
                account.openingBalance + incomeSum - expenseSum + transfersIn - transfersOut
            }

            AccountWithBalance(
                account = account,
                balance = computedBalance,
                totalIncome = incomeSum,
                totalExpense = expenseSum,
                totalTransfersIn = transfersIn,
                totalTransfersOut = transfersOut,
                transactionCount = count
            )
        }
    }

    suspend fun getAccountById(id: String): AccountEntity? = accountDao.getAccountById(id)

    suspend fun saveAccount(account: AccountEntity) {
        accountDao.insertAccount(account.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAccount(account: AccountEntity) {
        database.withTransaction {
            transactionDao.deleteTransactionsForAccount(account.id)
            accountDao.deleteAccount(account)
        }
    }

    // ----------------------------------------------------
    // CATEGORIES
    // ----------------------------------------------------

    val allCategoriesFlow: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoryById(id: String): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun saveCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategoryWithReassignment(category: CategoryEntity, replacementCategoryId: String) {
        database.withTransaction {
            transactionDao.reassignCategory(category.id, replacementCategoryId)
            categoryDao.deleteCategory(category)
        }
    }

    // ----------------------------------------------------
    // TRANSACTIONS & REVENUE CALCULATION
    // ----------------------------------------------------

    val allTransactionDetailsFlow: Flow<List<TransactionDetail>> =
        combine(
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts()
        ) { transactions, categories, accounts ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }

            transactions.map { tx ->
                TransactionDetail(
                    transaction = tx,
                    category = catMap[tx.categoryId],
                    account = accMap[tx.accountId],
                    destinationAccount = tx.destinationAccountId?.let { accMap[it] }
                )
            }
        }

    fun getRecentTransactionDetailsFlow(limit: Int = 10): Flow<List<TransactionDetail>> =
        combine(
            transactionDao.getRecentTransactions(limit),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts()
        ) { transactions, categories, accounts ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }

            transactions.map { tx ->
                TransactionDetail(
                    transaction = tx,
                    category = catMap[tx.categoryId],
                    account = accMap[tx.accountId],
                    destinationAccount = tx.destinationAccountId?.let { accMap[it] }
                )
            }
        }

    suspend fun getTransactionById(id: String): TransactionEntity? = transactionDao.getTransactionById(id)

    suspend fun saveTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            transactionDao.insertTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            transactionDao.deleteTransactionByIdWithLinked(transaction.id)
        }
    }

    suspend fun deleteTransactionById(id: String) {
        database.withTransaction {
            transactionDao.deleteTransactionByIdWithLinked(id)
        }
    }

    // ----------------------------------------------------
    // BUDGETS & PROGRESS
    // ----------------------------------------------------

    val allBudgetsWithProgressFlow: Flow<List<BudgetWithProgress>> =
        combine(
            budgetDao.getAllBudgets(),
            categoryDao.getAllCategories(),
            transactionDao.getAllTransactions()
        ) { budgets, categories, transactions ->
            val catMap = categories.associateBy { it.id }
            val now = Calendar.getInstance()

            budgets.map { budget ->
                val (startDate, endDate) = getPeriodDates(budget.period, budget.startDateEpochMillis, budget.endDateEpochMillis)

                // Calculate spending for this budget's category and date range
                val periodTransactions = transactions.filter { tx ->
                    tx.type == TransactionType.EXPENSE &&
                    tx.dateEpochMillis in startDate..endDate &&
                    (budget.categoryId == null || tx.categoryId == budget.categoryId)
                }

                val spent = periodTransactions.sumOf { it.amount }
                val remaining = budget.amountLimit - spent
                val percentage = if (budget.amountLimit > 0) ((spent / budget.amountLimit) * 100).toFloat() else 0f
                val isOverspent = spent > budget.amountLimit

                // Projected spending based on days elapsed
                val totalPeriodDays = Math.max(1, ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt())
                val daysElapsed = Math.max(1, Math.min(totalPeriodDays, ((System.currentTimeMillis() - startDate) / (1000 * 60 * 60 * 24)).toInt()))
                val dailyBurn = spent / daysElapsed
                val projected = dailyBurn * totalPeriodDays

                BudgetWithProgress(
                    budget = budget,
                    category = budget.categoryId?.let { catMap[it] },
                    spent = spent,
                    remaining = remaining,
                    percentageUsed = percentage,
                    isOverspent = isOverspent,
                    projectedSpending = projected
                )
            }
        }

    suspend fun saveBudget(budget: BudgetEntity) {
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.deleteBudget(budget)
    }

    // ----------------------------------------------------
    // RECURRING TRANSACTIONS & PROCESSOR
    // ----------------------------------------------------

    val allRecurringFlow: Flow<List<RecurringTransactionEntity>> = recurringDao.getAllRecurring()

    suspend fun saveRecurring(item: RecurringTransactionEntity) {
        recurringDao.insertRecurring(item)
    }

    suspend fun deleteRecurring(item: RecurringTransactionEntity) {
        recurringDao.deleteRecurring(item)
    }

    suspend fun processDueRecurringTransactions(): Int {
        val now = System.currentTimeMillis()
        val dueItems = recurringDao.getDueRecurring(now)
        var processedCount = 0

        database.withTransaction {
            for (item in dueItems) {
                if (!item.isActive) continue

                // Check if end date passed
                if (item.endDateEpochMillis != null && item.endDateEpochMillis < now) {
                    recurringDao.updateRecurring(item.copy(isActive = false))
                    continue
                }

                // Create ledger transaction
                val tx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amount = item.amount,
                    currency = item.currency,
                    dateEpochMillis = item.nextDueDateEpochMillis,
                    type = item.type,
                    categoryId = item.categoryId,
                    accountId = item.accountId,
                    destinationAccountId = item.destinationAccountId,
                    merchant = item.merchant,
                    note = item.note.ifBlank { "Recurring: ${item.frequency.displayName}" },
                    recurringId = item.id
                )
                transactionDao.insertTransaction(tx)

                // Compute next due date
                val nextDate = computeNextDueDate(item.nextDueDateEpochMillis, item.frequency)
                val updatedItem = item.copy(
                    lastExecutedDateEpochMillis = item.nextDueDateEpochMillis,
                    nextDueDateEpochMillis = nextDate
                )
                recurringDao.updateRecurring(updatedItem)
                processedCount++
            }
        }
        return processedCount
    }

    private fun computeNextDueDate(currentDue: Long, frequency: RecurrenceFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDue }
        when (frequency) {
            RecurrenceFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.BIWEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceFrequency.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            RecurrenceFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    // ----------------------------------------------------
    // BOOKMARKS (QUICK ADD)
    // ----------------------------------------------------

    val allBookmarksFlow: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun saveBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    // ----------------------------------------------------
    // CASH FLOW & AGGREGATIONS
    // ----------------------------------------------------

    fun getCashFlowSummaryFlow(startEpoch: Long, endEpoch: Long): Flow<CashFlowSummary> =
        combine(allAccountsWithBalanceFlow, transactionDao.getTransactionsByDateRange(startEpoch, endEpoch)) { accounts, transactions ->
            var income = 0.0
            var expenses = 0.0

            for (tx in transactions) {
                when (tx.type) {
                    TransactionType.INCOME, TransactionType.REFUND -> income += tx.amount
                    TransactionType.EXPENSE -> expenses += tx.amount
                    else -> {}
                }
            }

            var totalAssets = 0.0
            var totalLiabilities = 0.0

            for (acc in accounts) {
                if (acc.account.type == AccountType.CREDIT_CARD || acc.account.type == AccountType.LOAN) {
                    if (acc.balance < 0) totalLiabilities += Math.abs(acc.balance)
                } else {
                    if (acc.balance >= 0) totalAssets += acc.balance else totalLiabilities += Math.abs(acc.balance)
                }
            }

            val totalBalance = accounts.sumOf { it.balance }
            val netCashFlow = income - expenses
            val savingsRate = if (income > 0) ((netCashFlow / income) * 100).toFloat().coerceIn(-100f, 100f) else 0f

            CashFlowSummary(
                totalBalance = totalBalance,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                income = income,
                expenses = expenses,
                netCashFlow = netCashFlow,
                savingsRate = savingsRate
            )
        }

    fun getCategorySpendingFlow(type: CategoryType, startEpoch: Long, endEpoch: Long): Flow<List<CategorySpending>> =
        combine(transactionDao.getTransactionsByDateRange(startEpoch, endEpoch), categoryDao.getAllCategories()) { transactions, categories ->
            val targetType = if (type == CategoryType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME
            val filteredTx = transactions.filter { it.type == targetType }
            val totalAmount = filteredTx.sumOf { it.amount }
            val catMap = categories.associateBy { it.id }

            val grouped = filteredTx.groupBy { it.categoryId }
            grouped.mapNotNull { (catId, txList) ->
                val category = catMap[catId] ?: return@mapNotNull null
                val catSum = txList.sumOf { it.amount }
                val percentage = if (totalAmount > 0) ((catSum / totalAmount) * 100).toFloat() else 0f
                CategorySpending(
                    category = category,
                    amount = catSum,
                    percentage = percentage,
                    transactionCount = txList.size
                )
            }.sortedByDescending { it.amount }
        }

    // ----------------------------------------------------
    // FINANCIAL INSIGHTS ENGINE
    // ----------------------------------------------------

    fun getFinancialInsightsFlow(): Flow<List<FinancialInsight>> =
        combine(
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories(),
            budgetDao.getAllBudgets()
        ) { transactions, categories, budgets ->
            val insights = mutableListOf<FinancialInsight>()
            val now = Calendar.getInstance()
            val currentMonth = now.get(Calendar.MONTH)
            val currentYear = now.get(Calendar.YEAR)

            val currentMonthTx = transactions.filter { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.dateEpochMillis }
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }

            val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            val prevMonth = prevCal.get(Calendar.MONTH)
            val prevYear = prevCal.get(Calendar.YEAR)
            val prevMonthTx = transactions.filter { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.dateEpochMillis }
                cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear
            }

            val currentExpense = currentMonthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val prevExpense = prevMonthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val currentIncome = currentMonthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

            if (currentIncome > 0 && currentExpense > 0) {
                val savingsRate = ((currentIncome - currentExpense) / currentIncome * 100).toInt()
                if (savingsRate > 20) {
                    insights.add(
                        FinancialInsight(
                            id = "insight_savings",
                            title = "Strong Savings Rate",
                            message = "You saved $savingsRate% of your income this month. Keep up the solid momentum!",
                            type = InsightType.SUCCESS
                        )
                    )
                }
            }

            if (prevExpense > 0 && currentExpense > 0) {
                val diffPercent = (((currentExpense - prevExpense) / prevExpense) * 100).toInt()
                if (diffPercent < -10) {
                    insights.add(
                        FinancialInsight(
                            id = "insight_spending_down",
                            title = "Spending Down ${Math.abs(diffPercent)}%",
                            message = "You spent less this month compared to last month. Great discipline!",
                            type = InsightType.SUCCESS
                        )
                    )
                } else if (diffPercent > 20) {
                    insights.add(
                        FinancialInsight(
                            id = "insight_spending_up",
                            title = "Higher Spending Detected",
                            message = "Monthly expenses are up by $diffPercent% compared to last month.",
                            type = InsightType.WARNING
                        )
                    )
                }
            }

            // Top category analysis
            val catMap = categories.associateBy { it.id }
            val topCategory = currentMonthTx
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .maxByOrNull { it.value.sumOf { tx -> tx.amount } }

            topCategory?.let { (catId, list) ->
                val cat = catMap[catId]
                if (cat != null) {
                    val catTotal = list.sumOf { it.amount }
                    val percent = if (currentExpense > 0) (catTotal / currentExpense * 100).toInt() else 0
                    insights.add(
                        FinancialInsight(
                            id = "insight_top_cat",
                            title = "Top Category: ${cat.name}",
                            message = "${cat.name} represents $percent% of your spending this month.",
                            type = InsightType.INFO
                        )
                    )
                }
            }

            if (insights.isEmpty()) {
                insights.add(
                    FinancialInsight(
                        id = "insight_welcome",
                        title = "Private & Secure",
                        message = "All financial records are stored 100% locally on your device with double-entry integrity.",
                        type = InsightType.INFO
                    )
                )
            }

            insights
        }

    // ----------------------------------------------------
    // UTILS & PERIOD HELPERS
    // ----------------------------------------------------

    private fun getPeriodDates(period: BudgetPeriod, customStart: Long, customEnd: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (period) {
            BudgetPeriod.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            BudgetPeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            BudgetPeriod.YEARLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            BudgetPeriod.CUSTOM -> Pair(customStart, if (customEnd > 0) customEnd else System.currentTimeMillis())
        }
    }

    // ----------------------------------------------------
    // DEMO DATA SEEDER & RESET
    // ----------------------------------------------------

    suspend fun seedDemoData() {
        database.withTransaction {
            // Add realistic transactions for past 30 days
            val cal = Calendar.getInstance()
            val now = cal.timeInMillis

            val demoTxList = mutableListOf<TransactionEntity>()

            // Salary
            cal.set(Calendar.DAY_OF_MONTH, 1)
            demoTxList.add(
                TransactionEntity(
                    amount = 5200.0,
                    type = TransactionType.INCOME,
                    categoryId = "cat_salary",
                    accountId = "default_acc_checking",
                    merchant = "Acme Corp",
                    note = "Monthly Salary Deposit",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Rent
            cal.set(Calendar.DAY_OF_MONTH, 2)
            demoTxList.add(
                TransactionEntity(
                    amount = 1450.0,
                    type = TransactionType.EXPENSE,
                    categoryId = "cat_rent",
                    accountId = "default_acc_checking",
                    merchant = "Skyline Apartments",
                    note = "Apartment Rent",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Transfer to savings
            cal.set(Calendar.DAY_OF_MONTH, 3)
            demoTxList.add(
                TransactionEntity(
                    amount = 1000.0,
                    type = TransactionType.TRANSFER,
                    categoryId = "cat_other_expense",
                    accountId = "default_acc_checking",
                    destinationAccountId = "default_acc_savings",
                    note = "Monthly Savings Transfer",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Groceries
            cal.set(Calendar.DAY_OF_MONTH, 5)
            demoTxList.add(
                TransactionEntity(
                    amount = 142.50,
                    type = TransactionType.EXPENSE,
                    categoryId = "cat_groceries",
                    accountId = "default_acc_credit",
                    merchant = "Whole Foods",
                    tags = listOf("Groceries", "Organic"),
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Dining
            cal.set(Calendar.DAY_OF_MONTH, 8)
            demoTxList.add(
                TransactionEntity(
                    amount = 68.20,
                    type = TransactionType.EXPENSE,
                    categoryId = "cat_restaurants",
                    accountId = "default_acc_credit",
                    merchant = "Luigi's Trattoria",
                    note = "Dinner with friends",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Fuel
            cal.set(Calendar.DAY_OF_MONTH, 11)
            demoTxList.add(
                TransactionEntity(
                    amount = 55.00,
                    type = TransactionType.EXPENSE,
                    categoryId = "cat_fuel",
                    accountId = "default_acc_credit",
                    merchant = "Shell Gas Station",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Freelance
            cal.set(Calendar.DAY_OF_MONTH, 14)
            demoTxList.add(
                TransactionEntity(
                    amount = 850.0,
                    type = TransactionType.INCOME,
                    categoryId = "cat_freelance",
                    accountId = "default_acc_checking",
                    merchant = "Tech Studio LLC",
                    note = "Mobile App Design Contract",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Subscriptions
            cal.set(Calendar.DAY_OF_MONTH, 16)
            demoTxList.add(
                TransactionEntity(
                    amount = 19.99,
                    type = TransactionType.EXPENSE,
                    categoryId = "cat_subscriptions",
                    accountId = "default_acc_credit",
                    merchant = "Netflix 4K",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Utilities
            cal.set(Calendar.DAY_OF_MONTH, 18)
            demoTxList.add(
                TransactionEntity(
                    amount = 115.30,
                    type = TransactionType.EXPENSE,
                    categoryId = "cat_bills",
                    accountId = "default_acc_checking",
                    merchant = "Electric & Water Utility",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            // Coffee & Snacks
            cal.set(Calendar.DAY_OF_MONTH, 21)
            demoTxList.add(
                TransactionEntity(
                    amount = 14.50,
                    type = TransactionType.EXPENSE,
                    categoryId = "cat_restaurants",
                    accountId = "default_acc_cash",
                    merchant = "Blue Bottle Coffee",
                    dateEpochMillis = cal.timeInMillis
                )
            )

            transactionDao.insertTransactions(demoTxList)

            // Default budgets
            budgetDao.insertBudget(
                BudgetEntity(
                    id = "budget_food",
                    name = "Food & Dining",
                    categoryId = "cat_food",
                    amountLimit = 600.0,
                    period = BudgetPeriod.MONTHLY
                )
            )
            budgetDao.insertBudget(
                BudgetEntity(
                    id = "budget_shopping",
                    name = "Shopping & Personal",
                    categoryId = "cat_shopping",
                    amountLimit = 350.0,
                    period = BudgetPeriod.MONTHLY
                )
            )

            // Default bookmarks
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    id = "bm_coffee",
                    title = "Morning Coffee",
                    amount = 5.50,
                    categoryId = "cat_restaurants",
                    accountId = "default_acc_cash",
                    merchant = "Coffee Shop"
                )
            )
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    id = "bm_lunch",
                    title = "Workday Lunch",
                    amount = 15.00,
                    categoryId = "cat_restaurants",
                    accountId = "default_acc_credit",
                    merchant = "Bistro Deli"
                )
            )
        }
    }

    val allTransactionsFlow: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun deleteAccountById(id: String) {
        val account = accountDao.getAccountById(id)
        if (account != null) {
            deleteAccount(account)
        }
    }

    suspend fun deleteBudgetById(id: String) {
        val budget = budgetDao.getBudgetById(id)
        if (budget != null) {
            budgetDao.deleteBudget(budget)
        }
    }

    suspend fun deleteRecurringById(id: String) {
        val recurring = recurringDao.getRecurringById(id)
        if (recurring != null) {
            recurringDao.deleteRecurring(recurring)
        }
    }

    suspend fun clearAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun clearAllData() {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            budgetDao.deleteAllBudgets()
            recurringDao.deleteAllRecurring()
            bookmarkDao.deleteAllBookmarks()
            accountDao.deleteAllAccounts()
        }
    }
}
