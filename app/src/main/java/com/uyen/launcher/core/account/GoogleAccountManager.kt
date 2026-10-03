package com.uyen.launcher.core.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.uyen.launcher.data.model.GoogleAccount
import java.io.File

/**
 * 掌機 Google 帳號管理、系統授權選取器與頭像照片同步中心
 */
object GoogleAccountManager {

    private const val PREFS_NAME = "uyen_google_account_prefs"
    private const val KEY_SAVED_EMAIL = "saved_google_email"
    private const val KEY_SAVED_ACCOUNTS_SET = "saved_google_accounts_set"
    private const val KEY_AVATAR_PREFIX = "avatar_url_"

    /**
     * 讀取指定帳號的頭像照片（優先順序：本地快照檔案 -> 儲存的 URI/URL -> 訪客後備）
     */
    fun getAvatarForAccount(context: Context, email: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trimmed = email.trim()
        val emailKey = trimmed.lowercase()

        // 1. 本地沙盒永久快照檔案
        val localFile = File(context.filesDir, "avatar_${emailKey.hashCode()}.png")
        if (localFile.exists()) {
            return Uri.fromFile(localFile).toString()
        }

        // 2. 儲存於 SharedPreferences 的 URL 或 URI
        val saved = prefs.getString(KEY_AVATAR_PREFIX + emailKey, null)
        if (!saved.isNullOrBlank()) {
            return saved
        }

        // 3. 若為訪客或尚未登入，檢查 fallback 頭像
        if (emailKey == "尚未登入" || emailKey.isBlank() || emailKey == "guest") {
            val guestFile = File(context.filesDir, "avatar_${"guest".hashCode()}.png")
            if (guestFile.exists()) return Uri.fromFile(guestFile).toString()

            val unloggedFile = File(context.filesDir, "avatar_${"尚未登入".hashCode()}.png")
            if (unloggedFile.exists()) return Uri.fromFile(unloggedFile).toString()

            val guestSaved = prefs.getString(KEY_AVATAR_PREFIX + "guest", null)
                ?: prefs.getString(KEY_AVATAR_PREFIX + "尚未登入", null)
            if (!guestSaved.isNullOrBlank()) return guestSaved
        }

        return null
    }

    /**
     * 取得設備上所有可用的 Google 帳號清單（包含系統 AccountManager、GoogleSignIn 與曾選取之帳號）
     */
    fun getGoogleAccounts(context: Context): List<GoogleAccount> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accountsMap = mutableMapOf<String, GoogleAccount>()

        // 1. 嘗試從系統 AccountManager 讀取設備上已登入的 Google 帳號
        runCatching {
            val am = AccountManager.get(context)
            val accounts: Array<Account> = am.getAccountsByType("com.google")
            for (acc in accounts) {
                val email = acc.name
                if (!email.isNullOrBlank()) {
                    val key = email.lowercase()
                    val avatar = getAvatarForAccount(context, email)
                    val prefix = email.substringBefore("@")
                    val displayName = prefix.replaceFirstChar { it.uppercase() }
                    accountsMap[key] = GoogleAccount(
                        email = email,
                        displayName = displayName,
                        avatarUrl = avatar,
                        isConnected = true,
                        dataStatusMessage = "Google 帳號已連結 ($email)"
                    )
                }
            }
        }

