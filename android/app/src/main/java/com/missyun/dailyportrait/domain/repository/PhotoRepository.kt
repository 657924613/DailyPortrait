package com.missyun.dailyportrait.domain.repository

import com.missyun.dailyportrait.data.local.DailyPhoto
import kotlinx.coroutines.flow.Flow

/**
 * 照片仓储接口（Domain 层）
 *
 * UI 层与 ViewModel 仅依赖此接口，不直接接触 Room/FileManager 实现。
 * 这样保证 Clean Architecture 的依赖方向：UI → Domain ← Data。
 *
 * 步骤 2 将在 Data 层提供 [PhotoRepositoryImpl] 实现。
 */
interface PhotoRepository {

    /**
     * 观察所有照片，按时间倒序
     */
    fun observeAll(): Flow<List<DailyPhoto>>

    /**
     * 观察今日是否已打卡
     */
    fun observeToday(date: String): Flow<DailyPhoto?>

    /**
     * 观察最近一张（用于洋葱皮叠加路径）
     */
    fun observeLatest(): Flow<DailyPhoto?>

    /**
     * 观察照片总数
     */
    fun observeCount(): Flow<Int>

    /**
     * 观察日期区间（周进度环用）
     */
    fun observeRange(start: String, end: String): Flow<List<DailyPhoto>>

    /**
     * 查询某天记录（一次性）
     */
    suspend fun getByDate(date: String): DailyPhoto?

    /**
     * 获取最近一张（一次性，CameraViewModel 启动时获取洋葱皮路径）
     */
    suspend fun getLatest(): DailyPhoto?

    /**
     * 升序读取所有照片（视频合成用）
     */
    suspend fun getAllAsc(): List<DailyPhoto>

    /**
     * 保存一张新拍照片
     *
     * 实现细节（见 PhotoRepositoryImpl）：
     * 1. 通过 FileManager 把 Bitmap 压缩 + EXIF 修正 + 保存到沙盒
     * 2. 把返回的相对路径写入 Room
     * 3. 同一天再拍则覆盖（Dao 已配置 OnConflictStrategy.REPLACE）
     *
     * @return 保存后的相对路径（用于 UI 即时显示）
     */
    suspend fun savePhoto(
        bitmap: android.graphics.Bitmap,
        date: String,
        timestamp: Long,
        faceCenterX: Float?,
        faceCenterY: Float?
    ): String

    /**
     * 删除指定 id 的记录（同时删除磁盘文件）—— 直接硬删，不可撤销
     */
    suspend fun deletePhoto(photo: DailyPhoto)

    /**
     * 软删除：仅从数据库移除记录，磁盘文件保留。
     * 用于"删除 + 撤销 Snackbar" 场景，配合 [restorePhoto] 实现可撤销删除。
     *
     * @param photo 待删除的记录
     */
    suspend fun softDeletePhoto(photo: DailyPhoto)

    /**
     * 恢复软删除的照片 —— 把记录重新写回数据库
     * @param photo 包含原 id / date / imagePath 的完整记录
     */
    suspend fun restorePhoto(photo: DailyPhoto)

    /**
     * 真删磁盘文件 —— 由 ViewModel 在撤销窗口结束后调用
     * @param relativePath 沙盒相对路径
     */
    suspend fun purgeFile(relativePath: String)

    /**
     * 当前沙盒剩余字节数（用于 StorageFull 错误反馈真实数值）
     */
    suspend fun availableBytes(): Long
}
