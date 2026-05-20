package com.missyun.dailyportrait.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ExportConfig 派生属性测试
 *
 * 重点：
 * - frameDurationMs 永远是 1000ms（一帧一秒）
 * - frameDurationUs 是其 1000 倍
 * - width / height 透传 quality
 * - VideoQuality 4 档位分辨率符合 9:16
 */
class ExportConfigTest {

    @Test
    fun `默认配置 frameDurationMs 等于 1000`() {
        val cfg = ExportConfig()
        assertEquals(1000, cfg.frameDurationMs)
    }

    @Test
    fun `frameDurationUs 是 ms 的 1000 倍`() {
        val cfg = ExportConfig()
        assertEquals(1_000_000L, cfg.frameDurationUs)
    }

    @Test
    fun `width 和 height 透传 quality`() {
        val cfg = ExportConfig(quality = VideoQuality.High)
        assertEquals(VideoQuality.High.width, cfg.width)
        assertEquals(VideoQuality.High.height, cfg.height)
    }

    @Test
    fun `所有画质档位均为 9 比 16 竖屏`() {
        VideoQuality.values().forEach { q ->
            val ratio = q.width.toDouble() / q.height
            // 9:16 = 0.5625
            assertEquals(
                "Quality ${q.displayName} 不是 9:16",
                0.5625, ratio, 0.001
            )
        }
    }

    @Test
    fun `画质从流畅到超清严格递增`() {
        val q = VideoQuality.values()
        for (i in 0 until q.size - 1) {
            assertTrue(
                "${q[i].displayName} 应当小于 ${q[i + 1].displayName}",
                q[i].width <= q[i + 1].width
            )
        }
    }

    @Test
    fun `frameRate 在合法范围内`() {
        val cfg = ExportConfig(frameRate = 5)
        assertTrue(cfg.frameRate in 2..20)
    }

    @Test
    fun `默认画质为标清`() {
        assertEquals(VideoQuality.Standard, VideoQuality.DEFAULT)
        assertEquals(VideoQuality.Standard, ExportConfig().quality)
    }
}
