package com.sarisari.pos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sarisari.pos.AppContainer
import com.sarisari.pos.ui.components.AppIcons
import com.sarisari.pos.ui.inventory.InventoryScreen
import com.sarisari.pos.ui.assistant.AssistantScreen
import com.sarisari.pos.ui.pos.PosScreen
import com.sarisari.pos.ui.reports.ReportsScreen
import com.sarisari.pos.ui.settings.SettingsScreen
import com.sarisari.pos.ui.utang.UtangScreen

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val destinations = listOf(
    Destination("pos", "POS", AppIcons.Cash),
    Destination("utang", "Utang", AppIcons.Ledger),
    Destination("inventory", "Stock", AppIcons.Inventory),
    Destination("reports", "Reports", AppIcons.BarChart),
    Destination("settings", "Settings", AppIcons.Qr),
    Destination("assistant", "Ask", AppIcons.Assistant)
)

@Composable
fun SariSariNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val navEntry = navController.currentBackStackEntryAsState().value
    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { item ->
                    val selected = navEntry?.destination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = "pos", modifier = Modifier.padding(innerPadding)) {
            composable("pos") { PosScreen(container) }
            composable("utang") { UtangScreen(container) }
            composable("inventory") { InventoryScreen(container) }
            composable("reports") { ReportsScreen(container) }
            composable("settings") { SettingsScreen(container) }
            composable("assistant") { AssistantScreen(container) }
        }
    }
}
