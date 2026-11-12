package com.musta.iptv.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

object Http {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /** Blocking GET. Call from a background thread. */
    fun get(url: String, headers: Map<String, String> = emptyMap()): Response {
        val builder = Request.Builder().url(url)
        for ((k, v) in headers) builder.header(k, v)
        return client.newCall(builder.build()).execute()
    }
}
