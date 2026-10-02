package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.core.util.AppIconUtil
import com.uyen.launcher.core.util.rememberAppIcon
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.Ps5BlueGlow
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCardBorder
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary

@Composable
fun GameCarousel(
    games: List<GameItem>,
    selectedIndex: Int,
    onSelectGame: (Int) -> Unit,
    onLaunchGame: (GameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // 掌機等級溫潤平滑緩動：居中聚焦 + 520ms 旗艦主機阻尼物理動畫 (Silky Smooth Console Scroll Physics)
    LaunchedEffect(selectedIndex) {
        if (games.isNotEmpty() && selectedIndex in games.indices) {
            val layoutInfo = listState.layoutInfo
            val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == selectedIndex }
            if (visibleItem != null) {
                val viewportCenter = layoutInfo.viewportSize.width / 2
                val itemCenter = visibleItem.offset + visibleItem.size / 2
                val delta = (itemCenter - viewportCenter).toFloat()
                if (kotlin.math.abs(delta) > 3f) {
                    listState.animateScrollBy(
                        value = delta,
                        animationSpec = tween(
                            durationMillis = 520,
                            easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
                        )
                    )
                }
            } else {
                val viewportWidth = layoutInfo.viewportSize.width
                val cardWidthPx = with(density) { 188.dp.toPx() }
                val centerOffsetPx = if (viewportWidth > 0) {
                    -((viewportWidth - cardWidthPx) / 2).toInt()
                } else {
                    -220
                }
                listState.animateScrollToItem(
                    index = selectedIndex,
                    scrollOffset = centerOffsetPx
                )
            }
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(142.dp)
    ) {
        itemsIndexed(games) { index, game ->
            val isSelected = index == selectedIndex

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 0.94f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessLow
                ),
                label = "card_scale"
            )

            GameCard(
                game = game,
                isSelected = isSelected,
                scale = scale,
                onClick = {
                    if (isSelected) {
                        onLaunchGame(game)
                    } else {
                        onSelectGame(index)
                    }
                }
            )
        }
    }
}

/**
 * 掌機經典 16:9 低姿態橫向膠囊卡片 (矮長方形 + 滿版整張遊戲視覺)
 * 解決圖標與文字重疊問題，呈現無懈可擊的商業級掌機美學
 */
@Composable
private fun GameCard(
    game: GameItem,
    isSelected: Boolean,
    scale: Float,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(14.dp)
    val appIcon = rememberAppIcon(game.packageName)

    Box(
        modifier = Modifier
            .scale(scale)
            .width(188.dp)
            .height(106.dp)
            .shadow(
                elevation = if (isSelected) 16.dp else 4.dp,
                shape = cardShape,
                ambientColor = if (isSelected) Ps5BlueGlow else Color.Black,
                spotColor = if (isSelected) Ps5BlueGlow else Color.Black
            )
            .clip(cardShape)
            .background(AppIconUtil.getArtworkGradient(game))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.linearGradient(listOf(Ps5BlueGlow, SteamDeckAccent))
                } else {
                    Brush.linearGradient(
                        listOf(
                            SurfaceCardBorder.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                },
                shape = cardShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // 1. 底層滿版旋轉浮水印層 (全卡片滿版紋理)
        Icon(
            imageVector = when (game.category) {
                GameCategory.GALGAME -> Icons.AutoMirrored.Filled.MenuBook
                GameCategory.RETRO -> Icons.Default.SportsEsports
                GameCategory.STREAMING -> Icons.Default.Tv
                GameCategory.CUSTOM -> Icons.Default.Widgets
                else -> Icons.Default.Gamepad
            },
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(86.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 16.dp, y = (-8).dp)
                .rotate(-15f)
        )

        // 2. 上部圖標展示區 (靠上置中，避免與底部文字重疊)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            if (appIcon != null) {
                // 已安裝應用的真實高解析圖標
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shadow(6.dp, RoundedCornerShape(12.dp))
                )
            } else {
                // 內置核心 / 模擬器標誌
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (game.category) {
                            GameCategory.GALGAME -> Icons.AutoMirrored.Filled.MenuBook
                            GameCategory.RETRO -> Icons.Default.SportsEsports
                            GameCategory.STREAMING -> Icons.Default.Tv
                            GameCategory.CUSTOM -> Icons.Default.Widgets
                            else -> Icons.Default.Gamepad
                        },
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 3. 底部暗角漸層保護罩 (Dark Vignette Scrim) - 高度自 45% 向下覆蓋，保護標題對比度
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.92f)
                        ),
                        startY = 50f
                    )
                )
        )

        // 4. 右上角「選中就緒」提示微膠囊 (Focused Indicator Pill)
        if (isSelected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 7.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Ps5Blue.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "PLAY",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // 5. 底部資訊文字層 (標題與類別標籤，乾淨置底零重疊)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                text = game.title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = game.category.displayName,
                    fontSize = 9.sp,
                    color = if (isSelected) SteamDeckAccent else TextMuted,
                    fontWeight = FontWeight.SemiBold
                )

                if (game.playTimeHours > 0) {
                    Text(
                        text = "• ${String.format("%.0fh", game.playTimeHours)}",
                        fontSize = 8.5.sp,
                        color = AccentGold.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 6. 非選中時的微暗層 (讓焦點卡片躍然而出)
        if (!isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.16f))
            )
        }
    }
}
