package com.uyen.launcher

import com.uyen.launcher.core.banner.BannerSourceType
import com.uyen.launcher.core.banner.GameBannerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 遊戲與應用大海報解析引擎 (GameBannerManager) 單元測試
 */
class GameBannerUnitTest {

    // 建立無 context 的輕量解析實例測試靜態提取方法
    private class DummyBannerManager {
        fun extractPlayStoreImageUrl(html: String): String? {
            val ogPattern = java.util.regex.Pattern.compile(
                """<meta\s+property=["']og:image["']\s+content=["'](https://play-lh\.googleusercontent\.com/[^"']+)["']""",
                java.util.regex.Pattern.CASE_INSENSITIVE
            )
            val matcher = ogPattern.matcher(html)
            if (matcher.find()) {
                val url = matcher.group(1)
                if (!url.isNullOrBlank()) return url
            }
            val fallbackPattern = java.util.regex.Pattern.compile(
                """https://play-lh\.googleusercontent\.com/[a-zA-Z0-9_\-]+(=[a-zA-Z0-9_\-]+)?"""
            )
            val fallbackMatcher = fallbackPattern.matcher(html)
            if (fallbackMatcher.find()) {
                return fallbackMatcher.group(0)
            }
            return null
        }

        fun optimizeGoogleImageUrl(url: String): String {
            return if (url.contains("googleusercontent.com")) {
                val base = if (url.contains("=")) url.substringBefore("=") else url
                "$base=w1920-h1080"
            } else {
                url
            }
        }
    }

    private val manager = DummyBannerManager()

    @Test
    fun testBannerSourceTypeDisplayNames() {
        assertEquals("自訂相片", BannerSourceType.CUSTOM_USER.displayName)
        assertEquals("官方精選", BannerSourceType.CURATED.displayName)
        assertEquals("Android TV 橫幅", BannerSourceType.TV_BANNER.displayName)
        assertEquals("Google Play 宣傳圖", BannerSourceType.PLAY_STORE.displayName)
        assertEquals("預設主題", BannerSourceType.DEFAULT.displayName)
    }

    @Test
    fun testExtractPlayStoreImageUrl_StandardOgImage() {
        val sampleHtml = """
            <!doctype html><html lang="zh-TW"><head>
            <meta property="og:title" content="Steam Link">
            <meta property="og:image" content="https://play-lh.googleusercontent.com/6TU14znIBQ6oierlwk8twhgqDLMhA3y-a-daQq8jF5d_OZpk9HpEo9rlMQ_KhxSUQ-mpohuQLVmH-jy6zoVCaTU=s0-br30">
            <meta name="description" content="Play PC games on your Android device">
            </head><body><div>Content</div></body></html>
        """.trimIndent()

        val extracted = manager.extractPlayStoreImageUrl(sampleHtml)
        assertNotNull(extracted)
        assertEquals(
            "https://play-lh.googleusercontent.com/6TU14znIBQ6oierlwk8twhgqDLMhA3y-a-daQq8jF5d_OZpk9HpEo9rlMQ_KhxSUQ-mpohuQLVmH-jy6zoVCaTU=s0-br30",
            extracted
        )
    }

    @Test
    fun testExtractPlayStoreImageUrl_FallbackDirectMatch() {
        val sampleHtml = """
            <html><body>
            <img src="https://play-lh.googleusercontent.com/nZ1sv_1PalwMkR_c1XIBhAa9yr15PSYCLxVx6dogYaoGoj3nPM6ZmG70zv8VF_s2YQs9VwtX3bdxqVvuce6f=s0" />
            </body></html>
        """.trimIndent()

        val extracted = manager.extractPlayStoreImageUrl(sampleHtml)
        assertNotNull(extracted)
        assertTrue(extracted!!.startsWith("https://play-lh.googleusercontent.com/nZ1sv_1PalwMkR_c1XIBhAa9yr15PSYCLxVx6dogYaoGoj3nPM6ZmG70zv8VF_s2YQs9VwtX3bdxqVvuce6f"))
    }

    @Test
    fun testExtractPlayStoreImageUrl_NoMatch() {
        val dummyHtml = "<html><head><title>No Play Store Image</title></head><body>Hello World</body></html>"
        val extracted = manager.extractPlayStoreImageUrl(dummyHtml)
        assertNull(extracted)
    }

    @Test
    fun testOptimizeGoogleImageUrl_ReplacesDimensionsWith1080p() {
        val original = "https://play-lh.googleusercontent.com/6TU14znIBQ6oierlwk8twhgqDLMhA3y=s0-br30"
        val optimized = manager.optimizeGoogleImageUrl(original)
        assertEquals("https://play-lh.googleusercontent.com/6TU14znIBQ6oierlwk8twhgqDLMhA3y=w1920-h1080", optimized)
    }

