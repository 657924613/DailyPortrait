package com.missyun.dailyportrait.data.repository

import android.graphics.Bitmap
import com.missyun.dailyportrait.data.local.DailyPhoto
import com.missyun.dailyportrait.data.local.DailyPhotoDao
import com.missyun.dailyportrait.data.storage.FileManager
import com.missyun.dailyportrait.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 照片仓储实现
 *
 * 严格遵循 Clean Architecture：
 * - UI / ViewModel 通过 [PhotoRepository] 接口注入，不感知此类
 * - 此类编排 [DailyPhotoDao]（数据库）+ [FileManager]（文件系统）的协作
 * - 不在此层做线程切换：[FileManager] 内部已 `withContext(Dispatchers.IO)`，
 *   [DailyPhotoDao] 由 Room 自动调度到 IO 线程
 *
 * 关键设计：
 * - [savePhoto] 是"先写盘后写库"——先把图片落到沙盒拿到相对路径，
 *   再写入 Room。这样数据库永远不会有指向不存在文件的脏记录。
 * - [deletePhoto] 反之"先删库后删盘"——避免短暂的"记录在但文件没了"窗口
 *   被 Coil 命中导致 UI 报错。
 */
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val dao: DailyPhotoDao,
    private val fileManager: FileManager
) : PhotoRepository {

    // ============ 响应式查询 ============

    override fun observeAll(): Flow<List<DailyPhoto>> = dao.observeAll()

    override fun observeToday(date: String): Flow<DailyPhoto?> = dao.observeByDate(date)

    override fun observeLatest(): Flow<DailyPhoto?> = dao.observeLatest()

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override fun observeRange(start: String, end: String): Flow<List<DailyPhoto>> =
        dao.observeRange(start, end)

    // ============ 一次性查询 ============

    override suspend fun getByDate(date: String): DailyPhoto? = dao.getByDate(date)

    override suspend fun getLatest(): DailyPhoto? = dao.getLatest()

    override suspend fun getAllAsc(): List<DailyPhoto> = dao.getAllAsc()

    // ============ 写操作 ============

    /**
     * 保存新拍照片
     *
     * 流程：
     * 1. 用 FileManager 把 Bitmap 落盘（同时完成 EXIF 修正 + WEBP 压缩 + 缩放）
     * 2. 拿到相对路径后写入 Room（date 唯一索引会自动覆盖同日旧记录）
     * 3. 如果旧记录存在，且文件路径不同（极少见，例如旧版用过 jpg），删除旧文件
     *
     * 注意：调用方传入的 [bitmap] 由其自行回收，本方法不 recycle
     */
    override suspend fun savePhoto(
        bitmap: Bitmap,
        date: String,
        timestamp: Long,
        faceCenterX: Float?,
        faceCenterY: Float?
    ): String {
        val fileName = FileManager.fileNameFor(date)

        // 检查是否已存在同日记录（重拍场景）
        val existing = dao.getByDate(date)

        // 1. 写盘 —— 失败会抛 IOException，由 ViewModel 转 CameraError
        val relativePath = fileManager.savePhoto(
            bitmap = bitmap,
            fileName = fileName,
            // CameraX 的 ImageProxy.imageInfo.rotationDegrees 已被 ViewModel 转换为 EXIF orientation
            // 这里传入的 bitmap 默认已是 ORIENTATION_NORMAL，旋转修正在 ViewModel 把 ImageProxy → Bitmap 阶段完成
            // 此处 FileManager 只对"再次写入"的兜底
        )

        // 2. 写库
        val photo = DailyPhoto(
            // 复用旧 id 让 REPLACE 触发整行替换；新记录则 0 自增
            id = existing?.id ?: 0L,
            date = date,
            imagePath = relativePath,
            timestamp = timestamp,
            faceCenterX = faceCenterX,
            faceCenterY = faceCenterY
        )
        dao.upsert(photo)

        // 3. 极端情况：旧文件名与新文件名不同（如曾经的 jpg 升级到 webp）
        //    新 fileName 已覆盖落盘，需清理旧的"孤儿"文件
        if (existing != null && existing.imagePath != relativePath) {
            fileManager.deletePhoto(existing.imagePath)
        }

        return relativePath
    }

    /**
     * 删除一张照片（硬删，立即不可恢复）
     *
     * 顺序：先删库 → 再删盘
     */
    override suspend fun deletePhoto(photo: DailyPhoto) {
        dao.deleteById(photo.id)
        fileManager.deletePhoto(photo.imagePath)
    }

    /**
     * 软删除：仅从 DB 移除，磁盘文件保留
     * 让 UI 层能在撤销窗口期内通过 [restorePhoto] 恢复
     */
    override suspend fun softDeletePhoto(photo: DailyPhoto) {
        dao.deleteById(photo.id)
    }

    /**
     * 恢复软删除的照片
     * REPLACE 策略保证若用户在撤销窗口期间又拍了同日新照，不会覆盖
     */
    override suspend fun restorePhoto(photo: DailyPhoto) {
        // 用 upsert 让 date 唯一索引在冲突时不报错
        dao.upsert(photo)
    }

    /**
     * 真删磁盘文件
     */
    override suspend fun purgeFile(relativePath: String) {
        fileManager.deletePhoto(relativePath)
    }

    override suspend fun availableBytes(): Long = fileManager.availableBytes()
}
