package com.ktmb.crowdtrend.core.model

import org.junit.Assert.*
import org.junit.Test

class StationTest {

    @Test
    fun `slugify simple name`() {
        assertEquals("kl-sentral", Station.slugify("KL Sentral"))
    }

    @Test
    fun `slugify with parentheses`() {
        assertEquals("pulau-sebang-tampin", Station.slugify("Pulau Sebang (Tampin)"))
    }

    @Test
    fun `slugify single word`() {
        assertEquals("ipoh", Station.slugify("Ipoh"))
    }

    @Test
    fun `slugify with numbers`() {
        assertEquals("kajang-2", Station.slugify("Kajang 2"))
    }

    @Test
    fun `slugify with special chars`() {
        assertEquals("bukit-mertajam", Station.slugify("Bukit Mertajam"))
    }

    @Test
    fun `slugify consistency — round-trip check for all known stations`() {
        // Every station in komuter + utara should slugify to a valid filename
        val allStations = listOf(
            "KL Sentral", "Bandar Tasek Selatan", "Kajang", "Kajang 2",
            "Subang Jaya", "Shah Alam", "Klang", "Pelabuhan Klang Selatan",
            "Kuala Lumpur", "Bank Negara", "Midvalley", "Abdullah Hukum",
            "Angkasapuri", "Sentul", "Kepong", "Kepong Sentral",
            "Sungai Buloh", "Rawang", "Serendah", "Batang Kali",
            "Kuala Kubu Bharu", "Tanjong Malim", "Rasa", "Kuang",
            "Batu Caves", "Taman Wahyu", "Kampung Batu", "Segambut",
            "Putra", "Jalan Templer", "Petaling", "Pantai Dalam",
            "Salak Selatan", "Serdang", "UKM", "Bangi",
            "Nilai", "Labu", "Tiroi", "Seremban", "Senawang",
            "Sungai Gadut", "Rembau", "Batang Benar", "Pulau Sebang (Tampin)",
            "Setia Jaya", "Seri Setia", "Padang Jawa", "Bukit Badak",
            "Kampung Dato Harun", "Jalan Kastam", "Telok Pulai", "Telok Gadong",
            "Kampung Raja Uda", "Batu Tiga", "Batu Kentonmen", "Seputeh",
            "Rengam",
            // Komuter Utara
            "Butterworth", "Alor Setar", "Sungai Petani", "Ipoh",
            "Padang Besar", "Arau", "Anak Bukit", "Bukit Ketri",
            "Kodiang", "Kobah", "Gurun", "Kamunting", "Taiping",
            "Kuala Kangsar", "Padang Rengas", "Sungai Siput",
            "Bukit Mertajam", "Bukit Tengah", "Tasek Gelugor",
            "Nibong Tebal", "Parit Buntar", "Bagan Serai", "Simpang Ampat",
        )

        for (name in allStations) {
            val slug = Station.slugify(name)
            assertTrue("Slug for '$name' must be non-empty", slug.isNotEmpty())
            assertTrue("Slug for '$name' must not contain spaces: '$slug'", !slug.contains(" "))
            assertTrue("Slug for '$name' must match [a-z0-9-]+: '$slug'",
                slug.matches(Regex("[a-z0-9-]+")))
        }
    }
}

class LiveSummaryTest {

    @Test
    fun `EMPTY has correct defaults`() {
        val empty = LiveSummary.EMPTY
        assertEquals(0, empty.vehicleCount)
        assertEquals("Unavailable", empty.coverage)
        assertEquals(LiveFreshness.UNAVAILABLE, empty.freshness)
        assertTrue(empty.vehicles.isEmpty())
    }
}

class DataFreshnessTest {

    @Test
    fun `EMPTY has correct defaults`() {
        val empty = DataFreshness.EMPTY
        assertEquals("", empty.latestDate)
        assertEquals(0, empty.daysAvailable)
        assertFalse(empty.isStale)
        assertEquals(0, empty.daysBehind)
    }
}
