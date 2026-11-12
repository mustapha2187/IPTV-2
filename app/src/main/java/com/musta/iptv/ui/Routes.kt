package com.musta.iptv.ui

import com.musta.iptv.R

object Routes {
    const val LANGUAGE = "language"
    const val PROVIDERS = "providers"
    const val ADD = "add/{type}"
    const val IMPORT = "import"
    const val CATEGORIES = "categories/{sourceId}"
    const val CHANNELS = "channels/{sourceId}/{categoryId}"
    const val PLAYER = "player"
    const val MENU = "menu"
    const val EPG = "epg"
    const val SETTINGS = "settings"

    fun add(type: String) = "add/$type"
    fun categories(sourceId: Long) = "categories/$sourceId"
    fun channels(sourceId: Long, categoryId: Long) = "channels/$sourceId/$categoryId"
}

data class PlaceholderRoute(val route: String, val screenId: String, val titleRes: Int)

val placeholderRoutes = listOf(
    PlaceholderRoute(Routes.EPG, "EPG", R.string.title_epg),
    PlaceholderRoute(Routes.SETTINGS, "SET", R.string.title_settings),
)
