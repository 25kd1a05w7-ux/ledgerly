package com.example.presentation.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.presentation.components.AppleCard
import com.example.presentation.components.CategoryBadge
import com.example.presentation.components.CurrencyFormatter
import com.example.presentation.components.EmptyStateView
import com.example.presentation.dashboard.TransactionItemRow
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import kotlinx.coroutines.launch

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Header & Search Bar
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row {
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                TransactionSortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.displayName) },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        if (uiState.isSelectionMode) {
                            IconButton(
                                onClick = {
                                    viewModel.deleteSelected()
                                },
                                modifier = Modifier.testTag("delete_selected_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = ExpenseRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Input Field
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search merchant, note, #tag...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_search_bar")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips Row (Type & Date range)
                TransactionFilterChips(
                    selectedType = uiState.selectedTypeFilter,
                    selectedDate = uiState.dateFilter,
                    onSelectType = { viewModel.setTypeFilter(it) },
                    onSelectDate = { viewModel.setDateFilter(it) }
                )
            }

            // Summary Pill (Total Count, Income, Expense)
            if (uiState.filteredTransactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.filteredTransactions.size} transactions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "+${CurrencyFormatter.format(uiState.totalFilteredIncome, uiState.currency)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IncomeGreen
                        )
                        Text(
                            text = "-${CurrencyFormatter.format(uiState.totalFilteredExpense, uiState.currency)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            // Grouped Transaction List
            if (uiState.filteredTransactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = if (uiState.searchQuery.isNotBlank()) "No Matching Transactions" else "No Transactions Yet",
                    description = if (uiState.searchQuery.isNotBlank()) "Try adjusting your search terms or filters." else "Your financial ledger is empty. Tap below to log your first record.",
                    actionText = "Add Transaction",
                    onAction = onNavigateToAdd,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    uiState.groupedTransactions.forEach { (dateHeader, txList) ->
                        item(key = dateHeader) {
                            Column {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                AppleCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = MaterialTheme.colorScheme.surface
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        txList.forEachIndexed { index, item ->
                                            val isSelected = uiState.selectedTransactionIds.contains(item.transaction.id)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (uiState.isSelectionMode) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { viewModel.toggleTransactionSelection(item.transaction.id) }
                                                    )
                                                }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    TransactionItemRow(
                                                        item = item,
                                                        currency = uiState.currency,
                                                        onClick = {
                                                            if (uiState.isSelectionMode) {
                                                                viewModel.toggleTransactionSelection(item.transaction.id)
                                                            } else {
                                                                onNavigateToDetail(item.transaction.id)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            if (index < txList.size - 1) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionFilterChips(
    selectedType: TransactionType?,
    selectedDate: DateFilterOption,
    onSelectType: (TransactionType?) -> Unit,
    onSelectDate: (DateFilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All
        item {
            FilterChipItem(
                text = "All",
                isSelected = selectedType == null,
                onClick = { onSelectType(null) }
            )
        }
        // Expense
        item {
            FilterChipItem(
                text = "Expense",
                isSelected = selectedType == TransactionType.EXPENSE,
                selectedColor = ExpenseRed,
                onClick = { onSelectType(TransactionType.EXPENSE) }
            )
        }
        // Income
        item {
            FilterChipItem(
                text = "Income",
                isSelected = selectedType == TransactionType.INCOME,
                selectedColor = IncomeGreen,
                onClick = { onSelectType(TransactionType.INCOME) }
            )
        }
        // Transfer
        item {
            FilterChipItem(
                text = "Transfer",
                isSelected = selectedType == TransactionType.TRANSFER,
                selectedColor = TransferBlue,
                onClick = { onSelectType(TransactionType.TRANSFER) }
            )
        }
        // Date options
        DateFilterOption.values().forEach { opt ->
            item {
                FilterChipItem(
                    text = opt.displayName,
                    isSelected = selectedDate == opt,
                    onClick = { onSelectDate(opt) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    text: String,
    isSelected: Boolean,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) selectedColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (isSelected) selectedColor else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
