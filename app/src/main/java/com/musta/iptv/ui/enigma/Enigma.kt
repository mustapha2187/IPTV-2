@file:OptIn(ExperimentalFoundationApi::class, ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.enigma

import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.musta.iptv.playback.Playback
import com.musta.iptv.ui.components.rememberInitialFocus
import com.musta.iptv.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Colors of the set-top-box look. Drawn from scratch; nothing copied from any other app. */
object Enigma {
    val Background = Color(0xFF0A0A18)
    val Selection = Color(0xFF6B68E3)
    val Yellow = Color(0xFFF5C518)
    val Red = Color(0xFFE53935)
    val Green = Color(0xFF43A047)
    val Blue = Color(0xFF1E88E5)
    val TextMain = Color(0xFFF2F2FA)
    val TextDim = Color(0xFF9A9AB8)
    val Badge = Color(0xFF3A3A5C)
}

/** One entry of the footer: a colored dot or a key badge, plus its label. */
data class Hint(val color: Color?, val badge: String?, val label: String)

data class PopupItem(val label: String, val action: () -> Unit)

private fun clockText(): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

@Composable
private fun Clock() {
    val time by produceState(initialValue = clockText()) {
        while (true) {
            value = clockText()
            delay(1000)
        }
    }
    Text(text = time, style = MaterialTheme.typography.titleLarge, color = Enigma.TextMain)
}

@Composable
private fun Line() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Enigma.Yellow)
    )
}

@Composable
private fun Footer(hints: List<Hint>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (hint in hints) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (hint.color != null) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .background(hint.color, CircleShape)
                    )
                } else if (hint.badge != null) {
                    Box(
                        Modifier
                            .background(Enigma.Badge, RoundedCornerShape(6.dp))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    ) {
                        Text(hint.badge, style = MaterialTheme.typography.labelMedium, color = Enigma.TextMain)
                    }
                }
                Text(hint.label, style = MaterialTheme.typography.bodyMedium, color = Enigma.TextMain)
            }
        }
    }
}

/** The small live picture on the left, bound to the shared player. */
@Composable
fun VideoPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = remember { SurfaceView(context) }
    val aspect by Playback.aspect.collectAsState()
    DisposableEffect(view) {
        Playback.player.setVideoSurfaceView(view)
        onDispose { Playback.player.clearVideoSurfaceView(view) }
    }
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(factory = { view }, modifier = Modifier.aspectRatio(aspect))
    }
}

/**
 * Title and clock on top, picture on the left, list on the right, colored-key hints at the bottom.
 * The layout is always left-to-right so it looks the same in every language.
 */
@Composable
fun EnigmaFrame(
    title: String,
    hints: List<Hint>,
    onKey: (Int) -> Boolean = { false },
    showPreview: Boolean = true,
    leftContent: @Composable ColumnScope.() -> Unit = {},
    rightContent: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Enigma.Background)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) onKey(event.nativeKeyEvent.keyCode) else false
                }
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, color = Enigma.TextMain)
                Clock()
            }
            Spacer(Modifier.height(Spacing.xs))
            Line()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Column(modifier = Modifier.weight(0.4f)) {
                    if (showPreview) VideoPreview(Modifier.fillMaxWidth())
                    leftContent()
                }
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                    content = rightContent,
                )
            }
            Line()
            Footer(hints)
        }
    }
}

/** A list row: highlighted bar and yellow text while focused. OK clicks, long OK is optional. */
@Composable
fun EnigmaRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val requester = Modifier.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .background(if (focused) Enigma.Selection else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(focused)
    }
}

fun rowTextColor(focused: Boolean): Color = if (focused) Enigma.Yellow else Enigma.TextMain

/** Simple row with one line of text. */
@Composable
fun TextRow(
    text: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    EnigmaRow(onClick = onClick, focusRequester = focusRequester, onFocused = onFocused, onLongClick = onLongClick) { focused ->
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = rowTextColor(focused))
    }
}

/** A small list of actions shown in place of the main list. Back closes it. */
@Composable
fun ListPopup(title: String, items: List<PopupItem>, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val first = rememberInitialFocus()
    Column {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = Enigma.Yellow)
        Spacer(Modifier.height(Spacing.md))
        items.forEachIndexed { i, item ->
            TextRow(text = item.label, onClick = item.action, focusRequester = if (i == 0) first else null)
        }
    }
}
