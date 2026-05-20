package com.missyun.dailyportrait.ui.screens.camera

import android.graphics.Bitmap
import com.missyun.dailyportrait.domain.model.FaceAlignment

/**
 * Camera 页面 UI 状态（MVI Model 层）
 *
 * 严格按 architecture-android.md §4.2 定义。
 *
 * 体验补丁（v1.2）：
 * - [pendingPreview] 拍照后先进入预览态，用户确认或重拍
 * - 拍照不再立即落盘，由用户在预览态点 "使用这张" 才真正保存
 *
 * @property isCameraReady 相机初始化是否完成
 * @property cameraPermissionGranted 相机权限是否已授予
 * @property latestPhotoPath 上一张照片相对路径，null 表示首次使用 → 显示引导文字而非洋葱皮
 * @property faceAlignmentStatus 当前对齐状态
 * @property isCapturing 是否正在写盘（点击"使用这张"后短暂为 true，防连点）
 * @property pendingPreview 拍照后未确认的临时预览（含 Bitmap）；非 null 时全屏盖住相机
 * @property countdownSeconds 倒计时剩余秒数
 * @property showThumbnail 是否显示右上角缩略图角标
 * @property thumbnailRelativePath 缩略图来源路径
 * @property error 错误状态
 * @property shutterFlash 是否触发屏幕白闪
 */
data class CameraUiState(
    val isCameraReady: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val latestPhotoPath: String? = null,
    val faceAlignmentStatus: FaceAlignment = FaceAlignment.NONE,
    val isCapturing: Boolean = false,
    val pendingPreview: PendingPreview? = null,
    val countdownSeconds: Int? = null,
    val showThumbnail: Boolean = false,
    val thumbnailRelativePath: String? = null,
    val error: CameraError? = null,
    val shutterFlash: Boolean = false
) {
    /** 是否正在倒计时中 */
    val isCountingDown: Boolean get() = countdownSeconds != null

    /** 是否在预览刚拍照片（未保存） */
    val isPreviewing: Boolean get() = pendingPreview != null

    /** 快门是否可点击 */
    val isShutterEnabled: Boolean
        get() = isCameraReady &&
                cameraPermissionGranted &&
                !isCapturing &&
                !isCountingDown &&
                !isPreviewing &&
                error == null

    /** 是否需要遮罩相机预览 */
    val shouldHideCamera: Boolean
        get() = !cameraPermissionGranted ||
                error is CameraError.PermissionDenied ||
                error is CameraError.CameraUnavailable
}

/**
 * 拍照后的临时预览数据
 *
 * Bitmap 仅活在内存中，用户点 "使用这张" 才走 [com.missyun.dailyportrait.domain.repository.PhotoRepository.savePhoto]
 * 真正落盘 + 写库；点 "重拍" 直接 recycle 丢弃。
 */
data class PendingPreview(
    val bitmap: Bitmap,
    val rotationDegrees: Int
)

/**
 * Camera 错误密封类
 */
sealed class CameraError {
    data object PermissionDenied : CameraError()
    data class StorageFull(val availableBytes: Long) : CameraError()
    data class CameraUnavailable(val reason: String) : CameraError()
    data class FaceDetectionInitFailed(val reason: String) : CameraError()
    data class CaptureFailed(val reason: String) : CameraError()
}
