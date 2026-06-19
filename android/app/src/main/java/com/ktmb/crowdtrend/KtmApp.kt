package com.ktmb.crowdtrend

import android.app.Application
import com.ktmb.crowdtrend.core.util.AssetJsonLoader
import com.ktmb.crowdtrend.core.util.RemoteDataLoader

class KtmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AssetJsonLoader.configure(
            RemoteDataLoader(
                context = this,
                baseUrl = BuildConfig.DATA_BASE_URL,
            )
        )
        // DataStore and ViewModels handle state lazily.
        // No DI framework needed for MVP.
    }
}
