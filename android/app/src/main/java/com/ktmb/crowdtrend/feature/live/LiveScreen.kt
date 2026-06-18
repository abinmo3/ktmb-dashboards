package com.ktmb.crowdtrend.feature.live

import androidx.compose.foundation.clickable
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
import com.ktmb.crowdtrend.core.model.FeedState
import com.ktmb.crowdtrend.core.model.LiveFreshness
import com.ktmb.crowdtrend.core.model.LiveVehicle
import com.ktmb.crowdtrend.core.ui.components.*
import com.ktmb.crowdtrend.data.repository.LiveRepository
import com.ktmb.crowdtrend.ui.theme.*

@Composable
fun LiveScreen(vm: LiveViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val summary = state.summary

    // Debug reveal: tap the section header title 5 times
    var debugTapCount by remember { mutableIntStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        // Tap-to-debug header
        Column(
            Modifier.clickable {
                debugTapCount++
                if (debugTapCount >= 5) {
                    showDebug = !showDebug
                    debugTapCount = 0
                }
            }
        ) {
            SectionHeader(kicker = "Live feed", title = "Vehicle activity")
        }

        Spacer(Modifier.height(10.dp))

        // ── Freshness indicator ──
        FreshnessBar(summary, state.isStaleCache)

        Spacer(Modifier.height(10.dp))

        // ── Feed-state message ──
        FeedStateMessage(summary)

        // ── Summary stats row ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("Vehicles", "${summary.vehicleCount}")
            StatItem("HTTP", "${summary.httpStatus}")
            StatItem("Last fetch", if (state.lastFetchTime.isNotEmpty()) state.lastFetchTime else "—")
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

        // ── Network error (UNAVAILABLE, never fetched, no cache) ──
        if (state.error != null && summary.freshness == LiveFreshness.UNAVAILABLE) {
            Spacer(Modifier.height(8.dp))
            ErrorState(
                message = state.error!!,
                onRetry = { vm.manualRefresh() },
            )
        }

        // ── Stale / Expired notice ──
        if (state.isStaleCache) {
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AmberWarning.copy(alpha = 0.12f),
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "⚠ Live feed is currently unavailable. Showing last known data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning,
                        fontWeight = FontWeight.Medium,
                    )
                    if (state.debugInfo.cacheAgeSeconds > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Cache age: ${formatAge(state.debugInfo.cacheAgeSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        } else if (summary.freshness == LiveFreshness.STALE || summary.freshness == LiveFreshness.EXPIRED) {
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
                        Text(
                            if (state.isStaleCache) "Cached vehicle snapshots" else "Recent vehicle snapshots",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${summary.vehicles.size} shown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    if (state.isStaleCache) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "⚠ Stale data",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning,
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

        // ── Empty: successful feed but no vehicles (LIVE_EMPTY) ──
        if (!state.isLoading && state.error == null
            && summary.vehicles.isEmpty()
            && summary.feedState == FeedState.LIVE_EMPTY
        ) {
            Spacer(Modifier.height(12.dp))
            EmptyState(
                title = "No vehicles reported",
                subtitle = "KTMB realtime feed is reachable, but no trains are currently reported by the upstream API.",
            )
        }

        // ── Debug section (revealed by tapping title 5×) ──
        if (showDebug) {
            Spacer(Modifier.height(14.dp))
            DebugCard(state)
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
// Feed-state message
// ═══════════════════════════════════════════

@Composable
private fun FeedStateMessage(summary: com.ktmb.crowdtrend.core.model.LiveSummary) {
    val message = when (summary.feedState) {
        FeedState.LIVE_ACTIVE -> {
            if (summary.vehicleCount > 0) {
                "Active vehicles reported — ${summary.vehicleCount} detected"
            } else {
                null // Don't claim active with 0 entities
            }
        }
        FeedState.LIVE_EMPTY -> null // handled by EmptyState below
        FeedState.FEED_ERROR -> "KTMB realtime feed is currently unavailable."
        FeedState.STALE -> "Showing last known data — feed may be unavailable."
    }

    if (message != null) {
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dotColor = when (summary.feedState) {
                FeedState.LIVE_ACTIVE -> CrowdLow
                FeedState.FEED_ERROR -> CrowdPacked
                FeedState.STALE -> AmberWarning
                else -> Color.Gray
            }
            Surface(Modifier.size(6.dp), shape = RoundedCornerShape(3.dp), color = dotColor) {}
            Spacer(Modifier.width(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ═══════════════════════════════════════════
// Freshness bar
// ═══════════════════════════════════════════

@Composable
private fun FreshnessBar(summary: com.ktmb.crowdtrend.core.model.LiveSummary, isStaleCache: Boolean) {
    val (color, label) = when {
        isStaleCache -> AmberWarning to "Showing cached data — feed unavailable"
        summary.feedState == FeedState.LIVE_ACTIVE && summary.vehicleCount > 0 -> CrowdLow to "Active vehicles reported"
        summary.feedState == FeedState.LIVE_EMPTY -> Color(0xFF9E9E9E) to "Feed reachable, no vehicles"
        summary.feedState == FeedState.FEED_ERROR -> CrowdPacked to "Feed unavailable"
        summary.freshness == LiveFreshness.FRESH -> CrowdLow to "Feed is live"
        summary.freshness == LiveFreshness.STALE -> AmberWarning to "Live feed appears stale"
        summary.freshness == LiveFreshness.EXPIRED -> CrowdPacked to "Live feed may be delayed"
        else -> Color(0xFF9E9E9E) to "Live feed unavailable"
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
                val lastVehicleTime = summary.lastVehicleTimestamp
                if (lastVehicleTime > 0) {
                    Text(
                        "Last vehicle: ${LiveRepository.formatTimestamp(lastVehicleTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Debug card (tap title 5× to reveal)
// ═══════════════════════════════════════════

@Composable
private fun DebugCard(state: LiveUiState) {
    val di = state.debugInfo
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("🐛 Debug Info", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            DebugRow("HTTP status", "${di.httpStatus}")
            DebugRow("Response bytes", "${di.responseBytes}")
            DebugRow("Entity count", "${di.entityCount}")
            DebugRow("Last fetch time", if (di.lastFetchTimeMs > 0) LiveRepository.formatTimestamp(di.lastFetchTimeMs) else "—")
            DebugRow("Last success time", if (di.lastSuccessTimeMs > 0) LiveRepository.formatTimestamp(di.lastSuccessTimeMs) else "—")
            DebugRow("Last vehicle timestamp", if (di.lastVehicleTimestamp > 0) LiveRepository.formatTimestamp(di.lastVehicleTimestamp) else "—")
            DebugRow("Cache age", formatAge(di.cacheAgeSeconds))
            DebugRow("Feed state", state.summary.feedState.name)
            DebugRow("Freshness", state.summary.freshness.name)
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

private fun formatAge(seconds: Long): String = when {
    seconds <= 0 -> "—"
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
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
