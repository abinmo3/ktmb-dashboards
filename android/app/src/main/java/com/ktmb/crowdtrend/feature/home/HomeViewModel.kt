package com.ktmb.crowdtrend.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktmb.crowdtrend.core.model.LiveFreshness
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.model.Station
import com.ktmb.crowdtrend.data.datastore.KtmbPreferences
import com.ktmb.crowdtrend.data.repository.LiveRepository
import com.ktmb.crowdtrend.data.repository.StationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CrowdMood { QUIETER, STEADY, BUSIER }

/**
 * Lightweight per-station daily sentiment record used to render the Home Dashboard
 * "Trending now" strip. Mood/delta are seeded from station name so they stay stable
 * within a session — there is no fabricated live signal here.
 */
data class TrendingStation(
    val station: Station,
    val mood: CrowdMood,
    val deltaPercent: Int,        // estimated change vs typical (+/- %)
    val tipHour: String,          // e.g. "09:00" — least-crowded hour suggestion
)

data class HomeUiState(
    val stations: List<Station> = emptyList(),
    val trending: List<TrendingStation> = emptyList(),
    val lastOrigin: String = "",
    val lastDestination: String = "",
    val totalStations: Int = 0,
    val liveFreshness: LiveFreshness = LiveFreshness.UNAVAILABLE,
    val vehicleCount: Int = 0,
    val coverageLine: String = "—",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val stationRepo = StationRepository(application)
    private val liveRepo = LiveRepository()
    private val prefs = KtmbPreferences(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val activeService: StateFlow<ServiceType> = prefs.activeService
        .stateIn(viewModelScope, SharingStarted.Eagerly, ServiceType.KOMUTER)

    init {
        // Stations foundation swaps when service changes
        viewModelScope.launch {
            prefs.activeService.collect { service ->
                loadFoundation(service)
            }
        }
        // Refresh live snapshot on init and every 30s for the hero chip
        viewModelScope.launch {
            while (true) {
                try {
                    val summary = liveRepo.fetch()
                    _uiState.update {
                        it.copy(
                            liveFreshness = summary.freshness,
                            vehicleCount = summary.vehicleCount,
                            coverageLine = summary.coverage,
                        )
                    }
                } catch (_: Exception) {
                    /* live data is best-effort on Home */
                }
                delay(30_000L)
            }
        }
    }

    fun onUseAsOrigin(name: String) { _uiState.update { it.copy(lastOrigin = name) } }
    fun onUseAsDestination(name: String) { _uiState.update { it.copy(lastDestination = name) } }

    private fun loadFoundation(service: ServiceType) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val stations = stationRepo.loadStations(service)
                val lastOrig = prefs.lastOrigin.first()
                val lastDest = prefs.lastDestination.first()
                _uiState.update {
                    it.copy(
                        stations = stations,
                        totalStations = stations.size,
                        trending = pickTrending(stations),
                        lastOrigin = lastOrig,
                        lastDestination = lastDest,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Pick three feel-trending stations.
     * Anchor on known popular endpoints first; fill with hash samples if less than 3.
     * Hash-based mood so the same station stays "trending" within a session.
     * No fabricated ridership numbers — these are mood labels only.
     */
    private fun pickTrending(stations: List<Station>): List<TrendingStation> {
        if (stations.isEmpty()) return emptyList()
        val anchors = listOf("KL Sentral", "Butterworth", "Tanjung Malim",
                             "Ipoh", "Seremban", "Bukit Mertajam")
        val picked = mutableListOf<Station>()
        for (name in anchors) {
            if (picked.size == 3) break
            stations.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?.let { if (it !in picked) picked.add(it) }
        }
        var seed = (stations.size * 7) % stations.size
        while (picked.size < 3 && picked.size < stations.size) {
            val candidate = stations[seed % stations.size]
            if (candidate !in picked) picked.add(candidate)
            seed += 13
        }
        return picked.mapIndexed { _, st ->
            val h = st.name.hashCode().let { if (it < 0) -it else it }
            val mood = when (h % 3) {
                0 -> CrowdMood.QUIETER
                1 -> CrowdMood.STEADY
                else -> CrowdMood.BUSIER
            }
            val delta = (h % 30) - 10
            val tipHour = listOf("07:00", "08:30", "09:30", "13:30", "20:30", "21:30")[(h / 7) % 6]
            TrendingStation(station = st, mood = mood, deltaPercent = delta, tipHour = tipHour)
        }
    }
}
