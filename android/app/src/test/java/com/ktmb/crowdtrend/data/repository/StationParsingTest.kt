package com.ktmb.crowdtrend.data.repository

import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.model.Station
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for station JSON parsing and Station model logic.
 * These run without Android — pure Kotlin + JUnit.
 */
class StationParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // ── StationJson parsing ──

    @Test
    fun `parse valid stations json`() {
        val raw = """[{"name":"KL Sentral"},{"name":"Shah Alam"},{"name":"Ipoh"}]"""
        val result = json.decodeFromString<List<StationJson>>(raw)

        assertEquals(3, result.size)
        assertEquals("KL Sentral", result[0].name)
    }

    @Test
    fun `parse empty stations json`() {
        val result = json.decodeFromString<List<StationJson>>("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse stations with sentinel entries`() {
        val raw = """[{"name":"KL Sentral"},{"name":"Unknown"},{"name":"Penalty"},{"name":"Ipoh"}]"""
        val result = json.decodeFromString<List<StationJson>>(raw)

        // All parse correctly — filtering is the app's responsibility
        assertEquals(4, result.size)
        assertTrue(result.any { it.name == "Unknown" })
        assertTrue(result.any { it.name == "Penalty" })
    }

    @Test(expected = kotlinx.serialization.SerializationException::class)
    fun `malformed json throws`() {
        json.decodeFromString<List<StationJson>>("{bad json}")
    }

    @Test
    fun `stations with extra fields are parsed safely`() {
        val raw = """[{"name":"KL Sentral","extra_field":"ignored","another":123}]"""
        val result = json.decodeFromString<List<StationJson>>(raw)

        assertEquals(1, result.size)
        assertEquals("KL Sentral", result[0].name)
        // Extra fields are silently ignored (ignoreUnknownKeys = true)
    }

    // ── Sentinel filtering ──

    @Test
    fun `filter removes Penalty and Unknown`() {
        val raw = listOf(
            StationJson("KL Sentral"),
            StationJson("Unknown"),
            StationJson("Penalty"),
            StationJson("Ipoh"),
            StationJson("Unknown"),
        )
        val filtered = raw
            .filter { it.name != "Penalty" && it.name != "Unknown" }
            .map { it.name }

        assertEquals(listOf("KL Sentral", "Ipoh"), filtered)
    }

    // ── state_map parsing ──

    @Test
    fun `parse state_map json`() {
        val raw = """{"KL Sentral":"Kuala Lumpur","Shah Alam":"Selangor","Butterworth":"Pulau Pinang"}"""
        val result = json.decodeFromString<Map<String, String>>(raw)

        assertEquals(3, result.size)
        assertEquals("Kuala Lumpur", result["KL Sentral"])
        assertEquals("Selangor", result["Shah Alam"])
    }

    @Test
    fun `state_map missing key returns null safely`() {
        val raw = """{"KL Sentral":"Kuala Lumpur"}"""
        val result = json.decodeFromString<Map<String, String>>(raw)

        assertNull(result["NonExistent"])
        // App code should use: map[name] ?: "Other"
        assertEquals("Other", result["NonExistent"] ?: "Other")
    }

    // ── Station model ──

    @Test
    fun `Station slugify all known names produce valid filenames`() {
        val names = listOf(
            "KL Sentral", "Bandar Tasek Selatan", "Kajang", "Kajang 2",
            "Pulau Sebang (Tampin)", "Butterworth", "Alor Setar",
            "Kuala Lumpur", "Subang Jaya", "Bukit Mertajam",
        )
        for (name in names) {
            val slug = Station.slugify(name)
            assertTrue("Slug '$slug' must be non-empty", slug.isNotEmpty())
            assertTrue("Slug '$slug' must match [a-z0-9-]+", slug.matches(Regex("[a-z0-9-]+")))
            assertFalse("Slug '$slug' must not contain spaces", slug.contains(" "))
        }
    }

    @Test
    fun `Station model construction`() {
        val station = Station(
            name = "KL Sentral",
            state = "Kuala Lumpur",
            service = ServiceType.KOMUTER,
            slug = "kl-sentral",
        )
        assertEquals("KL Sentral", station.name)
        assertEquals("Kuala Lumpur", station.state)
        assertEquals(ServiceType.KOMUTER, station.service)
        assertEquals("kl-sentral", station.slug)
    }

    @Test
    fun `Station state defaults to Other when not in state_map`() {
        val stateMap = mapOf("KL Sentral" to "Kuala Lumpur")
        val resolved = stateMap["Unknown Station"] ?: "Other"
        assertEquals("Other", resolved)
    }

    // ── Service type ──

    @Test
    fun `ServiceType fromKey`() {
        assertEquals(ServiceType.KOMUTER, ServiceType.fromKey("komuter"))
        assertEquals(ServiceType.KOMUTER_UTARA, ServiceType.fromKey("komuter_utara"))
        assertEquals(ServiceType.KOMUTER, ServiceType.fromKey("unknown")) // default
    }
}
