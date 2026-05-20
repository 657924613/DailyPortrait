package com.missyun.dailyportrait.di

import com.missyun.dailyportrait.data.video.Media3VideoExporter
import com.missyun.dailyportrait.domain.usecase.VideoExporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 视频合成相关依赖注入
 *
 * 把 Domain 层接口 [VideoExporter] 绑定到 Data 层 Media3 实现，
 * 让 [com.missyun.dailyportrait.domain.usecase.GenerateVideoUseCase]
 * 仅依赖接口。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VideoModule {
    @Binds
    @Singleton
    abstract fun bindVideoExporter(impl: Media3VideoExporter): VideoExporter
}
