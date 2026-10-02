package com.uyen.launcher.presentation.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.uyen.launcher.presentation.controller.TouchGamepadOverlay
import com.uyen.launcher.presentation.home.components.GameCarousel
import com.uyen.launcher.presentation.home.components.HeroBanner
import com.uyen.launcher.presentation.home.components.QuickSettingsDrawer
import com.uyen.launcher.presentation.home.components.SteamLibraryDialog
import com.uyen.launcher.presentation.home.components.TopNavigationBar
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.Ps5Blue

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val games by viewModel.games.collectAsState()
    val selectedIndex by viewModel.selectedGameIndex.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()
    val stats by viewModel.systemStats.collectAsState()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isLibraryOpen by viewModel.isLibraryOpen.collectAsState()
    val isGamepadOverlayActive by viewModel.isGamepadOverlayActive.collectAsState()
    val isFullScreenControllerMode by viewModel.isFullScreenControllerMode.collectAsState()

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

            Spacer(modifier = Modifier.height(12.dp))

            // 底部 90Hz 水平輪播卡片列
            if (games.isNotEmpty()) {
                GameCarousel(
                    games = games,
                    selectedIndex = selectedIndex,
                    onSelectGame = { viewModel.selectGame(it) },
                    onLaunchGame = { viewModel.launchGame(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 觸控虛擬手柄懸浮層 (無實體手柄時使用)
        if (isGamepadOverlayActive && !isFullScreenControllerMode) {
            TouchGamepadOverlay(
                isFullScreenControllerMode = false,
                onClose = { viewModel.setGamepadOverlayActive(false) }
            )
        }

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
            onToggleGamepadOverlay = { viewModel.setGamepadOverlayActive(it) },
            onOpenControllerMode = { viewModel.setFullScreenControllerMode(true) },
            onClose = { viewModel.setSettingsOpen(false) }
        )

        // 全螢幕 UyenController 變身 PC 遊戲手柄模式
        if (isFullScreenControllerMode) {
            TouchGamepadOverlay(
                isFullScreenControllerMode = true,
                onClose = { viewModel.setFullScreenControllerMode(false) }
            )
        }
    }
}
