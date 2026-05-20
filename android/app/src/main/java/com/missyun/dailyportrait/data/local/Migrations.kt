package com.missyun.dailyportrait.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库迁移占位
 *
 * 严格按 architecture-android.md §4.1 与禁止事项 §7：
 * - 每次 schema 变更必须有对应 Migration
 * - 严禁使用 fallbackToDestructiveMigration（会丢用户数据）
 *
 * 当前 v1 版本，未发生过迁移。
 * 将来 schema 升级时按以下模式增加：
 *
 * ```
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE daily_photo ADD COLUMN note TEXT")
 *     }
 * }
 * ```
 *
 * 然后在 [AppDatabase] 的 builder 上调用 `.addMigrations(MIGRATION_1_2)`。
 */
object Migrations {

    /**
     * 所有已声明的迁移路径
     * Database builder 通过 spread operator 注入：`addMigrations(*Migrations.ALL)`
     */
    val ALL: Array<Migration> = arrayOf(
        // v1 是首版，暂无迁移
        // MIGRATION_1_2,
        // MIGRATION_2_3,
    )
}
