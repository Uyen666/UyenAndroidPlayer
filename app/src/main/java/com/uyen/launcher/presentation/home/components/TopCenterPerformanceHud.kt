package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.data.model.SystemStats
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary

/**
 * 螢幕中央頂部微型效能 HUD 膠囊列 (Top Center Performance HUD Bar)
 * 以精巧極簡的膠囊排版呈現 FPS、電池溫度、RAM 與電量，不遮擋主要遊戲海報。
 */
@Composable
fun TopCenterPerformanceHud(
    visible: Boolean,
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xE610131F))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. FPS 刷新率
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Text(
                    text = "${stats.fps} FPS",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(9.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // 2. 電池溫度
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "🌡",
                    fontSize = 9.sp
                )
                Text(
                    text = "${"%.1f".format(stats.batteryTempCelsius)}°C",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (stats.batteryTempCelsius >= 40f) Color(0xFFEF4444) else AccentGold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(9.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // 3. RAM 記憶體
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "RAM",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                val ramUsedGb = stats.ramUsedMb / 1024f
                val ramTotalGb = stats.ramTotalMb / 1024f
                val ramText = if (stats.ramTotalMb > 0) {
                    "${"%.1f".format(ramUsedGb)}/${"%.1f".format(ramTotalGb)}G"
                } else {
                    "${stats.ramUsedMb}M"
                }
                Text(
                    text = ramText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ps5Blue,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (stats.batteryPercent > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(9.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                // 4. 電量與充電
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (stats.isCharging) "⚡" else "🔋",
                        fontSize = 9.sp
                    )
                    Text(
                        text = "${stats.batteryPercent}%",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (stats.batteryPercent < 20) Color(0xFFEF4444) else TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
