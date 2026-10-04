package com.uyen.launcher.core.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
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
     * 完整收集設備上所有已安裝應用的套件名稱 (包含系統應用、Google Play 應用、多用戶 profile)
     */
    fun getAllInstalledPackageNames(context: Context): List<String> {
        val result = mutableSetOf<String>()
        result.add(context.packageName)

        val pm = context.packageManager
        runCatching {
            val apps = pm.getInstalledApplications(PackageManager.MATCH_ALL)
            result.addAll(apps.map { it.packageName })
        }
        runCatching {
            val pkgs = pm.getInstalledPackages(PackageManager.MATCH_ALL)
            result.addAll(pkgs.map { it.packageName })
        }
        runCatching {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            if (launcherApps != null && userManager != null) {
                for (profile in userManager.userProfiles) {
                    val list = launcherApps.getActivityList(null, profile)
                    result.addAll(list.map { it.applicationInfo.packageName })
                }
            }
        }
        runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            result.addAll(activities.mapNotNull { it.activityInfo?.packageName })
        }
        return result.toList()
    }

    /**
     * 動態全面刷新並同步 DevicePolicyManager LockTask 白名單：
     * 將 UyenLauncher、所有已安裝遊戲/模擬器/串流客戶端以及系統工具全部納入白名單
     */
    fun refreshLockTaskPackages(context: Context): Boolean {
        if (!isDeviceOwner(context)) return false
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
            val adminComponent = UyenDeviceAdminReceiver.getComponentName(context)
            val allPackages = getAllInstalledPackageNames(context).toMutableSet()
            allPackages.add(context.packageName)

            dpm.getLockTaskPackages(adminComponent)?.let { current ->
                allPackages.addAll(current)
            }

            dpm.setLockTaskPackages(adminComponent, allPackages.toTypedArray())
            Log.i(TAG, "已成功動態全面更新 LockTask 白名單，總計收錄: ${allPackages.size} 個套件")
            true
        } catch (e: Exception) {
            Log.e(TAG, "動態刷新 LockTask 白名單失敗: ${e.message}", e)
            false
        }
    }

    /**
     * 動態將目標應用程式納入 DevicePolicyManager LockTask 白名單，防止系統回傳 Error 101 攔截喚起
     */
    fun ensurePackageWhitelisted(context: Context, packageName: String): Boolean {
        if (!isDeviceOwner(context) || packageName.isBlank()) return false
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
            val adminComponent = UyenDeviceAdminReceiver.getComponentName(context)
            val currentPackages = dpm.getLockTaskPackages(adminComponent)?.toMutableSet() ?: mutableSetOf()
            if (!currentPackages.contains(packageName)) {
                currentPackages.add(packageName)
                currentPackages.add(context.packageName)
                dpm.setLockTaskPackages(adminComponent, currentPackages.toTypedArray())
                Log.i(TAG, "已成功動態將 $packageName 納入 LockTask 白名單")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "動態更新 LockTask 白名單失敗 ($packageName): ${e.message}")
            false
        }
    }

    /**
     * 啟用掌機級完全硬體鎖定 (Console Lock / Kiosk)
     * 1. 設置完整白名單包含本應用與所有已安裝遊戲/串流/模擬器
     * 2. 設置 LOCK_TASK_FEATURE (依掌機體驗配置)
     * 3. 呼叫 startLockTask()
     */
    fun enableConsoleLock(activity: Activity): Boolean {
        val context = activity.applicationContext
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val adminComponent = UyenDeviceAdminReceiver.getComponentName(context)

        return try {
            if (isDeviceOwner(context)) {
                // 白名單：全盤納入 UyenLauncher 本身以及所有已安裝應用與遊戲，保證各類 App 能順暢拉起
                refreshLockTaskPackages(context)

                // 核心關鍵 1：DevicePolicyManager.setStatusBarDisabled(true)
                // 彻底拔除狀態列視窗，封鎖通知、快捷開關與所有狀態列浮層/半透明拉條！
                try {
                    dpm.setStatusBarDisabled(adminComponent, true)
                } catch (e: Exception) {
                    Log.w(TAG, "setStatusBarDisabled error: ${e.message}")
                }

                // 核心關鍵 2：LockTask 特性設置
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
