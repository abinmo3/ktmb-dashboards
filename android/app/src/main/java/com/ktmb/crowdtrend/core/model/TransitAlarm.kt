package com.ktmb.crowdtrend.core.model

import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════
// Transit Alarm models
// ══════════════════════════════════════════════

data class TransitAlarm(
    val id: String,
    val stationName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 1000,
    val isActive: Boolean = true,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class StationCoords(
    val lat: Double,
    val lon: Double,
)
