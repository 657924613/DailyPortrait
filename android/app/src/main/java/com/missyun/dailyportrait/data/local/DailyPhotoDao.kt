package com.missyun.dailyportrait.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DailyPhoto 数据访问对象
 *
 * 严格遵循 architecture-android.md 禁止事项：
 * - 所有变更操作返回 [suspend] 函数，强制调用方在协程中执行（禁主线程 I/O）
 * - 所有查询返回 [Flow]，UI 层订阅即可获得反应式更新
 * - 插入冲突策略 [OnConflictStrategy.REPLACE]：同一天重拍时覆盖旧记录
 *   （唯一约束建立在 date 列上，参考 [DailyPhoto] 的 indices 定义）
 *
 * 命名约定：
 * - observeXxx() 返回 Flow（响应式）
 * - getXxx() 返回 suspend 单值（一次性查询）
 * - upsertXxx() / deleteXxx() 表示写操作
 */
@Dao
interface DailyPhotoDao {

    /**
     * 插入或替换一张照片记录
     * date 列冲突时整行替换，保证一天一条最终记录
     *
     * @return 插入后的行 id
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: DailyPhoto): Long

    /**
     * 更新已存在的记录（按 id 主键定位）
     */
    @Update
    suspend fun update(photo: DailyPhoto)

    /**
     * 按 date 删除某天的记录
     * @return 受影响行数（0 表示当日无记录）
     */
    @Query("DELETE FROM daily_photo WHERE date = :date")
    suspend fun deleteByDate(date: String): Int

    /**
     * 按 id 删除（Dashboard 长按删除场景）
     */
    @Query("DELETE FROM daily_photo WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * 观察所有照片，按时间倒序
     * Dashboard 历史宫格订阅此流，数据变更自动重组
     */
    @Query("SELECT * FROM daily_photo ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DailyPhoto>>

    /**
     * 按时间正序导出（视频合成时使用，最早的在前）
     */
    @Query("SELECT * FROM daily_photo ORDER BY timestamp ASC")
    suspend fun getAllAsc(): List<DailyPhoto>

    /**
     * 查询某天是否已经打卡（CameraViewModel 进入时判定）
     */
    @Query("SELECT * FROM daily_photo WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyPhoto?

    /**
     * 观察"今日是否已打卡" —— Dashboard 今日卡片订阅
     */
    @Query("SELECT * FROM daily_photo WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DailyPhoto?>

    /**
     * 获取最近一张照片（用于洋葱皮叠加）
     */
    @Query("SELECT * FROM daily_photo ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): DailyPhoto?

    /**
     * 观察最近一张（Camera 进入时洋葱皮路径来源）
     */
    @Query("SELECT * FROM daily_photo ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<DailyPhoto?>

    /**
     * 总记录数
     * 用途：
     * - Dashboard 空状态判定（== 0）
     * - 导出按钮 enabled 判定（>= 2）
     */
    @Query("SELECT COUNT(*) FROM daily_photo")
    fun observeCount(): Flow<Int>

    /**
     * 区间查询（本周进度环 / 月度统计扩展用）
     * date 字符串按 yyyy-MM-dd 格式做字典序比较，可正确反映时间顺序
     *
     * @param start 起始日期（含），yyyy-MM-dd
     * @param end 结束日期（含），yyyy-MM-dd
     */
    @Query("SELECT * FROM daily_photo WHERE date >= :start AND date <= :end ORDER BY date ASC")
    fun observeRange(start: String, end: String): Flow<List<DailyPhoto>>
}
