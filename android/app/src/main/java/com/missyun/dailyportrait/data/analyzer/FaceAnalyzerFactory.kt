package com.missyun.dailyportrait.data.analyzer

import com.missyun.dailyportrait.domain.model.FaceAnalysis
import com.missyun.dailyportrait.domain.model.NormalizedPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FaceAnalyzer 工厂
 *
 * 作用：
 * - 让 [com.missyun.dailyportrait.ui.screens.camera.CameraViewModel] 通过 Hilt 注入工厂，
 *   而不是直接 new [FaceAnalyzer]
 * - ViewModel 拿到上一张人脸坐标后调 [create] 得到分析器实例，
 *   离开 Camera 页时调 analyzer.close() 释放 native 句柄
 *
 * 之所以不直接把 FaceAnalyzer 注入 ViewModel：
 * - FaceAnalyzer 需要在每次进入相机时根据"是否首拍"重建（targetCenter 不同）
 * - 一个 ViewModel 生命周期可能创建多次 analyzer（用户重拍后阈值参考点会变）
 */
@Singleton
class FaceAnalyzerFactory @Inject constructor() {

    /**
     * 创建一个分析器
     *
     * @param targetCenter 对齐目标。null = 首拍对屏幕中心；非 null = 对齐上张人脸
     * @param onResult 每帧分析回调（CameraX 后台线程调用）
     */
    fun create(
        targetCenter: NormalizedPoint?,
        onResult: (FaceAnalysis) -> Unit
    ): FaceAnalyzer = FaceAnalyzer(targetCenter, onResult)
}
