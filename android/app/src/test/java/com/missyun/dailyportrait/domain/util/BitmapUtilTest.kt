package com.missyun.dailyportrait.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BitmapUtil 降采样算法测试
 *
 * inSampleSize 必须是 2 的幂（1, 2, 4, 8...），
 * 解码后短边应该 >= 1080px。
 */
class BitmapUtilTest {

    @Test
    fun `小于目标尺寸不降采样`() {
        // 720×1280 < 1080 → sampleSize = 1
        assertEquals(1, BitmapUtil.calculateInSampleSize(720, 1280))
    }

    @Test
    fun `刚好等于目标尺寸不降采样`() {
        assertEquals(1, BitmapUtil.calculateInSampleSize(1080, 1920))
    }

    @Test
    fun `2 倍目标尺寸降采样为 2`() {
        // 短边 2160 / 2 = 1080 ≥ 1080 → sampleSize = 2 OK
        // 但下一步 2160 / 4 = 540 < 1080 → 不能再降
        assertEquals(2, BitmapUtil.calculateInSampleSize(2160, 3840))
    }

    @Test
    fun `4 倍目标尺寸降采样为 4`() {
        // 短边 4320 / 4 = 1080 → 边界 OK
        assertEquals(4, BitmapUtil.calculateInSampleSize(4320, 7680))
    }

    @Test
    fun `结果永远是 2 的幂`() {
        // 测多组随机尺寸，验证返回值都是 2 的幂
        val sizes = listOf(
            300 to 400,
            1500 to 2000,
            3000 to 4000,
            5000 to 7000,
            8000 to 11000
        )
        for ((w, h) in sizes) {
            val s = BitmapUtil.calculateInSampleSize(w, h)
            // 2 的幂：s & (s - 1) == 0 且 s > 0
            val isPowerOfTwo = s > 0 && (s and (s - 1)) == 0
            assert(isPowerOfTwo) { "Sample size $s for ${w}x${h} 不是 2 的幂" }
        }
    }

    @Test
    fun `极小输入不会除零崩溃`() {
        // 0 会被 coerceAtLeast(1) 兜底
        assertEquals(1, BitmapUtil.calculateInSampleSize(0, 0))
        assertEquals(1, BitmapUtil.calculateInSampleSize(1, 1))
    }
}
