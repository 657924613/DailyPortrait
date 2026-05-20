package com.missyun.dailyportrait.ui.navigation

/**
 * 路由常量
 *
 * 严格按 architecture-android.md §4.5 定义。
 * 单独放一个 object 避免散落在 NavGraph 中导致拼写不一致。
 */
object Route {
    /** 首次引导 3 页 */
    const val Onboarding = "onboarding"

    /** Dashboard 主页（启动后默认目的地） */
    const val Dashboard = "dashboard"

    /** Camera 相机页 */
    const val Camera = "camera"

    /**
     * 导出视频底部 Sheet（非全屏路由）
     * 步骤 7 的 VideoExportSheet 会在 Dashboard 内 dialog 形式呈现，
     * 不走独立路由 —— 此处保留枚举以便未来扩展为全屏路由
     */
    const val VideoExport = "videoexport"

    /** 设置页（提醒等） */
    const val Settings = "settings"

    /** 统计大图页（月度热力图 + 总览） */
    const val Statistics = "statistics"
}
