package com.missyun.dailyportrait.domain.usecase

import com.missyun.dailyportrait.domain.model.ExportConfig
import com.missyun.dailyportrait.domain.model.ExportState
import com.missyun.dailyportrait.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 视频合成 UseCase（Domain 层入口）
 *
 * 职责：
 * - 校验照片数量（< 2 直接 Failed）
 * - 委托给 [VideoExporter] 抽象（Data 层 Media3 实现）
 * - 转发其进度 Flow 给上层
 *
 * UseCase 不直接 import Media3，让 Domain 层保持纯 Kotlin。
 * Service 层注入此 UseCase，UI 层通过 ViewModel 间接消费。
 */
class GenerateVideoUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val exporter: VideoExporter
) {
    /**
     * 启动合成
     *
     * @param config 输出参数
     * @return 进度 Flow，每个值是 [ExportState] 的某个子类
     *
     * 调用方应通过 [kotlinx.coroutines.flow.collect] 收集；
     * 协程取消时 [VideoExporter] 内部会调 Transformer.cancel
     */
    operator fun invoke(config: ExportConfig = ExportConfig()): Flow<ExportState> = flow {
        emit(ExportState.Preparing)

        val all = photoRepository.getAllAsc()
        val photos = if (config.useAlignedOnly) {
            all.filter { it.faceCenterX != null && it.faceCenterY != null }
        } else all

        if (photos.size < MIN_PHOTOS_REQUIRED) {
            val reason = if (config.useAlignedOnly && all.size >= MIN_PHOTOS_REQUIRED) {
                "已对齐的照片不足 $MIN_PHOTOS_REQUIRED 张，请关闭「仅使用已对齐照片」"
            } else {
                "至少需要 $MIN_PHOTOS_REQUIRED 张照片才能生成视频"
            }
            emit(ExportState.Failed(reason))
            return@flow
        }

        // 转发 Exporter 的进度
        exporter.export(photos, config).collect { state -> emit(state) }
    }

    companion object {
        const val MIN_PHOTOS_REQUIRED = 2
    }
}

/**
 * 视频导出能力的抽象（Domain 层接口）
 *
 * Data 层用 Media3 Transformer 实现。
 * 单元测试可以替换为 fake 实现，便于覆盖 UseCase 的边界逻辑。
 */
interface VideoExporter {
    fun export(
        photos: List<com.missyun.dailyportrait.data.local.DailyPhoto>,
        config: ExportConfig
    ): Flow<ExportState>
}
