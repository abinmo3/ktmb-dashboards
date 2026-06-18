package com.ktmb.crowdtrend.data.repository

import com.google.transit.realtime.FeedMessage
import com.ktmb.crowdtrend.BuildConfig
import com.ktmb.crowdtrend.core.model.LiveFreshness
import com.ktmb.crowdtrend.core.model.LiveSummary
import com.ktmb.crowdtrend.core.model.LiveVehicle
import com.ktmb.crowdtrend.data.remote.GtfsClient

/**
 * Fetches and parses the GTFS-realtime vehicle position feed.
 *
 * Data freshness thresholds:
 *   FRESH       — fetch succeeded, data < 60s old
 *   STALE       — last successful fetch 60–300s ago
 *   EXPIRED     — last successful fetch > 300s ago
 *   UNAVAILABLE — never fetched or network error
 */
class LiveRepository {

    private val service = GtfsClient.service

    /**
     * Fetch the latest vehicle positions from the GTFS proxy.
     *
     * @return LiveSummary with parsed vehicles, coverage label, and freshness.
     * @throws Exception on network error, protobuf parse failure, or empty response.
     */
    suspend fun fetch(): LiveSummary {
        val response = service.getVehiclePositions(BuildConfig.GTFS_PROXY_URL)
        val bytes = response.use { it.bytes() }

        val feed = FeedMessage.parseFrom(bytes)

        val vehicles = feed.entityList
            .filter { it.hasVehicle() }
            .map { entity ->
                val v = entity.vehicle
                LiveVehicle(
                    vehicleId = v.vehicleId.ifEmpty { entity.id },
                    latitude = v.position.latitude.toDouble(),
                    longitude = v.position.longitude.toDouble(),
                    bearing = v.position.bearing,
                    speed = v.position.speed,
                    timestamp = v.timestamp,
                )
            }

        val count = vehicles.size
        val coverage = coverageLabel(count)

        return LiveSummary(
            vehicleCount = count,
            coverage = coverage,
            freshness = LiveFreshness.FRESH,
            lastUpdated = formatTimestamp(System.currentTimeMillis()),
            vehicles = vehicles,
        )
    }

    companion object {
        fun coverageLabel(count: Int): String = when {
            count == 0 -> "No vehicles detected"
            count < 6 -> "Low activity"
            count < 14 -> "Moderate activity"
            count < 24 -> "High activity"
            else -> "Very high activity"
        }

        fun formatTimestamp(epochMillis: Long): String {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(epochMillis))
        }
    }
}
