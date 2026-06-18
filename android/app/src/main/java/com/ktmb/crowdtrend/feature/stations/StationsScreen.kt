package com.ktmb.crowdtrend.feature.stations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktmb.crowdtrend.core.model.Station
import com.ktmb.crowdtrend.core.ui.components.*

@Composable
fun StationsScreen(
    vm: StationsViewModel = viewModel(),
    onUseAsOrigin: ((String) -> Unit)? = null,
    onUseAsDestination: ((String) -> Unit)? = null,
) {
    val uiState by vm.uiState.collectAsState()
    val grouped by vm.groupedStations.collectAsState()

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(8.dp))
        SectionHeader(kicker = "Stations", title = "Search the network")

        Spacer(Modifier.height(10.dp))

        // ── Search field ──
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { vm.onSearch(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search station or state") },
            leadingIcon = { Icon(Icons.Filled.Search, "Search") },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { vm.onSearch("") }) {
                        Icon(Icons.Filled.Close, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        Spacer(Modifier.height(10.dp))

        // ── Loading / Error states ──
        when {
            uiState.isLoading -> LoadingState("Loading stations...")
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { /* reload triggered by service flow */ },
            )
            else -> {
                // ── Selected station detail card ──
                uiState.selectedStation?.let { station ->
                    StationDetailCard(
                        station = station,
                        onUseAsOrigin = {
                            onUseAsOrigin?.invoke(station.name)
                            vm.clearSelection()
                        },
                        onUseAsDestination = {
                            onUseAsDestination?.invoke(station.name)
                            vm.clearSelection()
                        },
                        onDismiss = { vm.clearSelection() },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // ── Station count ──
                Text(
                    "${uiState.stations.size} stations",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )

                Spacer(Modifier.height(6.dp))

                // ── Grouped station list ──
                if (grouped.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                    EmptyState(
                        title = "No stations found",
                        subtitle = "Try a different search term.",
                    )
                } else {
                    LazyColumn {
                        grouped.forEach { group ->
                            item {
                                Text(
                                    group.state,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                )
                            }
                            items(group.stations) { station ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.onSelectStation(station) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        station.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "→",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                )
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// Station Detail Card
// ══════════════════════════════════════════════

@Composable
private fun StationDetailCard(
    station: Station,
    onUseAsOrigin: () -> Unit,
    onUseAsDestination: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        station.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        station.state,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, "Close", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onUseAsOrigin,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Use as origin", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onUseAsDestination,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Use as destination", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
