package com.uyen.launcher.core.banner

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import com.uyen.launcher.data.model.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * 海報來源類型
 */
enum class BannerSourceType(val displayName: String) {
    CUSTOM_USER("自訂相片"),
    CURATED("官方精選"),
    TV_BANNER("Android TV 橫幅"),
    PLAY_STORE("Google Play 宣傳圖"),
    DEFAULT("預設主題")
}

/**
 * 解析後的海報資料
 */
data class ResolvedBanner(
    val imageModel: Any, // File, Uri, or URL String
    val sourceType: BannerSourceType
)

/**
 * 主機級遊戲與應用海報解析器 (Banner Resolution Engine)
 *
 * 嚴格遵循四段式解析層級：
 * 1. 玩家自訂海報 (Custom User Override) -> 本地永久儲存
 * 2. 官方精選海報 (Curated Art) -> 內建或遊戲指定
 * 3. Android TV Leanback 橫幅 (Native Banner) -> 系統 APK 原生提取並快取
 * 4. Google Play Store 官方宣傳圖 (Feature Graphic) -> 智能線上刮削 (1024x500/1080p)
 * 5. 漸層主題備用 (Fallback)
 */
class GameBannerManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("uyen_game_banners_v1", Context.MODE_PRIVATE)

    private val customBannerDir: File = File(context.filesDir, "custom_banners").apply {
        if (!exists()) mkdirs()
    }

    private val tvBannerDir: File = File(context.cacheDir, "tv_banners").apply {
        if (!exists()) mkdirs()
    }

    /**
     * 異步解析單一遊戲的海報 (線程安全，自動走遍四級層次)
     */
    suspend fun resolveBanner(game: GameItem): ResolvedBanner? = withContext(Dispatchers.IO) {
        // 第一級：玩家自訂相片 (優先權最高)
        val customFile = getCustomBannerFile(game.id)
        if (customFile.exists() && customFile.length() > 0) {
            return@withContext ResolvedBanner(customFile.absolutePath, BannerSourceType.CUSTOM_USER)
        }

        // 第二級：官方精選海報
        if (!game.bannerUrl.isNullOrBlank()) {
            return@withContext ResolvedBanner(game.bannerUrl, BannerSourceType.CURATED)
        }

        val pkg = game.packageName
        if (!pkg.isNullOrBlank()) {
            // 第三級：Android TV Leanback 原生橫幅
            val tvBannerPath = getOrExtractTvBanner(pkg)
            if (tvBannerPath != null) {
                return@withContext ResolvedBanner(tvBannerPath, BannerSourceType.TV_BANNER)
            }

            // 第四級：Google Play 官方宣傳海報刮削
            val playStoreUrl = getOrScrapePlayStoreGraphic(pkg)
            if (!playStoreUrl.isNullOrBlank() && playStoreUrl != CACHE_NOT_FOUND) {
                return@withContext ResolvedBanner(playStoreUrl, BannerSourceType.PLAY_STORE)
            }
        }

        // 第五級：無外部海報，返回 null (由 UI 渲染動態主題漸層與暗角)
        null
    }

    /**
     * 儲存玩家手動挑選的自訂海報
     */
    suspend fun setCustomBanner(gameId: String, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val destFile = getCustomBannerFile(gameId)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                // 解碼並智慧縮放，避免 50MB 巨圖消耗掌機過多顯存 (限制最大寬度 1920)
                val originalBitmap = BitmapFactory.decodeStream(input)
                    ?: return@withContext null

                val scaledBitmap = scaleDownBitmap(originalBitmap, maxDimension = 1920)
                FileOutputStream(destFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                if (scaledBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }
                scaledBitmap.recycle()
            }

            prefs.edit().putBoolean("is_custom_$gameId", true).apply()
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 還原為自動海報 (清除玩家自訂)
     */
    suspend fun resetCustomBanner(gameId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val customFile = getCustomBannerFile(gameId)
            if (customFile.exists()) {
                customFile.delete()
            }
            prefs.edit().remove("is_custom_$gameId").apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 檢查遊戲當前是否由玩家手動自訂海報
     */
    fun isCustomBanner(gameId: String): Boolean {
        val file = getCustomBannerFile(gameId)
        return file.exists() && file.length() > 0 && prefs.getBoolean("is_custom_$gameId", false)
    }

    /**
     * 取得已快取或從 APK 原生提取 Android TV Leanback Banner
     */
    private fun getOrExtractTvBanner(packageName: String): String? {
        val cachedFile = File(tvBannerDir, "$packageName.png")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile.absolutePath
        }

        return try {
            val pm = context.packageManager
            val drawable: Drawable? = pm.getApplicationBanner(packageName)
                ?: pm.getApplicationInfo(packageName, 0).loadBanner(pm)

            if (drawable != null) {
                val bitmap = drawableToBitmap(drawable)
                FileOutputStream(cachedFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
                cachedFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 取得已快取或線上爬取 Google Play Store 宣傳橫幅 (Feature Graphic)
     */
    suspend fun getOrScrapePlayStoreGraphic(packageName: String, forceRefresh: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            val cacheKey = "playstore_graphic_$packageName"
            if (!forceRefresh) {
                val cached = prefs.getString(cacheKey, null)
                if (cached != null) return@withContext if (cached == CACHE_NOT_FOUND) null else cached
            }

            val scrapedUrl = scrapeGooglePlayFeatureGraphic(packageName)
            if (scrapedUrl != null) {
                // 將圖片 URL 最佳化為高清 1920x1080 規格
                val highResUrl = optimizeGoogleImageUrl(scrapedUrl)
                prefs.edit().putString(cacheKey, highResUrl).apply()
                highResUrl
            } else {
                // 標記未找到 (快取以避免無意義的反覆聯網重試)
                prefs.edit().putString(cacheKey, CACHE_NOT_FOUND).apply()
                null
            }
        }

    /**
     * 線上爬取 Google Play 頁面中的 og:image 宣傳海報
     */
    private fun scrapeGooglePlayFeatureGraphic(packageName: String): String? {
        return try {
            val storeUrl = "https://play.google.com/store/apps/details?id=$packageName&hl=en"
            val conn = URL(storeUrl).openConnection() as HttpURLConnection
            conn.apply {
                connectTimeout = 4500
                readTimeout = 4500
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
            }

            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }

            val html = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            extractPlayStoreImageUrl(html)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 靜態解析方法：從 HTML 提取 og:image 或 play-lh 宣傳海報
     */
    internal fun extractPlayStoreImageUrl(html: String): String? {
        // 匹配 <meta property="og:image" content="(https://play-lh.googleusercontent.com/[^"]+)"
        val metaMatcher = OG_IMAGE_PATTERN.matcher(html)
        if (metaMatcher.find()) {
            val url = metaMatcher.group(1)
            if (!url.isNullOrBlank()) return url
        }

        // 次要備用匹配：直接搜尋 play-lh.googleusercontent.com 圖片
        val fallbackMatcher = PLAY_LH_PATTERN.matcher(html)
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group(0)
        }
        return null
    }

    /**
     * 將 Google User Content 圖片 URL 調校為 1920x1080 超清晰無壓縮版本
     */
    internal fun optimizeGoogleImageUrl(url: String): String {
        return if (url.contains("googleusercontent.com")) {
            val base = if (url.contains("=")) url.substringBefore("=") else url
            "$base=w1920-h1080"
        } else {
            url
        }
    }

    private fun getCustomBannerFile(gameId: String): File {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(customBannerDir, "$safeId.jpg")
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 320
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 180
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (ratio > 1) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    companion object {
        private const val CACHE_NOT_FOUND = "__NOT_FOUND__"

        private val OG_IMAGE_PATTERN: Pattern = Pattern.compile(
            """<meta\s+property=["']og:image["']\s+content=["'](https://play-lh\.googleusercontent\.com/[^"']+)["']""",
            Pattern.CASE_INSENSITIVE
        )

        private val PLAY_LH_PATTERN: Pattern = Pattern.compile(
            """https://play-lh\.googleusercontent\.com/[a-zA-Z0-9_\-]+(=[a-zA-Z0-9_\-]+)?"""
        )
    }
}
