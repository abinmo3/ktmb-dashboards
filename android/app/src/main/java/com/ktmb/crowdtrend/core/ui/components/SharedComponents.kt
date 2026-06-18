package com.ktmb.crowdtrend.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.ui.theme.*

// ═══════════════════════════════════════════
// Service Switch
// ═══════════════════════════════════════════

@Composable
fun ServiceSwitcher(
    selected: ServiceType,
    onSelect: (ServiceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.Center,
    ) {
        ServiceType.entries.forEach { service ->
            val isSel = service == selected
            val bg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            val fg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { onSelect(service) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = bg, contentColor = fg),
            ) {
                Text(service.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

// ═══════════════════════════════════════════
// LoadingState
// ═══════════════════════════════════════════

@Composable
fun LoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ═══════════════════════════════════════════
// ErrorState
// ═══════════════════════════════════════════

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "⚠",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// EmptyState
// ═══════════════════════════════════════════

@Composable
fun EmptyState(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "📭",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// AdvisoryDisclaimer
// ═══════════════════════════════════════════

@Composable
fun AdvisoryDisclaimer(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
        )
    }
}

// ═══════════════════════════════════════════
// DataFreshnessLabel
// ═══════════════════════════════════════════

@Composable
fun DataFreshnessLabel(
    latestDate: String,
    daysBehind: Int,
    isStale: Boolean,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor, label) = if (isStale) {
        Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onErrorContainer,
            "⚠ Data is $daysBehind days behind. Patterns may have shifted."
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Latest ridership: $latestDate · Updated within 2 weeks"
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (isStale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ═══════════════════════════════════════════
// Section Header (reusable)
// ═══════════════════════════════════════════

@Composable
fun SectionHeader(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 2.dp)) {
        Text(
            kicker,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ═══════════════════════════════════════════
// Metric Card (reusable)
// ═══════════════════════════════════════════

@Composable
fun MetricCard(
    label: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 2)
        }
    }
}

// ═══════════════════════════════════════════
// 24-hour Crowd Heatmap (placeholder)
// ═══════════════════════════════════════════

@Composable
fun CrowdHeatmap(
    values: List<Double?>,
    modifier: Modifier = Modifier,
) {
    val finite = values.filterNotNull()
    val max = finite.maxOrNull() ?: 1.0
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            values.forEach { value ->
                val norm = if (value != null && max > 0)
                    ((value / max) * 100).coerceIn(8.0, 100.0) else 8.0
                val color = when {
                    value == null -> Color(0xFFE0E0E0)
                    value <= max * 0.25 -> CrowdLow
                    value <= max * 0.5 -> CrowdMid
                    value <= max * 0.75 -> CrowdHigh
                    else -> CrowdPacked
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height((norm / 100 * 60).dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(color),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
