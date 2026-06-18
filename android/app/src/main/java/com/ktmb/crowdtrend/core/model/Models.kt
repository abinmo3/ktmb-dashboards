package com.ktmb.crowdtrend.core.model

import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════
// App Foundation models
// ══════════════════════════════════════════════

// ── Service type ──
enum class ServiceType(val key: String, val label: String) {
    KOMUTER("komuter", "Komuter"),
    KOMUTER_UTARA("komuter_utara", "Komuter Utara");

    companion object {
        fun fromKey(key: String): ServiceType =
            entries.firstOrNull { it.key == key } ?: KOMUTER
    }
}

// ── Station ──
@Serializable
data class StationJson(
    val name: String,
)

data class Station(
    val name: String,
    val state: String,      // resolved from state_map.json
    val service: ServiceType,
    val slug: String,       // pre-computed slugified name for forecast lookup
) {
    companion object {
        fun slugify(name: String): String =
            name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
    }
}

// ── Route selection (origin→destination pair) ──
data class RouteSelection(
    val origin: Station,
    val destination: Station,
) {
    val label: String get() = "${origin.name} → ${destination.name}"
}

// ── Hourly crowd data point ──
data class HourlyForecast(
    val hour: Int,              // 0–23
    val baseline: Double?,      // 12-month average ratio (null = no data)
    val baseline730: Double?,   // 24-month average ratio ("typical")
    val today: Double?,         // latest-day actual ratio
)

// ── Full forecast for a route ──
data class Forecast(
    val origin: String,
    val destination: String,
    val latestDate: String,
    val hourly: List<HourlyForecast>,  // always 24 elements (index = hour)
)

// ── Crowd level classification ──
enum class CrowdLevel(val label: String) {
    LOW("Low"),
    MODERATE("Moderate"),
    BUSY("Busy"),
    PACKED("Packed");
}

// ── Live vehicle (parsed from GTFS-realtime protobuf) ──
data class LiveVehicle(
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val speed: Float,           // m/s
    val timestamp: Long,        // epoch seconds
)

// ── Live feed summary state ──
data class LiveSummary(
    val vehicleCount: Int,
    val coverage: String,        // "Low" / "Medium" / "High" / "Very high" / "Unavailable"
    val freshness: LiveFreshness,
    val lastUpdated: String,     // human-readable timestamp
    val vehicles: List<LiveVehicle> = emptyList(),
) {
    companion object {
        val EMPTY = LiveSummary(
            vehicleCount = 0,
            coverage = "Unavailable",
            freshness = LiveFreshness.UNAVAILABLE,
            lastUpdated = "",
        )
    }
}

enum class LiveFreshness {
    FRESH,       // < 60s
    STALE,       // 60–300s
    EXPIRED,     // > 300s
    UNAVAILABLE, // network error or never fetched
}

// ── App preferences (persisted via DataStore) ──
data class AppPreferences(
    val activeService: ServiceType = ServiceType.KOMUTER,
    val lastOrigin: String = "",
    val lastDestination: String = "",
)

// ── Guide entry (from guides.json) ──
data class Guide(
    val slug: String,
    val title: String,
    val summary: String,
    val readTime: String,
    val updated: String,
    val sections: List<GuideSection>,
)

data class GuideSection(
    val heading: String,
    val body: String,
)

// ── Service metadata (from meta.json) ──
data class ServiceMeta(
    val service: String,
    val latestDate: String,      // ISO date "2025-12-31"
    val earliestDate: String,
    val daysAvailable: Int,
    val generatedAt: String,
    val source: String,
)

// ── Data freshness display model ──
data class DataFreshness(
    val latestDate: String,
    val daysAvailable: Int,
    val generatedAt: String,
    val isStale: Boolean,        // true if latestDate > 14 days behind system clock
    val daysBehind: Int,         // how many days behind
) {
    companion object {
        val EMPTY = DataFreshness(
            latestDate = "",
            daysAvailable = 0,
            generatedAt = "",
            isStale = false,
            daysBehind = 0,
        )
    }
}
