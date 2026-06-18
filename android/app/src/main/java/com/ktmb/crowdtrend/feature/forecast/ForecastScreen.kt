package com.ktmb.crowdtrend.feature.forecast

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktmb.crowdtrend.core.model.Forecast
import com.ktmb.crowdtrend.core.model.Station
import com.ktmb.crowdtrend.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    prefilledOrigin: String = "",
    prefilledDestination: String = "",
    vm: ForecastViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()

    // Apply nav args as initial selections (handled by ViewModel via loadFoundation)
    LaunchedEffect(prefilledOrigin, prefilledDestination) {
        if (prefilledOrigin.isNotEmpty()) {
            state.stations.firstOrNull { it.name == prefilledOrigin }?.let {
                vm.onOriginSelected(it)
            }
        }
        if (prefilledDestination.isNotEmpty()) {
            state.stations.firstOrNull { it.name == prefilledDestination }?.let {
                vm.onDestinationSelected(it)
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        // ── Service switch ──
        ServiceSwitcher(
            selected = vm.activeService.collectAsState().value,
            onSelect = { vm.setService(it) },
        )

        Spacer(Modifier.height(12.dp))

        // ── Route selection card ──
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                SectionHeader(kicker = "Crowd forecast", title = "Choose a route")

                Spacer(Modifier.height(10.dp))

                // Origin dropdown
                var originExp by remember { mutableStateOf(false) }
                Text("Origin", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                ExposedDropdownMenuBox(expanded = originExp, onExpandedChange = { originExp = it }) {
                    OutlinedTextField(
                        value = state.origin?.let { "${it.name} (${it.state})" } ?: "Select...",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(originExp) },
                        singleLine = true,
                    )
                    ExposedDropdownMenu(expanded = originExp, onDismissRequest = { originExp = false }) {
                        state.stations.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.name} (${s.state})") },
                                onClick = { vm.onOriginSelected(s); originExp = false },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Swap button
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilledIconButton(
                        onClick = { vm.swapRoute() },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(Icons.Filled.SwapHoriz, "Swap route")
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Destination dropdown
                var destExp by remember { mutableStateOf(false) }
                Text("Destination", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                ExposedDropdownMenuBox(expanded = destExp, onExpandedChange = { destExp = it }) {
                    OutlinedTextField(
                        value = state.destination?.let { "${it.name} (${it.state})" } ?: "Select...",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(destExp) },
                        singleLine = true,
                    )
                    ExposedDropdownMenu(expanded = destExp, onDismissRequest = { destExp = false }) {
                        if (state.availableDestinations.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Select an origin first") },
                                onClick = { destExp = false },
                                enabled = false,
                            )
                        } else {
                            state.availableDestinations.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.name} (${s.state})") },
                                    onClick = { vm.onDestinationSelected(s); destExp = false },
                                )
                            }
                        }
                    }
                }

                // Route summary
                if (state.origin != null && state.destination != null) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${state.origin!!.name} → ${state.destination!!.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (state.forecast != null) {
                            Text(
                                "Latest: ${state.forecast!!.latestDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Loading / Error / Forecast display ──
        when {
            state.isLoading -> LoadingState("Loading forecast...")

            state.error != null -> {
                ErrorState(
                    message = state.error!!,
                    onRetry = {
                        if (state.origin != null && state.destination != null) {
                            vm.onDestinationSelected(state.destination!!)
                        }
                    },
                )
            }

            state.forecast != null -> ForecastContent(
                forecast = state.forecast!!,
                mode = state.forecastMode,
                onModeChange = { vm.setForecastMode(it) },
                freshness = state.freshness,
            )

            state.origin != null && state.destination != null -> {
                // Both selected but no forecast (null = no data, not error)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    EmptyState(
                        title = "Route unavailable",
                        subtitle = "No historical data for ${state.origin!!.name} → ${state.destination!!.name}. Try another route.",
                    )
                }
            }

            else -> {
                // Nothing selected yet — show hint
                EmptyState(
                    title = "Select a route",
                    subtitle = "Choose origin and destination to see crowd forecasts.",
                )
            }
        }

        // Always show freshness + disclaimer at bottom
        if (state.freshness.latestDate.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            DataFreshnessLabel(
                latestDate = state.freshness.latestDate,
                daysBehind = state.freshness.daysBehind,
                isStale = state.freshness.isStale,
            )
        }

        Spacer(Modifier.height(8.dp))
        AdvisoryDisclaimer(
            "Forecast based on historical ridership ratios from data.gov.my. " +
            "Not live counts or official KTM data. Use for planning guidance only."
        )

        Spacer(Modifier.height(80.dp))
    }
}

// ═══════════════════════════════════════════
// Forecast Content (heatmap + windows + legend)
// ═══════════════════════════════════════════

@Composable
private fun ForecastContent(
    forecast: Forecast,
    mode: ForecastMode,
    onModeChange: (ForecastMode) -> Unit,
    freshness: com.ktmb.crowdtrend.core.model.DataFreshness,
) {
    val values = forecast.hourly.map { h ->
        when (mode) {
            ForecastMode.LATEST -> h.today ?: h.baseline730
            ForecastMode.TYPICAL -> h.baseline730
        }
    }

    // Compute best/avoid windows
    val bestHours = ForecastViewModel.bestWindows(values, 3)
    val avoidHours = ForecastViewModel.avoidWindows(values, 2)

    // ── Heatmap card ──
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("By hour", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = mode == ForecastMode.LATEST,
                        onClick = { onModeChange(ForecastMode.LATEST) },
                        label = { Text("Latest") },
                    )
                    FilterChip(
                        selected = mode == ForecastMode.TYPICAL,
                        onClick = { onModeChange(ForecastMode.TYPICAL) },
                        label = { Text("Typical") },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            CrowdHeatmap(values)

            Spacer(Modifier.height(8.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(
                    "Low" to com.ktmb.crowdtrend.ui.theme.CrowdLow,
                    "Mixed" to com.ktmb.crowdtrend.ui.theme.CrowdMid,
                    "Busy" to com.ktmb.crowdtrend.ui.theme.CrowdHigh,
                    "Packed" to com.ktmb.crowdtrend.ui.theme.CrowdPacked,
                ).forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(10.dp), shape = RoundedCornerShape(2.dp), color = color) {}
                        Spacer(Modifier.width(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // ── Best / Avoid windows ──
    if (bestHours.isNotEmpty() || avoidHours.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                label = "Best boarding",
                value = if (bestHours.isNotEmpty())
                    bestHours.joinToString(", ") { ForecastViewModel.formatHour(it) }
                else "—",
                note = "Lower density hours",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Avoid if possible",
                value = if (avoidHours.isNotEmpty())
                    avoidHours.joinToString(", ") { ForecastViewModel.formatHour(it) }
                else "—",
                note = "High pressure periods",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
