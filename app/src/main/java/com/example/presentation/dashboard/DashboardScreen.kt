package com.example.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BookmarkEntity
import com.example.domain.model.BudgetWithProgress
import com.example.domain.model.CategorySpending
import com.example.domain.model.FinancialInsight
import com.example.domain.model.InsightType
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.presentation.components.AppleCard
import com.example.presentation.components.CategoryBadge
import com.example.presentation.components.CurrencyFormatter
import com.example.presentation.components.EmptyStateView
import com.example.presentation.components.ProgressBarWithIndicator
import com.example.presentation.components.parseColorSafe
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBgLight
import com.example.ui.theme.ExpenseRedBorderLight
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBgLight
import com.example.ui.theme.IncomeGreenBorderLight
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.TransferBlueBgLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAdd: (type: String?, accountId: String?, categoryId: String?) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = uiState.preferences.currency

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Adaptive 2-Column Layout for Tablets / Foldables
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Header, Balance Card, Quick Actions & Recent Transactions
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DashboardHeader(
                            uiState = uiState,
                            onPrevMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            onCalendarClick = onNavigateToCalendar,
                            onSettingsClick = onNavigateToSettings
                        )
                    }
                    item {
                        TotalBalanceCard(
                            uiState = uiState,
                            currency = currency,
                            onAccountsClick = onNavigateToAccounts
                        )
                    }
                    item {
                        QuickActionBar(
                            onAddExpense = { onNavigateToAdd("EXPENSE", null, null) },
                            onAddIncome = { onNavigateToAdd("INCOME", null, null) },
                            onTransfer = { onNavigateToAdd("TRANSFER", null, null) }
                        )
                    }
                    if (uiState.bookmarks.isNotEmpty()) {
                        item {
                            QuickAddBookmarksSection(
                                bookmarks = uiState.bookmarks,
                                currency = currency,
                                onBookmarkClick = { bm ->
                                    onNavigateToAdd(bm.type.name, bm.accountId, bm.categoryId)
                                }
                            )
                        }
                    }
                    item {
                        RecentTransactionsSection(
                            transactions = uiState.recentTransactions,
                            currency = currency,
                            onViewAll = onNavigateToTransactions,
                            onTransactionClick = onNavigateToTransactionDetail
                        )
                    }
                }

                // Right Column: Spending Summary, Budgets & Financial Insights
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SpendingSummaryCard(
                            uiState = uiState,
                            currency = currency,
                            onTabSelected = { viewModel.setPeriodTab(it) },
                            onStatsClick = onNavigateToStatistics
                        )
                    }
                    item {
                        BudgetsProgressSection(
                            budgets = uiState.topBudgets,
                            currency = currency,
                            onViewAll = onNavigateToBudgets,
                            onAddBudget = onNavigateToBudgets
                        )
                    }
                    if (uiState.insights.isNotEmpty()) {
                        item {
                            FinancialInsightsCard(insights = uiState.insights)
                        }
                    }
                }
            }
        } else {
            // Standard Phone Single Column Layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardHeader(
                        uiState = uiState,
                        onPrevMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onCalendarClick = onNavigateToCalendar,
                        onSettingsClick = onNavigateToSettings
                    )
                }

                item {
                    TotalBalanceCard(
                        uiState = uiState,
                        currency = currency,
                        onAccountsClick = onNavigateToAccounts
                    )
                }

                item {
                    QuickActionBar(
                        onAddExpense = { onNavigateToAdd("EXPENSE", null, null) },
                        onAddIncome = { onNavigateToAdd("INCOME", null, null) },
                        onTransfer = { onNavigateToAdd("TRANSFER", null, null) }
                    )
                }

                if (uiState.bookmarks.isNotEmpty()) {
                    item {
                        QuickAddBookmarksSection(
                            bookmarks = uiState.bookmarks,
                            currency = currency,
                            onBookmarkClick = { bm ->
                                onNavigateToAdd(bm.type.name, bm.accountId, bm.categoryId)
                            }
                        )
                    }
                }

                item {
                    SpendingSummaryCard(
                        uiState = uiState,
                        currency = currency,
                        onTabSelected = { viewModel.setPeriodTab(it) },
                        onStatsClick = onNavigateToStatistics
                    )
                }

                item {
                    BudgetsProgressSection(
                        budgets = uiState.topBudgets,
                        currency = currency,
                        onViewAll = onNavigateToBudgets,
                        onAddBudget = onNavigateToBudgets
                    )
                }

                if (uiState.insights.isNotEmpty()) {
                    item {
                        FinancialInsightsCard(insights = uiState.insights)
                    }
                }

                item {
                    RecentTransactionsSection(
                        transactions = uiState.recentTransactions,
                        currency = currency,
                        onViewAll = onNavigateToTransactions,
                        onTransactionClick = onNavigateToTransactionDetail
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(
    uiState: DashboardUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, uiState.selectedYear)
        set(Calendar.MONTH, uiState.selectedMonth)
    }
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthString = monthFormat.format(monthCalendar.time)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clean Month Selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier = Modifier.size(28.dp).testTag("prev_month_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Previous Month",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = monthString,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(2.dp))
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(28.dp).testTag("next_month_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = "Next Month",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Top Right Actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onCalendarClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                    .testTag("dashboard_calendar_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                    .testTag("dashboard_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TotalBalanceCard(
    uiState: DashboardUiState,
    currency: String,
    onAccountsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBalance = uiState.cashFlow.totalBalance
    val income = uiState.cashFlow.income
    val expenses = uiState.cashFlow.expenses
    val net = uiState.cashFlow.netCashFlow

    AppleCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("total_balance_card"),
        backgroundColor = MaterialTheme.colorScheme.surface,
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            // Clean Top Row: Hierarchy with Total Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = CurrencyFormatter.format(totalBalance, currency),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Total balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { onAccountsClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${uiState.accounts.size} accounts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Accounts",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sub-grid with Minimalist Light Green and Light Red Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Income Pill (Light Green)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(IncomeGreenBgLight)
                        .border(1.dp, IncomeGreenBorderLight, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = IncomeGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.format(income, currency, showSign = true),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // Expenses Pill (Light Red)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ExpenseRedBgLight)
                        .border(1.dp, ExpenseRedBorderLight, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.format(expenses, currency, showSign = false),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }

                // Net Flow Pill
                val isNetPositive = net >= 0
                val netBg = if (isNetPositive) IncomeGreenBgLight else ExpenseRedBgLight
                val netBorder = if (isNetPositive) IncomeGreenBorderLight else ExpenseRedBorderLight
                val netColor = if (isNetPositive) IncomeGreen else ExpenseRed

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(netBg)
                        .border(1.dp, netBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "Net Flow",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = netColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.format(net, currency, showSign = true),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = netColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionBar(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pill-shaped / rounded rectangular buttons: White background, subtle border, green accent icons
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionButton(
            text = "Expense",
            icon = Icons.Default.Add,
            modifier = Modifier.weight(1f).testTag("quick_add_expense_btn"),
            onClick = onAddExpense
        )
        QuickActionButton(
            text = "Income",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            modifier = Modifier.weight(1f).testTag("quick_add_income_btn"),
            onClick = onAddIncome
        )
        QuickActionButton(
            text = "Transfer",
            icon = Icons.Default.SwapHoriz,
            modifier = Modifier.weight(1f).testTag("quick_transfer_btn"),
            onClick = onTransfer
        )
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuickAddBookmarksSection(
    bookmarks: List<BookmarkEntity>,
    currency: String,
    onBookmarkClick: (BookmarkEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Quick Add Bookmarks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(bookmarks) { bm ->
                AppleCard(
                    modifier = Modifier
                        .clickable { onBookmarkClick(bm) }
                        .testTag("bookmark_${bm.id}"),
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = bm.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CurrencyFormatter.format(bm.amount, currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = ExpenseRed
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Quick Add",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingSummaryCard(
    uiState: DashboardUiState,
    currency: String,
    onTabSelected: (Int) -> Unit,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Analytics ›",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onStatsClick() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtle Light-Green Segmented Control
            val tabs = listOf("This Week", "This Month", "This Year")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(IncomeGreenBgLight)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = uiState.periodTab == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clickable { onTabSelected(index) },
                        shape = RoundedCornerShape(11.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top categories breakdown or minimal empty message
            if (uiState.topExpenseCategories.isEmpty()) {
                val periodText = when (uiState.periodTab) {
                    0 -> "this week"
                    1 -> "this month"
                    else -> "this year"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No spending $periodText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.topExpenseCategories.forEach { item ->
                        CategorySpendingRow(item = item, currency = currency)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySpendingRow(
    item: CategorySpending,
    currency: String,
    modifier: Modifier = Modifier
) {
    val categoryColor = parseColorSafe(item.category.colorHex)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(
                    iconName = item.category.iconName,
                    colorHex = item.category.colorHex,
                    size = 32.dp,
                    iconSize = 16.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${CurrencyFormatter.format(item.amount, currency)} (${item.percentage.toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        ProgressBarWithIndicator(
            progress = item.percentage / 100f,
            color = categoryColor,
            height = 6.dp
        )
    }
}

@Composable
fun BudgetsProgressSection(
    budgets: List<BudgetWithProgress>,
    currency: String,
    onViewAll: () -> Unit,
    onAddBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (budgets.isEmpty()) "Create Budget" else "Manage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { if (budgets.isEmpty()) onAddBudget() else onViewAll() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (budgets.isEmpty()) {
                Text(
                    text = "No active budgets. Set category limits to control spending.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    budgets.forEach { item ->
                        val catName = item.category?.name ?: item.budget.name
                        val progressColor = if (item.isOverspent) ExpenseRed else MaterialTheme.colorScheme.primary

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${CurrencyFormatter.format(item.spent, currency)} / ${CurrencyFormatter.format(item.budget.amountLimit, currency)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isOverspent) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            ProgressBarWithIndicator(
                                progress = item.percentageUsed / 100f,
                                color = progressColor,
                                height = 8.dp
                            )
                            if (item.isOverspent) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Over budget by ${CurrencyFormatter.format(Math.abs(item.remaining), currency)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialInsightsCard(
    insights: List<FinancialInsight>,
    modifier: Modifier = Modifier
) {
    val topInsight = insights.firstOrNull() ?: return

    val icon = when (topInsight.type) {
        InsightType.WARNING -> Icons.Default.Warning
        InsightType.SUCCESS -> Icons.AutoMirrored.Filled.TrendingUp
        InsightType.BUDGET -> Icons.Default.PieChart
        InsightType.INFO -> Icons.Default.Lightbulb
    }
    val tint = when (topInsight.type) {
        InsightType.WARNING -> ExpenseRed
        InsightType.SUCCESS -> IncomeGreen
        InsightType.BUDGET -> TransferBlue
        InsightType.INFO -> MaterialTheme.colorScheme.primary
    }

    AppleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = tint.copy(alpha = 0.08f),
        borderColor = tint.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topInsight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = topInsight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<TransactionDetail>,
    currency: String,
    onViewAll: () -> Unit,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transactions.isNotEmpty()) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onViewAll() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                Text(
                    text = "No recent transactions found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    transactions.forEach { item ->
                        TransactionItemRow(
                            item = item,
                            currency = currency,
                            onClick = { onTransactionClick(item.transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    item: TransactionDetail,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tx = item.transaction
    val title = if (tx.merchant.isNotBlank()) tx.merchant else item.category?.name ?: "Transaction"
    val subtitle = buildString {
        append(item.account?.name ?: "Account")
        if (item.destinationAccount != null && tx.type == TransactionType.TRANSFER) {
            append(" → ")
            append(item.destinationAccount.name)
        }
    }
    val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateStr = timeFormat.format(Date(tx.dateEpochMillis))

    val amountColor = when (tx.type) {
        TransactionType.INCOME, TransactionType.REFUND -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
        TransactionType.ADJUSTMENT -> if (tx.amount >= 0) IncomeGreen else ExpenseRed
    }

    val amountPrefix = when (tx.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
        TransactionType.REFUND -> "+"
        TransactionType.ADJUSTMENT -> if (tx.amount >= 0) "+" else "-"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            CategoryBadge(
                iconName = item.category?.iconName ?: "category",
                colorHex = item.category?.colorHex ?: "#10B981",
                size = 42.dp,
                iconSize = 20.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "$subtitle • $dateStr",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$amountPrefix${CurrencyFormatter.format(Math.abs(tx.amount), currency)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

private fun getGreetingMessage(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}
