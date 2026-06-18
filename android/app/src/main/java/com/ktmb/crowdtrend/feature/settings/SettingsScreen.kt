package com.ktmb.crowdtrend.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.ui.components.*

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val activeService by vm.activeService.collectAsState()

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
                SectionHeader(kicker = "Data", title = "Freshness & sources")
                Spacer(Modifier.height(8.dp))
                DataFreshnessLabel(
                    latestDate = "2025-12-31",
                    daysBehind = 165,
                    isStale = true,
                )
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
