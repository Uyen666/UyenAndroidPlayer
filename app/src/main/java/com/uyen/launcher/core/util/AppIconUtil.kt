package com.uyen.launcher.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem

/**
 * 掌機遊戲與應用圖標載入快取與藝術主題渲染器
 * 1. 提供 LRU 高性能應用圖標快取，杜絕反覆 IPC 查詢造成的滑動卡頓
 * 2. 提供全卡片滿版藝術主題漸層 (Full-bleed Artwork Gradients)
 */
object AppIconUtil {

    private val iconCache = object : LruCache<String, ImageBitmap>(64) {}

    fun getAppIcon(context: Context, packageName: String?): ImageBitmap? {
        if (packageName.isNullOrBlank()) return null

        iconCache.get(packageName)?.let { return it }

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceIn(96, 256) else 144
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceIn(96, 256) else 144
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
            val imageBitmap = bitmap.asImageBitmap()
            iconCache.put(packageName, imageBitmap)
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 針對遊戲提供專屬滿版藝術背景漸層
     */
    fun getArtworkGradient(game: GameItem): Brush {
        return when {
            game.id == "streaming_moonlight" -> Brush.linearGradient(
                listOf(Color(0xFF022C22), Color(0xFF065F46), Color(0xFF0284C7))
            )
            game.id == "streaming_steamlink" -> Brush.linearGradient(
                listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF0284C7))
            )
            game.id == "controller_mode" -> Brush.linearGradient(
                listOf(Color(0xFF0F172A), Color(0xFF14532D), Color(0xFF047857))
            )
            game.id == "galgame_tyranor" -> Brush.linearGradient(
                listOf(Color(0xFF3B0764), Color(0xFF701A75), Color(0xFF831843))
            )
            game.id == "retro_8bit" -> Brush.linearGradient(
                listOf(Color(0xFF431407), Color(0xFF9A3412), Color(0xFFB45309))
            )
            game.category == GameCategory.GALGAME -> Brush.linearGradient(
                listOf(Color(0xFF3B0764), Color(0xFF581C87), Color(0xFF831843))
            )
            game.category == GameCategory.RETRO -> Brush.linearGradient(
                listOf(Color(0xFF451A03), Color(0xFF7C2D12), Color(0xFFC2410C))
            )
            game.category == GameCategory.STREAMING -> Brush.linearGradient(
                listOf(Color(0xFF082F49), Color(0xFF0369A1), Color(0xFF0284C7))
            )
            game.category == GameCategory.CUSTOM -> Brush.linearGradient(
                listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF0D9488))
            )
            game.category == GameCategory.TOOL -> Brush.linearGradient(
                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
            )
            else -> Brush.linearGradient(
                listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF2563EB))
            )
        }
    }
}

@Composable
fun rememberAppIcon(packageName: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        AppIconUtil.getAppIcon(context, packageName)
    }
}
