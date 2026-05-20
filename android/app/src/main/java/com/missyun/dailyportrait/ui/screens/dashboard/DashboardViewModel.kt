package com.missyun.dailyportrait.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missyun.dailyportrait.data.local.DailyPhoto
import com.missyun.dailyportrait.data.preferences.AppPreferences
import com.missyun.dailyportrait.domain.repository.PhotoRepository
import com.missyun.dailyportrait.domain.util.DateUtil
import com.missyun.dailyportrait.domain.util.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Dashboard ViewModel（MVI 架构）
 *
 * v2 增强：可撤销删除
 * - 用户确认删除 → 软删除（仅从 DB 移除，文件保留）+ 触发 ShowUndoSnackbar
 * - 用户点撤销 → 5 秒内调 undoDelete，记录回写
 * - 5 秒过后 → 真删磁盘文件
 *
 * v3 增强：首张照片庆祝
 * - 用户拍下第一张时弹"恭喜"对话框，鼓励持续打卡
 * - 用 AppPreferences.hasShownFirstShotCelebration 永久记录"已看过"
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effect = Channel<DashboardEffect>(Channel.BUFFERED)
    val effect get() = _effect.receiveAsFlow()

    /** 等待真删的照片 + purge 协程引用 */
    private var pendingPurge: DailyPhoto? = null
    private var purgeJob: Job? = null

    init {
        observeData()
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.OpenCamera -> emitEffect(DashboardEffect.NavigateToCamera)
            is DashboardIntent.RequestExport -> {
                if (_uiState.value.canExport) emitEffect(DashboardEffect.OpenExportSheet)
            }
            is DashboardIntent.RequestDelete -> emitEffect(DashboardEffect.ConfirmDelete(intent.photo))
            is DashboardIntent.ConfirmDelete -> softDeletePhoto(intent.photo)
            is DashboardIntent.UndoDelete -> undoDelete()
            is DashboardIntent.DismissError -> _uiState.update { it.copy(error = null) }
            is DashboardIntent.DismissFirstShotCelebration -> dismissFirstShotCelebration()
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            val today = DateUtil.todayString()
            val (weekStart, weekEnd) = DateUtil.currentWeekRange()

            combine(
                photoRepository.observeAll(),
                photoRepository.observeToday(today),
                photoRepository.observeRange(weekStart, weekEnd),
                preferences.hasShownFirstShotCelebration
            ) { all, todayPhoto, weekPhotos, hasShownCelebration ->
                StateBundle(all, todayPhoto, weekPhotos, hasShownCelebration)
            }.collect { bundle ->
                val streak = calculateStreak(bundle.all)
                val weekArr = buildWeekArray(weekStart, bundle.weekPhotos)
                // 首拍庆祝触发条件：有且仅有 1 张 + 用户尚未看过庆祝
                val showCelebration = bundle.all.size == 1 && !bundle.hasShownCelebration

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        today = bundle.todayPhoto,
                        allPhotos = bundle.all,
                        streakDays = streak,
                        weekProgress = weekArr,
                        showFirstShotCelebration = showCelebration
                    )
                }
            }
        }
    }

    private data class StateBundle(
        val all: List<DailyPhoto>,
        val todayPhoto: DailyPhoto?,
        val weekPhotos: List<DailyPhoto>,
        val hasShownCelebration: Boolean
    )

    private fun calculateStreak(photos: List<DailyPhoto>): Int =
        StreakCalculator.calculate(photos.map { it.date })

    private fun buildWeekArray(weekStart: String, weekPhotos: List<DailyPhoto>): BooleanArray {
        val arr = BooleanArray(7) { false }
        val start = LocalDate.parse(weekStart)
        weekPhotos.forEach { photo ->
            val d = LocalDate.parse(photo.date)
            val idx = java.time.temporal.ChronoUnit.DAYS.between(start, d).toInt()
            if (idx in 0..6) arr[idx] = true
        }
        return arr
    }

    // ============ 写操作（含可撤销删除） ============

    /**
     * 软删除：从 DB 移除并启动 5 秒延时真删
     * 期间用户调 undoDelete 可恢复
     */
    private fun softDeletePhoto(photo: DailyPhoto) {
        viewModelScope.launch {
            // 取消上一次未完成的 purge（防止用户连删多张时旧 job 抢先真删）
            purgeJob?.cancel()
            // 上一次"等待真删"的照片如果还没真删，立即真删（用户没撤销 = 默认确认）
            pendingPurge?.let { runCatching { photoRepository.purgeFile(it.imagePath) } }

            try {
                photoRepository.softDeletePhoto(photo)
                pendingPurge = photo
                emitEffect(DashboardEffect.ShowUndoSnackbar(photo))

                // 5 秒撤销窗口
                purgeJob = launch {
                    delay(UNDO_WINDOW_MS)
                    pendingPurge?.let {
                        runCatching { photoRepository.purgeFile(it.imagePath) }
                    }
                    pendingPurge = null
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败：${e.message}") }
            }
        }
    }

    private fun undoDelete() {
        val toRestore = pendingPurge ?: return
        purgeJob?.cancel()
        pendingPurge = null

        viewModelScope.launch {
            try {
                photoRepository.restorePhoto(toRestore)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "恢复失败：${e.message}") }
            }
        }
    }

    /**
     * 立即把 pendingPurge 真删（ViewModel 销毁时调用，避免文件遗留）
     */
    override fun onCleared() {
        super.onCleared()
        pendingPurge?.let { p ->
            // 不能再用 viewModelScope，直接同步删
            runCatching {
                kotlinx.coroutines.runBlocking {
                    photoRepository.purgeFile(p.imagePath)
                }
            }
        }
    }

    private fun emitEffect(effect: DashboardEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    /**
     * 用户确认看完"首张照片庆祝"弹窗 —— 永久持久化标志位，下次进 App 不再弹
     */
    private fun dismissFirstShotCelebration() {
        // 立即先把状态置 false 让 UI 关掉弹窗（DataStore 写入是异步的）
        _uiState.update { it.copy(showFirstShotCelebration = false) }
        viewModelScope.launch {
            runCatching { preferences.markFirstShotCelebrated() }
        }
    }

    private companion object {
        const val UNDO_WINDOW_MS = 5_000L
    }
}

/**
 * Dashboard 用户意图
 */
sealed class DashboardIntent {
    data object OpenCamera : DashboardIntent()
    data object RequestExport : DashboardIntent()
    data class RequestDelete(val photo: DailyPhoto) : DashboardIntent()
    data class ConfirmDelete(val photo: DailyPhoto) : DashboardIntent()
    data object UndoDelete : DashboardIntent()
    data object DismissError : DashboardIntent()
    /** 用户确认看完"首张照片庆祝"弹窗 */
    data object DismissFirstShotCelebration : DashboardIntent()
}
