# UyenLauncher (v1.0.0 正式版)

UyenLauncher 是專為 Android 打造的掌機風格遊戲啟動器（以 Kotlin、Jetpack Compose 建置，最佳化適配橫向掌機模式）。它提供 PS5/SteamOS 風格的 Hero Banner 巨幅海報牆、商業級 Galgame 遊戲目錄識別、已安裝 App 聚合庫、真實記憶體清理引擎、系統控制抽屜與全局懸浮導航條。

## 核心功能

- **商業級 Galgame 整合**：
  - **智慧目錄識別**：以遊戲子資料夾為單一實體單位，自動識別吉里吉里 2/Z（Kirikiri, `.xp3`, `startup.tjs`）、Ren'Py（`.rpa`）、TyranoBuilder（`index.html`）、RPG Maker / Wolf RPG（`data.wolf`, `Game.exe`）等主流 AVG 格式，避免同一遊戲被拆解為多個單檔碎片。
  - **封面海報自動探測**：優先識別子資料夾內之 `cover.jpg/png`、`folder.jpg`、`poster.webp`、`thumb.png`，無縫呈現在 2:3 直式海報與 Hero Banner 呼吸高斯模糊背景。
  - **SteamOS 收藏庫專區**：在收藏庫 GALGAME 分頁支援專屬空狀態、一鍵 SAF 資料夾授權選取、頂部重新整理/變更目錄，以及卡片右上角獨立「📌 釘選至首頁」切換。
  - **兩段式開玩體驗**：首頁卡片第 1 次點擊滾動並聚焦（更新大海報），第 2 次點擊直接啟動遊戲。
  - **核心引擎分發與引導**：自動探測 Tyranor、Kirikiroid2、JoiPlay 等相容核心；未安裝引擎時彈出專屬導航彈窗，提供格式說明與應用商店搜尋跳轉。
- **Google Play 新裝 App 實時熱同步（PackageChangeMonitor）**：
  - 雙通道事件監控引擎（`LauncherApps.Callback` 與 `BroadcastReceiver`）結合二段式防抖機制，Google Play 或外部安裝完成時無感自動更新。
  - 繞過 Android 11+ Package Visibility 限制，支援多用戶與 MIUI/HyperOS 應用雙開。
  - 生命週期返回桌面（`onLauncherResumed`）與進入收藏庫（`setLibraryOpen`）自動背景補償掃描，收藏庫頂部提供全局一鍵刷新。
- **旗艦掌機視覺展台與雙層環境光（Hero Stage & Ambient Lighting）**：
  - **右側獨立 Hero 展台**：徹底解決 1024x500 等低解析海報被全螢幕暴力放大拉伸（1600x720）造成的馬賽克與文字重疊問題。將海報視覺精確約束在右側黃金區域（寬度 64%、高度 72%），海報以接近 1:1 原生像素密度高畫質渲染。
  - **四向羽化柔和漸層**：左側羽化漸淡融入深色底座，使左側標題與副標題清晰呈現不被干擾；底部柔和隱入卡片；頂部防護狀態列；邊緣細緻暗角。
  - **智慧長寬比動態適配**：橫向海報（寬高比 > 1.22）右置 4 向羽化寬幅海報展台；正方形圖示（0.82 ~ 1.22，如 YouTube 等應用）自動轉化為精緻 150dp 3D 浮動徽章，終結全螢幕紅色色塊；直式海報（< 0.82，如 Galgame）呈現 Steam Deck 風格 2:3 獨立直式浮動膠囊海報。
  - **動態品牌雙層環境光**：依據聚焦項目之品牌色彩（Moonlight 青、YouTube 赤紅、Steam 蔚藍、Galgame 粉紫、8-Bit 暖琥珀、Uyen 綠洲青）動態渲染全螢幕放射狀氛圍光與展台聚焦光。
- **掌機首頁與遊戲庫**：
  - 首頁 5 張自訂輪播卡片（支援自由增刪與長按自訂相簿海報）。
  - SteamOS 1:1 移植全螢幕收藏庫（L1/R1 切換分頁、分類動態數量徽章、A啟動/B返回）。
- **全系統統一掌機控制台（QuickSettingsDrawer）**：
  - 首頁與全局跨應用懸浮抽屜（`GlobalConsoleEdgeService`）共享一致的 Compose 介面。
  - 支援媒體音量、視窗亮度調節、Wi-Fi/藍牙跳轉、效能 HUD 開關、遊戲目錄變更與安全的二次確認退出機制。
- **真實硬體記憶體釋放引擎（SystemMemoryManager）**：
  - 拒絕虛假展示數字。真實採樣 Linux 核心 `ActivityManager.MemoryInfo` 可用 RAM 差值、終止目標背景應用、清理 Coil 圖片快取池與 JVM GC，動態回報清出之 MB 數。
- **頂部中央微型效能 HUD**：
  - 實時呈現影格率（FPS）、電池溫度、全機可用 RAM 與電量狀態，觸控完全穿透至遊戲中。
- **本機資料與隱私保障**：
  - 遊戲目錄透過 Storage Access Framework（SAF）由玩家明確選取並授權，不要求整機儲存空間存取權。
  - 首頁釘選、收藏狀態、遊玩時數與自訂海報保存在 App 私有沙盒偏好資料中。
  - Google 帳號管理支援原生帳號選取與沙盒頭像同步，預設離線訪客模式。

## 架構

```text
app/src/main/java/com/uyen/launcher/
├── core/                  # 系統整合、SAF 智慧掃描器、海報解析、實時安裝監控、真實 RAM 管理與全局服務
├── data/
│   ├── model/             # 遊戲實體、帳號與畫面狀態模型
│   └── repository/        # 已安裝 App、本機遊戲聚合與偏好儲存
└── presentation/          # Activity 與 Jetpack Compose 畫面與掌機組件
    ├── home/              # 首頁主畫面與 ViewModel
    ├── home/components/   # SteamLibraryDialog, HeroBanner, GameCarousel, QuickSettingsDrawer 等
    └── minigame/          # 內建離線 8-bit 太空街機 (90Hz Canvas)
```

## 建置與測試

需求：JDK 21、Android SDK 35。

```powershell
# 建置 Debug APK
.\gradlew.bat assembleDebug

# 執行全套單元測試 (35 項測試 100% 通過)
.\gradlew.bat testDebugUnitTest
```

Debug APK 輸出路徑：`app/build/outputs/apk/debug/app-debug.apk`。

## 隱私與資安承諾

- 不要求讀取聯絡人、整機檔案全盤存取或整機帳號清單。
- 遊戲資料夾由使用者透過系統選擇器授權，授權 URI 保存在 App 私有偏好中。
- `.gitignore` 嚴格排除簽章金鑰（`*.keystore`, `*.jks`）、憑證、環境機密與編譯產物，落實 Zero-Leak 原則。
