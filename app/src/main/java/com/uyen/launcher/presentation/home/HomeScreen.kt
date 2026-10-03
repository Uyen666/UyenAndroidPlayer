package com.uyen.launcher.presentation.home

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.uyen.launcher.core.util.AppIconUtil
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.controller.TouchGamepadOverlay
import com.uyen.launcher.presentation.home.components.BottomControlBar
import com.uyen.launcher.presentation.home.components.EmulatorAssistantDialog
import com.uyen.launcher.presentation.home.components.GameActionMenuDialog
import com.uyen.launcher.presentation.home.components.GameBannerBackdrop
import com.uyen.launcher.presentation.home.components.GameCarousel
import com.uyen.launcher.presentation.home.components.GoogleAccountDialog
import com.uyen.launcher.presentation.home.components.HandheldTaskSwitcherDialog
import com.uyen.launcher.presentation.home.components.HeroBanner
import com.uyen.launcher.presentation.home.components.HomeCardManagerDialog
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
    val selectedTab by viewModel.selectedTab.collectAsState()
    val displayGames by viewModel.currentTabGames.collectAsState()
    val googleAccount by viewModel.googleAccount.collectAsState()
    val availableGoogleAccounts by viewModel.availableGoogleAccounts.collectAsState()
    val isAccountDialogOpen by viewModel.isAccountDialogOpen.collectAsState()
    val selectedIndex by viewModel.selectedGameIndex.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()
    val stats by viewModel.systemStats.collectAsState()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isLibraryOpen by viewModel.isLibraryOpen.collectAsState()
    val isCardManagerOpen by viewModel.isCardManagerOpen.collectAsState()
    val pinnedGameIds by viewModel.pinnedGameIds.collectAsState()
    val isTaskSwitcherOpen by viewModel.isTaskSwitcherOpen.collectAsState()
    val isGamepadOverlayActive by viewModel.isGamepadOverlayActive.collectAsState()
    val isFullScreenControllerMode by viewModel.isFullScreenControllerMode.collectAsState()
    val isKioskModeEnabled by viewModel.isKioskModeEnabled.collectAsState()
    val isDeviceOwner by viewModel.isDeviceOwner.collectAsState()
    val isRetroArcadeOpen by viewModel.isRetroArcadeOpen.collectAsState()
    val assistantDialogItem by viewModel.assistantDialogItem.collectAsState()

    val showPerformanceHud by viewModel.showPerformanceHud.collectAsState()
    val mediaVolume by viewModel.mediaVolume.collectAsState()
    val screenBrightness by viewModel.screenBrightness.collectAsState()
    val isFocusDndMode by viewModel.isFocusDndMode.collectAsState()

    val runningTasks by viewModel.runningTasks.collectAsState()
    val boostMessage by viewModel.boostMessage.collectAsState()
    val currentGame = displayGames.getOrNull(selectedIndex) ?: displayGames.firstOrNull()

    val resolvedBanners by viewModel.resolvedBanners.collectAsState()
    val activeActionMenuGame by viewModel.activeActionMenuGame.collectAsState()

    // 遊戲自訂大海報挑選器 Launcher
    var pendingBannerGameId by remember { mutableStateOf<String?>(null) }
    val bannerPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val gameId = pendingBannerGameId
        if (uri != null && gameId != null) {
            viewModel.setCustomBannerForGame(gameId, uri)
        }
        pendingBannerGameId = null
    }

    // 本機相簿自選個人相片 Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateCustomAvatar(uri.toString())
        }
    }

    // Google Sign-In 官方登入與獲取頭像照片 Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data) {
            try {
                photoPickerLauncher.launch("image/*")
            } catch (_: Exception) {}
        }
    }

    // 監聽懸浮窗權限與生命週期
    var hasOverlayPermission by remember { mutableStateOf(viewModel.isOverlayPermissionGranted()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = viewModel.isOverlayPermissionGranted()
                if (hasOverlayPermission) {
                    com.uyen.launcher.core.service.GlobalConsoleEdgeService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // PS5 旗艦等級遊戲大海報背景 (依序走訪 自訂相片 -> 官方精選 -> Android TV 橫幅 -> Google Play 宣傳圖)
        GameBannerBackdrop(
            currentGame = currentGame,
            resolvedBanner = resolvedBanners[currentGame?.id],
            fallbackGames = games
        )

        // 主畫面主要內容佈局
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 頂部導航欄 (LevelUp 風格：左設定/Uyen/Tabs，右刷新/搜尋/Google頭像)
            TopNavigationBar(
                selectedTab = selectedTab,
                onSelectTab = { viewModel.selectTab(it) },
                stats = stats,
                googleAccount = googleAccount,
                onOpenSettings = { viewModel.setSettingsOpen(true) },
                onRefresh = { viewModel.refreshGames() },
                onSearch = { viewModel.setLibraryOpen(true) },
                onAccountClick = { viewModel.setAccountDialogOpen(true) }
            )

            // 中央遊戲與橫向輪播區 (支援分頁左右橫向微滑動 + 交叉淡入絲滑動效)
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(
                        initialOffsetX = { if (forward) it / 3 else -it / 3 },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(220)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { if (forward) -it / 3 else it / 3 },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeOut(animationSpec = tween(180))
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "tab_content_anim"
            ) { tab ->
                val tabGames = when (tab) {
                    com.uyen.launcher.data.model.MainNavTab.HOME -> {
                        val gameMap = games.associateBy { it.id }
                        val pinned = pinnedGameIds.mapNotNull { gameMap[it] }
                        if (pinned.isNotEmpty()) pinned else games.take(5)
                    }
                    com.uyen.launcher.data.model.MainNavTab.STREAMING -> games.filter {
                        it.category == com.uyen.launcher.data.model.GameCategory.STREAMING || it.id == "controller_mode"
                    }
                    com.uyen.launcher.data.model.MainNavTab.GAMES -> games.filter {
                        it.category == com.uyen.launcher.data.model.GameCategory.GALGAME ||
                        it.category == com.uyen.launcher.data.model.GameCategory.RETRO ||
                        it.category == com.uyen.launcher.data.model.GameCategory.CUSTOM
                    }
                }
                val tabIndex = selectedIndex.coerceIn(0, (tabGames.size - 1).coerceAtLeast(0))
                val tabCurrentGame = tabGames.getOrNull(tabIndex) ?: tabGames.firstOrNull()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    // 中央 Hero Banner 遊戲大圖與詳情 (PS5 質感大氣字型，去按鈕化)
                    if (tabCurrentGame != null) {
                        HeroBanner(
                            game = tabCurrentGame
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 底部 90Hz 水平輪播卡片列 (首頁預設展示 5 張自定義卡片，末尾提供「+ 添加」膠囊)
                    if (tabGames.isNotEmpty()) {
                        GameCarousel(
                            games = tabGames,
                            selectedIndex = tabIndex,
                            onSelectGame = { viewModel.selectGame(it) },
                            onLaunchGame = { viewModel.launchGame(it) },
                            showAddCard = tab == com.uyen.launcher.data.model.MainNavTab.HOME,
                            onAddCardClick = { viewModel.setCardManagerOpen(true) },
                            onCardLongClick = { game -> viewModel.openGameActionMenu(game) },
                            onRemoveGame = if (tab == com.uyen.launcher.data.model.MainNavTab.HOME) {
                                { game -> viewModel.unpinGameFromHome(game.id) }
                            } else null
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            // 底部控制欄 (LevelUp 同款佈局：左選單膠囊、正中功能膠囊、右多工圓鈕)
            BottomControlBar(
                onOpenMenu = { viewModel.setLibraryOpen(true) },
                onQuickAction = { viewModel.setSettingsOpen(true) },
                onTaskSwitcher = { viewModel.setTaskSwitcherOpen(true) }
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        // 螢幕最左側邊緣手勢偵測：向右滑動平滑展開控制台
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(28.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 8f) {
                            viewModel.setSettingsOpen(true)
                        }
                    }
                }
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
            pinnedGameIds = pinnedGameIds,
            onTogglePin = { viewModel.togglePinGame(it) },
            onLaunchGame = { viewModel.launchGame(it) },
            onClose = { viewModel.setLibraryOpen(false) }
        )

        // 掌機系統控制台 (Quick Settings Drawer)
        QuickSettingsDrawer(
            visible = isSettingsOpen,
            stats = stats,
            isGamepadOverlayActive = isGamepadOverlayActive,
            isKioskModeActive = isKioskModeEnabled,
            showPerformanceHud = showPerformanceHud,
            isFocusDndMode = isFocusDndMode,
            mediaVolume = mediaVolume,
            screenBrightness = screenBrightness,
            isDeviceOwner = isDeviceOwner,
            onVolumeChange = { viewModel.setVolume(it) },
            onBrightnessChange = { viewModel.setBrightness(context as? Activity, it) },
            onTogglePerformanceHud = { viewModel.setPerformanceHudVisible(it) },
            onToggleFocusDndMode = { viewModel.setFocusDndMode(it) },
            onToggleGamepadOverlay = { viewModel.setGamepadOverlayActive(it) },
            onToggleKioskMode = {
                viewModel.toggleKioskLock(context as? Activity)
            },
            onOpenWifiSettings = { viewModel.openWifiSettings() },
            onOpenBluetoothSettings = { viewModel.openBluetoothSettings() },
            onOpenNotificationSettings = { viewModel.openNotificationSettings() },
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

        // Google 帳號與系統管理中心彈窗
        GoogleAccountDialog(
            visible = isAccountDialogOpen,
            currentAccount = googleAccount,
            availableAccounts = availableGoogleAccounts,
            onSelectAccount = { viewModel.switchGoogleAccount(it) },
            onManageSystemAccounts = { viewModel.openManageSystemAccounts() },
            onAddAccount = { viewModel.openAddGoogleAccount() },
            onSyncGooglePhoto = {
                try {
                    googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                } catch (_: Exception) {
                    viewModel.openManageSystemAccounts()
                }
            },
            onPickCustomPhoto = {
                photoPickerLauncher.launch("image/*")
            },
            onClearCustomPhoto = {
                viewModel.updateCustomAvatar(null)
            },
            onSyncNow = {
                viewModel.boostPerformance("Google Play 雲端存檔同步完成！")
                viewModel.refreshGoogleAccounts()
            },
            onClose = { viewModel.setAccountDialogOpen(false) }
        )

        // 首頁輪播卡片自定義管理彈窗 (從收藏庫添加/移除卡片，自訂海報)
        HomeCardManagerDialog(
            visible = isCardManagerOpen,
            allGames = games,
            pinnedGameIds = pinnedGameIds,
            onTogglePin = { viewModel.togglePinGame(it) },
            onOpenActionMenu = { game -> viewModel.openGameActionMenu(game) },
            onClose = { viewModel.setCardManagerOpen(false) }
        )

        // 遊戲海報客製化與動作選單彈窗 (長按卡片或點擊換海報時開啟)
        activeActionMenuGame?.let { actionGame ->
            GameActionMenuDialog(
                game = actionGame,
                resolvedBanner = resolvedBanners[actionGame.id],
                isPinnedToHome = viewModel.isGamePinned(actionGame.id),
                isCustomBanner = viewModel.isCustomBanner(actionGame.id),
                onPickCustomBanner = {
                    pendingBannerGameId = actionGame.id
                    try {
                        bannerPhotoPickerLauncher.launch("image/*")
                    } catch (_: Exception) {}
                },
                onResetCustomBanner = {
                    viewModel.resetCustomBannerForGame(actionGame)
                },
                onReScrapePlayStore = {
                    viewModel.reScrapePlayStoreGraphic(actionGame)
                },
                onTogglePinToHome = {
                    viewModel.togglePinGame(actionGame.id)
                },
                onLaunchGame = {
                    viewModel.launchGame(actionGame)
                },
                onDismiss = {
                    viewModel.closeGameActionMenu()
                }
            )
        }

        // 內建 8-bit 太空突擊懷舊街機 (90Hz Smooth Canvas Mini-Game)
        if (isRetroArcadeOpen) {
            RetroArcadeScreen(
                soundManager = viewModel.soundManagerInstance,
                onExit = { viewModel.setRetroArcadeOpen(false) }
            )
        }

        // 若尚未授權全局懸浮窗權限，右下方顯示醒目的掌機風格授權引導膠囊
        AnimatedVisibility(
            visible = !hasOverlayPermission,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 62.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF00F172A))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                    .clickable { viewModel.requestOverlayPermission() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎮 啟用邊緣返回鍵",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "點此開啟懸浮權限",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
