package com.musta.iptv.data

import android.net.Uri
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.security.MessageDigest

/**
 * Stalker / MAG middleware client. The MAC address must be the one registered with the user's provider.
 * It identifies itself as a MAG set-top box, as every Stalker client does.
 */
class StalkerClient(portalUrl: String, mac: String) {

    private val macAddress = mac.trim().uppercase().replace('-', ':')
    private val root: String
    private val candidates: List<String>

    private var endpoint: String? = null
    private var token: String = ""

    init {
        val clean = portalUrl.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
        val uri = URI(clean)
        root = "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
        val trimmed = clean.substringBefore("?").trimEnd('/')
        val prefix = if (trimmed.endsWith("/c")) trimmed.removeSuffix("/c") else trimmed
        val list = ArrayList<String>()
        if (trimmed.endsWith("load.php") || trimmed.endsWith("portal.php")) list.add(trimmed)
        list.add("$prefix/server/load.php")
        list.add("$prefix/portal.php")
        list.add("$root/stalker_portal/server/load.php")
        list.add("$root/portal.php")
        candidates = list.distinct()
    }

    private fun headers(): Map<String, String> = buildMap {
        put("User-Agent", USER_AGENT)
        put("X-User-Agent", "Model: MAG250; Link: WiFi")
        put("Accept", "*/*")
        put("Cookie", "mac=${Uri.encode(macAddress)}; stb_lang=en; timezone=Europe%2FParis")
        if (token.isNotEmpty()) put("Authorization", "Bearer $token")
    }

    private fun parse(text: String): JSONObject? = try {
        val t = text.trim()
        val start = t.indexOf('{')
        if (start < 0) null else JSONObject(t.substring(start))
    } catch (e: JSONException) {
        null
    }

    private fun call(ep: String, query: String): JSONObject? = try {
        Http.get("$ep?$query&JsHttpRequest=1-xml", headers()).use { r ->
            if (!r.isSuccessful) null else parse(r.body?.string().orEmpty())
        }
    } catch (e: Exception) {
        null
    }

    private fun digest(algorithm: String, text: String): String =
        MessageDigest.getInstance(algorithm).digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun profileQuery(): String {
        val serial = digest("MD5", macAddress).take(13).uppercase()
        val deviceId = digest("SHA-256", macAddress).uppercase()
        val params = linkedMapOf(
            "type" to "stb",
            "action" to "get_profile",
            "hd" to "1",
            "ver" to "ImageDescription: 0.2.18-r14-pub-250; ImageDate: Fri Jan 15 15:20:44 EET 2016; PORTAL version: 5.6.1; " +
                "API Version: JS API version: 328; STB API version: 134; Player Engine version: 0x566",
            "num_banks" to "2",
            "sn" to serial,
            "stb_type" to "MAG250",
            "client_type" to "STB",
            "image_version" to "218",
            "video_out" to "hdmi",
            "device_id" to deviceId,
            "device_id2" to deviceId,
            "signature" to "",
            "auth_second_step" to "1",
            "hw_version" to "1.7-BD-00",
            "not_valid_token" to "0",
            "metrics" to "",
            "hw_version_2" to digest("SHA-1", macAddress),
            "api_signature" to "262",
            "prehash" to "",
        )
        return params.entries.joinToString("&") { "${it.key}=${Uri.encode(it.value)}" }
    }

    @Synchronized
    private fun ensureSession() {
        if (endpoint != null && token.isNotEmpty()) return
        endpoint = null
        token = ""
        for (ep in candidates) {
            val js = call(ep, "type=stb&action=handshake&token=&prehash=0")?.optJSONObject("js") ?: continue
            val t = js.optString("token")
            if (t.isEmpty()) continue
            endpoint = ep
            token = t
            break
        }
        val ep = endpoint ?: throw IOException("The portal did not answer. Check the portal address.")
        if (call(ep, profileQuery()) == null) {
            endpoint = null
            token = ""
            throw IOException("The portal rejected this device. Check the MAC address.")
        }
    }

    private fun channelArray(obj: JSONObject?): JSONArray? = when (val js = obj?.opt("js")) {
        is JSONArray -> js
        is JSONObject -> js.optJSONArray("data")
        else -> null
    }

    fun loadChannels(): List<ChannelRow> {
        ensureSession()
        val ep = endpoint ?: throw IOException("No portal session")

        val genres = HashMap<String, String>()
        val genreArray = call(ep, "type=itv&action=get_genres")?.optJSONArray("js")
        if (genreArray != null) {
            for (i in 0 until genreArray.length()) {
                val g = genreArray.optJSONObject(i) ?: continue
                genres[g.optString("id")] = g.optString("title")
            }
        }

        val items = ArrayList<JSONObject>()
        val all = channelArray(call(ep, "type=itv&action=get_all_channels"))
        if (all != null) for (i in 0 until all.length()) all.optJSONObject(i)?.let { items.add(it) }

        if (items.isEmpty()) {
            for (page in 1..300) {
                val data = channelArray(
                    call(ep, "type=itv&action=get_ordered_list&genre=*&force_ch_link_check=&fav=0&sortby=number&hd=0&p=$page")
                )
                if (data == null || data.length() == 0) break
                for (i in 0 until data.length()) data.optJSONObject(i)?.let { items.add(it) }
            }
        }

        val rows = ArrayList<ChannelRow>(items.size)
        for ((i, o) in items.withIndex()) {
            val cmd = o.optString("cmd")
            if (cmd.isEmpty()) continue
            rows.add(
                ChannelRow(
                    categoryName = genres[o.optString("tv_genre_id")] ?: "",
                    number = o.optInt("number", i + 1),
                    name = o.optString("name"),
                    streamUrl = "",
                    logoUrl = o.optString("logo"),
                    tvgId = o.optString("xmltv_id"),
                    cmd = cmd,
                )
            )
        }
        return rows
    }

    /** Stream links expire, so this is called each time a channel is played. */
    fun createLink(cmd: String): String {
        repeat(2) {
            ensureSession()
            val ep = endpoint ?: return@repeat
            val reply = call(
                ep,
                "type=itv&action=create_link&cmd=${Uri.encode(cmd)}&series=&forced_storage=&disable_ad=0&download=0&force_ch_link_check=0"
            )
            val text = reply?.optJSONObject("js")?.optString("cmd").orEmpty()
            val url = URL_PATTERN.find(text)?.value
            if (url != null) return url
            endpoint = null
            token = ""
        }
        val direct = URL_PATTERN.find(cmd)?.value
        if (direct != null && !direct.contains("localhost")) return direct
        throw IOException("The portal did not give a stream link")
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3"
        val URL_PATTERN = Regex("""https?://\S+""")
    }
}
