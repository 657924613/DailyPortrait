package com.missyun.dailyportrait.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.missyun.dailyportrait.data.preferences.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 开机后重新调度提醒
 *
 * Android 设备重启会清掉所有 AlarmManager 注册的 alarm，
 * 这个 Receiver 监听 BOOT_COMPLETED，在用户开机后读取配置并重新挂上 alarm。
 *
 * 也监听 ACTION_MY_PACKAGE_REPLACED 处理 App 升级覆盖安装的场景。
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                val config = preferences.reminderConfig()
                if (config.enabled) {
                    scheduler.schedule(config.hour, config.minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
