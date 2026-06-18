package com.ktmb.crowdtrend.feature.settings

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
import com.ktmb.crowdtrend.core.model.RidershipFreshness
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.ui.components.*
import com.ktmb.crowdtrend.ui.theme.*

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val activeService by vm.activeService.collectAsState()
    val state by vm.uiState.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Service switch ──
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader(kicker = "Service", title = "Active line")
                Spacer(Modifier.height(10.dp))
                ServiceSwitcher(
                    selected = activeService,
                    onSelect = { vm.setService(it) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Data freshness ──
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(kicker = "Data", title = "Freshness & sources")
                    IconButton(onClick = { vm.refresh() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (state.isLoading) {
                    LoadingState("Checking sources...")
                } else {
                    // ── Realtime trains ──
                    SourceFreshnessRow(
                        sourceName = "Realtime trains",
                        status = feedStateLabel(state.liveFeedState),
                        detail = feedStateDescription(state.liveFeedState),
                        isHealthy = state.liveFeedState == FeedState.LIVE_ACTIVE || state.liveFeedState == FeedState.LIVE_EMPTY,
                    )

                    // ── Ridership ──
                    val rs = state.ridershipStatus
                    SourceFreshnessRow(
                        sourceName = "Ridership data",
                        status = rs.freshness.label,
                        detail = if (rs.latestDate.isNotEmpty())
                            "Latest: $rs.latestDate · ${rs.rowCount} rows · ${rs.source}"
                        else
                            "No data available · ${rs.source}",
                        isHealthy = rs.freshness == RidershipFreshness.FRESH,
                    )

                    // ── Forecast base ──
                    val meta = state.forecastMeta
                    SourceFreshnessRow(
                        sourceName = "Forecast base",
                        status = if (meta != null) "Available" else "Unavailable",
                        detail = if (meta != null && meta.latestDate.isNotEmpty())
                            "Latest: ${meta.latestDate} · ${meta.daysAvailable} days · ${meta.source}"
                        else
                            "No metadata loaded",
                        isHealthy = meta != null,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Attribution ──
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader(kicker = "Attribution", title = "Data sources")
                Spacer(Modifier.height(8.dp))
                Text("Ridership: data.gov.my (Malaysia Open Data)", style = MaterialTheme.typography.bodySmall)
                Text("Live positions: GTFS-realtime via data.gov.my", style = MaterialTheme.typography.bodySmall)
                Text("Not affiliated with KTM Berhad.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(12.dp))

        AdvisoryDisclaimer(
            "Forecasts use historical ridership ratios, not live counts. " +
            "Use for planning guidance only. Crowd levels may vary on the day."
        )

        Spacer(Modifier.height(80.dp))
    }
}

// ═══════════════════════════════════════════
// Source freshness row
// ═══════════════════════════════════════════

@Composable
private fun SourceFreshnessRow(
    sourceName: String,
    status: String,
    detail: String,
    isHealthy: Boolean,
) {
    val dotColor = if (isHealthy) CrowdLow else AmberWarning

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = dotColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(Modifier.size(8.dp), shape = RoundedCornerShape(4.dp), color = dotColor) {}
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sourceName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = dotColor.copy(alpha = 0.15f),
            ) {
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = dotColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// Feed state helpers
// ═══════════════════════════════════════════

private fun feedStateLabel(state: FeedState): String = when (state) {
    FeedState.LIVE_ACTIVE -> "Live"
    FeedState.LIVE_EMPTY -> "Empty"
    FeedState.FEED_ERROR -> "Error"
    FeedState.STALE -> "Stale"
}

private fun feedStateDescription(state: FeedState): String = when (state) {
    FeedState.LIVE_ACTIVE -> "Active vehicles reported by upstream API"
    FeedState.LIVE_EMPTY -> "Feed reachable but no vehicles currently reported"
    FeedState.FEED_ERROR -> "Feed is currently unavailable"
    FeedState.STALE -> "Last vehicle data is older than 2 minutes"
}
