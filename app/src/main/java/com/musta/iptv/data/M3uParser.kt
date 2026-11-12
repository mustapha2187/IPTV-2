package com.musta.iptv.data

import java.io.BufferedReader

/** Reads M3U / M3U8 playlists line by line, so a huge file never sits in memory as one string. */
object M3uParser {
    private val attributes = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(reader: BufferedReader, onEntry: (ChannelRow) -> Unit): Int {
        var count = 0
        var hasInfo = false
        var name = ""
        var group = ""
        var logo = ""
        var tvgId = ""
        var number = 0

        reader.forEachLine { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    hasInfo = true
                    group = ""
                    logo = ""
                    tvgId = ""
                    number = 0
                    for (m in attributes.findAll(line)) {
                        val value = m.groupValues[2]
                        when (m.groupValues[1].lowercase()) {
                            "group-title" -> group = value
                            "tvg-logo" -> logo = value
                            "tvg-id" -> tvgId = value
                            "tvg-chno" -> number = value.toIntOrNull() ?: 0
                        }
                    }
                    val afterAttributes = line.substringAfterLast('"', line)
                    name = afterAttributes.substringAfter(',', "").trim()
                }
                line.startsWith("#EXTGRP:") -> if (group.isEmpty()) group = line.substringAfter(':').trim()
                line.isEmpty() || line.startsWith("#") -> Unit
                else -> if (hasInfo) {
                    count++
                    onEntry(
                        ChannelRow(
                            categoryName = group,
                            number = if (number > 0) number else count,
                            name = name.ifEmpty { line },
                            streamUrl = line,
                            logoUrl = logo,
                            tvgId = tvgId,
                            cmd = "",
                        )
                    )
                    hasInfo = false
                }
            }
        }
        return count
    }
}
