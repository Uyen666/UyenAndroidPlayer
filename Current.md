# 📌 UyenLauncher 項目進度追蹤記錄 (Current.md)

- **專案名稱**：UyenLauncher (UyenAndroidPlayer)
- **遠端倉庫**：`https://github.com/Uyen666/UyenAndroidPlayer.git`
- **當前版本**：v0.1.1-alpha (Crash Resolved & Unit Tests Verified on Hardware)
- **目標設備**：小米 Redmi 13C (Helio G85 / 720×1600 @ 90Hz / Android 14 HyperOS)
- **最後更新時間**：2026-10-02 22:12

---

## 🚀 當前階段目標 (Phase 1: 核心架構、單元測試與實機驗證) - 全部通過 ✅

- [x] **實機硬體探測與相容性體檢**
  - 確認 Redmi 13C (MT6769V Helio G85) 支援 90Hz 高刷螢幕模式。
  - 確認可用儲存空間 97 GB、記憶體 4 GB。
- [x] **版本控制與安全規範設定**
  - 初始化 Git 倉庫並關聯至 `https://github.com/Uyen666/UyenAndroidPlayer.git`。
  - 配置商業級 `.gitignore`（防護 Keystore、金鑰、本機環境設定）。
  - 建立專案規範 `README.md` 與狀態日誌 `Current.md`。
- [x] **單元測試 (Unit Tests)**
  - 建立 `UyenLauncherUnitTest.kt`（驗證 PlayerProfile、SystemStats、GameCategory、GameItem、VirtualGamepadState 狀態機）。
  - 執行 `./gradlew test` 全部通過（Debug & Release Unit Tests: 100% PASS）。
- [x] **閃退問題修復 (DecorView NPE Fix)**
  - 捕捉 Logcat 崩潰根因：Android 14 在 `onCreate()` 時 DecorView 尚未附加，呼叫 `window.insetsController` 導致 NullPointerException。
  - 重構 `SystemBarUtil.kt` 使用 AndroidX `WindowCompat`，並移至 View 初始化後與 `onWindowFocusChanged` 安全執行。
- [x] **實機安裝與執行驗證 (Xiaomi Redmi 13C)**
  - 透過 ADB 成功安裝最新修正版 APK。
  - 啟動成功！已直接在手機螢幕上呈現 **PS5 輪播主畫面、效能 HUD、玩家狀態列與自製手柄模式**。
  - 截圖驗證完成存檔至 `screen.png`。

---

## 📋 功能模組狀態一覽

| 模組名稱 | 狀態 | 驗證結果 |
| :--- | :--- | :--- |
| **Steam OS Boot Intro** | 🟢 運行正常 | 支援觸控點擊即跳過 |
| **PS5 輪播主介面 (Home Deck)** | 🟢 運行正常 | 實機流暢滑動、焦點卡片放大與發光邊框正常 |
| **Steam OS 應用庫 (App Library)** | 🟢 運行正常 | 點擊右上角「遊戲庫」即可呼出分類網格 |
| **Quick Settings 效能 HUD** | 🟢 運行正常 | 左上角顯示實時 FPS 與電池溫度 (實測 35.5°C) |
| **玩家儀表板 (Profile Bar)** | 🟢 運行正常 | 正上方即時同步頭像、稱號、電量與時鐘 |
| **UyenController (PC手柄模式)** | 🟢 運行正常 | 點擊即可進入虛擬搖桿操作與 UDP 發送 |
| **單元測試套件** | 🟢 全部通過 | JUnit 4 + Coroutines Test |

---

## 🔜 下一階段目標 (Phase 2: 深入擴展)
1. **內建免網路 8-bit Canvas 懷舊迷你遊戲**（放進專案 Assets 隨開隨玩）。
2. **PC 端極輕量接收端腳本 (UyenReceiver.py)**：將手機 UDP 按鍵映射為電腦虛擬 Xbox 360 手柄。
3. **ROM 與 Galgame 資料夾自動關聯設定**。
