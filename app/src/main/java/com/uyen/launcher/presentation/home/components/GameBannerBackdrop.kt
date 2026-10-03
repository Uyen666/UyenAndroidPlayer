package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uyen.launcher.core.banner.ResolvedBanner
import com.uyen.launcher.core.util.AppIconUtil
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.Ps5Blue

/**
 * PS5 旗艦等級遊戲大海報動態背景 (Hero Poster Backdrop)
 *
 * 1. 支援大圖無縫平滑交叉淡入淡出 (Crossfade 380ms)
 * 2. 緩慢微呼吸縮放物理動效 (1.0f -> 1.025f)，營造真實主機沉浸感
 * 3. 三重光影遮罩：
 *    - 頂部導航保護遮罩 (Top Header Gradient)
 *    - 左側標題防眩光陰影 (Left Title Readability Mask)
 *    - 底部沉浸融合暗區 (Bottom Console Fade)
 * 4. 無海報時自動優雅回退至幾何海報牆與動態光暈
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
        targetValue = 1.022f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .clipToBounds()
    ) {
        Crossfade(
            targetState = Pair(currentGame?.id, resolvedBanner),
            animationSpec = tween(380),
            label = "banner_crossfade"
        ) { (_, banner) ->
            if (banner != null) {
                // 有解析出海報（自訂、精選、TV 橫幅或 Play Store 宣傳圖）
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(banner.imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Game Hero Backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(breathScale)
                    )

                    // 1. 頂部深色漸層（保護狀態列與頂部導航列）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        BackgroundDark.copy(alpha = 0.85f),
                                        BackgroundDark.copy(alpha = 0.40f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // 2. 左側深色陰影遮罩（保障遊戲標題、分類標籤、副標題永遠清晰易讀）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        BackgroundDark.copy(alpha = 0.85f),
                                        BackgroundDark.copy(alpha = 0.55f),
                                        BackgroundDark.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    endX = 900f
                                )
                            )
                    )

                    // 3. 底部主機融合漸層（平滑過渡到底部卡片輪播與控制列）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        BackgroundDark.copy(alpha = 0.45f),
                                        BackgroundDark.copy(alpha = 0.88f),
                                        BackgroundDark
                                    )
                                )
                            )
                    )

                    // 4. 環形沉浸暗角 (Radial Vignette)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        BackgroundDark.copy(alpha = 0.60f)
                                    ),
                                    radius = 1200f
                                )
                            )
                    )
                }
            } else {
                // 回退備用方案：動態主題色光暈 + 傾斜海報拼貼底層
                Box(modifier = Modifier.fillMaxSize()) {
                    DefaultPosterWallBackdrop(games = fallbackGames)

                    val cat = currentGame?.category
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        when (cat) {
                                            GameCategory.GALGAME -> Color(0x38A855F7)
                                            GameCategory.RETRO -> Color(0x38F97316)
                                            GameCategory.STREAMING -> Color(0x380284C7)
                                            GameCategory.CUSTOM -> Color(0x3822C55E)
                                            else -> Ps5Blue.copy(alpha = 0.28f)
                                        },
                                        Color.Transparent
                                    ),
                                    radius = 1100f
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
