package com.uyen.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.UserManager
import com.uyen.launcher.core.scanner.LocalRomScanner
import com.uyen.launcher.core.system.PackageChangeMonitor
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 聚合內建體驗、可啟動 App 與本機 ROM/Galgame 的商業級資料倉儲 (GameRepository)
 */
class GameRepository(private val context: Context) {

    private val scanner = LocalRomScanner(context.contentResolver)
    private val _games = MutableStateFlow(defaultGames)
    val games: StateFlow<List<GameItem>> = _games.asStateFlow()

    private var installedApps: List<GameItem> = emptyList()
    private var localGames: List<GameItem> = emptyList()
    private val favoriteIds = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toMutableSet()

    private var packageMonitor: PackageChangeMonitor? = null

    /**
     * 啟動系統級實時安裝/卸載監控，當 Google Play 或系統完成安裝時自動觸發無感更新
     */
    fun startMonitoring(scope: CoroutineScope) {
        if (packageMonitor != null) return
        packageMonitor = PackageChangeMonitor(context) {
            scope.launch {
                scanInstalledApps()
            }
        }.also { it.register(scope) }
    }

    /**
     * 釋放資源並取消廣播與 Launcher 回調監聽
     */
    fun release() {
        packageMonitor?.unregister()
        packageMonitor = null
    }

    fun toggleFavorite(gameId: String) {
        if (!favoriteIds.add(gameId)) favoriteIds.remove(gameId)
        favoriteIds.let {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY_FAVORITES, it.toSet()).apply()
        }
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
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

        val discoveredItems = mutableListOf<GameItem>()

        // 1. 優先使用 LauncherApps 官方啟動器 API (支援多用戶分身/應用雙開，且無 Android 11+ package visibility 限制)
        if (launcherApps != null && userManager != null) {
            try {
                val profiles = userManager.userProfiles
                for (profile in profiles) {
                    val activities = launcherApps.getActivityList(null, profile)
                    for (activity in activities) {
                        val packageName = activity.applicationInfo.packageName
                        if (packageName == context.packageName) continue
                        discoveredItems.add(
                            GameItem(
                                id = "app:$packageName",
                                title = activity.label?.toString() ?: packageName,
                                subtitle = packageName,
                                category = categorize(packageName),
                                packageName = packageName
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. 若 LauncherApps 未取得資料或在非標準環境，回退至 PackageManager 查詢
        if (discoveredItems.isEmpty()) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            activities.forEach { resolveInfo ->
                val activity = resolveInfo.activityInfo ?: return@forEach
                if (activity.packageName == context.packageName) return@forEach
                discoveredItems.add(
                    GameItem(
                        id = "app:${activity.packageName}",
                        title = resolveInfo.loadLabel(pm).toString(),
                        subtitle = activity.packageName,
                        category = categorize(activity.packageName),
                        packageName = activity.packageName
                    )
                )
            }
        }

        installedApps = discoveredItems
            .distinctBy(GameItem::id)
            .sortedBy(GameItem::title)

        publishGames()
    }

    suspend fun scanLocalRomFiles(treeUri: Uri?) = withContext(Dispatchers.IO) {
        localGames = treeUri?.let { scanner.scan(it) }.orEmpty()
        publishGames()
    }

    /**
     * 商業級掌機遊戲喚起分發引擎
     * 1. 原生 Android 應用直接透過 PackageManager 啟動
     * 2. Galgame 本機檔案智能調用 Tyranor / Kirikiroid2 / JoiPlay，未安裝引擎則回傳 false 觸發引導彈窗
     * 3. 復古 ROM 透過系統關聯相容模擬器開啟
     */
    fun launchGame(item: GameItem): Boolean {
        val pm = context.packageManager

        // 1. Android 原生已安裝 App
        val packageName = item.packageName
        if (packageName != null) {
            val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        }

        val uriString = item.launchIntentUri ?: return false
        val uri = Uri.parse(uriString)

        // 2. Galgame 專屬引擎智能分發
        if (item.category == GameCategory.GALGAME) {
            val candidateEngines = listOf(
                "com.tyranor",                 // Tyranor 通用視覺小說引擎 (麵包工房)
                "cn.yuri.kirikiri",            // Kirikiroid2 (吉里吉里2官方)
                "com.artemi.kirikiroid2",
                "com.artemi.kirikiroid2_free",
                "cyou.joiplay.joiplay",        // JoiPlay 主程式
                "cyou.joiplay.renpy"           // JoiPlay Ren'Py 外掛
            )

            val installedEngine = candidateEngines.firstOrNull { pkg ->
                runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
            }

            if (installedEngine != null) {
                // 嘗試以 ACTION_VIEW 傳遞 URI 權限直接進入遊戲
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, item.mimeType ?: "application/octet-stream")
                    setPackage(installedEngine)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (viewIntent.resolveActivity(pm) != null) {
                    context.startActivity(viewIntent)
                    return true
                }

                // 若該引擎不支援 URI 隱式調用，則啟動該引擎主介面
                val launchIntent = pm.getLaunchIntentForPackage(installedEngine)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }

            // 手機未安裝任何 Galgame 核心引擎 -> 回傳 false 觸發掌機助手引導安裝
            return false
        }

        // 3. 通用本機遊戲 (復古 ROM / 獨立檔案)
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType ?: "application/octet-stream")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
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

    companion object {
        const val KEY_FAVORITES = "favorite_game_ids"
        const val PREFS_NAME = "uyen_library_prefs"

        fun categorize(packageName: String): GameCategory = when {
            listOf("tyranor", "kirikiri", "renpy", "galgame").any { packageName.contains(it, ignoreCase = true) } -> GameCategory.GALGAME
            listOf("retro", "emu", "nostalgia", "arcade").any { packageName.contains(it, ignoreCase = true) } -> GameCategory.RETRO
            listOf("moonlight", "limelight", "steam", "parsec", "geforce", "sunshine").any { packageName.contains(it, ignoreCase = true) } -> GameCategory.STREAMING
            else -> GameCategory.TOOL
        }
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
