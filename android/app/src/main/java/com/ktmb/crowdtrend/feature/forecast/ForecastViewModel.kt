package com.ktmb.crowdtrend.feature.forecast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktmb.crowdtrend.core.model.*
import com.ktmb.crowdtrend.data.datastore.KtmbPreferences
import com.ktmb.crowdtrend.data.repository.ForecastRepository
import com.ktmb.crowdtrend.data.repository.StationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ── UI State ──

data class ForecastUiState(
    val stations: List<Station> = emptyList(),
    val origin: Station? = null,
    val destination: Station? = null,
    val availableDestinations: List<Station> = emptyList(),
    val forecast: Forecast? = null,
    val forecastMode: ForecastMode = ForecastMode.LATEST,
    val isLoading: Boolean = false,
    val isSwapping: Boolean = false,
    val error: String? = null,
    val freshness: DataFreshness = DataFreshness.EMPTY,
)

enum class ForecastMode { LATEST, TYPICAL }

// ── ViewModel ──

class ForecastViewModel(application: Application) : AndroidViewModel(application) {

    private val stationRepo = StationRepository(application)
    private val forecastRepo = ForecastRepository(application)
    private val prefs = KtmbPreferences(application)

    private val _uiState = MutableStateFlow(ForecastUiState())
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    val activeService: StateFlow<ServiceType> = prefs.activeService
        .stateIn(viewModelScope, SharingStarted.Eagerly, ServiceType.KOMUTER)

    init {
        // Load stations + restore last route when service changes
        viewModelScope.launch {
            prefs.activeService.collect { service ->
                loadFoundation(service)
            }
        }
    }

    // ── Actions ──

    fun onOriginSelected(station: Station) {
        val state = _uiState.value
        // Compute available destinations: all stations except origin
        val available = state.stations.filter { it.name != station.name }
        _uiState.update { it.copy(origin = station, availableDestinations = available, forecast = null, error = null) }
    }

    fun onDestinationSelected(station: Station) {
        _uiState.update { it.copy(destination = station) }
        loadForecast()
    }

    fun swapRoute() {
        val state = _uiState.value
        val orig = state.origin ?: return
        val dest = state.destination ?: return
        _uiState.update {
            it.copy(
                isSwapping = true,
                origin = dest,
                destination = orig,
                availableDestinations = state.stations.filter { s -> s.name != dest.name },
                forecast = null,
                error = null,
            )
        }
        loadForecast()
    }

    fun setForecastMode(mode: ForecastMode) {
        _uiState.update { it.copy(forecastMode = mode) }
    }

    fun setService(service: ServiceType) {
        viewModelScope.launch { prefs.setActiveService(service) }
        // loadFoundation is triggered by activeService collector
    }

    // ── Window computation (used by UI) ──

    companion object {
        /**
         * Find the hours with the lowest crowd values.
         * Returns up to [count] hour indices (0–23), sorted by value ascending.
         * Null values are excluded.
         */
        fun bestWindows(values: List<Double?>, count: Int): List<Int> {
            return values.mapIndexedNotNull { h, v -> if (v != null) h to v else null }
                .sortedBy { it.second }
                .take(count)
                .map { it.first }
        }

        /**
         * Find the hours with the highest crowd values.
         * Returns up to [count] hour indices (0–23), sorted by value descending.
         */
        fun avoidWindows(values: List<Double?>, count: Int): List<Int> {
            return values.mapIndexedNotNull { h, v -> if (v != null) h to v else null }
                .sortedByDescending { it.second }
                .take(count)
                .map { it.first }
        }

        fun formatHour(hour: Int): String = "${hour.toString().padStart(2, '0')}:00"
    }

    // ── Private ──

    private suspend fun loadFoundation(service: ServiceType) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val stations = stationRepo.loadStations(service)
            val meta = forecastRepo.loadMeta(service)

            // Compute freshness
            val freshness = computeFreshness(meta)

            // Restore last route or pick defaults
            val lastOrigin = prefs.lastOrigin.first()
            val lastDest = prefs.lastDestination.first()

            val defaultOrigin = stations.firstOrNull { it.name == lastOrigin }
                ?: defaultOriginFor(service, stations)
            val available = stations.filter { it.name != defaultOrigin?.name }

            val defaultDest = if (lastDest.isNotEmpty())
                available.firstOrNull { it.name == lastDest }
            else null
                ?: defaultDestFor(service, available)

            _uiState.update {
                it.copy(
                    stations = stations,
                    origin = defaultOrigin,
                    destination = defaultDest,
                    availableDestinations = available,
                    isLoading = false,
                    freshness = freshness,
                )
            }

            // Auto-load forecast if both origin and destination are set
            if (defaultOrigin != null && defaultDest != null) {
                loadForecast()
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, error = "Failed to load: ${e.message}")
            }
        }
    }

    private fun loadForecast() {
        val state = _uiState.value
        val service = activeService.value
        val origin = state.origin ?: return
        val destination = state.destination ?: return

        _uiState.update { it.copy(isLoading = true, error = null, isSwapping = false) }

        viewModelScope.launch {
            try {
                val fc = forecastRepo.loadForecast(service, origin, destination)
                if (fc == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            forecast = null,
                            error = "No historical data for ${origin.name} → ${destination.name}. Try a different route."
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, forecast = fc, error = null) }
                    // Persist route
                    prefs.setLastRoute(origin.name, destination.name)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, forecast = null, error = "Failed to load forecast: ${e.message}")
                }
            }
        }
    }

    private fun defaultOriginFor(service: ServiceType, stations: List<Station>): Station? {
        return when (service) {
            ServiceType.KOMUTER -> stations.firstOrNull { it.name == "KL Sentral" }
            ServiceType.KOMUTER_UTARA -> stations.firstOrNull { it.name == "Butterworth" }
        } ?: stations.firstOrNull()
    }

    private fun defaultDestFor(service: ServiceType, available: List<Station>): Station? {
        return when (service) {
            ServiceType.KOMUTER -> available.firstOrNull { it.name == "Bandar Tasek Selatan" }
            ServiceType.KOMUTER_UTARA -> available.firstOrNull { it.name == "Alor Setar" }
        } ?: available.firstOrNull()
    }

    private fun computeFreshness(meta: MetaJson): DataFreshness {
        return try {
            val latest = LocalDate.parse(meta.latestDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            val daysBehind = ChronoUnit.DAYS.between(latest, today).toInt()
            DataFreshness(
                latestDate = meta.latestDate,
                daysAvailable = meta.daysAvailable,
                generatedAt = meta.generatedAt,
                isStale = daysBehind > 14,
                daysBehind = daysBehind,
            )
        } catch (_: Exception) {
            DataFreshness.EMPTY
        }
    }
}
