package com.example.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.CashFlowSummary
import com.example.domain.model.CategorySpending
import com.example.domain.model.CategoryType
import com.example.domain.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class MonthlyTrendItem(
    val monthLabel: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class StatisticsUiState(
    val selectedPeriodTab: Int = 1, // 0 = Week, 1 = Month, 2 = Quarter, 3 = Year
    val selectedTypeTab: Int = 0, // 0 = Expense, 1 = Income
    val cashFlow: CashFlowSummary = CashFlowSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0f),
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val monthlyTrends: List<MonthlyTrendItem> = emptyList(),
    val averageDailySpend: Double = 0.0,
    val currency: String = "INR"
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val repository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedPeriodTab = MutableStateFlow(1)
    private val _selectedTypeTab = MutableStateFlow(0)

    val uiState: StateFlow<StatisticsUiState> = combine(
        _selectedPeriodTab,
        _selectedTypeTab,
        preferencesRepository.userPreferencesFlow
    ) { periodTab: Int, typeTab: Int, prefs: UserPreferences ->
        Triple(periodTab, typeTab, prefs)
    }.flatMapLatest { triple: Triple<Int, Int, UserPreferences> ->
        val (periodTab, typeTab, prefs) = triple
        val (startEpoch, endEpoch, daysCount) = getTimeRangeForTab(periodTab)
        val catType = if (typeTab == 0) CategoryType.EXPENSE else CategoryType.INCOME

        combine(
            repository.getCashFlowSummaryFlow(startEpoch, endEpoch),
            repository.getCategorySpendingFlow(catType, startEpoch, endEpoch),
            repository.allTransactionsFlow
        ) { cashFlow: CashFlowSummary, categories: List<CategorySpending>, allTx: List<TransactionEntity> ->
            val avgDaily = if (daysCount > 0) cashFlow.expenses / daysCount else 0.0

            // Generate monthly trends for past 6 months
            val trends = mutableListOf<MonthlyTrendItem>()
            val cal = Calendar.getInstance()
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

            for (i in 5 downTo 0) {
                val tempCal = cal.clone() as Calendar
                tempCal.add(Calendar.MONTH, -i)
                val month = tempCal.get(Calendar.MONTH)

                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                tempCal.set(Calendar.HOUR_OF_DAY, 0)
                tempCal.set(Calendar.MINUTE, 0)
                tempCal.set(Calendar.SECOND, 0)
                val mStart = tempCal.timeInMillis

                tempCal.set(Calendar.DAY_OF_MONTH, tempCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                tempCal.set(Calendar.HOUR_OF_DAY, 23)
                tempCal.set(Calendar.MINUTE, 59)
                tempCal.set(Calendar.SECOND, 59)
                val mEnd = tempCal.timeInMillis

                val mTx = allTx.filter { it.dateEpochMillis in mStart..mEnd }
                val inc = mTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val exp = mTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                trends.add(MonthlyTrendItem(monthNames[month], inc, exp, inc - exp))
            }

            StatisticsUiState(
                selectedPeriodTab = periodTab,
                selectedTypeTab = typeTab,
                cashFlow = cashFlow,
                categoryBreakdown = categories,
                monthlyTrends = trends,
                averageDailySpend = avgDaily,
                currency = prefs.currency
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    fun setPeriodTab(tab: Int) {
        _selectedPeriodTab.value = tab
    }

    fun setTypeTab(tab: Int) {
        _selectedTypeTab.value = tab
    }

    private fun getTimeRangeForTab(tab: Int): Triple<Long, Long, Int> {
        val cal = Calendar.getInstance()
        return when (tab) {
            0 -> { // Week
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
                Triple(start, cal.timeInMillis, 7)
            }
            1 -> { // Month
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, daysInMonth)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Triple(start, cal.timeInMillis, daysInMonth)
            }
            2 -> { // Quarter (last 90 days)
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -90)
                Triple(cal.timeInMillis, end, 90)
            }
            else -> { // Year
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
                Triple(start, cal.timeInMillis, 365)
            }
        }
    }
}
