package com.ktmb.crowdtrend.core.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

object AssetJsonLoader {

    @Volatile
    private var remoteDataLoader: RemoteDataLoader? = null

    @Volatile
    private var latestInfo: DataSourceInfo? = null

    private val sourceInfoByPath = ConcurrentHashMap<String, DataSourceInfo>()
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun configure(remoteDataLoader: RemoteDataLoader) {
        this.remoteDataLoader = remoteDataLoader
    }

    fun lastSourceInfo(): DataSourceInfo? = latestInfo

    fun sourceInfoFor(path: String): DataSourceInfo? {
        val normalizedPath = path.trimStart('/')
        return sourceInfoByPath[normalizedPath] ?: remoteDataLoader?.cachedInfo(normalizedPath)
    }

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    inline fun <reified T> load(context: Context, path: String): T {
        val raw = readRaw(context, path)
        return runCatching { json.decodeFromString<T>(raw) }
            .getOrElse { decodeError ->
                val info = sourceInfoFor(path)
                if (info?.freshnessState == FreshnessState.REMOTE_CACHED) {
                    val bundled = readAsset(context, path)
                    val fallbackInfo = staleInfo(path, decodeError.message)
                    rememberSourceInfo(fallbackInfo)
                    json.decodeFromString(bundled)
                } else {
                    throw decodeError
                }
            }
    }

    fun readRaw(context: Context, path: String): String {
        val loader = remoteDataLoader
        val cached = loader?.readCached(path)
        if (cached != null) {
            rememberSourceInfo(cached.info)
            refreshInBackground(loader, path, cached.info)
            return cached.text
        }

        return try {
            val bundled = readAsset(context, path)
            val info = bundledInfo(path)
            rememberSourceInfo(info)
            if (loader != null) {
                refreshInBackground(loader, path, info)
            }
            bundled
        } catch (assetError: IOException) {
            rememberSourceInfo(staleInfo(path, assetError.message))
            throw assetError
        }
    }

    fun readRawOrNull(context: Context, path: String): String? {
        return try {
            readRaw(context, path)
        } catch (_: IOException) {
            null
        }
    }

    @PublishedApi
    internal fun readAsset(context: Context, path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }

    private fun refreshInBackground(
        loader: RemoteDataLoader,
        path: String,
        previousInfo: DataSourceInfo?,
    ) {
        refreshScope.launch {
            runCatching { loader.refresh(path) }
                .onSuccess { rememberSourceInfo(it) }
                .onFailure {
                    val fallbackState = if (previousInfo?.freshnessState == FreshnessState.REMOTE_CACHED) {
                        FreshnessState.REMOTE_CACHED
                    } else {
                        FreshnessState.ERROR_USING_STALE_DATA
                    }
                    rememberSourceInfo(
                        previousInfo?.copy(
                            freshnessState = fallbackState,
                            warning = it.message,
                        ) ?: staleInfo(path, it.message)
                    )
                }
        }
    }

    @PublishedApi
    internal fun rememberSourceInfo(info: DataSourceInfo) {
        latestInfo = info
        sourceInfoByPath[info.path] = info
    }

    @PublishedApi
    internal fun bundledInfo(path: String, warning: String? = null): DataSourceInfo = DataSourceInfo(
        freshnessState = FreshnessState.BUNDLED_FALLBACK,
        path = path.trimStart('/'),
        lastFetchTimeMillis = 0L,
        warning = warning,
    )

    @PublishedApi
    internal fun staleInfo(path: String, warning: String? = null): DataSourceInfo = DataSourceInfo(
        freshnessState = FreshnessState.ERROR_USING_STALE_DATA,
        path = path.trimStart('/'),
        lastFetchTimeMillis = 0L,
        warning = warning,
    )
}
