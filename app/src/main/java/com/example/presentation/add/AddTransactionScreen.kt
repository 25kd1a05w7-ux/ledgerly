package com.example.presentation.add

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.CategoryType
import com.example.domain.model.Currencies
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import com.example.presentation.components.AppleCard
import com.example.presentation.components.CalculatorKeypad
import com.example.presentation.components.CategoryBadge
import com.example.presentation.components.CurrencyFormatter
import com.example.presentation.components.getIconForName
import com.example.presentation.components.parseColorSafe
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMoreDetails by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    val currencySymbol = Currencies.getSymbol(uiState.currency)

    val activeCategories = remember(uiState.availableCategories, uiState.transactionType) {
        val targetType = if (uiState.transactionType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        uiState.availableCategories.filter { it.type == targetType }
    }

    val primaryColor = when (uiState.transactionType) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
        else -> MaterialTheme.colorScheme.primary
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
                    modifier = Modifier.testTag("add_tx_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (uiState.editingTransactionId != null) "Edit Transaction" else "Add Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { viewModel.saveTransaction() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.testTag("save_transaction_button")
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type Selector (Expense / Income / Transfer)
            TypeSegmentedControl(
                selectedType = uiState.transactionType,
                onSelectType = { viewModel.setTransactionType(it) },
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Hero Amount Display with live calculator feedback
            HeroAmountDisplay(
                expression = uiState.amountExpression,
                evaluatedAmount = uiState.evaluatedAmount,
                currencySymbol = currencySymbol,
                accentColor = primaryColor
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Selector (Carousel / Chips) - only for Expense & Income
            if (uiState.transactionType != TransactionType.TRANSFER) {
                CategorySelectorBar(
                    categories = activeCategories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onSelect = { viewModel.selectCategory(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Account Selector (Source & Destination)
            AccountSelectorBar(
                accounts = uiState.availableAccounts,
                selectedAccountId = uiState.selectedAccountId,
                selectedDestinationAccountId = uiState.selectedDestinationAccountId,
                isTransfer = uiState.transactionType == TransactionType.TRANSFER,
                onSelectSource = { viewModel.selectAccount(it) },
                onSelectDestination = { viewModel.selectDestinationAccount(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Expandable details (Date, Merchant, Note, Tags, Recurring)
            AppleCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMoreDetails = !showMoreDetails },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                        val dateText = dateFormat.format(Date(uiState.selectedDateEpochMillis))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showMoreDetails) "Less Details" else "More Details",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (showMoreDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = showMoreDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Date Picker Button
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDateEpochMillis }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(y, m, d)
                                            }
                                            viewModel.setDateEpoch(newCal.timeInMillis)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Change Date")
                            }

                            // Merchant / Payee
                            OutlinedTextField(
                                value = uiState.merchant,
                                onValueChange = { viewModel.setMerchant(it) },
                                label = { Text("Merchant / Payee") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("tx_merchant_input")
                            )

                            // Notes
                            OutlinedTextField(
                                value = uiState.note,
                                onValueChange = { viewModel.setNote(it) },
                                label = { Text("Notes") },
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("tx_note_input")
                            )

                            // Tags row
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Tags",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = uiState.currentTagInput,
                                        onValueChange = { viewModel.setTagInput(it) },
                                        placeholder = { Text("Add tag (e.g. tax, vacation)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).testTag("tag_input")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.addTag(uiState.currentTagInput) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Add")
                                    }
                                }

                                if (uiState.tags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(uiState.tags) { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "#$tag",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove tag",
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable { viewModel.removeTag(tag) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Recurring toggle
                            if (uiState.editingTransactionId == null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = uiState.isRecurring,
                                        onCheckedChange = { viewModel.setRecurring(it) }
                                    )
                                    Text(
                                        text = "Repeat regularly (Recurring)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Direct Calculator Keypad
            CalculatorKeypad(
                onDigitClick = { viewModel.onDigitInput(it) },
                onOperatorClick = { viewModel.onOperatorInput(it) },
                onBackspace = { viewModel.onBackspace() },
                onClear = { viewModel.onClear() },
                onEquals = { viewModel.onEquals() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TypeSegmentedControl(
    selectedType: TransactionType,
    onSelectType: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = listOf(
        TransactionType.EXPENSE,
        TransactionType.INCOME,
        TransactionType.TRANSFER
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        types.forEach { type ->
            val isSelected = selectedType == type
            val activeColor = when (type) {
                TransactionType.EXPENSE -> ExpenseRed
                TransactionType.INCOME -> IncomeGreen
                TransactionType.TRANSFER -> TransferBlue
                else -> MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelectType(type) }
                    .testTag("type_${type.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HeroAmountDisplay(
    expression: String,
    evaluatedAmount: Double,
    currencySymbol: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val displayText = if (expression.isBlank()) "0" else expression

        Text(
            text = "$currencySymbol$displayText",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
            fontWeight = FontWeight.Bold,
            color = accentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // If expression contains operators, show evaluated preview below
        if (expression.contains("+") || expression.contains("-") || expression.contains("*") || expression.contains("/") || expression.contains("%")) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "= $currencySymbol${String.format(Locale.US, "%.2f", evaluatedAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategorySelectorBar(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategoryId == category.id
                val catColor = parseColorSafe(category.colorHex)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) catColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(category.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("cat_chip_${category.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(
                            iconName = category.iconName,
                            colorHex = category.colorHex,
                            size = 28.dp,
                            iconSize = 14.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSelectorBar(
    accounts: List<AccountEntity>,
    selectedAccountId: String?,
    selectedDestinationAccountId: String?,
    isTransfer: Boolean,
    onSelectSource: (String) -> Unit,
    onSelectDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isTransfer) "From Account" else "Account",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(accounts) { acc ->
                val isSelected = selectedAccountId == acc.id
                val accColor = parseColorSafe(acc.colorHex)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) accColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectSource(acc.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("acc_chip_${acc.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = acc.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (isTransfer) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "To Destination Account",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accounts) { acc ->
                    val isSelected = selectedDestinationAccountId == acc.id
                    val accColor = parseColorSafe(acc.colorHex)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) TransferBlue.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) TransferBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDestination(acc.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("dest_acc_chip_${acc.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