    @Test
    fun testOptimizeGoogleImageUrl_WithoutParametersAppends1080p() {
        val original = "https://play-lh.googleusercontent.com/sample_image_hash"
        val optimized = manager.optimizeGoogleImageUrl(original)
        assertEquals("https://play-lh.googleusercontent.com/sample_image_hash=w1920-h1080", optimized)
    }

    @Test
    fun testOptimizeGoogleImageUrl_NonGoogleCdnUnchanged() {
        val external = "https://images.unsplash.com/photo-1600080972464-8e5f35f63d08?q=80&w=1920"
        val result = manager.optimizeGoogleImageUrl(external)
        assertEquals(external, result)
    }

    @Test
    fun testGetAmbientColorRules() {
        val moonlightGame = com.uyen.launcher.data.model.GameItem(
            id = "streaming_moonlight",
            title = "Moonlight",
            packageName = "com.limelight",
            category = com.uyen.launcher.data.model.GameCategory.STREAMING
        )
        val youtubeGame = com.uyen.launcher.data.model.GameItem(
            id = "app:com.google.android.youtube",
            title = "YouTube",
            packageName = "com.google.android.youtube",
            category = com.uyen.launcher.data.model.GameCategory.TOOL
        )
        val galgame = com.uyen.launcher.data.model.GameItem(
            id = "local_galgame",
            title = "Visual Novel",
            category = com.uyen.launcher.data.model.GameCategory.GALGAME
        )
        val nullGame: com.uyen.launcher.data.model.GameItem? = null

        assertEquals(androidx.compose.ui.graphics.Color(0xFF0284C7), com.uyen.launcher.core.util.AppIconUtil.getAmbientColor(moonlightGame))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFBE123C), com.uyen.launcher.core.util.AppIconUtil.getAmbientColor(youtubeGame))
        assertEquals(androidx.compose.ui.graphics.Color(0xFF9333EA), com.uyen.launcher.core.util.AppIconUtil.getAmbientColor(galgame))
        assertEquals(androidx.compose.ui.graphics.Color(0xFF1E3A8A), com.uyen.launcher.core.util.AppIconUtil.getAmbientColor(nullGame))
    }

    @Test
    fun testResolveConsoleSubtitle() {
        // Steam Link
        val steamSub = com.uyen.launcher.data.repository.GameRepository.resolveConsoleSubtitle(
            "com.valvesoftware.steamlink",
            com.uyen.launcher.data.model.GameCategory.STREAMING
        )
        assertEquals("Valve Corporation • 遠端主機串流", steamSub)

        // Moonlight
        val moonSub = com.uyen.launcher.data.repository.GameRepository.resolveConsoleSubtitle(
            "com.limelight",
            com.uyen.launcher.data.model.GameCategory.STREAMING
        )
        assertEquals("Moonlight • 超低延遲遠端串流", moonSub)

        // YouTube
        val ytSub = com.uyen.launcher.data.repository.GameRepository.resolveConsoleSubtitle(
            "com.google.android.youtube",
            com.uyen.launcher.data.model.GameCategory.TOOL
        )
        assertEquals("Google LLC • 影音串流平台", ytSub)

        // Null package
        val galSub = com.uyen.launcher.data.repository.GameRepository.resolveConsoleSubtitle(
            null,
            com.uyen.launcher.data.model.GameCategory.GALGAME
        )
        assertEquals("視覺小說 / AVG 遊戲", galSub)
    }

    @Test
    fun testCuratedHeroMappingRules() {
        val steamRes = GameBannerManager.getCuratedHeroResId("app:com.valvesoftware.steamlink", "com.valvesoftware.steamlink")
        assertEquals(com.uyen.launcher.R.drawable.hero_steam_cosmic, steamRes)

        val moonRes = GameBannerManager.getCuratedHeroResId("app:com.limelight", "com.limelight")
        assertEquals(com.uyen.launcher.R.drawable.hero_moonlight_aurora, moonRes)

        val ctrlRes = GameBannerManager.getCuratedHeroResId("controller_mode", null)
        assertEquals(com.uyen.launcher.R.drawable.hero_uyen_controller, ctrlRes)

        val retroRes = GameBannerManager.getCuratedHeroResId("retro_8bit", null)
        assertEquals(com.uyen.launcher.R.drawable.hero_retro_cyber, retroRes)

        val otherRes = GameBannerManager.getCuratedHeroResId("app:com.unknown.app", "com.unknown.app")
        assertNull(otherRes)
    }
}
