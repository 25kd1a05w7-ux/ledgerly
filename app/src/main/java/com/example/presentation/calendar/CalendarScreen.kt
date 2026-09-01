package com.example.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.presentation.components.AppleCard
import com.example.presentation.components.CurrencyFormatter
import com.example.presentation.dashboard.TransactionItemRow
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    repository: FinanceRepository,
    preferencesRepository: UserPreferencesRepository,
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTx by repository.allTransactionDetailsFlow.collectAsState(initial = emptyList())
    val prefs by preferencesRepository.userPreferencesFlow.collectAsState(initial = com.example.data.preferences.UserPreferences())

    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthTitle = monthFormat.format(currentCalendar.time)

    // Compute transactions for selected month and group by day
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH)

    val monthTransactions = remember(allTx, currentYear, currentMonth) {
        allTx.filter { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.transaction.dateEpochMillis }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }
    }

    val dailyTotals = remember(monthTransactions) {
        val map = mutableMapOf<Int, Pair<Double, Double>>() // day -> Pair(income, expense)
        monthTransactions.forEach { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.transaction.dateEpochMillis }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val current = map[day] ?: Pair(0.0, 0.0)
            if (item.transaction.type == TransactionType.INCOME) {
                map[day] = Pair(current.first + item.transaction.amount, current.second)
            } else if (item.transaction.type == TransactionType.EXPENSE) {
                map[day] = Pair(current.first, current.second + item.transaction.amount)
            }
        }
        map
    }

    val selectedDayTransactions = remember(monthTransactions, selectedDay) {
        monthTransactions.filter { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.transaction.dateEpochMillis }
            cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("calendar_back_btn")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Financial Calendar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        currentCalendar = Calendar.getInstance()
                        selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    }
                ) {
                    Text("Today")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month Switcher Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newCal = currentCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentCalendar = newCal
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month", modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            val newCal = currentCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentCalendar = newCal
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowForwardIos, contentDescription = "Next Month", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Calendar Grid Card
            item {
                AppleCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Weekday headers (Sun, Mon, Tue, ...)
                        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            daysOfWeek.forEach { dayLetter ->
                                Text(
                                    text = dayLetter,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Grid days
                        val cal = currentCalendar.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                        var currentDayIndex = 1
                        val totalCells = ((startDayOfWeek + daysInMonth + 6) / 7) * 7

                        for (week in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (dayCol in 0..6) {
                                    val cellIndex = week * 7 + dayCol
                                    if (cellIndex < startDayOfWeek || currentDayIndex > daysInMonth) {
                                        Spacer(modifier = Modifier.width(36.dp).height(38.dp))
                                    } else {
                                        val dayNumber = currentDayIndex
                                        val isSelected = selectedDay == dayNumber
                                        val totals = dailyTotals[dayNumber]

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                                .clickable { selectedDay = dayNumber },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (totals != null) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        if (totals.first > 0) {
                                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Color.White else IncomeGreen))
                                                        }
                                                        if (totals.second > 0) {
                                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Color.White else ExpenseRed))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        currentDayIndex++
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Summary & Transactions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions on $monthTitle $selectedDay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (selectedDayTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions logged on this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(selectedDayTransactions) { item ->
                    TransactionItemRow(
                        item = item,
                        currency = prefs.currency,
                        onClick = { onTransactionClick(item.transaction.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
