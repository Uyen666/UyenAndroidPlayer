# UyenLauncher 專案現況

- **套件版本**：`1.0.0`（`versionCode 100`，第一次正式發行版）
- **GitHub**：https://github.com/Uyen666/UyenAndroidPlayer
- **更新日期**：2026-10-04
- **主要技術**：Kotlin、Jetpack Compose、Android SDK 35
- **目標裝置**：Redmi 13C / Android 14，橫向使用
- **Git 分支**：`main`；現有 GitHub remote 與忽略規則保留

## 目前已實作

- **PS5 旗艦全螢幕海報與雙向漸層遮罩系統（`GameBannerBackdrop` & `AppIconUtil`）**：
  - **消除硬切斷層（100% 全螢幕底圖）**：打破舊版只佔右側造成邊緣斷層的缺陷，背景底圖鋪滿 100% 全螢幕（`ContentScale.Crop`），以單一整體畫布渲染。
  - **雙向線性漸層遮罩系統（Dual Gradient Scrim）**：
    - **左側水平漸層**：從最左側實黑（`#070B10`）到 35% 處 85% 深黑，至 70% 處自然羽化溶解到背景圖中，高對比托住標題與資訊文字。
    - **底部垂直漸層**：從透明到底部實黑，柔和承托下方選中之卡片輪播列，避免卡片與背景雜色衝突。
    - **頂部狀態列漸層**：保護時間、電量、Wi-Fi 與頂部導航分頁列。
    - **景深暗角（Vignette）**：大半徑四周暗角壓暗邊緣，烘托劇院主機電影感。
  - **精選無字 Key Art / Hero Wallpaper（`GameBannerManager`）**：
    - 區分「Hero Art」與「Banner」：為 Steam Link（深邃星空科技藍）、Moonlight（極光夜空）、UyenController（幾何碳纖維科技網格）、8-Bit（賽博霓虹街機）準備高畫質 16:9 無字 Key Art，徹底解決橫幅 Banner 自帶大文字導致的重複打架雜亂感。
  - **去除開發者除錯感，提升主機排版精緻度（`GameRepository` & `HeroBanner`）**：
    - **隱藏 Package Name**：首頁主畫面絕不顯示生硬的 package name，改由 `resolveConsoleSubtitle` 自動解析為「Valve Corporation • 遠端主機串流」、「Google LLC • 影音串流平台」等發行商與平台描述；除錯資訊收斂於選單屬性彈窗。
    - **精緻膠囊標籤**：類別與標籤全面升級為圓角膠囊（`RoundedCornerShape(16.dp)`），11sp 字級搭配半透明毛玻璃底色（`Color.White.copy(alpha = 0.15f)`）與 0.5dp 微光細緻邊框。
    - **呼吸感排版**：標題設為 `FontWeight.ExtraBold` 搭配立體柔和文字陰影，增大垂直留白。
  - **方形圖示景深氛圍氛光（Atmospheric Glow）**：
    - 針對 YouTube、Chrome 等方形應用圖示，杜絕全螢幕馬賽克拉伸，底層以大半徑品牌色漫射氛光充盈全屏，右側呈現 136dp 3D 浮動圓角立體徽章。

- **Google Play 新裝 App 實時熱同步系統（`PackageChangeMonitor` & `GameRepository`）**：
  - **雙通道事件監控引擎**：註冊 Android 官方專為桌面啟動器設計的 `LauncherApps.Callback`（`onPackageAdded`, `onPackageRemoved`, `onPackageChanged` 等），輔以動態 `BroadcastReceiver`（`ACTION_PACKAGE_ADDED/REMOVED/REPLACED`）雙保險。當由 Google Play 或第三方商店安裝完成時，由 300ms/800ms 二段式防抖機制自動觸發重新掃描，遊戲庫即時自動更新。
  - **多用戶與分身雙開穿透（`LauncherApps.getActivityList`）**：支援 Android 企業工作設定檔與 MIUI/HyperOS 應用雙開，自動規避 Android 11+ Package Visibility 查詢限制，非標準環境下平滑退避至 `PackageManager`。
  - **生命週期返回無感熱補償**：在啟動器恢復前台 `onLauncherResumed()`（如從 Google Play 切回桌面）及開啟收藏庫 `setLibraryOpen(true)` 時自動排程非同步熱檢查，確保剛裝好的 App 永不丟失、直接出現在收藏庫中。
  - **SteamOS 收藏庫全局一鍵重新整理（`SteamLibraryDialog`）**：於收藏庫頂部工具列提供全局 `[ 🔄 重新整理 ]` 按鈕，使用者無需退出回首頁亦能隨時手動刷新。
