package com.example.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.AccountType
import com.example.domain.model.AccountWithBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = 0.0,
    val currency: String = "INR",
    val isAddAccountOpen: Boolean = false,
    val editingAccount: AccountEntity? = null
)

class AccountsViewModel(
    private val repository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isAddAccountOpen = MutableStateFlow(false)
    private val _editingAccount = MutableStateFlow<AccountEntity?>(null)

    val uiState: StateFlow<AccountsUiState> = combine(
        repository.activeAccountsWithBalanceFlow,
        preferencesRepository.userPreferencesFlow,
        _isAddAccountOpen,
        _editingAccount
    ) { accounts, prefs, isAddOpen, editingAcc ->
        val assets = accounts.filter { it.account.type != AccountType.CREDIT_CARD && it.account.type != AccountType.LOAN }
            .sumOf { if (it.balance > 0) it.balance else 0.0 }
        val liabilities = accounts.filter { it.account.type == AccountType.CREDIT_CARD || it.account.type == AccountType.LOAN || it.balance < 0 }
            .sumOf { Math.abs(if (it.balance < 0) it.balance else 0.0) }
        val netWorth = accounts.sumOf { it.balance }

        AccountsUiState(
            accounts = accounts,
            totalAssets = assets,
            totalLiabilities = liabilities,
            netWorth = netWorth,
            currency = prefs.currency,
            isAddAccountOpen = isAddOpen,
            editingAccount = editingAcc
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState()
    )

    fun openAddAccount() {
        _editingAccount.value = null
        _isAddAccountOpen.value = true
    }

    fun openEditAccount(account: AccountEntity) {
        _editingAccount.value = account
        _isAddAccountOpen.value = true
    }

    fun closeAccountDialog() {
        _editingAccount.value = null
        _isAddAccountOpen.value = false
    }

    fun saveAccount(
        name: String,
        type: AccountType,
        openingBalance: Double,
        currency: String,
        colorHex: String,
        iconName: String,
        creditLimit: Double
    ) {
        viewModelScope.launch {
            val existing = _editingAccount.value
            val account = AccountEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                openingBalance = openingBalance,
                currency = currency,
                colorHex = colorHex,
                iconName = iconName,
                creditLimit = creditLimit,
                isActive = existing?.isActive ?: true,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveAccount(account)
            closeAccountDialog()
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccountById(accountId)
            closeAccountDialog()
        }
    }

    fun toggleAccountActive(account: AccountEntity) {
        viewModelScope.launch {
            repository.saveAccount(account.copy(isActive = !account.isActive))
        }
    }
}
