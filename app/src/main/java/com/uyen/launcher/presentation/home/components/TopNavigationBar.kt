package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.data.model.GoogleAccount
import com.uyen.launcher.data.model.MainNavTab
import com.uyen.launcher.data.model.SystemStats
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.GlassBorder
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 頂部導航欄 (LevelUp 風格重構版)
 *
 * 佈局配置：
 * - 左側：[⚙ 小設定按鈕] -> [🎮 Uyen 粗體商標] -> [首頁 | 串流 | 遊戲 分頁切換]
 * - 中間：保持空曠純淨（砍掉原置中玩家資訊與徽章）
 * - 右側：[⟳ 刷新] [🔍 搜尋] [Google 圓形頭像] [Wi-Fi/電量狀態]
 */
@Composable
fun TopNavigationBar(
    selectedTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit,
    stats: SystemStats,
    googleAccount: GoogleAccount,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(10000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        // 最頂部微型狀態提示 (時間 / Wi-Fi / 電量)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentTime,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary.copy(alpha = 0.8f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Wi-Fi",
                    tint = TextSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.size(13.dp)
                )

                Icon(
                    imageVector = if (stats.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    tint = if (stats.batteryPercent < 20) Color(0xFFEF4444) else AccentGreen,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = "${stats.batteryPercent}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary.copy(alpha = 0.85f)
                )
            }
        }

        // 頂部主導航行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側群組：[小設定] -> [Uyen Logo] -> [首頁/串流/遊戲 Tabs]
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 最左邊：小小的設定圖標 (玻璃微粒按鈕)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlassBackground)
                        .border(1.dp, GlassBorder.copy(alpha = 0.4f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "系統設定",
                        tint = TextSecondary.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 2. "Uyen" 粗體字圖標 (幾何掌機 Icon + Uyen 粗體商標)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectTab(MainNavTab.HOME) }
                ) {
                    // 類似 LevelUp 三角立體箭頭/掌機的標識
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Uyen",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Uyen",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(22.dp))

                // 3. 在 Uyen 商標旁放置可切換的分頁標籤：首頁 | 串流 | 遊戲
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    MainNavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else TextMuted,
                            animationSpec = tween(250),
                            label = "tab_color"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelectTab(tab) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = tab.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // LevelUp 同款亮青/黃綠指示線條
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(
                                        if (isSelected) Color(0xFFA3E635) else Color.Transparent
                                    )
                            )
                        }
                    }
                }
            }

            // 中間已砍掉玩家徽章，保留開闊景深空間

            // 右側群組：[⟳ 刷新] -> [🔍 搜尋] -> [Google 帳號頭像]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 重新整理按鈕
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlassBackground)
                        .border(1.dp, GlassBorder.copy(alpha = 0.3f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重新整理遊戲庫",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 搜尋按鈕
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlassBackground)
                        .border(1.dp, GlassBorder.copy(alpha = 0.3f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSearch() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜尋遊戲",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 右上角 Google 帳號頭像 (附帶 Google 四色微光圓環)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF4285F4), // Google Blue
                                    Color(0xFFEA4335), // Google Red
                                    Color(0xFFFBBC05), // Google Yellow
                                    Color(0xFF34A853), // Google Green
                                    Color(0xFF4285F4)
                                )
                            ),
                            shape = CircleShape
                        )
                        .background(Color(0xFF2A2D3E))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onAccountClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = googleAccount.displayName.take(1).uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
