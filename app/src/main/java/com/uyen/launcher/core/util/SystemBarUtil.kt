package com.uyen.launcher.core.util

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 掌機全螢幕沉浸工具
 * 使用 AndroidX WindowCompat 安全控制系統列，適配 Android 8 ~ Android 14+
 */
object SystemBarUtil {
    fun hideSystemBars(activity: Activity) {
        try {
            val window = activity.window ?: return
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val decorView = window.peekDecorView() ?: window.decorView ?: return
            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (_: Exception) {
            // 安全容錯，防止特殊客製化 ROM 在 View 尚未附加前拋出異常
        }
    }
}
