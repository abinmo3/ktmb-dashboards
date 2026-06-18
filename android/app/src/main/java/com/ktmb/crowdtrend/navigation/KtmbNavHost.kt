package com.ktmb.crowdtrend.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ktmb.crowdtrend.feature.alarms.AlarmsScreen
import com.ktmb.crowdtrend.feature.forecast.ForecastScreen
import com.ktmb.crowdtrend.feature.home.HomeScreen
import com.ktmb.crowdtrend.feature.live.LiveScreen
import com.ktmb.crowdtrend.feature.settings.SettingsScreen
import com.ktmb.crowdtrend.feature.stations.StationsScreen

@Composable
fun KtmbNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = Dp(0f),
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route?.startsWith(screen.route.substringBefore("?")) == true
                    } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = {
                            Text(
                                screen.label,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route.substringBefore("?")) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Home Dashboard
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToForecast = { origin, dest ->
                        navController.navigate("forecast?origin=$origin&dest=$dest") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLive = {
                        navController.navigate(Screen.Live.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAlarms = {
                        navController.navigate(Screen.Alarms.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            // Forecast — accepts optional origin/dest arguments
            composable(
                route = "forecast?origin={origin}&dest={dest}",
                arguments = listOf(
                    navArgument("origin") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                    navArgument("dest") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { backStackEntry ->
                val originArg = backStackEntry.arguments?.getString("origin") ?: ""
                val destArg = backStackEntry.arguments?.getString("dest") ?: ""
                ForecastScreen(
                    prefilledOrigin = originArg,
                    prefilledDestination = destArg,
                )
            }

            composable(Screen.Live.route) { LiveScreen() }

            composable(Screen.Stations.route) { StationsScreen(
                onUseAsOrigin = { stationName ->
                    navController.navigate("forecast?origin=$stationName&dest=") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                },
                onUseAsDestination = { stationName ->
                    navController.navigate("forecast?origin=&dest=$stationName") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                },
            ) }

            // Transit Alarms
            composable(Screen.Alarms.route) { AlarmsScreen() }

            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
