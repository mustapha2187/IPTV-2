package com.musta.iptv.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    /** Wraps the base context so resources (and RTL direction) follow the language the user picked. */
    fun wrap(base: Context): Context {
        val tag = Prefs(base).languageTag ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
