package com.ktmb.crowdtrend.data.repository

import android.content.Context
import com.ktmb.crowdtrend.core.model.*
import com.ktmb.crowdtrend.core.util.AssetJsonLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Loads crowd forecast data from bundled assets.
 *
 * Forecast files live at:
 *   Komuter:       data/by_origin/{slug}.json
 *   Komuter Utara: data/komuter_utara/by_origin/{slug}.json
 *   ETS:           data/ets/by_origin/{slug}.json
 *   Intercity:     data/intercity/by_origin/{slug}.json
 *
 * Each file contains all destinations reachable from that origin.
 */
class ForecastRepository(private val context: Context) {

    /**
     * Load the forecast for origin → destination.
     *
     * @return [Forecast] if both origin file and destination exist, null if route is missing
     *         or if the asset file is missing/unreadable.
     */
    suspend fun loadForecast(
        service: ServiceType,
        origin: Station,
        destination: Station,
    ): Forecast? = withContext(Dispatchers.IO) {
        val prefix = when (service) {
            ServiceType.KOMUTER -> "data/"
            ServiceType.KOMUTER_UTARA -> "data/komuter_utara/"
            ServiceType.ETS -> "data/ets/"
            ServiceType.INTERCITY -> "data/intercity/"
            else -> "data/"
        }

        val dto: ByOriginJson = try {
            AssetJsonLoader.load(context, "${prefix}by_origin/${origin.slug}.json")
        } catch (e: IOException) {
            return@withContext null
        }

        val destData = dto.destinations[destination.name] ?: return@withContext null

        Forecast(
            origin = dto.origin,
            destination = destination.name,
            latestDate = dto.latestDate ?: "",
            hourly = (0..23).map { hour ->
                HourlyForecast(
                    hour = hour,
                    baseline = destData.baseline.getOrNull(hour),
                    baseline730 = destData.baseline730.getOrNull(hour),
                    today = destData.today.getOrNull(hour),
                )
            },
        )
    }

    /**
     * Load service metadata for freshness display.
     */
    suspend fun loadMeta(service: ServiceType): MetaJson = withContext(Dispatchers.IO) {
        val prefix = when (service) {
            ServiceType.KOMUTER -> "data/"
            ServiceType.KOMUTER_UTARA -> "data/komuter_utara/"
            ServiceType.ETS -> "data/ets/"
            ServiceType.INTERCITY -> "data/intercity/"
            else -> "data/"
        }
        AssetJsonLoader.load(context, "${prefix}meta.json")
    }
}
