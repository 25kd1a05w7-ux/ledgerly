package com.example.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val statusMessage: String? = null,
    val shareIntent: Intent? = null
)

class SettingsViewModel(
    private val repository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val preferencesFlow: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _shareIntent = MutableStateFlow<Intent?>(null)
    val shareIntent: StateFlow<Intent?> = _shareIntent.asStateFlow()

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            preferencesRepository.updateCurrency(currency)
            _statusMessage.value = "Currency updated to $currency"
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    fun updateStartDayOfMonth(day: Int) {
        viewModelScope.launch {
            preferencesRepository.updateStartDayOfMonth(day)
            _statusMessage.value = "Month start day set to $day"
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAppLockEnabled(enabled)
        }
    }

    fun toggleHideSensitiveBalances(hide: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateHideSensitiveBalances(hide)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearShareIntent() {
        _shareIntent.value = null
    }

    fun exportJsonBackup() {
        viewModelScope.launch {
            try {
                val json = backupManager.generateJsonBackup()
                val fileName = "Ledgerly_Backup_${System.currentTimeMillis()}.json"
                val intent = backupManager.shareExportFile(json, fileName, "application/json")
                _shareIntent.value = intent
                _statusMessage.value = "JSON Backup ready to share"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            try {
                val csv = backupManager.generateCsvExport()
                val fileName = "Ledgerly_Transactions_${System.currentTimeMillis()}.csv"
                val intent = backupManager.shareExportFile(csv, fileName, "text/csv")
                _shareIntent.value = intent
                _statusMessage.value = "CSV Export ready to share"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun restoreJsonBackup(jsonString: String, replace: Boolean) {
        viewModelScope.launch {
            val result = backupManager.restoreFromJson(jsonString, replace)
            result.onSuccess { count ->
                _statusMessage.value = "Successfully restored $count transactions!"
            }.onFailure { err ->
                _statusMessage.value = "Restore failed: ${err.message}"
            }
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
            _statusMessage.value = "Demo financial data populated!"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _statusMessage.value = "All application data cleared."
        }
    }
}
