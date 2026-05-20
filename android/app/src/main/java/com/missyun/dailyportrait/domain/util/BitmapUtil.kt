package com.missyun.dailyportrait.domain.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Bitmap / ImageProxy 转换工具
 *
 * v1.2 内存优化补丁：
 * - 解码时按 [TARGET_MAX_DIM] 计算 inSampleSize 降采样
 * - 拍照后 Bitmap 直接是目标尺寸（约 1080px），不再先解 4000×3000 再缩
 * - 单张内存占用从 ~50MB 降到 ~5MB，避免低内存设备 OOM 被系统杀死
 */
object BitmapUtil {

    /** 解码后图片最大边 (px)，与 FileManager.MAX_WIDTH_PX 对齐 */
    private const val TARGET_MAX_DIM = 1080

    /**
     * 把 CameraX 的 ImageProxy（JPEG 输出格式）转为已正确旋转的 Bitmap
     *
     * v1.2 流程：
     * - CameraX 的 takePicture 返回 JPEG bytes
     * - 第一次 decodeByteArray 用 `inJustDecodeBounds = true` 只读尺寸
     * - 计算合适的 inSampleSize（2 的幂）
     * - 第二次解码时按 inSampleSize 降采样，得到约 1080px 边长的 Bitmap
     * - 按 rotationDegrees 物理旋转
     *
     * 调用方必须自行 imageProxy.close()，本方法不关闭
     */
    @OptIn(ExperimentalGetImage::class)
    fun fromImageProxy(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // 第一遍：只读尺寸
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)

        // 第二遍：按 inSampleSize 解码到目标尺寸
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: error("BitmapFactory 解码失败: bytes=${bytes.size}")

        val rotation = imageProxy.imageInfo.rotationDegrees
        return if (rotation == 0) raw else rotate(raw, rotation)
    }

    /**
     * 计算合适的 inSampleSize（必须是 2 的幂）
     * 让短边尽量接近但不小于 TARGET_MAX_DIM
     *
     * internal 暴露给单元测试
     */
    internal fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        val shortDim = minOf(width, height).coerceAtLeast(1)
        while (shortDim / (sampleSize * 2) >= TARGET_MAX_DIM) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * 按角度物理旋转 Bitmap
     */
    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return src
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (rotated !== src) src.recycle()
        return rotated
    }

    /**
     * 把 rotationDegrees 转为 EXIF Orientation 常量
     */
    @Suppress("unused")
    fun rotationDegreesToExif(degrees: Int): Int = when (degrees) {
        0 -> ExifInterface.ORIENTATION_NORMAL
        90 -> ExifInterface.ORIENTATION_ROTATE_90
        180 -> ExifInterface.ORIENTATION_ROTATE_180
        270 -> ExifInterface.ORIENTATION_ROTATE_270
        else -> ExifInterface.ORIENTATION_UNDEFINED
    }
}
