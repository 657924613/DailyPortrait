package com.missyun.dailyportrait.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * DateUtil 测试
 *
 * 重点测：
 * - todayString 输出 ISO 格式
 * - currentWeekRange 永远是周一到周日（中国习惯）
 * - daysBetween 对称且边界正确
 */
class DateUtilTest {

    @Test
    fun `todayString 输出 ISO yyyy-MM-dd`() {
        val date = LocalDate.of(2026, 5, 19)
        assertEquals("2026-05-19", DateUtil.todayString(date))
    }

    @Test
    fun `parse 与 todayString 是逆运算`() {
        val str = "2026-12-31"
        val parsed = DateUtil.parse(str)
        assertEquals(str, DateUtil.todayString(parsed))
    }

    @Test
    fun `currentWeekRange 周三时返回的起点是周一`() {
        // 2026-05-20 是周三
        val wed = LocalDate.of(2026, 5, 20)
        assertEquals(DayOfWeek.WEDNESDAY, wed.dayOfWeek) // 前置条件验证
        val (start, end) = DateUtil.currentWeekRange(wed)
        assertEquals("2026-05-18", start)
        assertEquals("2026-05-24", end)
    }

    @Test
    fun `currentWeekRange 周日时返回本周一到当天`() {
        // 2026-05-24 是周日
        val sun = LocalDate.of(2026, 5, 24)
        val (start, end) = DateUtil.currentWeekRange(sun)
        assertEquals("2026-05-18", start)
        assertEquals("2026-05-24", end)
    }

    @Test
    fun `daysBetween 同一天为 0`() {
        assertEquals(0L, DateUtil.daysBetween("2026-05-19", "2026-05-19"))
    }

    @Test
    fun `daysBetween 跨月计算正确`() {
        // 4 月 30 → 5 月 2 = 2 天
        assertEquals(2L, DateUtil.daysBetween("2026-04-30", "2026-05-02"))
    }
}
