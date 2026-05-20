package com.missyun.dailyportrait.domain.util

import java.time.LocalDate

/**
 * 连续打卡天数计算器（纯函数，便于单元测试）
 *
 * 规则：
 * - 输入按 timestamp 倒序的日期列表（最近的在前）
 * - 从今天或最近一张往前推
 * - 相邻日期相差 1 天 → streak += 1
 * - 相差 > 1 天 → 中断
 * - 最近一张早于昨天 → streak 已断
 *
 * 抽出来后既能给 DashboardViewModel 用，也能写完整单测。
 */
object StreakCalculator {

    /**
     * @param dateStrings yyyy-MM-dd 格式日期，按 timestamp 倒序
     * @param today 当前日期（测试可注入）
     * @return 连续打卡天数
     */
    fun calculate(
        dateStrings: List<String>,
        today: LocalDate = LocalDate.now()
    ): Int {
        if (dateStrings.isEmpty()) return 0

        val datesDesc = dateStrings.map { LocalDate.parse(it) }.distinct()
        if (datesDesc.isEmpty()) return 0

        val firstDate = datesDesc[0]
        // 最近一张早于昨天 → 已断签
        if (firstDate.isBefore(today.minusDays(1))) return 0

        var streak = 0
        var expectedDate = firstDate
        for (date in datesDesc) {
            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    /**
     * 计算历史最长连续天数
     *
     * @param dateStrings yyyy-MM-dd 格式日期列表（任意顺序）
     * @return 最长连续打卡天数
     */
    fun longest(dateStrings: Collection<String>): Int {
        if (dateStrings.isEmpty()) return 0

        val sorted = dateStrings.map { LocalDate.parse(it) }.distinct().sorted()
        var maxStreak = 1
        var current = 1

        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                current++
                if (current > maxStreak) maxStreak = current
            } else {
                current = 1
            }
        }
        return maxStreak
    }
}
