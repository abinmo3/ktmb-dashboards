package com.ktmb.crowdtrend.feature.live

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktmb.crowdtrend.core.model.LiveFreshness
import com.ktmb.crowdtrend.core.model.LiveVehicle
import com.ktmb.crowdtrend.core.ui.components.*
import com.ktmb.crowdtrend.ui.theme.*

@Composable
fun LiveScreen(vm: LiveViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val summary = state.summary

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        SectionHeader(kicker = "Live feed", title = "Recent vehicle activity")

        Spacer(Modifier.height(10.dp))

        // ── Freshness indicator ──
        FreshnessBar(summary.freshness, state.lastFetchTime)

        Spacer(Modifier.height(10.dp))

        // ── Summary stats row ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("Vehicles detected", "${summary.vehicleCount}")
            StatItem("Activity level", summary.coverage)
            StatItem("Last updated", if (state.lastFetchTime.isNotEmpty()) state.lastFetchTime else "—")
        }

        Spacer(Modifier.height(12.dp))

        // ── Manual refresh ──
        OutlinedButton(
            onClick = { vm.manualRefresh() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            enabled = !state.isLoading,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(if (state.isLoading) "Refreshing..." else "Refresh")
        }

        Spacer(Modifier.height(14.dp))

        // ── Loading state ──
        if (state.isLoading && summary.vehicles.isEmpty()) {
            LoadingState("Contacting live feed...")
        }

        // ── Network error (UNAVAILABLE, never fetched) ──
        if (state.error != null && summary.freshness == LiveFreshness.UNAVAILABLE) {
            Spacer(Modifier.height(8.dp))
            ErrorState(
                message = state.error!!,
                onRetry = { vm.manualRefresh() },
            )
        }

        // ── Stale / Expired notice ──
        if (summary.freshness == LiveFreshness.STALE || summary.freshness == LiveFreshness.EXPIRED) {
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AmberWarning.copy(alpha = 0.12f),
                ),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (summary.freshness) {
                            LiveFreshness.STALE -> "⚠ Live feed appears stale. Last data may be a few minutes old."
                            LiveFreshness.EXPIRED -> "⚠ Live feed has not updated recently. Data shown may be outdated."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning,
                    )
                }
            }
        }

        // ── Vehicle list card ──
        if (summary.vehicles.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Recent vehicle snapshots", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${summary.vehicles.size} shown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    summary.vehicles.take(20).forEachIndexed { i, v ->
                        VehicleRow(v)
                        if (i < summary.vehicles.take(20).lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // ── Empty: successful fetch but no vehicles ──
        if (!state.isLoading && state.error == null && summary.vehicles.isEmpty() && summary.freshness == LiveFreshness.FRESH) {
            Spacer(Modifier.height(12.dp))
            EmptyState(
                title = "No vehicles detected",
                subtitle = "The live feed is active but no vehicles were reported in the last update. This may happen during off-peak hours.",
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Advisory note ──
        AdvisoryDisclaimer(
            "Vehicle positions from GTFS-realtime via data.gov.my. " +
            "This shows recent activity detected in the feed — not live arrivals, service guarantees, or official KTM schedules. " +
            "Refreshes every 30 seconds."
        )

        Spacer(Modifier.height(80.dp))
    }
}

// ═══════════════════════════════════════════
// Freshness bar
// ═══════════════════════════════════════════

@Composable
private fun FreshnessBar(freshness: LiveFreshness, time: String) {
    val (color, label) = when (freshness) {
        LiveFreshness.FRESH -> CrowdLow to "Recent vehicle activity detected"
        LiveFreshness.STALE -> AmberWarning to "Live feed appears stale"
        LiveFreshness.EXPIRED -> CrowdPacked to "Live feed may be delayed"
        LiveFreshness.UNAVAILABLE -> Color(0xFF9E9E9E) to "Live feed unavailable"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(Modifier.size(8.dp), shape = RoundedCornerShape(4.dp), color = color) {}
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
                if (time.isNotEmpty()) {
                    Text("Last update: $time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Stat item
// ═══════════════════════════════════════════

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

// ═══════════════════════════════════════════
// Vehicle row
// ═══════════════════════════════════════════

@Composable
private fun VehicleRow(v: LiveVehicle) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                v.vehicleId.ifEmpty { "—" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "%.4f, %.4f".format(v.latitude, v.longitude),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "%.0f km/h".format(v.speed * 3.6f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${v.bearing.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
