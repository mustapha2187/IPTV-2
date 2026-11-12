package com.musta.iptv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.musta.iptv.data.Prefs
import com.musta.iptv.playback.Playback
import com.musta.iptv.ui.screens.AddSourceScreen
import com.musta.iptv.ui.screens.CategoriesScreen
import com.musta.iptv.ui.screens.ChannelsScreen
import com.musta.iptv.ui.screens.ImportScreen
import com.musta.iptv.ui.screens.LanguageScreen
import com.musta.iptv.ui.screens.MenuScreen
import com.musta.iptv.ui.screens.PlaceholderScreen
import com.musta.iptv.ui.screens.PlayerScreen
import com.musta.iptv.ui.screens.ProvidersScreen
import java.util.Locale

@Composable
fun MustaApp(prefs: Prefs, onLanguageChanged: () -> Unit) {
    val nav = rememberNavController()
    val start = remember { if (prefs.languageTag == null) Routes.LANGUAGE else Routes.PROVIDERS }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LANGUAGE) {
            LanguageScreen(
                currentTag = prefs.languageTag ?: Locale.getDefault().language,
                onSelect = { tag ->
                    val changed = tag != Locale.getDefault().language
                    prefs.languageTag = tag
                    // Move on first, so the activity restarts (to apply the language) already on the next step.
                    nav.navigate(Routes.PROVIDERS) { popUpTo(Routes.LANGUAGE) { inclusive = true } }
                    if (changed) onLanguageChanged()
                },
            )
        }
        composable(Routes.PROVIDERS) {
            ProvidersScreen(
                onOpen = { id -> nav.navigate(Routes.categories(id)) },
                onAdd = { type -> nav.navigate(Routes.add(type)) },
                onImport = { nav.navigate(Routes.IMPORT) },
            )
        }
        composable(
            Routes.ADD,
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { entry ->
            AddSourceScreen(
                type = entry.arguments?.getString("type") ?: "m3u",
                onFinished = { nav.popBackStack(Routes.PROVIDERS, false) },
            )
        }
        composable(Routes.IMPORT) {
            ImportScreen(onFinished = { nav.popBackStack(Routes.PROVIDERS, false) })
        }
        composable(
            Routes.CATEGORIES,
            arguments = listOf(navArgument("sourceId") { type = NavType.LongType }),
        ) { entry ->
            val sourceId = entry.arguments?.getLong("sourceId") ?: return@composable
            CategoriesScreen(
                sourceId = sourceId,
                onOpenChannels = { categoryId -> nav.navigate(Routes.channels(sourceId, categoryId)) },
                onMenu = { nav.navigate(Routes.MENU) },
            )
        }
        composable(
            Routes.CHANNELS,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.LongType },
                navArgument("categoryId") { type = NavType.LongType },
            ),
        ) { entry ->
            val args = entry.arguments ?: return@composable
            ChannelsScreen(
                sourceId = args.getLong("sourceId"),
                categoryId = args.getLong("categoryId"),
                onBack = { nav.popBackStack() },
                onFullscreen = { nav.navigate(Routes.PLAYER) },
                onEpg = { nav.navigate(Routes.EPG) },
            )
        }
        composable(Routes.PLAYER) {
            PlayerScreen(
                onList = { nav.popBackStack() },
                onMenu = { nav.navigate(Routes.MENU) },
                onCategories = {
                    val id = Playback.current.value?.sourceId
                    if (id != null) nav.navigate(Routes.categories(id)) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.MENU) {
            MenuScreen(
                onCategories = { id -> nav.navigate(Routes.categories(id)) { launchSingleTop = true } },
                onEpg = { nav.navigate(Routes.EPG) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onExit = { nav.popBackStack(Routes.PROVIDERS, false) },
            )
        }
        placeholderRoutes.forEach { p ->
            composable(p.route) { PlaceholderScreen(p.screenId, p.titleRes) }
        }
    }
}
