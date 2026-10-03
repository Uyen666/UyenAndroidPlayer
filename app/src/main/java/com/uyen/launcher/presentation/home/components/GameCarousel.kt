package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import coil.compose.AsyncImage
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
import androidx.compose.material.icons.filled.Add
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
import com.uyen.launcher.presentation.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCarousel(
    games: List<GameItem>,
    selectedIndex: Int,
    onSelectGame: (Int) -> Unit,
    onLaunchGame: (GameItem) -> Unit,
    showAddCard: Boolean = true,
    onAddCardClick: () -> Unit = {},
    onRemoveGame: ((GameItem) -> Unit)? = null,
    onCardLongClick: ((GameItem) -> Unit)? = null,
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
                val cardWidthPx = with(density) { 216.dp.toPx() }
                val centerOffsetPx = if (viewportWidth > 0) {
                    -((viewportWidth - cardWidthPx) / 2).toInt()
                } else {
                    -240
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
        contentPadding = PaddingValues(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
    ) {
        itemsIndexed(games) { index, game ->
            val isSelected = index == selectedIndex

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.07f else 0.94f,
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
                },
                onLongClick = {
                    if (onCardLongClick != null) {
                        onCardLongClick(game)
                    } else if (onRemoveGame != null) {
                        onRemoveGame(game)
                    }
                }
            )
        }

        // 末尾快捷添加卡片 (附圖 2 風格：引導玩家從收藏庫選取並添加卡片到首頁)
        if (showAddCard) {
            item {
                AddShortcutCard(onClick = onAddCardClick)
            }
        }
    }
}

/**
 * 依據遊戲/應用動態提取專屬 LevelUp 主題色彩 (附圖 2 風格)
 */
private fun getLevelUpCardBrush(game: GameItem): Brush {
    val titleLower = game.title.lowercase()
    val idLower = game.id.lowercase()
    val pkgLower = (game.packageName ?: "").lowercase()

    return when {
        titleLower.contains("citra") || idLower.contains("citra") -> Brush.horizontalGradient(
            listOf(Color(0xFFD97706), Color(0xFFF59E0B))
        )
        titleLower.contains("duckstation") || idLower.contains("duckstation") -> Brush.horizontalGradient(
            listOf(Color(0xFF0369A1), Color(0xFF0284C7))
        )
        titleLower.contains("drastic") || idLower.contains("drastic") -> Brush.horizontalGradient(
            listOf(Color(0xFF0E7490), Color(0xFF06B6D4))
        )
        titleLower.contains("retroarch") || idLower.contains("retroarch") -> Brush.horizontalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF334155))
        )
        titleLower.contains("chrome") || idLower.contains("chrome") -> Brush.horizontalGradient(
            listOf(Color(0xFF1E293B), Color(0xFF475569))
        )
        titleLower.contains("play") || pkgLower.contains("vending") -> Brush.horizontalGradient(
            listOf(Color(0xFF1E293B), Color(0xFF334155))
        )
        game.category == GameCategory.GALGAME -> Brush.horizontalGradient(
            listOf(Color(0xFF581C87), Color(0xFF8B5CF6))
        )
        game.category == GameCategory.RETRO -> Brush.horizontalGradient(
            listOf(Color(0xFF7C2D12), Color(0xFFEA580C))
        )
        game.category == GameCategory.STREAMING -> Brush.horizontalGradient(
            listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
        )
        game.category == GameCategory.CUSTOM -> Brush.horizontalGradient(
            listOf(Color(0xFF065F46), Color(0xFF0D9488))
        )
        else -> AppIconUtil.getArtworkGradient(game)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCard(
    game: GameItem,
    isSelected: Boolean,
    scale: Float,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(16.dp)
    val appIcon = rememberAppIcon(game.packageName)
    val backgroundBrush = remember(game) { getLevelUpCardBrush(game) }

    Box(
        modifier = Modifier
            .scale(scale)
            .width(216.dp)
            .height(74.dp)
            .shadow(
                elevation = if (isSelected) 18.dp else 4.dp,
                shape = cardShape,
                ambientColor = if (isSelected) Ps5BlueGlow else Color.Black,
                spotColor = if (isSelected) Ps5BlueGlow else Color.Black
            )
            .clip(cardShape)
            .background(backgroundBrush)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.linearGradient(listOf(Color.White, Color(0xFF00E5FF)))
                } else {
                    Brush.linearGradient(
                        listOf(
                            SurfaceCardBorder.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                },
                shape = cardShape
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // 微光磨砂半透明遮罩層
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f)
                        )
                    )
                )
        )

        // 水平分佈佈局：左側文字標題與分類標籤，右側高解析度長方形/圓角圖示 (附圖 2 LevelUp 風格)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左側：標題文字與分類膠囊區
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = game.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = game.category.displayName,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF00E5FF) else TextSecondary
                        )
                    }

                    if (game.playTimeHours > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${String.format("%.0fh", game.playTimeHours)}",
                            fontSize = 8.5.sp,
                            color = AccentGold.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 右側：長方形 / 大圓角圖標區 (附圖 2 圖示展示區)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                val cardArtwork = game.coverUrl ?: game.bannerUrl
                if (!cardArtwork.isNullOrBlank()) {
                    AsyncImage(
                        model = cardArtwork,
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = game.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        imageVector = when (game.category) {
                            GameCategory.GALGAME -> Icons.AutoMirrored.Filled.MenuBook
                            GameCategory.RETRO -> Icons.Default.SportsEsports
                            GameCategory.STREAMING -> Icons.Default.Tv
                            GameCategory.CUSTOM -> Icons.Default.Widgets
                            else -> Icons.Default.Gamepad
                        },
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF00E5FF) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // 選中時頂部極致微光高亮線 (PS5 質感細節)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF00E5FF),
                                Color.White,
                                Color(0xFF00E5FF),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // 非選中焦點時的輕微灰階/暗化層，確保選中卡片對比強烈
        if (!isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }
    }
}

/**
 * 末尾快捷「+ 添加卡片」膠囊 (附圖 2 風格：引導玩家從收藏庫挑選遊戲釘選到首頁)
 */
@Composable
private fun AddShortcutCard(
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(74.dp)
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = cardShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加卡片",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "添加卡片",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}
