# UyenLauncher 專案現況

- **套件版本**：`1.0.1`（`versionCode 2`）
- **GitHub**：https://github.com/Uyen666/UyenAndroidPlayer
- **更新日期**：2026-10-03
- **主要技術**：Kotlin、Jetpack Compose、Android SDK 35
- **目標裝置**：Redmi 13C / Android 14，橫向使用
- **Git 分支**：`main`；現有 GitHub remote 與忽略規則保留

## 目前已實作

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
- 建置狀況：`assembleDebug` 42 項 Task 全部成功執行。
- 單元測試：`UyenLauncherUnitTest.kt` 擴充至 **31 項單元測試，通過率 100%（31/31 PASSED）**。
- 實機驗證：相容 Redmi 13C (Android 14) 橫向掌機環境。
