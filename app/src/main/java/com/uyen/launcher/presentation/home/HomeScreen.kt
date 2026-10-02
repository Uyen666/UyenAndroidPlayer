package com.uyen.launcher.presentation.home

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.controller.TouchGamepadOverlay
import com.uyen.launcher.presentation.home.components.ConsoleDockBar
import com.uyen.launcher.presentation.home.components.EmulatorAssistantDialog
import com.uyen.launcher.presentation.home.components.GameCarousel
import com.uyen.launcher.presentation.home.components.HandheldTaskSwitcherDialog
import com.uyen.launcher.presentation.home.components.HeroBanner
import com.uyen.launcher.presentation.home.components.QuickSettingsDrawer
import com.uyen.launcher.presentation.home.components.SteamLibraryDialog
import com.uyen.launcher.presentation.home.components.TopNavigationBar
import com.uyen.launcher.presentation.minigame.RetroArcadeScreen
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.Ps5Blue

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val games by viewModel.games.collectAsState()
    val selectedIndex by viewModel.selectedGameIndex.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()
    val stats by viewModel.systemStats.collectAsState()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isLibraryOpen by viewModel.isLibraryOpen.collectAsState()
    val isTaskSwitcherOpen by viewModel.isTaskSwitcherOpen.collectAsState()
    val isGamepadOverlayActive by viewModel.isGamepadOverlayActive.collectAsState()
    val isFullScreenControllerMode by viewModel.isFullScreenControllerMode.collectAsState()
    val isKioskModeEnabled by viewModel.isKioskModeEnabled.collectAsState()
    val isRetroArcadeOpen by viewModel.isRetroArcadeOpen.collectAsState()
    val assistantDialogItem by viewModel.assistantDialogItem.collectAsState()

    val runningTasks by viewModel.runningTasks.collectAsState()
    val boostMessage by viewModel.boostMessage.collectAsState()

    val currentGame = games.getOrNull(selectedIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // 動態景深背景 (根據選中遊戲呈現平滑色彩渲染)
        Crossfade(targetState = currentGame?.category, label = "bg_crossfade") { cat ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                when (cat) {
                                    com.uyen.launcher.data.model.GameCategory.GALGAME -> Color(0x33A855F7)
                                    com.uyen.launcher.data.model.GameCategory.RETRO -> Color(0x33F97316)
                                    com.uyen.launcher.data.model.GameCategory.STREAMING -> Color(0x330284C7)
                                    com.uyen.launcher.data.model.GameCategory.CUSTOM -> Color(0x3322C55E)
                                    else -> Ps5Blue.copy(alpha = 0.25f)
                                },
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
            )
        }

        // 主畫面主要內容佈局
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 頂部導航欄 (左上效能 HUD / 正中玩家看板 / 右上遊戲庫)
            TopNavigationBar(
                profile = profile,
                stats = stats,
                onOpenSettings = { viewModel.setSettingsOpen(true) },
                onOpenLibrary = { viewModel.setLibraryOpen(true) },
                onToggleControllerMode = {
                    viewModel.setFullScreenControllerMode(true)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 中央 Hero Banner 遊戲大圖與詳情
            if (currentGame != null) {
                HeroBanner(
                    game = currentGame,
                    onLaunch = { viewModel.launchGame(currentGame) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 底部 90Hz 水平輪播卡片列
            if (games.isNotEmpty()) {
                GameCarousel(
                    games = games,
                    selectedIndex = selectedIndex,
                    onSelectGame = { viewModel.selectGame(it) },
                    onLaunchGame = { viewModel.launchGame(it) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 掌機專屬底部控制欄 (替代系統 Home / Back / 多工鍵 / 一鍵清理 RAM)
        ConsoleDockBar(
            onBack = { viewModel.handleBack() },
            onHome = { viewModel.handleHome() },
            onTasks = { viewModel.setTaskSwitcherOpen(true) },
            onCleanRam = { viewModel.cleanMemory() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = 12.dp)
        )

        // 電競加速 / 系統通知浮動膠囊 (Toast Banner)
        AnimatedVisibility(
            visible = boostMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            boostMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassBackground)
                        .border(1.5.dp, AccentGreen, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 觸控虛擬手柄懸浮層 (無實體手柄時使用)
        if (isGamepadOverlayActive && !isFullScreenControllerMode) {
            TouchGamepadOverlay(
                isFullScreenControllerMode = false,
                onClose = { viewModel.setGamepadOverlayActive(false) }
            )
        }

        // 掌機真實多工任務管理器彈窗
        HandheldTaskSwitcherDialog(
            visible = isTaskSwitcherOpen,
            runningTasks = runningTasks,
            onSwitchToTask = { task ->
                val game = games.firstOrNull { it.id == task.id } ?: GameItem(
                    id = task.id,
                    title = task.title,
                    category = com.uyen.launcher.data.model.GameCategory.TOOL,
                    packageName = task.packageName
                )
                viewModel.launchGame(game)
            },
            onKillTask = { viewModel.killTask(it) },
            onCleanAll = {
                viewModel.cleanMemory()
                viewModel.setTaskSwitcherOpen(false)
            },
            onClose = { viewModel.setTaskSwitcherOpen(false) }
        )

        // Steam OS 應用庫全螢幕彈窗
        SteamLibraryDialog(
            visible = isLibraryOpen,
            games = games,
            onLaunchGame = { viewModel.launchGame(it) },
            onClose = { viewModel.setLibraryOpen(false) }
        )

        // Quick Settings 效能監控側邊抽屜
        QuickSettingsDrawer(
            visible = isSettingsOpen,
            stats = stats,
            isGamepadOverlayActive = isGamepadOverlayActive,
            isKioskModeActive = isKioskModeEnabled,
            onToggleGamepadOverlay = { viewModel.setGamepadOverlayActive(it) },
            onToggleKioskMode = { enabled ->
                val activity = context as? Activity
                if (enabled) {
                    try {
                        activity?.startLockTask()
                        viewModel.setKioskModeEnabled(true)
                    } catch (_: Exception) {}
                } else {
                    try {
                        activity?.stopLockTask()
                        viewModel.setKioskModeEnabled(false)
                    } catch (_: Exception) {}
                }
            },
            onOpenControllerMode = { viewModel.setFullScreenControllerMode(true) },
            onOpenTaskSwitcher = { viewModel.setTaskSwitcherOpen(true) },
            onCleanRam = { viewModel.cleanMemory() },
            onClose = { viewModel.setSettingsOpen(false) }
        )

        // 全螢幕 UyenController 變身 PC 遊戲手柄模式
        if (isFullScreenControllerMode) {
            TouchGamepadOverlay(
                isFullScreenControllerMode = true,
                onClose = { viewModel.setFullScreenControllerMode(false) }
            )
        }

        // 掌機模擬器與核心導航指引彈窗
        EmulatorAssistantDialog(
            visible = assistantDialogItem != null,
            gameItem = assistantDialogItem,
            onCreateDirectories = { viewModel.createGameDirectories() },
            onPlayBuiltinArcade = { viewModel.setRetroArcadeOpen(true) },
            onClose = { viewModel.setAssistantDialogItem(null) }
        )

        // 內建 8-bit 太空突擊懷舊街機 (90Hz Smooth Canvas Mini-Game)
        if (isRetroArcadeOpen) {
            RetroArcadeScreen(
                soundManager = viewModel.soundManagerInstance,
                onExit = { viewModel.setRetroArcadeOpen(false) }
            )
        }
    }
}
