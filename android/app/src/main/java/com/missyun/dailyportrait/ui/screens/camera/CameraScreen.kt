package com.missyun.dailyportrait.ui.screens.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.missyun.dailyportrait.data.storage.FileManager
import com.missyun.dailyportrait.domain.util.BitmapUtil
import com.missyun.dailyportrait.ui.components.GuideRing
import com.missyun.dailyportrait.ui.components.ShutterButton
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executor

/**
 * Camera 页面入口
 *
 * 严格按 architecture-android.md §4.2 的 UI 层叠顺序：
 * 1. AndroidView(CameraX Preview) — 全屏底层
 * 2. AsyncImage(洋葱皮叠加, alpha=0.35) — 仅 latestPhotoPath != null
 * 3. GuideRing — 居中
 * 4. 首次引导文字 — 仅 latestPhotoPath == null
 * 5. ShutterButton — 底部居中
 * 6. 倒计时大数字 — 居中遮罩
 * 7. 缩略图角标 — 拍照成功后右上
 * 8. 白闪 - shutterFlash == true
 * 9. 错误覆盖层
 *
 * @param onClose 用户点击关闭按钮 / 拍完照后回退到 Dashboard
 */
@Composable
fun CameraScreen(
    onClose: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }

    // 持有 ImageCapture 引用以便快门触发拍照
    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }

    // FileManager 单例（用 Hilt EntryPoint 在非 ViewModel/Activity 处取）
    val fileManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FileManagerEntryPoint::class.java
        ).fileManager()
    }

    // ============ 权限申请 ============
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onIntent(CameraIntent.PermissionGranted)
        else viewModel.onIntent(CameraIntent.PermissionDenied)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.onIntent(CameraIntent.PermissionGranted)
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 离开时释放分析器
    DisposableEffect(Unit) {
        onDispose { viewModel.onIntent(CameraIntent.Dispose) }
    }

    // 倒计时归零自动触发拍照
    LaunchedEffect(state.countdownSeconds) {
        if (state.countdownSeconds == 0 && !state.isCapturing) {
            imageCaptureRef.value?.let { capture ->
                takePictureAndDispatch(capture, executor) { bmp, rot ->
                    viewModel.onIntent(CameraIntent.CountdownFired(bmp, rot))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. CameraX Preview
        if (state.cameraPermissionGranted && !state.shouldHideCamera) {
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                analyzerProvider = { viewModel.acquireAnalyzer() },
                onImageCaptureReady = { imageCaptureRef.value = it },
                onCameraReady = { viewModel.onIntent(CameraIntent.CameraReady) },
                onCameraFailed = { viewModel.onIntent(CameraIntent.CameraFailed(it)) },
                executor = executor
            )
        }

        // 2. 洋葱皮叠加（半透明 0.35）
        state.latestPhotoPath?.let { rel ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fileManager.resolveAbsolutePath(rel))
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.35f)
            )
        }

        // 3. 引导环（居中）
        if (state.cameraPermissionGranted && state.isCameraReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GuideRing(alignment = state.faceAlignmentStatus)
            }
        }

        // 4. 首次使用引导文字
        if (state.latestPhotoPath == null && state.isCameraReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 220.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "拍摄你的第一张自拍\n明天起这里将显示昨天的照片作为对齐参考",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 5. 顶部关闭按钮
        TopBar(onClose = onClose)

        // 6. 缩略图角标
        AnimatedVisibility(
            visible = state.showThumbnail && state.thumbnailRelativePath != null,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 16.dp)
        ) {
            state.thumbnailRelativePath?.let { rel ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fileManager.resolveAbsolutePath(rel))
                        .build(),
                    contentDescription = "已保存今日照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 3.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }
        }

        // 7. 快门按钮
        if (!state.shouldHideCamera && !state.isPreviewing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            ) {
                ShutterButton(
                    onClick = {
                        imageCaptureRef.value?.let { capture ->
                            takePictureAndDispatch(capture, executor) { bmp, rot ->
                                viewModel.onIntent(CameraIntent.CapturePressed(bmp, rot))
                            }
                        }
                    },
                    onLongPress = { viewModel.onIntent(CameraIntent.TimerCapturePressed) },
                    enabled = state.isShutterEnabled,
                    aligned = state.faceAlignmentStatus.canCapture
                )
            }
        }

        // 7.5 预览覆盖层（拍照后等待用户确认）
        state.pendingPreview?.let { preview ->
            PreviewOverlay(
                bitmap = preview.bitmap,
                onRetake = { viewModel.onIntent(CameraIntent.Retake) },
                onConfirm = { viewModel.onIntent(CameraIntent.ConfirmSave) },
                isSaving = state.isCapturing
            )
        }

        // 8. 倒计时大数字
        AnimatedVisibility(
            visible = state.isCountingDown && (state.countdownSeconds ?: 0) > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (state.countdownSeconds ?: 0).toString(),
                    color = Color.White,
                    fontSize = 180.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.semantics {
                        contentDescription = "倒计时 ${state.countdownSeconds} 秒"
                    }
                )
            }
        }

        // 9. 白闪
        if (state.shutterFlash) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        }

        // 10. 错误覆盖层
        ErrorOverlay(
            error = state.error,
            onDismiss = { viewModel.onIntent(CameraIntent.DismissError) },
            onOpenSettings = { openAppSettings(context) }
        )
    }
}

