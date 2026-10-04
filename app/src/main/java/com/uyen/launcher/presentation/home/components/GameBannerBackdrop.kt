package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uyen.launcher.core.banner.BannerSourceType
import com.uyen.launcher.core.banner.ResolvedBanner
import com.uyen.launcher.core.util.AppIconUtil
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem

/**
 * 掌機旗艦級全螢幕海報與雙向漸層遮罩系統 (PS5 Scrim Backdrop Architecture)
 *
 * 核心設計遵循 PS5 / Steam Deck 旗艦掌機標準：
 * 1. 消除硬切邊：底圖鋪滿 100% 全螢幕（fillMaxSize, ContentScale.Crop），不切割左右版面。
 * 2. 雙向線性漸層遮罩（Dual Gradient Scrim）：
 *    - 左側漸層遮罩：從左側實黑 (0xFF070B10) 到右側透明，保護標題與資訊文字。
 *    - 底部漸層遮罩：從透明到底部實黑，承托下方輪播卡片。
 *    - 頂部漸層遮罩：保護系統狀態列與頂部導航分頁。
 * 3. 智慧長寬比分流：
 *    - 寬幅海報 (Landscape)：全螢幕滿版展示，左側漸層羽化融入底色。
 *    - 方形應用圖示 (Square Icon)：底層轉為大半徑氛圍光暈 (Atmospheric Glow)，右側呈現 136dp 3D 浮動徽章，杜絕粗暴拉伸。
 *    - 2:3 直式海報 (Portrait)：全螢幕大半徑漫射，右側浮動 2:3 經典膠囊封面。
 * 4. 景深暗角 (Vignette)：微弱壓暗四周邊緣，提升劇院主機沉浸感。
 */
