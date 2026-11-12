@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.musta.iptv.R
import com.musta.iptv.data.Cat
import com.musta.iptv.data.Category
import com.musta.iptv.data.SourceManager
import com.musta.iptv.ui.components.rememberInitialFocus
import com.musta.iptv.ui.enigma.Enigma
import com.musta.iptv.ui.enigma.EnigmaFrame
import com.musta.iptv.ui.enigma.Hint
import com.musta.iptv.ui.enigma.ListPopup
import com.musta.iptv.ui.enigma.PopupItem
import com.musta.iptv.ui.enigma.Rc
import com.musta.iptv.ui.enigma.TextRow
import com.musta.iptv.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Entry(val id: Long, val label: String)

/** Category selection. Right arrow or OK opens the channel list; left arrow opens the menu. */
@Composable
fun CategoriesScreen(
    sourceId: Long,
    onOpenChannels: (Long) -> Unit,
    onMenu: () -> Unit,
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var focusedId by remember { mutableLongStateOf(Cat.ALL) }
    var showActions by remember { mutableStateOf(false) }
    val first = rememberInitialFocus()

    LaunchedEffect(sourceId) {
        categories = withContext(Dispatchers.IO) { SourceManager.db.categories(sourceId) }
    }

    val all = stringResource(R.string.group_all)
    val favorites = stringResource(R.string.group_favorites)
    val other = stringResource(R.string.group_other)
    val entries = remember(categories, all, favorites, other) {
        listOf(Entry(Cat.ALL, all), Entry(Cat.FAVORITES, favorites)) +
            categories.map { Entry(it.id, it.name.ifEmpty { other }) }
    }

    val hints = listOf(
        Hint(Enigma.Red, null, stringResource(R.string.hint_favorites)),
        Hint(Enigma.Yellow, null, stringResource(R.string.hint_actions)),
        Hint(null, "◄", stringResource(R.string.hint_menu)),
        Hint(null, "►", stringResource(R.string.hint_channels)),
    )

    EnigmaFrame(
        title = stringResource(R.string.cat_title),
        hints = hints,
        onKey = { code ->
            when {
                showActions -> false
                Rc.isRed(code) -> { onOpenChannels(Cat.FAVORITES); true }
                Rc.isYellow(code) || Rc.isActions(code) -> { showActions = true; true }
                code == Rc.RIGHT -> { onOpenChannels(focusedId); true }
                code == Rc.LEFT -> { onMenu(); true }
                else -> false
            }
        },
    ) {
        if (showActions) {
            ListPopup(
                title = stringResource(R.string.act_title),
                items = listOf(
                    PopupItem(stringResource(R.string.act_refresh)) {
                        SourceManager.refresh(sourceId)
                        showActions = false
                    },
                ),
                onDismiss = { showActions = false },
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                    TextRow(
                        text = entry.label,
                        onClick = { onOpenChannels(entry.id) },
                        focusRequester = if (index == 0) first else null,
                        onFocused = { focusedId = entry.id },
                    )
                }
            }
        }
    }
}
