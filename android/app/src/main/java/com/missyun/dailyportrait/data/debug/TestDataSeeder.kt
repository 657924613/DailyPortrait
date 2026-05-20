package com.missyun.dailyportrait.data.debug

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.missyun.dailyportrait.data.local.DailyPhotoDao
import com.missyun.dailyportrait.data.storage.FileManager
import com.missyun.dailyportrait.data.local.DailyPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调试数据填充器（仅供测试使用）
 *
 * 一次性生成 N 天的伪照片，每天一张：
 * - 用 Canvas 画渐变色块作为照片占位
 * - 颜色按日期循环（让宫格视觉上有变化）
 * - 通过 [FileManager] + [DailyPhotoDao] 同样的写入路径，
 *   保证生成的数据能被视频合成等所有下游正确消费
 *
 * 注意：仅在 debug 构建中暴露给 UI，避免误用污染用户数据。
 */
@Singleton
class TestDataSeeder @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val dao: DailyPhotoDao,
    private val fileManager: FileManager
) {
    /**
     * 生成最近 [days] 天的测试数据，覆盖式写入。
     *
     * @param days 默认 30 天
     * @return 实际写入的张数
     */
    suspend fun seed(days: Int = 30): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        var written = 0

        for (offset in days - 1 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val dateStr = date.toString()
            val bitmap = generateMockPortrait(dateStr, indexHash = offset)

            try {
                val fileName = FileManager.fileNameFor(dateStr)
                val relativePath = fileManager.savePhoto(
                    bitmap = bitmap,
                    fileName = fileName
                )

                // 时间戳调到对应日的 09:24，让 timestamp 顺序与 date 顺序一致
                val ts = date.atTime(9, 24)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                // 模拟人脸中心点，让对齐过滤功能可以测试
                // 30 天里前 5 天没记录人脸（faceCenter null），后 25 天有
                val withFace = offset < days - 5
                val faceX = if (withFace) 0.5f + (offset % 7 - 3) * 0.01f else null
                val faceY = if (withFace) 0.5f + (offset % 5 - 2) * 0.01f else null

                dao.upsert(
                    DailyPhoto(
                        date = dateStr,
                        imagePath = relativePath,
                        timestamp = ts,
                        faceCenterX = faceX,
                        faceCenterY = faceY
                    )
                )
                written++
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
        written
    }

    /**
     * 清空所有测试数据（包括真实拍摄的照片）。
     * 调试期间反复测试用。
     */
    suspend fun wipeAll(): Int = withContext(Dispatchers.IO) {
        val all = dao.getAllAsc()
        var removed = 0
        all.forEach { p ->
            dao.deleteById(p.id)
            fileManager.deletePhoto(p.imagePath)
            removed++
        }
        removed
    }

    /**
     * 生成一张模拟肖像照
     * 1080×1440 渐变 + 居中文字（日期）
     */
    private fun generateMockPortrait(dateStr: String, indexHash: Int): Bitmap {
        val width = 1080
        val height = 1440
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 渐变背景：随天数循环 6 种调色
        val palettes = arrayOf(
            intArrayOf(Color.parseColor("#FFE0B5"), Color.parseColor("#D9683E")),  // 焦糖
            intArrayOf(Color.parseColor("#D8E8DA"), Color.parseColor("#4A7A4F")),  // 苔藓绿
            intArrayOf(Color.parseColor("#DCE4F0"), Color.parseColor("#5C7AA0")),  // 灰蓝
            intArrayOf(Color.parseColor("#F5DAD3"), Color.parseColor("#B45A4A")),  // 砖红
            intArrayOf(Color.parseColor("#EFE4D2"), Color.parseColor("#8A7A52")),  // 麦色
            intArrayOf(Color.parseColor("#E4D9E8"), Color.parseColor("#7A5C8A"))   // 雾紫
        )
        val palette = palettes[indexHash % palettes.size]

        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            palette[0], palette[1],
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 圆形"人脸"占位
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 0, 0, 0)
        }
        canvas.drawCircle(width / 2f, height / 2f - 100f, 240f, facePaint)

        // 日期文字
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 80f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(dateStr, width / 2f, height - 120f, textPaint)

        return bmp
    }
}
