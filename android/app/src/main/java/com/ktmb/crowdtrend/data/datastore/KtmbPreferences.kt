package com.ktmb.crowdtrend.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ktmb.crowdtrend.core.model.ServiceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ktmb_prefs")

class KtmbPreferences(private val context: Context) {

    companion object {
        private val KEY_SERVICE = stringPreferencesKey("service")
        private val KEY_ORIGIN = stringPreferencesKey("last_origin")
        private val KEY_DEST = stringPreferencesKey("last_destination")
    }

    val activeService: Flow<ServiceType> = context.dataStore.data.map { prefs ->
        ServiceType.fromKey(prefs[KEY_SERVICE] ?: "komuter")
    }

    val lastOrigin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ORIGIN] ?: ""
    }

    val lastDestination: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEST] ?: ""
    }

    suspend fun setActiveService(service: ServiceType) {
        context.dataStore.edit { it[KEY_SERVICE] = service.key }
    }

    suspend fun setLastRoute(origin: String, destination: String) {
        context.dataStore.edit {
            it[KEY_ORIGIN] = origin
            it[KEY_DEST] = destination
        }
    }
}
