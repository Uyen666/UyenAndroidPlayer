# UyenLauncher

UyenLauncher 是以 Kotlin、Jetpack Compose 建置的 Android 橫向遊戲啟動器。它提供掌機風格首頁、已安裝 App 遊戲庫、本機遊戲檔案匯入、系統控制抽屜、內建 8-bit 小遊戲，以及選用的 Kiosk 與邊緣快捷列。

## 功能

- **遊戲首頁與遊戲庫**：列出內建功能、可啟動的已安裝 App，以及使用者選取資料夾中的 `.xp3`、`.rpa`、`.ons`、`.nes`、`.fc`、`.gba`、`.sfc` 和 `.smc` 檔案。
- **本機資料**：首頁釘選、收藏、遊戲海報設定與實際啟動遊玩時間保存在 App 私有偏好資料中。
- **資料夾匯入**：使用 Android 系統文件選擇器授權遊戲資料夾；啟動器只讀取使用者選取的資料夾，不要求整機儲存空間存取權。
- **掌機介面**：Compose 遊戲卡片、遊戲庫、媒體音量與視窗亮度控制，以及電池與記憶體資訊。
- **內建街機與控制器畫面**：離線 8-bit 小遊戲和觸控手柄介面。
- **選用系統整合**：Kiosk 鎖定與跨 App 邊緣快捷列需要使用者授權；部分鎖定能力需要 Device Owner 設定及相容裝置。
- **Google 登入**：用於顯示目前登入的 Google 個人資料與頭像。遊戲庫和遊玩紀錄目前保存在本機，尚未提供雲端存檔同步。

## 架構

```text
app/src/main/java/com/uyen/launcher/
├── core/                  # 系統整合、文件掃描、效能讀取、海報與音效
├── data/
│   ├── model/             # 遊戲、帳號與畫面狀態模型
│   └── repository/        # 已安裝 App、本機遊戲及本機遊戲庫偏好
└── presentation/          # Activity 與 Jetpack Compose 畫面
scripts/                   # UyenController 電腦端 UDP 接收器
```

遊戲資料由 `GameRepository` 聚合；文件存取經由 Storage Access Framework 的 URI 權限，不使用全碟掃描。畫面狀態由 `HomeViewModel` 提供給 Compose。

## 建置

需求：Android Studio、JDK 21、Android SDK 35。

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 會輸出到 `app/build/outputs/apk/debug/app-debug.apk`。PC 端接收器位於 `scripts/uyen_controller_receiver.py`，依賴列於 `scripts/requirements.txt`。

## 隱私與版本管理

- 不要求讀取聯絡人、整機帳號清單或所有外部儲存資料。
- 遊戲資料夾由使用者透過系統選擇器授權，授權 URI 保存在 App 私有偏好中。
- Google 登入只讀取使用者在此 App 授權的個人資料；目前不會上傳遊戲庫或遊玩時間。
- Android Auto Backup 已關閉；裝置移轉行為仍可能依 Android/OEM 實作而異。
- 本機 SDK 路徑、簽章金鑰、服務設定與環境檔應留在 Git 忽略清單內。提交前請檢查 `git status`，勿提交裝置資料、憑證或個人匯出檔。
