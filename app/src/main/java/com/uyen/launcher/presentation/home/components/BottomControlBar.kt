package com.uyen.launcher.presentation.home.components

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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.GlassBorder

/**
 * 底部控制欄 (LevelUp 風格底欄：左選單膠囊、正中功能膠囊、右多工圓鈕)
 */
@Composable
fun BottomControlBar(
    onOpenMenu: () -> Unit,
    onQuickAction: () -> Unit,
    onTaskSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左下角：[ ⠶ 菜單 ] 藥丸膠囊按鈕 (開啟全部應用程式與遊戲庫)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onOpenMenu() }
                .padding(horizontal = 14.dp)
        ) {
            // LevelUp 同款亮綠九宮格圖示
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "菜單",
                tint = Color(0xFFA3E635),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "菜單",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 正下方：[ ≡ ] 居中功能藥丸膠囊 (頂部附帶狀態小指示點，點擊展開控制面板)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onQuickAction() }
        ) {
            // 頂部小小白點指示燈
            Box(
                modifier = Modifier
                    .size(3.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f))
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassBackground)
                    .border(1.dp, GlassBorder.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "快捷功能",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // 右下角：( ⧉ ) 懸浮圓鈕 (掌機真實多工任務切換器)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GlassBackground)
                .border(1.dp, GlassBorder, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onTaskSwitcher() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlipToFront,
                contentDescription = "多工切換",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
