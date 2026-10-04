package com.uyen.launcher

import com.uyen.launcher.core.controller.VirtualGamepadState
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.data.model.PlayerProfile
import com.uyen.launcher.data.model.SystemStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UyenLauncher 核心商業邏輯與資料單元測試
 */
class UyenLauncherUnitTest {

    @Test
    fun testDefaultPlayerProfile() {
        val profile = PlayerProfile()
        assertEquals("Uyen", profile.username)
        assertEquals(99, profile.level)
        assertTrue(profile.badge.contains("PRO CONSOLE"))
    }

    @Test
    fun testSystemStatsDefaults() {
        val stats = SystemStats()
        assertEquals(0, stats.fps)
        assertEquals(0L, stats.ramUsedMb)
        assertEquals(0L, stats.ramTotalMb)
        assertEquals(0, stats.batteryPercent)
        assertEquals(0f, stats.batteryTempCelsius, 0.001f)
        assertFalse(stats.isCharging)
    }

    @Test
    fun testGameCategoriesIntegrity() {
        val categories = GameCategory.entries
        assertTrue(categories.contains(GameCategory.ALL))
        assertTrue(categories.contains(GameCategory.GALGAME))
        assertTrue(categories.contains(GameCategory.RETRO))
        assertTrue(categories.contains(GameCategory.CUSTOM))
        assertTrue(categories.contains(GameCategory.STREAMING))
        assertTrue(categories.contains(GameCategory.TOOL))
    }

    @Test
    fun testGameItemCreationAndTags() {
        val item = GameItem(
            id = "test_game",
            title = "Test VN",
            category = GameCategory.GALGAME,
            tags = listOf("AVG", "KRKR"),
            playTimeHours = 10.5f,
            isFavorite = true
        )

        assertEquals("test_game", item.id)
        assertEquals("Test VN", item.title)
        assertEquals(GameCategory.GALGAME, item.category)
        assertEquals(2, item.tags.size)
        assertTrue(item.isFavorite)
    }

    @Test
    fun testVirtualGamepadStateDefaultNeutral() {
        val state = VirtualGamepadState()
        assertEquals(0f, state.leftStickX, 0.001f)
        assertEquals(0f, state.leftStickY, 0.001f)
        assertEquals(0f, state.rightStickX, 0.001f)
        assertEquals(0f, state.rightStickY, 0.001f)
        assertFalse(state.btnA)
        assertFalse(state.btnB)
        assertFalse(state.btnX)
        assertFalse(state.btnY)
        assertFalse(state.dpadUp)
        assertFalse(state.dpadDown)
        assertEquals(0f, state.btnL2, 0.001f)
        assertEquals(0f, state.btnR2, 0.001f)
    }

    @Test
    fun testVirtualGamepadStateUpdates() {
        val initial = VirtualGamepadState()
        val pressedA = initial.copy(btnA = true, leftStickX = 0.75f, btnR2 = 1.0f)

        assertTrue(pressedA.btnA)
        assertEquals(0.75f, pressedA.leftStickX, 0.001f)
        assertEquals(1.0f, pressedA.btnR2, 0.001f)
        assertFalse(pressedA.btnB)
    }

    @Test
    fun testRunningTaskCreationAndLifecycle() {
        val task = com.uyen.launcher.data.model.RunningTask(
            id = "galgame_tyranor",
            title = "Tyranor",
            packageName = "com.tyranor"
        )

        assertEquals("galgame_tyranor", task.id)
        assertEquals("com.tyranor", task.packageName)

        val taskList = mutableListOf(task)
        assertEquals(1, taskList.size)

        taskList.removeIf { it.id == "galgame_tyranor" }
        assertTrue(taskList.isEmpty())
    }

