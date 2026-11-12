@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.musta.iptv.R
import com.musta.iptv.data.AddState
import com.musta.iptv.data.Limits
import com.musta.iptv.data.Source
import com.musta.iptv.data.SourceManager
import com.musta.iptv.data.SourceType
import com.musta.iptv.ui.components.rememberInitialFocus
import com.musta.iptv.ui.enigma.Enigma
import com.musta.iptv.ui.enigma.EnigmaFrame
import com.musta.iptv.ui.enigma.EnigmaRow
import com.musta.iptv.ui.enigma.Hint
import com.musta.iptv.ui.enigma.ListPopup
import com.musta.iptv.ui.enigma.PopupItem
import com.musta.iptv.ui.enigma.Rc
import com.musta.iptv.ui.enigma.TextRow
import com.musta.iptv.ui.enigma.rowTextColor
import com.musta.iptv.ui.theme.Spacing
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun typeColor(type: SourceType): Color = when (type) {
    SourceType.M3U -> Enigma.Red
    SourceType.STALKER -> Enigma.Green
    SourceType.XTREAM -> Enigma.Yellow
}

@Composable
fun typeLabel(type: SourceType): String = when (type) {
    SourceType.M3U -> stringResource(R.string.type_m3u)
    SourceType.XTREAM -> stringResource(R.string.type_xtream)
    SourceType.STALKER -> stringResource(R.string.type_stalker)
}

/** Start screen. Red / green / yellow add an M3U, Stalker or Xtream server; blue imports a file. */
@Composable
fun ProvidersScreen(
    onOpen: (Long) -> Unit,
    onAdd: (String) -> Unit,
    onImport: () -> Unit,
) {
    val version by SourceManager.version.collectAsState()
    val state by SourceManager.addState.collectAsState()
    var sources by remember { mutableStateOf<List<Source>>(emptyList()) }
    var focusedId by remember { mutableStateOf<Long?>(null) }
    var actionTarget by remember { mutableStateOf<Source?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val first = rememberInitialFocus()

    LaunchedEffect(version) {
        sources = withContext(Dispatchers.IO) { SourceManager.db.listSources() }
    }

    val hints = listOf(
        Hint(Enigma.Red, null, stringResource(R.string.hint_m3u)),
        Hint(Enigma.Green, null, stringResource(R.string.hint_stalker)),
        Hint(Enigma.Yellow, null, stringResource(R.string.hint_xtream)),
        Hint(Enigma.Blue, null, stringResource(R.string.hint_import)),
        Hint(null, "OK", stringResource(R.string.hint_open)),
        Hint(null, "Menu", stringResource(R.string.hint_actions)),
    )

    EnigmaFrame(
        title = stringResource(R.string.prov_title),
        hints = hints,
        onKey = { code ->
            when {
                actionTarget != null -> false
                Rc.isRed(code) -> { onAdd("m3u"); true }
                Rc.isGreen(code) -> { onAdd("stalker"); true }
                Rc.isYellow(code) -> { onAdd("xtream"); true }
                Rc.isBlue(code) -> { onImport(); true }
                Rc.isActions(code) -> {
                    actionTarget = sources.firstOrNull { it.id == focusedId }
                    confirmDelete = false
                    true
                }
                else -> false
            }
        },
        leftContent = {
            Spacer(Modifier.height(Spacing.md))
            val selected = sources.firstOrNull { it.id == focusedId }
            if (selected != null) {
                val sameType = sources.filter { it.type == selected.type }
                Text(selected.name, style = MaterialTheme.typography.titleLarge, color = Enigma.Yellow)
                Text(typeLabel(selected.type), style = MaterialTheme.typography.bodyLarge, color = Enigma.TextMain)
                Text(
                    stringResource(R.string.prov_url, selected.url),
                    style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                )
                if (selected.username.isNotEmpty()) {
                    Text(
                        stringResource(R.string.prov_user, selected.username),
                        style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                    )
                }
                if (selected.mac.isNotEmpty()) {
                    Text(
                        stringResource(R.string.prov_mac, selected.mac),
                        style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                    )
                }
                Text(
                    stringResource(
                        R.string.prov_slots, typeLabel(selected.type), sameType.size, Limits.forType(selected.type)
                    ),
                    style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                )
            }
            val status = when (val s = state) {
                is AddState.Loading -> stringResource(R.string.add_loading, s.name)
                is AddState.Failed -> stringResource(R.string.add_failed, s.message)
                is AddState.Progress -> stringResource(R.string.add_progress, s.done, s.total, s.failed)
                else -> ""
            }
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(status, style = MaterialTheme.typography.bodyMedium, color = Enigma.Yellow)
            }
        },
    ) {
        val target = actionTarget
        if (target != null) {
            ListPopup(
                title = target.name,
                items = listOf(
                    PopupItem(stringResource(R.string.act_refresh)) {
                        SourceManager.refresh(target.id)
                        actionTarget = null
                    },
                    PopupItem(
                        stringResource(if (confirmDelete) R.string.act_delete_confirm else R.string.act_delete)
                    ) {
                        if (confirmDelete) {
                            SourceManager.delete(target.id)
                            actionTarget = null
                            focusedId = null
                        } else {
                            confirmDelete = true
                        }
                    },
                ),
                onDismiss = { actionTarget = null },
            )
        } else if (sources.isEmpty()) {
            Column {
                TextRow(
                    text = stringResource(R.string.prov_empty),
                    onClick = {},
                    focusRequester = first,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                itemsIndexed(sources, key = { _, s -> s.id }) { index, source ->
                    val serverNumber = sources.filter { it.type == source.type }.indexOfFirst { it.id == source.id } + 1
                    EnigmaRow(
                        onClick = { onOpen(source.id) },
                        focusRequester = if (index == 0) first else null,
                        onFocused = { focusedId = source.id },
                        onLongClick = {
                            actionTarget = source
                            confirmDelete = false
                        },
                    ) { focused ->
                        Box(
                            Modifier
                                .size(14.dp)
                                .background(typeColor(source.type), CircleShape)
                        )
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = rowTextColor(focused),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.prov_meta, serverNumber, source.channelCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (focused) Enigma.Yellow else Enigma.TextDim,
                        )
                    }
                }
            }
        }
    }
}
