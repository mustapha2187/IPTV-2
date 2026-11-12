package com.musta.iptv.data

import java.io.IOException

object SourceLoader {

    /** Downloads and stores every channel of a source. Returns how many were stored. Blocking. */
    fun load(db: Db, source: Source): Int {
        val rows: List<ChannelRow> = when (source.type) {
            SourceType.M3U -> loadM3u(source.url)
            SourceType.XTREAM -> XtreamClient.loadChannels(source.url, source.username, source.password)
            SourceType.STALKER -> StalkerClient(source.url, source.mac).loadChannels()
        }
        if (rows.isEmpty()) throw IOException("No channels were found in this source")
        return db.replaceChannels(source.id, rows)
    }

    private fun loadM3u(url: String): List<ChannelRow> {
        val rows = ArrayList<ChannelRow>()
        Http.get(url).use { response ->
            if (!response.isSuccessful) throw IOException("Server answered with code ${response.code}")
            val body = response.body ?: throw IOException("Empty answer from the server")
            body.charStream().buffered().use { reader ->
                M3uParser.parse(reader) { row -> rows.add(row) }
            }
        }
        return rows
    }
}
