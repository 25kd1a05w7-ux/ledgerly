package com.example.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.BudgetWithProgress
import com.example.domain.model.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class BudgetsUiState(
    val budgets: List<BudgetWithProgress> = emptyList(),
    val recurringList: List<RecurringTransactionEntity> = emptyList(),
    val totalBudgeted: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val overallPercentage: Float = 0f,
    val availableCategories: List<CategoryEntity> = emptyList(),
    val currency: String = "INR",
    val isAddBudgetOpen: Boolean = false,
    val editingBudget: BudgetEntity? = null,
    val activeTab: Int = 0 // 0 = Budgets, 1 = Subscriptions & Recurring
)

class BudgetsViewModel(
    private val repository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isAddBudgetOpen = MutableStateFlow(false)
    private val _editingBudget = MutableStateFlow<BudgetEntity?>(null)
    private val _activeTab = MutableStateFlow(0)

    val uiState: StateFlow<BudgetsUiState> = combine(
        repository.allBudgetsWithProgressFlow,
        repository.allRecurringFlow,
        repository.allCategoriesFlow,
        preferencesRepository.userPreferencesFlow,
        _isAddBudgetOpen,
        _editingBudget,
        _activeTab
    ) { args ->
        val budgets = args[0] as List<BudgetWithProgress>
        val recurring = args[1] as List<RecurringTransactionEntity>
        val categories = args[2] as List<CategoryEntity>
        val prefs = args[3] as com.example.data.preferences.UserPreferences
        val isAddOpen = args[4] as Boolean
        val editingBgt = args[5] as BudgetEntity?
        val tab = args[6] as Int

        val totalBudgeted = budgets.sumOf { it.budget.amountLimit }
        val totalSpent = budgets.sumOf { it.spent }
        val totalRemaining = totalBudgeted - totalSpent
        val overallPct = if (totalBudgeted > 0) (totalSpent / totalBudgeted).toFloat() * 100f else 0f

        val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }

        BudgetsUiState(
            budgets = budgets,
            recurringList = recurring,
            totalBudgeted = totalBudgeted,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            overallPercentage = overallPct,
            availableCategories = expenseCategories,
            currency = prefs.currency,
            isAddBudgetOpen = isAddOpen,
            editingBudget = editingBgt,
            activeTab = tab
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState()
    )

    fun setTab(tab: Int) {
        _activeTab.value = tab
    }

    fun openAddBudget() {
        _editingBudget.value = null
        _isAddBudgetOpen.value = true
    }

    fun openEditBudget(budget: BudgetEntity) {
        _editingBudget.value = budget
        _isAddBudgetOpen.value = true
    }

    fun closeBudgetDialog() {
        _editingBudget.value = null
        _isAddBudgetOpen.value = false
    }

    fun saveBudget(
        name: String,
        categoryId: String?,
        amountLimit: Double,
        period: BudgetPeriod,
        rollover: Boolean,
        notifyAt80: Boolean,
        notifyAt100: Boolean
    ) {
        viewModelScope.launch {
            val existing = _editingBudget.value
            val budget = BudgetEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                categoryId = categoryId,
                amountLimit = amountLimit,
                period = period,
                rollover = rollover,
                alert75 = notifyAt80,
                alert100 = notifyAt100,
                startDateEpochMillis = existing?.startDateEpochMillis ?: System.currentTimeMillis()
            )
            repository.saveBudget(budget)
            closeBudgetDialog()
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudgetById(budgetId)
            closeBudgetDialog()
        }
    }

    fun toggleRecurringActive(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.saveRecurring(recurring.copy(isActive = !recurring.isActive))
        }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringById(id)
        }
    }
}