    @Test
    fun testRetroArcadeBulletAndInvader() {
        val bullet = com.uyen.launcher.presentation.minigame.Bullet(x = 0.5f, y = 0.8f)
        assertEquals(0.5f, bullet.x, 0.001f)
        assertEquals(0.8f, bullet.y, 0.001f)
        assertTrue(bullet.vy < 0) // 子彈向上飛

        val invader = com.uyen.launcher.presentation.minigame.Invader(
            x = 0.3f,
            y = 0.1f,
            vx = 0.01f,
            vy = 0.02f,
            hp = 2,
            type = 1
        )
        assertEquals(2, invader.hp)
        assertEquals(1, invader.type)
        assertEquals(0.3f, invader.x, 0.001f)
    }

    @Test
    fun testLocalRomScannerCategoryExtensions() {
        assertEquals(GameCategory.GALGAME, com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("xp3"))
        assertEquals(GameCategory.GALGAME, com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("rpa"))
        assertEquals(GameCategory.GALGAME, com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("ons"))
        assertEquals(GameCategory.RETRO, com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("nes"))
        assertEquals(GameCategory.RETRO, com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("fc"))
        assertEquals(GameCategory.RETRO, com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("gba"))
        assertEquals(GameCategory.RETRO, com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("sfc"))
        assertEquals(GameCategory.RETRO, com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("smc"))
        org.junit.Assert.assertNull(com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("txt"))
        org.junit.Assert.assertNull(com.uyen.launcher.core.scanner.LocalRomScanner.categoryForExtension("apk"))
    }

    @Test
    fun testHardwareBrightnessAndVolumeBounds() {
        val volumePercent = 0.85f.coerceIn(0f, 1f)
        assertEquals(0.85f, volumePercent, 0.001f)

        val brightnessOver = 1.5f.coerceIn(0.05f, 1.0f)
        assertEquals(1.0f, brightnessOver, 0.001f)

        val brightnessUnder = 0.01f.coerceIn(0.05f, 1.0f)
        assertEquals(0.05f, brightnessUnder, 0.001f)
    }

    @Test
    fun testConsoleLockManagerCommands() {
        val cmd = com.uyen.launcher.core.kiosk.ConsoleLockManager.getDeviceOwnerAdbCommand()
        assertTrue(cmd.contains("dpm set-device-owner"))
        assertTrue(cmd.contains("UyenDeviceAdminReceiver"))

        val statusCmd = com.uyen.launcher.core.kiosk.ConsoleLockManager.getStatusBarDisableAdbCommand()
        assertTrue(statusCmd.contains("statusbar-expansion"))
        assertTrue(statusCmd.contains("notification-peek"))
    }

    @Test
    fun testMainNavTabIntegrity() {
        val tabs = com.uyen.launcher.data.model.MainNavTab.entries
        assertEquals(3, tabs.size)
        assertEquals("首頁", com.uyen.launcher.data.model.MainNavTab.HOME.displayName)
        assertEquals("串流", com.uyen.launcher.data.model.MainNavTab.STREAMING.displayName)
        assertEquals("遊戲", com.uyen.launcher.data.model.MainNavTab.GAMES.displayName)
    }

    @Test
    fun testGoogleAccountDefaults() {
        val account = com.uyen.launcher.data.model.GoogleAccount()
        assertEquals("訪客", account.displayName)
        assertEquals("", account.email)
        assertFalse(account.isConnected)
        assertTrue(account.dataStatusMessage.contains("本機"))
    }

    @Test
    fun testTabFilteringLogic() {
        val testGames = listOf(
            GameItem(id = "home_item", title = "All Item", category = GameCategory.ALL),
            GameItem(id = "stream_item", title = "Moonlight", category = GameCategory.STREAMING),
            GameItem(id = "controller_mode", title = "UyenController", category = GameCategory.CUSTOM),
            GameItem(id = "galgame_item", title = "Tyranor", category = GameCategory.GALGAME),
            GameItem(id = "retro_item", title = "8-bit", category = GameCategory.RETRO)
        )

        // HOME 標籤包含所有項目
        assertEquals(5, testGames.size)

        // STREAMING 標籤過濾
        val streamingGames = testGames.filter {
            it.category == GameCategory.STREAMING || it.id == "controller_mode"
        }
        assertEquals(2, streamingGames.size)
        assertTrue(streamingGames.any { it.id == "stream_item" })
        assertTrue(streamingGames.any { it.id == "controller_mode" })

        // GAMES 標籤過濾
        val handheldGames = testGames.filter {
            it.category == GameCategory.GALGAME ||
            it.category == GameCategory.RETRO ||
            it.category == GameCategory.CUSTOM
        }
        assertEquals(3, handheldGames.size)
        assertTrue(handheldGames.any { it.id == "galgame_item" })
        assertTrue(handheldGames.any { it.id == "retro_item" })
    }

