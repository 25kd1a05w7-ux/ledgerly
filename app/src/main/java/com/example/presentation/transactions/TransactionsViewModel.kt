package com.example.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TransactionSortOrder(val displayName: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_AMOUNT("Highest Amount"),
    LOWEST_AMOUNT("Lowest Amount")
}

enum class DateFilterOption(val displayName: String) {
    ALL("All Time"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year")
}

data class TransactionsUiState(
    val searchQuery: String = "",
    val selectedTypeFilter: TransactionType? = null,
    val selectedCategoryId: String? = null,
    val selectedAccountId: String? = null,
    val selectedTag: String? = null,
    val dateFilter: DateFilterOption = DateFilterOption.THIS_MONTH,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.NEWEST,
    val transactions: List<TransactionDetail> = emptyList(),
    val filteredTransactions: List<TransactionDetail> = emptyList(),
    val groupedTransactions: Map<String, List<TransactionDetail>> = emptyMap(),
    val totalFilteredIncome: Double = 0.0,
    val totalFilteredExpense: Double = 0.0,
    val availableAccounts: List<AccountEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val allTags: List<String> = emptyList(),
    val selectedTransactionIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val currency: String = "INR",
    val lastDeletedTransaction: TransactionEntity? = null
)

class TransactionsViewModel(
    private val repository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _dateFilter = MutableStateFlow(DateFilterOption.THIS_MONTH)
    private val _sortOrder = MutableStateFlow(TransactionSortOrder.NEWEST)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _lastDeleted = MutableStateFlow<TransactionEntity?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.allTransactionDetailsFlow,
        repository.activeAccountsWithBalanceFlow,
        repository.allCategoriesFlow,
        preferencesRepository.userPreferencesFlow,
        _searchQuery,
        _selectedType,
        _selectedCategoryId,
        _selectedAccountId,
        _selectedTag
    ) { args ->
        val allTx = args[0] as List<TransactionDetail>
        val accounts = (args[1] as List<com.example.domain.model.AccountWithBalance>).map { it.account }
        val categories = args[2] as List<CategoryEntity>
        val prefs = args[3] as com.example.data.preferences.UserPreferences
        val query = args[4] as String
        val type = args[5] as TransactionType?
        val catId = args[6] as String?
        val accId = args[7] as String?
        val tag = args[8] as String?

        val tagsSet = allTx.flatMap { it.transaction.tags }.distinct()

        TransactionsUiState(
            searchQuery = query,
            selectedTypeFilter = type,
            selectedCategoryId = catId,
            selectedAccountId = accId,
            selectedTag = tag,
            transactions = allTx,
            availableAccounts = accounts,
            availableCategories = categories,
            allTags = tagsSet,
            currency = prefs.currency,
            lastDeletedTransaction = _lastDeleted.value
        )
    }.combine(_dateFilter) { state, dateOpt ->
        state.copy(dateFilter = dateOpt)
    }.combine(_sortOrder) { state, sort ->
        state.copy(sortOrder = sort)
    }.combine(_selectedIds) { state, ids ->
        val filtered = filterAndSort(
            transactions = state.transactions,
            query = state.searchQuery,
            type = state.selectedTypeFilter,
            categoryId = state.selectedCategoryId,
            accountId = state.selectedAccountId,
            tag = state.selectedTag,
            dateOption = state.dateFilter,
            sort = state.sortOrder
        )

        val income = filtered.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
        val expense = filtered.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        val grouped = groupTransactionsByDate(filtered)

        state.copy(
            filteredTransactions = filtered,
            groupedTransactions = grouped,
            totalFilteredIncome = income,
            totalFilteredExpense = expense,
            selectedTransactionIds = ids,
            isSelectionMode = ids.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedType.value = type
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun setAccountFilter(accountId: String?) {
        _selectedAccountId.value = accountId
    }

    fun setTagFilter(tag: String?) {
        _selectedTag.value = tag
    }

    fun setDateFilter(option: DateFilterOption) {
        _dateFilter.value = option
    }

    fun setSortOrder(order: TransactionSortOrder) {
        _sortOrder.value = order
    }

    fun toggleTransactionSelection(id: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _lastDeleted.value = transaction
            repository.deleteTransaction(transaction)
        }
    }

    fun undoDelete() {
        val deleted = _lastDeleted.value ?: return
        viewModelScope.launch {
            repository.saveTransaction(deleted)
            _lastDeleted.value = null
        }
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        viewModelScope.launch {
            ids.forEach { id ->
                repository.deleteTransactionById(id)
            }
            _selectedIds.value = emptySet()
        }
    }

    private fun filterAndSort(
        transactions: List<TransactionDetail>,
        query: String,
        type: TransactionType?,
        categoryId: String?,
        accountId: String?,
        tag: String?,
        dateOption: DateFilterOption,
        sort: TransactionSortOrder
    ): List<TransactionDetail> {
        val (startEpoch, endEpoch) = getDateBoundsForOption(dateOption)

        var result = transactions.filter { item ->
            val tx = item.transaction

            val matchesQuery = query.isBlank() ||
                tx.merchant.contains(query, ignoreCase = true) ||
                tx.note.contains(query, ignoreCase = true) ||
                (item.category?.name?.contains(query, ignoreCase = true) == true) ||
                (item.account?.name?.contains(query, ignoreCase = true) == true) ||
                tx.tags.any { it.contains(query, ignoreCase = true) }

            val matchesType = type == null || tx.type == type
            val matchesCategory = categoryId == null || tx.categoryId == categoryId
            val matchesAccount = accountId == null || tx.accountId == accountId || tx.destinationAccountId == accountId
            val matchesTag = tag == null || tx.tags.contains(tag)
            val matchesDate = tx.dateEpochMillis in startEpoch..endEpoch

            matchesQuery && matchesType && matchesCategory && matchesAccount && matchesTag && matchesDate
        }

        result = when (sort) {
            TransactionSortOrder.NEWEST -> result.sortedByDescending { it.transaction.dateEpochMillis }
            TransactionSortOrder.OLDEST -> result.sortedBy { it.transaction.dateEpochMillis }
            TransactionSortOrder.HIGHEST_AMOUNT -> result.sortedByDescending { it.transaction.amount }
            TransactionSortOrder.LOWEST_AMOUNT -> result.sortedBy { it.transaction.amount }
        }

        return result
    }

    private fun getDateBoundsForOption(option: DateFilterOption): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (option) {
            DateFilterOption.ALL -> Pair(0L, Long.MAX_VALUE)
            DateFilterOption.THIS_WEEK -> {
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
                Pair(start, cal.timeInMillis)
            }
            DateFilterOption.THIS_MONTH -> {
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
                Pair(start, cal.timeInMillis)
            }
            DateFilterOption.THIS_YEAR -> {
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
                Pair(start, cal.timeInMillis)
            }
        }
    }

    private fun groupTransactionsByDate(transactions: List<TransactionDetail>): Map<String, List<TransactionDetail>> {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val now = Date()
        val todayStr = todayFormat.format(now)

        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = todayFormat.format(calYesterday.time)

        return transactions.groupBy { item ->
            val txDate = Date(item.transaction.dateEpochMillis)
            val txDateStr = todayFormat.format(txDate)
            when (txDateStr) {
                todayStr -> "Today • ${SimpleDateFormat("MMM d", Locale.getDefault()).format(txDate)}"
                yesterdayStr -> "Yesterday • ${SimpleDateFormat("MMM d", Locale.getDefault()).format(txDate)}"
                else -> dateFormat.format(txDate)
            }
        }
    }
}
