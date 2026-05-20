package com.missyun.dailyportrait.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missyun.dailyportrait.domain.repository.PhotoRepository
import com.missyun.dailyportrait.domain.util.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 统计页 ViewModel
 *
 * 提供：
 * - 总拍照天数
 * - 当前连续天数
 * - 最长连续天数
 * - 当月热力图数据（哪些天拍了照）
 * - 月份切换
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onMonthChange(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedMonth = yearMonth) }
        loadHeatmap(yearMonth)
    }

    private fun loadData() {
        viewModelScope.launch {
            photoRepository.observeAll().collect { photos ->
                val dates = photos.map { it.date }
                val currentStreak = StreakCalculator.calculate(dates)
                val longestStreak = StreakCalculator.longest(dates)
                val totalDays = dates.distinct().size

                val selectedMonth = _uiState.value.selectedMonth
                val heatmap = buildHeatmap(selectedMonth, dates)

                _uiState.update {
                    it.copy(
                        totalDays = totalDays,
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        heatmapDays = heatmap,
                        allDates = dates.toSet()
                    )
                }
            }
        }
    }

    private fun loadHeatmap(yearMonth: YearMonth) {
        val dates = _uiState.value.allDates
        val heatmap = buildHeatmap(yearMonth, dates.toList())
        _uiState.update { it.copy(heatmapDays = heatmap) }
    }

    private fun buildHeatmap(yearMonth: YearMonth, dates: Collection<String>): Set<Int> {
        val prefix = yearMonth.toString() // yyyy-MM
        return dates
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.substringAfterLast("-").toIntOrNull() }
            .toSet()
    }
}

/**
 * 统计页 UI 状态
 */
data class StatisticsUiState(
    val totalDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val selectedMonth: YearMonth = YearMonth.now(),
    val heatmapDays: Set<Int> = emptySet(),
    val allDates: Set<String> = emptySet()
)
