package com.missyun.dailyportrait.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 应用全局数据库
 *
 * 严格按 architecture-android.md §4.1 规范：
 * - 仅一个 Entity ([DailyPhoto])，未来扩展（如标签、注释）需 schema 升级 + Migration
 * - exportSchema = true：CI/CD 可基于 schema 文件做版本对比测试
 * - schema 文件输出路径：`app/schemas/com.missyun.dailyportrait.data.local.AppDatabase/1.json`
 *   （需在 build.gradle 里配置 KSP 参数 `room.schemaLocation`，本步骤已完成）
 *
 * 实例由 Hilt [com.missyun.dailyportrait.di.DatabaseModule] 提供，
 * UI 层禁止直接 newInstance —— 走依赖注入。
 */
@Database(
    entities = [DailyPhoto::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyPhotoDao(): DailyPhotoDao

    companion object {
        /** 数据库文件名 */
        const val DATABASE_NAME = "daily_portrait.db"
    }
}
