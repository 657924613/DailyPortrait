package com.missyun.dailyportrait.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.missyun.dailyportrait.data.storage.FileManager
import com.missyun.dailyportrait.ui.theme.DPColors
import com.missyun.dailyportrait.ui.theme.DPDimens
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 历史宫格缩略图
 *
 * Soft Cloud 风格：
 * - 18dp 圆角
 * - 占位渐变按 index % 6 循环（呼应原型 prototype-2）
 * - 单击进入大图（步骤 5 仅触发回调）
 * - 长按进入删除流程
 */
@OptIn(ExperimentalComposeUiApi::class)
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun HistoryThumb(
    relativePath: String,
    index: Int,
    dateLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            HistoryThumbFileManagerEntryPoint::class.java
        ).fileManager()
    }
    val absolutePath = remember(relativePath) { fileManager.resolveAbsolutePath(relativePath) }
    val gradientPair = gradientForIndex(index)
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(elevation = DPDimens.Elev1, shape = shape, clip = false)
            .clip(shape)
            .background(brush = Brush.linearGradient(gradientPair), shape = shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics { contentDescription = "$dateLabel 的肖像，点击查看，长按删除" }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(absolutePath).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun gradientForIndex(index: Int): List<Color> = when (index % 6) {
    0 -> listOf(DPColors.HistGradient1Start, DPColors.HistGradient1End)
    1 -> listOf(DPColors.HistGradient2Start, DPColors.HistGradient2End)
    2 -> listOf(DPColors.HistGradient3Start, DPColors.HistGradient3End)
    3 -> listOf(DPColors.HistGradient4Start, DPColors.HistGradient4End)
    4 -> listOf(DPColors.HistGradient5Start, DPColors.HistGradient5End)
    else -> listOf(DPColors.HistGradient6Start, DPColors.HistGradient6End)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HistoryThumbFileManagerEntryPoint {
    fun fileManager(): FileManager
}
