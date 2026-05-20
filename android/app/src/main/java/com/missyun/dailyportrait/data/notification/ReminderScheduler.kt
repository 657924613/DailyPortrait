package com.missyun.dailyportrait.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 拍照提醒调度器
 *
 * 每天到指定时间触发一次本地通知。
 *
 * 实现要点：
 * - 用 [AlarmManager.setExactAndAllowWhileIdle]（API 23+）保证准时，
 *   即使 Doze 模式下也能触发
 * - PendingIntent 携带 [ReminderReceiver] 作为目标
 * - 触发后 [ReminderReceiver] 自己再调度下一天 → 形成循环
 * - 用户关闭提醒 → [cancel] 取消挂起的 alarm
 *
 * Android 12+ 的注意事项：
 * - SCHEDULE_EXACT_ALARM 权限不再默认授予
 * - 需要在 Manifest 声明 USE_EXACT_ALARM（API 33+）或 SCHEDULE_EXACT_ALARM
 * - 用户也可以手动撤销
 * - 如果失败了我们降级用 setAndAllowWhileIdle（不那么精准但不需要权限）
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 调度下一次提醒
     *
     * @param hour 0~23
     * @param minute 0~59
     */
    fun schedule(hour: Int, minute: Int) {
        val triggerAt = nextTriggerMillis(hour, minute)
        val pendingIntent = buildPendingIntent()

        try {
            // Android 12+ 检查能不能调度精确闹钟
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true

            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                // 降级方案：不精确但 Doze 友好
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
            Log.d(TAG, "Reminder scheduled at $triggerAt ($hour:$minute)")
        } catch (e: SecurityException) {
            // 用户在系统设置里撤销了精确闹钟权限，降级
            Log.w(TAG, "Exact alarm denied, fallback", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    /**
     * 取消已挂起的提醒
     */
    fun cancel() {
        alarmManager.cancel(buildPendingIntent())
        Log.d(TAG, "Reminder cancelled")
    }

    /**
     * 计算下一次触发时间戳（毫秒）
     * - 如果今天的 H:M 还没过 → 用今天
     * - 已过 → 用明天
     */
    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private companion object {
        const val TAG = "ReminderScheduler"
        const val REQUEST_CODE = 9101
    }
}
