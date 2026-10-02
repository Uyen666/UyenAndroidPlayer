# 📌 UyenLauncher 項目進度追蹤記錄 (Current.md)

- **專案名稱**：UyenLauncher (UyenAndroidPlayer)
- **遠端倉庫**：`https://github.com/Uyen666/UyenAndroidPlayer.git`
- **當前版本**：v0.3.1-rc1 (16:9 Landscape Artwork Capsules, Luxury Damped Carousel & Spring Library Modal)
- **目標設備**：小米 Redmi 13C (Helio G85 / 720×1600 @ 90Hz / Android 14 HyperOS)
- **最後更新時間**：2026-10-03 00:41

---

## 🚀 階段目標進度

### Phase 1: 核心架構、單元測試與實機體驗優化 - 全部通過 ✅
- [x] **實機硬體探測與相容性體檢**：Redmi 13C 90Hz 高刷確認、儲存空間與記憶體探測。
- [x] **版本控制與安全規範設定**：GitHub 遠端關聯、`.gitignore` 安全配置、`README.md`、`Current.md`。
- [x] **單元測試 (Unit Tests)**：JUnit 4 單元測試 100% 通過。
- [x] **閃退修復與架構穩定**：Android 14 WindowCompat 沉浸模式 NPE 修復。
- [x] **PS5 卡片平滑滾動同步**：`LaunchedEffect(selectedIndex)` 驅動 `animateScrollToItem`。
- [x] **真實動態多工管理 (RunningTask Lifecycle)**：杜絕假數據，支援個別關閉與一鍵清理。

### Phase 2: 深入擴展（8-bit 遊戲、PC 接收端、ROM 掃描與核心指引） - 全部通過 ✅
- [x] **PC 端極輕量接收端腳本 (`scripts/uyen_controller_receiver.py`)**：UDP 8999 轉虛擬 Xbox 360 手柄。
- [x] **內建 8-bit 太空突擊懷舊街機 (`RetroArcadeScreen.kt`)**：90Hz 流暢街機小遊戲，含合成 8-bit 音效。
- [x] **本地 ROM 與 Galgame 掃描器 (`LocalRomScanner.kt`)**：自動掃描並建立 `/sdcard/Games/`。
- [x] **掌機模擬器指引彈窗 (`EmulatorAssistantDialog.kt`)**：未安裝引擎時友善指引。

### Phase 3: 極致純淨美學、邊緣手勢與硬體控制台 - 全部通過 ✅
- [x] **自動隱藏邊緣側邊小條 (AutoHideEdgeHandle)**
  - 徹底移除常態顯示之大塊 ConsoleDockBar，杜絕死碼與 UI 雜亂。
  - 常態僅保留右側邊緣一條 3.5dp 極細灰色微光線條 (Alpha = 0.25f)，看海報時 100% 純淨不擋畫面。
  - 點擊或向內撥動以彈簧動畫滑出微型藥丸膠囊（返回、主頁、多工、設定），3 秒無操作自動平滑縮回淡出。
- [x] **左上角隱藏滑出之掌機系統控制台 (SystemControlManager & QuickSettingsDrawer)**
  - 支援螢幕最左邊緣向右滑動手勢平滑呼出控制台。
  - **音量滑桿**：即時讀取與無段調節媒體音量 (`AudioManager.STREAM_MUSIC`)。
  - **亮度滑桿**：即時調節視窗螢幕亮度 (`screenBrightness`)。
  - **Wi-Fi & 藍牙**：快捷狀態檢視與系統設定直達。
  - **效能 HUD 顯示開關**：可在設定中開啟/關閉左上角固定 FPS/溫度顯示，關閉時完全隱藏無遮擋。
- [x] **通知聲突兀問題解決 (🎮 掌機遊戲專注模式 Game Focus DND)**
  - 解決「後台常有通知聲但沉浸模式看不到」的痛點：開啟時自動將通知音量與鈴聲靜音，保留遊戲音樂，並提供通知管理快捷通道。
- [x] **黑邊與系統返回手勢抑制**
  - 配置 `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`，視窗鋪滿水滴屏區域，消除橫屏黑邊。
  - 配置 `setSystemGestureExclusionRects` 邊緣手勢排除，抑制滑動時彈出系統返回箭頭與黑色手勢條。

### Phase 4: 實體主機等級硬體鎖死 (Device Owner, LockTask & Status Bar Disable) - 全部通過 ✅
- [x] **Device Owner 模式授權成功 (`UyenDeviceAdminReceiver`)**
  - 小米 Redmi 13C 實機成功綁定：`Success: Device owner set to package com.uyen.launcher/.receiver.UyenDeviceAdminReceiver`
  - 系統底層驗證確認：`admin=com.uyen.launcher/.receiver.UyenDeviceAdminReceiver,DeviceOwner,Affiliated`
- [x] **硬體級 LockTask Kiosk 模式生效 (`mLockTaskModeState=LOCKED`)**
  - 動態配置 `setLockTaskPackages` 白名單（包含本啟動器與所有遊戲/模擬器）。
  - 設定 `LOCK_TASK_FEATURE_NONE`，徹底從 Android framework 拔除狀態列下拉手勢、遮蔽返回鍵與多工鍵。
