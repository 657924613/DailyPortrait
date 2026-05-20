package com.missyun.dailyportrait.ui.screens.dashboard

import com.missyun.dailyportrait.data.local.DailyPhoto

/**
 * Dashboard 页面 UI 状态（MVI Model 层）
 *
 * 严格按 architecture-android.md §4.3 与交付文档 v1.1 §3.1。
 *
 * 派生字段（hasData / canExport）封装在状态类内部，
 * UI 层永远只读 [hasData] 等便捷属性，不重复推导。
 *
 * @property isLoading 首次加载或刷新中
 * @property today 今日打卡记录（null = 今日未打卡）
 * @property allPhotos 所有照片，按 timestamp 倒序，用于历史宫格
 * @property streakDays 连续打卡天数（已断签则归 0）
 * @property weekProgress 本周 7 天的打卡布尔数组（周一 → 周日）
 * @property error 错误信息（一次性，UI 消费后通过 Intent.DismissError 清空）
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val today: DailyPhoto? = null,
    val allPhotos: List<DailyPhoto> = emptyList(),
    val streakDays: Int = 0,
    val weekProgress: BooleanArray = BooleanArray(7) { false },
    val error: String? = null,
    /** 是否需要显示"恭喜你拍下第一张"庆祝弹窗 —— 用户首次仅有 1 张且尚未看过时为 true */
    val showFirstShotCelebration: Boolean = false
) {
    /** 是否有任何数据：决定 Dashboard 走"空状态"还是"有数据" */
    val hasData: Boolean get() = allPhotos.isNotEmpty()

    /** 今日是否已打卡 */
    val isTodayDone: Boolean get() = today != null

    /** 是否允许导出视频（≥2 张照片） */
    val canExport: Boolean get() = allPhotos.size >= 2

    // BooleanArray 没有数据类的默认 equals/hashCode，需手动 override
    // 避免 Compose Flow.collectAsState 比较时永远认为不等导致重组爆炸

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashboardUiState) return false

        if (isLoading != other.isLoading) return false
        if (today != other.today) return false
        if (allPhotos != other.allPhotos) return false
        if (streakDays != other.streakDays) return false
        if (!weekProgress.contentEquals(other.weekProgress)) return false
        if (error != other.error) return false
        if (showFirstShotCelebration != other.showFirstShotCelebration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + (today?.hashCode() ?: 0)
        result = 31 * result + allPhotos.hashCode()
        result = 31 * result + streakDays
        result = 31 * result + weekProgress.contentHashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + showFirstShotCelebration.hashCode()
        return result
    }
}

/**
 * Dashboard 一次性事件
 * 区别于 [DashboardUiState]：navigation / sheet 显隐这类"一次发生"的命令
 */
sealed class DashboardEffect {
    /** 跳转到 Camera 页面 */
    data object NavigateToCamera : DashboardEffect()

    /** 打开导出视频 Sheet */
    data object OpenExportSheet : DashboardEffect()

    /** 删除照片确认弹窗 */
    data class ConfirmDelete(val photo: com.missyun.dailyportrait.data.local.DailyPhoto) : DashboardEffect()

    /** 显示"已删除 [撤销]"的 Snackbar */
    data class ShowUndoSnackbar(val deletedPhoto: com.missyun.dailyportrait.data.local.DailyPhoto) : DashboardEffect()
}
