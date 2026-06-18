package com.ktmb.crowdtrend.feature.stations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.model.Station
import com.ktmb.crowdtrend.data.datastore.KtmbPreferences
import com.ktmb.crowdtrend.data.repository.StationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StationsUiState(
    val stations: List<Station> = emptyList(),
    val searchQuery: String = "",
    val selectedStation: Station? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class StationGroup(
    val state: String,
    val stations: List<Station>,
)

class StationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = StationRepository(application)
    private val prefs = KtmbPreferences(application)

    private val _uiState = MutableStateFlow(StationsUiState())
    val uiState: StateFlow<StationsUiState> = _uiState.asStateFlow()

    val activeService: StateFlow<ServiceType> = prefs.activeService
        .stateIn(viewModelScope, SharingStarted.Eagerly, ServiceType.KOMUTER)

    /** Stations grouped by state, filtered by search query. */
    val groupedStations: StateFlow<List<StationGroup>> = combine(
        _uiState, prefs.activeService
    ) { state, _ ->
        groupByState(state.stations, state.searchQuery)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load stations whenever the active service changes
        viewModelScope.launch {
            prefs.activeService.collect { service ->
                loadStationsFor(service)
            }
        }
    }

    fun onSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, selectedStation = null) }
    }

    fun onSelectStation(station: Station) {
        _uiState.update { it.copy(selectedStation = station) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedStation = null) }
    }

    // ── Private ──

    private fun loadStationsFor(service: ServiceType) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val stations = repo.loadStations(service)
                _uiState.update {
                    it.copy(
                        stations = stations,
                        isLoading = false,
                        error = null,
                        selectedStation = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load stations: ${e.message}",
                    )
                }
            }
        }
    }

    private fun groupByState(
        stations: List<Station>,
        query: String,
    ): List<StationGroup> {
        val filtered = if (query.isBlank()) stations
        else stations.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.state.contains(query, ignoreCase = true)
        }

        val stateOrder = listOf(
            "Kuala Lumpur", "Selangor", "Negeri Sembilan", "Perak",
            "Kedah", "Perlis", "Johor", "Pulau Pinang", "Melaka", "Other"
        )

        val grouped = LinkedHashMap<String, MutableList<Station>>()
        stateOrder.forEach { grouped[it] = mutableListOf() }
        filtered.forEach { station ->
            grouped.getOrPut(station.state) { mutableListOf() }.add(station)
        }
        return grouped
            .filter { it.value.isNotEmpty() }
            .map { (state, sts) -> StationGroup(state, sts.sortedBy { it.name }) }
    }
}
