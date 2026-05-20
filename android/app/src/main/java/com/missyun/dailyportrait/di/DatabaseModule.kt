package com.missyun.dailyportrait.di

import android.content.Context
import androidx.room.Room
import com.missyun.dailyportrait.data.local.AppDatabase
import com.missyun.dailyportrait.data.local.DailyPhotoDao
import com.missyun.dailyportrait.data.local.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room 数据库依赖注入模块
 *
 * 严格遵循禁止事项：
 * - 严禁 fallbackToDestructiveMigration（用户照片数据不可丢）
 * - 严禁 allowMainThreadQueries（强制协程访问）
 *
 * 提供：
 * - [AppDatabase] 单例
 * - [DailyPhotoDao] 单例（由 Database 派发，无需独立 @Singleton）
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context = context.applicationContext,
            klass = AppDatabase::class.java,
            name = AppDatabase.DATABASE_NAME
        )
            // 注入所有 Migration（步骤 1 暂为空数组，预留升级路径）
            .addMigrations(*Migrations.ALL)
            // 严禁 fallbackToDestructiveMigration —— 用户照片数据无价
            // .fallbackToDestructiveMigration() ← 不要打开
            .build()
    }

    @Provides
    fun provideDailyPhotoDao(database: AppDatabase): DailyPhotoDao =
        database.dailyPhotoDao()
}
