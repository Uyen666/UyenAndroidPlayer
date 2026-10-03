package com.uyen.launcher.core.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.uyen.launcher.R
import com.uyen.launcher.presentation.MainActivity

/**
 * 掌機全局右下角邊緣懸浮小條服務 (GlobalConsoleEdgeService)
 *
 * 專為解決「打開其他 App / 遊戲後受限於沉浸/鎖定模式而回不來」的痛點：
 * 1. 在其他任何遊戲與應用中，螢幕右側保留清晰、質感出色的電競青色微光抽屜拉把 (Cyberpunk Pill Tab)
 * 2. 點擊即可滑出掌機操作膠囊：
 *    - [ ⌂ 主頁 ]：直接無縫返回 UyenLauncher 主頁
 *    - [ ↩ 返回 ]：注入系統 Back 鍵，在遊戲內返回上一頁
 *    - [ ⧉ 多工 ]：呼出 Uyen 多工任務切換器
 * 3. 3.5 秒無操作自動平滑縮回為邊緣小條。
 */
class GlobalConsoleEdgeService : Service() {

    private var windowManager: WindowManager? = null
    private var rootView: FrameLayout? = null
    private var collapsedHandle: View? = null
    private var expandedCapsule: LinearLayout? = null
    private var isExpanded = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoCollapseRunnable = Runnable { collapseCapsule() }

    private var vibrator: Vibrator? = null

    companion object {
        private const val TAG = "GlobalConsoleEdge"
        private const val CHANNEL_ID = "uyen_edge_overlay_channel"
        private const val NOTIFICATION_ID = 2001

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

        fun stop(context: Context) {
            context.stopService(Intent(context, GlobalConsoleEdgeService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        startAsForeground()
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (rootView == null) {
            setupOverlayView()
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
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Uyen 掌機邊緣懸浮導航",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "提供遊戲內的懸浮返回與主頁膠囊"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Uyen 掌機全局導航")
                .setContentText("右側邊緣懸浮返回/主頁鍵運作中")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private var overlayLayoutParams: WindowManager.LayoutParams? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val dp7 = dpToPx(7)   // 使用者要求嚴格 7dp 視覺寬度，精簡不突兀
        val dp36 = dpToPx(36) // 36dp 觸控熱區，向內撥動或點擊皆極其滑順
        val dp56 = dpToPx(56) // 56dp 舒適高度

        // 根佈局
        rootView = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        // 1. 常態收合狀態：右側邊緣電競質感微型指示條 (嚴格 7dp 視覺寬度，微光霓虹收邊，無冗餘符號)
        collapsedHandle = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false

            val handleTab = View(context).apply {
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#E60F172A")) // 深色掌機黑曜石金屬質感 (90% alpha)
                    setStroke(dpToPx(1), Color.parseColor("#8000E5FF")) // 1dp 雅緻電競霓虹青色收邊
                    val radius = dp7.toFloat() * 0.5f
                    cornerRadii = floatArrayOf(
                        radius, radius, // 左上圓角
                        0f, 0f,         // 右上平直靠邊
                        0f, 0f,         // 右下平直靠邊
                        radius, radius  // 左下圓角
                    )
                }
                background = bg
            }

            val tabParams = FrameLayout.LayoutParams(dp7, dp56).apply {
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

        // 2. 展開狀態：滑出的微型藥丸膠囊列
        expandedCapsule = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            val padH = dpToPx(10)
            val padV = dpToPx(6)
            setPadding(padH, padV, padH, padV)

            // 磨砂暗色毛玻璃卡片背景
            val capsuleBg = GradientDrawable().apply {
                setColor(Color.parseColor("#EE121424"))
                setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"))
                cornerRadius = dpToPx(20).toFloat()
            }
            background = capsuleBg

            // 按鈕 1：[ ⌂ 主頁 ] (返回 UyenLauncher)
            val btnHome = createPillButton("⌂", "主頁", Color.parseColor("#22C55E")) {
                triggerHaptic()
                returnToLauncherHome()
                collapseCapsule()
            }
            addView(btnHome)

            addSpacer(dpToPx(6))

            // 按鈕 2：[ ↩ 返回 ] (全局返回鍵)
            val btnBack = createPillButton("↩", "返回", Color.parseColor("#38BDF8")) {
                triggerHaptic()
                injectBackAction()
                resetAutoCollapseTimer()
            }
            addView(btnBack)

            addSpacer(dpToPx(6))

            // 按鈕 3：[ ⧉ 多工 ] (呼出任務切換器)
            val btnTasks = createPillButton("⧉", "多工", Color.parseColor("#F59E0B")) {
                triggerHaptic()
                openTaskSwitcherInLauncher()
                collapseCapsule()
            }
            addView(btnTasks)

            addSpacer(dpToPx(6))

            // 按鈕 4：[ ✕ 縮回 ]
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

        rootView?.addView(collapsedHandle, FrameLayout.LayoutParams(dp36, dp56).apply {
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
            width = dp36
            height = dp56
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
            params.width = dpToPx(36)
            params.height = dpToPx(56)
            try {
                windowManager?.updateViewLayout(rootView, params)
            } catch (_: Exception) {}
        }
    }

    private fun resetAutoCollapseTimer() {
        mainHandler.removeCallbacks(autoCollapseRunnable)
        mainHandler.postDelayed(autoCollapseRunnable, 3500)
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

    private fun openTaskSwitcherInLauncher() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TASK_SWITCHER", true)
        }
        startActivity(intent)
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
        mainHandler.removeCallbacks(autoCollapseRunnable)
        if (rootView != null && windowManager != null) {
            try {
                windowManager?.removeView(rootView)
            } catch (_: Exception) {}
        }
    }
}
