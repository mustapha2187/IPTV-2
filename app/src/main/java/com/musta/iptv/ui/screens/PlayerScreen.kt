@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.musta.iptv.R
import com.musta.iptv.playback.AspectMode
import com.musta.iptv.playback.PlayStatus
import com.musta.iptv.playback.Playback
import com.musta.iptv.ui.components.rememberInitialFocus
import com.musta.iptv.ui.enigma.Enigma
import com.musta.iptv.ui.enigma.Rc
import com.musta.iptv.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Full screen. Up / Down (or CH+ / CH-) zap, OK returns to the list, Menu opens the menu, blue opens categories. */
@Composable
fun PlayerScreen(onList: () -> Unit, onMenu: () -> Unit, onCategories: () -> Unit) {
    val context = LocalContext.current
    val view = remember { SurfaceView(context) }
    val channel by Playback.current.collectAsState()
    val status by Playback.status.collectAsState()
    val aspect by Playback.aspect.collectAsState()
    val mode by Playback.aspectMode.collectAsState()
    var showBanner by remember { mutableStateOf(true) }
    val focus = rememberInitialFocus()

    DisposableEffect(view) {
        Playback.player.setVideoSurfaceView(view)
        onDispose { Playback.player.clearVideoSurfaceView(view) }
    }
    LaunchedEffect(channel?.id) {
        showBanner = true
        delay(4000)
        showBanner = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val code = event.nativeKeyEvent.keyCode
                when {
                    code == Rc.UP || code == Rc.CH_UP -> { Playback.zap(1); true }
                    code == Rc.DOWN || code == Rc.CH_DOWN -> { Playback.zap(-1); true }
                    Rc.isOk(code) -> { onList(); true }
                    Rc.isActions(code) || Rc.isYellow(code) -> { onMenu(); true }
                    Rc.isBlue(code) -> { onCategories(); true }
                    else -> false
                }
            }
            .focusRequester(focus)
            .focusable(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val boxAspect = maxWidth / maxHeight
            val videoModifier = when (mode) {
                AspectMode.FIT -> Modifier.aspectRatio(aspect)
                AspectMode.STRETCH -> Modifier.fillMaxSize()
                AspectMode.ZOOM ->
                    if (aspect > boxAspect) {
                        Modifier.requiredSize(maxHeight * aspect, maxHeight)
                    } else {
                        Modifier.requiredSize(maxWidth, maxWidth / aspect)
                    }
            }
            AndroidView(factory = { view }, modifier = videoModifier)
        }

        val current = channel
        if (current != null && (showBanner || status !is PlayStatus.Playing)) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Enigma.Background)
                    .padding(Spacing.lg),
            ) {
                Text(
                    text = if (current.number > 0) "${current.number}. ${current.name}" else current.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Enigma.TextMain,
                )
                when (status) {
                    is PlayStatus.Loading -> Text(
                        stringResource(R.string.player_loading),
                        style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim,
                    )
                    is PlayStatus.Error -> Text(
                        stringResource(R.string.player_error),
                        style = MaterialTheme.typography.bodyMedium, color = Enigma.Yellow,
                    )
                    else -> Unit
                }
            }
        }
    }
}
