package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.Ps5BlueGlow
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary

/**
 * 收藏庫分類結構 (1:1 還原 SteamOS 頂部標籤與動態數量徽章)
 */
private data class SteamDeckCategory(
    val id: String,
    val title: String,
    val category: GameCategory?
)

/**
 * Steam OS 掌機收藏庫全螢幕彈窗 (附圖 1：1:1 移植 Steam Deck 頂級體驗)
 * 1. 螢幕最底部平滑往上滑動展開動畫 (Slide-Up Spring Damping 0.85)
 * 2. 頂部 L1 / R1 實體按鈕提示與動態計數徽章
 * 3. 2:3 直式長方形海報卡片網格 (Steam Deck Capsule Poster)
 * 4. 底部 SteamOS 主機操控提示列 (A啟動 / B返回 / X篩選 / Y排序)
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
    var isSearchExpanded by remember { mutableStateOf(false) }
    var sortMode by remember { mutableIntStateOf(0) } // 0: 預設, 1: 依遊玩時間, 2: 依名稱

    val tabs = remember {
        listOf(
            SteamDeckCategory("all", "ALL GAMES", GameCategory.ALL),
            SteamDeckCategory("installed", "INSTALLED", null),
            SteamDeckCategory("favorites", "FAVORITES", null),
            SteamDeckCategory("galgame", "GALGAME", GameCategory.GALGAME),
            SteamDeckCategory("retro", "RETRO", GameCategory.RETRO),
            SteamDeckCategory("custom", "SANDBOX", GameCategory.CUSTOM),
            SteamDeckCategory("streaming", "STREAMING", GameCategory.STREAMING),
            SteamDeckCategory("tool", "TOOLS", GameCategory.TOOL)
        )
    }

    val filteredGames = remember(games, selectedTabIndex, searchQuery, sortMode) {
        val currentTab = tabs.getOrNull(selectedTabIndex) ?: tabs.first()
        val list = games.filter { item ->
            val matchesCategory = when (currentTab.id) {
                "all" -> true
                "installed" -> !item.packageName.isNullOrBlank()
                "favorites" -> item.isFavorite
                else -> currentTab.category == null || item.category == currentTab.category
            }
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        when (sortMode) {
            1 -> list.sortedByDescending { it.playTimeHours }
            2 -> list.sortedBy { it.title.lowercase() }
            else -> list
        }
    }

    // 全螢幕底部平滑彈出動畫 (Slide-Up Spring + Fade)
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(240)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E2838), // SteamOS 深藍灰色頂部
                            Color(0xFF141A24), // SteamOS 中央網格主色
                            Color(0xFF0D1117)  // SteamOS 底部暗黑操控條
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 頂部 SteamOS 導航列 (L1 / 標籤組 / R1 / 搜尋 / 關閉)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // L1 肩鍵提示膠囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                if (selectedTabIndex > 0) selectedTabIndex--
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "L1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // SteamOS 分類頁籤滾動列 (附帶各類別動態計數徽章)
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        edgePadding = 0.dp,
                        indicator = {},
                        divider = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTabIndex == index
                            val count = remember(games, tab.id) {
                                games.count { item ->
                                    when (tab.id) {
                                        "all" -> true
                                        "installed" -> !item.packageName.isNullOrBlank()
                                        "favorites" -> item.isFavorite
                                        else -> tab.category == null || item.category == tab.category
                                    }
                                }
                            }

                            Tab(
                                selected = isSelected,
                                onClick = { selectedTabIndex = index },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color(0xFF384358) else Color.Transparent
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF8F98A0)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // R1 肩鍵提示膠囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                if (selectedTabIndex < tabs.size - 1) selectedTabIndex++
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "R1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 搜尋展開或圖示
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜尋遊戲庫...", fontSize = 11.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .width(180.dp)
                                .height(40.dp)
                        )
                    } else {
                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜尋",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 關閉按鈕
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "關閉",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 2. 遊戲海報網格列表 (2:3 直式長方形卡片，1:1 還原附圖 1)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    if (filteredGames.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "此分類下暫無收錄遊戲或符合條件之應用",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 126.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredGames, key = { it.id }) { game ->
                                SteamDeckPosterCard(
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

                // 3. 底部 SteamOS 掌機操控提示欄 (1:1 附圖 1 底欄：A選擇 / B返回 / X篩選 / Y排序)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color(0xF00A0D14))
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左側 STEAM MENU 標章膠囊
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STEAM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MENU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // 右側實體鍵指引組
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ControllerKeyHint("X", "篩選") {
                            isSearchExpanded = !isSearchExpanded
                        }

                        val sortText = when (sortMode) {
                            1 -> "排序: 時長"
                            2 -> "排序: 字母"
                            else -> "排序: 預設"
                        }
                        ControllerKeyHint("Y", sortText) {
                            sortMode = (sortMode + 1) % 3
                        }

                        ControllerKeyHint("A", "啟動") {}

                        ControllerKeyHint("B", "返回") {
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 掌機按鈕提示組件 (如: (A) 選擇、(B) 返回)
 */
@Composable
private fun ControllerKeyHint(
    keyLabel: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = keyLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = actionLabel,
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Steam Deck 2:3 直式長方形海報卡片 (附圖 1：高亮微光白框、滿版海報、右下角 Verified 標籤)
 */
@Composable
private fun SteamDeckPosterCard(
    game: GameItem,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(10.dp)
    val appIcon = rememberAppIcon(game.packageName)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium),
        label = "poster_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .aspectRatio(2f / 3f) // 嚴格 2:3 Steam Deck 垂直海報比例
            .shadow(
                elevation = if (isPressed) 16.dp else 6.dp,
                shape = cardShape,
                ambientColor = Ps5BlueGlow,
                spotColor = Ps5BlueGlow
            )
            .clip(cardShape)
            .background(AppIconUtil.getArtworkGradient(game))
            .border(
                width = if (isPressed) 2.5.dp else 1.dp,
                brush = if (isPressed) {
                    Brush.linearGradient(listOf(Color.White, Color(0xFF00E5FF)))
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                },
                shape = cardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // 1. 底層旋轉浮水印層
        Icon(
            imageVector = when (game.category) {
                GameCategory.GALGAME -> Icons.AutoMirrored.Filled.MenuBook
                GameCategory.RETRO -> Icons.Default.SportsEsports
                GameCategory.STREAMING -> Icons.Default.Tv
                GameCategory.CUSTOM -> Icons.Default.Widgets
                else -> Icons.Default.Gamepad
            },
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.Center)
                .rotate(-15f)
        )

        // 2. 中央視覺圖標區 (居中高解析度展示)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = game.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shadow(8.dp, RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.32f)),
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
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        // 3. 左上角平台/類別小標籤
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = game.category.displayName,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 4. 底部暗黑漸層罩
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // 5. 底部遊戲標題與右下角 Steam Deck Verified 綠色勾勾認證標記 (附圖 1)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (game.playTimeHours > 0) {
                    Text(
                        text = "${String.format("%.0fh", game.playTimeHours)} 遊玩",
                        fontSize = 8.5.sp,
                        color = AccentGold
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Steam Deck 綠色驗證徽章 (✔)
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Deck Verified",
                    tint = Color.Black,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
