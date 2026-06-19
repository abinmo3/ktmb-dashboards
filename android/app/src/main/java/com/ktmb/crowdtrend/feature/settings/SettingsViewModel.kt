package com.ktmb.crowdtrend.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktmb.crowdtrend.core.model.FeedState
import com.ktmb.crowdtrend.core.model.MetaJson
import com.ktmb.crowdtrend.core.model.RidershipStatus
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.core.util.AssetJsonLoader
import com.ktmb.crowdtrend.core.util.DataSourceInfo
import com.ktmb.crowdtrend.data.datastore.KtmbPreferences
import com.ktmb.crowdtrend.data.repository.ForecastRepository
import com.ktmb.crowdtrend.data.repository.LiveRepository
import com.ktmb.crowdtrend.data.repository.RidershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Per-source freshness state displayed on the Settings screen.
 */
data class SourceFreshness(
    val name: String,
    val status: String,
    val detail: String,
    val isHealthy: Boolean,
)

data class SettingsUiState(
    val ridershipStatus: RidershipStatus = RidershipStatus(
        latestDate = "",
        rowCount = 0,
        freshness = com.ktmb.crowdtrend.core.model.RidershipFreshness.STALE,
        source = "",
    ),
    val liveFeedState: FeedState = FeedState.FEED_ERROR,
    val forecastMeta: MetaJson? = null,
    val forecastSourceInfo: DataSourceInfo? = null,
    val isLoading: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = KtmbPreferences(application)
    private val ridershipRepo = RidershipRepository(application)
    private val liveRepo = LiveRepository()
    private val forecastRepo = ForecastRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val activeService: StateFlow<ServiceType> = prefs.activeService
        .stateIn(viewModelScope, SharingStarted.Eagerly, ServiceType.KOMUTER)

    init {
        loadFreshness()
    }

    fun setService(service: ServiceType) {
        viewModelScope.launch {
            prefs.setActiveService(service)
            loadForecastMeta(service)
        }
    }

    fun refresh() {
        loadFreshness()
    }

    // ── Private ──

    private fun loadFreshness() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Load ridership data
            val ridership = ridershipRepo.fetchStatus()
            _uiState.update { it.copy(ridershipStatus = ridership) }

            // Check live feed (best-effort)
            try {
                val summary = liveRepo.fetch()
                _uiState.update { it.copy(liveFeedState = summary.feedState) }
            } catch (_: Exception) {
                _uiState.update { it.copy(liveFeedState = FeedState.FEED_ERROR) }
            }

            // Load forecast meta for current service
            loadForecastMeta(activeService.value)

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadForecastMeta(service: ServiceType) {
        try {
            val meta = forecastRepo.loadMeta(service)
            val sourceInfo = AssetJsonLoader.sourceInfoFor("${dataPrefix(service)}meta.json")
            _uiState.update { it.copy(forecastMeta = meta, forecastSourceInfo = sourceInfo) }
        } catch (_: Exception) {
            // meta is best-effort
        }
    }

    private fun dataPrefix(service: ServiceType): String {
        return when (service) {
            ServiceType.KOMUTER -> "data/"
            ServiceType.KOMUTER_UTARA -> "data/komuter_utara/"
            ServiceType.ETS -> "data/ets/"
            ServiceType.INTERCITY -> "data/intercity/"
        }
    }
}
