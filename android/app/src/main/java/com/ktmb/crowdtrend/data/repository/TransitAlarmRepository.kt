package com.ktmb.crowdtrend.data.repository

import android.content.Context
import com.ktmb.crowdtrend.core.model.TransitAlarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists [TransitAlarm]s in app-private file storage as a JSON array.
 * Lightweight — no Room needed for ~10 alarms.
 */
class TransitAlarmRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _alarms = MutableStateFlow<List<TransitAlarm>>(emptyList())
    val alarms: StateFlow<List<TransitAlarm>> = _alarms.asStateFlow()

    init {
        loadFromDisk()
    }

    fun add(alarm: TransitAlarm) {
        val updated = _alarms.value.toMutableList().apply { add(alarm) }
        _alarms.value = updated
        saveToDisk(updated)
    }

    fun remove(id: String) {
        val updated = _alarms.value.filter { it.id != id }
        _alarms.value = updated
        saveToDisk(updated)
    }

    fun toggle(id: String) {
        val updated = _alarms.value.map {
            if (it.id == id) it.copy(isActive = !it.isActive) else it
        }
        _alarms.value = updated
        saveToDisk(updated)
    }

    fun updateRadius(id: String, radiusMeters: Int) {
        val updated = _alarms.value.map {
            if (it.id == id) it.copy(radiusMeters = radiusMeters) else it
        }
        _alarms.value = updated
        saveToDisk(updated)
    }

    private fun loadFromDisk() {
        try {
            val raw = context.openFileInput(FILE_NAME).use { it.readBytes().decodeToString() }
            val decoded: List<AlarmDto> = json.decodeFromString(raw)
            _alarms.value = decoded.map { it.toDomain() }
        } catch (_: Exception) {
            _alarms.value = emptyList()
        }
    }

    private fun saveToDisk(alarms: List<TransitAlarm>) {
        try {
            val raw = json.encodeToString(alarms.map { AlarmDto.fromDomain(it) })
            context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(raw.encodeToByteArray())
            }
        } catch (_: Exception) {
            // best-effort persistence
        }
    }

    companion object {
        private const val FILE_NAME = "transit_alarms.json"
    }
}

@Serializable
private data class AlarmDto(
    val id: String,
    val stationName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val isActive: Boolean,
    val label: String,
    val createdAt: Long,
) {
    fun toDomain() = TransitAlarm(
        id = id,
        stationName = stationName,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        isActive = isActive,
        label = label,
        createdAt = createdAt,
    )
    companion object {
        fun fromDomain(a: TransitAlarm) = AlarmDto(
            id = a.id,
            stationName = a.stationName,
            latitude = a.latitude,
            longitude = a.longitude,
            radiusMeters = a.radiusMeters,
            isActive = a.isActive,
            label = a.label,
            createdAt = a.createdAt,
        )
    }
}
