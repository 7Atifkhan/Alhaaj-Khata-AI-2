package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    companion object {
        const val ROUTE_SPLASH = "splash"
        const val ROUTE_WELCOME = "welcome"
        const val ROUTE_LOGIN = "login"
        const val ROUTE_REGISTER = "register"
        const val ROUTE_FORGOT_PASSWORD = "forgot_password"
        const val ROUTE_RESET_PASSWORD = "reset_password"
        const val ROUTE_EMAIL_VERIFICATION = "email_verification"
        const val ROUTE_BUSINESS_PROFILE_SETUP = "business_profile_setup"
        const val ROUTE_ACTIVE_DEVICES = "active_devices"

        val items = listOf(
            Dashboard,
            Customers,
            Transactions,
            Reports,
            Settings
        )
    }

    object Dashboard : NavItem(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    )

    object Customers : NavItem(
        route = "customers",
        title = "Customers",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
        testTag = "nav_customers"
    )

    object Transactions : NavItem(
        route = "transactions",
        title = "Transactions",
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_transactions"
    )

    object Reports : NavItem(
        route = "reports",
        title = "Reports",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_reports"
    )

    object Settings : NavItem(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
}
