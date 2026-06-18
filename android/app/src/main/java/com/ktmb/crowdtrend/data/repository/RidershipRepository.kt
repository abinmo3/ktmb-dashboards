package com.ktmb.crowdtrend.data.repository

import android.content.Context
import com.ktmb.crowdtrend.core.model.RidershipFreshness
import com.ktmb.crowdtrend.core.model.RidershipStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Fetches and parses the daily ridership CSV from data.gov.my.
 *
 * Caches the raw CSV to an app-private file so the app can display
 * the last-known-good data when the network is unavailable.
 *
 * Freshness thresholds (vs today):
 *   FRESH   — latest date ≤ 3 days behind
 *   DELAYED — latest date 4–7 days behind
 *   STALE   — latest date > 7 days behind
 */
class RidershipRepository(private val context: Context) {

    companion object {
        private const val CSV_URL =
            "https://storage.data.gov.my/transportation/ktmb/ridership_ktmb_daily.csv"
        private const val CACHE_FILENAME = "ridership_cache.csv"
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Fetch the latest ridership status.
     * Tries network first; falls back to cached CSV on failure.
     */
    suspend fun fetchStatus(): RidershipStatus = withContext(Dispatchers.IO) {
        try {
            fetchFromNetwork()
        } catch (e: Exception) {
            fetchFromCache()
        }
    }

    // ── Network ──

    private fun fetchFromNetwork(): RidershipStatus {
        val request = Request.Builder().url(CSV_URL).build()
        val response = client.newCall(request).execute()
        val body = response.body ?: throw Exception("Empty response body")
        val csv = body.string()

        // Cache the raw CSV
        cacheCsv(csv)

        return parseAndBuild(csv, source = "data.gov.my", isCached = false)
    }

    // ── Cache ──

    private fun cacheCsv(csv: String) {
        try {
            context.openFileOutput(CACHE_FILENAME, Context.MODE_PRIVATE).use { out ->
                out.write(csv.toByteArray())
            }
        } catch (_: Exception) {
            // best-effort cache write
        }
    }

    private fun fetchFromCache(): RidershipStatus {
        val file = File(context.filesDir, CACHE_FILENAME)
        if (!file.exists()) {
            return RidershipStatus(
                latestDate = "",
                rowCount = 0,
                freshness = RidershipFreshness.STALE,
                source = "offline cache",
                isCached = true,
            )
        }
        val csv = file.readText()
        return parseAndBuild(csv, source = "offline cache", isCached = true)
    }

    // ── CSV parsing ──

    private fun parseAndBuild(csv: String, source: String, isCached: Boolean): RidershipStatus {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size < 2) {
            return RidershipStatus(
                latestDate = "",
                rowCount = 0,
                freshness = RidershipFreshness.STALE,
                source = source,
                isCached = isCached,
            )
        }

        // Parse header to find 'date' column index
        val header = parseCsvLine(lines[0])
        val dateColIndex = header.indexOfFirst {
            it.trim().lowercase() == "date"
        }
        if (dateColIndex == -1) {
            throw Exception("CSV missing 'date' column")
        }

        // Parse all data rows, collect dates
        var maxDate: LocalDate? = null
        var rowCount = 0
        for (i in 1 until lines.size) {
            val cols = parseCsvLine(lines[i])
            if (cols.size > dateColIndex) {
                val dateStr = cols[dateColIndex].trim()
                if (dateStr.isNotEmpty()) {
                    try {
                        val date = LocalDate.parse(dateStr, DATE_FORMATTER)
                        if (maxDate == null || date.isAfter(maxDate)) {
                            maxDate = date
                        }
                        rowCount++
                    } catch (_: Exception) {
                        // skip unparseable rows
                    }
                }
            }
        }

        val latestDateStr = maxDate?.format(DATE_FORMATTER) ?: ""
        val freshness = computeFreshness(maxDate)

        return RidershipStatus(
            latestDate = latestDateStr,
            rowCount = rowCount,
            freshness = freshness,
            source = source,
            isCached = isCached,
        )
    }

    /**
     * Simple CSV line parser that handles quoted fields.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    // ── Freshness computation ──

    private fun computeFreshness(latestDate: LocalDate?): RidershipFreshness {
        if (latestDate == null) return RidershipFreshness.STALE
        val today = LocalDate.now()
        val daysBehind = today.toEpochDay() - latestDate.toEpochDay()
        return when {
            daysBehind <= 3 -> RidershipFreshness.FRESH
            daysBehind <= 7 -> RidershipFreshness.DELAYED
            else -> RidershipFreshness.STALE
        }
    }
}
