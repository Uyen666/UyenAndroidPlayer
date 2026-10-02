# 📌 UyenLauncher 項目進度追蹤記錄 (Current.md)

- **專案名稱**：UyenLauncher (UyenAndroidPlayer)
- **遠端倉庫**：`https://github.com/Uyen666/UyenAndroidPlayer.git`
- **當前版本**：v0.2.0-beta (UyenController PC Receiver, 8-Bit Arcade Engine & ROM Scanner)
- **目標設備**：小米 Redmi 13C (Helio G85 / 720×1600 @ 90Hz / Android 14 HyperOS)
- **最後更新時間**：2026-10-02 22:56

---

## 🚀 階段目標進度

### Phase 1: 核心架構、單元測試與實機體驗優化 - 全部通過 ✅
- [x] **實機硬體探測與相容性體檢**：Redmi 13C 90Hz 高刷確認、儲存空間與記憶體探測。
- [x] **版本控制與安全規範設定**：GitHub 遠端關聯、`.gitignore` 安全配置、`README.md`、`Current.md`。
- [x] **單元測試 (Unit Tests)**：JUnit 4 單元測試 100% 通過。
- [x] **閃退修復與架構穩定**：Android 14 WindowCompat 沉浸模式 NPE 修復。
- [x] **系統按鍵與下拉選單沉浸控制**：ADB disable flag + Kiosk 鎖定。
- [x] **專屬掌機 ConsoleDockBar**：右下角實體化導航列（[返回]、[主頁]、[多工]、[加速]）。
- [x] **PS5 卡片平滑滾動同步**：`LaunchedEffect(selectedIndex)` 驅動 `animateScrollToItem`。
- [x] **真實動態多工管理 (RunningTask Lifecycle)**：杜絕假數據，支援個別關閉與一鍵清理。

### Phase 2: 深入擴展（8-bit 遊戲、PC 接收端、ROM 掃描與核心指引） - 全部通過 ✅
- [x] **PC 端極輕量接收端腳本 (`scripts/uyen_controller_receiver.py`)**
  - 監聽 0.0.0.0:8999 UDP 封包，支援 Windows 虛擬 Xbox 360 手柄模擬 (`vgamepad` / ViGEmBus)。
  - 提供本地 IP 自動檢測、輸入封包即時監控與控制台日誌。
  - 附帶 `scripts/requirements.txt`。
- [x] **內建 8-bit 太空突擊懷舊街機 (`RetroArcadeScreen.kt`)**
  - 純 Kotlin + Jetpack Compose Canvas 打造 90Hz 流暢街機小遊戲。
  - 支援雙發雷射、幾何外星怪侵略者、碰撞檢測、爆炸粒子特效、CRT 復古掃描線。
  - 整合 `SoundManager` 合成 8-bit 音效 (DTMF 雷射聲、爆炸轟鳴、波次通關聲)。
  - 本地 SharedPreferences 最高分持久化紀錄。
- [x] **本地 ROM 與 Galgame 掃描器 (`LocalRomScanner.kt`)**
  - 支援標準目錄結構 `/sdcard/Games/Galgames/` 與 `/sdcard/Games/ROMs/`。
  - 自動偵測識別 `.xp3`、`.rpa` (Galgame) 與 `.nes`、`.gba`、`.sfc` (復古 ROM)。
  - 提供一鍵建立標準遊戲資料夾功能。
- [x] **掌機模擬器與遊戲引擎導航指南彈窗 (`EmulatorAssistantDialog.kt`)**
  - 針對未安裝之 Tyranor、Kirikiroid2、RetroArch、Moonlight 提示放置路徑與安裝引導。
  - 支援一鍵快速啟動內建 8-bit 街機遊玩。
- [x] **單元測試擴展**
  - 新增 `testRetroArcadeBulletAndInvader` 與 `testLocalRomScannerDirectoryStructure`。
  - `./gradlew test` 全部通過 (100% PASS)。

---

## 📋 功能模組狀態一覽

| 模組名稱 | 狀態 | 驗證結果 |
| :--- | :--- | :--- |
| **Steam OS Boot Intro** | 🟢 運行正常 | 支援觸控點擊即跳過 |
| **PS5 輪播主介面 (Home Deck)** | 🟢 運行正常 | 支援主頁鍵平滑彈簧滾動回第一張卡片 |
| **ConsoleDockBar 掌機導航列** | 🟢 運行正常 | 右下角集成 [返回]、[主頁]、[多工]、[加速] |
| **真實掌機多工管理器** | 🟢 運行正常 | 動態 `RunningTask`，支援個別終止與一鍵釋放記憶體 |
| **Kiosk 掌機沉浸鎖定** | 🟢 運行正常 | 封鎖下拉通知欄與系統手勢 |
| **Steam OS 應用庫 (App Library)** | 🟢 運行正常 | 點擊右上角「遊戲庫」即可呼出分類網格 |
| **Quick Settings 效能 HUD** | 🟢 運行正常 | 左上角顯示實時 FPS 與電池溫度 |
| **玩家儀表板 (Profile Bar)** | 🟢 運行正常 | 正上方即時同步頭像、稱號、電量與時鐘 |
| **UyenController (PC手柄模式)** | 🟢 運行正常 | 支援 UDP 廣播，PC 接收端 `scripts/uyen_controller_receiver.py` 就緒 |
| **8-bit 太空突擊街機 (內建小遊戲)** | 🟢 運行正常 | 90Hz Canvas 繪製、復古音效、CRT 掃描線、離線隨開隨玩 |
| **ROM & Galgame 本地掃描器** | 🟢 運行正常 | 自動探測與建立 `/sdcard/Games` 目錄 |
| **掌機模擬器指引彈窗** | 🟢 運行正常 | 未安裝第三方引擎時友善指引與快速捷徑 |
| **單元測試套件** | 🟢 全部通過 | 100% 通過（共 9 項測試案例） |

---

## 🔜 下一階段目標 (Phase 3: 深度體驗精進)
1. 支援在觸控虛擬手柄上自訂透明度與按鍵佈局位置拖曳儲存。
2. 支援藍牙實體手柄 (如 PS4/PS5/Xbox/Switch Pro Controller) OTG 或藍牙直連按鍵映射。
3. 支援多國語言與自訂 Steam OS 開機影片/自訂音效上傳。
