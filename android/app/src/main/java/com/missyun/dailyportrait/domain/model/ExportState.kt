package com.missyun.dailyportrait.domain.model

/**
 * 视频合成状态
 *
 * v2 保持原有 6 态密封类不变。
 */
sealed class ExportState {
    data object Idle : ExportState()
    data object Preparing : ExportState()
    data class Progress(val percent: Int) : ExportState()
    data class Completed(val outputPath: String) : ExportState()
    data class Failed(val reason: String) : ExportState()
    data object Cancelled : ExportState()
}

/**
 * 视频画质档位
 *
 * v2 新增。对应中国视频网站惯用的 4 档分类，
 * 每档决定输出分辨率与文件体积。
 *
 * @property displayName 用户可见名称
 * @property width 输出宽度（像素）
 * @property height 输出高度（像素，9:16 竖屏）
 * @property hint 副说明
 */
enum class VideoQuality(
    val displayName: String,
    val width: Int,
    val height: Int,
    val hint: String
) {
    Smooth("流畅", 720, 1280, "体积最小"),
    Standard("标清", 1080, 1920, "推荐"),
    High("高清", 1440, 2560, "更清晰"),
    Ultra("超清", 2160, 3840, "体积最大");

    companion object {
        val DEFAULT = Standard
    }
}

/**
 * 日期水印格式
 *
 * v2 暂未实装：Media3 Transformer 烧录文字到视频帧需要 OverlayEffect，
 * 当前阶段保留枚举但 UI 不暴露开关，避免给用户假承诺。
 * 真实现见 v3 路线（OverlayEffect + Canvas 文字渲染）
 */
enum class DateFormat(val displayName: String) {
    YearMonthDay("年月日"),
    MonthDay("月日");

    companion object {
        val DEFAULT = YearMonthDay
    }
}

/**
 * 视频合成参数
 *
 * @property frameRate 帧率 2~20，默认 5
 * @property quality 画质档位
 * @property crossfadeMs 相邻照片之间淡入淡出（保留字段）
 * @property useAlignedOnly 是否仅使用已对齐过的照片
 */
data class ExportConfig(
    val frameRate: Int = 5,
    val quality: VideoQuality = VideoQuality.DEFAULT,
    val crossfadeMs: Int = 250,
    val useAlignedOnly: Boolean = false
) {
    /**
     * 单张照片展示帧数
     * 固定 1 秒展示 = frameRate 帧
     */
    val frameDurationMs: Int get() = 1000

    /** 单张照片展示时长（微秒） */
    val frameDurationUs: Long get() = frameDurationMs * 1000L

    /** 输出宽度 */
    val width: Int get() = quality.width

    /** 输出高度 */
    val height: Int get() = quality.height
}
