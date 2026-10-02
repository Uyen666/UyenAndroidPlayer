package com.uyen.launcher

import com.uyen.launcher.core.controller.VirtualGamepadState
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.data.model.PlayerProfile
import com.uyen.launcher.data.model.SystemStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        assertEquals(90, stats.fps)
        assertEquals(90, stats.refreshRateFps)
        assertTrue(stats.batteryPercent in 0..100)
        assertTrue(stats.batteryTempCelsius > 0)
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
            packageName = "com.tyranor",
            memoryUsageMb = 120
        )

        assertEquals("galgame_tyranor", task.id)
        assertEquals("com.tyranor", task.packageName)
        assertEquals(120L, task.memoryUsageMb)

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
    fun testLocalRomScannerDirectoryStructure() {
        val testBase = java.io.File(System.getProperty("java.io.tmpdir"), "uyen_test_dir")
        val scanner = com.uyen.launcher.core.scanner.LocalRomScanner(baseDir = testBase)
        assertNotNull(scanner)
        assertTrue(scanner.ensureDirectoryStructure())
        testBase.deleteRecursively()
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
}
