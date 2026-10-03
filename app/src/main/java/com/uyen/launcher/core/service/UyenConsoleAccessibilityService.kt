package com.uyen.launcher.core.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * 掌機全局無障礙服務 (UyenConsoleAccessibilityService)
 * 專為解決在其他遊戲/應用內被鎖定無法返回的問題
 * 提供原生系統級的 GLOBAL_ACTION_BACK 與 GLOBAL_ACTION_HOME
 */
class UyenConsoleAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "UyenAccessibility"
        var instance: UyenConsoleAccessibilityService? = null
            private set

        /**
         * 在任何前台遊戲或 App 中注入全局返回操作
         */
        fun performBack(): Boolean {
            return try {
                instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform back: ${e.message}")
                false
            }
        }

        /**
         * 全局返回主頁
         */
        fun performHome(): Boolean {
            return try {
                instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform home: ${e.message}")
                false
            }
        }

        /**
         * 全局呼出多工列表
         */
        fun performRecents(): Boolean {
            return try {
                instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform recents: ${e.message}")
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "UyenConsoleAccessibilityService connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要監聽無關事件，保持最低功耗
    }

    override fun onInterrupt() {
        Log.w(TAG, "UyenConsoleAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "UyenConsoleAccessibilityService destroyed")
    }
}
