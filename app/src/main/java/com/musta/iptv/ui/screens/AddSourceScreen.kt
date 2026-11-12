@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.musta.iptv.R
import com.musta.iptv.data.AddState
import com.musta.iptv.data.SourceManager
import com.musta.iptv.server.LocalServer
import com.musta.iptv.server.NetUtil
import com.musta.iptv.ui.enigma.Enigma
import com.musta.iptv.ui.enigma.EnigmaFrame
import com.musta.iptv.ui.enigma.Hint
import com.musta.iptv.ui.theme.Spacing
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.Locale

/** Shows an address; the phone opens it and sends M3U links, Xtream or Stalker details, or a whole TXT list. */
@Composable
fun AddSourceScreen(type: String, onFinished: () -> Unit) {
    val state by SourceManager.addState.collectAsState()
    var address by remember { mutableStateOf<String?>(null) }
    var networkMissing by remember { mutableStateOf(false) }
    val lang = remember { Locale.getDefault().language }

    DisposableEffect(Unit) {
        SourceManager.resetAdd()
        val pin = (1000..9999).random().toString()
        val ip = NetUtil.lanIp()
        var server: LocalServer? = null
        var port = 0
        if (ip != null) {
            for (candidate in 8080..8099) {
                try {
                    val s = LocalServer(candidate, pin, lang) { draft -> SourceManager.add(draft) }
                    s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
                    server = s
                    port = candidate
                    break
                } catch (e: IOException) {
                    // port busy, try the next one
                }
            }
        }
        if (server != null && ip != null) address = "http://$ip:$port/?k=$pin&t=$type" else networkMissing = true
        onDispose { server?.stop() }
    }

    LaunchedEffect(state) {
        val s = state
        if ((s is AddState.Done) || (s is AddState.BulkDone && s.added > 0)) {
            delay(2500)
            onFinished()
        }
    }

    EnigmaFrame(
        title = stringResource(R.string.add_title),
        hints = listOf(Hint(null, "Back", stringResource(R.string.hint_close))),
    ) {
        Column {
            if (networkMissing) {
                Text(stringResource(R.string.add_no_network), style = MaterialTheme.typography.titleMedium, color = Enigma.Yellow)
            } else {
                Text(stringResource(R.string.add_step1), style = MaterialTheme.typography.titleMedium, color = Enigma.TextMain)
                Spacer(Modifier.height(Spacing.sm))
                Text(stringResource(R.string.add_step2), style = MaterialTheme.typography.titleMedium, color = Enigma.TextMain)
                Spacer(Modifier.height(Spacing.md))
                Text(address.orEmpty(), style = MaterialTheme.typography.headlineMedium, color = Enigma.Yellow)
                Spacer(Modifier.height(Spacing.lg))
                val statusText = when (val s = state) {
                    is AddState.Idle -> stringResource(R.string.add_waiting)
                    is AddState.Loading -> stringResource(R.string.add_loading, s.name)
                    is AddState.Done -> stringResource(R.string.add_done, s.count)
                    is AddState.Failed -> stringResource(R.string.add_failed, s.message)
                    is AddState.Progress -> stringResource(R.string.add_progress, s.done, s.total, s.failed)
                    is AddState.BulkDone -> stringResource(R.string.add_bulk_done, s.added, s.failed)
                }
                Text(statusText, style = MaterialTheme.typography.titleMedium, color = Enigma.TextMain)
                if (state is AddState.Failed) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        stringResource(R.string.add_try_again),
                        style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                    )
                }
            }
        }
    }
}