        // 2. 嘗試從 GoogleSignIn 快取中讀取
        runCatching {
            GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                val email = account.email.orEmpty()
                if (email.isNotBlank()) {
                    val key = email.lowercase()
                    val avatar = getAvatarForAccount(context, email) ?: account.photoUrl?.toString()
                    val displayName = account.displayName ?: email.substringBefore('@').ifBlank { "Google 使用者" }
                    accountsMap[key] = GoogleAccount(
                        email = email,
                        displayName = displayName,
                        avatarUrl = avatar,
                        isConnected = true,
                        dataStatusMessage = "Google 帳號已連結 ($email)"
                    )
                }
            }
        }

        // 3. 讀取使用者先前選取或儲存的帳號
        val savedAccounts = prefs.getStringSet(KEY_SAVED_ACCOUNTS_SET, emptySet()).orEmpty()
        val savedActive = prefs.getString(KEY_SAVED_EMAIL, null)
        val allKnown = (savedAccounts + listOfNotNull(savedActive)).filter { it.isNotBlank() && it != "尚未登入" }

        for (email in allKnown) {
            val key = email.lowercase()
            if (!accountsMap.containsKey(key)) {
                val avatar = getAvatarForAccount(context, email)
                val prefix = email.substringBefore("@")
                val displayName = prefix.replaceFirstChar { it.uppercase() }
                accountsMap[key] = GoogleAccount(
                    email = email,
                    displayName = displayName,
                    avatarUrl = avatar,
                    isConnected = true,
                    dataStatusMessage = "Google 帳號已連結 ($email)"
                )
            }
        }

        return accountsMap.values.toList()
    }

    /**
     * 儲存指定帳號的頭像照片 (支援本機相簿 Uri 或 Google 雲端照片 Url)
     */
    fun saveAvatarUrl(context: Context, email: String, avatarUrl: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = email.trim().lowercase()
        prefs.edit().apply {
            if (avatarUrl != null) {
                putString(KEY_AVATAR_PREFIX + key, avatarUrl)
            } else {
                remove(KEY_AVATAR_PREFIX + key)
            }
        }.apply()
    }

    /**
     * 從 Content Uri 或 相簿 Uri 複製並持久化為本機檔案 (永久不會失效、離線可用)
     */
    fun saveCustomAvatarFromUri(context: Context, email: String, sourceUri: Uri?): String? {
        val trimmed = email.trim()
        val key = if (trimmed.isBlank() || trimmed == "尚未登入") "guest" else trimmed.lowercase()
        val targetFile = File(context.filesDir, "avatar_${key.hashCode()}.png")

        if (sourceUri == null) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            saveAvatarUrl(context, key, null)
            if (key == "guest") {
                saveAvatarUrl(context, "尚未登入", null)
                File(context.filesDir, "avatar_${"尚未登入".hashCode()}.png").delete()
            }
            return null
        }

        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val localUri = Uri.fromFile(targetFile).toString()
            saveAvatarUrl(context, key, localUri)
            if (key == "guest") {
                saveAvatarUrl(context, "尚未登入", localUri)
            }
            localUri
        } catch (_: Exception) {
            saveAvatarUrl(context, key, sourceUri.toString())
            if (key == "guest") {
                saveAvatarUrl(context, "尚未登入", sourceUri.toString())
            }
            sourceUri.toString()
        }
    }

    /**
     * 構建系統原生 Google 帳號選取器 Intent (零設定、原生相容所有已登入 Google 帳號)
     */
    fun getChooseAccountIntent(): Intent {
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
    }

    /**
     * 構建 Google Sign-In 用於獲取高畫質頭像照片的登入 Intent
     */
    fun getGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    /**
     * 解析 Google Sign-In 回傳的成果並儲存頭像照片
     */
    fun handleGoogleSignInResult(context: Context, data: Intent?): String? {
        if (data == null) return null
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val photoUrl = account.photoUrl?.toString()
            val email = account.email ?: getActiveGoogleAccount(context).email
            if (photoUrl != null) {
                saveAvatarUrl(context, email, photoUrl)
            }
            saveActiveGoogleAccount(context, email)
            photoUrl
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 取得當前作用中的 Google 帳號
     */
    fun getActiveGoogleAccount(context: Context): GoogleAccount {
        val accounts = getGoogleAccounts(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEmail = prefs.getString(KEY_SAVED_EMAIL, null)

        if (!savedEmail.isNullOrBlank() && savedEmail != "尚未登入") {
            val matched = accounts.find { it.email.equals(savedEmail, ignoreCase = true) }
            if (matched != null) return matched

            val avatar = getAvatarForAccount(context, savedEmail)
            val prefix = savedEmail.substringBefore("@")
            val displayName = prefix.replaceFirstChar { it.uppercase() }
            return GoogleAccount(
                email = savedEmail,
                displayName = displayName,
                avatarUrl = avatar,
                isConnected = true,
                dataStatusMessage = "Google 帳號已連結 ($savedEmail)"
            )
        }

        if (accounts.isNotEmpty()) {
            return accounts.first()
        }

        // 訪客／未登入狀態，檢查是否有為訪客或未登入設定的自訂頭像相片
        val guestAvatar = getAvatarForAccount(context, "guest")
            ?: getAvatarForAccount(context, "尚未登入")

        return GoogleAccount(
            email = "尚未登入",
            displayName = "訪客",
            avatarUrl = guestAvatar,
            isConnected = false,
            dataStatusMessage = "遊戲資料保存在本機；此版本沒有雲端存檔功能"
        )
    }

    /**
     * 儲存並切換使用者選取的 Google 帳號
     */
    fun saveActiveGoogleAccount(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_SAVED_ACCOUNTS_SET, emptySet()).orEmpty().toMutableSet()
        if (email.isNotBlank() && email != "尚未登入") {
            currentSet.add(email)
        }
        prefs.edit()
            .putString(KEY_SAVED_EMAIL, email)
            .putStringSet(KEY_SAVED_ACCOUNTS_SET, currentSet)
            .apply()
    }

    /**
     * 登出當前帳號並切換回訪客模式
     */
    fun logoutGoogleAccount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SAVED_EMAIL).apply()
        runCatching {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(context, gso).signOut()
        }
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
