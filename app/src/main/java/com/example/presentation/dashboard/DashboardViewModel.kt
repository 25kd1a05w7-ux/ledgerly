package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BookmarkEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.AccountWithBalance
import com.example.domain.model.BudgetWithProgress
import com.example.domain.model.CashFlowSummary
import com.example.domain.model.CategorySpending
import com.example.domain.model.CategoryType
import com.example.domain.model.FinancialInsight
import com.example.domain.model.TransactionDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DashboardUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-indexed
    val periodTab: Int = 1, // 0 = Week, 1 = Month, 2 = Year
    val cashFlow: CashFlowSummary = CashFlowSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0f),
    val recentTransactions: List<TransactionDetail> = emptyList(),
    val topBudgets: List<BudgetWithProgress> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val insights: List<FinancialInsight> = emptyList(),
    val topExpenseCategories: List<CategorySpending> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

class DashboardViewModel(
    private val repository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    private val _periodTab = MutableStateFlow(1) // 0: Week, 1: Month, 2: Year

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedCalendar,
        _periodTab,
        preferencesRepository.userPreferencesFlow,
        repository.activeAccountsWithBalanceFlow,
        repository.getRecentTransactionDetailsFlow(8),
        repository.allBudgetsWithProgressFlow,
        repository.allBookmarksFlow,
        repository.getFinancialInsightsFlow()
    ) { args ->
        val cal = args[0] as Calendar
        val periodTab = args[1] as Int
        val prefs = args[2] as UserPreferences
        val accounts = args[3] as List<AccountWithBalance>
        val recentTx = args[4] as List<TransactionDetail>
        val budgets = args[5] as List<BudgetWithProgress>
        val bookmarks = args[6] as List<BookmarkEntity>
        val insights = args[7] as List<FinancialInsight>

        val (startEpoch, endEpoch) = getRangeForPeriod(cal, periodTab, prefs.startDayOfMonth)

        DashboardUiState(
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH),
            periodTab = periodTab,
            recentTransactions = recentTx,
            topBudgets = budgets.take(4),
            accounts = accounts,
            bookmarks = bookmarks,
            insights = insights,
            preferences = prefs
        )
    }.flatMapLatest { state ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, state.selectedYear)
            set(Calendar.MONTH, state.selectedMonth)
        }
        val (startEpoch, endEpoch) = getRangeForPeriod(cal, state.periodTab, state.preferences.startDayOfMonth)

        combine(
            repository.getCashFlowSummaryFlow(startEpoch, endEpoch),
            repository.getCategorySpendingFlow(CategoryType.EXPENSE, startEpoch, endEpoch)
        ) { cashFlow, topCategories ->
            state.copy(
                cashFlow = cashFlow,
                topExpenseCategories = topCategories.take(5)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        // Run recurring checks on launch
        viewModelScope.launch {
            repository.processDueRecurringTransactions()
        }
    }

    fun setPeriodTab(tab: Int) {
        _periodTab.value = tab
    }

    fun previousMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _selectedCalendar.value = cal
    }

    fun nextMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        _selectedCalendar.value = cal
    }

    fun currentMonth() {
        _selectedCalendar.value = Calendar.getInstance()
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
        }
    }

    private fun getRangeForPeriod(cal: Calendar, tab: Int, startDay: Int): Pair<Long, Long> {
        val startCal = cal.clone() as Calendar
        val endCal = cal.clone() as Calendar

        when (tab) {
            0 -> { // Week
                startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_WEEK, 6)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
            }
            1 -> { // Month (with financial start day support)
                startCal.set(Calendar.DAY_OF_MONTH, Math.min(startDay, startCal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.MONTH, 1)
                endCal.add(Calendar.SECOND, -1)
            }
            2 -> { // Year
                startCal.set(Calendar.DAY_OF_YEAR, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.set(Calendar.DAY_OF_YEAR, endCal.getActualMaximum(Calendar.DAY_OF_YEAR))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
            }
        }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}
