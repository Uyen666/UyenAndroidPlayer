package com.uyen.launcher.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uyen.launcher.core.hardware.PerformanceMonitor
import com.uyen.launcher.core.util.SoundManager
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.data.model.PlayerProfile
import com.uyen.launcher.data.model.SystemStats
import com.uyen.launcher.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 主畫面核心 ViewModel
 * 統一管理 PS5 輪播狀態、Steam OS 遊戲庫、效能 HUD 與手柄模式
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val gameRepository = GameRepository(application)
    private val performanceMonitor = PerformanceMonitor(application)
    private val soundManager = SoundManager(application)

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

    private val _isGamepadOverlayActive = MutableStateFlow(false)
    val isGamepadOverlayActive: StateFlow<Boolean> = _isGamepadOverlayActive.asStateFlow()

    private val _isFullScreenControllerMode = MutableStateFlow(false)
    val isFullScreenControllerMode: StateFlow<Boolean> = _isFullScreenControllerMode.asStateFlow()

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

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun setLibraryOpen(open: Boolean) {
        _isLibraryOpen.value = open
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
