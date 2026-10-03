package com.uyen.launcher.presentation.home.components

import android.app.Activity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.GlassBorder
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCard
import com.uyen.launcher.presentation.theme.SurfaceCardBorder
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary

/**
 * 掌機系統控制台 (System Quick Drawer)
 * 整合：
 * 1. 音量與視窗亮度調節滑桿
 * 2. Wi-Fi / 藍牙快速跳轉卡片
 * 3. 掌機專注模式 (通知靜音)
 * 4. 左上角效能 HUD 開關 (關閉後主畫面 100% 純淨)
 * 5. Kiosk 鎖定、虛擬手柄層、即時硬體狀態
 */
@Composable
fun QuickSettingsDrawer(
    visible: Boolean,
    stats: SystemStats,
    isGamepadOverlayActive: Boolean,
    isKioskModeActive: Boolean,
    showPerformanceHud: Boolean,
    isFocusDndMode: Boolean,
    mediaVolume: Float,
    screenBrightness: Float,
    isDeviceOwner: Boolean = false,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onTogglePerformanceHud: (Boolean) -> Unit,
    onToggleFocusDndMode: (Boolean) -> Unit,
    onToggleGamepadOverlay: (Boolean) -> Unit,
    onToggleKioskMode: (Boolean) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
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
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(380.dp)
                        .background(BackgroundDark)
                        .border(1.dp, GlassBorder, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 頂部標題與關閉按鈕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎮 掌機系統控制台",
                                fontSize = 17.sp,
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

                    // 1. 音量與亮度控制滑桿
                    Text(
                        text = "硬體調節 (Volume & Brightness)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SteamDeckAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 媒體音量
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("媒體音量", fontSize = 12.sp, color = TextPrimary)
                                    Text("${(mediaVolume * 100).toInt()}%", fontSize = 11.sp, color = AccentGreen)
                                }
                                Slider(
                                    value = mediaVolume,
                                    onValueChange = onVolumeChange,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentGreen,
                                        activeTrackColor = AccentGreen,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }

                        // 螢幕亮度
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessHigh,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("螢幕亮度", fontSize = 12.sp, color = TextPrimary)
                                    Text("${(screenBrightness * 100).toInt()}%", fontSize = 11.sp, color = AccentGold)
                                }
                                Slider(
                                    value = screenBrightness,
                                    onValueChange = onBrightnessChange,
                                    valueRange = 0.05f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentGold,
                                        activeTrackColor = AccentGold,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Wi-Fi 與 藍牙快速設定卡片
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Default.Wifi,
                            title = "Wi-Fi 連線",
                            subtitle = "管理無線網路",
                            tint = Ps5Blue,
                            onClick = onOpenWifiSettings,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            icon = Icons.Default.Bluetooth,
                            title = "藍牙配對",
                            subtitle = "外接手柄/耳機",
                            tint = SteamDeckAccent,
                            onClick = onOpenBluetoothSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. 掌機專注模式與介面自訂開關
                    Text(
                        text = "介面與專注防護",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SteamDeckAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 🎮 掌機專注模式 (靜音系統通知)
                    ToggleCard(
                        title = "🎮 掌機專注模式 (通知靜音)",
                        description = "靜音後台通知聲與鈴聲，解決突然發聲卻看不到的困擾",
                        checked = isFocusDndMode,
                        onCheckedChange = onToggleFocusDndMode,
                        actionText = "通知管理",
                        onActionClick = onOpenNotificationSettings
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 📊 顯示左上角效能 HUD
                    ToggleCard(
                        title = "📊 左上角效能 HUD (FPS/溫度)",
                        description = "關閉後完全隱藏左上角狀態，享受 100% 純淨海報視覺",
                        checked = showPerformanceHud,
                        onCheckedChange = onTogglePerformanceHud
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔒 掌機沉浸鎖定 (Kiosk)
                    ToggleCard(
                        title = if (isDeviceOwner) "🔒 掌機極限硬體鎖死 (Device Owner)" else "🔒 掌機沉浸鎖定 (Kiosk)",
                        description = if (isDeviceOwner) "已取得設備擁有者特權：底層徹底拔除狀態列下拉與系統鍵" else "停用系統下拉通知欄與邊緣返回手勢 (常規模式)",
                        checked = isKioskModeActive,
                        onCheckedChange = onToggleKioskMode
                    )

                    if (!isDeviceOwner) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF161B22))
                                .border(1.dp, SteamDeckAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "💡 如需實體主機級「徹底廢除狀態列下拉」，請於電腦終端執行：",
                                fontSize = 10.sp,
                                color = SteamDeckAccent
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "adb shell dpm set-device-owner com.uyen.launcher/.receiver.UyenDeviceAdminReceiver",
                                fontSize = 9.sp,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 觸控虛擬手柄懸浮層
                    ToggleCard(
                        title = "🕹️ 觸控虛擬手柄層",
                        description = "為復古小遊戲提供半透明搖桿按鍵覆蓋",
                        checked = isGamepadOverlayActive,
                        onCheckedChange = onToggleGamepadOverlay
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. 即時效能數據
                    Text(
                        text = "即時硬體監控 (Helio G85)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SteamDeckAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HudMiniCard(label = "FPS", value = "${stats.fps}", sub = "90Hz", color = AccentGreen, modifier = Modifier.weight(1f))
                        HudMiniCard(label = "溫度", value = "${stats.batteryTempCelsius}°C", sub = if (stats.isCharging) "充電中" else "電池", color = AccentGold, modifier = Modifier.weight(1f))
                        HudMiniCard(label = "RAM", value = "${stats.ramUsedMb}M", sub = "/ ${stats.ramTotalMb}M", color = Ps5Blue, modifier = Modifier.weight(1f))
                        HudMiniCard(label = "電量", value = "${stats.batteryPercent}%", sub = "5000mAh", color = AccentGreen, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. 底部快捷操作
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onClose()
                                onOpenTaskSwitcher()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = SteamDeckAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .border(1.dp, SteamDeckAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Text(text = "📑 多工管理", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onCleanRam,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.2f), contentColor = AccentGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Text(text = "⚡ 釋放記憶體", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onClose()
                            onOpenControllerMode()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold.copy(alpha = 0.2f), contentColor = AccentGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Text(text = "🎮 進入 UyenController PC 手柄模式", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(text = description, fontSize = 10.sp, color = TextMuted, lineHeight = 14.sp)
            if (actionText != null && onActionClick != null) {
                Text(
                    text = "⚙️ $actionText",
                    fontSize = 10.sp,
                    color = Ps5Blue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onActionClick() }
                        .padding(top = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentGold
            )
        )
    }
}

@Composable
private fun HudMiniCard(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 9.sp, color = TextMuted)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = sub, fontSize = 8.sp, color = TextSecondary)
    }
}
