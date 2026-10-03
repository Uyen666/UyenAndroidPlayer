package com.uyen.launcher.data.model

enum class MainNavTab(val displayName: String) {
    HOME("首頁"),
    STREAMING("串流"),
    GAMES("遊戲")
}

data class GoogleAccount(
    val email: String = "linshangkai@gmail.com",
    val displayName: String = "尚楷",
    val avatarUrl: String? = null,
    val isConnected: Boolean = true,
    val cloudSyncStatus: String = "已同步雲端存檔"
)

enum class GameCategory(val displayName: String) {
    ALL("全部遊戲"),
    GALGAME("Galgame"),
    RETRO("復古 8-bit"),
    CUSTOM("自製小遊戲"),
    STREAMING("主機串流"),
    TOOL("系統工具")
}

data class GameItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val category: GameCategory,
    val packageName: String? = null,
    val launchIntentUri: String? = null,
    val coverUrl: String? = null,
    val bannerUrl: String? = null,
    val playTimeHours: Float = 0f,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false
)

data class PlayerProfile(
    val username: String = "Uyen",
    val level: Int = 99,
    val title: String = "Master Handheld Gamer",
    val badge: String = "👑 PRO CONSOLE",
    val avatarUrl: String? = null
)

data class SystemStats(
    val fps: Int = 90,
    val cpuUsagePercent: Int = 18,
    val ramUsedMb: Long = 2350,
    val ramTotalMb: Long = 3808,
    val batteryPercent: Int = 93,
    val batteryTempCelsius: Float = 33.8f,
    val isCharging: Boolean = true,
    val refreshRateFps: Int = 90
)

data class RunningTask(
    val id: String,
    val title: String,
    val packageName: String,
    val memoryUsageMb: Long = 85,
    val startTimeMillis: Long = System.currentTimeMillis()
)
