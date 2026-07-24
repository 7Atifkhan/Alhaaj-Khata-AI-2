package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.data.remote.AuthRepository
import com.example.data.remote.AuthResult
import com.example.data.remote.ProfileRepository
import com.example.ui.screens.BusinessProfileSetupScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.auth.EmailVerificationScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.auth.ResetPasswordScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.auth.WelcomeScreen

import com.example.sync.NetworkMonitor
import com.example.sync.SyncEngine
import com.example.ui.screens.ActiveDevicesScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    authRepository: AuthRepository,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser by authRepository.currentUser.collectAsState()
    var lastRegisteredEmail by remember { mutableStateOf("") }
    val profileRepository = remember { ProfileRepository() }

    val db = remember { com.example.data.local.KhataDatabase.getDatabase(context) }
    val networkMonitor = remember { NetworkMonitor(context) }
    val customerRepo = remember { com.example.data.remote.CustomerRepository(db.customerDao()) }
    val transactionRepo = remember { com.example.data.remote.TransactionRepository(db.transactionDao(), db.customerDao()) }
    val settingsRepo = remember { com.example.data.remote.SettingsRepository() }

    val syncEngine = remember {
        SyncEngine(
            context = context,
            database = db,
            customerRepository = customerRepo,
            transactionRepository = transactionRepo,
            profileRepository = profileRepository,
            settingsRepository = settingsRepo,
            authRepository = authRepository,
            networkMonitor = networkMonitor
        )
    }

    val syncState by syncEngine.syncState.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val checkAuthAndNavigate: (String) -> Unit = { targetRoute ->
        if (authRepository.isLoggedIn()) {
            navController.navigate(targetRoute) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(NavItem.ROUTE_LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavItem.ROUTE_SPLASH,
        modifier = modifier
    ) {
        // 1. Splash Screen
        composable(NavItem.ROUTE_SPLASH) {
            SplashScreen(
                isLoggedIn = authRepository.isLoggedIn(),
                onNavigateToNext = { isLoggedIn ->
                    val dest = if (isLoggedIn) NavItem.Dashboard.route else NavItem.ROUTE_WELCOME
                    navController.navigate(dest) {
                        popUpTo(NavItem.ROUTE_SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // 2. Welcome Screen
        composable(NavItem.ROUTE_WELCOME) {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(NavItem.ROUTE_LOGIN)
                },
                onNavigateToRegister = {
                    navController.navigate(NavItem.ROUTE_REGISTER)
                }
            )
        }

        // 3. Login Screen
        composable(NavItem.ROUTE_LOGIN) {
            LoginScreen(
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate(NavItem.Dashboard.route) {
                        popUpTo(NavItem.ROUTE_WELCOME) { inclusive = true }
                        popUpTo(NavItem.ROUTE_LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NavItem.ROUTE_REGISTER)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(NavItem.ROUTE_FORGOT_PASSWORD)
                }
            )
        }

        // 4. Register Screen
        composable(NavItem.ROUTE_REGISTER) {
            RegisterScreen(
                authRepository = authRepository,
                onRegisterSuccess = { registeredEmail ->
                    lastRegisteredEmail = registeredEmail
                    navController.navigate(NavItem.ROUTE_EMAIL_VERIFICATION)
                },
                onNavigateToLogin = {
                    navController.navigate(NavItem.ROUTE_LOGIN)
                }
            )
        }

        // 5. Forgot Password Screen
        composable(NavItem.ROUTE_FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                authRepository = authRepository,
                onNavigateToLogin = {
                    navController.navigate(NavItem.ROUTE_LOGIN)
                }
            )
        }

        // 6. Reset Password Screen
        composable(NavItem.ROUTE_RESET_PASSWORD) {
            ResetPasswordScreen(
                authRepository = authRepository,
                onNavigateToLogin = {
                    navController.navigate(NavItem.ROUTE_LOGIN)
                }
            )
        }

        // 7. Email Verification Screen
        composable(NavItem.ROUTE_EMAIL_VERIFICATION) {
            EmailVerificationScreen(
                email = lastRegisteredEmail,
                authRepository = authRepository,
                onNavigateToLogin = {
                    navController.navigate(NavItem.ROUTE_LOGIN) {
                        popUpTo(NavItem.ROUTE_REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // 8. Business Profile Setup Screen
        composable(NavItem.ROUTE_BUSINESS_PROFILE_SETUP) {
            BusinessProfileSetupScreen(
                authRepository = authRepository,
                profileRepository = profileRepository,
                onProfileSetupComplete = {
                    navController.navigate(NavItem.Dashboard.route) {
                        popUpTo(NavItem.ROUTE_BUSINESS_PROFILE_SETUP) { inclusive = true }
                    }
                }
            )
        }

        // --- PROTECTED MAIN ROUTES ---

        // Dashboard
        composable(NavItem.Dashboard.route) {
            if (!authRepository.isLoggedIn()) {
                checkAuthAndNavigate(NavItem.Dashboard.route)
            } else {
                val user = currentUser
                var checkedProfile by remember { mutableStateOf(false) }

                LaunchedEffect(user?.id) {
                    if (user != null && !checkedProfile) {
                        checkedProfile = true
                        val profileResult = profileRepository.getProfile(user.id, user.accessToken)
                        if (profileResult is AuthResult.Success && profileResult.data == null) {
                            navController.navigate(NavItem.ROUTE_BUSINESS_PROFILE_SETUP) {
                                launchSingleTop = true
                            }
                        }
                    }
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                val db = remember { com.example.data.local.KhataDatabase.getDatabase(context) }
                val transactionRepo = remember { com.example.data.remote.TransactionRepository(db.transactionDao(), db.customerDao()) }
                val customerRepo = remember { com.example.data.remote.CustomerRepository(db.customerDao()) }
                val dashboardViewModel = remember(authRepository.currentUser.value?.id) {
                    com.example.ui.viewmodels.DashboardViewModel(transactionRepo, customerRepo, authRepository)
                }

                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToCustomers = { checkAuthAndNavigate(NavItem.Customers.route) },
                    onNavigateToTransactions = { checkAuthAndNavigate(NavItem.Transactions.route) },
                    syncState = syncState,
                    onSyncNow = {
                        scope.launch {
                            syncEngine.triggerFullSync()
                        }
                    }
                )
            }
        }

        // Customers
        composable(NavItem.Customers.route) {
            if (!authRepository.isLoggedIn()) {
                checkAuthAndNavigate(NavItem.Customers.route)
            } else {
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = remember { com.example.data.local.KhataDatabase.getDatabase(context) }
                val customerRepository = remember { com.example.data.remote.CustomerRepository(db.customerDao()) }
                val transactionRepository = remember { com.example.data.remote.TransactionRepository(db.transactionDao(), db.customerDao()) }
                val viewModel = remember(authRepository.currentUser.value?.id) {
                    com.example.ui.viewmodels.CustomersViewModel(customerRepository, authRepository, transactionRepository)
                }
                CustomersScreen(viewModel = viewModel)
            }
        }

        // Transactions
        composable(NavItem.Transactions.route) {
            if (!authRepository.isLoggedIn()) {
                checkAuthAndNavigate(NavItem.Transactions.route)
            } else {
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = remember { com.example.data.local.KhataDatabase.getDatabase(context) }
                val transactionRepository = remember { com.example.data.remote.TransactionRepository(db.transactionDao(), db.customerDao()) }
                val customerRepository = remember { com.example.data.remote.CustomerRepository(db.customerDao()) }
                val viewModel = remember(authRepository.currentUser.value?.id) {
                    com.example.ui.viewmodels.TransactionsViewModel(transactionRepository, customerRepository, authRepository)
                }
                TransactionsScreen(viewModel = viewModel)
            }
        }


        // Reports
        composable(NavItem.Reports.route) {
            if (!authRepository.isLoggedIn()) {
                checkAuthAndNavigate(NavItem.Reports.route)
            } else {
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = remember { com.example.data.local.KhataDatabase.getDatabase(context) }
                val transactionRepository = remember { com.example.data.remote.TransactionRepository(db.transactionDao(), db.customerDao()) }
                val customerRepository = remember { com.example.data.remote.CustomerRepository(db.customerDao()) }
                val reportsViewModel = remember(authRepository.currentUser.value?.id) {
                    com.example.ui.viewmodels.ReportsViewModel(transactionRepository, customerRepository, authRepository)
                }
                ReportsScreen(viewModel = reportsViewModel)
            }
        }

        // Settings
        composable(NavItem.Settings.route) {
            if (!authRepository.isLoggedIn()) {
                checkAuthAndNavigate(NavItem.Settings.route)
            } else {
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = remember { com.example.data.local.KhataDatabase.getDatabase(context) }
                val profileRepo = remember { com.example.data.remote.ProfileRepository() }
                val settingsRepo = remember { com.example.data.remote.SettingsRepository() }
                val transactionRepo = remember { com.example.data.remote.TransactionRepository(db.transactionDao(), db.customerDao()) }
                val customerRepo = remember { com.example.data.remote.CustomerRepository(db.customerDao()) }
                val settingsViewModel = remember(authRepository.currentUser.value?.id) {
                    com.example.ui.viewmodels.SettingsViewModel(
                        authRepository = authRepository,
                        profileRepository = profileRepo,
                        settingsRepository = settingsRepo,
                        transactionRepository = transactionRepo,
                        customerRepository = customerRepo,
                        database = db,
                        context = context
                    )
                }
                SettingsScreen(
                    viewModel = settingsViewModel,
                    currentUser = currentUser,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onNavigateToActiveDevices = {
                        navController.navigate(NavItem.ROUTE_ACTIVE_DEVICES)
                    },
                    onLogout = {
                        authRepository.logout()
                        navController.navigate(NavItem.ROUTE_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Active Devices
        composable(NavItem.ROUTE_ACTIVE_DEVICES) {
            ActiveDevicesScreen(
                activeDevices = syncState.activeDevices,
                onRemoveDevice = { deviceId -> syncEngine.removeDeviceSession(deviceId) },
                onSignOutOtherDevices = { syncEngine.signOutOtherDevices() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
