package com.musta.iptv.data

import android.content.Context

/** Tiny synchronous settings store. Read synchronously because the UI language must be known before the first frame. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("musta_prefs", Context.MODE_PRIVATE)

    var languageTag: String?
        get() = sp.getString(KEY_LANGUAGE, null)
        set(value) {
            sp.edit().putString(KEY_LANGUAGE, value).apply()
        }

    var setupDone: Boolean
        get() = sp.getBoolean(KEY_SETUP_DONE, false)
        set(value) {
            sp.edit().putBoolean(KEY_SETUP_DONE, value).apply()
        }

    private companion object {
        const val KEY_LANGUAGE = "language_tag"
        const val KEY_SETUP_DONE = "setup_done"
    }
}
