# 📌 UyenLauncher 項目進度追蹤記錄 (Current.md)

- **專案名稱**：UyenLauncher (UyenAndroidPlayer)
- **遠端倉庫**：`https://github.com/Uyen666/UyenAndroidPlayer.git`
- **當前版本**：v0.1.0-alpha (Scaffolding & Core Architecture)
- **目標設備**：小米 Redmi 13C (Helio G85 / 720×1600 @ 90Hz / Android 14 HyperOS)
- **最後更新時間**：2026-10-02

---

## 🚀 當前階段目標 (Current Sprint: Phase 1)

- [x] **實機硬體探測與相容性體檢**
  - 確認 Redmi 13C (MT6769V Helio G85) 具備 90Hz 高刷螢幕模式。
  - 確認可用儲存空間 97 GB、記憶體 4 GB。
- [x] **版本控制與安全規範設定**
  - 初始化 Git 倉庫並關聯至 `https://github.com/Uyen666/UyenAndroidPlayer.git`。
  - 配置商業級 `.gitignore`（防護 Keystore、金鑰、本機環境設定）。
  - 建立專案規範 `README.md` 與狀態日誌 `Current.md`。
- [ ] **Android 原生骨架建置 (Clean Architecture + Compose)**
  - 建立標準 Gradle Android 專案結構（Kotlin 2.0 + Compose Material 3）。
  - 註冊為 Android Home Launcher（具備預設桌面能力與全螢幕沉浸模式）。
- [ ] **Steam OS 開機動畫與即時跳過**
  - 實作自訂影片播放器與流暢淡出跳過手勢。
- [ ] **PS5 絲滑主介面原型**
  - 90Hz 水平輪播卡片列（選中放大、磁吸對齊）。
  - 中央 Hero Banner 動態海報連動。
  - 頂部狀態列（左上效能 HUD、正中玩家、右上 App Library）。

---

## 📋 功能模組開發狀態總覽

| 模組名稱 | 規劃狀態 | 技術重點 |
| :--- | :--- | :--- |
| **Steam OS Boot Intro** | 🟡 進行中 | ExoPlayer / TextureView + 任意點擊淡出跳過 |
| **PS5 輪播主介面 (Home Deck)** | 🟡 進行中 | Compose HorizontalPager + 90Hz 物理縮放動畫 + 背景模糊壁紙 |
| **Steam OS 應用庫 (App Library)** | ⚪ 待開發 | LazyVerticalGrid + 分類標籤篩選 + 模糊搜尋 |
| **Quick Settings 效能 HUD** | ⚪ 待開發 | 讀取系統 `/sys/class/thermal`、BatteryManager、FPS 測量 |
| **玩家儀表板 (Profile Bar)** | ⚪ 待開發 | 自訂稱號、成就徽章、即時時鐘與電量 |
| **Galgame 遊戲中心** | ⚪ 待開發 | 掃描本機資料夾、Tyranor/KRKR2/Ren'Py 意圖調用 |
| **8-bit / 復古遊戲模擬器** | ⚪ 待開發 | 復古核心調用 + 內建精選小遊戲沙盒 |
| **UyenController (PC手柄模式)** | ⚪ 待開發 | 虛擬搖桿 UI + Wi-Fi UDP (XInput) / 藍牙 HID 通訊 |
| **主機串流快捷通道** | ⚪ 待開發 | Moonlight / Steam Link 深度整合 |

---

## 🔒 安全性與私密資料守則
1. **嚴禁提交敏感憑證**：包括 `.jks` 簽名金鑰、`local.properties`、第三方 API Key。
2. **乾淨代碼原則**：全專案嚴禁無效死碼（Dead Code）、未使用的 Import、魔術數字與面條式架構。
3. **記憶體指標**：Launcher 常駐 RAM 嚴格限制在 120MB 以內，確保遊戲時有最大系統可用記憶體。
