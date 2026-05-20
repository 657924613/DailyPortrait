package com.missyun.dailyportrait.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * 沙盒文件管理器
 *
 * 严格按 architecture-android.md §4.1 的"防坑必读"实现：
 *
 * 1. **沙盒路径**：所有图片保存在 `Context.getExternalFilesDir(DIRECTORY_PICTURES)`，
 *    无需运行时存储权限，App 卸载时自动清理。
 *
 * 2. **EXIF 旋转修复**：CameraX 拍照偶尔会带 EXIF Orientation 标签导致显示倒置。
 *    保存前先用 [ExifInterface] 读取 Orientation，再用 [Matrix] 物理旋转 Bitmap，
 *    保存时清除 EXIF 信息（WEBP 不支持完整 EXIF，旋转必须落到像素上）。
 *
 * 3. **WEBP 80% 质量压缩**：相比 JPEG 同质量小 30%~40%，
 *    Android 4.0+ 全平台支持，无解码开销。
 *
 * 4. **1080px 宽度上限**：Bitmap 宽度超过 1080 时按比例缩放，
 *    保证 Dashboard 缩略图加载流畅 + 视频合成内存安全。
 *
 * 5. **返回相对路径**：永远返回相对于 picturesRoot 的子路径，
 *    防止用户清理数据 / 系统升级导致绝对路径变化时图片失效。
 *
 * 6. **协程 I/O**：所有公开方法都用 `withContext(Dispatchers.IO)`，
 *    确保不会在主线程读写文件。
 *
 * 7. **存储空间检查**：保存前用 [StatFs] 检查可用空间，
 *    < 阈值（10MB）抛 [IOException]，由 ViewModel 转为 CameraError.StorageFull。
 */
