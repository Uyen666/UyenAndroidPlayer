package com.uyen.launcher.core.hardware

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 掌機系統硬體控制管理器
 * 負責：音量、視窗亮度、Wi-Fi/藍牙跳轉、掌機遊戲專注模式 (通知靜音)
 */
class SystemControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _mediaVolume = MutableStateFlow(getCurrentVolumeRatio())
    val mediaVolume: StateFlow<Float> = _mediaVolume.asStateFlow()

    private val _screenBrightness = MutableStateFlow(0.7f)
    val screenBrightness: StateFlow<Float> = _screenBrightness.asStateFlow()

    private val _isFocusDndMode = MutableStateFlow(false)
    val isFocusDndMode: StateFlow<Boolean> = _isFocusDndMode.asStateFlow()

    private var previousNotificationVolume: Int = -1
    private var previousRingVolume: Int = -1

    private fun getCurrentVolumeRatio(): Float {
        val am = audioManager ?: return 0.5f
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * 設定媒體音樂音量 (0.0f ~ 1.0f)
     */
    fun setMediaVolume(ratio: Float) {
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (ratio.coerceIn(0f, 1f) * max).toInt()
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            _mediaVolume.value = ratio
        } catch (_: Exception) {}
    }

    /**
     * 調節當前視窗螢幕亮度 (0.05f ~ 1.0f)
     */
    fun setBrightness(activity: Activity?, ratio: Float) {
        val safeRatio = ratio.coerceIn(0.05f, 1.0f)
        _screenBrightness.value = safeRatio
        activity?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = safeRatio
            window.attributes = layoutParams
        }
    }

    /**
     * 掌機遊戲專注模式 (Game Focus / DND Mode)
     * 解決「後台常有通知聲但沉浸模式看不到」之痛點：
     * 開啟時將系統通知音 (STREAM_NOTIFICATION) 與鈴聲 (STREAM_RING) 靜音，保留純淨遊戲音樂。
     */
    fun setFocusDndMode(enabled: Boolean) {
        val am = audioManager ?: return
        _isFocusDndMode.value = enabled
        try {
            if (enabled) {
                previousNotificationVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                previousRingVolume = am.getStreamVolume(AudioManager.STREAM_RING)
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            } else {
                if (previousNotificationVolume >= 0) {
                    am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, previousNotificationVolume, 0)
                }
                if (previousRingVolume >= 0) {
                    am.setStreamVolume(AudioManager.STREAM_RING, previousRingVolume, 0)
                }
            }
        } catch (_: Exception) {}
    }

    fun openWifiSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openBluetoothSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }
}
