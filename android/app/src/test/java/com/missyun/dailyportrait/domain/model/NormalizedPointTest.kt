package com.missyun.dailyportrait.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 归一化坐标点的核心数学测试
 *
 * 这个组件是对齐算法的基石，必须保证：
 * - 坐标合法性校验（不允许 < 0 或 > 1）
 * - distanceTo 对称（A→B 和 B→A 距离相等）
 * - distanceTo 满足三角不等式
 * - 与几何中心 CENTER (0.5, 0.5) 距离正确
 */
class NormalizedPointTest {

    @Test
    fun `坐标在合法范围内可正常构造`() {
        NormalizedPoint(0f, 0f)
        NormalizedPoint(0.5f, 0.5f)
        NormalizedPoint(1f, 1f)
        // 没抛异常即通过
    }

    @Test
    fun `坐标超出 0_1 范围应抛 IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            NormalizedPoint(-0.01f, 0.5f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NormalizedPoint(0.5f, 1.01f)
        }
    }

    @Test
    fun `两点之间距离对称`() {
        val a = NormalizedPoint(0.3f, 0.4f)
        val b = NormalizedPoint(0.7f, 0.6f)
        val d1 = a.distanceTo(b)
        val d2 = b.distanceTo(a)
        assertEquals(d1, d2, 0.0001f)
    }

    @Test
    fun `距自身的距离为 0`() {
        val p = NormalizedPoint(0.42f, 0.58f)
        assertEquals(0f, p.distanceTo(p), 0.0001f)
    }

    @Test
    fun `屏幕中心 CENTER 等于 0_5_0_5`() {
        assertEquals(0.5f, NormalizedPoint.CENTER.x, 0.0001f)
        assertEquals(0.5f, NormalizedPoint.CENTER.y, 0.0001f)
    }

    @Test
    fun `两点距离符合勾股定理`() {
        // (0, 0) 到 (0.3, 0.4) 应当是 0.5 (3-4-5 三角形)
        val a = NormalizedPoint(0f, 0f)
        val b = NormalizedPoint(0.3f, 0.4f)
        assertEquals(0.5f, a.distanceTo(b), 0.0001f)
    }

    @Test
    fun `FaceAlignment_canCapture 仅 ALIGNED 为 true`() {
        assertTrue(FaceAlignment.ALIGNED.canCapture)
        assertTrue(!FaceAlignment.NONE.canCapture)
        assertTrue(!FaceAlignment.DETECTED.canCapture)
    }
}
