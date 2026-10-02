package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.data.model.RunningTask
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCard
import com.uyen.launcher.presentation.theme.SurfaceCardBorder
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary

/**
 * 掌機多工任務管理視窗 (Handheld Task Switcher)
 * 真實管理當前啟動的後台遊戲與應用程序
 */
@Composable
fun HandheldTaskSwitcherDialog(
    visible: Boolean,
    runningTasks: List<RunningTask>,
    onSwitchToTask: (RunningTask) -> Unit,
    onKillTask: (RunningTask) -> Unit,
    onCleanAll: () -> Unit,
    onClose: () -> Unit
) {
    if (!visible) return

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark.copy(alpha = 0.95f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBackground)
                    .border(1.5.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 攔截內部點擊
                    )
                    .padding(24.dp)
            ) {
                // 頂部欄：標題與一鍵釋放
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📑 掌機多工任務管理",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = if (runningTasks.isEmpty()) "後台已清空" else "目前有 ${runningTasks.size} 個遊戲後台運行中",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (runningTasks.isNotEmpty()) {
                            Button(
                                onClick = onCleanAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen.copy(alpha = 0.2f),
                                    contentColor = AccentGreen
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "一鍵結束全部", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "關閉",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (runningTasks.isEmpty()) {
                    // 空狀態：記憶體已清空
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "後台已無任何運行程序",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "系統 RAM 與 GPU 處於最高電競可用狀態",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // 真實運行任務列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(runningTasks, key = { it.id }) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Ps5Blue.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gamepad,
                                            contentDescription = null,
                                            tint = SteamDeckAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = task.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${task.packageName} • 記憶體: ~${task.memoryUsageMb} MB",
                                            fontSize = 11.sp,
                                            color = AccentGold
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onClose()
                                            onSwitchToTask(task)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Ps5Blue),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("切換", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { onKillTask(task) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "結束程序",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
