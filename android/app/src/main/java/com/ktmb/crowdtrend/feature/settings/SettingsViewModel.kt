package com.ktmb.crowdtrend.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktmb.crowdtrend.core.model.ServiceType
import com.ktmb.crowdtrend.data.datastore.KtmbPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = KtmbPreferences(application)

    val activeService: StateFlow<ServiceType> = prefs.activeService
        .stateIn(viewModelScope, SharingStarted.Eagerly, ServiceType.KOMUTER)

    fun setService(service: ServiceType) {
        viewModelScope.launch {
            prefs.setActiveService(service)
        }
    }
}
