@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.ktmb.crowdtrend.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// ═══════════════════════════════════════════
// JSON DTOs for forecast files (by_origin/*.json)
// Maps snake_case JSON keys to Kotlin camelCase.
// ═══════════════════════════════════════════

@Serializable
data class ByOriginJson(
    val service: String,
    val origin: String,
    @JsonNames("latest_date") val latestDate: String? = null,
    val destinations: Map<String, DestinationJson> = emptyMap(),
)

@Serializable
data class DestinationJson(
    val baseline: List<Double?> = emptyList(),
    @JsonNames("baseline_730") val baseline730: List<Double?> = emptyList(),
    val today: List<Double?> = emptyList(),
)

@Serializable
data class MetaJson(
    val service: String,
    @JsonNames("latest_date") val latestDate: String,
    @JsonNames("earliest_date") val earliestDate: String = "",
    @JsonNames("days_available") val daysAvailable: Int = 0,
    @JsonNames("generated_at") val generatedAt: String = "",
    val source: String = "",
)
