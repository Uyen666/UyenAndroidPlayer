package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.uyen.launcher.data.model.SystemStats
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.GlassBorder
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCard
import com.uyen.launcher.presentation.theme.SurfaceCardBorder
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary

@Composable
fun QuickSettingsDrawer(
    visible: Boolean,
    stats: SystemStats,
    isGamepadOverlayActive: Boolean,
    isKioskModeActive: Boolean,
    onToggleGamepadOverlay: (Boolean) -> Unit,
    onToggleKioskMode: (Boolean) -> Unit,
    onOpenControllerMode: () -> Unit,
    onOpenTaskSwitcher: () -> Unit,
    onCleanRam: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it }),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .background(GlassBackground)
                        .border(1.dp, GlassBorder, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // 攔截抽屜內點擊
                        )
                        .padding(24.dp)
                ) {
                    // 頂部標題與關閉按鈕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "效能監控與設定",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "關閉",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 效能 HUD 網格
                    Text(
                        text = "即時硬體狀態 (Helio G85)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SteamDeckAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HudCard(
                            label = "FPS 影格",
                            value = "${stats.fps}",
                            sub = "90Hz 模式",
                            accentColor = AccentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "電池溫度",
                            value = "${stats.batteryTempCelsius}°C",
                            sub = if (stats.isCharging) "充電中" else "放電中",
                            accentColor = if (stats.batteryTempCelsius > 40f) Color.Red else AccentGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HudCard(
                            label = "RAM 記憶體",
                            value = "${stats.ramUsedMb} MB",
                            sub = "/ ${stats.ramTotalMb} MB",
                            accentColor = SteamDeckAccent,
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "電量",
                            value = "${stats.batteryPercent}%",
                            sub = "5000 mAh",
                            accentColor = if (stats.batteryPercent < 20) Color.Red else AccentGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 快速開關
                    Text(
                        text = "掌機功能切換",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SteamDeckAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 觸控虛擬手柄懸浮層開關
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "觸控輔助手柄層",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "無實體手柄時於螢幕覆蓋虛擬按鍵",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isGamepadOverlayActive,
                            onCheckedChange = onToggleGamepadOverlay,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Ps5Blue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 掌機純淨鎖定 (Kiosk 模式)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔒 掌機沉浸鎖定 (Kiosk)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "停用系統下拉通知欄與邊緣返回手勢",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isKioskModeActive,
                            onCheckedChange = onToggleKioskMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentGold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 掌機多工管理與一鍵清理按鈕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onClose()
                                onOpenTaskSwitcher()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceCard,
                                contentColor = SteamDeckAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .border(1.dp, SteamDeckAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Text(text = "📑 多工管理", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onCleanRam,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen.copy(alpha = 0.2f),
                                contentColor = AccentGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Text(text = "⚡ 釋放記憶體", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 進入 PC 手柄模式按鈕
                    Button(
                        onClick = {
                            onClose()
                            onOpenControllerMode()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold.copy(alpha = 0.2f),
                            contentColor = AccentGold
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "🎮 進入 UyenController (PC手柄模式)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudCard(
    label: String,
    value: String,
    sub: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(text = sub, fontSize = 9.sp, color = TextSecondary)
    }
}
