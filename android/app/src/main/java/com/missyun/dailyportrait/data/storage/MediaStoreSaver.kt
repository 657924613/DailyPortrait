package com.missyun.dailyportrait.data.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 把视频文件复制一份到系统媒体库（让用户能在系统相册 App 里看到）
 *
 * 平台差异：
 * - Android 10+ (API 29)：用 MediaStore.Video API，无需任何权限
 *   插入到 Movies/DailyPortrait/ 子目录
 * - Android 9 及以下：需要 WRITE_EXTERNAL_STORAGE 权限（已声明 maxSdkVersion=28）
 *   直接写到 /storage/emulated/0/Movies/DailyPortrait/
 *
 * 沙盒文件保留：copy 到媒体库后，原沙盒文件不删除
 * - 万一用户清空相册，App 内仍然能播放
 * - 重新合成视频时不需要再次跑 Media3
 */
@Singleton
class MediaStoreSaver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 把沙盒里的视频文件保存到系统媒体库
     *
     * @param sandboxAbsolutePath 沙盒视频绝对路径
     * @return Pair(success, mediaStoreUri 或 错误消息)
     */
    suspend fun saveToGallery(sandboxAbsolutePath: String): Result = withContext(Dispatchers.IO) {
        val sourceFile = File(sandboxAbsolutePath)
        if (!sourceFile.exists()) {
            return@withContext Result.Failed("视频文件不存在")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(sourceFile)
            } else {
                saveViaLegacyPath(sourceFile)
            }
        } catch (e: Exception) {
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Android 10+ 推荐路径：MediaStore Content Provider
     */
    private fun saveViaMediaStore(sourceFile: File): Result {
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/DailyPortrait")
            put(MediaStore.Video.Media.IS_PENDING, 1)  // 写入期间标记 pending,完成再清
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }

        val targetUri = resolver.insert(collection, values)
            ?: return Result.Failed("无法创建媒体库条目")

        try {
            resolver.openOutputStream(targetUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IOException("无法打开媒体库输出流")

            // 标记为已就绪
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(targetUri, values, null, null)

            return Result.Success(targetUri.toString())
        } catch (e: Exception) {
            // 失败时清理半截文件
            runCatching { resolver.delete(targetUri, null, null) }
            throw e
        }
    }

    /**
     * Android 9 及以下：直传公共目录
     */
    @Suppress("DEPRECATION")
    private fun saveViaLegacyPath(sourceFile: File): Result {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val subDir = File(moviesDir, "DailyPortrait")
        if (!subDir.exists() && !subDir.mkdirs()) {
            return Result.Failed("无法创建相册目录")
        }
        val targetFile = File(subDir, sourceFile.name)
        sourceFile.copyTo(targetFile, overwrite = true)

        // 通知系统媒体扫描器，新视频立刻出现在相册
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(targetFile.absolutePath),
            arrayOf("video/mp4"),
            null
        )

        return Result.Success(targetFile.absolutePath)
    }

    sealed class Result {
        data class Success(val mediaStoreLocation: String) : Result()
        data class Failed(val reason: String) : Result()
    }
}
