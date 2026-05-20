package com.missyun.dailyportrait.data.video

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.missyun.dailyportrait.data.local.DailyPhoto
import com.missyun.dailyportrait.data.storage.FileManager
import com.missyun.dailyportrait.data.storage.MediaStoreSaver
import com.missyun.dailyportrait.data.storage.VideoOutputManager
import com.missyun.dailyportrait.domain.model.ExportConfig
import com.missyun.dailyportrait.domain.model.ExportState
import com.missyun.dailyportrait.domain.usecase.VideoExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 Media3 Transformer 的视频导出实现
 *
 * v2 修复：
 * - 图片型 EditedMediaItem 必须显式调 setFrameRate()，否则 start() 抛 NPE
 * - 用 Uri.fromFile(file) 取代 file.toURI()，保证 file:/// 三斜杠规范格式
 * - 异常打到 logcat 含完整 stack，方便定位
 */
@OptIn(UnstableApi::class)
@Singleton
class Media3VideoExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager,
    private val videoOutputManager: VideoOutputManager,
    private val mediaStoreSaver: MediaStoreSaver
) : VideoExporter {

    override fun export(
        photos: List<DailyPhoto>,
        config: ExportConfig
    ): Flow<ExportState> = callbackFlow {
        val tempFile = videoOutputManager.newTempOutput()

        // 1. 把每张照片转为 EditedMediaItem
        val editedItems = buildEditedMediaItems(photos, config)

        if (editedItems.isEmpty()) {
            trySend(ExportState.Failed("无法读取任何照片"))
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Starting export: ${editedItems.size} items, ${config.width}x${config.height}, fps=${config.frameRate}")

        // 2. 组装 Composition
        val sequence = EditedMediaItemSequence.Builder().run {
            editedItems.forEach { addItem(it) }
            build()
        }
        val composition = Composition.Builder(sequence).build()

        // 3. 创建 Transformer
        var hasFinished = false
        // 强制使用 H.264 (AVC) 编码 —— 几乎所有 Android 设备 / 模拟器都支持，
        // 避免 HEVC 在低端模拟器上不可用导致 codec exception
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(VideoEncoderSettings.NO_VALUE) // 让编码器自决定
                    .build()
            )
            .build()

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    hasFinished = true
                    Log.d(TAG, "Export completed")
                    launchSafe {
                        try {
                            // 1. 先把临时文件提交到沙盒永久目录
                            val finalPath = videoOutputManager.commit(tempFile)

                            // 2. 再 copy 一份到系统相册（让用户能在系统相册 App 里看到）
                            //    失败不影响主流程，只在 logcat 记录
                            val saveResult = mediaStoreSaver.saveToGallery(finalPath)
                            if (saveResult is MediaStoreSaver.Result.Failed) {
                                Log.w(TAG, "Save to gallery failed: ${saveResult.reason}")
                            } else {
                                Log.d(TAG, "Saved to gallery")
                            }

                            trySend(ExportState.Progress(100))
                            trySend(ExportState.Completed(finalPath))
                        } catch (e: Exception) {
                            Log.e(TAG, "Commit failed", e)
                            trySend(ExportState.Failed("提交视频失败：${e.message ?: e.javaClass.simpleName}"))
                        } finally {
                            close()
                        }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    hasFinished = true
                    Log.e(TAG, "Export error", exportException)
                    val msg = exportException.message
                        ?: exportException.cause?.message
                        ?: exportException.errorCodeName
                        ?: "未知错误"
                    trySend(ExportState.Failed("合成失败：$msg"))
                    launchSafe { videoOutputManager.cleanupTemp(tempFile) }
                    close()
                }
            })
            .build()

        // 4. 启动合成
        try {
            transformer.start(composition, tempFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Transformer.start failed", e)
            val msg = e.message
                ?: e.cause?.message
                ?: e.javaClass.simpleName
            trySend(ExportState.Failed("启动失败：$msg"))
            close()
            return@callbackFlow
        }

        // 5. 进度轮询协程
        val pollScope = CoroutineScope(Dispatchers.Main)
        pollScope.launch {
            try {
                while (isActive && !hasFinished) {
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val state = transformer.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        trySend(ExportState.Progress(progressHolder.progress.coerceIn(0, 99)))
                    }
                    delay(POLL_INTERVAL_MS)
                }
            } catch (_: Exception) {
                // 轮询本身出错不影响合成主流程
            }
        }

        // 6. 协程取消时调 transformer.cancel
        awaitClose {
            pollScope.cancel()
            // 只有"还没真正结束"时才算用户主动取消
            if (!hasFinished) {
                runCatching { transformer.cancel() }
                launchSafe { videoOutputManager.cleanupTemp(tempFile) }
                trySend(ExportState.Cancelled)
            }
            // 否则 onCompleted / onError 已经处理过了，这里只是 Flow 正常关闭
        }
    }.flowOn(Dispatchers.Main) // Transformer 必须在主线程

    /**
     * 把照片列表转为 EditedMediaItem 序列
     *
     * 关键点：
     * - 图片型 MediaItem 必须显式调 setFrameRate()，否则编码器报错
     * - 用 Uri.fromFile() 保证 file:/// 三斜杠格式
     * - Presentation 强制裁剪到目标分辨率（适配 9:16 竖屏）
     */
    private fun buildEditedMediaItems(
        photos: List<DailyPhoto>,
        config: ExportConfig
    ): List<EditedMediaItem> {
        val presentation = Presentation.createForWidthAndHeight(
            config.width,
            config.height,
            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
        )
        val videoEffects = Effects(emptyList(), listOf(presentation))

        return photos.mapNotNull { photo ->
            val absolute = fileManager.resolveAbsolutePath(photo.imagePath)
            val file = File(absolute)
            if (!file.exists()) {
                Log.w(TAG, "Photo file missing: $absolute")
                return@mapNotNull null
            }

            // Uri.fromFile 生成 file:///path 三斜杠规范格式，比 file.toURI() 更稳
            val uri: Uri = Uri.fromFile(file)
            val mediaItem = MediaItem.fromUri(uri)

            EditedMediaItem.Builder(mediaItem)
                .setDurationUs(config.frameDurationUs)
                // 图片型 EditedMediaItem 必须设 frameRate，否则 Transformer 不知道
                // 怎么把静态图扩展为视频帧 → start() 抛 NPE message=null
                .setFrameRate(config.frameRate)
                .setEffects(videoEffects)
                .build()
        }
    }

    private fun launchSafe(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { block() }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 200L
        const val TAG = "Media3VideoExporter"
    }
}