    @Test
    fun testGoogleAccountCustomDisplay() {
        val email = "player@example.com"
        val prefix = email.substringBefore("@")
        val displayName = prefix.replaceFirstChar { it.uppercase() }
        val account = com.uyen.launcher.data.model.GoogleAccount(
            email = email,
            displayName = displayName,
            isConnected = true
        )

        assertEquals("player@example.com", account.email)
        assertEquals("Player", account.displayName)
        assertTrue(account.isConnected)
    }

    @Test
    fun testAccessibilityServiceSafetyWhenDisconnected() {
        // 服務未連線時不拋出異常並回傳 false
        val backHandled = com.uyen.launcher.core.service.UyenConsoleAccessibilityService.performBack()
        assertFalse(backHandled)

        val homeHandled = com.uyen.launcher.core.service.UyenConsoleAccessibilityService.performHome()
        assertFalse(homeHandled)
    }

    @Test
    fun testCleanRamResultCalculation() {
        // 模擬真實釋放場景
        val resultFreed = com.uyen.launcher.core.hardware.CleanRamResult(
            freedMb = 250,
            availBeforeMb = 1000,
            availAfterMb = 1250,
            totalMb = 3700,
            usedMb = 2450,
            displayMessage = "⚡ 釋放成功！已清出 250MB 記憶體 (可用 RAM: 1250MB)"
        )
        assertEquals(250, resultFreed.freedMb)
        assertEquals(1250, resultFreed.availAfterMb)
        assertTrue(resultFreed.displayMessage.contains("250MB"))

        // 模擬已達最佳狀態場景 (拒絕重複展示假數字)
        val resultOptimal = com.uyen.launcher.core.hardware.CleanRamResult(
            freedMb = 0,
            availBeforeMb = 1250,
            availAfterMb = 1250,
            totalMb = 3700,
            usedMb = 2450,
            displayMessage = "⚡ 記憶體已達最佳狀態！目前可用 RAM: 1250MB"
        )
        assertEquals(0, resultOptimal.freedMb)
        assertTrue(resultOptimal.displayMessage.contains("最佳狀態"))
        assertFalse(resultOptimal.displayMessage.contains("快取程序"))
    }

    @Test
    fun testGalgameEngineDetectionKirikiri() {
        val files = listOf("data.xp3", "patch.xp3", "cover.jpg", "system.xp3")
        val engine = com.uyen.launcher.core.scanner.LocalRomScanner.detectGalgameEngine(files)
        assertNotNull(engine)
        assertTrue(engine!!.engineName.contains("吉里吉里"))
        assertEquals("application/x-xp3", engine.mimeType)
        assertTrue(engine.tags.contains("Kirikiri"))
    }

    @Test
    fun testGalgameEngineDetectionRenPy() {
        val files = listOf("game.rpa", "options.rpy", "cover.png")
        val engine = com.uyen.launcher.core.scanner.LocalRomScanner.detectGalgameEngine(files)
        assertNotNull(engine)
        assertTrue(engine!!.engineName.contains("Ren'Py"))
        assertEquals("application/x-rpa", engine.mimeType)
        assertTrue(engine.tags.contains("Ren'Py"))
    }

