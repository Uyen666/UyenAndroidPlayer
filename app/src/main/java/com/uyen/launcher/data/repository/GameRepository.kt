package com.uyen.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 遊戲與應用庫資料倉庫
 */
class GameRepository(private val context: Context) {

    private val _games = MutableStateFlow<List<GameItem>>(emptyList())
    val games: StateFlow<List<GameItem>> = _games.asStateFlow()

    init {
        loadDefaultHandheldGames()
    }

    private fun loadDefaultHandheldGames() {
        val initialList = mutableListOf(
            GameItem(
                id = "controller_mode",
                title = "UyenController",
                subtitle = "將手機化身為 PC 無線 Xbox 手柄",
                category = GameCategory.CUSTOM,
                tags = listOf("手柄模式", "UDP/藍牙", "低延遲"),
                playTimeHours = 12.5f,
                isFavorite = true
            ),
            GameItem(
                id = "galgame_tyranor",
                title = "Tyranor 視覺小說引擎",
                subtitle = "相容 Artemis / RPG Maker / Tyranor / O2",
                category = GameCategory.GALGAME,
                packageName = "com.tyranor",
                tags = listOf("Galgame", "AVG", "ONS", "KRKR"),
                playTimeHours = 28.4f,
                isFavorite = true
            ),
            GameItem(
                id = "retro_8bit",
                title = "8-bit 復古懷舊精選",
                subtitle = "紅白機 NES / FC 經典像素遊戲核心",
                category = GameCategory.RETRO,
                tags = listOf("8-bit", "FC", "像素", "街機"),
                playTimeHours = 15.2f,
                isFavorite = true
            ),
            GameItem(
                id = "streaming_moonlight",
                title = "Moonlight 串流主機",
                subtitle = "720p 90Hz 極低延遲 Sunshine / Nvidia 串流",
                category = GameCategory.STREAMING,
                packageName = "com.limelight",
                tags = listOf("3A大作", "PC串流", "90FPS"),
                playTimeHours = 45.0f,
                isFavorite = true
            ),
            GameItem(
                id = "custom_sandbox",
                title = "自製遊戲沙盒 (Canvas)",
                subtitle = "支援 HTML5 / PixiJS / Godot Web 自製微遊戲",
                category = GameCategory.CUSTOM,
                tags = listOf("自製作品", "Web", "獨立遊戲"),
                playTimeHours = 8.1f
            ),
            GameItem(
                id = "galgame_krkr",
                title = "Kirikiroid2 吉里吉里",
                subtitle = "XP3 經典日系 Galgame 引擎模擬庫",
                category = GameCategory.GALGAME,
                packageName = "org.tvp.kirikiri2",
                tags = listOf("Galgame", "AVG", "日系"),
                playTimeHours = 19.3f
            ),
            GameItem(
                id = "retro_gba",
                title = "GBA 掌機模擬庫",
                subtitle = "經典 Game Boy Advance 核心",
                category = GameCategory.RETRO,
                tags = listOf("GBA", "16-bit", "掌機"),
                playTimeHours = 34.6f
            ),
            GameItem(
                id = "streaming_steamlink",
                title = "Steam Link",
                subtitle = "Steam 官方遠端暢玩直連",
                category = GameCategory.STREAMING,
                packageName = "com.valvesoftware.steamlink",
                tags = listOf("Steam", "串流", "遠端"),
                playTimeHours = 22.0f
            )
        )
        _games.value = initialList
    }

    suspend fun scanInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val scannedItems = resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            // 排除自身啟動器
            if (pkg == context.packageName) return@mapNotNull null
            val label = resolveInfo.loadLabel(pm).toString()

            val category = when {
                pkg.contains("tyranor", true) || pkg.contains("kirikiri", true) ||
                        pkg.contains("renpy", true) || pkg.contains("gal", true) -> GameCategory.GALGAME
                pkg.contains("retro", true) || pkg.contains("emu", true) ||
                        pkg.contains("nostalgia", true) || pkg.contains("arcade", true) -> GameCategory.RETRO
                pkg.contains("moonlight", true) || pkg.contains("steam", true) ||
                        pkg.contains("parsec", true) -> GameCategory.STREAMING
                else -> GameCategory.TOOL
            }

            GameItem(
                id = pkg,
                title = label,
                subtitle = pkg,
                category = category,
                packageName = pkg,
                playTimeHours = 0f
            )
        }

        // 合併預置遊戲與掃描出的本機應用 (避免重複)
        val currentList = _games.value.toMutableList()
        scannedItems.forEach { scanned ->
            if (currentList.none { it.packageName == scanned.packageName }) {
                currentList.add(scanned)
            }
        }
        _games.value = currentList
    }

    fun launchGame(item: GameItem): Boolean {
        item.packageName?.let { pkg ->
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }
        return false
    }
}
