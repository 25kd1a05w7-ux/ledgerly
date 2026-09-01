package com.example.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.data.backup.BackupManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.presentation.accounts.AccountsScreen
import com.example.presentation.accounts.AccountsViewModel
import com.example.presentation.add.AddTransactionScreen
import com.example.presentation.add.AddTransactionViewModel
import com.example.presentation.budgets.BudgetsScreen
import com.example.presentation.budgets.BudgetsViewModel
import com.example.presentation.calendar.CalendarScreen
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.statistics.StatisticsScreen
import com.example.presentation.statistics.StatisticsViewModel
import com.example.presentation.transactions.TransactionDetailScreen
import com.example.presentation.transactions.TransactionsScreen
import com.example.presentation.transactions.TransactionsViewModel
import com.example.ui.theme.DividerLight
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.IncomeGreenBgLight
import com.example.ui.theme.TextSecondaryLight

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Overview", Icons.Outlined.Dashboard, Icons.Outlined.Dashboard)
    object Transactions : Screen("transactions", "Activity", Icons.Outlined.ReceiptLong, Icons.Outlined.ReceiptLong)
    object Accounts : Screen("accounts", "Accounts", Icons.Outlined.AccountBalance, Icons.Outlined.AccountBalance)
    object Budgets : Screen("budgets", "Budgets", Icons.Outlined.PieChart, Icons.Outlined.PieChart)
    object Statistics : Screen("statistics", "Analytics", Icons.Outlined.BarChart, Icons.Outlined.BarChart)

    // Sub-screens
    object AddTransaction : Screen("add_transaction?type={type}&accountId={accountId}&categoryId={categoryId}", "Add Transaction", Icons.Filled.Add, Icons.Filled.Add) {
        fun createRoute(type: String? = null, accountId: String? = null, categoryId: String? = null): String {
            val params = mutableListOf<String>()
            if (type != null) params.add("type=$type")
            if (accountId != null) params.add("accountId=$accountId")
            if (categoryId != null) params.add("categoryId=$categoryId")
            return if (params.isEmpty()) "add_transaction" else "add_transaction?${params.joinToString("&")}"
        }
    }

    object EditTransaction : Screen("edit_transaction/{transactionId}", "Edit Transaction", Icons.Filled.Add, Icons.Filled.Add) {
        fun createRoute(transactionId: String) = "edit_transaction/$transactionId"
    }

    object TransactionDetail : Screen("transaction_detail/{transactionId}", "Details", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong) {
        fun createRoute(transactionId: String) = "transaction_detail/$transactionId"
    }

    object Calendar : Screen("calendar", "Calendar", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Settings : Screen("settings", "Settings", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
}

val BottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Accounts,
    Screen.Budgets,
    Screen.Statistics
)

@Composable
fun MainAppScaffold(
    navController: NavHostController,
    repository: FinanceRepository,
    preferencesRepository: UserPreferencesRepository,
    backupManager: BackupManager,
    dashboardViewModel: DashboardViewModel,
    transactionsViewModel: TransactionsViewModel,
    accountsViewModel: AccountsViewModel,
    budgetsViewModel: BudgetsViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    addTransactionViewModel: AddTransactionViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = BottomNavScreens.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevelDestination,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                // Floating, Rounded Minimalist Apple-style Navigation Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val activeColor = EmeraldDark
                            val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Dashboard.route) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("nav_tab_${screen.route}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) IncomeGreenBgLight else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) activeColor else inactiveColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = screen.title,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) activeColor else inactiveColor
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAdd = { type, accountId, categoryId ->
                        addTransactionViewModel.initForNew(type, accountId, categoryId)
                        navController.navigate(Screen.AddTransaction.createRoute(type, accountId, categoryId))
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onNavigateToTransactionDetail = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    },
                    onNavigateToBudgets = {
                        navController.navigate(Screen.Budgets.route)
                    },
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts.route)
                    },
                    onNavigateToStatistics = {
                        navController.navigate(Screen.Statistics.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToCalendar = {
                        navController.navigate(Screen.Calendar.route)
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onNavigateToAdd = {
                        addTransactionViewModel.initForNew(null, null, null)
                        navController.navigate(Screen.AddTransaction.createRoute())
                    },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    }
                )
            }

            composable(Screen.Accounts.route) {
                AccountsScreen(
                    viewModel = accountsViewModel,
                    onNavigateToAddTransactionForAccount = { accId ->
                        addTransactionViewModel.initForNew(null, accId, null)
                        navController.navigate(Screen.AddTransaction.createRoute(accountId = accId))
                    }
                )
            }

            composable(Screen.Budgets.route) {
                BudgetsScreen(
                    viewModel = budgetsViewModel
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    viewModel = statisticsViewModel
                )
            }

            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; nullable = true },
                    navArgument("accountId") { type = NavType.StringType; nullable = true },
                    navArgument("categoryId") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type")
                val accountId = backStackEntry.arguments?.getString("accountId")
                val categoryId = backStackEntry.arguments?.getString("categoryId")

                AddTransactionScreen(
                    viewModel = addTransactionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: return@composable
                addTransactionViewModel.initForEdit(transactionId)
                AddTransactionScreen(
                    viewModel = addTransactionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: return@composable
                TransactionDetailScreen(
                    transactionId = transactionId,
                    repository = repository,
                    preferencesRepository = preferencesRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onEditTransaction = { id ->
                        navController.navigate(Screen.EditTransaction.createRoute(id))
                    }
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(
                    repository = repository,
                    preferencesRepository = preferencesRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onTransactionClick = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
