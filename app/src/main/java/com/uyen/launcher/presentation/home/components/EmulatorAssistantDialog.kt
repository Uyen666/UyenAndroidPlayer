package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCard
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary

/**
 * 掌機模擬器與遊戲引擎導航指南彈窗
 * 當目標模擬器尚未安裝時提供指引、ROM 目錄一鍵建立與內建 8-bit 街機快捷遊玩
 */
@Composable
fun EmulatorAssistantDialog(
    visible: Boolean,
    gameItem: GameItem?,
    onChooseGamesFolder: () -> Unit,
    onPlayBuiltinArcade: () -> Unit,
    onClose: () -> Unit
) {
    var lastGameItem by remember { mutableStateOf(gameItem) }
    if (gameItem != null) {
        lastGameItem = gameItem
    }

    AnimatedVisibility(
        visible = visible && lastGameItem != null,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
    ) {
        val currentItem = lastGameItem ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BackgroundDark)
                    .border(1.5.dp, Ps5Blue.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column {
                    // 頂部標題與關閉按鈕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "掌機核心指引 • ${currentItem.title}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "關閉",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 說明內容
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "💡 此遊戲核心尚未安裝 (${currentItem.packageName ?: "獨立執行檔"})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentGold
                            )
                            Text(
                                text = when (currentItem.category) {
                                    com.uyen.launcher.data.model.GameCategory.GALGAME ->
                                        "若要遊玩 Galgame，請先安裝相容引擎，再選取包含 .xp3 / .rpa 遊戲檔的資料夾。"
                                    com.uyen.launcher.data.model.GameCategory.RETRO ->
                                        "若要遊玩 FC/GBA/SFC 懷舊遊戲，請先安裝相容模擬器，再選取包含 ROM 的資料夾。"
                                    com.uyen.launcher.data.model.GameCategory.STREAMING ->
                                        "若要進行 PC 主機串流，請安裝 Moonlight 或 Steam Link 並與電腦 Sunshine/Steam 配對。"
                                    else ->
                                        "請將自製遊戲 APK 或 Web 資源部署至本機。"
                                },
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 操作按鈕群
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 啟動內建 8-bit 街機
                        Button(
                            onClick = {
                                onClose()
                                onPlayBuiltinArcade()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Ps5Blue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("遊玩內建 8-bit 街機", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 一鍵建立遊戲資料夾
                        Button(
                            onClick = onChooseGamesFolder,
                            colors = ButtonDefaults.buttonColors(containerColor = GlassBackground),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .border(1.dp, SteamDeckAccent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        ) {
                            Text("📁 選擇遊戲資料夾", fontSize = 12.sp, color = AccentGreen)
                        }
                    }
                }
            }
        }
    }
}
