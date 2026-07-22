package com.miruronative.data

import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.File
import java.util.concurrent.TimeUnit

object AppGraph {
    lateinit var httpClient: OkHttpClient
        private set

    var isTv: Boolean = true
        private set

    fun init() {
        if (::httpClient.isInitialized) return
        val cache = Cache(File("cacheDir/anilili_appgraph"), 5L * 1024 * 1024)
        val bootstrap = OkHttpClient.Builder().cache(cache).build()
        val doh = DnsOverHttps.Builder()
            .client(bootstrap)
            .url("https://1.1.1.1/dns-query".toHttpUrl())
            .build()
        httpClient = bootstrap.newBuilder().dns(doh)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
