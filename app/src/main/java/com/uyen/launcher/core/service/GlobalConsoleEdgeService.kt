package com.uyen.launcher.core.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
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
 * 1. 在其他任何遊戲與應用中，螢幕右下角常態保留 4dp 極細微光線條 (Alpha 0.35f)，不干擾遊戲畫面。
 * 2. 點擊或向內撥動即可滑出微型藥丸膠囊：
 *    - [ ⌂ 主頁 ]：直接無縫返回 UyenLauncher 主頁
 *    - [ ↩ 返回 ]：注入系統 Back 鍵，在遊戲內返回上一頁
 *    - [ ⧉ 多工 ]：呼出 Uyen 多工任務切換器
 * 3. 3.5 秒無操作自動平滑縮回為極細邊緣小條。
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
        private const val CHANNEL_ID = "uyen_edge_overlay_channel"
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
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
                .setContentText("右下角邊緣懸浮小條運作中")
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

        val dp4 = dpToPx(4)
        val dp24 = dpToPx(24)
        val dp64 = dpToPx(64)

        // 根佈局
        rootView = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        // 1. 常態收合狀態：右下角邊緣微光小條 (4dp 視覺線條，24dp 寬熱區易於滑動)
        collapsedHandle = FrameLayout(this).apply {
            val handleBar = View(context).apply {
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#99E2E8F0"))
                    cornerRadius = dpToPx(2).toFloat()
                }
                background = bg
                alpha = 0.45f
            }
            val barParams = FrameLayout.LayoutParams(dp4, dp64).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            addView(handleBar, barParams)

            // 背景點綴微弱暗色保護區
            val containerBg = GradientDrawable().apply {
                setColor(Color.parseColor("#44000000"))
                cornerRadius = dpToPx(12).toFloat()
            }
            background = containerBg
            alpha = 0.65f

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        triggerHaptic()
                        expandCapsule()
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

        rootView?.addView(collapsedHandle, FrameLayout.LayoutParams(dp24, dp64).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        })
        rootView?.addView(expandedCapsule, rootParams)

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

            gravity = Gravity.BOTTOM or Gravity.END
            x = 0
            y = dpToPx(55)
            width = dp24
            height = dp64
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
            params.width = dpToPx(28)
            params.height = dpToPx(72)
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
        val handled = UyenConsoleAccessibilityService.performHome()
        if (!handled) {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private fun injectBackAction() {
        // 優先透過無障礙服務全局注入 BACK
        val handled = UyenConsoleAccessibilityService.performBack()
        if (!handled) {
            // 次要備援：透過 runtime input keyevent 4
            try {
                Runtime.getRuntime().exec("input keyevent 4")
            } catch (_: Exception) {
                // 若均不支援，直接跳回主頁保證絕不卡死
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
