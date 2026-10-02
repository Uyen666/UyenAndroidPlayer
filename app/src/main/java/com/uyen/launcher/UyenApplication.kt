package com.uyen.launcher

import android.app.Application

/**
 * UyenLauncher Application Entry Point
 * 掌機啟動器全域 Application，負責生命週期與底層服務初始化
 */
class UyenApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: UyenApplication
            private set
    }
}
