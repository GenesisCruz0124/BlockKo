package com.blockko.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blockko.app.ui.screens.about.AboutScreen
import com.blockko.app.ui.screens.apps.AppListScreen
import com.blockko.app.ui.screens.blocklist.BlocklistScreen
import com.blockko.app.ui.screens.home.HomeScreen
import com.blockko.app.ui.screens.onboarding.OnboardingScreen
import com.blockko.app.ui.screens.settings.SettingsScreen
import com.blockko.app.ui.screens.settings.SettingsViewModel

private fun destinationIcon(destination: Destination): ImageVector = when (destination) {
    Destination.Home -> Icons.Default.Home
    Destination.Apps -> Icons.Default.Apps
    Destination.Blocklist -> Icons.Default.Block
    Destination.Settings -> Icons.Default.Settings
    else -> Icons.Default.Home
}

private fun destinationLabel(destination: Destination): String = when (destination) {
    Destination.Home -> "Home"
    Destination.Apps -> "Apps"
    Destination.Blocklist -> "Blocklist"
    Destination.Settings -> "Settings"
    else -> ""
}

@Composable
fun BlockKoNavHost(
    onRequestEnableVpn: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val navController = rememberNavController()
    val settings by settingsViewModel.settings.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BOTTOM_NAV_DESTINATIONS.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BOTTOM_NAV_DESTINATIONS.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destinationIcon(destination), contentDescription = null) },
                            label = { Text(destinationLabel(destination)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Onboarding.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Onboarding.route) {
                LaunchedEffect(settings.onboardingCompleted) {
                    if (settings.onboardingCompleted) {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Onboarding.route) { inclusive = true }
                        }
                    }
                }
                OnboardingScreen(onEnableProtection = onRequestEnableVpn)
            }
            composable(Destination.Home.route) {
                HomeScreen(onRequestEnableVpn = onRequestEnableVpn)
            }
            composable(Destination.Apps.route) {
                AppListScreen()
            }
            composable(Destination.Blocklist.route) {
                BlocklistScreen()
            }
            composable(Destination.Settings.route) {
                SettingsScreen(onOpenAbout = { navController.navigate(Destination.About.route) })
            }
            composable(Destination.About.route) {
                AboutScreen()
            }
        }
    }
}
