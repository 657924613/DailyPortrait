package com.missyun.dailyportrait.ui.screens.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missyun.dailyportrait.data.analyzer.FaceAnalyzer
import com.missyun.dailyportrait.data.analyzer.FaceAnalyzerFactory
import com.missyun.dailyportrait.domain.model.FaceAnalysis
import com.missyun.dailyportrait.domain.model.NormalizedPoint
import com.missyun.dailyportrait.domain.repository.PhotoRepository
import com.missyun.dailyportrait.domain.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Camera 页面 ViewModel（MVI 架构）
 *
 * v1.2 体验补丁：
 * - 拍照不再直接落盘，先进入 [PendingPreview] 让用户确认
 * - 用户点 "使用这张" → [ConfirmSave] 才走 PhotoRepository 落盘
 * - 用户点 "重拍" → [Retake] 丢弃 Bitmap 继续取景
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val analyzerFactory: FaceAnalyzerFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var currentAnalyzer: FaceAnalyzer? = null
    private var countdownJob: Job? = null
    private var flashJob: Job? = null

    private var cachedTargetCenter: NormalizedPoint? = null
    private var lastDetectedCenter: NormalizedPoint? = null

    init {
        loadLatestPhoto()
    }

    fun onIntent(intent: CameraIntent) {
        when (intent) {
            is CameraIntent.PermissionGranted -> handlePermissionGranted()
            is CameraIntent.PermissionDenied -> handlePermissionDenied()
            is CameraIntent.CameraReady -> _uiState.update { it.copy(isCameraReady = true) }
            is CameraIntent.CameraFailed -> handleCameraFailed(intent.reason)
            is CameraIntent.FaceAnalyzed -> handleFaceAnalyzed(intent.analysis)
            is CameraIntent.CapturePressed -> enterPreview(intent.bitmap, intent.rotationDegrees)
            is CameraIntent.TimerCapturePressed -> startCountdown()
            is CameraIntent.CountdownTick -> _uiState.update { it.copy(countdownSeconds = intent.secondsLeft) }
            is CameraIntent.CountdownFired -> enterPreview(intent.bitmap, intent.rotationDegrees)
            is CameraIntent.ConfirmSave -> confirmSave()
            is CameraIntent.Retake -> retake()
            is CameraIntent.DismissThumbnail -> _uiState.update {
                it.copy(showThumbnail = false, thumbnailRelativePath = null)
            }
            is CameraIntent.DismissError -> _uiState.update { it.copy(error = null) }
            is CameraIntent.Dispose -> dispose()
        }
    }

    fun acquireAnalyzer(): FaceAnalyzer {
        currentAnalyzer?.close()

        val analyzer = analyzerFactory.create(
            targetCenter = cachedTargetCenter
        ) { result ->
            onIntent(CameraIntent.FaceAnalyzed(result))
        }
        currentAnalyzer = analyzer
        return analyzer
    }

    // ============ Intent Handlers ============

    private fun handlePermissionGranted() {
        _uiState.update { it.copy(cameraPermissionGranted = true, error = null) }
    }

    private fun handlePermissionDenied() {
        _uiState.update {
            it.copy(
                cameraPermissionGranted = false,
                error = CameraError.PermissionDenied
            )
        }
    }

    private fun handleCameraFailed(reason: String) {
        _uiState.update {
            it.copy(
                isCameraReady = false,
                error = CameraError.CameraUnavailable(reason)
            )
        }
    }

    private fun handleFaceAnalyzed(analysis: FaceAnalysis) {
        analysis.normalizedCenter?.let { lastDetectedCenter = it }
        _uiState.update { it.copy(faceAlignmentStatus = analysis.alignment) }
    }

    private fun startCountdown() {
        if (_uiState.value.isCountingDown) return
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in COUNTDOWN_SECONDS downTo 1) {
                onIntent(CameraIntent.CountdownTick(i))
                delay(1000)
            }
            _uiState.update { it.copy(countdownSeconds = 0) }
        }
    }

    /**
     * 进入预览态：拍下来的 Bitmap 暂存内存，等用户决定
     */
    private fun enterPreview(bitmap: Bitmap, rotationDegrees: Int) {
        if (_uiState.value.isPreviewing) return // 防重复

        // 触发白闪
        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            _uiState.update { it.copy(shutterFlash = true) }
            delay(SHUTTER_FLASH_DURATION_MS)
            _uiState.update { it.copy(shutterFlash = false) }
        }

        _uiState.update {
            it.copy(
                pendingPreview = PendingPreview(bitmap, rotationDegrees),
                countdownSeconds = null
            )
        }
    }

    /**
     * 用户确认：把预览中的 Bitmap 真正落盘
     */
    private fun confirmSave() {
        val pending = _uiState.value.pendingPreview ?: return
        if (_uiState.value.isCapturing) return

        _uiState.update { it.copy(isCapturing = true) }

        viewModelScope.launch {
            try {
                val today = DateUtil.todayString()
                val timestamp = System.currentTimeMillis()
                val faceCenter = lastDetectedCenter

                val relativePath = photoRepository.savePhoto(
                    bitmap = pending.bitmap,
                    date = today,
                    timestamp = timestamp,
                    faceCenterX = faceCenter?.x,
                    faceCenterY = faceCenter?.y
                )

                cachedTargetCenter = faceCenter

                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        pendingPreview = null,
                        showThumbnail = true,
                        thumbnailRelativePath = relativePath,
                        latestPhotoPath = relativePath
                    )
                }

                // 缩略图角标短暂展示
                delay(THUMBNAIL_VISIBLE_DURATION_MS)
                onIntent(CameraIntent.DismissThumbnail)

                // 重建分析器以新的对齐目标继续取景
                acquireAnalyzer()
            } catch (e: IOException) {
                val available = runCatching { photoRepository.availableBytes() }.getOrDefault(0L)
                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        pendingPreview = null,
                        error = if (e.message?.contains("空间不足") == true) {
                            CameraError.StorageFull(available)
                        } else {
                            CameraError.CaptureFailed(e.message ?: "未知错误")
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        pendingPreview = null,
                        error = CameraError.CaptureFailed(e.message ?: "未知错误")
                    )
                }
            } finally {
                if (!pending.bitmap.isRecycled) pending.bitmap.recycle()
            }
        }
    }

    /**
     * 用户重拍：丢弃当前 Bitmap，回到取景态
     */
    private fun retake() {
        val pending = _uiState.value.pendingPreview ?: return
        if (!pending.bitmap.isRecycled) pending.bitmap.recycle()
        _uiState.update { it.copy(pendingPreview = null) }
    }

    private fun loadLatestPhoto() {
        viewModelScope.launch {
            val latest = runCatching { photoRepository.getLatest() }.getOrNull()
            cachedTargetCenter = if (latest?.faceCenterX != null && latest.faceCenterY != null) {
                NormalizedPoint(latest.faceCenterX, latest.faceCenterY)
            } else null
            _uiState.update { it.copy(latestPhotoPath = latest?.imagePath) }
        }
    }

    private fun dispose() {
        countdownJob?.cancel()
        flashJob?.cancel()
        // 离开页面时若仍有未确认的预览，回收内存
        _uiState.value.pendingPreview?.let { p ->
            if (!p.bitmap.isRecycled) p.bitmap.recycle()
        }
        currentAnalyzer?.close()
        currentAnalyzer = null
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }

    companion object {
        const val COUNTDOWN_SECONDS = 3
        const val SHUTTER_FLASH_DURATION_MS = 120L
        const val THUMBNAIL_VISIBLE_DURATION_MS = 2500L
    }
}
