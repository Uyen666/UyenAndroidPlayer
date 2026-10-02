package com.uyen.launcher.presentation.home

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uyen.launcher.core.hardware.MemoryCleaner
import com.uyen.launcher.core.hardware.PerformanceMonitor
import com.uyen.launcher.core.util.SoundManager
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.data.model.PlayerProfile
import com.uyen.launcher.data.model.SystemStats
import com.uyen.launcher.data.repository.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 主畫面核心 ViewModel
 * 統一管理 PS5 輪播狀態、Steam OS 遊戲庫、效能 HUD、多工管理器與記憶體加速
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val gameRepository = GameRepository(application)
    private val performanceMonitor = PerformanceMonitor(application)
    private val soundManager = SoundManager(application)
    private val memoryCleaner = MemoryCleaner(application)

    val games: StateFlow<List<GameItem>> = gameRepository.games

    val systemStats: StateFlow<SystemStats> = performanceMonitor.stats
        .stateIn(viewModelScope, SharingStarted.Eagerly, SystemStats())

    private val _selectedGameIndex = MutableStateFlow(0)
    val selectedGameIndex: StateFlow<Int> = _selectedGameIndex.asStateFlow()

    private val _playerProfile = MutableStateFlow(PlayerProfile())
    val playerProfile: StateFlow<PlayerProfile> = _playerProfile.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isLibraryOpen = MutableStateFlow(false)
    val isLibraryOpen: StateFlow<Boolean> = _isLibraryOpen.asStateFlow()

    private val _isTaskSwitcherOpen = MutableStateFlow(false)
    val isTaskSwitcherOpen: StateFlow<Boolean> = _isTaskSwitcherOpen.asStateFlow()

    private val _isGamepadOverlayActive = MutableStateFlow(false)
    val isGamepadOverlayActive: StateFlow<Boolean> = _isGamepadOverlayActive.asStateFlow()

    private val _isFullScreenControllerMode = MutableStateFlow(false)
    val isFullScreenControllerMode: StateFlow<Boolean> = _isFullScreenControllerMode.asStateFlow()

    private val _boostMessage = MutableStateFlow<String?>(null)
    val boostMessage: StateFlow<String?> = _boostMessage.asStateFlow()

    init {
        performanceMonitor.startMonitoring(viewModelScope)
        viewModelScope.launch {
            gameRepository.scanInstalledApps()
        }
    }

    fun selectGame(index: Int) {
        if (_selectedGameIndex.value != index) {
            _selectedGameIndex.value = index
            viewModelScope.launch {
                soundManager.playCardFocusSound()
            }
        }
    }

    fun launchGame(item: GameItem) {
        viewModelScope.launch {
            soundManager.playConfirmSound()
        }
        if (item.id == "controller_mode") {
            _isFullScreenControllerMode.value = true
        } else {
            gameRepository.launchGame(item)
        }
    }

    /**
     * 掌機主頁鍵 (Home)：重置選中焦點，關閉所有彈窗與抽屜
     */
    fun handleHome() {
        viewModelScope.launch {
            soundManager.playConfirmSound()
        }
        _isSettingsOpen.value = false
        _isLibraryOpen.value = false
        _isTaskSwitcherOpen.value = false
        _isFullScreenControllerMode.value = false
        _selectedGameIndex.value = 0
    }

    /**
     * 掌機返回鍵 (Back)：情境式關閉上層視窗
     */
    fun handleBack() {
        viewModelScope.launch {
            soundManager.playCardFocusSound()
        }
        when {
            _isFullScreenControllerMode.value -> _isFullScreenControllerMode.value = false
            _isTaskSwitcherOpen.value -> _isTaskSwitcherOpen.value = false
            _isLibraryOpen.value -> _isLibraryOpen.value = false
            _isSettingsOpen.value -> _isSettingsOpen.value = false
            _selectedGameIndex.value != 0 -> _selectedGameIndex.value = 0
        }
    }

    /**
     * 電競級一鍵清理背景與釋放記憶體
     */
    fun cleanMemory() {
        viewModelScope.launch {
            soundManager.playConfirmSound()
            val freedMb = memoryCleaner.cleanMemory()
            _boostMessage.value = "⚡ 電競加速完成！已釋放約 ${freedMb} MB RAM"
            delay(3000)
            _boostMessage.value = null
        }
    }

    /**
     * 關閉指定背景遊戲
     */
    fun killGame(item: GameItem) {
        item.packageName?.let { pkg ->
            val actManager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            actManager?.killBackgroundProcesses(pkg)
            viewModelScope.launch {
                _boostMessage.value = "已關閉 ${item.title}"
                delay(2000)
                _boostMessage.value = null
            }
        }
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun setLibraryOpen(open: Boolean) {
        _isLibraryOpen.value = open
    }

    fun setTaskSwitcherOpen(open: Boolean) {
        _isTaskSwitcherOpen.value = open
    }

    fun setGamepadOverlayActive(active: Boolean) {
        _isGamepadOverlayActive.value = active
    }

    fun setFullScreenControllerMode(active: Boolean) {
        _isFullScreenControllerMode.value = active
    }

    override fun onCleared() {
        super.onCleared()
        performanceMonitor.stopMonitoring()
        soundManager.release()
    }
}
