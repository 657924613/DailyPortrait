package com.missyun.dailyportrait.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日肖像数据实体
 *
 * 严格按 architecture-android.md §4.1 规范：
 * - [date] 建立 Unique 索引，保证每天只有一条最终记录（重拍后覆盖）
 * - [imagePath] 必须存储相对路径，防止 Android 沙盒绝对路径变更导致图片失效
 * - [timestamp] 拍摄毫秒戳，用于二级排序与"最近一张"查询
 * - [faceCenterX] / [faceCenterY] 为人脸中心点归一化坐标（0~1），
 *   后续拍摄对齐参考此前一张的位置（§4.2）
 *
 * @property id 主键，自增
 * @property date 拍摄日期 yyyy-MM-dd（业务唯一键）
 * @property imagePath 相对沙盒图片路径（相对于 getExternalFilesDir(DIRECTORY_PICTURES)）
 * @property timestamp 拍摄时刻的 System.currentTimeMillis()
 * @property faceCenterX 人脸中心 x 坐标，0~1，可空（首拍未检测时为 null）
 * @property faceCenterY 人脸中心 y 坐标，0~1，可空
 */
@Entity(
    tableName = "daily_photo",
    indices = [
        // date 列唯一索引：保证一天一条
        Index(value = ["date"], unique = true),
        // timestamp 索引：加速按时间倒序查询历史
        Index(value = ["timestamp"])
    ]
)
data class DailyPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "image_path")
    val imagePath: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "face_center_x")
    val faceCenterX: Float? = null,

    @ColumnInfo(name = "face_center_y")
    val faceCenterY: Float? = null
)
