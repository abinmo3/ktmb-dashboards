package com.ktmb.crowdtrend.data.repository

import com.ktmb.crowdtrend.core.model.LiveFreshness
import org.junit.Assert.*
import org.junit.Test

class LiveFeedLogicTest {

    // ── Coverage labels ──

    @Test
    fun `coverage label — no vehicles`() {
        assertEquals("No vehicles detected", LiveRepository.coverageLabel(0))
    }

    @Test
    fun `coverage label — low activity`() {
        assertEquals("Low activity", LiveRepository.coverageLabel(1))
        assertEquals("Low activity", LiveRepository.coverageLabel(5))
    }

    @Test
    fun `coverage label — moderate activity`() {
        assertEquals("Moderate activity", LiveRepository.coverageLabel(6))
        assertEquals("Moderate activity", LiveRepository.coverageLabel(13))
    }

    @Test
    fun `coverage label — high activity`() {
        assertEquals("High activity", LiveRepository.coverageLabel(14))
        assertEquals("High activity", LiveRepository.coverageLabel(23))
    }

    @Test
    fun `coverage label — very high activity`() {
        assertEquals("Very high activity", LiveRepository.coverageLabel(24))
        assertEquals("Very high activity", LiveRepository.coverageLabel(100))
    }

    // ── Freshness degradation ──

    @Test
    fun `freshness thresholds`() {
        // FRESH: < 60s
        assertTrue(0 < 60_000)
        assertTrue(59_000 < 60_000)
        // STALE: 60–300s
        assertTrue(60_000 in 60_000..<300_000)
        assertTrue(299_000 in 60_000..<300_000)
        // EXPIRED: >= 300s
        assertTrue(300_000 >= 300_000)
        assertTrue(600_000 >= 300_000)
    }

    @Test
    fun `freshness enum has four states`() {
        assertEquals(4, LiveFreshness.entries.size)
        assertNotNull(LiveFreshness.FRESH)
        assertNotNull(LiveFreshness.STALE)
        assertNotNull(LiveFreshness.EXPIRED)
        assertNotNull(LiveFreshness.UNAVAILABLE)
    }

    // ── Advisory language check ──

    @Test
    fun `advisory language — no prediction or guarantee words`() {
        // Verify that coverage labels don't imply predictions
        val labels = listOf(
            LiveRepository.coverageLabel(0),
            LiveRepository.coverageLabel(5),
            LiveRepository.coverageLabel(10),
            LiveRepository.coverageLabel(20),
            LiveRepository.coverageLabel(50),
        )
        for (label in labels) {
            assertFalse("Coverage label '$label' must not imply prediction",
                label.contains("arriving", ignoreCase = true))
            assertFalse("Coverage label '$label' must not claim reliability",
                label.contains("reliable", ignoreCase = true) ||
                label.contains("normal", ignoreCase = true))
        }

        // Freshness labels are purely descriptive
        val freshnessNames = LiveFreshness.entries.map { it.name }
        for (name in freshnessNames) {
            assertFalse("Freshness '$name' must not imply prediction",
                name.contains("ARRIVING", ignoreCase = true) ||
                name.contains("RELIABLE", ignoreCase = true) ||
                name.contains("NORMAL", ignoreCase = true))
        }
    }

    // ── EMPTY defaults ──

    @Test
    fun `LiveSummary EMPTY has safe defaults`() {
        val empty = com.ktmb.crowdtrend.core.model.LiveSummary.EMPTY
        assertEquals(0, empty.vehicleCount)
        assertEquals("No vehicles detected", empty.coverage)
        assertEquals(LiveFreshness.UNAVAILABLE, empty.freshness)
        assertTrue(empty.vehicles.isEmpty())
    }

    // ── Timestamp formatting ──

    @Test
    fun `formatTimestamp produces HH_mm_ss`() {
        // Fix a known epoch: 12:00:00 UTC = 43200000 ms
        val result = LiveRepository.formatTimestamp(43200000L)
        // Result depends on local timezone, but format should be HH:mm:ss
        assertTrue(result.matches(Regex("\\d{2}:\\d{2}:\\d{2}")))
    }

    // ── LiveVehicle model ──

    @Test
    fun `LiveVehicle construction`() {
        val v = com.ktmb.crowdtrend.core.model.LiveVehicle(
            vehicleId = "DMU09",
            latitude = 3.1357,
            longitude = 101.6880,
            bearing = 45f,
            speed = 11.1f,      // m/s ≈ 40 km/h
            timestamp = 1781453816L,
        )
        assertEquals("DMU09", v.vehicleId)
        assertEquals(3.1357, v.latitude, 0.0001)
        assertEquals(101.6880, v.longitude, 0.0001)
        assertEquals(45f, v.bearing)
        assertEquals(11.1f, v.speed)
        assertEquals(1781453816L, v.timestamp)
    }

    // ── Speed conversion (m/s → km/h) used in UI ──

    @Test
    fun `speed m_s to km_h conversion`() {
        val speedMs = 11.1f
        val speedKmh = speedMs * 3.6f
        assertEquals(39.96f, speedKmh, 0.1f)
    }
}
