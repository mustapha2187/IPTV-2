package com.musta.iptv.data

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Xtream Codes style API: player_api.php with username and password. */
object XtreamClient {

    fun cleanHost(raw: String): String {
        var u = raw.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        return u.substringBefore("/player_api.php").substringBefore("/get.php").trimEnd('/')
    }

    private fun fetch(url: String): String {
        Http.get(url).use { r ->
            if (!r.isSuccessful) throw IOException("Server answered with code ${r.code}")
            val text = r.body?.string().orEmpty().trim()
            if (!text.startsWith("{") && !text.startsWith("[")) throw IOException("Unexpected answer from the server")
            return text
        }
    }

    fun loadChannels(rawHost: String, user: String, pass: String): List<ChannelRow> {
        val host = cleanHost(rawHost)
        val u = Uri.encode(user)
        val p = Uri.encode(pass)
        val api = "$host/player_api.php?username=$u&password=$p"

        val auth = JSONObject(fetch(api)).optJSONObject("user_info")
        if (auth == null || auth.optString("auth") != "1") {
            throw IOException("Login rejected. Check the username and password.")
        }

        val categoryNames = HashMap<String, String>()
        val cats = JSONArray(fetch("$api&action=get_live_categories"))
        for (i in 0 until cats.length()) {
            val c = cats.optJSONObject(i) ?: continue
            categoryNames[c.optString("category_id")] = c.optString("category_name")
        }

        val streams = JSONArray(fetch("$api&action=get_live_streams"))
        val rows = ArrayList<ChannelRow>(streams.length())
        for (i in 0 until streams.length()) {
            val s = streams.optJSONObject(i) ?: continue
            val id = s.optString("stream_id")
            if (id.isEmpty()) continue
            rows.add(
                ChannelRow(
                    categoryName = categoryNames[s.optString("category_id")] ?: "",
                    number = s.optInt("num", i + 1),
                    name = s.optString("name"),
                    streamUrl = "$host/live/$u/$p/$id.ts",
                    logoUrl = s.optString("stream_icon"),
                    tvgId = s.optString("epg_channel_id"),
                    cmd = "",
                )
            )
        }
        return rows
    }
}
