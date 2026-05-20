package com.missyun.dailyportrait.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.missyun.dailyportrait.data.preferences.AppPreferences
import com.missyun.dailyportrait.domain.repository.PhotoRepository
import com.missyun.dailyportrait.domain.util.DateUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 拍照提醒接收器
 *
 * 触发时机：
 * - 系统时间到 [AppPreferences] 配置的提醒时刻
 * - 或开机后 [BootReceiver] 调度的次日提醒
 *
 * 处理流程：
 * 1. 检查"今天是否已打卡"——已打过就不打扰，省一次通知
 * 2. 未打卡 → 发本地通知
 * 3. 调度明天同一时间的下一次提醒（保证循环）
 *
 * 用 [AndroidEntryPoint] 让 Hilt 能注入 PhotoRepository / AppPreferences。
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var photoRepository: PhotoRepository

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                handleReminder(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context) {
        // 1. 读最新配置
        val config = preferences.reminderConfig()
        if (!config.enabled) return

        // 2. 检查今天是否已打卡
        val today = DateUtil.todayString()
        val alreadyShot = photoRepository.getByDate(today) != null

        // 3. 未打卡才发通知
        if (!alreadyShot) {
            showNotification(context)
        }

        // 4. 调度明天同一时间
        scheduler.schedule(config.hour, config.minute)
    }

    private fun showNotification(context: Context) {
        ReminderNotifier.show(context)
    }

    private fun ensureChannel(context: Context) {
        ReminderNotifier.ensureChannel(context)
    }

    companion object {
        const val ACTION_REMINDER = "com.missyun.dailyportrait.REMINDER"
        const val CHANNEL_ID = "daily_reminder"
        const val NOTIF_ID = 2001
    }
}
