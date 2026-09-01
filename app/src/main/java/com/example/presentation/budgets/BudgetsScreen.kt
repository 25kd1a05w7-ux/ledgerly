package com.example.presentation.budgets

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.BudgetWithProgress
import com.example.presentation.components.AppleCard
import com.example.presentation.components.CategoryBadge
import com.example.presentation.components.CurrencyFormatter
import com.example.presentation.components.EmptyStateView
import com.example.presentation.components.ProgressBarWithIndicator
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddBudget() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_budget")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Budgets & Planning",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // Segment Tabs (Budgets vs Recurring)
            val tabs = listOf("Category Budgets", "Recurring & Subscriptions")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = uiState.activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { viewModel.setTab(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.activeTab == 0) {
                // Total Budget Hero Overview Card
                TotalBudgetHealthCard(
                    totalBudgeted = uiState.totalBudgeted,
                    totalSpent = uiState.totalSpent,
                    totalRemaining = uiState.totalRemaining,
                    percentage = uiState.overallPercentage,
                    currency = uiState.currency
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.budgets.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.PieChart,
                        title = "No Active Budgets",
                        description = "Create category budgets to set spending boundaries and receive smart threshold alerts.",
                        actionText = "Create Budget",
                        onAction = { viewModel.openAddBudget() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.budgets) { budgetWithProgress ->
                            BudgetItemCard(
                                item = budgetWithProgress,
                                currency = uiState.currency,
                                onClick = { viewModel.openEditBudget(budgetWithProgress.budget) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            } else {
                // Recurring Subscriptions Tab
                if (uiState.recurringList.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Repeat,
                        title = "No Recurring Transactions",
                        description = "Track subscriptions, monthly rent, and scheduled salary payments in one place.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recurringList) { recurring ->
                            RecurringItemCard(
                                recurring = recurring,
                                currency = uiState.currency,
                                onToggleActive = { viewModel.toggleRecurringActive(recurring) },
                                onDelete = { viewModel.deleteRecurring(recurring.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddBudgetOpen) {
        BudgetEditDialog(
            budget = uiState.editingBudget,
            categories = uiState.availableCategories,
            onDismiss = { viewModel.closeBudgetDialog() },
            onSave = { name, catId, limit, period, rollover, notif80, notif100 ->
                viewModel.saveBudget(name, catId, limit, period, rollover, notif80, notif100)
            },
            onDelete = {
                uiState.editingBudget?.id?.let { viewModel.deleteBudget(it) }
            }
        )
    }
}

@Composable
fun TotalBudgetHealthCard(
    totalBudgeted: Double,
    totalSpent: Double,
    totalRemaining: Double,
    percentage: Float,
    currency: String,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        percentage > 100f -> ExpenseRed
        percentage > 80f -> WarningAmber
        else -> EmeraldPrimary
    }

    // Days remaining in month calculation
    val cal = Calendar.getInstance()
    val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
    val daysRemaining = Math.max(1, totalDays - currentDay)
    val dailyAllowance = if (totalRemaining > 0) totalRemaining / daysRemaining else 0.0

    AppleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Monthly Budget",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${daysRemaining}d left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${CurrencyFormatter.format(totalSpent, currency)} / ${CurrencyFormatter.format(totalBudgeted, currency)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))
            ProgressBarWithIndicator(
                progress = percentage / 100f,
                color = progressColor,
                height = 10.dp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (totalRemaining >= 0) "Remaining" else "Overspent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(Math.abs(totalRemaining), currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalRemaining >= 0) IncomeGreen else ExpenseRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Safe Daily Spend",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${CurrencyFormatter.format(dailyAllowance, currency)}/day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetItemCard(
    item: BudgetWithProgress,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = item.budget
    val category = item.category
    val progressColor = if (item.isOverspent) ExpenseRed else if (item.percentageUsed > 80f) WarningAmber else EmeraldPrimary

    AppleCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("budget_card_${budget.id}"),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(
                        iconName = category?.iconName ?: "pie_chart",
                        colorHex = category?.colorHex ?: "#10B981",
                        size = 38.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category?.name ?: budget.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = budget.period.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${CurrencyFormatter.format(item.spent, currency)} / ${CurrencyFormatter.format(budget.amountLimit, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isOverspent) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.percentageUsed.toInt()}% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isOverspent) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ProgressBarWithIndicator(
                progress = item.percentageUsed / 100f,
                color = progressColor,
                height = 7.dp
            )

            if (item.isOverspent) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Exceeded by ${CurrencyFormatter.format(Math.abs(item.remaining), currency)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ExpenseRed,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${CurrencyFormatter.format(item.remaining, currency)} remaining (${CurrencyFormatter.format(item.dailySafeSpend, currency)}/day)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecurringItemCard(
    recurring: RecurringTransactionEntity,
    currency: String,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dueDateStr = timeFormat.format(Date(recurring.nextDueDateEpochMillis))

    AppleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (recurring.merchant.isNotBlank()) recurring.merchant else "Recurring Payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${recurring.frequency.displayName} • Next: $dueDateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyFormatter.format(recurring.amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = recurring.isActive,
                    onCheckedChange = { onToggleActive() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditDialog(
    budget: BudgetEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        categoryId: String?,
        amountLimit: Double,
        period: BudgetPeriod,
        rollover: Boolean,
        notifyAt80: Boolean,
        notifyAt100: Boolean
    ) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(budget?.name ?: "") }
    var selectedCategoryId by remember { mutableStateOf(budget?.categoryId ?: categories.firstOrNull()?.id) }
    var amountText by remember { mutableStateOf(budget?.amountLimit?.toString() ?: "500.0") }
    var selectedPeriod by remember { mutableStateOf(budget?.period ?: BudgetPeriod.MONTHLY) }
    var rollover by remember { mutableStateOf(budget?.rollover ?: false) }
    var notifyAt80 by remember { mutableStateOf(budget?.alert75 ?: true) }
    var notifyAt100 by remember { mutableStateOf(budget?.alert100 ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (budget == null) "Create Budget" else "Edit Budget",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Selector
                Column {
                    Text("Select Category", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedCategoryId = cat.id
                                        if (name.isBlank()) name = cat.name
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("budget_name_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Budget Limit") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                )

                // Rollover toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rollover unused balance to next period", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = rollover, onCheckedChange = { rollover = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = amountText.toDoubleOrNull() ?: 0.0
                    val finalName = if (name.isNotBlank()) name else categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Budget"
                    if (limit > 0) {
                        onSave(finalName, selectedCategoryId, limit, selectedPeriod, rollover, notifyAt80, notifyAt100)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_budget_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (budget != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = ExpenseRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
