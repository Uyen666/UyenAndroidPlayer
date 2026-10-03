package com.uyen.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.uyen.launcher.core.scanner.LocalRomScanner
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Aggregates built-in experiences, launchable apps, and user-selected local game folders. */
class GameRepository(private val context: Context) {

    private val scanner = LocalRomScanner(context.contentResolver)
    private val _games = MutableStateFlow(defaultGames)
    val games: StateFlow<List<GameItem>> = _games.asStateFlow()

    private var installedApps: List<GameItem> = emptyList()
    private var localGames: List<GameItem> = emptyList()
    private val favoriteIds = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toMutableSet()

    fun toggleFavorite(gameId: String) {
        if (!favoriteIds.add(gameId)) favoriteIds.remove(gameId)
        favoriteIds.let { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_FAVORITES, it.toSet()).apply() }
        publishGames()
    }

    fun recordPlaySession(gameId: String, durationMillis: Long) {
        if (durationMillis <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "play_millis_$gameId"
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + durationMillis).apply()
        publishGames()
    }

    suspend fun scanInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        installedApps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activity = resolveInfo.activityInfo ?: return@mapNotNull null
                if (activity.packageName == context.packageName) return@mapNotNull null
                GameItem(
                    id = "app:${activity.packageName}",
                    title = resolveInfo.loadLabel(pm).toString(),
                    subtitle = activity.packageName,
                    category = categorize(activity.packageName),
                    packageName = activity.packageName
                )
            }
            .distinctBy(GameItem::id)
            .sortedBy(GameItem::title)
            .toList()
        publishGames()
    }

    suspend fun scanLocalRomFiles(treeUri: Uri?) = withContext(Dispatchers.IO) {
        localGames = treeUri?.let { scanner.scan(it) }.orEmpty()
        publishGames()
    }

    fun launchGame(item: GameItem): Boolean {
        val packageName = item.packageName
        if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        }
        val uri = item.launchIntentUri?.let(Uri::parse) ?: return false
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType ?: "application/octet-stream")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun publishGames() {
        _games.value = (defaultGames + installedApps + localGames)
            .distinctBy(GameItem::id)
            .map { game ->
                val playedMillis = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getLong("play_millis_${game.id}", 0L)
                game.copy(
                    isFavorite = game.id in favoriteIds,
                    playTimeHours = playedMillis / 3_600_000f
                )
            }
    }

    private fun categorize(packageName: String): GameCategory = when {
        listOf("tyranor", "kirikiri", "renpy", "galgame").any { packageName.contains(it, ignoreCase = true) } -> GameCategory.GALGAME
        listOf("retro", "emu", "nostalgia", "arcade").any { packageName.contains(it, ignoreCase = true) } -> GameCategory.RETRO
        listOf("moonlight", "steam", "parsec").any { packageName.contains(it, ignoreCase = true) } -> GameCategory.STREAMING
        else -> GameCategory.TOOL
    }

    private companion object {
        const val KEY_FAVORITES = "favorite_game_ids"
        const val PREFS_NAME = "uyen_library_prefs"
        val defaultGames = listOf(
            GameItem(
                id = "controller_mode",
                title = "UyenController",
                subtitle = "將手機化身為 PC 無線手柄",
                category = GameCategory.CUSTOM,
                tags = listOf("手柄模式", "UDP")
            ),
            GameItem(
                id = "retro_8bit",
                title = "Uyen 8-Bit Cyber Strike",
                subtitle = "內建離線街機遊戲",
                category = GameCategory.RETRO,
                tags = listOf("8-bit", "離線")
            )
        )
    }
}
