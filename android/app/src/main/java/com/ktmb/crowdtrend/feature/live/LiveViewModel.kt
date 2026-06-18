package com.ktmb.crowdtrend.feature.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktmb.crowdtrend.core.model.FeedState
import com.ktmb.crowdtrend.core.model.LiveFreshness
import com.ktmb.crowdtrend.core.model.LiveSummary
import com.ktmb.crowdtrend.data.repository.LiveRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Debug info exposed for the developer debug screen (tap title 5× to reveal).
 */
data class LiveDebugInfo(
    val httpStatus: Int = 0,
    val responseBytes: Int = 0,
    val lastFetchTimeMs: Long = 0L,
    val lastSuccessTimeMs: Long = 0L,
    val entityCount: Int = 0,
    val lastVehicleTimestamp: Long = 0L,
    val cacheAgeSeconds: Long = 0L,
)

data class LiveUiState(
    val summary: LiveSummary = LiveSummary.EMPTY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastFetchTime: String = "",
    val debugInfo: LiveDebugInfo = LiveDebugInfo(),
    val isStaleCache: Boolean = false,
)

class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LiveRepository()

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var lastSuccessTime: Long = 0L

    /** Last known good summary kept in memory for cache fallback. */
    private var cachedSummary: LiveSummary? = null

    init {
        startPolling()
    }

    fun manualRefresh() {
        viewModelScope.launch { fetchNow() }
    }

    // ── Private ──

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            // Initial fetch
            fetchNow()
            // Then poll every 30 seconds
            while (isActive) {
                delay(30_000L)
                fetchNow()
            }
        }
    }

    private suspend fun fetchNow() {
        _uiState.update { it.copy(isLoading = it.summary.vehicleCount == 0, error = null) }
        try {
            val summary = repo.fetch()
            lastSuccessTime = System.currentTimeMillis()

            // Cache the last known good summary
            cachedSummary = summary

            _uiState.update {
                it.copy(
                    summary = summary,
                    isLoading = false,
                    error = null,
                    isStaleCache = false,
                    lastFetchTime = LiveRepository.formatTimestamp(lastSuccessTime),
                    debugInfo = LiveDebugInfo(
                        httpStatus = summary.httpStatus,
                        responseBytes = summary.responseBytes,
                        lastFetchTimeMs = summary.lastFetchTimeMs,
                        lastSuccessTimeMs = summary.lastSuccessTimeMs,
                        entityCount = summary.vehicleCount,
                        lastVehicleTimestamp = summary.lastVehicleTimestamp,
                        cacheAgeSeconds = summary.cacheAgeSeconds,
                    ),
                )
            }
        } catch (e: Exception) {
            // Use cached summary if available, mark as STALE
            val fallback = cachedSummary?.copy(
                freshness = LiveFreshness.EXPIRED,
                feedState = FeedState.STALE,
            )

            if (fallback != null) {
                _uiState.update {
                    it.copy(
                        summary = fallback,
                        isLoading = false,
                        error = null,
                        isStaleCache = true,
                        debugInfo = LiveDebugInfo(
                            httpStatus = fallback.httpStatus,
                            responseBytes = fallback.responseBytes,
                            lastFetchTimeMs = fallback.lastFetchTimeMs,
                            lastSuccessTimeMs = fallback.lastSuccessTimeMs,
                            entityCount = fallback.vehicleCount,
                            lastVehicleTimestamp = fallback.lastVehicleTimestamp,
                            cacheAgeSeconds = (System.currentTimeMillis() - lastSuccessTime) / 1000,
                        ),
                    )
                }
            } else {
                // No cache at all — truly unavailable
                _uiState.update {
                    it.copy(
                        summary = LiveSummary.EMPTY,
                        isLoading = false,
                        error = "Unable to reach live feed",
                        isStaleCache = false,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
