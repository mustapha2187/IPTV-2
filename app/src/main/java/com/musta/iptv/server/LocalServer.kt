package com.musta.iptv.server

import android.net.Uri
import com.musta.iptv.data.AddState
import com.musta.iptv.data.BulkParser
import com.musta.iptv.data.SourceDraft
import com.musta.iptv.data.SourceManager
import com.musta.iptv.data.SourceType
import fi.iki.elonen.NanoHTTPD

/**
 * Tiny web server that lives only while the "add a source" screen is open.
 * The phone opens http://<tv-ip>:<port>/?k=<pin>; every request must carry the PIN.
 */
class LocalServer(
    port: Int,
    private val pin: String,
    private val lang: String,
    private val onDraft: (SourceDraft) -> Unit,
) : NanoHTTPD(port) {

    private val macPattern = Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")

    private fun Map<String, List<String>>.field(name: String): String =
        this[name]?.firstOrNull()?.trim().orEmpty()

    private fun html(status: NanoHTTPD.Response.Status, body: String): NanoHTTPD.Response =
        NanoHTTPD.newFixedLengthResponse(status, "text/html; charset=utf-8", body)

    private fun form(error: String = ""): NanoHTTPD.Response {
        val box = if (error.isEmpty()) "" else "<div class=\"err\">$error</div>"
        return html(NanoHTTPD.Response.Status.OK, SetupPage.render(lang, SetupPage.FORM, pin, mapOf("error" to box)))
    }

    private fun message(status: NanoHTTPD.Response.Status, text: String, withBack: Boolean): NanoHTTPD.Response {
        val back = if (withBack) "<a href=\"javascript:history.back()\">" + SetupPage.texts(lang).getValue("back") + "</a>" else ""
        return html(status, SetupPage.render(lang, SetupPage.MESSAGE, pin, mapOf("message" to text, "back_link" to back)))
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val isPost = session.method == NanoHTTPD.Method.POST
            if (isPost) session.parseBody(HashMap<String, String>())
            val p: Map<String, List<String>> = session.parameters
            val t = SetupPage.texts(lang)

            if (p.field("k") != pin) {
                message(NanoHTTPD.Response.Status.FORBIDDEN, t.getValue("bad_link"), false)
            } else when (session.uri) {
                "/status" -> NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK, "text/plain; charset=utf-8", statusText()
                )
                "/add" -> if (isPost) handleAdd(p, t) else form()
                else -> form()
            }
        } catch (e: Exception) {
            message(NanoHTTPD.Response.Status.INTERNAL_ERROR, "Error", false)
        }
    }

    private fun statusText(): String = when (val s = SourceManager.addState.value) {
        is AddState.Idle -> "idle"
        is AddState.Loading -> "loading"
        is AddState.Done -> "done:" + s.count
        is AddState.Failed -> "failed:" + s.message
        is AddState.Progress -> "progress:" + s.done + "/" + s.total
        is AddState.BulkDone -> "bulk:" + s.added + "/" + s.failed
    }

    private fun withScheme(raw: String): String =
        if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://$raw"

    private fun handleAdd(p: Map<String, List<String>>, t: Map<String, String>): NanoHTTPD.Response {
        if (p.field("type") == "bulk") {
            val result = BulkParser.parse(p.field("bulk_text"))
            if (result.drafts.isEmpty()) return form(t.getValue("e_bulk"))
            SourceManager.addMany(result.drafts, result.skipped)
            return html(NanoHTTPD.Response.Status.OK, SetupPage.render(lang, SetupPage.SENT, pin))
        }
        val type = SourceType.from(p.field("type")) ?: SourceType.M3U
        val name = p.field("name")

        val draft: SourceDraft = when (type) {
            SourceType.M3U -> {
                val url = p.field("m3u_url")
                if (!url.startsWith("http://") && !url.startsWith("https://")) return form(t.getValue("e_url"))
                SourceDraft(type, name.ifEmpty { hostOf(url) }, url)
            }
            SourceType.XTREAM -> {
                val url = p.field("x_url")
                val user = p.field("x_user")
                val pass = p.field("x_pass")
                if (url.isEmpty()) return form(t.getValue("e_url"))
                if (user.isEmpty() || pass.isEmpty()) return form(t.getValue("e_login"))
                val full = withScheme(url)
                SourceDraft(type, name.ifEmpty { hostOf(full) }, full, user, pass)
            }
            SourceType.STALKER -> {
                val url = p.field("s_url")
                val mac = p.field("s_mac").uppercase().replace('-', ':')
                if (url.isEmpty()) return form(t.getValue("e_url"))
                if (!macPattern.matches(mac)) return form(t.getValue("e_mac"))
                val full = withScheme(url)
                SourceDraft(type, name.ifEmpty { hostOf(full) }, full, mac = mac)
            }
        }

        onDraft(draft)
        return html(NanoHTTPD.Response.Status.OK, SetupPage.render(lang, SetupPage.SENT, pin))
    }

    private fun hostOf(url: String): String = Uri.parse(url).host ?: url
}
