package com.example.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.CategoryType
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import com.example.presentation.components.CalculatorEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class AddTransactionUiState(
    val editingTransactionId: String? = null,
    val amountExpression: String = "",
    val evaluatedAmount: Double = 0.0,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedCategoryId: String? = null,
    val selectedAccountId: String? = null,
    val selectedDestinationAccountId: String? = null,
    val selectedDateEpochMillis: Long = System.currentTimeMillis(),
    val merchant: String = "",
    val note: String = "",
    val tags: List<String> = emptyList(),
    val currentTagInput: String = "",
    val attachmentPath: String? = null,
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val availableAccounts: List<AccountEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val currency: String = "INR",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AddTransactionViewModel(
    private val repository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.activeAccountsWithBalanceFlow,
                repository.allCategoriesFlow,
                preferencesRepository.userPreferencesFlow
            ) { accountsWithBal, categories, prefs ->
                val accounts = accountsWithBal.map { it.account }
                val current = _uiState.value

                val defaultAccount = current.selectedAccountId ?: accounts.firstOrNull()?.id
                val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
                val defaultCategory = current.selectedCategoryId ?: expenseCategories.firstOrNull()?.id

                current.copy(
                    availableAccounts = accounts,
                    availableCategories = categories,
                    currency = prefs.currency,
                    selectedAccountId = defaultAccount,
                    selectedCategoryId = defaultCategory
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun initForNew(typeStr: String?, accountId: String?, categoryId: String?) {
        val type = when (typeStr?.uppercase()) {
            "INCOME" -> TransactionType.INCOME
            "TRANSFER" -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        _uiState.value = _uiState.value.copy(
            transactionType = type,
            selectedAccountId = accountId ?: _uiState.value.selectedAccountId,
            selectedCategoryId = categoryId ?: _uiState.value.selectedCategoryId,
            amountExpression = "",
            evaluatedAmount = 0.0
        )
    }

    fun initForEdit(transactionId: String) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(transactionId) ?: return@launch
            _uiState.value = _uiState.value.copy(
                editingTransactionId = tx.id,
                amountExpression = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else tx.amount.toString(),
                evaluatedAmount = tx.amount,
                transactionType = tx.type,
                selectedCategoryId = tx.categoryId,
                selectedAccountId = tx.accountId,
                selectedDestinationAccountId = tx.destinationAccountId,
                selectedDateEpochMillis = tx.dateEpochMillis,
                merchant = tx.merchant,
                note = tx.note,
                tags = tx.tags,
                attachmentPath = tx.attachmentPath
            )
        }
    }

    fun setTransactionType(type: TransactionType) {
        val targetCatType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val firstMatchingCategory = _uiState.value.availableCategories
            .firstOrNull { it.type == targetCatType }?.id

        _uiState.value = _uiState.value.copy(
            transactionType = type,
            selectedCategoryId = firstMatchingCategory ?: _uiState.value.selectedCategoryId
        )
    }

    fun onDigitInput(digit: String) {
        val current = _uiState.value.amountExpression
        // Prevent multiple leading zeros or multiple dots in the same number segment
        val updated = current + digit
        val evaluated = CalculatorEvaluator.evaluate(updated) ?: _uiState.value.evaluatedAmount
        _uiState.value = _uiState.value.copy(amountExpression = updated, evaluatedAmount = evaluated)
    }

    fun onOperatorInput(op: String) {
        val current = _uiState.value.amountExpression
        if (current.isBlank()) return
        val lastChar = current.last()
        if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/' || lastChar == '%') {
            val updated = current.dropLast(1) + op
            _uiState.value = _uiState.value.copy(amountExpression = updated)
        } else {
            val updated = current + op
            _uiState.value = _uiState.value.copy(amountExpression = updated)
        }
    }

    fun onBackspace() {
        val current = _uiState.value.amountExpression
        if (current.isNotEmpty()) {
            val updated = current.dropLast(1)
            val evaluated = CalculatorEvaluator.evaluate(updated) ?: 0.0
            _uiState.value = _uiState.value.copy(amountExpression = updated, evaluatedAmount = evaluated)
        }
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(amountExpression = "", evaluatedAmount = 0.0)
    }

    fun onEquals() {
        val evaluated = CalculatorEvaluator.evaluate(_uiState.value.amountExpression) ?: 0.0
        val cleanStr = if (evaluated % 1.0 == 0.0) evaluated.toLong().toString() else "%.2f".format(evaluated)
        _uiState.value = _uiState.value.copy(amountExpression = cleanStr, evaluatedAmount = evaluated)
    }

    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun selectAccount(accountId: String) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
    }

    fun selectDestinationAccount(accountId: String) {
        _uiState.value = _uiState.value.copy(selectedDestinationAccountId = accountId)
    }

    fun setDateEpoch(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(selectedDateEpochMillis = dateMillis)
    }

    fun setMerchant(merchant: String) {
        _uiState.value = _uiState.value.copy(merchant = merchant)
    }

    fun setNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun setTagInput(input: String) {
        _uiState.value = _uiState.value.copy(currentTagInput = input)
    }

    fun addTag(tag: String) {
        val cleanTag = tag.trim().removePrefix("#")
        if (cleanTag.isNotBlank() && cleanTag !in _uiState.value.tags) {
            _uiState.value = _uiState.value.copy(
                tags = _uiState.value.tags + cleanTag,
                currentTagInput = ""
            )
        }
    }

    fun removeTag(tag: String) {
        _uiState.value = _uiState.value.copy(
            tags = _uiState.value.tags - tag
        )
    }

    fun setAttachmentPath(path: String?) {
        _uiState.value = _uiState.value.copy(attachmentPath = path)
    }

    fun setRecurring(isRecurring: Boolean, frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY) {
        _uiState.value = _uiState.value.copy(
            isRecurring = isRecurring,
            recurrenceFrequency = frequency
        )
    }

    fun saveTransaction() {
        val state = _uiState.value
        val finalAmount = CalculatorEvaluator.evaluate(state.amountExpression) ?: state.evaluatedAmount

        if (finalAmount <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Please enter an amount greater than 0")
            return
        }

        val accountId = state.selectedAccountId
        if (accountId == null) {
            _uiState.value = state.copy(errorMessage = "Please select an account")
            return
        }

        if (state.transactionType == TransactionType.TRANSFER && state.selectedDestinationAccountId == null) {
            _uiState.value = state.copy(errorMessage = "Please select a destination account for transfer")
            return
        }

        if (state.transactionType == TransactionType.TRANSFER && state.selectedDestinationAccountId == accountId) {
            _uiState.value = state.copy(errorMessage = "Source and destination accounts must be different")
            return
        }

        val categoryId = state.selectedCategoryId ?: "cat_other_expense"

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)

            val txId = state.editingTransactionId ?: UUID.randomUUID().toString()
            val tx = TransactionEntity(
                id = txId,
                amount = finalAmount,
                currency = state.currency,
                dateEpochMillis = state.selectedDateEpochMillis,
                type = state.transactionType,
                categoryId = categoryId,
                accountId = accountId,
                destinationAccountId = state.selectedDestinationAccountId,
                merchant = state.merchant.trim(),
                note = state.note.trim(),
                tags = state.tags,
                attachmentPath = state.attachmentPath
            )

            repository.saveTransaction(tx)

            // If user checked recurring
            if (state.isRecurring && state.editingTransactionId == null) {
                val recurring = RecurringTransactionEntity(
                    amount = finalAmount,
                    currency = state.currency,
                    type = state.transactionType,
                    categoryId = categoryId,
                    accountId = accountId,
                    destinationAccountId = state.selectedDestinationAccountId,
                    merchant = state.merchant.trim(),
                    note = state.note.trim(),
                    frequency = state.recurrenceFrequency,
                    startDateEpochMillis = state.selectedDateEpochMillis,
                    nextDueDateEpochMillis = state.selectedDateEpochMillis + (1000L * 60 * 60 * 24 * 30)
                )
                repository.saveRecurring(recurring)
            }

            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
        }
    }
}