/* ============ CameraX 预览组件 ============ */
@Composable
private fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    analyzerProvider: () -> ImageAnalysis.Analyzer,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onCameraReady: () -> Unit,
    onCameraFailed: (String) -> Unit,
    executor: Executor
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .build()
                        .apply { setSurfaceProvider(previewView.surfaceProvider) }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        // 限制输出分辨率，避免 4000×3000 大图把模拟器内存打爆
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        android.util.Size(1080, 1440),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                    )
                                )
                                .build()
                        )
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply { setAnalyzer(executor, analyzerProvider()) }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        // 硬编码前置摄像头 —— 自拍场景，按 §4.2 不提供切换
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )

                    onImageCaptureReady(imageCapture)
                    onCameraReady()
                } catch (e: Exception) {
                    onCameraFailed(e.message ?: "相机绑定失败")
                }
            }, executor)

            previewView
        }
    )
}

/* ============ takePicture 封装 ============ */
private fun takePictureAndDispatch(
    imageCapture: ImageCapture,
    executor: Executor,
    onCaptured: (android.graphics.Bitmap, Int) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = BitmapUtil.fromImageProxy(image)
                    onCaptured(bitmap, image.imageInfo.rotationDegrees)
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                // 由 ViewModel 在 capture() 协程的 catch 兜底
            }
        }
    )
}

/* ============ 顶部关闭按钮 ============ */
@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.95f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭相机",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.size(1.dp))
    }
}

/* ============ 错误覆盖层 ============ */
@Composable
private fun ErrorOverlay(
    error: CameraError?,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (error == null) return

    when (error) {
        is CameraError.PermissionDenied -> PermissionCard(
            title = "需要相机权限",
            desc = "DailyPortrait 用相机记录你每天的肖像，所有照片只保存在本机沙盒，不会上传或分享。\n\n请前往「设置 → 权限 → 相机」开启",
            primaryText = "打开设置",
            onPrimary = onOpenSettings
        )
        is CameraError.CameraUnavailable -> PermissionCard(
            title = "相机不可用",
            desc = "可能原因：\n• 其他应用正在使用相机（如视频通话）\n• 设备相机硬件暂时故障\n\n请关闭其他相机应用后重试",
            primaryText = "重试",
            onPrimary = onDismiss
        )
        is CameraError.StorageFull -> StorageFullCard(
            availableMB = (error.availableBytes / 1024 / 1024).coerceAtLeast(0L),
            onDismiss = onDismiss
        )
        is CameraError.CaptureFailed -> SnackbarBottom(
            message = "拍照失败：${error.reason}",
            actionText = "好",
            onAction = onDismiss
        )
        is CameraError.FaceDetectionInitFailed -> SnackbarBottom(
            message = "人脸识别不可用，已切换为手动拍摄",
            actionText = "继续",
            onAction = onDismiss
        )
    }
}

/**
 * 存储不足专用卡片（带可恢复 CTA）
 *
 * 交付文档 v1.1 §8 P2-#9：Snackbar 增加"清理旧照片" CTA
 * 当前实现：跳转到 Dashboard 让用户长按删除旧照片，
 * 未来可演进为应用内"清理建议"专用页
 */
@Composable
private fun StorageFullCard(
    availableMB: Long,
    onDismiss: () -> Unit
) {
    PermissionCard(
        title = "存储空间不足",
        desc = if (availableMB > 0) {
            "剩余空间 ${availableMB}MB，无法保存新照片。\n请回到主页长按旧照片删除以释放空间。"
        } else {
            "无法保存新照片。\n请回到主页长按旧照片删除以释放空间。"
        },
        primaryText = "知道了",
        onPrimary = onDismiss
    )
}

@Composable
private fun SnackbarBottom(
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 110.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = onAction) { Text(actionText) }
            }
        ) { Text(message) }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    desc: String,
    primaryText: String,
    onPrimary: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(primaryText, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/* ============ 工具：跳应用设置 ============ */
private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
}

/* ============ Hilt EntryPoint：在 Composable 中拿单例 ============ */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FileManagerEntryPoint {
    fun fileManager(): FileManager
}
