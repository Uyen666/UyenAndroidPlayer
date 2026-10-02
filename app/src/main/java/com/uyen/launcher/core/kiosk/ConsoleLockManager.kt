package com.uyen.launcher.core.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.uyen.launcher.receiver.UyenDeviceAdminReceiver

/**
 * 掌機硬體鎖定管理器 (Console Lock & Kiosk Mode)
 * 提供實體遊戲機等級的鎖死：
 * 1. 廢除狀態列下拉 (SystemUI 下拉手勢完全停用)
 * 2. 封鎖系統返回、Home 鍵與多工切換鍵 (HARDWARE LEVEL)
 * 3. 允許 UyenLauncher 與已啟動之遊戲/模擬器執行，防止跳出至手機系統桌面
 */
object ConsoleLockManager {

    private const val TAG = "ConsoleLockManager"

    /**
     * 檢查當前應用是否已取得 Device Owner (設備擁有者) 最高管理特權
     */
    fun isDeviceOwner(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.isDeviceOwnerApp(context.packageName) == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device owner: ${e.message}")
            false
        }
    }

    /**
     * 檢查是否允許直接無彈窗鎖定任務
     */
    fun isLockTaskPermitted(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.isLockTaskPermitted(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 檢查當前是否正處於 LockTask 鎖定模式
     */
    fun isLockTaskActive(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 啟用掌機級完全硬體鎖定 (Console Lock / Kiosk)
     * 1. 設置白名單包含本應用與所有已安裝遊戲/模擬器
     * 2. 設置 LOCK_TASK_FEATURE_NONE (完全禁用狀態列、無下拉、無導航條)
     * 3. 呼叫 startLockTask()
     */
    fun enableConsoleLock(activity: Activity): Boolean {
        val context = activity.applicationContext
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val adminComponent = UyenDeviceAdminReceiver.getComponentName(context)

        return try {
            if (isDeviceOwner(context)) {
                // 白名單：納入 UyenLauncher 本身以及所有已安裝應用程式，讓遊戲與模擬器能順利運行
                val installedPackages = context.packageManager.getInstalledApplications(0)
                    .map { it.packageName }
                    .toMutableList()
                if (!installedPackages.contains(context.packageName)) {
                    installedPackages.add(context.packageName)
                }

                dpm.setLockTaskPackages(adminComponent, installedPackages.toTypedArray())

                // 核心關鍵 1：DevicePolicyManager.setStatusBarDisabled(true)
                // 彻底拔除狀態列視窗，封鎖通知、快捷開關與所有狀態列浮層/半透明拉條！
                try {
                    dpm.setStatusBarDisabled(adminComponent, true)
                } catch (e: Exception) {
                    Log.w(TAG, "setStatusBarDisabled error: ${e.message}")
                }

                // 核心關鍵 2：將 LockTask 特性設為 NONE
                // 效果：底層徹底拔除狀態列下拉手勢、遮蔽實體與虛擬 Home / Recents 鍵
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.setLockTaskFeatures(
                        adminComponent,
                        DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                    )
                }
            }

            // 啟動鎖定
            activity.startLockTask()
            Log.i(TAG, "Console lock task started successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start console lock: ${e.message}", e)
            false
        }
    }

    /**
     * 解除掌機鎖定模式
     */
    fun disableConsoleLock(activity: Activity): Boolean {
        val context = activity.applicationContext
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = UyenDeviceAdminReceiver.getComponentName(context)

        return try {
            if (isDeviceOwner(context) && dpm != null) {
                try {
                    dpm.setStatusBarDisabled(adminComponent, false)
                } catch (_: Exception) {}
            }
            activity.stopLockTask()
            Log.i(TAG, "Console lock task stopped successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop console lock: ${e.message}", e)
            false
        }
    }

    /**
     * 取得設定 Device Owner 的 ADB 終端機指令
     */
    fun getDeviceOwnerAdbCommand(): String {
        return "adb shell dpm set-device-owner com.uyen.launcher/.receiver.UyenDeviceAdminReceiver"
    }

    /**
     * 取得免 Device Owner 的狀態列極限禁用 ADB 指令 (備用快捷方案)
     */
    fun getStatusBarDisableAdbCommand(): String {
        return "adb shell cmd statusbar send-disable-flag statusbar-expansion notification-peek home recents notification-icons"
    }
}
