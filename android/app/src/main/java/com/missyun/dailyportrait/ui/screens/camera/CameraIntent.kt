package com.missyun.dailyportrait.ui.screens.camera

import android.graphics.Bitmap
import com.missyun.dailyportrait.domain.model.FaceAnalysis

/**
 * Camera 页面意图（MVI Intent 层）
 *
 * v1.2 补丁：拆分 [CapturePressed]（弹预览）与 [ConfirmSave]（落盘），
 * 同时新增 [Retake]（重拍）。
 */
sealed class CameraIntent {

    data object PermissionGranted : CameraIntent()
    data object PermissionDenied : CameraIntent()
    data object CameraReady : CameraIntent()
    data class CameraFailed(val reason: String) : CameraIntent()

    /** ML Kit 每帧分析回调 */
    data class FaceAnalyzed(val analysis: FaceAnalysis) : CameraIntent()

    /**
     * 用户按下快门 / 倒计时结束 → 进入预览态（不直接保存）
     */
    data class CapturePressed(
        val bitmap: Bitmap,
        val rotationDegrees: Int
    ) : CameraIntent()

    /** 长按快门：启动 3 秒倒计时 */
    data object TimerCapturePressed : CameraIntent()

    /** 倒计时进度 tick（每秒） */
    data class CountdownTick(val secondsLeft: Int) : CameraIntent()

    /** 倒计时归零触发拍照 */
    data class CountdownFired(
        val bitmap: Bitmap,
        val rotationDegrees: Int
    ) : CameraIntent()

    /**
     * 预览态：用户点"使用这张"，正式落盘
     */
    data object ConfirmSave : CameraIntent()

    /**
     * 预览态：用户点"重拍"，丢弃当前预览继续取景
     */
    data object Retake : CameraIntent()

    /** 关闭右上角缩略图 */
    data object DismissThumbnail : CameraIntent()

    /** 关闭错误提示 */
    data object DismissError : CameraIntent()

    /** 离开 Camera 页面 → 释放资源 */
    data object Dispose : CameraIntent()
}
