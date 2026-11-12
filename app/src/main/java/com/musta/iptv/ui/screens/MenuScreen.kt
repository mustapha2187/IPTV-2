@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
import com.musta.iptv.data.Source
import com.musta.iptv.data.SourceManager
import com.musta.iptv.playback.AspectMode
import com.musta.iptv.playback.Playback
import com.musta.iptv.ui.components.rememberInitialFocus
import com.musta.iptv.ui.enigma.Enigma
import com.musta.iptv.ui.enigma.EnigmaFrame
import com.musta.iptv.ui.enigma.Hint
import com.musta.iptv.ui.enigma.TextRow
import com.musta.iptv.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SourceInfo(val source: Source, val serverNumber: Int)

/** Player menu: aspect, previous channel, restart, categories, and the details of the server being played. */
@Composable
fun MenuScreen(
    onCategories: (Long) -> Unit,
    onEpg: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
) {
    val current by Playback.current.collectAsState()
    val mode by Playback.aspectMode.collectAsState()
    var info by remember { mutableStateOf<SourceInfo?>(null) }
    val first = rememberInitialFocus()

    LaunchedEffect(current?.sourceId) {
        val sourceId = current?.sourceId
        info = if (sourceId == null) null else withContext(Dispatchers.IO) {
            val all = SourceManager.db.listSources()
            val source = all.firstOrNull { it.id == sourceId }
            source?.let { s -> SourceInfo(s, all.filter { it.type == s.type }.indexOfFirst { it.id == s.id } + 1) }
        }
    }

    val modeLabel = stringResource(
        when (mode) {
            AspectMode.FIT -> R.string.aspect_fit
            AspectMode.ZOOM -> R.string.aspect_zoom
            AspectMode.STRETCH -> R.string.aspect_stretch
        }
    )

    EnigmaFrame(
        title = stringResource(R.string.menu_title),
        hints = listOf(Hint(null, "Back", stringResource(R.string.hint_close))),
    ) {
        Column {
            TextRow(stringResource(R.string.menu_aspect, modeLabel), onClick = { Playback.cycleAspect() }, focusRequester = first)
            TextRow(stringResource(R.string.menu_previous), onClick = { Playback.playPrevious() })
            TextRow(stringResource(R.string.menu_restart), onClick = { Playback.restart() })
            TextRow(stringResource(R.string.menu_category), onClick = { current?.let { onCategories(it.sourceId) } })
            TextRow(stringResource(R.string.menu_epg), onClick = onEpg)
            TextRow(stringResource(R.string.menu_settings), onClick = onSettings)
            TextRow(stringResource(R.string.menu_exit), onClick = {
                Playback.stop()
                onExit()
            })

            val details = info
            val channel = current
            if (details != null && channel != null) {
                Spacer(Modifier.height(Spacing.lg))
                val streamType = when {
                    channel.cmd.isNotEmpty() -> "Stalker"
                    channel.streamUrl.contains(".m3u8") -> "HLS"
                    channel.streamUrl.contains(".ts") -> "MPEGTS"
                    else -> "Stream"
                }
                val typeName = details.source.type.name
                Text(
                    stringResource(R.string.menu_info_server, typeName, details.serverNumber),
                    style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                )
                Text(
                    stringResource(R.string.menu_info_url, details.source.url),
                    style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                )
                if (details.source.username.isNotEmpty()) {
                    Text(
                        stringResource(R.string.menu_info_user, details.source.username),
                        style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                    )
                    Text(
                        stringResource(R.string.menu_info_pass),
                        style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                    )
                }
                Text(
                    stringResource(R.string.menu_info_stream, streamType),
                    style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                )
            }
        }
    }
}
