package com.missyun.dailyportrait.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.missyun.dailyportrait.MainActivity
import com.missyun.dailyportrait.R
import com.missyun.dailyportrait.domain.model.ExportConfig
import com.missyun.dailyportrait.domain.model.ExportState
import com.missyun.dailyportrait.domain.usecase.GenerateVideoUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 视频合成前台服务
 *
 * 严格按 architecture-android.md §4.4：
 * - foregroundServiceType = "mediaProcessing"（Manifest 已声明）
 * - 通知栏含进度条 + 取消按钮
 * - onDestroy 调 transformer.cancel + 清理临时文件
 *
 * 与外部通信：
 * - 启动：[startService] / Intent.ACTION_START
 * - 取消：Intent.ACTION_CANCEL
 * - 状态广播：[stateFlow] 单例 SharedFlow，UI 层通过
 *   [com.missyun.dailyportrait.ui.screens.dashboard.VideoExportViewModel] 订阅
 *
 * 为何用 SharedFlow companion 而非绑定 Service：
 * - Bound Service 与 Compose 生命周期协调复杂
 * - 视频合成是单实例任务，全局 SharedFlow 更直接
 * - 重启 Service 前会 emit Idle，避免进度残影
 */
@AndroidEntryPoint
class VideoRenderService : Service() {

    @Inject
    lateinit var generateVideoUseCase: GenerateVideoUseCase

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var exportJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startExport(pendingConfig ?: ExportConfig())
            ACTION_CANCEL -> cancelExport()
        }
        // 系统杀死后不重启 —— 用户没看到进度通知就当作放弃
        return START_NOT_STICKY
    }

    private fun startExport(config: ExportConfig) {
        if (exportJob?.isActive == true) return // 防重复

        // 立即升级前台，绑定通知
        startForegroundCompat(buildNotification(progress = 0, indeterminate = true))

        emitState(ExportState.Preparing)

        // 标记本次合成是否已经达到终态，避免后续协程清理时误报失败
        var reachedTerminal = false

        exportJob = serviceScope.launch {
            try {
                generateVideoUseCase(config).collect { state ->
                    if (state is ExportState.Completed ||
                        state is ExportState.Failed ||
                        state is ExportState.Cancelled) {
                        reachedTerminal = true
                    }
                    handleState(state)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程取消是正常流程：Flow 自然结束 / 用户主动取消都会经过这里
                // 已经达到终态就静默退出，避免覆盖正确的最终状态
                if (!reachedTerminal) {
                    emitState(ExportState.Cancelled)
                }
                throw e
            } catch (e: Exception) {
                android.util.Log.e("VideoRenderService", "Export crashed", e)
                if (!reachedTerminal) {
                    emitState(ExportState.Failed(
                        e.message ?: e.cause?.message ?: e.javaClass.simpleName
                    ))
                }
                stopSelfSafely()
            }
        }
    }

    private fun cancelExport() {
        exportJob?.cancel()
        emitState(ExportState.Cancelled)
        stopSelfSafely()
    }

    private fun handleState(state: ExportState) {
        emitState(state)
        when (state) {
            is ExportState.Progress -> {
                updateNotification(buildNotification(progress = state.percent))
            }
            is ExportState.Completed -> {
                showFinalNotification(success = true, message = "延时视频已保存到相册")
                stopSelfSafely()
            }
            is ExportState.Failed -> {
                showFinalNotification(success = false, message = "合成失败：${state.reason}")
                stopSelfSafely()
            }
            is ExportState.Cancelled -> {
                showFinalNotification(success = false, message = "已取消")
                stopSelfSafely()
            }
            else -> Unit
        }
    }

    private fun stopSelfSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    /* ============ 通知 ============ */

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 必须显式声明前台服务类型
            startForeground(
                NOTIF_ID_PROGRESS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else {
            startForeground(NOTIF_ID_PROGRESS, notification)
        }
    }

    private fun updateNotification(notification: Notification) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_PROGRESS, notification)
    }

    private fun buildNotification(progress: Int, indeterminate: Boolean = false): Notification {
        ensureChannel()

        val cancelIntent = PendingIntent.getService(
            this, 0,
            Intent(this, VideoRenderService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
            .setContentTitle("正在生成延时视频")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, indeterminate)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelIntent)
            .build()
    }

    private fun showFinalNotification(success: Boolean, message: String) {
        ensureChannel()
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID_RESULT)
            .setContentTitle(if (success) "导出成功" else "导出失败")
            .setContentText(message)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIF_ID_RESULT, notif)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID_PROGRESS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_PROGRESS,
                    "视频合成进度",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        if (nm.getNotificationChannel(CHANNEL_ID_RESULT) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_RESULT,
                    "视频合成结果",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exportJob?.cancel()
        serviceScope.cancel()
    }

    /* ============ 状态广播 ============ */

    private fun emitState(state: ExportState) {
        _stateFlow.tryEmit(state)
    }

    companion object {
        const val ACTION_START = "com.missyun.dailyportrait.START_EXPORT"
        const val ACTION_CANCEL = "com.missyun.dailyportrait.CANCEL_EXPORT"

        const val CHANNEL_ID_PROGRESS = "video_render_progress"
        const val CHANNEL_ID_RESULT = "video_render_result"

        const val NOTIF_ID_PROGRESS = 1001
        const val NOTIF_ID_RESULT = 1002

        /**
         * 全局状态广播
         * UI 通过 [com.missyun.dailyportrait.ui.screens.videoexport.VideoExportViewModel]
         * 订阅此流获取实时进度
         */
        private val _stateFlow = MutableSharedFlow<ExportState>(
            replay = 1,
            extraBufferCapacity = 8
        )
        val stateFlow: SharedFlow<ExportState> get() = _stateFlow.asSharedFlow()

        /**
         * 当前一次启动用的合成参数
         * 通过单例传递避免 Parcelable 序列化（ExportConfig 含枚举 + 多种字段）
         */
        @Volatile
        private var pendingConfig: ExportConfig? = null

        /**
         * 启动合成（UI 层入口）
         * @param config 用户在设置面板中选择的参数
         */
        fun start(context: Context, config: ExportConfig = ExportConfig()) {
            pendingConfig = config
            // emit Idle 一次,清除上一次结果状态的"残影"
            _stateFlow.tryEmit(ExportState.Idle)
            val intent = Intent(context, VideoRenderService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 取消合成
         */
        fun cancel(context: Context) {
            val intent = Intent(context, VideoRenderService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}