@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** 沙盒图片根目录，App 卸载自动清理 */
    private val picturesRoot: File
        get() = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir // 极端情况下外部存储不可用时回退到内部

    /**
     * 保存一张已经在内存中的 Bitmap
     *
     * @param bitmap 待保存图片
     * @param fileName 文件名（不含路径，含扩展名）
     * @param exifOrientation EXIF 方向，由 CameraX 的 ImageProxy.imageInfo.rotationDegrees 转换得来。
     *                       传 [ExifInterface.ORIENTATION_NORMAL] 表示无需旋转
     * @param maxWidthPx 最大宽度上限，默认 [MAX_WIDTH_PX]
     * @param quality WEBP 压缩质量 0~100，默认 [DEFAULT_WEBP_QUALITY]
     * @return 相对路径（相对于 [picturesRoot]），形如 `2026-05-18.webp`
     * @throws IOException 存储不足或写入失败
     */
    suspend fun savePhoto(
        bitmap: Bitmap,
        fileName: String,
        exifOrientation: Int = ExifInterface.ORIENTATION_NORMAL,
        maxWidthPx: Int = MAX_WIDTH_PX,
        quality: Int = DEFAULT_WEBP_QUALITY
    ): String = withContext(Dispatchers.IO) {
        // 1. 存储空间预检
        ensureFreeSpace(MIN_FREE_BYTES)

        // 2. EXIF 旋转修复
        val rotated = rotateBitmapByExif(bitmap, exifOrientation)

        // 3. 缩放到 1080px 宽度上限
        val resized = resizeIfNeeded(rotated, maxWidthPx)

        // 4. WEBP 压缩到字节数组（先序列化再写盘，避免半写文件）
        val bytes = compressToWebp(resized, quality)

        // 5. 写盘 —— 使用 .tmp 中间文件 + rename，避免崩溃产生半截文件
        val target = File(picturesRoot, fileName)
        val tmp = File(picturesRoot, "$fileName.tmp")
        try {
            FileOutputStream(tmp).use { it.write(bytes) }
            if (target.exists() && !target.delete()) {
                throw IOException("无法覆盖旧文件: ${target.name}")
            }
            if (!tmp.renameTo(target)) {
                throw IOException("重命名失败: ${tmp.name} → ${target.name}")
            }
        } catch (e: IOException) {
            // 失败时清理临时文件
            tmp.delete()
            throw e
        } finally {
            // 6. 主动回收中间 Bitmap 减少内存压力（原传入 bitmap 不回收，调用方持有）
            if (rotated !== bitmap) rotated.recycle()
            if (resized !== rotated && resized !== bitmap) resized.recycle()
        }

        return@withContext fileName
    }

    /**
     * 删除指定相对路径的文件
     * @return true 表示删除成功或文件本不存在
     */
    suspend fun deletePhoto(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(picturesRoot, relativePath)
        !file.exists() || file.delete()
    }

    /**
     * 把相对路径还原为绝对路径
     * 仅供 Coil / MediaStore 等需要绝对路径的场景使用，UI 层不直接调用
     */
    fun resolveAbsolutePath(relativePath: String): String =
        File(picturesRoot, relativePath).absolutePath

    /**
     * 获取所有照片的相对路径（视频合成时遍历用）
     * 调用方应当通过 Repository 而不是直接调本方法
     */
    suspend fun listAllPhotos(): List<String> = withContext(Dispatchers.IO) {
        picturesRoot.listFiles { _, name -> name.endsWith(EXT_WEBP) }
            ?.map { it.name }
            ?: emptyList()
    }

    /**
     * 当前沙盒可用字节数
     * Camera 出现 StorageFull 错误时让 ViewModel 携带真实剩余空间数值反馈给 UI
     */
    suspend fun availableBytes(): Long = withContext(Dispatchers.IO) {
        runCatching { StatFs(picturesRoot.absolutePath).availableBytes }.getOrDefault(0L)
    }

    // ============================================================
    // 内部实现
    // ============================================================

    /**
     * 校验剩余空间，不足抛 IOException 让上层转 CameraError.StorageFull
     */
    private fun ensureFreeSpace(minBytes: Long) {
        val stat = StatFs(picturesRoot.absolutePath)
        val free = stat.availableBytes
        if (free < minBytes) {
            throw IOException("存储空间不足，剩余 ${free / 1024 / 1024} MB")
        }
    }

    /**
     * 按 EXIF 方向物理旋转 Bitmap（解决 CameraX 偶发 EXIF 倒置 bug）
     */
    private fun rotateBitmapByExif(src: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
            // ORIENTATION_NORMAL / UNDEFINED: 无需旋转
            else -> return src
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    /**
     * 按宽度上限按比例缩放
     */
    private fun resizeIfNeeded(src: Bitmap, maxWidthPx: Int): Bitmap {
        if (src.width <= maxWidthPx) return src
        val ratio = maxWidthPx.toFloat() / src.width
        val newWidth = maxWidthPx
        val newHeight = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newWidth, newHeight, true)
    }

    /**
     * 兼容性 WEBP 压缩
     * - Android Q (API 29)+ 用新枚举 [Bitmap.CompressFormat.WEBP_LOSSY]
     * - 老版本回退到旧枚举 [Bitmap.CompressFormat.WEBP]
     */
    @Suppress("DEPRECATION")
    private fun compressToWebp(src: Bitmap, quality: Int): ByteArray {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        ByteArrayOutputStream().use { out ->
            if (!src.compress(format, quality, out)) {
                throw IOException("WEBP 压缩失败")
            }
            return out.toByteArray()
        }
    }

    /**
     * 解码相对路径为 Bitmap（视频合成 / 缩略图测试用）
     * Coil 库会优先调此种解码途径，但我们也对外暴露给非 Compose 场景
     */
    @Suppress("unused")
    suspend fun decodeBitmap(relativePath: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(picturesRoot, relativePath)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    companion object {
        /** WEBP 默认质量 80%（相对 JPEG 文件小 30%~40%，肉眼无差） */
        const val DEFAULT_WEBP_QUALITY = 80

        /** 最大宽度像素，超过即等比缩放 */
        const val MAX_WIDTH_PX = 1080

        /** 拍照前剩余空间最低要求 10MB */
        const val MIN_FREE_BYTES = 10L * 1024 * 1024

        /** WEBP 文件扩展名（含点） */
        const val EXT_WEBP = ".webp"

        /**
         * 按日期生成文件名
         * @param date yyyy-MM-dd 格式的日期字符串
         * @return 形如 `2026-05-18.webp`
         */
        fun fileNameFor(date: String): String = "$date$EXT_WEBP"
    }
}
