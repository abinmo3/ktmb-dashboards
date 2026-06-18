package com.ktmb.crowdtrend

import android.app.Application

class KtmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // DataStore and ViewModels handle state lazily.
        // No DI framework needed for MVP.
    }
}
