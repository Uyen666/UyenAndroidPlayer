# 📌 UyenLauncher 項目進度追蹤記錄 (Current.md)

- **專案名稱**：UyenLauncher (UyenAndroidPlayer)
- **遠端倉庫**：`https://github.com/Uyen666/UyenAndroidPlayer.git`
- **當前版本**：v0.1.2-alpha (Console Dock, Real Task Manager & Gesture Lock Verified)
- **目標設備**：小米 Redmi 13C (Helio G85 / 720×1600 @ 90Hz / Android 14 HyperOS)
- **最後更新時間**：2026-10-02 22:45

---

## 🚀 當前階段目標 (Phase 1: 核心架構、單元測試與實機體驗優化) - 全部通過 ✅

- [x] **實機硬體探測與相容性體檢**
  - 確認 Redmi 13C (MT6769V Helio G85) 支援 90Hz 高刷螢幕模式。
  - 確認可用儲存空間 97 GB、記憶體 4 GB。
- [x] **版本控制與安全規範設定**
  - 初始化 Git 倉庫並關聯至 `https://github.com/Uyen666/UyenAndroidPlayer.git`。
  - 配置商業級 `.gitignore`（防護 Keystore、金鑰、本機環境設定）。
  - 建立專案規範 `README.md` 與狀態日誌 `Current.md`。
- [x] **單元測試 (Unit Tests)**
  - 建立 `UyenLauncherUnitTest.kt`（驗證 PlayerProfile、SystemStats、GameCategory、GameItem、VirtualGamepadState、RunningTask 狀態機）。
  - 執行 `./gradlew test` 全部通過（Debug & Release Unit Tests: 100% PASS）。
- [x] **閃退修復與架構穩定**
  - Android 14 `WindowCompat` 沉浸式相容處理，View 初始化安全執行。
- [x] **系統按鍵與下拉選單沉浸控制**
  - 設定 `systemBarsBehavior = BEHAVIOR_DEFAULT` 防止邊緣滑動喚出虛擬鍵。
  - 透過 `cmd statusbar send-disable-flag statusbar-expansion home recents notification-peek` 完全封鎖系統下拉列。
  - 整合掌機沉浸鎖定（Kiosk Lock `startLockTask`）於快捷設定中。
- [x] **專屬掌機 ConsoleDockBar**
  - 在螢幕右下角打造專屬懸浮導航列：提供 [返回]、[主頁]、[多工]、[加速] 實體化操作。
- [x] **PS5 卡片平滑滾動同步**
  - 修復卡片選擇回到第 1 張時 LazyList 未同步滾動的問題，採用 `LaunchedEffect(selectedIndex)` 驅動 `animateScrollToItem`。
- [x] **真實動態多工管理 (RunningTask Lifecycle)**
  - 徹底剔除靜態全庫 mock 假清單，改採真實記錄的動態 `RunningTask` 生命週期管理。
  - 支援單一結束進程與一鍵加速清理，無任務時優雅呈現清空狀態。

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
| **UyenController (PC手柄模式)** | 🟢 運行正常 | 點擊即可進入虛擬搖桿操作與 UDP 發送 |
| **單元測試套件** | 🟢 全部通過 | 100% 通過（含 RunningTask 測試） |

---

## 🔜 下一階段目標 (Phase 2: 深入擴展)
1. **PC 端極輕量接收端腳本 (`scripts/uyen_controller_receiver.py`)**：將手機 UDP 按鍵映射為電腦虛擬 Xbox 360 手柄 (ViGEmBus / vgamepad)。
2. **內建免網路 8-bit / Canvas 懷舊自製迷你遊戲引擎**（隨開隨玩，高刷流暢）。
3. **ROM 與 Galgame 本地檔案掃描器**（自動識別 `/sdcard/Games/` 的 `.xp3`、Ren'Py 與 NES/GBA ROMs）。
