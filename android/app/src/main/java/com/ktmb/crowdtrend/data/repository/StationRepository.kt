package com.ktmb.crowdtrend.data.repository

import android.content.Context
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.model.Station
import com.ktmb.crowdtrend.core.model.StationCoords
import com.ktmb.crowdtrend.core.model.StationJson
import com.ktmb.crowdtrend.core.util.AssetJsonLoader
import com.ktmb.crowdtrend.data.datastore.KtmbPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Loads station data from bundled Android assets.
 *
 * Data paths:
 *   Komuter:       data/stations.json   + data/state_map.json
 *   Komuter Utara: data/komuter_utara/stations.json  (+ shared state_map)
 *   ETS:           data/ets/stations.json
 *   Intercity:     data/intercity/stations.json
 *   Coordinates:   data/station_coords.json
 */
class StationRepository(private val context: Context) {

    private val prefs = KtmbPreferences(context)

    val activeService: Flow<ServiceType> = prefs.activeService

    // ── Public API ──

    /**
     * Load all real stations for a given service.
     * Filters out sentinel entries ("Penalty", "Unknown").
     */
    suspend fun loadStations(service: ServiceType): List<Station> = withContext(Dispatchers.IO) {
        val prefix = when (service) {
            ServiceType.KOMUTER -> "data/"
            ServiceType.KOMUTER_UTARA -> "data/komuter_utara/"
            ServiceType.ETS -> "data/ets/"
            ServiceType.INTERCITY -> "data/intercity/"
        }

        val stationsJson: List<StationJson> = AssetJsonLoader.load(context, "${prefix}stations.json")
        val stateMap: Map<String, String> = loadStateMap()

        stationsJson
            .filter { it.name != "Penalty" && it.name != "Unknown" }
            .map { sj ->
                Station(
                    name = sj.name,
                    state = stateMap[sj.name] ?: "Other",
                    service = service,
                    slug = Station.slugify(sj.name),
                )
            }
            .sortedBy { it.name }
    }

    /**
     * Load stations from ALL services (for alarm station search).
     */
    suspend fun loadStationsForAlarms(): List<Station> = withContext(Dispatchers.IO) {
        val all = mutableListOf<Station>()
        for (service in ServiceType.entries) {
            try {
                all.addAll(loadStations(service))
            } catch (_: Exception) {
                // skip services that don't have data bundled
            }
        }
        all.distinctBy { it.name }.sortedBy { it.name }
    }

    /**
     * Get coordinates for a station by name.
     */
    suspend fun getStationCoords(name: String): StationCoords? = withContext(Dispatchers.IO) {
        cachedCoords?.get(name) ?: run {
            try {
                val map: Map<String, StationCoords> = AssetJsonLoader.load(context, "data/station_coords.json")
                cachedCoords = map
                map[name]
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Load the state map (shared across all services).
     */
    suspend fun loadStateMap(): Map<String, String> = withContext(Dispatchers.IO) {
        cachedStateMap ?: run {
            val map: Map<String, String> = AssetJsonLoader.load(context, "data/state_map.json")
            cachedStateMap = map
            map
        }
    }

    // ── Internal ──

    @Volatile
    private var cachedStateMap: Map<String, String>? = null

    @Volatile
    private var cachedCoords: Map<String, StationCoords>? = null
}
