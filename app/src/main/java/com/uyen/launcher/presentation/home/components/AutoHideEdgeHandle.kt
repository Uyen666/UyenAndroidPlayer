package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.TextPrimary
import kotlinx.coroutines.delay

/**
 * 掌機極簡自動隱藏邊緣側邊小條 (Auto-hide Drawer Handle)
 * 仿造遊戲內快捷小精靈 / Steam Overlay：
 * - 常態：螢幕右側邊緣僅留一條 3.5dp 灰白微光極細線條 (Alpha = 0.25f)，完全不破壞海報視覺美感。
 * - 喚醒：點擊或往內撥動，彈簧滑出微型藥丸膠囊（返回、主頁、多工、設定）。
 * - 自動隱藏：3 秒無操作自動平滑縮回。
 */
@Composable
fun AutoHideEdgeHandle(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onTasks: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var interactionTimerTick by remember { mutableIntStateOf(0) }

    // 3 秒無操作自動隱藏計時器
    LaunchedEffect(isExpanded, interactionTimerTick) {
        if (isExpanded) {
            delay(3000L)
            isExpanded = false
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        if (!isExpanded) {
            // 常態顯示：極細微光線條 (可觸摸寬度 24dp，視覺線條僅 3.5dp)
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(64.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -5f) { // 向內撥動
                                isExpanded = true
                                interactionTimerTick++
                            }
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isExpanded = true
                        interactionTimerTick++
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                // 3.5dp 極細微光指示條
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }
        }

        // 展開狀態：向左滑出的微型藥丸膠囊 (Mini Pill Capsule)
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .background(Color(0xEE0B0E14))
                    .border(
                        width = 1.dp,
                        color = Ps5Blue.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. 返回鍵
                IconButton(
                    onClick = {
                        interactionTimerTick++
                        isExpanded = false
                        onBack()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. 主頁鍵
                IconButton(
                    onClick = {
                        interactionTimerTick++
                        isExpanded = false
                        onHome()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "主頁",
                        tint = Ps5Blue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3. 多工鍵
                IconButton(
                    onClick = {
                        interactionTimerTick++
                        isExpanded = false
                        onTasks()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBox,
                        contentDescription = "多工",
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. 系統設定鍵
                IconButton(
                    onClick = {
                        interactionTimerTick++
                        isExpanded = false
                        onOpenSettings()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "設定",
                        tint = SteamDeckAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
