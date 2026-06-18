package com.ktmb.crowdtrend.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Train
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Forecast : Screen("forecast?origin={origin}&dest={dest}", "Forecast", Icons.Filled.Leaderboard) {
        const val ROUTE_PATTERN = "forecast"
        const val ARG_ORIGIN = "origin"
        const val ARG_DEST = "dest"
    }
    data object Live : Screen("live", "Live", Icons.Filled.Map)
    data object Stations : Screen("stations", "Stations", Icons.Filled.Train)
    data object Settings : Screen("settings", "Info", Icons.Filled.Info)

    companion object {
        val bottomNavItems = listOf(Forecast, Live, Stations, Settings)
    }
}
