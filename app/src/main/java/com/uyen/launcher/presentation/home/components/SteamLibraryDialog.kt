package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.core.util.AppIconUtil
import com.uyen.launcher.core.util.rememberAppIcon
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.Ps5BlueGlow
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCard
import com.uyen.launcher.presentation.theme.SurfaceCardBorder
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary

/**
 * Steam OS 掌機應用庫全螢幕彈窗
 * 具備頂級遊戲主機級別的彈性微回彈展開 (Spring Scale-in) 與淡出動畫
 */
@Composable
fun SteamLibraryDialog(
    visible: Boolean,
    games: List<GameItem>,
    onLaunchGame: (GameItem) -> Unit,
    onClose: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf(
        GameCategory.ALL,
        GameCategory.GALGAME,
        GameCategory.RETRO,
        GameCategory.CUSTOM,
        GameCategory.STREAMING,
        GameCategory.TOOL
    )

    val filteredGames = remember(games, selectedTabIndex, searchQuery) {
        val cat = categories[selectedTabIndex]
        games.filter { item ->
            val matchesCategory = cat == GameCategory.ALL || item.category == cat
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    // 外層黑色景深遮罩層動畫
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(280, easing = LinearOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark.copy(alpha = 0.94f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            // 內層彈窗主體：微彈簧縮放與平滑位移動畫 (Console Grade Scale-in + Slide)
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(240)),
                exit = scaleOut(
                    targetScale = 0.94f,
                    animationSpec = tween(180, easing = FastOutLinearInEasing)
                ) + slideOutVertically(
                    targetOffsetY = { 40 },
                    animationSpec = tween(180)
                ) + fadeOut(animationSpec = tween(180)),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(Ps5BlueGlow.copy(alpha = 0.4f), Color.White.copy(alpha = 0.08f))),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // 攔截點擊，防止點擊內容關閉
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 頂部導航與搜尋列
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(SteamDeckAccent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "STEAM OS 應用庫",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "共收錄 ${filteredGames.size} 款遊戲與掌機模組 • 點選即刻啟動",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("搜尋遊戲或模組...", fontSize = 12.sp, color = TextMuted) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Ps5Blue,
                                        unfocusedBorderColor = SurfaceCardBorder,
                                        focusedContainerColor = SurfaceCard,
                                        unfocusedContainerColor = SurfaceCard,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(240.dp)
                                        .height(44.dp)
                                )

                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "關閉",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 分類頁籤
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = TextPrimary,
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = Ps5Blue,
                                    height = 3.dp
                                )
                            },
                            divider = {}
                        ) {
                            categories.forEachIndexed { index, cat ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Text(
                                            text = cat.displayName,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTabIndex == index) TextPrimary else TextMuted
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 遊戲網格清單 (16:9 低姿態橫向卡片網格)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 164.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredGames, key = { it.id }) { game ->
                                LibraryGameCard(
                                    game = game,
                                    onClick = {
                                        onClose()
                                        onLaunchGame(game)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 應用庫橫向膠囊卡片 (低姿態矮長方形 + 滿版整張遊戲圖示)
 */
@Composable
private fun LibraryGameCard(
    game: GameItem,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    val appIcon = rememberAppIcon(game.packageName)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "lib_card_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .height(96.dp)
            .shadow(6.dp, cardShape)
            .clip(cardShape)
            .background(AppIconUtil.getArtworkGradient(game))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                cardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // 1. 底層旋轉浮水印
        Icon(
            imageVector = when (game.category) {
                GameCategory.GALGAME -> Icons.AutoMirrored.Filled.MenuBook
                GameCategory.RETRO -> Icons.Default.SportsEsports
                GameCategory.STREAMING -> Icons.Default.Tv
                GameCategory.CUSTOM -> Icons.Default.Widgets
                else -> Icons.Default.Gamepad
            },
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .size(76.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 14.dp, y = (-4).dp)
                .rotate(-15f)
        )

        // 2. 上部圖示區 (置頂居中，杜絕與底部文字重疊)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 3. 底部暗角漸層保護罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.90f)
                        ),
                        startY = 44f
                    )
                )
        )

        // 底部文字與標籤
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = game.title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = game.category.displayName,
                fontSize = 9.sp,
                color = SteamDeckAccent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
