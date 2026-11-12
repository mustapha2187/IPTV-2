@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.musta.iptv.data.LocaleHelper
import com.musta.iptv.data.Prefs
import com.musta.iptv.data.SourceManager
import com.musta.iptv.playback.Playback
import com.musta.iptv.ui.MustaApp
import com.musta.iptv.ui.theme.MustaTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SourceManager.init(applicationContext)
        Playback.init(applicationContext)
        val prefs = Prefs(this)
        setContent {
            MustaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MustaApp(prefs = prefs, onLanguageChanged = { recreate() })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) Playback.release()
    }
}
