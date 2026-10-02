# 🎮 UyenLauncher (UyenAndroidPlayer)

> **專為 Android 手機打造的掌機化系統啟動器**  
> 融合 **PS5 絲滑主介面**、**Steam OS 擴充體驗**、**Galgame / 復古遊戲中心**、**PC 虛擬手柄 (UyenController)** 與 **低延遲主機串流**。

---

## 🌟 核心特色 (Key Features)

- **🎬 Steam OS 開機與喚醒動畫 (Boot Video)**
  - 支援高畫質開機/解鎖影片動畫（支援自訂 MP4/WEBM 影片）。
  - 任意觸控螢幕立即無縫淡出跳過（Instant Tap-to-Skip），不拖沓遊戲啟動節奏。

- **✨ PS5 沉浸式主介面 (PS5-Style Deck)**
  - **90Hz 原生高刷新率支援**：搭配 Android Jetpack Compose 宣告式物理動畫引擎。
  - **下排遊戲卡片列**：具備磁吸輪播（Snap Carousel）、焦點放大（Scale 1.15x）、光澤高光與自訂反饋音效。
  - **中央 Hero Banner**：與卡片列即時聯動的高清封面海報、Logo、遊戲時數與一鍵 Play 大按鈕。
  - **背景動態景深**：選中遊戲時全螢幕平滑淡入該遊戲的高斯模糊環境壁紙。

- **📚 Steam OS 應用庫 (App Library)**
  - 右上角一鍵開啟全螢幕格狀視圖。
  - 多維度標籤分類：`全部 (All)`、`Galgame`、`復古 8-bit`、`自製作品`、`主機串流`、`系統工具`。
  - 即時模糊搜尋、自訂收藏與應用管理。

- **⚡ 效能監控與 Quick Settings (Performance HUD)**
  - 左上角展開半透明毛玻璃側邊抽屜。
  - **即時效能 HUD**：實時讀取 FPS、CPU 佔用率、RAM 可用量、電池溫度與充電瓦數。
  - **快速開關**：效能檔位切換 (節能 / 平衡 / 狂暴)、亮度、音量、手柄模式開關。

- **🎖️ 玩家儀表板 (Player Profile)**
  - 正上方專屬看板：自訂玩家名稱、專屬頭像、成就徽章展示。
  - 系統時鐘、Wi-Fi 信號與電池狀態即時同步。

- **🕹️ 觸控虛擬手柄 & UyenController (PC 搖桿模式)**
  - **無手柄輔助**：為復古遊戲與小遊戲提供高度自訂的半透明虛擬按鍵層。
  - **變身電腦手柄**：支援透過 Wi-Fi (極低延遲 UDP 8999 埠) 廣播，直接將手機化身為電腦的 Xbox 360 遊戲手柄。
  - **PC 端伴侶腳本**：內建 `scripts/uyen_controller_receiver.py`，支援 ViGEmBus / vgamepad 虛擬手柄驅動與封包調試。

- **👾 內建 8-bit 懷舊街機 (Uyen 8-Bit Cyber Strike)**
  - 純 Kotlin + Jetpack Compose Canvas 打造之 **90Hz 高刷流暢太空突擊遊戲**。
  - 無需網路、隨開隨玩，具備像素雙雷射、敵機 AI、爆炸粒子、波次遞增、最高分紀錄與復古 8-bit 音效。

- **🚀 全面遊戲相容中心 & 本地 ROM 掃描器 (LocalRomScanner)**
  - **目錄架構**：自動在手機儲存空間建立標準化路徑 `/sdcard/Games/Galgames/`、`/sdcard/Games/ROMs/`。
  - **檔案辨識**：自動掃描並載入 `.xp3`、`.rpa` (Galgame) 以及 `.nes`、`.gba`、`.sfc` (復古 ROM)。
  - **核心引導**：當未安裝 Tyranor、Kirikiroid2、RetroArch 或 Moonlight 時，彈出「掌機核心指引彈窗」，提供一鍵建立目錄與內建街機體驗。

- **🚢 專屬掌機 ConsoleDockBar & 純淨沉浸防護**
  - 右下角懸浮常駐導航：提供直覺的 `[返回]`、`[主頁]`、`[多工]`、`[加速]` 操作。
  - 徹底封鎖系統下拉通知列與 Android 邊緣返回手勢，支援 Kiosk 模式與真實 `RunningTask` 進程管理。

---

## 🏗️ 商業級專案架構 (Architecture)

本專案拒絕任何無效死碼與混亂結構，遵循 **Google Clean Architecture** 與 **MVI (Model-View-Intent)** 架構規範：

```
app/src/main/java/com/uyen/launcher/
├── core/                  # 底層核心模組 (網絡、硬體感測、協程調度)
│   ├── controller/        # UyenController 虛擬手柄與 UDP 廣播通訊
│   ├── hardware/          # 效能監控 (FPS, CPU, Battery, Thermal) & MemoryCleaner
│   ├── scanner/           # LocalRomScanner (自動掃描 SDCard/Games)
│   └── util/              # SoundManager (8-bit 音效合成)、SystemBarUtil (沉浸防護)
├── data/                  # 資料層 (Repository, 資料模型)
│   ├── model/             # 遊戲實體、玩家檔案、RunningTask 模型
│   └── repository/        # GameRepository (動態聚合安裝套件與 ROM 項目)
└── presentation/          # 表現層 (Jetpack Compose 現代化 UI)
    ├── controller/        # TouchGamepadOverlay (虛擬按鍵與類比搖桿)
    ├── home/              # PS5 主介面 (Hero Banner + 90Hz 卡片輪播)
    │   └── components/    # ConsoleDockBar, TaskSwitcher, Library, QuickSettings, EmulatorAssistant
    ├── minigame/          # RetroArcadeScreen (內建 8-bit 太空突擊街機引擎)
    ├── splash/            # Steam OS 開機動畫與跳過邏輯
    └── theme/             # 主題配色、玻璃擬物化質感、Typography
scripts/
├── requirements.txt       # PC 端依賴 (vgamepad)
└── uyen_controller_receiver.py # Windows PC 端 UDP 接收與虛擬 Xbox 360 手柄驅動腳本
```

---

## 📱 實機目標硬體規格

- **設備型號**：小米 Redmi 13C (Redmi 23108RN04Y)
- **處理器**：MediaTek Helio G85 (8-core, Mali-G52 GPU)
- **螢幕規格**：720 × 1600 @ **90Hz 高刷新率**
- **作業系統**：Android 14 (API Level 34) / Xiaomi HyperOS
- **記憶體 / 空間**：4 GB RAM / 128 GB ROM (約 97 GB 遊戲與素材可用空間)

---

## 🛠️ 開發與建置 (Build & Run)

### 前置需求
- **JDK**：OpenJDK 21 (推薦 Android Studio 內建 JBR)
- **Android SDK**：API 34+
- **Gradle**：8.11+
- **Android Studio**：Ladybug (2024.2+) 或更高版本

### 建置與安裝
```powershell
# 1. 設置環境變數
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\林尚楷\AppData\Local\Android\Sdk"

# 2. 執行單元測試 (Unit Tests: 100% PASS)
./gradlew test

# 3. 編譯 Debug APK
./gradlew assembleDebug

# 4. 安裝至已連線之手機
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 🎮 UyenController PC 接收端執行方式
```bash
cd scripts
pip install -r requirements.txt
python uyen_controller_receiver.py
```

---

## 📄 開源協議
MIT License © 2026 Uyen Team
