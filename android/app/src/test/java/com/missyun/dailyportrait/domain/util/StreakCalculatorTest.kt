package com.missyun.dailyportrait.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 连续打卡天数 streak 算法测试
 *
 * 这是 Dashboard 的核心业务逻辑，需要覆盖：
 * - 空列表 → 0
 * - 仅今天 → 1
 * - 连续多天 → 等于天数
 * - 中间有缺勤 → 在断点处停止
 * - 最近一张早于昨天 → 已断签 → 0
 * - 重复日期不重复计数
 */
class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 5, 19)

    @Test
    fun `空列表返回 0`() {
        assertEquals(0, StreakCalculator.calculate(emptyList(), today))
    }

    @Test
    fun `仅今天一张返回 1`() {
        assertEquals(1, StreakCalculator.calculate(listOf("2026-05-19"), today))
    }

    @Test
    fun `仅昨天一张返回 1（今天还没拍但 streak 仍连续）`() {
        assertEquals(1, StreakCalculator.calculate(listOf("2026-05-18"), today))
    }

    @Test
    fun `连续 5 天返回 5`() {
        val photos = listOf(
            "2026-05-19", "2026-05-18", "2026-05-17", "2026-05-16", "2026-05-15"
        )
        assertEquals(5, StreakCalculator.calculate(photos, today))
    }

    @Test
    fun `中间断签从最近的连续段开始计数`() {
        val photos = listOf(
            "2026-05-19", "2026-05-18", // 连续
            "2026-05-15", "2026-05-14"  // 之前断了
        )
        assertEquals(2, StreakCalculator.calculate(photos, today))
    }

    @Test
    fun `最近一张是前天则 streak 归 0`() {
        // 最近一张 = 5/17（前天）→ 已断签
        val photos = listOf("2026-05-17", "2026-05-16", "2026-05-15")
        assertEquals(0, StreakCalculator.calculate(photos, today))
    }

    @Test
    fun `重复日期不重复计数`() {
        val photos = listOf(
            "2026-05-19", "2026-05-19", "2026-05-18", "2026-05-18"
        )
        assertEquals(2, StreakCalculator.calculate(photos, today))
    }

    @Test
    fun `跨月连续打卡`() {
        // 4 月 30 → 5 月 1 → 5 月 2 → 5 月 3 → 5 月 4 共 5 天
        val today2 = LocalDate.of(2026, 5, 4)
        val photos = listOf(
            "2026-05-04", "2026-05-03", "2026-05-02", "2026-05-01", "2026-04-30"
        )
        assertEquals(5, StreakCalculator.calculate(photos, today2))
    }

    @Test
    fun `跨年连续打卡`() {
        val today2 = LocalDate.of(2026, 1, 2)
        val photos = listOf(
            "2026-01-02", "2026-01-01", "2025-12-31", "2025-12-30"
        )
        assertEquals(4, StreakCalculator.calculate(photos, today2))
    }

    @Test
    fun `单张照片是上周早期则 streak 已断`() {
        val photos = listOf("2026-05-10") // 9 天前
        assertEquals(0, StreakCalculator.calculate(photos, today))
    }
}