@Composable
fun GameBannerBackdrop(
    currentGame: GameItem?,
    resolvedBanner: ResolvedBanner?,
    fallbackGames: List<GameItem>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_breath_transition")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_scale"
    )

    val ambientColor = remember(currentGame) {
        AppIconUtil.getAmbientColor(currentGame)
    }

    // 掌機沉浸式黑底 (對應 PS5 Scrim 標準 #070B10)
    val scrimColor = Color(0xFF070B10)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(scrimColor)
            .clipToBounds()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // ══════════════════════════════════════════════════════════════════════
        // 第一層：底層全螢幕大半徑品牌動態氛圍微光漫射 (Atmospheric Glow)
        // ══════════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ambientColor.copy(alpha = 0.28f),
                            ambientColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(screenWidthPx * 0.72f, screenHeightPx * 0.38f),
                        radius = (screenWidthPx * 0.75f).coerceAtLeast(700f)
                    )
                )
        )

        // ══════════════════════════════════════════════════════════════════════
        // 第二層：海報平滑交叉淡入淡出 (Crossfade 350ms)
        // ══════════════════════════════════════════════════════════════════════
        Crossfade(
            targetState = Pair(currentGame?.id, resolvedBanner),
            animationSpec = tween(350),
            label = "banner_crossfade"
        ) { (_, banner) ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (banner != null) {
                    var detectedAspectRatio by remember(banner.imageModel) {
                        mutableFloatStateOf(
                            when {
                                currentGame?.category == GameCategory.GALGAME -> 0.67f
                                currentGame?.category == GameCategory.TOOL && banner.sourceType != BannerSourceType.CUSTOM_USER -> 1.0f
                                else -> 1.78f
                            }
                        )
                    }

                    when {
                        // ─────────────────────────────────────────────────────────────
                        // 模式 A：寬幅橫向海報 (Landscape Hero Wallpaper - 100% 全螢幕鋪滿，無邊界裁切)
                        // ─────────────────────────────────────────────────────────────
                        detectedAspectRatio >= 1.22f -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(banner.imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                onSuccess = { state ->
                                    val w = state.result.drawable.intrinsicWidth
                                    val h = state.result.drawable.intrinsicHeight
                                    if (w > 0 && h > 0) {
                                        detectedAspectRatio = w.toFloat() / h.toFloat()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(breathScale)
                            )
                        }

                        // ─────────────────────────────────────────────────────────────
                        // 模式 B：方形 / 應用圖示 (Square App Icon - YouTube / 工具類)
                        // 杜絕全螢幕馬賽克拉伸，底層為超大半徑氛圍光暈，右側呈現精緻 136dp 3D 浮動徽章
                        // ─────────────────────────────────────────────────────────────
                        detectedAspectRatio in 0.82f..1.22f -> {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 84.dp, bottom = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 徽章背部專屬微光
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    ambientColor.copy(alpha = 0.50f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .size(136.dp)
                                        .scale(breathScale)
                                        .shadow(
                                            elevation = 28.dp,
                                            shape = RoundedCornerShape(26.dp),
                                            spotColor = ambientColor.copy(alpha = 0.70f),
                                            ambientColor = Color.Black
                                        )
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(Color(0xFF141A26))
                                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(26.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(banner.imageModel)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        onSuccess = { state ->
                                            val w = state.result.drawable.intrinsicWidth
                                            val h = state.result.drawable.intrinsicHeight
                                            if (w > 0 && h > 0) {
                                                detectedAspectRatio = w.toFloat() / h.toFloat()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(104.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                    )
                                }
                            }
                        }

                        // ─────────────────────────────────────────────────────────────
                        // 模式 C：2:3 縱向海報 (Portrait Capsule - Galgame 經典直式封面)
                        // 底圖鋪滿全螢幕 + 右側 2:3 Steam Deck 獨立立體膠囊
                        // ─────────────────────────────────────────────────────────────
                        else -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(banner.imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                onSuccess = { state ->
                                    val w = state.result.drawable.intrinsicWidth
                                    val h = state.result.drawable.intrinsicHeight
                                    if (w > 0 && h > 0) {
                                        detectedAspectRatio = w.toFloat() / h.toFloat()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(breathScale)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 76.dp, bottom = 48.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(240.dp)
                                        .scale(breathScale)
                                        .shadow(
                                            elevation = 30.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            spotColor = Color.Black.copy(alpha = 0.88f)
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                                ) {
                                    AsyncImage(
                                        model = banner.imageModel,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 備用方案：微幾何海報牆底層 + 全螢幕品牌色氛圍光漫射
                    DefaultPosterWallBackdrop(games = fallbackGames)
                }

                // ══════════════════════════════════════════════════════════════════
                // PS5 旗艦雙向漸層遮罩系統 (消除任何生硬切邊，保證前景文字與下方卡片可讀性)
                // ══════════════════════════════════════════════════════════════════

                // 1. 左側漸層遮罩：保護左側標題與文字（從左側實黑到右側透明羽化溶解）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.00f to scrimColor,                           // 最左側完全不透明，承托標題文字
                                0.35f to scrimColor.copy(alpha = 0.85f),
                                0.70f to Color.Transparent                     // 向右自然羽化溶解到背景圖中
                            )
                        )
                )

                // 2. 底部漸層遮罩：保護下方選中卡片（從底部實黑到上方透明）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.45f to Color.Transparent,
                                0.70f to scrimColor.copy(alpha = 0.55f),
                                1.00f to scrimColor                            // 底部托底，避免卡片與背景雜色衝突
                            )
                        )
                )

                // 3. 頂部漸層遮罩：保護狀態列與頂部導航列
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(
                            Brush.verticalGradient(
                                0.00f to scrimColor.copy(alpha = 0.85f),
                                0.50f to scrimColor.copy(alpha = 0.40f),
                                1.00f to Color.Transparent
                            )
                        )
                )

                // 4. 景深暗角 (Vignette)：微弱壓暗四周邊緣，提升劇院主機沉浸感
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    scrimColor.copy(alpha = 0.35f)
                                ),
                                center = Offset(screenWidthPx * 0.5f, screenHeightPx * 0.5f),
                                radius = screenWidthPx * 0.82f
                            )
                        )
                )
            }
        }
    }
}

/**
 * 備用幾何海報牆底層
 */
@Composable
private fun DefaultPosterWallBackdrop(
    games: List<GameItem>,
    modifier: Modifier = Modifier
) {
    val sampleGames = remember(games) { games.take(16) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .rotate(-5f)
                .scale(1.15f)
                .alpha(0.08f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sampleGames.shuffled(java.util.Random(row * 42L)).forEach { game ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppIconUtil.getArtworkGradient(game))
                        )
                    }
                }
            }
        }

        // 滿版深色徑向暗角
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCC090C15),
                            Color(0xF8090C15)
                        ),
                        radius = 1100f
                    )
                )
        )
    }
}
