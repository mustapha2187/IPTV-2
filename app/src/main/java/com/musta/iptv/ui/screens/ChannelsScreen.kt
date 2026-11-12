@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.musta.iptv.R
import com.musta.iptv.data.Cat
import com.musta.iptv.data.Channel
import com.musta.iptv.data.SourceManager
import com.musta.iptv.playback.Playback
import com.musta.iptv.ui.components.rememberInitialFocus
import com.musta.iptv.ui.enigma.Enigma
import com.musta.iptv.ui.enigma.EnigmaFrame
import com.musta.iptv.ui.enigma.EnigmaRow
import com.musta.iptv.ui.enigma.Hint
import com.musta.iptv.ui.enigma.ListPopup
import com.musta.iptv.ui.enigma.PopupItem
import com.musta.iptv.ui.enigma.Rc
import com.musta.iptv.ui.enigma.rowTextColor
import com.musta.iptv.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Channel list. OK plays the channel in the small picture; OK again on the playing channel opens full screen.
 * Yellow (or Menu) opens actions, left arrow or blue goes back to the categories.
 */
@Composable
fun ChannelsScreen(
    sourceId: Long,
    categoryId: Long,
    onBack: () -> Unit,
    onFullscreen: () -> Unit,
    onEpg: () -> Unit,
) {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var categoryName by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var showActions by remember { mutableStateOf(false) }
    val playing by Playback.current.collectAsState()
    val scope = rememberCoroutineScope()
    val first = rememberInitialFocus()

    val all = stringResource(R.string.group_all)
    val favorites = stringResource(R.string.group_favorites)
    val other = stringResource(R.string.group_other)

    LaunchedEffect(sourceId, categoryId, reload) {
        channels = withContext(Dispatchers.IO) { SourceManager.db.channels(sourceId, categoryId) }
    }
    LaunchedEffect(sourceId, categoryId, all, favorites, other) {
        categoryName = when (categoryId) {
            Cat.ALL -> all
            Cat.FAVORITES -> favorites
            else -> withContext(Dispatchers.IO) {
                SourceManager.db.categories(sourceId).firstOrNull { it.id == categoryId }?.name
            }?.ifEmpty { other } ?: other
        }
    }

    fun select(index: Int) {
        val channel = channels.getOrNull(index) ?: return
        if (playing?.id == channel.id) onFullscreen() else Playback.start(channels, index)
    }

    val hints = listOf(
        Hint(Enigma.Red, null, stringResource(R.string.hint_epg)),
        Hint(Enigma.Blue, "◄", stringResource(R.string.hint_category)),
        Hint(Enigma.Yellow, null, stringResource(R.string.hint_actions)),
        Hint(null, "OK", stringResource(R.string.hint_select)),
    )

    EnigmaFrame(
        title = stringResource(R.string.ch_title, categoryName),
        hints = hints,
        onKey = { code ->
            when {
                showActions -> false
                Rc.isRed(code) -> { onEpg(); true }
                Rc.isBlue(code) || code == Rc.LEFT -> { onBack(); true }
                Rc.isYellow(code) || Rc.isActions(code) -> { showActions = true; true }
                else -> false
            }
        },
    ) {
        if (showActions) {
            ListPopup(
                title = stringResource(R.string.act_title),
                items = listOf(
                    PopupItem(stringResource(R.string.act_favorite)) {
                        val channel = channels.getOrNull(focusedIndex)
                        showActions = false
                        if (channel != null) {
                            scope.launch {
                                withContext(Dispatchers.IO) { SourceManager.db.toggleFavorite(channel.id) }
                                reload += 1
                            }
                        }
                    },
                    PopupItem(stringResource(R.string.act_fullscreen)) {
                        showActions = false
                        val channel = channels.getOrNull(focusedIndex)
                        if (channel != null) {
                            if (playing?.id != channel.id) Playback.start(channels, focusedIndex)
                            onFullscreen()
                        }
                    },
                ),
                onDismiss = { showActions = false },
            )
        } else if (channels.isEmpty()) {
            Text(
                text = stringResource(R.string.ch_empty),
                style = MaterialTheme.typography.titleMedium,
                color = Enigma.TextDim,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                itemsIndexed(channels, key = { _, c -> c.id }) { index, channel ->
                    EnigmaRow(
                        onClick = { select(index) },
                        focusRequester = if (index == 0) first else null,
                        onFocused = { focusedIndex = index },
                        onLongClick = {
                            focusedIndex = index
                            showActions = true
                        },
                    ) { focused ->
                        Box(
                            Modifier
                                .size(width = 4.dp, height = 22.dp)
                                .background(if (playing?.id == channel.id) Enigma.Green else androidx.compose.ui.graphics.Color.Transparent)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = if (channel.number > 0) channel.number.toString() else "",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (focused) Enigma.Yellow else Enigma.TextDim,
                            modifier = Modifier.width(56.dp),
                        )
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = rowTextColor(focused),
                            modifier = Modifier.weight(1f),
                        )
                        if (channel.isFavorite) {
                            Text("★", style = MaterialTheme.typography.titleMedium, color = Enigma.Yellow)
                        }
                    }
                }
            }
        }
    }
}
