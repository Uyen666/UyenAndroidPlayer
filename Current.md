# UyenLauncher 專案現況

- **套件版本**：`1.0.1`（`versionCode 2`）
- **GitHub**：https://github.com/Uyen666/UyenAndroidPlayer
- **更新日期**：2026-10-03
- **主要技術**：Kotlin、Jetpack Compose、Android SDK 35
- **目標裝置**：Redmi 13C / Android 14，橫向使用
- **Git 分支**：`main`；現有 GitHub remote 與忽略規則保留

## 目前已實作

- Compose 掌機首頁、分頁、遊戲輪播、遊戲庫與自訂海報。
- 遊戲來源包含兩項內建功能、透過 Android Launcher 查詢可啟動 App，以及使用者選取文件資料夾中的支援遊戲檔案。
- ROM/Galgame 掃描使用 Storage Access Framework；讀取權限限於使用者選取並授權的資料夾。
- 首頁釘選、收藏、自訂海報與遊玩時間保存在 App 私有本機偏好資料。
- 遊戲啟動時間會在返回啟動器時累計；最近啟動清單只是啟動器紀錄，不代表 Android 的完整背景程序清單。
- 系統控制抽屜、效能/電池資訊、Kiosk 管理、邊緣快捷列、Google 登入個人資料、離線 8-bit 街機與觸控手柄畫面。
- 全系統掌機系統控制台完全統一：無論從首頁還是從跨應用邊緣膠囊（`GlobalConsoleEdgeService`）呼出，均共享同套 Compose UI（`QuickSettingsDrawer`），呈現完全一致的視覺佈局與功能。
- 真實硬體記憶體釋放與優化管理器（`SystemMemoryManager`）：徹底告別展示假功能，真正採樣 Linux 核心 `ActivityManager.MemoryInfo` 前後真實可用 RAM 差值、終止目標背景應用（含 YouTube、Chrome 等預裝重度用戶應用）、清空 Coil 高解析圖片記憶體池與 GC 碎片整理，零時差刷新 RAM 監控卡片與中央頂部 HUD，動態回報清出之 MB 數與可用 RAM。
- 全局常駐邊緣快捷：右下角收納導航膠囊（主頁/返回/多工/設定），以及中央頂部微型效能 HUD（跨所有 App 全局懸浮展示 FPS、電池溫度、RAM 與電量，觸控完全穿透底層遊戲）。
- 控制台抽屜提供「🚪 退出 Uyen 啟動器」功能，作為系統唯一正規退出啟動器的入口，具備就地確認彈窗並引導切換回原生桌面。
- `.gitignore` 完善排除 SDK 本機設定、簽章檔、服務憑證、環境檔、編譯產物（`*.jar`, `*.apk`）與私人裝置匯出資料。

## 目前界線

- Google 登入只顯示授權的個人資料與頭像；雲端存檔同步尚未實作，遊戲庫資料留在本機。
- 已選取資料夾中的遊戲可交由 Android 匹配的 App 開啟；不同模擬器支援的 MIME 類型不一，未必都能直接接受每一種格式。
- 遊戲分類依套件名稱推測；尚未提供完整的手動分類、遊戲庫匯出/匯入或資料庫遷移工具。
- 多工視窗優先透過原生 AccessibilityService 呼出系統原生 Recents，亦提供啟動器最近開啟項目的就地切換。
- CPU 使用率尚未提供；效能 HUD 以頂部中央極簡微型膠囊列全局懸浮呈現（實時顯示畫面影格率、電池溫度、全機 RAM 與電量/充電狀態），可於掌機控制台隨時開啟或隱藏，觸控完全穿透至遊戲中。
- Kiosk/Device Owner 與跨 App 覆蓋能力依 Android 版本、OEM 與使用者授權而異。

## 待辦方向

1. 增加遊戲庫本機匯出/匯入與偏好設定版本遷移。
2. 加入手動分類與模擬器關聯設定，改善各 ROM 格式的啟動相容性。
3. 為正式雲端同步設計明確的使用者授權、衝突處理與資料刪除流程，再接入雲端服務。
4. 補齊裝置權限與 Kiosk 相容性說明，並建立可重複的實機驗證流程。

## 版本與驗證

目前工作在既有 `main` 分支上，沒有建立新分支或變更遠端設定。`assembleDebug` 與 `testDebugUnitTest` 均驗證通過（單元測試 23/23 項 100% 通過）。
