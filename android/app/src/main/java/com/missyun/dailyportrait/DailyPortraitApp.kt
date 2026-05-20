package com.missyun.dailyportrait

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 入口
 *
 * [HiltAndroidApp] 触发 Hilt 代码生成。
 *
 * Sentry 崩溃监控暂时关闭中（待你按 SENTRY-SETUP.md 启用后再恢复）。
 * 启用方法：
 * 1. build.gradle.kts 取消注释 sentry.android 依赖
 * 2. 取消注释下方 initSentryIfConfigured()
 * 3. 创建 sentry.properties 填入 DSN
 */
@HiltAndroidApp
class DailyPortraitApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // initSentryIfConfigured()
    }

    /*
    private fun initSentryIfConfigured() {
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isBlank()) return
        try {
            io.sentry.android.core.SentryAndroid.init(this) { options ->
                options.dsn = dsn
                options.isDebug = BuildConfig.DEBUG
                options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2
                options.isSendDefaultPii = false
                options.release = "${packageName}@${BuildConfig.VERSION_NAME}"
                options.environment = if (BuildConfig.DEBUG) "debug" else "release"
            }
        } catch (_: Exception) {
            // 静默失败，不影响 App 启动
        }
    }
    */
}
