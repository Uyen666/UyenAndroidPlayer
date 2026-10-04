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
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.uyen.launcher.presentation.theme.BackgroundDark

/**
 * 掌機旗艦級「雙層環境光暈 + 右側英雄限定舞台 (Hero Stage)」動態背景
 *
 * 核心設計升級：
 * 1. 徹底廢除全螢幕粗暴裁切放大，海報收斂於螢幕中右側 60% 英雄舞台（對應用戶圈選之黃金視覺區）。
 * 2. 1024x500 宣傳圖於中右側以接近 1:1 原生像素密度點對點顯示，徹底杜絕拉伸模糊與顆粒感。
 * 3. 四向無邊界柔和羽化（左側消融於黑底、底部消融於卡片列、頂部保護狀態列、右側微暗角）。
 * 4. 智慧長寬比分流機制：
 *    - 橫向寬幅 (Moonlight, Steam Link)：右側舞台四向羽化展開。
 *    - 正方形圖示 (YouTube, 工具類)：轉化為 PS5 浮雕立體發光徽章，杜絕「刺眼紅牆」困擾。
 *    - 2:3 縱向 (Galgame)：轉化為 Steam Deck 經典立體膠囊封面。
 * 5. 底層全螢幕雙層品牌自適應微光漫射 (Dual-Layer Ambient Mesh Glow)。
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
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_scale"
    )

    val ambientColor = remember(currentGame) {
        AppIconUtil.getAmbientColor(currentGame)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .clipToBounds()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // ══════════════════════════════════════════════════════════════════════
        // 第一層：全螢幕深邃底座與自適應環境色微光漫射 (Dual Ambient Glow)
        // ══════════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ambientColor.copy(alpha = 0.28f),
                            ambientColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(screenWidthPx * 0.72f, screenHeightPx * 0.35f),
                        radius = (screenWidthPx * 0.70f).coerceAtLeast(600f)
                    )
                )
        )

        // ══════════════════════════════════════════════════════════════════════
        // 第二層：海報平滑交叉淡入淡出 (Crossfade 350ms)
        // ══════════════════════════════════════════════════════════════════════
        Crossfade(
            targetState = Pair(currentGame?.id, resolvedBanner),
            animationSpec = tween(350),
            label = "banner_stage_crossfade"
        ) { (_, banner) ->
            if (banner != null) {
                // 依據類別與來源先驗長寬比，載入後自適應更新
                var detectedAspectRatio by remember(banner.imageModel) {
                    mutableFloatStateOf(
                        when {
                            currentGame?.category == GameCategory.GALGAME -> 0.67f
                            currentGame?.category == GameCategory.TOOL && banner.sourceType != BannerSourceType.CUSTOM_USER -> 1.0f
                            else -> 1.8f
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        // ─────────────────────────────────────────────────────────────
                        // 模式 A：橫向寬幅英雄海報 (Landscape Hero Stage - Moonlight / Steam Link 等)
                        // ─────────────────────────────────────────────────────────────
                        detectedAspectRatio >= 1.22f -> {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .fillMaxWidth(0.64f)
                                    .fillMaxHeight(0.72f)
                                    .clipToBounds()
                            ) {
                                // 1. 核心海報（以點對點原生畫質在中右側舞台清晰呈現）
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(banner.imageModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Landscape Hero Artwork",
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

                                // 2. 左側平滑羽化消融遮罩 (最關鍵：消除海報左側生硬邊界，自然過渡到左側純黑文字區)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                0.00f to BackgroundDark,
                                                0.14f to BackgroundDark.copy(alpha = 0.90f),
                                                0.32f to BackgroundDark.copy(alpha = 0.45f),
                                                0.54f to Color.Transparent,
                                                1.00f to Color.Transparent
                                            )
                                        )
                                )

                                // 3. 底部主機融合漸層 (平滑過渡到底部卡片輪播與控制列)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.00f to Color.Transparent,
                                                0.38f to Color.Transparent,
                                                0.65f to BackgroundDark.copy(alpha = 0.45f),
                                                0.86f to BackgroundDark.copy(alpha = 0.90f),
                                                1.00f to BackgroundDark
                                            )
                                        )
                                )

                                // 4. 頂部深色漸層（保護狀態列與頂部導航列）
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                0.00f to BackgroundDark.copy(alpha = 0.85f),
                                                0.35f to BackgroundDark.copy(alpha = 0.40f),
                                                1.00f to Color.Transparent
                                            )
                                        )
                                )

                                // 5. 右側柔和暗角微漸層
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                0.00f to Color.Transparent,
                                                0.86f to Color.Transparent,
                                                0.96f to BackgroundDark.copy(alpha = 0.35f),
                                                1.00f to BackgroundDark.copy(alpha = 0.70f)
                                            )
                                        )
                                )
                            }
                        }

                        // ─────────────────────────────────────────────────────────────
                        // 模式 B：方形 / 應用圖示浮雕徽章 (Square Emblem - YouTube / 工具類，徹底杜絕紅牆)
                        // ─────────────────────────────────────────────────────────────
                        detectedAspectRatio in 0.82f..1.22f -> {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 68.dp, top = 16.dp, bottom = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 背部局部品牌光暈
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    ambientColor.copy(alpha = 0.48f),
                                                    ambientColor.copy(alpha = 0.15f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                // PS5 風格懸浮立體微光徽章
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .scale(breathScale)
                                        .shadow(
                                            elevation = 22.dp,
                                            shape = RoundedCornerShape(28.dp),
                                            spotColor = ambientColor.copy(alpha = 0.65f),
                                            ambientColor = ambientColor.copy(alpha = 0.35f)
                                        )
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(Color(0xFF131824))
                                        .border(
                                            width = 1.2.dp,
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.32f),
                                                    Color.White.copy(alpha = 0.06f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(28.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(banner.imageModel)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "App Hero Emblem",
                                        contentScale = ContentScale.Fit,
                                        onSuccess = { state ->
                                            val w = state.result.drawable.intrinsicWidth
                                            val h = state.result.drawable.intrinsicHeight
                                            if (w > 0 && h > 0) {
                                                detectedAspectRatio = w.toFloat() / h.toFloat()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(112.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                    )
                                }
                            }
                        }

                        // ─────────────────────────────────────────────────────────────
                        // 模式 C：2:3 縱向海報 (Portrait Capsule - Galgame / 懷舊 ROM 經典封面)
                        // ─────────────────────────────────────────────────────────────
                        else -> {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 64.dp, top = 20.dp, bottom = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 背部光環
                                Box(
                                    modifier = Modifier
                                        .size(280.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    ambientColor.copy(alpha = 0.40f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                // Steam Deck 經典 2:3 直式封套
                                Box(
                                    modifier = Modifier
                                        .width(165.dp)
                                        .height(245.dp)
                                        .scale(breathScale)
                                        .shadow(
                                            elevation = 26.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            spotColor = Color.Black.copy(alpha = 0.85f)
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(banner.imageModel)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Galgame Capsule Cover",
                                        contentScale = ContentScale.Crop,
                                        onSuccess = { state ->
                                            val w = state.result.drawable.intrinsicWidth
                                            val h = state.result.drawable.intrinsicHeight
                                            if (w > 0 && h > 0) {
                                                detectedAspectRatio = w.toFloat() / h.toFloat()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 備用方案：微幾何海報牆底層 + 專屬品牌色漫射
                Box(modifier = Modifier.fillMaxSize()) {
                    DefaultPosterWallBackdrop(games = fallbackGames)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ambientColor.copy(alpha = 0.32f),
                                        Color.Transparent
                                    ),
                                    center = Offset(screenWidthPx * 0.72f, screenHeightPx * 0.35f),
                                    radius = (screenWidthPx * 0.70f).coerceAtLeast(600f)
                                )
                            )
                    )
                }
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
