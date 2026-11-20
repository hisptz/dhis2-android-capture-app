package org.dhis2.mobile.omrocr.ui.contracts

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.dhis2.mobile.omrocr.data.OpenCv
import org.dhis2.mobile.omrocr.ui.camera.AutoCaptureState
import org.dhis2.mobile.omrocr.ui.component.SHUTTER_BUTTON_SIZE
import org.dhis2.mobile.omrocr.ui.component.ScanGuideOverlay
import org.dhis2.mobile.omrocr.ui.component.ShutterButton
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

private val CAPTURE_TARGET_SIZE = Size(2560, 1920)
private val PREVIEW_TARGET_SIZE = Size(1600, 1200)
private val ANALYSIS_TARGET_SIZE = Size(640, 480)
private val SHUTTER_PADDING = 24.dp
private val SHUTTER_AREA_HEIGHT = SHUTTER_BUTTON_SIZE + SHUTTER_PADDING * 2

@Composable
actual fun CameraCapture(
    onImageCaptured: (ImageBitmap) -> Unit,
    onCaptureFailed: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            if (!granted) onPermissionDenied()
        }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        CameraPreview(
            modifier = modifier,
            onImageCaptured = onImageCaptured,
            onCaptureFailed = onCaptureFailed,
        )
    }
}

@Composable
private fun CameraPreview(
    onImageCaptured: (ImageBitmap) -> Unit,
    onCaptureFailed: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val previewView =
        remember {
            PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        }
    var frameAspect by remember(isLandscape) { mutableFloatStateOf(if (isLandscape) 4f / 3f else 3f / 4f) }
    val guideInFrame = remember { AtomicReference<Rect?>(null) }
    val imageCapture =
        remember {
            ImageCapture
                .Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setResolutionSelector(fourByThreeSelector(CAPTURE_TARGET_SIZE))
                .build()
        }
    val imageAnalysis =
        remember {
            ImageAnalysis
                .Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(fourByThreeSelector(ANALYSIS_TARGET_SIZE))
                .build()
        }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var isTakingPhoto by remember { mutableStateOf(false) }
    var autoCaptureState by remember { mutableStateOf<AutoCaptureState>(AutoCaptureState.Searching) }

    val takePhoto = {
        if (!isTakingPhoto) {
            isTakingPhoto = true
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = image.use { it.toUprightBitmap() }
                        // The capture flag stays set so frames still being analysed can't take a second photo.
                        onImageCaptured(bitmap.asImageBitmap())
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isTakingPhoto = false
                        onCaptureFailed()
                    }
                },
            )
        }
    }

    LaunchedEffect(autoCaptureState) {
        if (autoCaptureState is AutoCaptureState.Ready) takePhoto()
    }

    DisposableEffect(lifecycleOwner) {
        var isDisposed = false
        val mainExecutor = ContextCompat.getMainExecutor(context)
        imageAnalysis.setAnalyzer(
            analysisExecutor,
            CornerMarkerAnalyzer(
                isAvailable = { OpenCv.isLoaded },
                guide = { guideInFrame.get() },
                onFrameAspect = { aspect ->
                    mainExecutor.execute { if (!isDisposed && aspect != frameAspect) frameAspect = aspect }
                },
                onStateChanged = { state ->
                    mainExecutor.execute { if (!isDisposed) autoCaptureState = state }
                },
            ),
        )
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                if (isDisposed) return@addListener
                val preview =
                    Preview
                        .Builder()
                        .setResolutionSelector(fourByThreeSelector(PREVIEW_TARGET_SIZE))
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                providerFuture.get().apply {
                    unbindAll()
                    try {
                        bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalysis)
                    } catch (e: IllegalArgumentException) {
                        autoCaptureState = AutoCaptureState.Unavailable
                        bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    }
                }
            },
            mainExecutor,
        )
        onDispose {
            isDisposed = true
            imageAnalysis.clearAnalyzer()
            if (providerFuture.isDone) providerFuture.get().unbindAll()
        }
    }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView },
        )
        ScanGuideOverlay(
            state = autoCaptureState,
            frameAspect = frameAspect,
            bottomInset = SHUTTER_AREA_HEIGHT,
            onGuideChanged = guideInFrame::set,
            modifier = Modifier.fillMaxSize(),
        )
        ShutterButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SHUTTER_PADDING),
            enabled = !isTakingPhoto,
            onClick = takePhoto,
            progress =
                when (val state = autoCaptureState) {
                    is AutoCaptureState.Holding -> state.progress
                    is AutoCaptureState.Ready -> 1f
                    else -> 0f
                },
        )
    }
}

private fun fourByThreeSelector(targetSize: Size) =
    ResolutionSelector
        .Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(targetSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
        ).build()

private fun ImageProxy.toUprightBitmap(): Bitmap {
    val bitmap = toBitmap()
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
