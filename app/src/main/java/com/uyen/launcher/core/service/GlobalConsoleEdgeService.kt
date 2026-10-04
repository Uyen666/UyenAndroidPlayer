package com.uyen.launcher.core.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.uyen.launcher.R
import com.uyen.launcher.core.hardware.PerformanceMonitor
import com.uyen.launcher.core.hardware.SystemControlManager
import com.uyen.launcher.core.hardware.SystemMemoryManager
import com.uyen.launcher.data.model.SystemStats
import com.uyen.launcher.presentation.MainActivity
import com.uyen.launcher.presentation.home.components.QuickSettingsDrawer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 掌機全局懸浮快捷與效能監控服務 (GlobalConsoleEdgeService)
 *
 * 整合三大全局系統功能：
 * 1. 右下角邊緣懸浮膠囊：提供全局 [主頁]、[返回]、[多工(呼出原生系統真實後台)]、[設定] 快捷導航
 * 2. 左側全局懸浮設定抽屜：點擊 [設定] 直接在當前 App 左側彈出懸浮控制台小視窗，無需切回主畫面
 * 3. 頂部中央微型效能 HUD：全局透明懸浮顯示實時 FPS、電池溫度、RAM 與電量，觸控完全穿透至遊戲
 */
class GlobalConsoleEdgeService : Service() {

    private var windowManager: WindowManager? = null

    // 1. 右下角導航抽屜
    private var rootView: FrameLayout? = null
    private var collapsedHandle: View? = null
    private var expandedCapsule: LinearLayout? = null
    private var isExpanded = false
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private val autoCollapseRunnable = Runnable { collapseCapsule() }

    // 2. 左側全局懸浮設定面板 (在任何 App 內彈出，統一為掌機控制台 QuickSettingsDrawer 佈局)
    private var settingsOverlayRootView: View? = null
    private var isSettingsOverlayVisible = false
    private var settingsOverlayLayoutParams: WindowManager.LayoutParams? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private lateinit var systemControlManager: SystemControlManager
    private val drawerVisibleState = mutableStateOf(false)
    private val drawerStatsState = mutableStateOf(SystemStats())
    private val drawerVolumeState = mutableStateOf(0.5f)
    private val drawerBrightnessState = mutableStateOf(0.7f)
    private val drawerFocusDndState = mutableStateOf(false)
    private val drawerGamepadState = mutableStateOf(false)
    private val drawerKioskState = mutableStateOf(false)
    private val drawerHudState = mutableStateOf(true)
    private val drawerRamMessageState = mutableStateOf<String?>(null)

    // 3. 頂部中央全局效能 HUD
    private var hudRootView: LinearLayout? = null
    private var tvFps: TextView? = null
    private var tvTemp: TextView? = null
    private var tvRam: TextView? = null
    private var tvBattery: TextView? = null
    private var tvBatteryIcon: TextView? = null
    private var hudLayoutParams: WindowManager.LayoutParams? = null
    private var isHudVisible = false
    private var isHudAttached = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null