    @Test
    fun testGalgameEngineDetectionTyrano() {
        val files = listOf("index.html", "tyrano.js", "data")
        val engine = com.uyen.launcher.core.scanner.LocalRomScanner.detectGalgameEngine(files)
        assertNotNull(engine)
        assertTrue(engine!!.engineName.contains("Tyrano"))
        assertEquals("text/html", engine.mimeType)
        assertTrue(engine.tags.contains("Tyrano"))
    }

    @Test
    fun testGalgameEngineDetectionWolfRpg() {
        val files = listOf("Game.exe", "data.wolf", "folder.jpg")
        val engine = com.uyen.launcher.core.scanner.LocalRomScanner.detectGalgameEngine(files)
        assertNotNull(engine)
        assertTrue(engine!!.engineName.contains("Wolf RPG"))
        assertEquals("application/x-msdos-program", engine.mimeType)
    }

    @Test
    fun testGalgameEngineDetectionNegative() {
        val files = listOf("document.txt", "video.mp4", "music.mp3")
        val engine = com.uyen.launcher.core.scanner.LocalRomScanner.detectGalgameEngine(files)
        assertNull(engine)
    }

    @Test
    fun testCoverFileNameRecognition() {
        assertTrue(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("cover.jpg"))
        assertTrue(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("Cover.PNG"))
        assertTrue(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("folder.jpg"))
        assertTrue(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("POSTER.webp"))
        assertTrue(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("thumb.jpg"))
        assertTrue(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("front_cover.jpeg"))

        assertFalse(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("data.xp3"))
        assertFalse(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("readme.txt"))
        assertFalse(com.uyen.launcher.core.scanner.LocalRomScanner.isCoverFileName("bg_01.jpg"))
    }

    @Test
    fun testFormatGameSize() {
        val gbStr = com.uyen.launcher.core.scanner.LocalRomScanner.formatGameSize(3_400_000_000L)
        assertTrue(gbStr.contains("GB"))

        val mbStr = com.uyen.launcher.core.scanner.LocalRomScanner.formatGameSize(250_000_000L)
        assertTrue(mbStr.contains("MB"))

        val zeroStr = com.uyen.launcher.core.scanner.LocalRomScanner.formatGameSize(0L)
        assertEquals("", zeroStr)
    }

    @Test
    fun testGalgameItemCreationAndAttributes() {
        val game = GameItem(
            id = "local_dir:senren_banka",
            title = "千戀萬花",
            subtitle = "吉里吉里 2/Z (Kirikiri) • 3.2 GB",
            category = GameCategory.GALGAME,
            launchIntentUri = "content://launch_target",
            mimeType = "application/x-xp3",
            coverUrl = "content://cover_jpg",
            bannerUrl = "content://cover_jpg",
            tags = listOf("Galgame", "吉里吉里", "Kirikiri")
        )

        assertEquals("local_dir:senren_banka", game.id)
        assertEquals("千戀萬花", game.title)
        assertEquals(GameCategory.GALGAME, game.category)
        assertEquals("content://cover_jpg", game.coverUrl)
        assertEquals("content://cover_jpg", game.bannerUrl)
        assertTrue(game.tags.contains("吉里吉里"))
    }

    @Test
    fun testShouldClearDebugAppLogic() {
        val ourPackage = "com.uyen.launcher"
        assertTrue(com.uyen.launcher.core.hardware.SystemControlManager.shouldClearDebugApp("com.uyen.launcher", ourPackage))
        assertTrue(com.uyen.launcher.core.hardware.SystemControlManager.shouldClearDebugApp(" com.uyen.launcher ", ourPackage))
        assertFalse(com.uyen.launcher.core.hardware.SystemControlManager.shouldClearDebugApp(null, ourPackage))
        assertFalse(com.uyen.launcher.core.hardware.SystemControlManager.shouldClearDebugApp("", ourPackage))
        assertFalse(com.uyen.launcher.core.hardware.SystemControlManager.shouldClearDebugApp("   ", ourPackage))
        assertFalse(com.uyen.launcher.core.hardware.SystemControlManager.shouldClearDebugApp("com.other.app", ourPackage))
    }
}

