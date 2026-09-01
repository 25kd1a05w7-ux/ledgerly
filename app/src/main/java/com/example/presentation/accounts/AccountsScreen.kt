package com.example.presentation.accounts

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
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
import com.example.data.local.entity.AccountEntity
import com.example.domain.model.AccountType
import com.example.domain.model.AccountWithBalance
import com.example.presentation.components.AppleCard
import com.example.presentation.components.CurrencyFormatter
import com.example.presentation.components.ProgressBarWithIndicator
import com.example.presentation.components.getIconForName
import com.example.presentation.components.parseColorSafe
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onNavigateToAddTransactionForAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddAccount() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_account")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Account")
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
                text = "Accounts & Net Worth",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
            )

            // Net Worth Hero Card
            NetWorthBannerCard(
                netWorth = uiState.netWorth,
                assets = uiState.totalAssets,
                liabilities = uiState.totalLiabilities,
                currency = uiState.currency
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account groups
            val assetAccounts = uiState.accounts.filter { it.account.type != AccountType.CREDIT_CARD && it.account.type != AccountType.LOAN }
            val liabilityAccounts = uiState.accounts.filter { it.account.type == AccountType.CREDIT_CARD || it.account.type == AccountType.LOAN }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (assetAccounts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cash & Assets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(assetAccounts) { accWithBal ->
                        AccountItemCard(
                            accountWithBalance = accWithBal,
                            currency = uiState.currency,
                            onEdit = { viewModel.openEditAccount(accWithBal.account) },
                            onAddTransaction = { onNavigateToAddTransactionForAccount(accWithBal.account.id) }
                        )
                    }
                }

                if (liabilityAccounts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Credit Cards & Liabilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(liabilityAccounts) { accWithBal ->
                        AccountItemCard(
                            accountWithBalance = accWithBal,
                            currency = uiState.currency,
                            onEdit = { viewModel.openEditAccount(accWithBal.account) },
                            onAddTransaction = { onNavigateToAddTransactionForAccount(accWithBal.account.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (uiState.isAddAccountOpen) {
        AccountEditDialog(
            account = uiState.editingAccount,
            currency = uiState.currency,
            onDismiss = { viewModel.closeAccountDialog() },
            onSave = { name, type, initBal, curr, color, icon, limit ->
                viewModel.saveAccount(name, type, initBal, curr, color, icon, limit)
            },
            onDelete = {
                uiState.editingAccount?.id?.let { viewModel.deleteAccount(it) }
            }
        )
    }
}

@Composable
fun NetWorthBannerCard(
    netWorth: Double,
    assets: Double,
    liabilities: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Total Net Worth",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyFormatter.format(netWorth, currency),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (netWorth >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Assets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(assets, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
                Column {
                    Text(
                        text = "Liabilities",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(liabilities, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun AccountItemCard(
    accountWithBalance: AccountWithBalance,
    currency: String,
    onEdit: () -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = accountWithBalance.account
    val balance = accountWithBalance.currentBalance
    val accColor = parseColorSafe(account.colorHex)

    AppleCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("account_card_${account.id}"),
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
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForName(account.iconName),
                            contentDescription = null,
                            tint = accColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = account.type.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.format(balance, account.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (account.type == AccountType.CREDIT_CARD || account.type == AccountType.LOAN) {
                            if (balance < 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        } else {
                            if (balance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                        }
                    )
                    Text(
                        text = "${accountWithBalance.transactionCount} transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // If credit card with a credit limit, show utilization bar
            if (account.type == AccountType.CREDIT_CARD && account.creditLimit > 0) {
                val used = Math.abs(balance)
                val limit = account.creditLimit
                val progress = (used / limit).toFloat()

                Spacer(modifier = Modifier.height(10.dp))
                ProgressBarWithIndicator(
                    progress = progress,
                    color = if (progress > 0.8f) ExpenseRed else TransferBlue,
                    height = 6.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% utilized",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Limit: ${CurrencyFormatter.format(limit, account.currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditDialog(
    account: AccountEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: AccountType,
        openingBalance: Double,
        currency: String,
        colorHex: String,
        iconName: String,
        creditLimit: Double
    ) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var selectedType by remember { mutableStateOf(account?.type ?: AccountType.BANK) }
    var initialBalanceText by remember { mutableStateOf(account?.openingBalance?.toString() ?: "0.0") }
    var creditLimitText by remember { mutableStateOf(account?.creditLimit?.toString() ?: "0.0") }
    var selectedColor by remember { mutableStateOf(account?.colorHex ?: "#10B981") }
    var selectedIcon by remember { mutableStateOf(account?.iconName ?: "account_balance") }
    var expandedTypeDropdown by remember { mutableStateOf(false) }

    val colors = listOf("#10B981", "#3B82F6", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899", "#6366F1", "#14B8A6")
    val icons = listOf("account_balance", "account_balance_wallet", "savings", "credit_card", "attach_money")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (account == null) "Add New Account" else "Edit Account",
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                )

                // Account Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Initial Balance
                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("Opening / Starting Balance") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("initial_balance_input")
                )

                // Credit Limit if credit card
                if (selectedType == AccountType.CREDIT_CARD) {
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = { creditLimitText = it },
                        label = { Text("Credit Limit") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Color picker row
                Column {
                    Text("Theme Color", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colors) { hex ->
                            val color = parseColorSafe(hex)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex }
                                    .then(
                                        if (selectedColor == hex) Modifier.background(Color.White.copy(alpha = 0.3f)) else Modifier
                                    )
                            )
                        }
                    }
                }

                // Icon picker row
                Column {
                    Text("Icon", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(icons) { iconName ->
                            val isSelected = selectedIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForName(iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initBal = initialBalanceText.toDoubleOrNull() ?: 0.0
                    val limit = creditLimitText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onSave(name, selectedType, initBal, currency, selectedColor, selectedIcon, limit)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_account_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (account != null) {
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
