package com.ktmb.crowdtrend.data.repository

import com.google.transit.realtime.FeedMessage
import com.ktmb.crowdtrend.core.model.FeedState
import com.ktmb.crowdtrend.core.model.LiveFreshness
import com.ktmb.crowdtrend.core.model.LiveSummary
import com.ktmb.crowdtrend.core.model.LiveVehicle
import com.ktmb.crowdtrend.data.remote.GtfsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fetches and parses the GTFS-realtime vehicle position feed.
 *
 * Data freshness thresholds:
 *   FRESH       — fetch succeeded, data < 60s old
 *   STALE       — last successful fetch 60–300s ago
 *   EXPIRED     — last successful fetch > 300s ago
 *   UNAVAILABLE — never fetched or network error
 *
 * On fetch failure, the last successful [LiveSummary] is returned with
 * [FeedState.STALE] freshness so the UI can continue showing vehicle data
 * while clearly indicating it may be out of date.
 */
class LiveRepository {

    /** Last successful fetch result.  null until the first success. */
    private var lastSuccess: LiveSummary? = null

    // ── Public ──────────────────────────────────────────────────

    /**
     * Fetch the latest vehicle positions directly from the data.gov.my
     * GTFS-realtime API and parse the protobuf response.
     *
     * - HTTP 200 + entities > 0  →  [FeedState.LIVE_ACTIVE]
     * - HTTP 200 + entities == 0 →  [FeedState.LIVE_EMPTY]
     * - Any failure              →  [FeedState.FEED_ERROR] (or
     *   [FeedState.STALE] if cached data is available)
     */
    suspend fun fetch(): LiveSummary {
        val fetchTimeMs = System.currentTimeMillis()

        return try {
            // Offload blocking OkHttp call to IO dispatcher
            val response = withContext(Dispatchers.IO) {
                GtfsClient.fetch()
            }

            val bytes = response.bytes
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
            val feedState = if (response.httpStatus == 200) {
                if (count > 0) FeedState.LIVE_ACTIVE
                else FeedState.LIVE_EMPTY
            } else {
                FeedState.FEED_ERROR
            }

            // Max vehicle timestamp (epoch seconds → epoch millis)
            val lastVehicleTimestamp =
                vehicles.maxOfOrNull { it.timestamp }?.let { it * 1000L } ?: 0L

            val cacheAgeSeconds = if (lastVehicleTimestamp > 0L) {
                (fetchTimeMs - lastVehicleTimestamp) / 1000
            } else {
                0L
            }

            val summary = LiveSummary(
                vehicleCount = count,
                coverage = coverageLabel(count),
                freshness = LiveFreshness.FRESH,
                feedState = feedState,
                lastUpdated = formatTimestamp(fetchTimeMs),
                vehicles = vehicles,
                httpStatus = response.httpStatus,
                responseBytes = bytes.size,
                lastFetchTimeMs = fetchTimeMs,
                lastSuccessTimeMs = fetchTimeMs,
                lastVehicleTimestamp = lastVehicleTimestamp,
                cacheAgeSeconds = cacheAgeSeconds,
            )

            lastSuccess = summary
            summary

        } catch (_: Exception) {
            // On any failure, return cached data (if available) or empty
            val cached = lastSuccess
            if (cached != null) {
                val ageSecs = (fetchTimeMs - cached.lastSuccessTimeMs) / 1000
                cached.copy(
                    freshness = LiveFreshness.STALE,
                    feedState = FeedState.STALE,
                    lastFetchTimeMs = fetchTimeMs,
                    cacheAgeSeconds = ageSecs,
                )
            } else {
                LiveSummary.EMPTY.copy(
                    lastFetchTimeMs = fetchTimeMs,
                )
            }
        }
    }

    // ── Companion ───────────────────────────────────────────────

    companion object {
        fun coverageLabel(count: Int): String = when {
            count == 0 -> "No vehicles detected"
            count < 6  -> "Low activity"
            count < 14 -> "Moderate activity"
            count < 24 -> "High activity"
            else       -> "Very high activity"
        }

        fun formatTimestamp(epochMillis: Long): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(epochMillis))
        }
    }
}
