package com.missyun.dailyportrait.data.storage

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视频输出文件管理
 *
 * 与 [FileManager]（图片）分开管理，原因：
 * - 视频文件较大（数 MB ~ 数十 MB），独立目录便于清理
 * - 临时合成文件（.tmp）用 cache 目录，App 退出时系统可能清理；
 *   最终视频用 files 子目录长期保留
 *
 * 严格遵循 architecture-android.md：
 * - 沙盒路径，不需要存储权限
 * - 主线程禁用 IO，所有方法 suspend
 */
@Singleton
class VideoOutputManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** 视频最终输出目录：getExternalFilesDir(Movies)/dailyportrait/ */
    private val videoDir: File
        get() {
            val root = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "movies")
            val sub = File(root, SUBDIR)
            if (!sub.exists()) sub.mkdirs()
            return sub
        }

    /** 临时合成文件目录：cache/dailyportrait_temp/ */
    private val tempDir: File
        get() {
            val sub = File(context.cacheDir, "dailyportrait_temp")
            if (!sub.exists()) sub.mkdirs()
            return sub
        }

    /**
     * 生成唯一的临时输出文件路径
     */
    suspend fun newTempOutput(): File = withContext(Dispatchers.IO) {
        val name = "video_${System.currentTimeMillis()}.tmp.mp4"
        File(tempDir, name)
    }

    /**
     * 把临时文件提交为最终视频
     * @return 最终绝对路径
     */
    suspend fun commit(temp: File): String = withContext(Dispatchers.IO) {
        val finalName = "DailyPortrait_${System.currentTimeMillis()}.mp4"
        val finalFile = File(videoDir, finalName)
        if (!temp.renameTo(finalFile)) {
            // 跨设备 rename 可能失败，回退到拷贝
            temp.copyTo(finalFile, overwrite = true)
            temp.delete()
        }
        finalFile.absolutePath
    }

    /**
     * 删除临时文件（取消 / 失败时清理，失败时不抛异常）
     */
    suspend fun cleanupTemp(temp: File): Boolean = withContext(Dispatchers.IO) {
        runCatching { temp.delete() }.getOrDefault(false)
    }

    /**
     * 清理所有遗留临时文件（启动时可调用）
     */
    @Suppress("unused")
    suspend fun cleanupAllTemp() = withContext(Dispatchers.IO) {
        tempDir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val SUBDIR = "videos"
    }
}
