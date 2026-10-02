package com.uyen.launcher.core.util

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 掌機全螢幕沉浸工具
 * 專為 Android 掌機體驗打造：
 * 1. 消除前置鏡頭水滴屏黑邊 (LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
 * 2. 全邊緣手勢排除區 (SystemGestureExclusionRects: 頂部、左右、底部)，抑制系統返回手勢箭頭、黑色手勢條與通知下拉
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

            // 4. 註冊全邊緣手勢排除區 (Android 10+)，告訴系統「這些邊緣的觸控全歸 UyenLauncher 獨佔」
            updateGestureExclusions(decorView)

        } catch (_: Exception) {
            // 安全容錯，防止特定客製化 ROM 初始化異常
        }
    }

    /**
     * 動態更新 Android 10+ 全邊緣手勢排除矩形區 (左右兩側、頂部與底部)
     */
    fun updateGestureExclusions(decorView: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val applyExclusions = {
            val w = decorView.width
            val h = decorView.height
            if (w > 0 && h > 0) {
                // 排除兩側邊緣手勢感應區 (各 140px 寬度)
                val leftRect = Rect(0, 0, 140, h)
                val rightRect = Rect(w - 140, 0, w, h)
                // 排除頂部邊緣 (防止下拉通知欄手勢干擾，高度 100px)
                val topRect = Rect(0, 0, w, 100)
                // 排除底部邊緣 (高度 80px)
                val bottomRect = Rect(0, h - 80, w, h)

                try {
                    decorView.systemGestureExclusionRects = listOf(
                        leftRect,
                        rightRect,
                        topRect,
                        bottomRect
                    )
                } catch (_: Exception) {}
            }
        }

        decorView.post(applyExclusions)
        // 綁定佈局變化監聽，保證螢幕轉向或視窗重繪時持續生效
        decorView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applyExclusions()
        }
    }
}
