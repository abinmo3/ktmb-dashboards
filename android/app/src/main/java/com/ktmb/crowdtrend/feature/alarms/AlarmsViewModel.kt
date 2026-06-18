package com.ktmb.crowdtrend.feature.alarms

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktmb.crowdtrend.core.model.Station
import com.ktmb.crowdtrend.data.repository.StationRepository
import com.ktmb.crowdtrend.data.repository.TransitAlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlarmsUiState(
    val stations: List<Station> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Station> = emptyList(),
    val isSearchActive: Boolean = false,
    val isServiceRunning: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val error: String? = null,
)

class AlarmsViewModel(application: Application) : AndroidViewModel(application) {

    private val stationRepo = StationRepository(application)
    private val alarmRepo = TransitAlarmRepository(application)

    private val _uiState = MutableStateFlow(AlarmsUiState())
    val uiState: StateFlow<AlarmsUiState> = _uiState.asStateFlow()

    val alarms = alarmRepo.alarms
    val activeService = stationRepo.activeService
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.ktmb.crowdtrend.core.model.ServiceType.KOMUTER)

    init {
        viewModelScope.launch {
            try {
                val stations = stationRepo.loadStationsForAlarms()
                _uiState.value = _uiState.value.copy(stations = stations)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
        refreshServiceState()
    }

    fun onSearchQueryChanged(query: String) {
        val trimmed = query.trim()
        _uiState.value = _uiState.value.copy(
            searchQuery = trimmed,
            isSearchActive = trimmed.isNotEmpty(),
            searchResults = if (trimmed.length >= 2) {
                _uiState.value.stations.filter {
                    it.name.contains(trimmed, ignoreCase = true)
                }.take(20)
            } else emptyList(),
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "", isSearchActive = false, searchResults = emptyList())
    }

    fun createAlarm(station: Station) {
        viewModelScope.launch {
            val coords = stationRepo.getStationCoords(station.name)
            if (coords == null) {
                _uiState.value = _uiState.value.copy(error = "Coordinates not available for ${station.name}")
                return@launch
            }
            val alarm = com.ktmb.crowdtrend.core.model.TransitAlarm(
                id = "alarm_${station.name.hashCode()}_${System.currentTimeMillis()}",
                stationName = station.name,
                latitude = coords.lat,
                longitude = coords.lon,
                radiusMeters = 1000,
                label = station.name,
            )
            alarmRepo.add(alarm)
            clearSearch()
            startServiceIfNeeded()
        }
    }

    fun removeAlarm(id: String) {
        alarmRepo.remove(id)
        if (alarmRepo.alarms.value.none { it.isActive }) {
            stopService()
        }
    }

    fun toggleAlarm(id: String) {
        alarmRepo.toggle(id)
        refreshServiceState()
    }

    fun updateRadius(id: String, radiusMeters: Int) {
        alarmRepo.updateRadius(id, radiusMeters)
    }

    fun checkLocationPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission)
        return hasPermission
    }

    private fun refreshServiceState() {
        val hasActive = alarmRepo.alarms.value.any { it.isActive }
        _uiState.value = _uiState.value.copy(isServiceRunning = hasActive)
    }

    private fun startServiceIfNeeded() {
        if (alarmRepo.alarms.value.any { it.isActive }) {
            val intent = Intent(getApplication(), TransitAlarmService::class.java).apply {
                action = TransitAlarmService.ACTION_START
            }
            ContextCompat.startForegroundService(getApplication(), intent)
            _uiState.value = _uiState.value.copy(isServiceRunning = true)
        }
    }

    private fun stopService() {
        val intent = Intent(getApplication(), TransitAlarmService::class.java).apply {
            action = TransitAlarmService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        _uiState.value = _uiState.value.copy(isServiceRunning = false)
    }
}
