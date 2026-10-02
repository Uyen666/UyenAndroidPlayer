# 📌 UyenLauncher 項目進度追蹤記錄 (Current.md)

- **專案名稱**：UyenLauncher (UyenAndroidPlayer)
- **遠端倉庫**：`https://github.com/Uyen666/UyenAndroidPlayer.git`
- **當前版本**：v0.1.0-alpha (Core Architecture & PS5 Handheld UI Implemented)
- **目標設備**：小米 Redmi 13C (Helio G85 / 720×1600 @ 90Hz / Android 14 HyperOS)
- **最後更新時間**：2026-10-02

---

## 🚀 當前階段目標 (Phase 1: 核心架構與掌機介面) - 已完成 ✅

- [x] **實機硬體探測與相容性體檢**
  - 確認 Redmi 13C (MT6769V Helio G85) 具備 90Hz 高刷螢幕模式。
  - 確認可用儲存空間 97 GB、記憶體 4 GB。
- [x] **版本控制與安全規範設定**
  - 初始化 Git 倉庫並關聯至 `https://github.com/Uyen666/UyenAndroidPlayer.git`。
  - 配置商業級 `.gitignore`（防護 Keystore、金鑰、本機環境設定）。
  - 建立專案規範 `README.md` 與狀態日誌 `Current.md`。
- [x] **Android 原生骨架建置 (Clean Architecture + Compose)**
  - 建立標準 Gradle Android 專案結構（Kotlin 2.0 + Compose Material 3）。
  - 註冊為 Android Home Launcher（具備預設桌面能力與全螢幕沉浸模式）。
- [x] **Steam OS 開機動畫與即時跳過 (Boot Screen)**
  - 實作旋轉弧光科技感動畫，支援任意點擊螢幕無縫淡出跳過。
- [x] **PS5 絲滑主介面 (PS5 Deck)**
  - 90Hz 物理彈簧縮放輪播卡片（選中放大 1.1x，帶發光藍色邊框與陰影）。
  - 中央 Hero Banner 動態海報連動與「開始遊戲」按鈕。
  - 頂部狀態列（左上效能 HUD、正中玩家、右上 App Library）。
- [x] **Steam OS 應用庫視窗 (App Library Dialog)**
  - 分類標籤：`全部`、`Galgame`、`復古 8-bit`、`自製作品`、`主機串流`、`系統工具`。
  - 支援即時搜尋與網格展示。
- [x] **Quick Settings 效能監控側邊抽屜 (Performance HUD)**
  - 即時 FPS、電池溫度 (°C)、RAM 使用量、電量狀態顯示。
  - 支援快速切換虛擬手柄懸浮層。
- [x] **Touch Gamepad Overlay & UyenController (PC 手柄模式)**
  - 虛擬搖桿、ABXY、L1/R1/L2/R2 按鈕組。
  - 具備低延遲 UDP 通訊器，可直接將按鍵廣播給電腦接收器。
- [x] **編譯與安裝打包**
  - Gradle `assembleDebug` 編譯通過 (零報錯)。
  - APK 已推播至手機儲存空間：`/sdcard/Download/UyenLauncher.apk`。

---

## 📋 功能模組開發狀態總覽

| 模組名稱 | 規劃狀態 | 技術重點 |
| :--- | :--- | :--- |
| **Steam OS Boot Intro** | 🟢 已完成 | Canvas 科技弧光 + 任意點擊即時跳過 |
| **PS5 輪播主介面 (Home Deck)** | 🟢 已完成 | Compose LazyRow + 90Hz 物理縮放動畫 + 背景動態景深 |
| **Steam OS 應用庫 (App Library)** | 🟢 已完成 | LazyVerticalGrid + 分類標籤篩選 + 模糊搜尋 |
| **Quick Settings 效能 HUD** | 🟢 已完成 | 實時採樣 BatteryManager、ActivityManager、Choreographer FPS |
| **玩家儀表板 (Profile Bar)** | 🟢 已完成 | 自訂稱號、成就徽章、即時時鐘與電量 |
| **Galgame 遊戲中心** | 🟢 已架構 | 支援 Tyranor / Kirikiroid2 / Ren'Py / WebGL 意圖調用 |
| **8-bit / 復古遊戲模擬器** | 🟢 已架構 | 復古核心調用與遊戲預置清單 |
| **UyenController (PC手柄模式)** | 🟢 已完成 | 虛擬搖桿 UI + Wi-Fi UDP (XInput) 通訊協定 |
| **主機串流快捷通道** | 🟢 已架構 | Moonlight / Steam Link 深度整合 |

---

## 🔜 下一階段目標 (Phase 2: 深入擴展)
1. **內建 8-bit 迷你小遊戲沙盒**：在 Assets 加入一兩款免網路直接可玩的 8-bit Canvas 懷舊小遊戲。
2. **PC 端接收程式 (UyenReceiver.py)**：提供單檔極輕量 Python 接收器，將手機 UDP 轉為虛擬 Xbox 360 手柄。
3. **HyperOS 掌機模式專屬優化腳本**：一鍵凍結小米背景雜訊進程。
