package com.uyen.launcher.core.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 掌機應用安裝/卸載/更新實時監控器 (PackageChangeMonitor)
 * 結合 Android 官方 LauncherApps.Callback 與系統廣播接收器，
 * 當 Google Play 或系統完成 App 安裝、卸載或更新時，即時發出回調通知，
 * 實現無需手動刷新即可瞬間於掌機收藏庫與首頁看見新下載之遊戲與應用。
 */
class PackageChangeMonitor(
    private val context: Context,
    private val onPackageChanged: () -> Unit
) {
    private var launcherApps: LauncherApps? = null
    private var launcherCallback: LauncherApps.Callback? = null
    private var packageReceiver: BroadcastReceiver? = null
    private var isRegistered = false

    private var debounceJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 啟動實時套件變更監聽 (LauncherApps API + BroadcastReceiver 雙重防護)
     */
    fun register(scope: CoroutineScope) {
        if (isRegistered) return
        isRegistered = true

        val triggerChange = {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                com.uyen.launcher.core.kiosk.ConsoleLockManager.refreshLockTaskPackages(context)
                delay(300) // 防抖動：防止 Google Play 分包安裝或多重廣播同時觸發頻繁掃描
                onPackageChanged()
                delay(800) // 二次保證，確保 Google Play 分包/標籤完全寫入 PackageManager
                com.uyen.launcher.core.kiosk.ConsoleLockManager.refreshLockTaskPackages(context)
                onPackageChanged()
            }
        }

        // 1. Android 原生 LauncherApps API (專為桌面啟動器設計的最高優先級監控)
        try {
            launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            launcherCallback = object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) {
                    com.uyen.launcher.core.kiosk.ConsoleLockManager.ensurePackageWhitelisted(context, packageName)
                    triggerChange()
                }

                override fun onPackageRemoved(packageName: String, user: UserHandle) {
                    triggerChange()
                }

                override fun onPackageChanged(packageName: String, user: UserHandle) {
                    com.uyen.launcher.core.kiosk.ConsoleLockManager.ensurePackageWhitelisted(context, packageName)
                    triggerChange()
                }

                override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                    packageNames.forEach { com.uyen.launcher.core.kiosk.ConsoleLockManager.ensurePackageWhitelisted(context, it) }
                    triggerChange()
                }

                override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                    triggerChange()
                }
            }
            launcherApps?.registerCallback(launcherCallback!!, mainHandler)
        } catch (_: Exception) {}

        // 2. 系統 BroadcastReceiver 雙重保險 (相容側載 APK、第三方商店與客製化 ROM)
        try {
            packageReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val pkgName = intent?.data?.schemeSpecificPart
                    if (pkgName != null && context != null) {
                        com.uyen.launcher.core.kiosk.ConsoleLockManager.ensurePackageWhitelisted(context, pkgName)
                    }
                    when (intent?.action) {
                        Intent.ACTION_PACKAGE_ADDED,
                        Intent.ACTION_PACKAGE_REMOVED,
                        Intent.ACTION_PACKAGE_REPLACED -> {
                            triggerChange()
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            ContextCompat.registerReceiver(
                context,
                packageReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {}
    }

    /**
     * 取消監聽並清理所有系統回調與協程 Job
     */
    fun unregister() {
        if (!isRegistered) return
        isRegistered = false
        debounceJob?.cancel()
        debounceJob = null

        launcherCallback?.let { callback ->
            try {
                launcherApps?.unregisterCallback(callback)
            } catch (_: Exception) {}
        }
        launcherCallback = null
        launcherApps = null

        packageReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
        packageReceiver = null
    }

    fun isRegistered(): Boolean = isRegistered

    companion object {
        const val ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED"
        const val ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED"
        const val ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED"

        fun isPackageAction(action: String?): Boolean = when (action) {
            ACTION_PACKAGE_ADDED,
            ACTION_PACKAGE_REMOVED,
            ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> true
            else -> false
        }
    }
}
