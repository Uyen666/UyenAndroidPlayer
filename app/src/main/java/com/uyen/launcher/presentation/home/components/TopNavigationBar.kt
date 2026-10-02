package com.uyen.launcher.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.data.model.PlayerProfile
import com.uyen.launcher.data.model.SystemStats
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.GlassBorder
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopNavigationBar(
    profile: PlayerProfile,
    stats: SystemStats,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onToggleControllerMode: () -> Unit,
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左上角：Quick Settings & 效能監控 HUD 觸發鈕
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .clickable { onOpenSettings() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "效能監控",
                tint = SteamDeckAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${stats.fps} FPS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreen
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${stats.batteryTempCelsius}°C",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // 正上方：玩家身分面板與即時狀態
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // 玩家頭像圓圈
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Ps5Blue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.username.take(1).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            // 玩家名稱與徽章
            Text(
                text = profile.username,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentGold.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = profile.badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 狀態列圖示：Wi-Fi、電池、時間
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Wi-Fi",
                tint = TextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (stats.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                contentDescription = "Battery",
                tint = if (stats.batteryPercent < 20) Color(0xFFEF4444) else AccentGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${stats.batteryPercent}%",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = currentTime,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // 右上角：手柄模式切換 & Steam OS 遊戲庫
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 虛擬手柄觸發鈕
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBackground)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .clickable { onToggleControllerMode() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = "虛擬手柄",
                    tint = AccentGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "手柄模式",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            // 遊戲庫觸發鈕 (Steam OS Library)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Ps5Blue)
                    .clickable { onOpenLibrary() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "遊戲庫",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "遊戲庫",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
