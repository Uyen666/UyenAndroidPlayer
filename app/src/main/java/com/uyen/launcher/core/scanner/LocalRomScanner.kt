package com.uyen.launcher.core.scanner

import android.os.Environment
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 掌機本地 ROM 與 Galgame 遊戲目錄掃描器
 * 自動偵測 SDCard/Games 中的 .xp3, .rpa, .nes, .gba, .sfc 遊戲檔案
 */
class LocalRomScanner(baseDir: File? = null) {

    private val gamesRoot: File = File(
        baseDir ?: try {
            Environment.getExternalStorageDirectory()
        } catch (_: Exception) {
            File("/sdcard")
        },
        "Games"
    )
    private val galgameDir = File(gamesRoot, "Galgames")
    private val romsDir = File(gamesRoot, "ROMs")
    private val customDir = File(gamesRoot, "Custom")

    /**
     * 初始化掌機標準遊戲目錄架構
     */
    fun ensureDirectoryStructure(): Boolean {
        return try {
            if (!gamesRoot.exists()) gamesRoot.mkdirs()
            if (!galgameDir.exists()) galgameDir.mkdirs()
            if (!romsDir.exists()) romsDir.mkdirs()
            if (!customDir.exists()) customDir.mkdirs()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 掃描本地遊戲檔案並轉換為 GameItem 清單
     */
    suspend fun scanLocalGames(): List<GameItem> = withContext(Dispatchers.IO) {
        val foundGames = mutableListOf<GameItem>()
        ensureDirectoryStructure()

        val directoriesToScan = listOf(
            galgameDir,
            romsDir,
            customDir,
            File(Environment.getExternalStorageDirectory(), "Download")
        )

        directoriesToScan.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    val extension = file.extension.lowercase()
                    val itemName = file.nameWithoutExtension

                    when (extension) {
                        // Galgame 格式 (吉里吉里 / Ren'Py / Tyranor)
                        "xp3", "rpa", "ons" -> {
                            foundGames.add(
                                GameItem(
                                    id = "local_gal_${file.name.hashCode()}",
                                    title = itemName,
                                    subtitle = "Galgame 本地映像檔 (${file.length() / (1024 * 1024)} MB)",
                                    category = GameCategory.GALGAME,
                                    packageName = "com.tyranor", // 預設優先以 Tyranor 引擎開啓
                                    tags = listOf("本地 Galgame", extension.uppercase()),
                                    playTimeHours = 0f,
                                    isFavorite = false
                                )
                            )
                        }

                        // 復古懷舊 ROM (FC 紅白機 / GBA / SFC)
                        "nes", "fc" -> {
                            foundGames.add(
                                GameItem(
                                    id = "local_nes_${file.name.hashCode()}",
                                    title = itemName,
                                    subtitle = "FC/NES 8-bit ROM (${file.length() / 1024} KB)",
                                    category = GameCategory.RETRO,
                                    packageName = "com.retroarch",
                                    tags = listOf("8-bit", "NES", "本地 ROM"),
                                    playTimeHours = 0f,
                                    isFavorite = false
                                )
                            )
                        }

                        "gba" -> {
                            foundGames.add(
                                GameItem(
                                    id = "local_gba_${file.name.hashCode()}",
                                    title = itemName,
                                    subtitle = "Game Boy Advance ROM (${file.length() / (1024 * 1024)} MB)",
                                    category = GameCategory.RETRO,
                                    packageName = "com.retroarch",
                                    tags = listOf("GBA", "掌機", "本地 ROM"),
                                    playTimeHours = 0f,
                                    isFavorite = false
                                )
                            )
                        }

                        "sfc", "smc" -> {
                            foundGames.add(
                                GameItem(
                                    id = "local_sfc_${file.name.hashCode()}",
                                    title = itemName,
                                    subtitle = "Super Famicom ROM (${file.length() / 1024} KB)",
                                    category = GameCategory.RETRO,
                                    packageName = "com.retroarch",
                                    tags = listOf("16-bit", "SFC", "本地 ROM"),
                                    playTimeHours = 0f,
                                    isFavorite = false
                                )
                            )
                        }
                    }
                }
            }
        }

        foundGames
    }
}