- [x] **自動 Console 模式啟動與控制台記憶**
  - `MainActivity` 於 `onResume` 自動進入極限掌機鎖死，並於控制台提供一鍵切換與開關記憶。
- [x] **控制台狀態聯動與引導**
  - `QuickSettingsDrawer` 即時感知 Device Owner 狀態，提供一鍵啟用/解除與終端指令指引。

### Phase 5: 掌機經典橫向膠囊卡片、滿版藝術視覺、阻尼物理滾動與遊戲庫彈簧動畫 - 全部通過 ✅
- [x] **低姿態橫向膠囊卡片 (Landscape Capsule Cards)**
  - 廢除過去偏正方形卡片 (140×150dp)，改為 SteamOS / Switch 經典 16:9 低姿態橫向長方形膠囊 (188×106dp)。
  - `LazyRow` 高度由 180dp 調整為 142dp，在 Redmi 13C 橫屏釋放關鍵垂直空間，與 `HeroBanner` 達成黃金比例呼吸感。
- [x] **整張卡都是遊戲圖示 (Full-Bleed Artwork Visuals & Zero Overlap)**
  - 打造 `AppIconUtil.kt` 高性能 LRU 圖標快取與專屬主題漸層引擎。
  - 已安裝應用顯示真實高解析 Android 應用圖標（置頂居中 46dp），搭配背景動態漫射光。
  - 內建遊戲與模擬器呈現全卡片主題藝術漸層與 86dp 旋轉高科技主題浮水印。
  - 底部配置深色暗角漸層保護罩 (`Brush.verticalGradient`)，標題與分類標籤獨立置底，徹底根除圖標與文字重疊問題。
- [x] **520ms 旗艦主機阻尼物理滾動 (Silky Smooth Console Scroll Physics)**
  - 告別原生 `animateScrollToItem` 的生硬猛衝，引入 `animateScrollBy` 搭配 `CubicBezierEasing(0.22f, 1f, 0.36f, 1f)` 旗艦主機減速曲線。
  - 卡片焦點移動時溫潤滑向視窗正中央，具備 PS5 / Steam Deck 等級的平滑減速與懸浮微彈性（Spring 0.85f）。
- [x] **Steam OS 應用庫彈簧微回彈展開動畫 (Spring Scale-in Modal)**
  - 徹底移除阻礙 Compose 退場動畫之 `if (!visible) return`。
  - 展開時採用彈簧縮放與平滑位移：`scaleIn(0.90f, spring(0.80f, StiffnessMediumLow)) + slideInVertically + fadeIn()`。
  - 退出時採用平滑收縮：`scaleOut(0.94f) + slideOutVertically + fadeOut()`。
  - 應用庫內所有項目同步升級為橫向全幅藝術膠囊，開啟與關閉全流程聯動 `SoundManager` 音效回饋。

---

## 📋 功能模組狀態一覽

| 模組名稱 | 狀態 | 驗證結果 |
| :--- | :--- | :--- |
| **Steam OS Boot Intro** | 🟢 運行正常 | 支援觸控點擊即跳過 |
| **16:9 橫向滿版遊戲卡片** | 🟢 運行正常 | 188×106dp 橫向低姿態膠囊，滿版藝術漸層，文字圖標零重疊 |
| **旗艦主機阻尼平滑滾動** | 🟢 運行正常 | 520ms 貝塞爾居中滑動，PS5/Steam Deck 級別絲滑物理體感 |
| **Steam OS 應用庫 (彈簧動畫)** | 🟢 運行正常 | 具備 0.80f 彈簧縮放進出場動畫，卡片全幅藝術視覺化 |
| **AutoHideEdgeHandle 微型膠囊** | 🟢 運行正常 | 右側 3.5dp 微光條，點擊滑出藥丸，3秒自動縮回 |
| **掌機系統控制台 (左側滑出)** | 🟢 運行正常 | 整合音量/亮度滑桿、Wi-Fi/藍牙、HUD 開關、專注靜音 |
| **實體主機級鎖死 (LockTask)** | 🟢 運行正常 | `ConsoleLockManager` + `StatusBarManager` 廢除狀態列下拉 |
| **遊戲專注模式 (通知靜音)** | 🟢 運行正常 | 解決看不見通知卻有突然提示聲的問題 |
| **全螢幕挖孔無黑邊與手勢排除** | 🟢 運行正常 | 水滴屏滿版渲染 + 邊緣手勢排除抑制返回箭頭 |
| **真實掌機多工管理器** | 🟢 運行正常 | 動態 `RunningTask`，支援個別終止與一鍵釋放記憶體 |
| **UyenController (PC手柄模式)** | 🟢 運行正常 | 支援 UDP 廣播，PC 接收端 `scripts/uyen_controller_receiver.py` 就緒 |
| **8-bit 太空突擊街機 (內建小遊戲)** | 🟢 運行正常 | 90Hz Canvas 繪製、復古音效、CRT 掃描線、離線隨開隨玩 |
| **ROM & Galgame 本地掃描器** | 🟢 運行正常 | 自動探測與建立 `/sdcard/Games` 目錄 |
| **單元測試套件** | 🟢 全部通過 | 100% 通過（共 11 項測試案例） |

