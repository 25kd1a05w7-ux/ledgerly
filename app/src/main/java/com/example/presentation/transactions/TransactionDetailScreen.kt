package com.example.presentation.transactions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.presentation.components.AppleCard
import com.example.presentation.components.CategoryBadge
import com.example.presentation.components.CurrencyFormatter
import com.example.presentation.components.parseColorSafe
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    repository: FinanceRepository,
    preferencesRepository: UserPreferencesRepository,
    onNavigateBack: () -> Unit,
    onEditTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var detail by remember { mutableStateOf<TransactionDetail?>(null) }
    var currency by remember { mutableStateOf("USD") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(transactionId) {
        val prefs = preferencesRepository.userPreferencesFlow.first()
        currency = prefs.currency
        val allTx = repository.allTransactionDetailsFlow.first()
        detail = allTx.firstOrNull { it.transaction.id == transactionId }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to permanently delete this transaction? All associated account balances will be adjusted.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteTransactionById(transactionId)
                            showDeleteConfirm = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
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
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("detail_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(
                        onClick = { onEditTransaction(transactionId) },
                        modifier = Modifier.testTag("detail_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("detail_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ExpenseRed
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val currentDetail = detail
        if (currentDetail == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading transaction...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val tx = currentDetail.transaction
            val cat = currentDetail.category
            val acc = currentDetail.account
            val destAcc = currentDetail.destinationAccount

            val amountColor = when (tx.type) {
                TransactionType.INCOME -> IncomeGreen
                TransactionType.EXPENSE -> ExpenseRed
                TransactionType.TRANSFER -> TransferBlue
                else -> MaterialTheme.colorScheme.onSurface
            }
            val sign = when (tx.type) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE -> "-"
                else -> ""
            }

            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
            val dateStr = dateFormat.format(Date(tx.dateEpochMillis))

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Receipt Header
                AppleCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CategoryBadge(
                            iconName = cat?.iconName ?: "category",
                            colorHex = cat?.colorHex ?: "#10B981",
                            size = 64.dp,
                            iconSize = 32.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (tx.merchant.isNotBlank()) tx.merchant else cat?.name ?: "Transaction",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$sign${CurrencyFormatter.format(tx.amount, currency)}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(amountColor.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tx.type.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = amountColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown Attributes Card
                AppleCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DetailItemRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Date & Time",
                            value = dateStr
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        DetailItemRow(
                            icon = Icons.Default.Category,
                            label = "Category",
                            value = cat?.name ?: "Uncategorized"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        DetailItemRow(
                            icon = Icons.Default.AccountBalance,
                            label = if (tx.type == TransactionType.TRANSFER) "From Account" else "Account",
                            value = acc?.name ?: "Unknown"
                        )

                        if (tx.type == TransactionType.TRANSFER && destAcc != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            DetailItemRow(
                                icon = Icons.Default.AccountBalance,
                                label = "To Account",
                                value = destAcc.name
                            )
                        }

                        if (tx.merchant.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            DetailItemRow(
                                icon = Icons.Default.Storefront,
                                label = "Merchant / Payee",
                                value = tx.merchant
                            )
                        }

                        if (tx.note.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            DetailItemRow(
                                icon = Icons.Default.Notes,
                                label = "Notes",
                                value = tx.note
                            )
                        }

                        if (tx.tags.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            DetailItemRow(
                                icon = Icons.Default.Tag,
                                label = "Tags",
                                value = tx.tags.joinToString(" ") { "#$it" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Duplicate Button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val copy = tx.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                dateEpochMillis = System.currentTimeMillis()
                            )
                            repository.saveTransaction(copy)
                            onNavigateBack()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("duplicate_tx_btn")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duplicate This Transaction")
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.4f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}
