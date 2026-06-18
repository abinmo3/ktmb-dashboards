package com.ktmb.crowdtrend.data.remote

import com.ktmb.crowdtrend.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * Raw byte fetcher for the GTFS-realtime vehicle position feed.
 *
 * Security: No API keys or secrets. The proxy is a public Cloudflare Worker
 * that simply adds CORS headers to the upstream data.gov.my endpoint.
 */
interface GtfsService {
    @GET
    suspend fun getVehiclePositions(@Url url: String): okhttp3.ResponseBody
}

object GtfsClient {

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.NONE  // no URL logging in any build
                else
                    HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    val service: GtfsService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.GTFS_PROXY_URL)
            .client(okHttp)
            .build()
            .create(GtfsService::class.java)
    }
}
