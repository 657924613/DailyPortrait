package com.missyun.dailyportrait.ui.screens.videoexport

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missyun.dailyportrait.domain.model.ExportState
import com.missyun.dailyportrait.service.VideoRenderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 视频导出 Sheet 的 ViewModel
 *
 * v2 增强：基于进度速率推算"还需 N 秒"
 */
@HiltViewModel
class VideoExportViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(VideoExportUiState())
    val uiState: StateFlow<VideoExportUiState> = _uiState.asStateFlow()

    /** 启动时间 + 上一次进度的时间戳，用于估算 ETA */
    private var startTimeMs: Long = 0L
    private var lastProgress: Int = 0
    private var lastProgressTimeMs: Long = 0L

    init {
        observeService()
    }

    private fun observeService() {
        viewModelScope.launch {
            VideoRenderService.stateFlow.collect { state ->
                _uiState.value = mapState(state)
            }
        }
    }

    private fun mapState(state: ExportState): VideoExportUiState = when (state) {
        is ExportState.Idle -> {
            startTimeMs = 0L
            lastProgress = 0
            VideoExportUiState(phase = ExportPhase.Idle)
        }
        is ExportState.Preparing -> {
            startTimeMs = System.currentTimeMillis()
            VideoExportUiState(
                phase = ExportPhase.Running, progress = 0, isIndeterminate = true
            )
        }
        is ExportState.Progress -> {
            val now = System.currentTimeMillis()
            val eta = estimateRemainingSeconds(state.percent, now)
            lastProgress = state.percent
            lastProgressTimeMs = now
            VideoExportUiState(
                phase = ExportPhase.Running,
                progress = state.percent,
                etaSeconds = eta
            )
        }
        is ExportState.Completed -> VideoExportUiState(
            phase = ExportPhase.Completed,
            progress = 100,
            outputPath = state.outputPath
        )
        is ExportState.Failed -> VideoExportUiState(
            phase = ExportPhase.Failed,
            errorMessage = state.reason
        )
        is ExportState.Cancelled -> VideoExportUiState(phase = ExportPhase.Cancelled)
    }

    /**
     * 估算剩余秒数：用 (已用时间 / 已完成进度) × 剩余进度
     * 进度 < 5% 时不估算（数据少，估算值波动大）
     */
    private fun estimateRemainingSeconds(currentPercent: Int, nowMs: Long): Int? {
        if (currentPercent < 5 || startTimeMs == 0L) return null
        val elapsedMs = nowMs - startTimeMs
        if (elapsedMs <= 0) return null
        val msPerPercent = elapsedMs.toDouble() / currentPercent
        val remainingMs = msPerPercent * (100 - currentPercent)
        return (remainingMs / 1000).toInt().coerceAtLeast(1)
    }

    fun startExport(context: Context, config: com.missyun.dailyportrait.domain.model.ExportConfig) {
        VideoRenderService.start(context.applicationContext, config)
    }

    fun cancel(context: Context) {
        VideoRenderService.cancel(context.applicationContext)
    }
}

data class VideoExportUiState(
    val phase: ExportPhase = ExportPhase.Idle,
    val progress: Int = 0,
    val isIndeterminate: Boolean = false,
    val etaSeconds: Int? = null,
    val outputPath: String? = null,
    val errorMessage: String? = null
) {
    val isRunning: Boolean get() = phase == ExportPhase.Running

    /** "还需 N 秒"友好文案，无估算时返回 null */
    val etaLabel: String? get() = etaSeconds?.let {
        when {
            it < 5 -> "即将完成"
            it < 60 -> "还需约 $it 秒"
            else -> "还需约 ${it / 60} 分 ${it % 60} 秒"
        }
    }
}

enum class ExportPhase {
    Idle, Running, Completed, Failed, Cancelled
}
