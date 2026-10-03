package com.uyen.launcher.core.hardware

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.Choreographer
import com.uyen.launcher.data.model.SystemStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 效能監控器 (Performance Monitor)
 * 負責實時擷取 FPS、記憶體使用、電池溫度與充放電狀態
 */
class PerformanceMonitor(private val context: Context) {

    private val _stats = MutableStateFlow(SystemStats())
    val stats: StateFlow<SystemStats> = _stats.asStateFlow()

    private var monitorJob: Job? = null
    private var fpsCounter = 0
    private var lastFpsTimestamp = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            fpsCounter++
            val now = System.currentTimeMillis()
            if (now - lastFpsTimestamp >= 1000) {
                val currentFps = fpsCounter
                fpsCounter = 0
                lastFpsTimestamp = now
                _stats.value = _stats.value.copy(fps = currentFps)
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun startMonitoring(scope: CoroutineScope) {
        lastFpsTimestamp = System.currentTimeMillis()
        Choreographer.getInstance().postFrameCallback(frameCallback)

        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                updateSystemStats()
                delay(2000) // 每 2 秒採樣一次電池與記憶體，避免耗電
            }
        }
    }

    fun stopMonitoring() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        monitorJob?.cancel()
        monitorJob = null
    }

    fun updateSystemStats() {
        // 記憶體資訊
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val usedMb = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024)

        // 電池與溫度
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 90) ?: 90
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = (level * 100 / scale.coerceAtLeast(1)).coerceIn(0, 100)
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320
        val tempCelsius = tempRaw / 10.0f
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        _stats.value = _stats.value.copy(
            ramUsedMb = usedMb,
            ramTotalMb = totalMb,
            batteryPercent = batteryPct,
            batteryTempCelsius = tempCelsius,
            isCharging = isCharging
        )
    }
}
