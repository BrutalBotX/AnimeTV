package com.miruronative.data.cache

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppCache(context: Context, json: Json) {
    private val cache = mutableMapOf<String, CacheEntry>()
    private val mutex = Mutex()

    data class CacheEntry(val key: String, val payload: String, val expiresAt: Long)

    suspend fun <T> getOrFetch(
        key: String,
        serializer: KSerializer<T>,
        ttlMs: Long,
        block: suspend () -> T
    ): T {
        mutex.withLock {
            val entry = cache[key]
            if (entry != null && System.currentTimeMillis() < entry.expiresAt) {
                return@withLock
            }
        }
        val value = block()
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(serializer, value)
        mutex.withLock {
            cache[key] = CacheEntry(key, jsonStr, System.currentTimeMillis() + ttlMs)
        }
        return value
    }

    fun clear() {
        cache.clear()
    }
}
