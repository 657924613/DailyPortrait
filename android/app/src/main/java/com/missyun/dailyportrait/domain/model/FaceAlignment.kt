package com.missyun.dailyportrait.domain.model

/**
 * 人脸对齐状态
 *
 * 严格按 architecture-android.md §4.2 定义。
 * UI 层据此渲染引导环颜色（虚线灰 / 实线红 / 实线绿+脉冲）
 * 与色盲友好的 ✓ / ✗ 图标。
 */
enum class FaceAlignment {
    /** 未检测到人脸 —— 灰色虚线引导环 */
    NONE,

    /** 检测到人脸但偏离目标 —— 红色实线 + ✗ 图标 */
    DETECTED,

    /** 已对齐目标位置 —— 绿色实线 + 脉冲 + ✓ 图标 */
    ALIGNED;

    /** 是否允许触发拍照（仅 ALIGNED 状态可拍） */
    val canCapture: Boolean
        get() = this == ALIGNED
}

/**
 * 一次人脸分析结果
 *
 * @property alignment 状态枚举
 * @property normalizedCenter 归一化人脸中心坐标 (0~1)，未检测到则为 null
 *                            归一化方便跨分辨率 / 跨摄像头复用
 * @property boundingBoxRatio 人脸边框占画面的比例 (0~1)，可用于 UI 提示"离远点 / 凑近"
 *                            未检测到则为 null
 */
data class FaceAnalysis(
    val alignment: FaceAlignment,
    val normalizedCenter: NormalizedPoint? = null,
    val boundingBoxRatio: Float? = null
) {
    companion object {
        /** 未检测到人脸时的默认值 */
        val EMPTY = FaceAnalysis(alignment = FaceAlignment.NONE)
    }
}

/**
 * 归一化二维坐标 (0~1)
 * 与具体像素分辨率解耦，便于跨分辨率比较
 */
data class NormalizedPoint(
    val x: Float,
    val y: Float
) {
    init {
        require(x in 0f..1f) { "x 必须在 0~1 范围内: $x" }
        require(y in 0f..1f) { "y 必须在 0~1 范围内: $y" }
    }

    /** 与另一点的欧式距离（仍归一化） */
    fun distanceTo(other: NormalizedPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    companion object {
        /** 屏幕几何中心，首拍的对齐目标 */
        val CENTER = NormalizedPoint(0.5f, 0.5f)
    }
}
