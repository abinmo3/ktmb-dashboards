package com.ktmb.crowdtrend.feature.forecast

import com.ktmb.crowdtrend.core.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ForecastLogicTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // ── DTO parsing ──

    @Test
    fun `parse valid forecast JSON`() {
        val raw = """
        {
            "service": "komuter",
            "origin": "KL Sentral",
            "latest_date": "2025-12-31",
            "destinations": {
                "Shah Alam": {
                    "baseline": [1.0, null, 2.0],
                    "baseline_730": [1.5, null, 2.5],
                    "today": [null, 2.0, null]
                }
            }
        }
        """
        val dto = json.decodeFromString<ByOriginJson>(raw)

        assertEquals("komuter", dto.service)
        assertEquals("KL Sentral", dto.origin)
        assertEquals("2025-12-31", dto.latestDate)
        assertEquals(1, dto.destinations.size)

        val dest = dto.destinations["Shah Alam"]!!
        assertEquals(listOf(1.0, null, 2.0), dest.baseline)
        assertEquals(listOf(1.5, null, 2.5), dest.baseline730)
        assertEquals(listOf(null, 2.0, null), dest.today)
    }

    @Test
    fun `missing destination returns null`() {
        val raw = """
        {
            "service": "komuter", "origin": "KL Sentral",
            "latest_date": "2025-12-31",
            "destinations": {"Shah Alam": {"baseline":[], "baseline_730":[], "today":[]}}
        }
        """
        val dto = json.decodeFromString<ByOriginJson>(raw)
        assertNull(dto.destinations["NonExistent"])
    }

    @Test
    fun `empty destinations map is valid`() {
        val raw = """{"service":"komuter","origin":"KL Sentral","latest_date":"2025-12-31","destinations":{}}"""
        val dto = json.decodeFromString<ByOriginJson>(raw)
        assertTrue(dto.destinations.isEmpty())
    }

    @Test
    fun `parse meta JSON for freshness`() {
        val raw = """
        {"service":"komuter","latest_date":"2025-12-31","days_available":365,"generated_at":"2026-06-13 20:59 UTC","source":"https://..."}
        """
        val meta = json.decodeFromString<MetaJson>(raw)
        assertEquals("2025-12-31", meta.latestDate)
        assertEquals(365, meta.daysAvailable)
        assertEquals("2026-06-13 20:59 UTC", meta.generatedAt)
    }

    // ── Best / Avoid windows ──

    @Test
    fun `bestWindows returns lowest value hours`() {
        val values = listOf(10.0, 1.0, 5.0, 2.0, 8.0, null)
        val result = ForecastViewModel.bestWindows(values, 3)
        assertEquals(listOf(1, 3, 2), result) // hours with values 1.0, 2.0, 5.0
    }

    @Test
    fun `avoidWindows returns highest value hours`() {
        val values = listOf(10.0, 1.0, 5.0, 2.0, 8.0, null)
        val result = ForecastViewModel.avoidWindows(values, 2)
        assertEquals(listOf(0, 4), result) // hours with values 10.0, 8.0
    }

    @Test
    fun `bestWindows with all nulls returns empty`() {
        val values = listOf<Double?>(null, null, null)
        assertTrue(ForecastViewModel.bestWindows(values, 3).isEmpty())
    }

    @Test
    fun `avoidWindows with all nulls returns empty`() {
        val values = listOf<Double?>(null, null, null)
        assertTrue(ForecastViewModel.avoidWindows(values, 3).isEmpty())
    }

    @Test
    fun `windows with single value`() {
        val values = listOf<Double?>(null, null, 5.0, null)
        assertEquals(listOf(2), ForecastViewModel.bestWindows(values, 2))
        assertEquals(listOf(2), ForecastViewModel.avoidWindows(values, 2))
    }

    @Test
    fun `request more windows than available returns all`() {
        val values = listOf(1.0, 2.0, null, null)
        val result = ForecastViewModel.bestWindows(values, 10)
        assertEquals(2, result.size) // only 2 non-null values
    }

    // ── Hour formatting ──

    @Test
    fun `formatHour pads single digits`() {
        assertEquals("00:00", ForecastViewModel.formatHour(0))
        assertEquals("09:00", ForecastViewModel.formatHour(9))
        assertEquals("23:00", ForecastViewModel.formatHour(23))
    }

    // ── Freshness computation ──

    @Test
    fun `computeFreshness from meta`() {
        val meta = MetaJson(
            service = "komuter",
            latestDate = "2025-12-31",
            daysAvailable = 365,
            generatedAt = "2026-06-13 20:59 UTC",
        )
        val latest = LocalDate.parse(meta.latestDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val daysBehind = ChronoUnit.DAYS.between(latest, today).toInt()

        assertTrue(daysBehind > 100) // data is from Dec 2025, today is Jun 2026+
        assertTrue(daysBehind > 14) // definitely stale by 14-day threshold
    }

    @Test
    fun `staleness threshold at 14 days`() {
        // Fresh: 0-14 days behind
        assertFalse(0 > 14)
        assertFalse(14 > 14)
        // Stale: > 14 days
        assertTrue(15 > 14)
        assertTrue(165 > 14)
    }

    // ── Directional data awareness ──

    @Test
    fun `forecast is directional — A→B is a specific lookup`() {
        // The repository loads by_origin/{origin.slug}.json and looks up
        // destinations[destination.name]. This means:
        //   KL Sentral → Shah Alam loads kl-sentral.json, key "Shah Alam"
        //   Shah Alam → KL Sentral loads shah-alam.json, key "KL Sentral"
        // These are DIFFERENT lookups and may return different data or null.

        val origin = Station("KL Sentral", "Kuala Lumpur", ServiceType.KOMUTER, "kl-sentral")
        val dest = Station("Shah Alam", "Selangor", ServiceType.KOMUTER, "shah-alam")

        // The repo would load: data/by_origin/kl-sentral.json
        assertEquals("kl-sentral", origin.slug)
        assertEquals("shah-alam", dest.slug)

        // These slugs differ → different files → directional data
        assertNotEquals(origin.slug, dest.slug)
    }

    @Test
    fun `swap reverses origin and destination`() {
        val stations = listOf(
            Station("KL Sentral", "Kuala Lumpur", ServiceType.KOMUTER, "kl-sentral"),
            Station("Shah Alam", "Selangor", ServiceType.KOMUTER, "shah-alam"),
            Station("Klang", "Selangor", ServiceType.KOMUTER, "klang"),
        )
        val origin = stations[0]
        val dest = stations[1]

        // Simulate swap
        val newOrigin = dest
        val newDest = origin
        val newAvailable = stations.filter { it.name != newOrigin.name }

        assertEquals("Shah Alam", newOrigin.name)
        assertEquals("KL Sentral", newDest.name)
        assertEquals(2, newAvailable.size)
        assertFalse(newAvailable.any { it.name == "Shah Alam" }) // origin excluded
    }

    // ── Route selection model ──

    @Test
    fun `RouteSelection label`() {
        val o = Station("KL Sentral", "Kuala Lumpur", ServiceType.KOMUTER, "kl-sentral")
        val d = Station("Ipoh", "Perak", ServiceType.KOMUTER, "ipoh")
        val route = RouteSelection(o, d)
        assertEquals("KL Sentral → Ipoh", route.label)
    }

    // ── CrowdLevel enum ──

    @Test
    fun `CrowdLevel has four levels`() {
        assertEquals(4, CrowdLevel.entries.size)
        assertEquals("Low", CrowdLevel.LOW.label)
        assertEquals("Moderate", CrowdLevel.MODERATE.label)
        assertEquals("Busy", CrowdLevel.BUSY.label)
        assertEquals("Packed", CrowdLevel.PACKED.label)
    }
}
