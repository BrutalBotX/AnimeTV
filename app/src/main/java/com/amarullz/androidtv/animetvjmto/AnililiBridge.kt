package com.amarullz.androidtv.animetvjmto

import android.util.Log
import com.miruronative.data.ProviderCatalog
import com.miruronative.data.remote.AniListClient
import com.miruronative.data.remote.AnivexaClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object AnililiBridge {

    private const val TAG = "ATVLOG-ANILILI"

    private val client: OkHttpClient by lazy {
        val cacheDir = File(
            AnimeApi.okCacheDir ?: "cacheDir",
            "anilili_okhttp"
        )
        val cache = Cache(cacheDir, 10L * 1024 * 1024)
        val bootstrap = OkHttpClient.Builder().cache(cache).build()
        val doh = DnsOverHttps.Builder()
            .client(bootstrap)
            .url("https://1.1.1.1/dns-query".toHttpUrl())
            .build()
        bootstrap.newBuilder().dns(doh)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }

    private val aniList: AniListClient by lazy { AniListClient(client, json) }
    private val anivexa: AnivexaClient by lazy { AnivexaClient(client, json, aniList) }
    private val mediaCache = HashMap<Int, com.miruronative.data.model.Media>()

    /** Short-timeout client for stream reachability checks. */
    private val probeClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Probe a stream URL the same way ExoPlayer will fetch it.
     * Returns true when the server answers 2xx with the provider's headers.
     */
    private fun probeStream(url: String, referer: String?): Boolean {
        return try {
            val rb = Request.Builder()
                .url(url)
                .header("User-Agent", Conf.USER_AGENT)
                .header("Range", "bytes=0-0")
                .header("Accept", "*/*")
            if (!referer.isNullOrEmpty()) {
                rb.header("Referer", referer)
                rb.header("Origin", referer)
            }
            probeClient.newCall(rb.build()).execute().use { resp ->
                Log.d(TAG, "  probe ${resp.code} <- ${url.take(90)}")
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.d(TAG, "  probe FAIL (${e.javaClass.simpleName}: ${e.message}) <- ${url.take(90)}")
            false
        }
    }

    /** Keep only streams that actually respond; probes run in parallel. */
    private suspend fun filterPlayable(
        streams: List<com.miruronative.data.model.StreamItem>
    ): List<com.miruronative.data.model.StreamItem> {
        // Embed URLs are HTML pages, not media — ExoPlayer can never play them.
        val direct = streams.filter { !it.isEmbed }
        return kotlinx.coroutines.coroutineScope {
            direct.take(6).map { s ->
                async(Dispatchers.IO) { if (probeStream(s.url, s.referer)) s else null }
            }.awaitAll().filterNotNull()
        }
    }

    private fun media(id: Int): com.miruronative.data.model.Media? {
        return mediaCache[id] ?: runBlocking {
            withContext(Dispatchers.IO) { aniList.animeInfo(id) }?.also { mediaCache[id] = it }
        }
    }

    val PROVIDER_NAMES = listOf("senshi", "anikoto", "anibd", "anizone", "anineko", "animegg", "reanime", "2dhive", "anikai")

    @JvmStatic
    fun search(query: String, page: Int): String = runBlocking {
        try {
            val result: List<com.miruronative.data.model.Media> = withContext(Dispatchers.IO) {
                if (query.isEmpty()) aniList.homeCollections().popular
                else aniList.search(query, page, 20).items
            }
            val out = JSONArray()
            for (media in result) {
                val item = JSONObject()
                item.put("url", media.id.toString())
                item.put("slug", media.id.toString())
                item.put("title", media.title?.english ?: media.title?.romaji ?: "")
                item.put("poster", media.coverImage?.large ?: "")
                item.put("totalEp", media.episodes ?: 0)
                item.put("format", media.format ?: "")
                item.put("type", media.format ?: "TV")
                item.put("genres", media.genres?.joinToString() ?: "[]")
                out.put(item)
            }
            out.toString()
        } catch (e: Exception) {
            Log.e(TAG, "search error: ${e.message}", e)
            "[]"
        }
    }

    @JvmStatic
    fun getPopular(page: Int): String = runBlocking {
        try {
            val result: List<com.miruronative.data.model.Media> = withContext(Dispatchers.IO) {
                aniList.homeCollections().popular
            }
            val out = JSONArray()
            for (media in result) {
                val item = JSONObject()
                item.put("url", media.id.toString())
                item.put("slug", media.id.toString())
                item.put("title", media.title?.english ?: media.title?.romaji ?: "")
                item.put("poster", media.coverImage?.large ?: "")
                item.put("totalEp", media.episodes ?: 0)
                item.put("format", media.format ?: "")
                item.put("type", media.format ?: "TV")
                out.put(item)
            }
            out.toString()
        } catch (e: Exception) {
            Log.e(TAG, "getPopular error: ${e.message}", e)
            "[]"
        }
    }

    @JvmStatic
    fun getView(anilistId: Int): String = runBlocking {
        try {
            val media = withTimeout(20000) {
                withContext(Dispatchers.IO) { aniList.animeInfo(anilistId) }
            }
            if (media == null) return@runBlocking """{"status":false,"error":"Anime $anilistId not found"}"""
            mediaCache[anilistId] = media

            val result = JSONObject()
            result.put("status", true)
            result.put("title", media.title?.english ?: media.title?.romaji ?: "")
            result.put("title_jp", media.title?.romaji ?: "")
            result.put("synopsis", media.description ?: "")
            result.put("poster", media.coverImage?.extraLarge ?: media.coverImage?.large ?: "")
            result.put("banner", media.bannerImage ?: "")
            result.put("url", media.id.toString())

            val epCount = media.episodes ?: media.nextAiringEpisode?.episode?.minus(1) ?: 0
            val actualEpCount = if (epCount > 0) epCount else 1
            val episodes = JSONArray()
            for (i in 1..minOf(actualEpCount, 2000)) {
                val ep = JSONObject()
                ep.put("ep", i)
                ep.put("url", "${media.id}#$i")
                ep.put("title", "Episode $i")
                ep.put("filler", false)
                ep.put("active", i == 1)
                ep.put("dub", false)
                ep.put("epuri", "${media.id}#$i")
                episodes.put(ep)
            }
            result.put("episodes", episodes)
            result.put("epavail", actualEpCount)
            result.put("epdub", actualEpCount)
            result.put("genres", media.genres?.joinToString() ?: "[]")
            result.put("type", media.format ?: "TV")
            result.put("rating", (media.averageScore ?: 0).toString())
            result.put("status", media.status ?: "")
            result.put("streamtype", "sub")
            result.put("related", JSONArray())
            result.put("seasons", JSONArray())
            result.put("recs", JSONArray())
            result.put("info", JSONObject().put("type", "").put("rating", "").put("quality", ""))
            result.put("stream_url", JSONObject()
                .put("hard", "").put("soft", media.id.toString()).put("dub", ""))
            result.put("stream_vurl", "")
            result.put("skip", JSONArray().put(JSONArray().put(0).put(0)).put(JSONArray().put(0).put(0)))
            result.put("servers", JSONObject()
                .put("sub", JSONArray().put(JSONArray().put("Anilili").put(0)))
                .put("dub", JSONArray())
                .put("softsub", JSONArray()))
            result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "getView error: ${e.message}", e)
            """{"status":false,"error":"${e.message?.replace("\"", "\\\"")}"}"""
        }
    }

    @JvmStatic
    fun getVideoSources(anilistId: Int, episodeNum: Int, category: String): String = runBlocking {
        val cat = if (category == "dub") "dub" else "sub"
        var seedMedia = mediaCache[anilistId]
        val errors = mutableListOf<String>()

        Log.d(TAG, "getVideoSources: id=$anilistId ep=$episodeNum cat=$cat")
        if (seedMedia == null) {
            Log.w(TAG, "getVideoSources: media cache cold for $anilistId; fetching from AniList...")
            seedMedia = try {
                withTimeout(15000) {
                    withContext(Dispatchers.IO) { aniList.animeInfo(anilistId) }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "getVideoSources: emergency AniList fetch timed out")
                null
            } catch (e: Exception) {
                Log.e(TAG, "getVideoSources: emergency AniList fetch failed: ${e.message}")
                null
            }
            if (seedMedia != null) {
                mediaCache[anilistId] = seedMedia
                Log.d(TAG, "getVideoSources: emergency fetch succeeded")
            } else {
                return@runBlocking """{"status":false,"error":"Media data not loaded. Please try again."}"""
            }
        }
        for (provider in PROVIDER_NAMES) {
            if (provider == "senshi" && seedMedia.idMal == null) {
                Log.w(TAG, "  $provider: SKIPPED - no idMal (AniList entry missing MAL mapping)")
                errors.add("$provider(skipped:no mal id)")
                continue
            }
            val episodeId = "watch/$provider/$anilistId/$cat/$provider-$episodeNum"
            val result = kotlin.runCatching {
                withTimeout(10000) {
                    withContext(Dispatchers.IO) { anivexa.getSources(episodeId, seedMedia) }
                }
            }
            if (result.isFailure) {
                val exc = result.exceptionOrNull()
                val err = when (exc) {
                    is TimeoutCancellationException -> "timeout(10s)"
                    else -> exc?.message ?: "unknown"
                }
                Log.d(TAG, "  $provider: FAILED - $err")
                errors.add("$provider($err)")
                continue
            }
            val sources = result.getOrNull()
            if (sources != null && sources.streams.isNotEmpty()) {
                Log.d(TAG, "  $provider: ${sources.streams.size} stream(s) resolved, probing...")
                val playable = try {
                    withTimeout(15000) { filterPlayable(sources.streams) }
                } catch (e: Exception) {
                    Log.w(TAG, "  $provider: probe interrupted (${e.javaClass.simpleName})")
                    emptyList()
                }
                if (playable.isNotEmpty()) {
                    Log.d(TAG, "  $provider: OK - ${playable.size} playable stream(s)")
                    return@runBlocking buildSourceJson(sources, playable)
                }
                Log.w(TAG, "  $provider: all streams failed probe, trying next provider")
                errors.add("$provider(dead-streams)")
                continue
            }
            Log.d(TAG, "  $provider: no streams")
            errors.add("$provider(empty)")
        }
        Log.w(TAG, "getVideoSources: ALL FAILED - ${errors.joinToString("; ")}")
        """{"status":false,"error":"No sources. Tried: ${errors.joinToString(", ")}"}"""
    }

    private fun buildSourceJson(
        result: com.miruronative.data.model.SourcesResult,
        streams: List<com.miruronative.data.model.StreamItem> = result.streams
    ): String {
        val out = JSONObject()
        val sources = JSONArray()
        for (stream in streams) {
            val src = JSONObject()
            src.put("file", stream.url)
            src.put("quality", stream.quality ?: "auto")
            src.put("type", stream.type)
            if (stream.referer != null) src.put("referer", stream.referer)
            sources.put(src)
        }
        val tracks = JSONArray()
        for (sub in result.subtitles) {
            val track = JSONObject()
            track.put("file", sub.url)
            track.put("label", sub.label)
            track.put("lang", sub.language)
            track.put("kind", "captions")
            tracks.put(track)
        }
        out.put("sources", sources)
        out.put("subtitles", tracks)
        out.put("skip", JSONArray()
            .put(JSONArray().put(result.skip?.introStart ?: 0).put(result.skip?.introEnd ?: 0))
            .put(JSONArray().put(result.skip?.outroStart ?: 0).put(result.skip?.outroEnd ?: 0)))
        return out.toString()
    }
}
