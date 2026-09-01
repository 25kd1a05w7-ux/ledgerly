package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.AppThemeMode
import com.example.presentation.accounts.AccountsViewModel
import com.example.presentation.add.AddTransactionViewModel
import com.example.presentation.budgets.BudgetsViewModel
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.navigation.MainAppScaffold
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.statistics.StatisticsViewModel
import com.example.presentation.transactions.TransactionsViewModel
import com.example.ui.theme.LedgerlyTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { FinanceRepository(database) }
    private val preferencesRepository by lazy { UserPreferencesRepository(applicationContext) }
    private val backupManager by lazy { BackupManager(applicationContext, database) }

    private val dashboardViewModel by viewModels<DashboardViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(repository, preferencesRepository) as T
            }
        }
    }

    private val transactionsViewModel by viewModels<TransactionsViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TransactionsViewModel(repository, preferencesRepository) as T
            }
        }
    }

    private val accountsViewModel by viewModels<AccountsViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountsViewModel(repository, preferencesRepository) as T
            }
        }
    }

    private val budgetsViewModel by viewModels<BudgetsViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetsViewModel(repository, preferencesRepository) as T
            }
        }
    }

    private val statisticsViewModel by viewModels<StatisticsViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StatisticsViewModel(repository, preferencesRepository) as T
            }
        }
    }

    private val settingsViewModel by viewModels<SettingsViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(repository, preferencesRepository, backupManager) as T
            }
        }
    }

    private val addTransactionViewModel by viewModels<AddTransactionViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddTransactionViewModel(repository, preferencesRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by preferencesRepository.userPreferencesFlow.collectAsState(
                initial = com.example.data.preferences.UserPreferences()
            )

            LedgerlyTheme(themeMode = prefs.themeMode) {
                val navController = rememberNavController()
                MainAppScaffold(
                    navController = navController,
                    repository = repository,
                    preferencesRepository = preferencesRepository,
                    backupManager = backupManager,
                    dashboardViewModel = dashboardViewModel,
                    transactionsViewModel = transactionsViewModel,
                    accountsViewModel = accountsViewModel,
                    budgetsViewModel = budgetsViewModel,
                    statisticsViewModel = statisticsViewModel,
                    settingsViewModel = settingsViewModel,
                    addTransactionViewModel = addTransactionViewModel
                )
            }
        }
    }
}
