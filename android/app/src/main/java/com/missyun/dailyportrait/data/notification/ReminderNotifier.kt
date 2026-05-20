package com.missyun.dailyportrait.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.missyun.dailyportrait.MainActivity

/**
 * 提醒通知发送器
 *
 * 把通知构建/渠道创建逻辑抽出来，让 [ReminderReceiver]（定时触发）
 * 和 SettingsViewModel（用户主动测试）共用一份代码
 */
object ReminderNotifier {

    const val CHANNEL_ID = "daily_reminder"
    const val NOTIF_ID = 2001

    /**
     * 立即发送一条提醒通知
     */
    fun show(context: Context, isTest: Boolean = false) {
        ensureChannel(context)

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isTest) "测试通知 · DailyPortrait" else "该拍今天的肖像啦"
        val content = if (isTest) "提醒功能正常工作" else "用一张照片记录今天的自己"

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "每日拍照提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "提醒你每天定时记录自己的肖像"
                }
            )
        }
    }
}
