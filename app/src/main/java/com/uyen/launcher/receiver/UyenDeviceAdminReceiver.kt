package com.uyen.launcher.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * UyenLauncher 掌機模式設備管理接收器 (Device Admin / Device Owner)
 * 用於啟用實體遊戲機等級的硬體完全鎖定 (LockTask Kiosk Mode)：
 * 1. 徹底關閉 SystemUI 狀態列下拉手勢監聽
 * 2. 遮蔽返回、Home、多工按鍵
 * 3. 確保除 UyenLauncher 與遊戲/模擬器外，任何人都無法跳出掌機模式
 */
class UyenDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "UyenDeviceAdmin"

        /**
         * 取得 UyenDeviceAdminReceiver 的組件名稱
         * 用於 ADB 執行:
         * adb shell dpm set-device-owner com.uyen.launcher/.receiver.UyenDeviceAdminReceiver
         */
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, UyenDeviceAdminReceiver::class.java)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "UyenLauncher 掌機設備管理員權限已啟動")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "UyenLauncher 掌機設備管理員權限已停用")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.i(TAG, "進入掌機獨佔模式 (LockTask Mode) - 封鎖狀態列與導航鍵: pkg=$pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.i(TAG, "退出掌機獨佔模式 (LockTask Mode)")
    }
}
