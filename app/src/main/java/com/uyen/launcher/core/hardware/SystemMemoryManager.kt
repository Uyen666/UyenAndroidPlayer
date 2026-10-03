package com.uyen.launcher.core.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import coil.Coil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 掌機系統真實記憶體釋放與優化管理器 (System Memory Manager)
 * 解決「假釋放 / 固定跳展示文字」的問題，真正執行：
 * 1. 測量操作前真實可用 RAM (ActivityManager.MemoryInfo)
 * 2. 清理啟動器本體快取 (Coil 圖片記憶體快取與垃圾回收 System.gc())
 * 3. 收集真實具備介面/可執行之後台第三方與預裝用戶應用程式 (如 YouTube, Chrome, 影音, 社群, 遊戲)
 * 4. 透過 Android ActivityManager.killBackgroundProcesses 進行底層程序回收
 * 5. 測量操作後真實可用 RAM，動態計算釋放的 MB 數與當前真實空閒記憶體
 */
data class CleanRamResult(
    val freedMb: Int,
    val availBeforeMb: Int,
    val availAfterMb: Int,
    val totalMb: Int,
    val usedMb: Int,
    val displayMessage: String
)

object SystemMemoryManager {
    private const val TAG = "SystemMemoryManager"

    fun getCurrentMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        return memInfo
    }

    suspend fun cleanRam(
        context: Context,
        additionalPackages: List<String> = emptyList()
    ): CleanRamResult = withContext(Dispatchers.Default) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val pm = context.packageManager
        val myPkg = context.packageName

        // 1. 獲取清理前精確真實數據
        val memBefore = ActivityManager.MemoryInfo().apply { am?.getMemoryInfo(this) }
        val availBeforeMb = (memBefore.availMem / (1024L * 1024L)).toInt()
        val totalMb = (memBefore.totalMem / (1024L * 1024L)).toInt()

        // 2. 清理本體內部快取
        try {
            Coil.imageLoader(context).memoryCache?.clear()
        } catch (_: Throwable) {}
        System.gc()
        Runtime.getRuntime().gc()

        // 3. 收集應終止後台的候選名單
        val targets = mutableSetOf<String>()
        targets.addAll(additionalPackages.filter { it.isNotBlank() && it != myPkg })

        try {
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                if (app.packageName == myPkg) continue

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null

                // 排除系統底層服務 (輸入法、動態桌布、鎖屏保護)
                val isProtected = app.packageName.contains("inputmethod") ||
                        app.packageName.contains("wallpaper") ||
                        app.packageName.contains("keyguard") ||
                        app.packageName == "android"

                // 包含所有非系統安裝 App，以及具備啟動介面的重度預裝用戶 App (如 YouTube, Chrome, 瀏覽器等)
                if ((!isSystem || hasLauncher) && !isProtected) {
                    targets.add(app.packageName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing packages: ${e.message}")
        }

        // 4. 真正執行 killBackgroundProcesses
        for (pkg in targets) {
            try {
                am?.killBackgroundProcesses(pkg)
            } catch (_: Exception) {}
        }

        // 5. 等待 Linux 核心與 Android LMK 完成記憶體頁釋放
        delay(350)
        System.gc()

        // 6. 獲取清理後真實數據
        val memAfter = ActivityManager.MemoryInfo().apply { am?.getMemoryInfo(this) }
        val availAfterMb = (memAfter.availMem / (1024L * 1024L)).toInt()
        val freedMb = (availAfterMb - availBeforeMb).coerceAtLeast(0)
        val usedMb = (totalMb - availAfterMb).coerceAtLeast(0)

        val message = if (freedMb > 0) {
            "⚡ 釋放成功！已清出 ${freedMb}MB 記憶體 (可用 RAM: ${availAfterMb}MB)"
        } else {
            "⚡ 記憶體已達最佳狀態！目前可用 RAM: ${availAfterMb}MB"
        }

        CleanRamResult(
            freedMb = freedMb,
            availBeforeMb = availBeforeMb,
            availAfterMb = availAfterMb,
            totalMb = totalMb,
            usedMb = usedMb,
            displayMessage = message
        )
    }
}
