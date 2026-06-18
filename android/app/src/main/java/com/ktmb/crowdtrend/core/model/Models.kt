package com.ktmb.crowdtrend.core.model

import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════
// App Foundation models
// ══════════════════════════════════════════════

// ── Service type ──
enum class ServiceType(val key: String, val label: String) {
    KOMUTER("komuter", "Komuter"),
    KOMUTER_UTARA("komuter_utara", "Komuter Utara"),
    ETS("ets", "ETS"),
    INTERCITY("intercity", "Intercity");

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

// ── GTFS-R feed state ──
enum class FeedState(val label: String, val description: String) {
    LIVE_ACTIVE("Live", "Active vehicles reported by upstream API"),
    LIVE_EMPTY("Empty", "Feed reachable but no vehicles currently reported"),
    FEED_ERROR("Error", "Feed is currently unavailable"),
    STALE("Stale", "Last vehicle data is older than 2 minutes"),
}

// ── Live feed summary with debug metrics ──
data class LiveSummary(
    val vehicleCount: Int,
    val coverage: String,
    val freshness: LiveFreshness,
    val feedState: FeedState = FeedState.FEED_ERROR,
    val lastUpdated: String,
    val vehicles: List<LiveVehicle> = emptyList(),
    // Debug fields
    val httpStatus: Int = 0,
    val responseBytes: Int = 0,
    val lastFetchTimeMs: Long = 0L,
    val lastSuccessTimeMs: Long = 0L,
    val lastVehicleTimestamp: Long = 0L,
    val cacheAgeSeconds: Long = 0L,
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

data class RidershipStatus(
    val latestDate: String,         // ISO date from dataset max
    val rowCount: Int,              // total parsed rows
    val freshness: RidershipFreshness,
    val source: String,             // "data.gov.my" or "offline cache"
    val isCached: Boolean = false,
)

enum class RidershipFreshness(val label: String) {
    FRESH("Fresh"),
    DELAYED("Delayed"),
    STALE("Stale"),
}
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
