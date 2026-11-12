package com.musta.iptv.data

import android.net.Uri

/**
 * Reads a list of servers, one per line. Accepted lines:
 *   http://host/playlist.m3u                      -> M3U
 *   http://host/get.php?username=U&password=P...  -> Xtream
 *   http://host:8080 user pass                    -> Xtream (separators: space, |, comma, semicolon)
 *   http://portal/c/ 00:1A:79:AA:BB:CC            -> Stalker (MAC may come first)
 * Empty lines and lines starting with # or // are ignored.
 */
object BulkParser {
    data class Result(val drafts: List<SourceDraft>, val skipped: Int)

    private val macPattern = Regex("^[0-9A-Fa-f]{2}([:-][0-9A-Fa-f]{2}){5}$")
    private val splitter = Regex("[|,;\\s]+")

    fun parse(text: String): Result {
        val seen = HashSet<String>()
        val drafts = ArrayList<SourceDraft>()
        var skipped = 0
        for (raw in text.lineSequence()) {
            val line = raw.trim().trimStart('\uFEFF')
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue
            val draft = parseLine(line)
            if (draft == null) {
                skipped++
                continue
            }
            if (seen.add(draft.type.key + "|" + draft.url + "|" + draft.username + "|" + draft.mac)) drafts.add(draft)
        }
        return Result(drafts, skipped)
    }

    private fun withScheme(raw: String): String =
        if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "http://$raw"

    private fun hostOf(url: String): String = Uri.parse(url).host ?: url

    private fun looksLikeAddress(token: String): Boolean =
        token.startsWith("http://", true) || token.startsWith("https://", true) ||
            (token.contains('.') || token.contains(':')) && !macPattern.matches(token)

    private fun parseLine(line: String): SourceDraft? {
        val tokens = line.split(splitter).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null

        val mac = tokens.firstOrNull { macPattern.matches(it) }
        if (mac != null) {
            val address = tokens.firstOrNull { it != mac && looksLikeAddress(it) } ?: return null
            val url = withScheme(address)
            return SourceDraft(SourceType.STALKER, hostOf(url), url, mac = mac.uppercase().replace('-', ':'))
        }

        if (tokens.size >= 3 && looksLikeAddress(tokens[0])) {
            val url = withScheme(tokens[0])
            return SourceDraft(SourceType.XTREAM, hostOf(url), url, tokens[1], tokens[2])
        }

        val single = tokens[0]
        if (!single.startsWith("http://", true) && !single.startsWith("https://", true)) return null
        val uri = Uri.parse(single)
        val user = uri.getQueryParameter("username")
        val pass = uri.getQueryParameter("password")
        if (!user.isNullOrEmpty() && !pass.isNullOrEmpty() && uri.host != null) {
            val port = if (uri.port != -1) ":" + uri.port else ""
            val base = uri.scheme + "://" + uri.host + port
            return SourceDraft(SourceType.XTREAM, uri.host ?: base, base, user, pass)
        }
        return SourceDraft(SourceType.M3U, hostOf(single), single)
    }
}
