package com.ktmb.crowdtrend.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktmb.crowdtrend.core.model.LiveFreshness
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.ui.components.*
import com.ktmb.crowdtrend.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    vm: HomeViewModel = viewModel(),
    onNavigateToForecast: (String, String) -> Unit = { _, _ -> },
    onNavigateToLive: () -> Unit = {},
    onNavigateToAlarms: () -> Unit = {},
) {
    val state by vm.uiState.collectAsState()
    val activeService by vm.activeService.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // ── Hero header ──
        Text(
            "KTMB Pulse",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Malaysia's Railway Network",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        // ── Service switch ──
        ServiceSwitcher(
            selected = activeService,
            onSelect = { vm.setService(it) },
        )

        Spacer(Modifier.height(14.dp))

        // ── KPI row ──
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricCard(
                label = "Stations",
                value = "${state.totalStations}",
                note = "Across ${activeService.label}",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Live Trains",
                value = "${state.vehicleCount}",
                note = state.coverageLine,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Ridership",
                value = if (state.liveFreshness == LiveFreshness.UNAVAILABLE) "—"
                        else state.ridershipStatus.latestDate.ifEmpty { "—" },
                note = state.ridershipStatus.freshness.label,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Quick actions ──
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionCard(
                icon = Icons.Filled.Search,
                label = "Find Route",
                onClick = { onNavigateToForecast("", "") },
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.Filled.DirectionsTransit,
                label = "Live Map",
                onClick = onNavigateToLive,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.Filled.NotificationsActive,
                label = "Alarms",
                onClick = onNavigateToAlarms,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(18.dp))

        // ── Trending stations ──
        if (state.trending.isNotEmpty()) {
            Text(
                "Trending Now",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.trending) { t ->
                    TrendingCard(
                        name = t.station.name,
                        state = t.station.state,
                        mood = t.mood,
                        delta = t.deltaPercent,
                        tipHour = t.tipHour,
                        onClick = { onNavigateToForecast(t.station.name, "") },
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Last route quick-resume ──
        if (state.lastOrigin.isNotEmpty()) {
            Surface(
                onClick = { onNavigateToForecast(state.lastOrigin, state.lastDestination) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Continue where you left off",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            state.lastOrigin,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.lastDestination.isNotEmpty()) {
                            Text(
                                "→ ${state.lastDestination}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Data freshness ──
        val rs = state.ridershipStatus
        if (rs.latestDate.isNotEmpty()) {
            DataFreshnessLabel(
                latestDate = rs.latestDate,
                daysBehind = computeDaysBehind(rs.latestDate),
                isStale = rs.freshness != com.ktmb.crowdtrend.core.model.RidershipFreshness.FRESH,
            )
        } else {
            DataFreshnessLabel(
                latestDate = state.liveFreshness.name,
                daysBehind = 0,
                isStale = false,
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TrendingCard(
    name: String,
    state: String,
    mood: CrowdMood,
    delta: Int,
    tipHour: String,
    onClick: () -> Unit,
) {
    val (moodIcon, moodColor, moodLabel) = when (mood) {
        CrowdMood.BUSIER -> Triple("🔥", MaterialTheme.colorScheme.error, "Busier")
        CrowdMood.STEADY -> Triple("➡", AmberWarning, "Steady")
        CrowdMood.QUIETER -> Triple("✅", CrowdLow, "Quieter")
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            Modifier.width(180.dp).padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape).background(moodColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                state,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(moodIcon, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(4.dp))
                Text(moodLabel, style = MaterialTheme.typography.labelMedium, color = moodColor)
                Spacer(Modifier.weight(1f))
                Text(
                    "${if (delta > 0) "+" else ""}$delta%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Best: $tipHour",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun computeDaysBehind(dateStr: String): Int {
    return try {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        ChronoUnit.DAYS.between(date, LocalDate.now()).toInt()
    } catch (_: Exception) {
        0
    }
}
