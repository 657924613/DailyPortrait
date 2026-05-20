package com.missyun.dailyportrait.di

import com.missyun.dailyportrait.data.repository.PhotoRepositoryImpl
import com.missyun.dailyportrait.domain.repository.PhotoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 依赖注入模块
 *
 * 用 @Binds 把 Domain 层接口绑定到 Data 层实现，
 * 让 ViewModel 通过接口注入，无需感知具体实现类。
 *
 * 此处启用了步骤 1 预留的 Impl 绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(
        impl: PhotoRepositoryImpl
    ): PhotoRepository
}
