package com.missyun.dailyportrait.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missyun.dailyportrait.ui.theme.DPDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Onboarding v3 —— Immersive Visual Storytelling
 *
 * 设计升级：
 * - 每页顶部 50% 区域为大图标 + 渐变光晕背景（视觉冲击）
 * - 图标带缩放 + 淡入动画（有生命力）
 * - 文案区域居中对齐，标题加大加粗
 * - 底部按钮保持药丸形态，最后一页变焦糖橘色
 * - 页面指示器用胶囊形态（当前页拉长）
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = remember { onboardingPages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    // Circular Reveal 动画状态
    var showReveal by remember { mutableStateOf(false) }
    val revealProgress = remember { Animatable(0f) }

    // 当 showReveal 触发时，播放动画然后导航
    LaunchedEffect(showReveal) {
        if (showReveal) {
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
            // 动画播完后导航
            viewModel.finishOnboarding(onFinished)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 跳过按钮（右上）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, end = 16.dp)
            ) {
                if (!isLastPage) {
                    TextButton(
                        onClick = { viewModel.finishOnboarding(onFinished) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .sizeIn(
                                minWidth = DPDimens.TouchPrimary,
                                minHeight = DPDimens.TouchPrimary
                            )
                            .semantics { contentDescription = "跳过引导" }
                    ) {
                        Text(
                            text = "跳过",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 主内容（Pager）
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                ImmersivePage(
                    page = page,
                    isActive = pagerState.currentPage == pageIndex
                )
            }

            // 底部区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 页面指示器
                PageIndicator(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage
                )

                // 主按钮
                PrimaryPill(
                    text = if (isLastPage) "开始记录" else "下一步",
                    isAccent = isLastPage,
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            // 触发 Circular Reveal 动画
                            showReveal = true
                        }
                    }
                )
            }
        }

        // Circular Reveal 覆盖层
        if (showReveal) {
            val maxRadius = 2000f // 足够覆盖整个屏幕的半径
            val currentRadius = maxRadius * revealProgress.value
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            color = Color(0xFF2C2520), // 深棕墨色
                            radius = currentRadius,
                            center = center
                        )
                    }
            )
        }
    }
}

/**
 * 沉浸式页面：大图标光晕 + 文案
 */
@Composable
private fun ImmersivePage(
    page: OnboardingPage,
    isActive: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    val iconScale = remember { Animatable(0.6f) }
    val iconAlpha = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            visible = false
            delay(100)
            visible = true
            // 图标动画
            launch {
                iconAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
            launch {
                iconScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            }
        } else {
            iconScale.snapTo(0.6f)
            iconAlpha.snapTo(0f)
            visible = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标光晕区域
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(iconScale.value)
                .alpha(iconAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            // 光晕背景
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                page.glowColor.copy(alpha = 0.2f),
                                page.glowColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            // 内圈
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = page.glowColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.glowColor,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 文案区域（带入场动画）
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(500, delayMillis = 200)
            ) + fadeIn(animationSpec = tween(500, delayMillis = 200))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = page.eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = page.glowColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 40.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "引导页 ${currentPage + 1} / $pageCount"
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (active) 28.dp else 6.dp)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
private fun PrimaryPill(text: String, isAccent: Boolean, onClick: () -> Unit) {
    val bgColor = if (isAccent) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface
    val textColor = if (isAccent) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = DPDimens.TouchPrimary)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )
        }
    }
}

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val glowColor: Color
)

private fun onboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        eyebrow = "第一步 · 记录",
        title = "每天一张\n属于你的肖像",
        description = "在固定时间记录此刻的自己\n累积属于你的视觉档案",
        icon = Icons.Filled.CameraAlt,
        glowColor = Color(0xFFD9683E) // 焦糖橘
    ),
    OnboardingPage(
        eyebrow = "第二步 · 对齐",
        title = "洋葱皮叠加\n构图始终一致",
        description = "上一张照片半透明叠加在取景框\n帮你保持角度和位置稳定",
        icon = Icons.Filled.Layers,
        glowColor = Color(0xFF4A7A4F) // 绿色
    ),
    OnboardingPage(
        eyebrow = "第三步 · 回顾",
        title = "时间的痕迹\n变成动人短片",
        description = "365 天的坚持\n自动合成几十秒延时视频",
        icon = Icons.Filled.PlayCircle,
        glowColor = Color(0xFF5B7EC2) // 蓝色
    )
)
