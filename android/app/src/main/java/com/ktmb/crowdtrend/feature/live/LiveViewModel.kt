package com.ktmb.crowdtrend.feature.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

data class LiveUiState(
    val summary: LiveSummary = LiveSummary.EMPTY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastFetchTime: String = "",
)

class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LiveRepository()

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var lastSuccessTime: Long = 0L

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
            _uiState.update {
                it.copy(
                    summary = summary,
                    isLoading = false,
                    error = null,
                    lastFetchTime = LiveRepository.formatTimestamp(lastSuccessTime),
                )
            }
        } catch (e: Exception) {
            // Compute degradation: how long since last success?
            val degraded = computeDegradedState()
            _uiState.update {
                it.copy(
                    summary = degraded,
                    isLoading = false,
                    error = if (degraded.freshness == LiveFreshness.UNAVAILABLE)
                        "Unable to reach live feed"
                    else
                        null,
                )
            }
        }
    }

    /**
     * Compute freshness degradation based on time since last successful fetch.
     * - < 60s:      still FRESH (keep last data, mark STALE at worst)
     * - 60–300s:    STALE
     * - > 300s:     EXPIRED
     * - never:      UNAVAILABLE
     */
    private fun computeDegradedState(): LiveSummary {
        if (lastSuccessTime == 0L) {
            return LiveSummary.EMPTY
        }
        val elapsed = System.currentTimeMillis() - lastSuccessTime
        val freshness = when {
            elapsed < 60_000 -> LiveFreshness.FRESH
            elapsed < 300_000 -> LiveFreshness.STALE
            else -> LiveFreshness.EXPIRED
        }
        return _uiState.value.summary.copy(freshness = freshness)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
