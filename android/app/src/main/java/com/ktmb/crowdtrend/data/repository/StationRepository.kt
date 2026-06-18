package com.ktmb.crowdtrend.data.repository

import android.content.Context
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.model.Station
import com.ktmb.crowdtrend.core.model.StationJson
import com.ktmb.crowdtrend.core.util.AssetJsonLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads station data from bundled Android assets.
 *
 * Data paths:
 *   Komuter:       data/stations.json   + data/state_map.json
 *   Komuter Utara: data/komuter_utara/stations.json  (+ shared state_map)
 */
class StationRepository(private val context: Context) {

    // ── Public API ──

    /**
     * Load all real stations for a given service.
     * Filters out sentinel entries ("Penalty", "Unknown").
     * Runs on IO dispatcher — safe to call from ViewModel.
     */
    suspend fun loadStations(service: ServiceType): List<Station> = withContext(Dispatchers.IO) {
        val prefix = when (service) {
            ServiceType.KOMUTER -> "data/"
            ServiceType.KOMUTER_UTARA -> "data/komuter_utara/"
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
     * Load the state map (shared across both services).
     * Cached after first load — file is small (~2.5 KB).
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
}
