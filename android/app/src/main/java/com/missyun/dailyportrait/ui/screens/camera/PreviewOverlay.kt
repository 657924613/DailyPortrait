package com.missyun.dailyportrait.ui.screens.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.missyun.dailyportrait.ui.theme.DPDimens

/**
 * 拍照预览覆盖层
 *
 * 全屏盖住相机预览，显示用户刚拍的 Bitmap，
 * 提供"重拍 / 使用这张"两个操作。
 *
 * 严格遵循交付文档 v1.1 §3.2 P1：
 * - 体验补齐："拍完后允许确认或重拍，避免强迫接受"
 * - WCAG：触控按钮 ≥ 48dp + 文本对比度
 *
 * @param bitmap 待预览的图片
 * @param onRetake 重拍：回到取景
 * @param onConfirm 使用这张：保存到相册
 * @param isSaving 保存进行中（防连点）
 */
@Composable
fun PreviewOverlay(
    bitmap: Bitmap,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .semantics { contentDescription = "拍照预览，可以重拍或保存" }
    ) {
        // 大图填满
        Image(
            bitmap = imageBitmap,
            contentDescription = "刚刚拍摄的照片",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp)
        )

        // 底部操作栏（半透明 + 模糊感）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(
                    horizontal = DPDimens.ScreenPadding,
                    vertical = DPDimens.Space5
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 重拍按钮（次要）
                OutlinedButton(
                    onClick = onRetake,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(DPDimens.TouchPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "  重拍",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }

                // 使用这张按钮（主操作）
                Button(
                    onClick = onConfirm,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(DPDimens.TouchPrimary),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isSaving) "  保存中…" else "  使用这张",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
