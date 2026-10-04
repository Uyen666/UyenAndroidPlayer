package com.uyen.launcher.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.uyen.launcher.core.kiosk.ConsoleLockManager
import com.uyen.launcher.core.service.GlobalConsoleEdgeService
import com.uyen.launcher.core.util.SystemBarUtil
import com.uyen.launcher.presentation.home.HomeScreen
import com.uyen.launcher.presentation.home.HomeViewModel
import com.uyen.launcher.presentation.splash.SteamOsBootScreen
import com.uyen.launcher.presentation.theme.UyenTheme

/**
 * UyenLauncher 主 Activity
 * 作為 Android 系統預設 Home Launcher 入口點
 */
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UyenTheme {
                val openSettingsDirectly = intent.getBooleanExtra("OPEN_SETTINGS", false)
                var showBootAnimation by remember { mutableStateOf(!openSettingsDirectly) }

                AnimatedVisibility(
                    visible = showBootAnimation,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SteamOsBootScreen(
                        onFinished = {
                            showBootAnimation = false
                        }
                    )
                }

                if (!showBootAnimation) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 於 View 結構初始化後安全隱藏系統列
        SystemBarUtil.hideSystemBars(this)
        com.uyen.launcher.core.hardware.SystemControlManager.checkAndClearDebugApp(this)
        com.uyen.launcher.core.service.GlobalConsoleEdgeService.start(this)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        com.uyen.launcher.core.hardware.SystemControlManager.checkAndClearDebugApp(this)
        homeViewModel.onLauncherResumed()
        SystemBarUtil.hideSystemBars(this)
        com.uyen.launcher.core.service.GlobalConsoleEdgeService.start(this)
        homeViewModel.refreshGoogleAccounts()
        homeViewModel.checkDeviceOwnerState()
        val prefs = getSharedPreferences("uyen_launcher_ui_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("kiosk_auto_lock", true) &&
            ConsoleLockManager.isDeviceOwner(this) &&
            !ConsoleLockManager.isLockTaskActive(this)
        ) {
            ConsoleLockManager.enableConsoleLock(this)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("EXIT_LAUNCHER", false)) {
            homeViewModel.exitLauncher(this)
            return
        }
        if (intent.getBooleanExtra("OPEN_OVERLAY_SETTINGS", false)) {
            GlobalConsoleEdgeService.showSettings(this)
            return
        } else if (intent.getBooleanExtra("OPEN_TASK_SWITCHER", false)) {
            homeViewModel.setTaskSwitcherOpen(true)
        } else if (intent.getBooleanExtra("OPEN_SETTINGS", false)) {
            homeViewModel.setSettingsOpen(true)
        } else if (intent.getBooleanExtra("OPEN_CONTROLLER_MODE", false)) {
            homeViewModel.setFullScreenControllerMode(true)
        } else {
            homeViewModel.handleHome()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemBarUtil.hideSystemBars(this)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 作為預設 Launcher，攔截 Back 鍵以關閉懸浮面板或回到主畫面
        homeViewModel.handleBack()
    }
}
