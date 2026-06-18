package com.ktmb.crowdtrend.feature.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktmb.crowdtrend.core.model.Station
import com.ktmb.crowdtrend.core.model.TransitAlarm
import com.ktmb.crowdtrend.core.ui.components.LoadingState
import com.ktmb.crowdtrend.core.ui.components.ErrorState
import com.ktmb.crowdtrend.data.repository.TransitAlarmRepository
import com.ktmb.crowdtrend.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    vm: AlarmsViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()
    val alarms by vm.alarms.collectAsState()

    var showStationSearch by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        // ── Header ──
        Text(
            "Transit Alarms",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Get notified when you're approaching your station",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        // ── Service status chip ──
        if (alarms.any { it.isActive }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Monitoring ${alarms.count { it.isActive }} station${if (alarms.count { it.isActive } != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        alarms.filter { it.isActive }.forEach { vm.toggleAlarm(it.id) }
                    }) {
                        Text("Stop All", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Location permission prompt ──
        if (!state.hasLocationPermission) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⚠", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Location permission required", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Enable GPS access for proximity alerts",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Add alarm button ──
        Button(
            onClick = { showStationSearch = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Alarm")
        }

        Spacer(Modifier.height(16.dp))

        // ── Station search sheet ──
        if (showStationSearch) {
            StationSearchSheet(
                query = state.searchQuery,
                results = state.searchResults,
                isSearchActive = state.isSearchActive,
                onQueryChanged = vm::onSearchQueryChanged,
                onStationSelected = { station ->
                    vm.createAlarm(station)
                    showStationSearch = false
                },
                onDismiss = {
                    vm.clearSearch()
                    showStationSearch = false
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── Existing alarms ──
        if (alarms.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No alarms yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Add an alarm for your destination or transfer station",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { vm.toggleAlarm(alarm.id) },
                        onDelete = { vm.removeAlarm(alarm.id) },
                        onRadiusChange = { vm.updateRadius(alarm.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StationSearchSheet(
    query: String,
    results: List<Station>,
    isSearchActive: Boolean,
    onQueryChanged: (String) -> Unit,
    onStationSelected: (Station) -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = { Text("Search station...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Results
            if (results.isEmpty() && isSearchActive) {
                Text(
                    "No stations found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                results.forEach { station ->
                    Surface(
                        onClick = { onStationSelected(station) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Train,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(station.name, fontWeight = FontWeight.Medium)
                                Text(
                                    station.state,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add alarm",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: TransitAlarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onRadiusChange: (Int) -> Unit,
) {
    var showRadiusOption by remember { mutableStateOf(false) }
    val radiusOptions = listOf(500, 1000, 2000)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = if (alarm.isActive) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (alarm.isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        alarm.stationName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alarm.isActive) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${alarm.radiusMeters}m radius",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                IconButton(onClick = { showRadiusOption = !showRadiusOption }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Radius", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (alarm.isActive) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        contentDescription = if (alarm.isActive) "Disable" else "Enable",
                        tint = if (alarm.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Radius selector
            if (showRadiusOption) {
                Spacer(Modifier.height(8.dp))
                Text("Alarm radius", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    radiusOptions.forEach { radius ->
                        FilterChip(
                            selected = alarm.radiusMeters == radius,
                            onClick = { onRadiusChange(radius) },
                            label = {
                                Text(
                                    if (radius < 1000) "${radius}m" else "${radius / 1000}km"
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
