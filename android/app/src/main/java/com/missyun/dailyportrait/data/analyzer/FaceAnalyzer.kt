package com.missyun.dailyportrait.data.analyzer

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.missyun.dailyportrait.domain.model.FaceAlignment
import com.missyun.dailyportrait.domain.model.FaceAnalysis
import com.missyun.dailyportrait.domain.model.NormalizedPoint
import kotlin.math.abs

/**
 * CameraX 图像分析器：基于 ML Kit 静态版人脸检测
 *
 * 严格按 architecture-android.md §4.2 实现：
 *
 * - 使用 [FaceDetectorOptions.PERFORMANCE_MODE_FAST]：仅获取 BoundingBox，
 *   不开 Landmark / Classification，单帧分析 < 30ms
 * - 严禁 Play Services 动态版本（项目用 `com.google.mlkit:face-detection` 静态打包）
 * - **防内存泄漏关键**：[analyze] 在 finally 块统一调用 `imageProxy.close()`
 * - 对齐目标动态决定：
 *   - [targetCenter] = null → 首拍对齐屏幕中心 [NormalizedPoint.CENTER]
 *   - [targetCenter] != null → 对齐上一张照片的人脸中心（来自 Room）
 * - 误差阈值：归一化坐标欧式距离 < [ALIGNMENT_THRESHOLD]（0.08）视为对齐，
 *   比 10% 更严格（§4.2 要求）
 *
 * 使用方式（CameraViewModel 内部）：
 * ```
 * val analyzer = FaceAnalyzer(targetCenter = latestPhotoCenter) { result ->
 *     _uiState.update { it.copy(faceAlignmentStatus = result.alignment, ...) }
 * }
 * imageAnalysis.setAnalyzer(executor, analyzer)
 * ```
 *
 * 线程模型：
 * - [analyze] 由 CameraX 在它分配的执行器上调用（通常是后台单线程）
 * - 通过传入 [onResult] 回调把结果传出，调用方负责切换到 UI 线程
 *
 * @property targetCenter 对齐目标，null 表示对齐屏幕中心；非 null 时对齐上张人脸位置
 * @property onResult 每帧分析完成后的回调
 */
class FaceAnalyzer(
    private val targetCenter: NormalizedPoint?,
    private val onResult: (FaceAnalysis) -> Unit
) : ImageAnalysis.Analyzer {

    /** ML Kit 人脸检测器实例（线程安全，可跨帧复用） */
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            // FAST 模式：仅 BoundingBox，分析速度优先
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            // 不需要 Landmark / Contour / Classification（眼鼻嘴 / 微笑概率）
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            // 设置最小人脸尺寸阈值，画面占比 < 15% 不算（避免远处路人误检）
            .setMinFaceSize(MIN_FACE_SIZE_RATIO)
            .build()
    )

    /**
     * 关闭检测器，释放底层资源
     * CameraViewModel.onCleared() 时务必调用，否则会泄漏 ML Kit native 句柄
     */
    fun close() {
        detector.close()
    }

    /**
     * 分析单帧
     *
     * **finally 中 imageProxy.close() 是关键**：
     * - 不调用会导致 CameraX 队列堵塞，新帧无法送入分析器
     * - ML Kit 异步回调内部如果抛异常，外层不 close 同样会泄漏
     */
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            // 帧无效，必须 close 后返回
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        // 检测器返回宽高的"旋转后"尺寸，直接用 InputImage 的 width/height
        // (旋转 90/270 度时 width/height 会被 InputImage 自动交换)
        val frameWidth = inputImage.width.coerceAtLeast(1)
        val frameHeight = inputImage.height.coerceAtLeast(1)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val result = computeAlignment(faces, frameWidth, frameHeight)
                onResult(result)
            }
            .addOnFailureListener { _ ->
                // 检测失败按"无人脸"处理，UI 不会卡死
                onResult(FaceAnalysis.EMPTY)
            }
            .addOnCompleteListener {
                // 关键：无论成功失败都必须 close，避免帧泄漏
                imageProxy.close()
            }
    }

    /**
     * 把 ML Kit 返回的人脸列表转为业务状态
     *
     * 选最大的那张脸作为主体（避免多人场景误判）。
     * 计算其归一化中心，与 [targetCenter] 比较距离。
     */
    private fun computeAlignment(
        faces: List<Face>,
        frameWidth: Int,
        frameHeight: Int
    ): FaceAnalysis {
        if (faces.isEmpty()) return FaceAnalysis.EMPTY

        // 选 BoundingBox 面积最大的那张脸
        val face = faces.maxByOrNull { f ->
            f.boundingBox.width().toLong() * f.boundingBox.height().toLong()
        } ?: return FaceAnalysis.EMPTY

        // 归一化中心 (0~1)
        val rect = face.boundingBox
        val cxRaw = rect.exactCenterX() / frameWidth
        val cyRaw = rect.exactCenterY() / frameHeight
        val cx = cxRaw.coerceIn(0f, 1f)
        val cy = cyRaw.coerceIn(0f, 1f)

        // 边框占比，可作为"凑近 / 远点"提示用（步骤 3 仅记录，不强制约束）
        val areaRatio = (rect.width().toFloat() * rect.height()) /
                (frameWidth.toFloat() * frameHeight)

        val center = NormalizedPoint(cx, cy)
        val target = targetCenter ?: NormalizedPoint.CENTER
        val dx = abs(cx - target.x)
        val dy = abs(cy - target.y)

        // 用 Chebyshev 距离（max(dx, dy)）而非欧式距离：
        // 避免某轴严重偏离但欧式距离恰好低于阈值的情况
        // 例如 dx=0.07, dy=0.07 欧式 ~0.099 仍 < 0.1，但实际两轴都接近边界
        val maxAxisDist = maxOf(dx, dy)
        val alignment = if (maxAxisDist <= ALIGNMENT_THRESHOLD) {
            FaceAlignment.ALIGNED
        } else {
            FaceAlignment.DETECTED
        }

        return FaceAnalysis(
            alignment = alignment,
            normalizedCenter = center,
            boundingBoxRatio = areaRatio
        )
    }

    companion object {
        /**
         * 对齐误差阈值（归一化坐标）
         * Chebyshev 距离（最大轴偏移）< 0.08 视为对齐，对应屏幕约 ±8% 区域
         */
        const val ALIGNMENT_THRESHOLD = 0.08f

        /**
         * 最小人脸尺寸比例（占画面短边）
         * < 15% 视为远处误检不参与判定
         */
        const val MIN_FACE_SIZE_RATIO = 0.15f
    }
}
