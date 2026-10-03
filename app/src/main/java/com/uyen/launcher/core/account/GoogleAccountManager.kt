package com.uyen.launcher.core.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.uyen.launcher.data.model.GoogleAccount

/**
 * 掌機真實 Google 帳號讀取與系統管理中心
 *
 * 1. 真實讀取設備上的 Google 帳號清單 (AccountManager)
 * 2. 支援切換多個 Google 帳號
 * 3. 支援一鍵跳轉系統原生「帳號與同步」與 Google 帳號管理中心
 * 4. 支援新增 Google 帳號
 */
object GoogleAccountManager {

    private const val TAG = "GoogleAccountManager"
    private const val PREFS_NAME = "uyen_google_account_prefs"
    private const val KEY_SAVED_EMAIL = "saved_google_email"

    /**
     * 讀取設備上所有已登入的 Google 帳號
     */
    fun getGoogleAccounts(context: Context): List<GoogleAccount> {
        val accountList = mutableListOf<GoogleAccount>()
        try {
            val am = AccountManager.get(context)
            val accounts: Array<Account> = am.getAccountsByType("com.google")

            for (acc in accounts) {
                val email = acc.name
                val displayPrefix = email.substringBefore("@")
                val capitalizedName = displayPrefix.replaceFirstChar { it.uppercase() }
                accountList.add(
                    GoogleAccount(
                        email = email,
                        displayName = capitalizedName,
                        avatarUrl = null,
                        isConnected = true,
                        cloudSyncStatus = "本機已同步 ($email)"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get google accounts: ${e.message}")
        }
        return accountList
    }

    /**
     * 取得當前作用中的 Google 帳號
     */
    fun getActiveGoogleAccount(context: Context): GoogleAccount {
        val accounts = getGoogleAccounts(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEmail = prefs.getString(KEY_SAVED_EMAIL, null)

        if (savedEmail != null) {
            val matched = accounts.find { it.email.equals(savedEmail, ignoreCase = true) }
            if (matched != null) return matched
        }

        if (accounts.isNotEmpty()) {
            return accounts.first()
        }

        // 若設備無 Google 帳號，顯示清晰的指引狀態
        return GoogleAccount(
            email = "請點擊登入 Google 帳號",
            displayName = "訪客",
            avatarUrl = null,
            isConnected = false,
            cloudSyncStatus = "尚未綁定 Google Play 遊戲帳號"
        )
    }

    /**
     * 儲存並切換使用者選取的 Google 帳號
     */
    fun saveActiveGoogleAccount(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVED_EMAIL, email).apply()
    }

    /**
     * 一鍵跳轉系統原生 Google 帳號管理或帳號同步設定
     */
    fun openManageAccountSettings(context: Context) {
        val intents = listOf(
            Intent("com.google.android.gms.accountsettings.ACCOUNT_PREFERENCES_ENTRANCE"),
            Intent(Settings.ACTION_SYNC_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    /**
     * 呼出系統原生「新增 Google 帳號」流程
     */
    fun openAddGoogleAccount(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openManageAccountSettings(context)
        }
    }

    /**
     * 開啟 Google Play 遊戲 (Google Play Games)
     */
    fun openPlayGames(context: Context) {
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.google.android.play.games")
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                openManageAccountSettings(context)
            }
        } catch (_: Exception) {
            openManageAccountSettings(context)
        }
    }
}
