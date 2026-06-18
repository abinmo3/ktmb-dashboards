package com.ktmb.crowdtrend.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Direct GTFS-Realtime API fetcher using OkHttp.
 *
 * Fetches raw protobuf bytes from the public data.gov.my endpoint.
 * No auth, no proxy — direct HTTPS call with 10 s connect + read timeout.
 */
object GtfsClient {

    private const val GTFS_URL =
        "https://api.data.gov.my/gtfs-realtime/vehicle-position/ktmb"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Result of a GTFS-realtime fetch.
     *
     * @param bytes     raw protobuf response body
     * @param httpStatus HTTP status code (e.g. 200, 503)
     */
    data class GtfsResponse(
        val bytes: ByteArray,
        val httpStatus: Int,
    )

    /**
     * Perform a blocking HTTP GET to the GTFS-realtime feed.
     *
     * Callers MUST run this off the main thread (e.g. via withContext(Dispatchers.IO)).
     *
     * @return [GtfsResponse] with bytes and status code
     * @throws IOException on network failure, timeout, or DNS error
     */
    fun fetch(): GtfsResponse {
        val request = Request.Builder()
            .url(GTFS_URL)
            .get()
            .build()

        val response = client.newCall(request).execute()
        return response.use { resp ->
            val body = resp.body
                ?: throw IllegalStateException("Empty response body (HTTP ${resp.code})")
            val bytes = body.bytes()
            GtfsResponse(bytes = bytes, httpStatus = resp.code)
        }
    }
}
