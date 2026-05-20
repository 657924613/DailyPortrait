package com.missyun.dailyportrait.domain.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 日期工具
 *
 * 统一应用内 yyyy-MM-dd 字符串与 [LocalDate] 的转换，
 * 避免 SimpleDateFormat 的线程安全问题（[DateTimeFormatter] 是不可变线程安全的）。
 *
 * 仅支持 API 26+，与 minSdk 一致。
 */
object DateUtil {

    /** Room 中 date 列的标准格式 */
    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** 今日 yyyy-MM-dd 字符串（可被测试 mock LocalDate.now() 替换） */
    fun todayString(today: LocalDate = LocalDate.now()): String =
        today.format(ISO_DATE)

    /** 解析 yyyy-MM-dd 字符串到 LocalDate */
    fun parse(dateStr: String): LocalDate = LocalDate.parse(dateStr, ISO_DATE)

    /**
     * 计算"本周一到本周日"的字符串区间（用于 Dashboard 周进度环）
     * 周一定义为一周开始，符合中国习惯
     *
     * @return Pair(start = 本周一, end = 本周日)
     */
    fun currentWeekRange(today: LocalDate = LocalDate.now()): Pair<String, String> {
        val monday = today.with(DayOfWeek.MONDAY)
        val sunday = today.with(DayOfWeek.SUNDAY)
        return monday.format(ISO_DATE) to sunday.format(ISO_DATE)
    }

    /**
     * 计算两个日期相差天数（绝对值）
     */
    fun daysBetween(from: String, to: String): Long {
        val f = parse(from)
        val t = parse(to)
        return java.time.temporal.ChronoUnit.DAYS.between(f, t)
    }
}
