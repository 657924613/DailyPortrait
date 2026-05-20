package com.missyun.dailyportrait.ui.screens.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.missyun.dailyportrait.data.local.DailyPhoto
import com.missyun.dailyportrait.data.storage.FileManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 照片详情大图 Dialog（v3 升级为左右滑动浏览）
 *
 * - 接收完整照片列表 + 当前点击的索引
 * - HorizontalPager 支持左右滑动切换
 * - 顶部显示"X / N"位置指示
 * - 删除当前页时自动跳到下一张（或 dismiss 如果只剩 0 张）
 *
 * @param photos 完整照片列表（与 Dashboard 显示顺序一致）
 * @param initialIndex 用户点击的索引
 * @param onDismiss 关闭
 * @param onRequestDelete 请求删除某张照片（带 photo 参数）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoDetailDialog(
    photos: List<DailyPhoto>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onRequestDelete: (DailyPhoto) -> Unit
) {
    if (photos.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    val fileManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PhotoDetailFileManagerEntryPoint::class.java
        ).fileManager()
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size }
    )

    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .semantics {
                    contentDescription = "照片详情,共 ${photos.size} 张,当前 ${pagerState.currentPage + 1}"
                }
        ) {
            // 横向滑动浏览所有照片
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val photo = photos[pageIndex]
                val absolutePath = remember(photo.imagePath) {
                    fileManager.resolveAbsolutePath(photo.imagePath)
                }
                AsyncImage(
                    model = ImageRequest.Builder(context).data(absolutePath).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp, bottom = 100.dp)
                )
            }

            // 顶部栏：关闭按钮 + 位置指示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
                // 位置指示
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            // 底部信息栏
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatChinese(currentPhoto.date),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTime(currentPhoto.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = { onRequestDelete(currentPhoto) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除这张照片",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** yyyy-MM-dd → 中文 5 月 18 日 */
private fun formatChinese(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    return "${parts[0]}年${month}月${day}日"
}

/** 时间戳 → HH:mm */
private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return "拍摄于 " + sdf.format(java.util.Date(timestamp))
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PhotoDetailFileManagerEntryPoint {
    fun fileManager(): FileManager
}
