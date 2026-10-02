package com.uyen.launcher.core.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 掌機操作音效管理器
 * 模擬 PS5 / Steam OS 游標切換與選取之音效反饋
 */
class SoundManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 60)
        } catch (_: Exception) {
            // Audio policy fallback
        }
    }

    suspend fun playCardFocusSound() = withContext(Dispatchers.Default) {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
        } catch (_: Exception) {}
    }

    suspend fun playConfirmSound() = withContext(Dispatchers.Default) {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
        } catch (_: Exception) {}
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
