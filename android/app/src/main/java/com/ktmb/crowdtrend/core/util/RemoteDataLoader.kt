package com.ktmb.crowdtrend.core.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class RemoteDataLoader(
    context: Context,
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    private val cacheRoot = File(context.applicationContext.filesDir, CACHE_DIR)

    fun readCached(path: String): RemoteReadResult? {
        val normalizedPath = normalizePath(path)
        val cacheFile = cacheFileFor(normalizedPath)
        if (!cacheFile.exists()) return null
        return RemoteReadResult(
            text = cacheFile.readText(Charsets.UTF_8),
            info = info(
                freshnessState = FreshnessState.REMOTE_CACHED,
                path = normalizedPath,
                lastFetchTimeMillis = cacheFile.lastModified(),
            ),
        )
    }

    fun cachedInfo(path: String): DataSourceInfo? {
        val normalizedPath = normalizePath(path)
        val cacheFile = cacheFileFor(normalizedPath)
        if (!cacheFile.exists()) return null
        return info(
            freshnessState = FreshnessState.REMOTE_CACHED,
            path = normalizedPath,
            lastFetchTimeMillis = cacheFile.lastModified(),
        )
    }

    suspend fun refresh(path: String): DataSourceInfo = withContext(Dispatchers.IO) {
        val normalizedPath = normalizePath(path)
        val body = download(urlFor(normalizedPath))
        validateJson(body)
        writeCacheAtomically(normalizedPath, body)
        info(
            freshnessState = FreshnessState.REMOTE_LIVE,
            path = normalizedPath,
            lastFetchTimeMillis = System.currentTimeMillis(),
        )
    }

    private fun download(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for $url")
            }
            return response.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    private fun validateJson(body: String) {
        if (body.isBlank()) {
            throw IOException("Remote JSON is empty")
        }
        runCatching { Json.parseToJsonElement(body) }
            .getOrElse { throw IOException("Remote JSON is not parseable", it) }
    }

    private fun writeCacheAtomically(path: String, body: String) {
        val cacheFile = cacheFileFor(path)
        cacheFile.parentFile?.mkdirs()
        val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
        tempFile.writeText(body, Charsets.UTF_8)
        if (cacheFile.exists() && !cacheFile.delete()) {
            tempFile.delete()
            throw IOException("Could not replace cached data at $path")
        }
        if (!tempFile.renameTo(cacheFile)) {
            tempFile.delete()
            throw IOException("Could not move refreshed data into cache for $path")
        }
    }

    private fun cacheFileFor(path: String): File = File(cacheRoot, path)

    private fun normalizePath(path: String): String = path.trimStart('/')

    private fun urlFor(path: String): String = baseUrl.trimEnd('/') + "/" + path.removePrefix("data/")

    private fun info(
        freshnessState: FreshnessState,
        path: String,
        lastFetchTimeMillis: Long,
        warning: String? = null,
    ): DataSourceInfo = DataSourceInfo(
        freshnessState = freshnessState,
        path = path,
        sourceUrl = urlFor(path),
        lastFetchTimeMillis = lastFetchTimeMillis,
        warning = warning,
    )

    companion object {
        private const val CACHE_DIR = "remote_data"
    }
}

data class RemoteReadResult(
    val text: String,
    val info: DataSourceInfo,
)

data class DataSourceInfo(
    val freshnessState: FreshnessState,
    val path: String,
    val sourceUrl: String? = null,
    val lastFetchTimeMillis: Long = 0L,
    val warning: String? = null,
) {
    val displayStatus: String
        get() = freshnessState.displayLabel
}

enum class FreshnessState(val displayLabel: String) {
    REMOTE_LIVE("Live"),
    REMOTE_CACHED("Cached"),
    BUNDLED_FALLBACK("Bundled fallback"),
    ERROR_USING_STALE_DATA("Remote failed"),
}
