package com.uyen.launcher.presentation.home

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uyen.launcher.core.account.GoogleAccountManager
import com.uyen.launcher.core.banner.BannerSourceType
import com.uyen.launcher.core.banner.GameBannerManager
import com.uyen.launcher.core.banner.ResolvedBanner
import com.uyen.launcher.core.hardware.PerformanceMonitor
import com.uyen.launcher.core.hardware.SystemControlManager
import com.uyen.launcher.core.hardware.SystemMemoryManager
import com.uyen.launcher.core.kiosk.ConsoleLockManager
import com.uyen.launcher.core.service.GlobalConsoleEdgeService
import com.uyen.launcher.core.util.SoundManager
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.data.model.GoogleAccount
import com.uyen.launcher.data.model.MainNavTab
import com.uyen.launcher.data.model.PlayerProfile
import com.uyen.launcher.data.model.RunningTask
import com.uyen.launcher.data.model.SystemStats
import com.uyen.launcher.data.repository.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 主畫面核心 ViewModel
 * 管理首頁狀態、遊戲庫、本機紀錄、效能 HUD 與系統控制台。
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val gameRepository = GameRepository(application)
    private val performanceMonitor = PerformanceMonitor(application)
    private val soundManager = SoundManager(application)
    private val systemControlManager = SystemControlManager(application)
    private val gameBannerManager = GameBannerManager(application)
    private val prefs = application.getSharedPreferences("uyen_launcher_ui_prefs", Context.MODE_PRIVATE)
    private var activePlaySessionId: String? = null
    private var activePlaySessionStartedAt: Long = 0L

    val games: StateFlow<List<GameItem>> = gameRepository.games

    // 已解析的各遊戲大海報快取 (gameId -> ResolvedBanner)
    private val _resolvedBanners = MutableStateFlow<Map<String, ResolvedBanner>>(emptyMap())
    val resolvedBanners: StateFlow<Map<String, ResolvedBanner>> = _resolvedBanners.asStateFlow()

    // 遊戲動作與海報客製化彈窗狀態
    private val _activeActionMenuGame = MutableStateFlow<GameItem?>(null)
    val activeActionMenuGame: StateFlow<GameItem?> = _activeActionMenuGame.asStateFlow()

    // 頂部導航分頁切換 (首頁 / 串流 / 遊戲)
    private val _selectedTab = MutableStateFlow(MainNavTab.HOME)
    val selectedTab: StateFlow<MainNavTab> = _selectedTab.asStateFlow()

    companion object {
        private const val PREF_KEY_HOME_PINNED = "home_pinned_games_ids_v2"
        private const val KEY_ACTIVE_PLAY_ID = "active_play_session_id"
        private const val KEY_ACTIVE_PLAY_STARTED_AT = "active_play_session_started_at"
        val DEFAULT_PINNED_IDS = listOf("controller_mode", "retro_8bit")
        private const val PREF_KEY_GAMES_TREE = "games_folder_tree_uri"
    }

    // 首頁自定義釘選卡片清單 (預設精選 5 張主機/串流/模擬器卡片)
    private val _pinnedGameIds = MutableStateFlow<List<String>>(loadPinnedGameIds())
    val pinnedGameIds: StateFlow<List<String>> = _pinnedGameIds.asStateFlow()

    // 首頁卡片管理/添加彈窗狀態
    private val _isCardManagerOpen = MutableStateFlow(false)
    val isCardManagerOpen: StateFlow<Boolean> = _isCardManagerOpen.asStateFlow()

    fun setCardManagerOpen(open: Boolean) {
        _isCardManagerOpen.value = open
    }

    private fun loadPinnedGameIds(): List<String> {
        val raw = prefs.getString(PREF_KEY_HOME_PINNED, null)
        if (raw.isNullOrBlank()) {
            return DEFAULT_PINNED_IDS
        }
        return try {
            val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (list.isNotEmpty()) list else DEFAULT_PINNED_IDS
        } catch (_: Exception) {
            DEFAULT_PINNED_IDS
        }
    }

    private fun savePinnedGameIds(ids: List<String>) {
        prefs.edit().putString(PREF_KEY_HOME_PINNED, ids.joinToString(",")).apply()
        _pinnedGameIds.value = ids
    }

    fun pinGameToHome(gameId: String) {
        val current = _pinnedGameIds.value.toMutableList()
        if (!current.contains(gameId)) {
            current.add(gameId)
            savePinnedGameIds(current)
            boostPerformance("已添加卡片至首頁")
        }
    }

    fun unpinGameFromHome(gameId: String) {
        val current = _pinnedGameIds.value.toMutableList()
        if (current.remove(gameId)) {
            savePinnedGameIds(current)
            boostPerformance("已從首頁移除卡片")
        }
    }

    fun togglePinGame(gameId: String) {
        if (_pinnedGameIds.value.contains(gameId)) {
            unpinGameFromHome(gameId)
        } else {
            pinGameToHome(gameId)
        }
    }

    fun isGamePinned(gameId: String): Boolean {
        return _pinnedGameIds.value.contains(gameId)
    }

    fun toggleFavorite(gameId: String) {
        gameRepository.toggleFavorite(gameId)
        _activeActionMenuGame.value = games.value.firstOrNull { it.id == gameId }
    }

    // 依據目前選取的分頁標籤動態過濾遊戲清單 (首頁預設只展示 5 張玩家自定義卡片)
    val currentTabGames: StateFlow<List<GameItem>> = combine(games, _selectedTab, _pinnedGameIds) { allGames, tab, pinnedIds ->
        when (tab) {
            MainNavTab.HOME -> {
                val gameMap = allGames.associateBy { it.id }
                val pinnedList = pinnedIds.mapNotNull { gameMap[it] }
                if (pinnedList.isNotEmpty()) pinnedList else allGames.take(5)
            }
            MainNavTab.STREAMING -> allGames.filter {
                it.category == GameCategory.STREAMING || it.id == "controller_mode"
            }
            MainNavTab.GAMES -> allGames.filter {
                it.category == GameCategory.GALGAME ||
                it.category == GameCategory.RETRO ||
                it.category == GameCategory.CUSTOM
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 真實 Google 帳號狀態與可用帳號列表
    private val _googleAccount = MutableStateFlow(GoogleAccountManager.getActiveGoogleAccount(application))
    val googleAccount: StateFlow<GoogleAccount> = _googleAccount.asStateFlow()

    private val _availableGoogleAccounts = MutableStateFlow(GoogleAccountManager.getGoogleAccounts(application))
    val availableGoogleAccounts: StateFlow<List<GoogleAccount>> = _availableGoogleAccounts.asStateFlow()

    private val _isAccountDialogOpen = MutableStateFlow(false)
    val isAccountDialogOpen: StateFlow<Boolean> = _isAccountDialogOpen.asStateFlow()

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

    // 最近由啟動器開啟的項目；Android 不提供一般 App 可靠的全域前台任務清單。
    private val _runningTasks = MutableStateFlow<List<RunningTask>>(emptyList())
    val runningTasks: StateFlow<List<RunningTask>> = _runningTasks.asStateFlow()

    // 掌機沉浸鎖定模式 (Kiosk Mode，關閉系統邊緣手勢與狀態欄下滑)
    private val _isKioskModeEnabled = MutableStateFlow(ConsoleLockManager.isLockTaskActive(application))
    val isKioskModeEnabled: StateFlow<Boolean> = _isKioskModeEnabled.asStateFlow()

    // 是否取得 Device Owner 掌機最高管理特權
    private val _isDeviceOwner = MutableStateFlow(ConsoleLockManager.isDeviceOwner(application))
    val isDeviceOwner: StateFlow<Boolean> = _isDeviceOwner.asStateFlow()

    // 內建 8-bit 太空突擊懷舊街機狀態
    private val _isRetroArcadeOpen = MutableStateFlow(false)
    val isRetroArcadeOpen: StateFlow<Boolean> = _isRetroArcadeOpen.asStateFlow()

    // 掌機模擬器與遊戲引擎導航指南彈窗
    private val _assistantDialogItem = MutableStateFlow<GameItem?>(null)
    val assistantDialogItem: StateFlow<GameItem?> = _assistantDialogItem.asStateFlow()

    // 是否常態顯示左上角效能 HUD (FPS/溫度)
    private val _showPerformanceHud = MutableStateFlow(prefs.getBoolean("show_hud", true))
    val showPerformanceHud: StateFlow<Boolean> = _showPerformanceHud.asStateFlow()

    val mediaVolume: StateFlow<Float> = systemControlManager.mediaVolume
    val screenBrightness: StateFlow<Float> = systemControlManager.screenBrightness
    val isFocusDndMode: StateFlow<Boolean> = systemControlManager.isFocusDndMode

    private val _boostMessage = MutableStateFlow<String?>(null)
    val boostMessage: StateFlow<String?> = _boostMessage.asStateFlow()

    val soundManagerInstance: SoundManager get() = soundManager

    init {
        performanceMonitor.startMonitoring(viewModelScope)
        GlobalConsoleEdgeService.start(application)
        refreshGoogleAccounts()
        gameRepository.startMonitoring(viewModelScope)
        viewModelScope.launch {
            gameRepository.scanInstalledApps()
            gameRepository.scanLocalRomFiles(savedGamesFolderUri())
        }
        // 背景預先解析遊戲大海報 (優先解析首頁釘選與可見卡片)
        viewModelScope.launch {
            games.collect { allGames ->
                allGames.forEach { game ->
                    if (!_resolvedBanners.value.containsKey(game.id)) {
                        val banner = gameBannerManager.resolveBanner(game)
                        if (banner != null) {
                            _resolvedBanners.value = _resolvedBanners.value + (game.id to banner)
                        }
                    }
                }
            }
        }
    }

    fun selectTab(tab: MainNavTab) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            _selectedGameIndex.value = 0
            viewModelScope.launch {
                soundManager.playClick()
            }
        }
    }

    fun nextTab() {
        val tabs = MainNavTab.entries
        val nextIdx = (tabs.indexOf(_selectedTab.value) + 1) % tabs.size
        selectTab(tabs[nextIdx])
    }

    fun prevTab() {
        val tabs = MainNavTab.entries
        val prevIdx = if (tabs.indexOf(_selectedTab.value) - 1 < 0) tabs.size - 1 else tabs.indexOf(_selectedTab.value) - 1
        selectTab(tabs[prevIdx])
    }

    fun refreshGoogleAccounts() {
        val app = getApplication<Application>()
        val accounts = GoogleAccountManager.getGoogleAccounts(app)
        val active = GoogleAccountManager.getActiveGoogleAccount(app)
        _availableGoogleAccounts.value = accounts
        _googleAccount.value = active
        _playerProfile.value = _playerProfile.value.copy(
            username = if (active.isConnected) active.displayName else "Uyen",
            avatarUrl = active.avatarUrl
        )
    }

    fun switchGoogleAccount(account: GoogleAccount) {
        val app = getApplication<Application>()
        GoogleAccountManager.saveActiveGoogleAccount(app, account.email)
        _googleAccount.value = account
        _playerProfile.value = _playerProfile.value.copy(
            username = account.displayName,
            avatarUrl = account.avatarUrl
        )
        viewModelScope.launch {
            soundManager.playClick()
            boostPerformance("已切換帳號：${account.email}")
        }
    }

    fun openManageSystemAccounts() {
        GoogleAccountManager.openManageAccountSettings(getApplication())
    }

    fun openAddGoogleAccount() {
        GoogleAccountManager.openAddGoogleAccount(getApplication())
    }

    fun getChooseAccountIntent(): Intent {
        return GoogleAccountManager.getChooseAccountIntent()
    }

    fun onGoogleAccountSelected(email: String) {
        val app = getApplication<Application>()
        GoogleAccountManager.saveActiveGoogleAccount(app, email)
        refreshGoogleAccounts()
        viewModelScope.launch {
            soundManager.playConfirmSound()
            boostPerformance("Google 帳號已連結：$email")
        }
    }

    fun logoutGoogleAccount() {
        val app = getApplication<Application>()
        GoogleAccountManager.logoutGoogleAccount(app)
        refreshGoogleAccounts()
        viewModelScope.launch {
            soundManager.playCancelSound()
            boostPerformance("已切換為訪客模式")
        }
    }

    fun getGoogleSignInIntent(): Intent {
        return GoogleAccountManager.getGoogleSignInIntent(getApplication())
    }

    fun handleGoogleSignInResult(data: Intent?, onFallback: () -> Unit = {}) {
        val app = getApplication<Application>()
        val photoUrl = GoogleAccountManager.handleGoogleSignInResult(app, data)
        if (photoUrl != null) {
            refreshGoogleAccounts()
            boostPerformance("已同步 Google 帳號相片！")
        } else {
            onFallback()
        }
    }

    fun updateCustomAvatar(uriString: String?) {
        val app = getApplication<Application>()
        val currentEmail = _googleAccount.value.email
        val sourceUri = uriString?.let { Uri.parse(it) }
        val newAvatarUri = GoogleAccountManager.saveCustomAvatarFromUri(app, currentEmail, sourceUri)
        refreshGoogleAccounts()
        _playerProfile.value = _playerProfile.value.copy(
            avatarUrl = newAvatarUri ?: _googleAccount.value.avatarUrl
        )
        viewModelScope.launch {
            soundManager.playConfirmSound()
            boostPerformance(if (uriString != null) "已成功套用個人頭像相片！" else "已重設個人相片")
        }
    }

    fun isOverlayPermissionGranted(): Boolean {
        return GlobalConsoleEdgeService.isOverlayPermissionGranted(getApplication())
    }

    fun requestOverlayPermission() {
        GlobalConsoleEdgeService.requestOverlayPermission(getApplication())
    }

    fun setAccountDialogOpen(open: Boolean) {
        _isAccountDialogOpen.value = open
        if (open) {
            refreshGoogleAccounts()
            viewModelScope.launch {
                soundManager.playClick()
            }
        }
    }

    fun refreshGames() {
        viewModelScope.launch {
            soundManager.playClick()
            boostPerformance("已重新整理遊戲庫與本機 ROM")
            gameRepository.scanInstalledApps()
            gameRepository.scanLocalRomFiles(savedGamesFolderUri())
            refreshGoogleAccounts()
        }
    }

    fun selectGame(index: Int) {
        if (_selectedGameIndex.value != index) {
            _selectedGameIndex.value = index
            viewModelScope.launch {
                soundManager.playCardFocusSound()
            }
            val currentList = currentTabGames.value
            currentList.getOrNull(index)?.let { game ->
                if (!_resolvedBanners.value.containsKey(game.id)) {
                    resolveBannerForGame(game)
                }
            }
        }
    }

    fun openGameActionMenu(game: GameItem) {
        _activeActionMenuGame.value = game
        // 確保彈窗打開時有解析其海報
        if (!_resolvedBanners.value.containsKey(game.id)) {
            resolveBannerForGame(game)
        }
    }

    fun closeGameActionMenu() {
        _activeActionMenuGame.value = null
    }

    fun resolveBannerForGame(game: GameItem) {
        viewModelScope.launch {
            val banner = gameBannerManager.resolveBanner(game)
            if (banner != null) {
                _resolvedBanners.value = _resolvedBanners.value + (game.id to banner)
            }
        }
    }

    fun setCustomBannerForGame(gameId: String, uri: Uri) {
        viewModelScope.launch {
            val path = gameBannerManager.setCustomBanner(gameId, uri)
            if (path != null) {
                val banner = ResolvedBanner(
                    imageModel = path,
                    sourceType = BannerSourceType.CUSTOM_USER
                )
                _resolvedBanners.value = _resolvedBanners.value + (gameId to banner)
                boostPerformance("已成功套用自訂遊戲大海報！")
            } else {
                boostPerformance("讀取自訂相片失敗")
            }
        }
    }

    fun resetCustomBannerForGame(game: GameItem) {
        viewModelScope.launch {
            gameBannerManager.resetCustomBanner(game.id)
            val banner = gameBannerManager.resolveBanner(game)
            val currentMap = _resolvedBanners.value.toMutableMap()
            if (banner != null) {
                currentMap[game.id] = banner
            } else {
                currentMap.remove(game.id)
            }
            _resolvedBanners.value = currentMap
            boostPerformance("已還原為自動海報")
        }
    }

    fun reScrapePlayStoreGraphic(game: GameItem) {
        val pkg = game.packageName ?: return
        viewModelScope.launch {
            boostPerformance("正在重新抓取 Google Play 宣傳海報...")
            val url = gameBannerManager.getOrScrapePlayStoreGraphic(pkg, forceRefresh = true)
            if (url != null) {
                val banner = ResolvedBanner(
                    imageModel = url,
                    sourceType = BannerSourceType.PLAY_STORE
                )
                _resolvedBanners.value = _resolvedBanners.value + (game.id to banner)
                boostPerformance("Google Play 高清海報更新成功！")
            } else {
                boostPerformance("Google Play 未找到宣傳海報")
            }
        }
    }

    fun isCustomBanner(gameId: String): Boolean {
        return gameBannerManager.isCustomBanner(gameId)
    }

    fun setRetroArcadeOpen(open: Boolean) {
        if (!open) finishPlaySession()
        _isRetroArcadeOpen.value = open
    }

    fun onLauncherResumed() {
        val id = prefs.getString(KEY_ACTIVE_PLAY_ID, null)
        if (id != null) {
            val startedAt = prefs.getLong(KEY_ACTIVE_PLAY_STARTED_AT, 0L)
            if (startedAt > 0L) gameRepository.recordPlaySession(id, System.currentTimeMillis() - startedAt)
            prefs.edit().remove(KEY_ACTIVE_PLAY_ID).remove(KEY_ACTIVE_PLAY_STARTED_AT).apply()
            activePlaySessionId = null
            activePlaySessionStartedAt = 0L
        }
        // 即時刷新已安裝應用，確保從 Google Play 或其他程式返回桌面時瞬間顯示新 App
        viewModelScope.launch {
            gameRepository.scanInstalledApps()
        }
    }

    private fun startPlaySession(gameId: String) {
        finishPlaySession()
        activePlaySessionId = gameId
        activePlaySessionStartedAt = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_ACTIVE_PLAY_ID, gameId)
            .putLong(KEY_ACTIVE_PLAY_STARTED_AT, activePlaySessionStartedAt)
            .apply()
    }

    private fun finishPlaySession() {
        val gameId = activePlaySessionId ?: prefs.getString(KEY_ACTIVE_PLAY_ID, null) ?: return
        val startedAt = activePlaySessionStartedAt.takeIf { it > 0L }
            ?: prefs.getLong(KEY_ACTIVE_PLAY_STARTED_AT, 0L)
        if (startedAt > 0L) gameRepository.recordPlaySession(gameId, System.currentTimeMillis() - startedAt)
        prefs.edit().remove(KEY_ACTIVE_PLAY_ID).remove(KEY_ACTIVE_PLAY_STARTED_AT).apply()
        activePlaySessionId = null
        activePlaySessionStartedAt = 0L
    }

    fun setAssistantDialogItem(item: GameItem?) {
        _assistantDialogItem.value = item
    }

    fun setPerformanceHudVisible(visible: Boolean) {
        _showPerformanceHud.value = visible
        prefs.edit().putBoolean("show_hud", visible).apply()
        GlobalConsoleEdgeService.updateHudVisibility(getApplication(), visible)
    }

    /**
     * 退出 UyenLauncher (系統唯一正規退出管道)
     * 解除 Kiosk 掌機鎖定、終止常駐懸浮快捷列服務，並導向原生桌面設定後結束應用
     */
    fun exitLauncher(activity: Activity?) {
        viewModelScope.launch {
            soundManager.playCancelSound()
        }
        if (activity == null) return

        try {
            ConsoleLockManager.disableConsoleLock(activity)
            _isKioskModeEnabled.value = false
        } catch (_: Exception) {}

        GlobalConsoleEdgeService.stop(activity)

        try {
            val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(homeSettingsIntent)
        } catch (_: Exception) {
            try {
                val manageAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(manageAppsIntent)
            } catch (_: Exception) {}
        }

        activity.finishAffinity()
    }

    fun setVolume(volume: Float) {
        systemControlManager.setMediaVolume(volume)
    }

    fun setBrightness(activity: Activity?, brightness: Float) {
        systemControlManager.setBrightness(activity, brightness)
    }

    fun setFocusDndMode(enabled: Boolean) {
        systemControlManager.setFocusDndMode(enabled)
    }

    fun openWifiSettings() {
        systemControlManager.openWifiSettings()
    }

    fun openBluetoothSettings() {
        systemControlManager.openBluetoothSettings()
    }

    fun openNotificationSettings() {
        systemControlManager.openNotificationSettings()
    }

    fun setGamesFolder(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        savedGamesFolderUri()?.takeIf { it != uri }?.let { previous ->
            runCatching { resolver.releasePersistableUriPermission(previous, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prefs.edit().putString(PREF_KEY_GAMES_TREE, uri.toString()).apply()
        viewModelScope.launch {
            gameRepository.scanLocalRomFiles(uri)
            boostPerformance("已更新所選資料夾中的遊戲檔案")
        }
    }

    fun rescanGamesFolder() {
        val uri = savedGamesFolderUri()
        if (uri != null) {
            viewModelScope.launch {
                soundManager.playConfirmSound()
                boostPerformance("正在重新掃描遊戲目錄...")
                gameRepository.scanLocalRomFiles(uri)
                boostPerformance("遊戲目錄重新掃描完成！")
            }
        } else {
            boostPerformance("尚未設定遊戲目錄，請先點選「選取目錄」")
        }
    }

    fun savedGamesFolderUri(): Uri? = prefs.getString(PREF_KEY_GAMES_TREE, null)?.let(Uri::parse)

    fun launchGame(item: GameItem) {
        viewModelScope.launch {
            soundManager.playConfirmSound()
        }
        // 確保右下角全局懸浮返回/主頁小條已運行，防止進遊戲後回不來
        GlobalConsoleEdgeService.start(getApplication())

        when (item.id) {
            "controller_mode" -> {
                _isFullScreenControllerMode.value = true
            }
            "retro_8bit" -> {
                // 啟動內建 8-bit 太空突擊懷舊街機
                _isRetroArcadeOpen.value = true
                startPlaySession(item.id)
                val newTask = RunningTask(
                    id = item.id,
                    title = item.title,
                    packageName = "com.uyen.launcher.arcade",
                    startTimeMillis = System.currentTimeMillis()
                )
                _runningTasks.value = listOf(newTask) + _runningTasks.value.filter { it.id != item.id }
            }
            else -> {
                val launched = gameRepository.launchGame(item)
                if (launched) {
                    startPlaySession(item.id)
                    // 將真正啟動的遊戲登記進後台運行程序列表
                    val newTask = RunningTask(
                        id = item.id,
                        title = item.title,
                        packageName = item.packageName ?: item.launchIntentUri.orEmpty(),
                        startTimeMillis = System.currentTimeMillis()
                    )
                    _runningTasks.value = listOf(newTask) + _runningTasks.value.filter { it.packageName != item.packageName }
                } else {
                    // 模擬器或核心尚未安裝，彈出指引彈窗
                    _assistantDialogItem.value = item
                }
            }
        }
    }

    /**
     * 掌機主頁鍵 (Home)：重置選中焦點，關閉所有彈窗與抽屜，並滑動回第一個卡片
     */
    fun handleHome() {
        viewModelScope.launch {
            soundManager.playConfirmSound()
        }
        setRetroArcadeOpen(false)
        _assistantDialogItem.value = null
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
            _isRetroArcadeOpen.value -> setRetroArcadeOpen(false)
            _assistantDialogItem.value != null -> _assistantDialogItem.value = null
            _isFullScreenControllerMode.value -> _isFullScreenControllerMode.value = false
            _isTaskSwitcherOpen.value -> _isTaskSwitcherOpen.value = false
            _isLibraryOpen.value -> _isLibraryOpen.value = false
            _isSettingsOpen.value -> _isSettingsOpen.value = false
            _selectedGameIndex.value != 0 -> _selectedGameIndex.value = 0
        }
    }

    /** Clears launcher recents and kills cached background processes to free RAM with real measurements. */
    fun cleanMemory() {
        viewModelScope.launch {
            soundManager.playConfirmSound()

            val recentPackages = _runningTasks.value.map { it.packageName }
            _runningTasks.value = emptyList()

            _boostMessage.value = "⚡ 正在釋放系統 RAM 與後台程序..."

            val result = SystemMemoryManager.cleanRam(getApplication(), recentPackages)

            // 立即刷新效能監控數據與 HUD
            performanceMonitor.updateSystemStats()

            _boostMessage.value = result.displayMessage
            delay(3500)
            _boostMessage.value = null
        }
    }

    /** Removes one item from this launcher's recent-launch history. */
    fun removeRecentLaunch(task: RunningTask) {
        _runningTasks.value = _runningTasks.value.filter { it.id != task.id }
        viewModelScope.launch {
            soundManager.playCardFocusSound()
            _boostMessage.value = "已移除 ${task.title} 的啟動紀錄"
            delay(2000)
            _boostMessage.value = null
        }
    }

    fun boostPerformance(message: String) {
        viewModelScope.launch {
            _boostMessage.value = message
            delay(2500)
            _boostMessage.value = null
        }
    }

    fun setSettingsOpen(open: Boolean) {
        if (_isSettingsOpen.value != open) {
            _isSettingsOpen.value = open
            viewModelScope.launch {
                if (open) soundManager.playConfirmSound() else soundManager.playCancelSound()
            }
        }
    }

    fun setLibraryOpen(open: Boolean) {
        if (_isLibraryOpen.value != open) {
            _isLibraryOpen.value = open
            viewModelScope.launch {
                if (open) {
                    soundManager.playConfirmSound()
                    gameRepository.scanInstalledApps()
                } else {
                    soundManager.playCancelSound()
                }
            }
        }
    }

    fun setTaskSwitcherOpen(open: Boolean) {
        if (_isTaskSwitcherOpen.value != open) {
            _isTaskSwitcherOpen.value = open
            viewModelScope.launch {
                if (open) soundManager.playConfirmSound() else soundManager.playCancelSound()
            }
        }
    }

    fun setGamepadOverlayActive(active: Boolean) {
        _isGamepadOverlayActive.value = active
    }

    fun setFullScreenControllerMode(active: Boolean) {
        _isFullScreenControllerMode.value = active
    }

    fun checkDeviceOwnerState() {
        _isDeviceOwner.value = ConsoleLockManager.isDeviceOwner(getApplication())
        _isKioskModeEnabled.value = ConsoleLockManager.isLockTaskActive(getApplication())
    }

    fun toggleKioskLock(activity: Activity?) {
        if (activity == null) return
        val currentLocked = ConsoleLockManager.isLockTaskActive(activity)
        if (currentLocked) {
            ConsoleLockManager.disableConsoleLock(activity)
            _isKioskModeEnabled.value = false
            prefs.edit().putBoolean("kiosk_auto_lock", false).apply()
            _boostMessage.value = "已解除掌機鎖定模式"
        } else {
            val success = ConsoleLockManager.enableConsoleLock(activity)
            _isKioskModeEnabled.value = success
            if (success) {
                prefs.edit().putBoolean("kiosk_auto_lock", true).apply()
                _boostMessage.value = if (_isDeviceOwner.value) {
                    "🔒 實體掌機級硬體鎖定已啟動 (狀態列下拉徹底廢除)"
                } else {
                    "🔒 掌機鎖定模式已啟動 (建議配置 Device Owner 達成完全鎖死)"
                }
            } else {
                _boostMessage.value = "啟動鎖定失敗，請確認權限"
            }
        }
        viewModelScope.launch {
            delay(2500)
            _boostMessage.value = null
        }
    }

    fun setKioskModeEnabled(enabled: Boolean) {
        _isKioskModeEnabled.value = enabled
    }

    override fun onCleared() {
        super.onCleared()
        performanceMonitor.stopMonitoring()
        soundManager.release()
        gameRepository.release()
    }
}