- **商業級 Galgame 掌機整合子系統**：
  - **智慧子資料夾識別架構（`LocalRomScanner`）**：以遊戲子資料夾為單一實體單位，自動辨識吉里吉里 2/Z（Kirikiri, `.xp3`, `startup.tjs`）、Ren'Py（`.rpa`, `options.rpy`）、TyranoBuilder（`index.html`）、RPG Maker / Wolf RPG（`data.wolf`, `Game.exe`）等主流 AVG 格式，徹底杜絕單款遊戲被拆散成數十個 `patch.xp3` 碎片的痛點。
  - **高畫質封面海報自動探測**：優先識別遊戲資料夾內之 `cover.jpg/png`、`folder.jpg`、`poster.webp`、`thumb.png` 等海報圖，無縫繫結為直式 2:3 海報與首頁 Hero Banner 巨幅視覺。
  - **SteamOS 收藏庫 GALGAME 專區（`SteamLibraryDialog`）**：配備專屬高質感空狀態卡片，提供 `[ 📁 選取 Galgame 目錄 (SAF 授權) ]` 快速按鈕；支援標籤列頂部 `[ 📁 變更目錄 ]` 與 `[ 🔄 重新整理 ]`；每張卡片右上角均具備 `[ 📌 釘選至首頁 ]` 獨立切換按鈕。
  - **首頁兩段式聚焦與開玩**：輪播卡片第 1 次點擊滾動並聚焦（更新 Hero Banner 海報與背景呼吸高斯模糊），第 2 次點擊（或點擊開始遊戲）直接啟動。
  - **智慧核心分發與相容引導（`GameRepository` & `EmulatorAssistantDialog`）**：啟動 Galgame 時自動偵測手機是否已安裝 Tyranor、Kirikiroid2、JoiPlay 等相容播放器；未安裝時優雅彈出掌機引導彈窗，清楚解說格式需求並提供一鍵應用商店搜尋與下載跳轉，絕不閃退。
- **全系統掌機控制台統一架構**：
  - 首頁與全局跨應用邊緣懸浮快捷服務（`GlobalConsoleEdgeService`）全面共享同一套 Compose UI（`QuickSettingsDrawer`），具備系統音量/亮度滑桿、Wi-Fi/藍牙跳轉、效能 HUD 開關、遊戲目錄管理與安全的二次確認退出機制。
- **真實硬體記憶體釋放與優化引擎（`SystemMemoryManager`）**：
  - 徹底移除假展示邏輯，精確採樣 Linux 核心 `ActivityManager.MemoryInfo` 前後真實可用 RAM 差值、終止目標背景應用、清空 Coil 高解析圖片快取池與 JVM GC 碎片整理，動態回報真實清出 MB 數。
- **頂部中央微型效能 HUD（`TopCenterPerformanceHud`）**：
  - 實時呈現影格率（FPS）、電池溫度、全機可用 RAM 與電量狀態，觸控完全穿透至底層遊戲。
- **Google 帳號管理與頭像同步**：
  - 支援原生帳號選取與沙盒私有頭像儲存，兼具離線訪客模式。
- **全局邊緣導航條與開發者選項防禦（`GlobalConsoleEdgeService` & `SystemControlManager`）**：
  - **自動清理偵錯應用保護機制**：自動檢測並清除系統 `DEBUG_APP` 全局設定，徹底解決 Android / MIUI 在進入「開發者選項」時因偵錯機制強制殺死 UyenLauncher（`stop due to set debug app`）導致全局懸浮膠囊消失、使用者受困於系統設定的底層缺陷。
  - **前台懸浮服務防殺韌性**：加入 `stopWithTask=false` 與高對比度 40dp 觸控手柄邊框，並由最高優先權之無障礙服務（`UyenConsoleAccessibilityService`）雙向看守，確保跨應用返回/主頁/多工按鈕永不遺失。
- **建置與版本安全管理**：
  - `.gitignore` 完備排除 keystore、憑證、SDK 本機設定、環境機密與二進位檔案。

## 目前界線

- Google 登入目前僅顯示授權的個人資料與頭像；雲端存檔同步尚未實作，遊戲庫與遊玩紀錄保存在本機私有沙盒。
- Galgame 與 ROM 執行高度依賴 Android 本機之模擬器生態（如 Tyranor、Kirikiroid2、RetroArch）；啟動器提供完整權限通道與無縫調用，若裝置未安裝目標核心則依賴引導面板指引。
- 多工視窗優先透過原生 AccessibilityService 呼出系統原生 Recents，亦提供啟動器內部最近開啟項目的快捷切換。

## 待辦方向

1. 增加遊戲庫本機匯出/匯入與偏好設定版本遷移。
2. 加入復古遊戲 8-bit / 16-bit 專區深度整合與外接手柄藍牙鍵位映射。
3. 為正式雲端同步設計明確的使用者授權、衝突處理與資料刪除流程。

## 版本與驗證

- 工作分支：`main`。
- 建置狀況：`assembleDebug` 35 項 Task 全部成功執行。
- 單元測試：`UyenLauncherUnitTest.kt` 與 `GameBannerUnitTest.kt` 擴充至 **36 項單元測試，通過率 100%（36/36 PASSED）**。
- 實機驗證：相容 Redmi 13C (Android 14) 橫向掌機環境。