    private var performanceMonitor: PerformanceMonitor? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "show_hud") {
            val show = prefs.getBoolean("show_hud", true)
            setHudVisible(show)
        }
    }

    companion object {
        private const val TAG = "GlobalConsoleEdge"
        private const val CHANNEL_ID = "uyen_edge_overlay_channel"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_UPDATE_HUD = "ACTION_UPDATE_HUD"
        private const val EXTRA_SHOW_HUD = "EXTRA_SHOW_HUD"
        private const val ACTION_SHOW_SETTINGS = "ACTION_SHOW_SETTINGS"

        fun isOverlayPermissionGranted(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun requestOverlayPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun start(context: Context) {
            if (!isOverlayPermissionGranted(context)) {
                Log.w(TAG, "Cannot start GlobalConsoleEdgeService: overlay permission not granted")
                return
            }
            val intent = Intent(context, GlobalConsoleEdgeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateHudVisibility(context: Context, visible: Boolean) {
            if (!isOverlayPermissionGranted(context)) return
            val intent = Intent(context, GlobalConsoleEdgeService::class.java).apply {
                putExtra(ACTION_UPDATE_HUD, true)
                putExtra(EXTRA_SHOW_HUD, visible)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun showSettings(context: Context) {
            if (!isOverlayPermissionGranted(context)) return
            val intent = Intent(context, GlobalConsoleEdgeService::class.java).apply {
                putExtra(ACTION_SHOW_SETTINGS, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GlobalConsoleEdgeService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        SystemControlManager.checkAndClearDebugApp(applicationContext)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()

        setupOverlayView()
        setupHudOverlayView()

        systemControlManager = SystemControlManager(applicationContext)

        // 啟動實時硬體監控 (全域採樣)
        performanceMonitor = PerformanceMonitor(applicationContext).apply {
            startMonitoring(serviceScope)
        }
        serviceScope.launch {
            performanceMonitor?.stats?.collect { stats ->
                updateHudUI(stats)
                drawerStatsState.value = stats
            }
        }
        serviceScope.launch {
            systemControlManager.mediaVolume.collect {
                drawerVolumeState.value = it
            }
        }
        serviceScope.launch {
            systemControlManager.screenBrightness.collect {
                drawerBrightnessState.value = it
            }
        }
        serviceScope.launch {
            systemControlManager.isFocusDndMode.collect {
                drawerFocusDndState.value = it
            }
        }

        // 監聽並同步偏好設定
        val prefs = getSharedPreferences("uyen_launcher_ui_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        val shouldShowHud = prefs.getBoolean("show_hud", true)
        drawerHudState.value = shouldShowHud
        drawerGamepadState.value = prefs.getBoolean("gamepad_overlay", false)
        drawerKioskState.value = prefs.getBoolean("kiosk_mode", false)
        setHudVisible(shouldShowHud)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (rootView == null) setupOverlayView()
        if (hudRootView == null) setupHudOverlayView()

        if (intent?.getBooleanExtra(ACTION_UPDATE_HUD, false) == true) {
            val show = intent.getBooleanExtra(EXTRA_SHOW_HUD, true)
            setHudVisible(show)
        }
        if (intent?.getBooleanExtra(ACTION_SHOW_SETTINGS, false) == true) {
            showSettingsOverlay()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT

        overlayLayoutParams?.let { params ->
            params.gravity = if (isPortrait) (Gravity.CENTER_VERTICAL or Gravity.END) else (Gravity.BOTTOM or Gravity.END)
            params.y = if (isPortrait) dpToPx(80) else dpToPx(55)
            try {
                windowManager?.updateViewLayout(rootView, params)
            } catch (_: Exception) {}
        }

        hudLayoutParams?.let { params ->
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = dpToPx(8)
            try {
                if (isHudAttached) {
                    windowManager?.updateViewLayout(hudRootView, params)
                }
            } catch (_: Exception) {}
        }
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Uyen 掌機全局邊緣懸浮導航",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "提供遊戲與應用內的全局返回、控制台與效能 HUD"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Uyen 掌機全局導航與效能監控")
                .setContentText("掌機控制台與中央頂部效能 HUD 運行中")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ==========================================
    // 1. 右下角邊緣懸浮小條
    // ==========================================
    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        val dp8 = dpToPx(8)
        val dp40 = dpToPx(40)
        val dp60 = dpToPx(60)

        rootView = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        collapsedHandle = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false

            val handleTab = View(context).apply {
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#EE0F172A"))
                    setStroke(dpToPx(2), Color.parseColor("#00E5FF"))
                    val radius = dp8.toFloat() * 0.5f
                    cornerRadii = floatArrayOf(
                        radius, radius,
                        0f, 0f,
                        0f, 0f,
                        radius, radius
                    )
                }
                background = bg
            }

            val tabParams = FrameLayout.LayoutParams(dp8, dp60).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            addView(handleTab, tabParams)

            var startX = 0f
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        triggerHaptic()
                        expandCapsule()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!isExpanded && (startX - event.rawX) > dpToPx(6)) {
                            triggerHaptic()
                            expandCapsule()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        expandedCapsule = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            val padH = dpToPx(10)
            val padV = dpToPx(6)
            setPadding(padH, padV, padH, padV)

            val capsuleBg = GradientDrawable().apply {
                setColor(Color.parseColor("#EE121424"))
                setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"))
                cornerRadius = dpToPx(20).toFloat()
            }
            background = capsuleBg

            // 按鈕 1：[ ⌂ 主頁 ]
            val btnHome = createPillButton("⌂", "主頁", Color.parseColor("#22C55E")) {
                triggerHaptic()
                returnToLauncherHome()
                collapseCapsule()
            }
            addView(btnHome)

            addSpacer(dpToPx(6))

            // 按鈕 2：[ ↩ 返回 ]
            val btnBack = createPillButton("↩", "返回", Color.parseColor("#38BDF8")) {
                triggerHaptic()
                injectBackAction()
                resetAutoCollapseTimer()
            }
            addView(btnBack)

            addSpacer(dpToPx(6))

            // 按鈕 3：[ ⧉ 多工 ] (優先呼出原生系統多工，看見所有執行中 App)
            val btnTasks = createPillButton("⧉", "多工", Color.parseColor("#F59E0B")) {
                triggerHaptic()
                openSystemTaskSwitcher()
                collapseCapsule()
            }
            addView(btnTasks)

            addSpacer(dpToPx(6))

            // 按鈕 4：[ ⚙ 設定 ] (在當前 App 左邊直接彈出懸浮設定視窗！)
            val btnSettings = createPillButton("⚙", "設定", Color.parseColor("#FFD54F")) {
                triggerHaptic()
                collapseCapsule()
                showSettingsOverlay()
            }
            addView(btnSettings)

            addSpacer(dpToPx(6))

            // 按鈕 5：[ ✕ 縮回 ]
            val btnClose = createPillButton("✕", "", Color.parseColor("#94A3B8")) {
                triggerHaptic()
                collapseCapsule()
            }
            addView(btnClose)
        }

        val rootParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }

        rootView?.addView(collapsedHandle, FrameLayout.LayoutParams(dp40, dp60).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        })
        rootView?.addView(expandedCapsule, rootParams)

        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

            gravity = if (isPortrait) (Gravity.CENTER_VERTICAL or Gravity.END) else (Gravity.BOTTOM or Gravity.END)
            x = 0
            y = if (isPortrait) dpToPx(80) else dpToPx(55)
            width = dp40
            height = dp60
        }
        overlayLayoutParams = layoutParams

        rootView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE && isExpanded) {
                collapseCapsule()
                true
            } else {
                false
            }
        }

        try {
            windowManager?.addView(rootView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun expandCapsule() {
        if (isExpanded) return
        isExpanded = true
        collapsedHandle?.visibility = View.GONE
        expandedCapsule?.visibility = View.VISIBLE
        overlayLayoutParams?.let { params ->
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            try {
                windowManager?.updateViewLayout(rootView, params)
            } catch (_: Exception) {}
        }
        resetAutoCollapseTimer()
    }

    private fun collapseCapsule() {
        if (!isExpanded) return
        isExpanded = false
        mainHandler.removeCallbacks(autoCollapseRunnable)
        expandedCapsule?.visibility = View.GONE
        collapsedHandle?.visibility = View.VISIBLE
        overlayLayoutParams?.let { params ->
            params.width = dpToPx(40)
            params.height = dpToPx(60)
            try {
                windowManager?.updateViewLayout(rootView, params)
            } catch (_: Exception) {}
        }
    }

    private fun resetAutoCollapseTimer() {
        mainHandler.removeCallbacks(autoCollapseRunnable)
        mainHandler.postDelayed(autoCollapseRunnable, 3500)
    }

    // ==========================================
    // 2. 左側全局懸浮設定面板 (在當前 App 內直接彈出，統一為掌機控制台 QuickSettingsDrawer 佈局)
    // ==========================================
    private fun showSettingsOverlay() {
        if (isSettingsOverlayVisible) return
        if (settingsOverlayRootView == null) {
            buildSettingsOverlayView()
        }
        isSettingsOverlayVisible = true

        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            gravity = Gravity.FILL
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
        settingsOverlayLayoutParams = params

        try {
            if (settingsOverlayRootView?.isAttachedToWindow != true) {
                windowManager?.addView(settingsOverlayRootView, params)
            } else {
                windowManager?.updateViewLayout(settingsOverlayRootView, params)
            }
            mainHandler.post {
                drawerVisibleState.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show settings overlay", e)
        }
    }

    private fun hideSettingsOverlay() {
        if (!isSettingsOverlayVisible) return
        isSettingsOverlayVisible = false
        drawerVisibleState.value = false

        settingsOverlayLayoutParams?.let { params ->
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            try {
                windowManager?.updateViewLayout(settingsOverlayRootView, params)
            } catch (_: Exception) {}
        }

        mainHandler.postDelayed({
            try {
                if (!isSettingsOverlayVisible && settingsOverlayRootView?.isAttachedToWindow == true) {
                    windowManager?.removeView(settingsOverlayRootView)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove settings overlay", e)
            }
        }, 280)
    }

    private fun buildSettingsOverlayView() {
        val owner = OverlayLifecycleOwner().apply { onCreate() }
        overlayLifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                val stats by remember { drawerStatsState }
                val volume by remember { drawerVolumeState }
                val brightness by remember { drawerBrightnessState }
                val focusDnd by remember { drawerFocusDndState }
                val gamepad by remember { drawerGamepadState }
                val kiosk by remember { drawerKioskState }
                val hud by remember { drawerHudState }
                val ramMsg by remember { drawerRamMessageState }
                val visible by remember { drawerVisibleState }

                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                val isDeviceOwner = dpm?.isDeviceOwnerApp(packageName) == true

                QuickSettingsDrawer(
                    visible = visible,
                    stats = stats,
                    isGamepadOverlayActive = gamepad,
                    isKioskModeActive = kiosk,
                    showPerformanceHud = hud,
                    isFocusDndMode = focusDnd,
                    mediaVolume = volume,
                    screenBrightness = brightness,
                    isDeviceOwner = isDeviceOwner,
                    onVolumeChange = { vol ->
                        drawerVolumeState.value = vol
                        systemControlManager.setMediaVolume(vol)
                    },
                    onBrightnessChange = { b ->
                        drawerBrightnessState.value = b
                        applyOverlayBrightness(b)
                    },
                    onTogglePerformanceHud = { show ->
                        drawerHudState.value = show
                        val prefs = getSharedPreferences("uyen_launcher_ui_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("show_hud", show).apply()
                        setHudVisible(show)
                    },
                    onToggleFocusDndMode = { dnd ->
                        drawerFocusDndState.value = dnd
                        systemControlManager.setFocusDndMode(dnd)
                    },
                    onToggleGamepadOverlay = { active ->
                        drawerGamepadState.value = active
                        val prefs = getSharedPreferences("uyen_launcher_ui_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("gamepad_overlay", active).apply()
                    },
                    onToggleKioskMode = { k ->
                        drawerKioskState.value = k
                        val prefs = getSharedPreferences("uyen_launcher_ui_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("kiosk_mode", k).apply()
                    },
                    onOpenWifiSettings = {
                        hideSettingsOverlay()
                        systemControlManager.openWifiSettings()
                    },
                    onOpenBluetoothSettings = {
                        hideSettingsOverlay()
                        systemControlManager.openBluetoothSettings()
                    },
                    onOpenNotificationSettings = {
                        hideSettingsOverlay()
                        systemControlManager.openNotificationSettings()
                    },
                    onOpenControllerMode = {
                        hideSettingsOverlay()
                        openControllerMode()
                    },
                    onOpenTaskSwitcher = {
                        hideSettingsOverlay()
                        openSystemTaskSwitcher()
                    },
                    onCleanRam = {
                        cleanBackgroundProcesses()
                    },
                    ramCleanMessage = ramMsg,
                    onExitLauncher = {
                        hideSettingsOverlay()
                        exitLauncherDirectly()
                    },
                    onClose = {
                        hideSettingsOverlay()
                    }
                )
            }
        }
        settingsOverlayRootView = composeView
    }

    private fun applyOverlayBrightness(ratio: Float) {
        val safeRatio = ratio.coerceIn(0.05f, 1.0f)
        settingsOverlayLayoutParams?.let { params ->
            params.screenBrightness = safeRatio
            try {
                windowManager?.updateViewLayout(settingsOverlayRootView, params)
            } catch (_: Exception) {}
        }
        val prefs = getSharedPreferences("uyen_launcher_ui_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("screen_brightness", safeRatio).apply()
    }

    private fun openControllerMode() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("OPEN_CONTROLLER_MODE", true)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private class OverlayLifecycleOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        fun onCreate() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }

    private fun cleanBackgroundProcesses() {
        serviceScope.launch {
            drawerRamMessageState.value = "⚡ 正在釋放系統 RAM 與後台程序..."
            val result = SystemMemoryManager.cleanRam(this@GlobalConsoleEdgeService)
            performanceMonitor?.updateSystemStats()
            drawerRamMessageState.value = result.displayMessage
            android.widget.Toast.makeText(
                this@GlobalConsoleEdgeService,
                result.displayMessage,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            delay(3500)
            drawerRamMessageState.value = null
        }
    }

    private fun exitLauncherDirectly() {
        hideSettingsOverlay()
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXIT_LAUNCHER", true)
            }
            startActivity(intent)
        } catch (_: Exception) {
            stop(this)
        }
    }

    // ==========================================
    // 3. 頂部中央全局微型效能 HUD
    // ==========================================
    private fun setupHudOverlayView() {
        hudRootView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = dpToPx(12)
            val padV = dpToPx(3)
            setPadding(padH, padV, padH, padV)

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E610131F"))
                setStroke(dpToPx(1), Color.parseColor("#26FFFFFF"))
                cornerRadius = dpToPx(14).toFloat()
            }
            background = bg

            // 1. FPS 綠點 + 文字
            val fpsDot = View(context).apply {
                val dotBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#22C55E"))
                }
                background = dotBg
            }
            addView(fpsDot, LinearLayout.LayoutParams(dpToPx(5), dpToPx(5)).apply {
                rightMargin = dpToPx(4)
                gravity = Gravity.CENTER_VERTICAL
            })

            tvFps = TextView(context).apply {
                text = "60 FPS"
                setTextColor(Color.parseColor("#22C55E"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            }
            addView(tvFps)

            // 分隔線
            addHudDivider(this)

            // 2. 電池溫度
            val tvTempIcon = TextView(context).apply {
                text = "🌡 "
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            }
            addView(tvTempIcon)

            tvTemp = TextView(context).apply {
                text = "34.0°C"
                setTextColor(Color.parseColor("#FFD54F"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            }
            addView(tvTemp)

            // 分隔線
            addHudDivider(this)

            // 3. RAM 使用量
            val tvRamLabel = TextView(context).apply {
                text = "RAM "
                setTextColor(Color.parseColor("#94A3B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f)
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }
            addView(tvRamLabel)

            tvRam = TextView(context).apply {
                text = "2.4/3.6G"
                setTextColor(Color.parseColor("#38BDF8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            }
            addView(tvRam)

            // 分隔線
            addHudDivider(this)

            // 4. 電池電量
            tvBatteryIcon = TextView(context).apply {
                text = "⚡ "
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            }
            addView(tvBatteryIcon)

            tvBattery = TextView(context).apply {
                text = "100%"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            }
            addView(tvBattery)
        }

        val hudParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            // 注意：設定 FLAG_NOT_TOUCHABLE 使觸控完全穿透至底層 App/遊戲，絕不干擾遊玩操作
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(8)
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        hudLayoutParams = hudParams
    }

    private fun addHudDivider(parent: LinearLayout) {
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#26FFFFFF"))
        }
        val params = LinearLayout.LayoutParams(dpToPx(1), dpToPx(9)).apply {
            leftMargin = dpToPx(7)
            rightMargin = dpToPx(7)
            gravity = Gravity.CENTER_VERTICAL
        }
        parent.addView(divider, params)
    }

    private fun setHudVisible(visible: Boolean) {
        isHudVisible = visible
        if (visible) {
            if (!isHudAttached && hudRootView != null) {
                try {
                    windowManager?.addView(hudRootView, hudLayoutParams)
                    isHudAttached = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to attach HUD overlay", e)
                }
            }
        } else {
            if (isHudAttached && hudRootView != null) {
                try {
                    windowManager?.removeView(hudRootView)
                    isHudAttached = false
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to detach HUD overlay", e)
                }
            }
        }
    }

    private fun updateHudUI(stats: SystemStats) {
        if (!isHudVisible) return
        tvFps?.text = "${stats.fps} FPS"
        tvTemp?.apply {
            text = "${"%.1f".format(stats.batteryTempCelsius)}°C"
            setTextColor(if (stats.batteryTempCelsius >= 40f) Color.parseColor("#EF4444") else Color.parseColor("#FFD54F"))
        }
        val ramUsedGb = stats.ramUsedMb / 1024f
        val ramTotalGb = stats.ramTotalMb / 1024f
        tvRam?.text = if (stats.ramTotalMb > 0) {
            "${"%.1f".format(ramUsedGb)}/${"%.1f".format(ramTotalGb)}G"
        } else {
            "${stats.ramUsedMb}M"
        }
        tvBatteryIcon?.text = if (stats.isCharging) "⚡ " else "🔋 "
        tvBattery?.apply {
            text = "${stats.batteryPercent}%"
            setTextColor(if (stats.batteryPercent < 20) Color.parseColor("#EF4444") else Color.WHITE)
        }
    }

    // ==========================================
    // 通用導航操作
    // ==========================================
    private fun LinearLayout.addSpacer(widthPx: Int) {
        val spacer = View(context)
        addView(spacer, LinearLayout.LayoutParams(widthPx, 1))
    }

    private fun createPillButton(iconText: String, label: String, accentColor: Int, onClick: () -> Unit): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val ph = dpToPx(8)
            val pv = dpToPx(5)
            setPadding(ph, pv, ph, pv)

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#2AFFFFFF"))
                cornerRadius = dpToPx(14).toFloat()
            }
            background = bg

            setOnClickListener { onClick() }
        }

        val tvIcon = TextView(this).apply {
            text = iconText
            setTextColor(accentColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }
        layout.addView(tvIcon)

        if (label.isNotEmpty()) {
            val tvLabel = TextView(this).apply {
                text = " $label"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            layout.addView(tvLabel)
        }

        return layout
    }

    private fun returnToLauncherHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            UyenConsoleAccessibilityService.performHome()
        }
    }

    /**
     * 呼出真實系統多工列表 (優先呼出 Android 原生 Recents，看清所有執行中 App)
     */
    private fun openSystemTaskSwitcher() {
        val handled = UyenConsoleAccessibilityService.performRecents()
        if (!handled) {
            // 若尚未開啟無障礙服務，退回到啟動器自訂多工視窗
            openTaskSwitcherInLauncher()
        }
    }

    private fun openTaskSwitcherInLauncher() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TASK_SWITCHER", true)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun injectBackAction() {
        val handled = UyenConsoleAccessibilityService.performBack()
        if (!handled) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                android.widget.Toast.makeText(
                    this,
                    "請在「已下載的應用程式」中啟用 UyenConsole 服務以啟用全局返回鍵",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                returnToLauncherHome()
            }
        }
    }

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18)
            }
        } catch (_: Exception) {}
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        performanceMonitor?.stopMonitoring()
        mainHandler.removeCallbacks(autoCollapseRunnable)

        val prefs = getSharedPreferences("uyen_launcher_ui_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)

        hideSettingsOverlay()
        overlayLifecycleOwner?.onDestroy()
        overlayLifecycleOwner = null

        if (windowManager != null) {
            try {
                if (rootView != null) windowManager?.removeView(rootView)
            } catch (_: Exception) {}
            try {
                if (isHudAttached && hudRootView != null) {
                    windowManager?.removeView(hudRootView)
                    isHudAttached = false
                }
            } catch (_: Exception) {}
        }
    }
}
