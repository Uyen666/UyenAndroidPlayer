package com.uyen.launcher.core.util

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 掌機全螢幕沉浸工具
 * 專為 Android 掌機體驗打造：
 * 1. 消除前置鏡頭水滴屏黑邊 (LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
 * 2. 邊緣手勢排除 (setSystemGestureExclusionRects)，抑制系統返回手勢箭頭與手勢條
 * 3. 系統列透明化與完全隱藏 (BEHAVIOR_DEFAULT)
 */
object SystemBarUtil {

    fun hideSystemBars(activity: Activity) {
        try {
            val window = activity.window ?: return

            // 1. 允許內容延伸至挖孔/水滴屏區域，徹底消除兩側黑邊 (Letterbox)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
            }

            // 2. 視窗邊界完全鋪滿 (Edge-to-Edge)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val decorView = window.peekDecorView() ?: window.decorView ?: return
            val controller = WindowCompat.getInsetsController(window, decorView)

            // 3. 隱藏狀態欄與導航欄
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

            // 4. 註冊系統手勢排除區 (Android 10+)，防止左右邊緣滑動彈出系統返回箭頭與遮罩
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                decorView.post {
                    val w = decorView.width
                    val h = decorView.height
                    if (w > 0 && h > 0) {
                        // 排除兩側邊緣手勢感應區
                        val leftRect = Rect(0, 0, 120, h)
                        val rightRect = Rect(w - 120, 0, w, h)
                        try {
                            decorView.systemGestureExclusionRects = listOf(leftRect, rightRect)
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {
            // 安全容錯，防止特定 ROM 初始化異常
        }
    }
}
