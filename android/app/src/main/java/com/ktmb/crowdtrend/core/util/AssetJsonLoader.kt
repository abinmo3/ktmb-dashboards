package com.ktmb.crowdtrend.core.util

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Reads and parses JSON files from Android assets.
 * All parsing errors surface as clear exceptions — no silent nulls.
 */
object AssetJsonLoader {

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Read a text file from assets and decode it as [T].
     *
     * @param context  Android context for asset access.
     * @param path     Asset path relative to assets/ (e.g. "data/stations.json").
     * @return         Deserialized object of type [T].
     * @throws IOException          if the asset file is missing or unreadable.
     * @throws kotlinx.serialization.SerializationException  if JSON is malformed.
     */
    inline fun <reified T> load(context: Context, path: String): T {
        val raw = readAsset(context, path)
        return json.decodeFromString(raw)
    }

    /**
     * Read a text file from assets and return the raw string.
     * Useful for debugging or passing to external parsers.
     */
    fun readRaw(context: Context, path: String): String = readAsset(context, path)

    /**
     * Read a text file from assets, returning null instead of throwing on missing files.
     * Use this for optional data sources that may not exist in all service directories.
     */
    fun readRawOrNull(context: Context, path: String): String? {
        return try {
            readAsset(context, path)
        } catch (_: IOException) {
            null
        }
    }

    // ── Internal ──

    @PublishedApi
    internal fun readAsset(context: Context, path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}
