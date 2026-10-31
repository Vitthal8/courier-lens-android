package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LogisticsAmber
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun CameraScannerScreen(
    isBatchModeInitially: Boolean,
    onCaptured: (Bitmap, Boolean) -> Unit, // bitmap, isBatchMode
    onGalleryPick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isBatchMode by remember { mutableStateOf(isBatchModeInitially) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var batchCount by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraBindError by remember { mutableStateOf<String?>(null) }

    // Zoom & Tap-to-Focus state
    var linearZoom by remember { mutableFloatStateOf(0f) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var focusTapTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(focusTapTimestamp) {
        if (focusTapTimestamp > 0L) {
            delay(1200)
            focusTapOffset = null
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_scan"
    )

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Camera Permission Needed",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "CourierLens uses your camera to scan shipping labels, barcodes, and parcel information locally on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Layer 1: CameraX Live Preview View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // COMPATIBLE uses TextureView for reliable Compose layering and instant bitmap capture
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewViewRef = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        imageCapture = capture

                        val selector = when {
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                            else -> null
                        }

                        if (selector != null) {
                            cameraProvider.unbindAll()
                            try {
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                // Fallback: bind only Preview on virtual devices if dual-streaming times out
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview
                                )
                            }
                        } else {
                            cameraBindError = "No camera hardware detected on device."
                        }
                    } catch (exc: Exception) {
                        cameraBindError = "Camera initialization notice: ${exc.localizedMessage ?: "Virtual device camera"}"
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Layer 2: Difference-Path Overlay (Zero BlendMode.Clear -> No Black Patch!) + Tap-to-Focus
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        focusTapOffset = tapOffset
                        focusTapTimestamp = System.currentTimeMillis()
                        val pv = previewViewRef
                        val cam = camera
                        if (pv != null && cam != null) {
                            try {
                                val point = pv.meteringPointFactory.createPoint(tapOffset.x, tapOffset.y)
                                val action = FocusMeteringAction.Builder(
                                    point,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                )
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                cam.cameraControl.startFocusAndMetering(action)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val boxWidth = width * 0.84f
            val boxHeight = height * 0.44f
            val left = (width - boxWidth) / 2f
            val top = (height - boxHeight) / 2.5f

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Build a geometric difference path: Full Screen MINUS Center Cutout Window
                // This leaves the center scanning window 100% untouched & transparent!
                val fullScreenPath = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                }
                val cutoutWindowPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left, top, left + boxWidth, top + boxHeight),
                            cornerRadius = CornerRadius(24f, 24f)
                        )
                    )
                }
                val surroundScrimPath = Path().apply {
                    op(fullScreenPath, cutoutWindowPath, PathOperation.Difference)
                }

                // Draw dimmed surround only outside the scanning window
                drawPath(
                    path = surroundScrimPath,
                    color = Color.Black.copy(alpha = 0.50f)
                )

                // Subtle frame border around the clear window
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.25f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 2f)
                )

                // Corner Brackets (Electric Cyan)
                val bracketLen = 46f
                val strokeW = 7f
                val bracketColor = Color(0xFF38BDF8)

                // Top-Left
                drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), strokeW)
                drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), strokeW)

                // Top-Right
                drawLine(bracketColor, Offset(left + boxWidth, top), Offset(left + boxWidth - bracketLen, top), strokeW)
                drawLine(bracketColor, Offset(left + boxWidth, top), Offset(left + boxWidth, top + bracketLen), strokeW)

                // Bottom-Left
                drawLine(bracketColor, Offset(left, top + boxHeight), Offset(left + bracketLen, top + boxHeight), strokeW)
                drawLine(bracketColor, Offset(left, top + boxHeight), Offset(left, top + boxHeight - bracketLen), strokeW)

                // Bottom-Right
                drawLine(bracketColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth - bracketLen, top + boxHeight), strokeW)
                drawLine(bracketColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + bracketLen), strokeW)

                // Animated Laser Line inside the clear cutout
                val laserY = top + (boxHeight * laserProgress)
                drawLine(
                    color = Color(0xFFEF4444).copy(alpha = 0.85f),
                    start = Offset(left + 12f, laserY),
                    end = Offset(left + boxWidth - 12f, laserY),
                    strokeWidth = 3.5f
                )

                // Tap-to-Focus Reticle Indicator
                focusTapOffset?.let { tapPt ->
                    drawCircle(
                        color = LogisticsAmber,
                        radius = 34f,
                        center = tapPt,
                        style = Stroke(width = 4f)
                    )
                    drawCircle(
                        color = LogisticsAmber.copy(alpha = 0.35f),
                        radius = 8f,
                        center = tapPt
                    )
                }
            }

            // Guidance pill positioned cleanly above the cutout so the center view is unobstructed
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 104.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "ALIGN LABEL IN FRAME • TAP TO FOCUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )
                }

                if (cameraBindError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = cameraBindError ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = LogisticsAmber,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("camera_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Mode Pill Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isBatchMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isBatchMode = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Single Scan",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isBatchMode) LogisticsAmber else Color.Transparent)
                        .clickable { isBatchMode = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (batchCount > 0) "Batch ($batchCount)" else "Batch Mode",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBatchMode) Color.Black else Color.White
                    )
                }
            }

            // Flashlight Toggle
            IconButton(
                onClick = {
                    camera?.cameraControl?.enableTorch(!isFlashOn)
                    isFlashOn = !isFlashOn
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("toggle_torch_button")
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = if (isFlashOn) LogisticsAmber else Color.White
                )
            }
        }

        // Layer 3: Bottom Zoom & Shutter Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(top = 12.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zoom Slider & Quick Preset Pills (1x / 2x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )

                // 1x Preset Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (linearZoom < 0.2f) ElectricCyan else Color.White.copy(alpha = 0.15f))
                        .clickable {
                            linearZoom = 0f
                            camera?.cameraControl?.setLinearZoom(0f)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("zoom_1x_button")
                ) {
                    Text(
                        text = "1x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (linearZoom < 0.2f) Color.Black else Color.White
                    )
                }

                // 2x Preset Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (linearZoom >= 0.45f && linearZoom <= 0.6f) ElectricCyan else Color.White.copy(alpha = 0.15f))
                        .clickable {
                            linearZoom = 0.5f
                            camera?.cameraControl?.setLinearZoom(0.5f)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("zoom_2x_button")
                ) {
                    Text(
                        text = "2x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (linearZoom >= 0.45f && linearZoom <= 0.6f) Color.Black else Color.White
                    )
                }

                Slider(
                    value = linearZoom,
                    onValueChange = { newZoom ->
                        linearZoom = newZoom
                        camera?.cameraControl?.setLinearZoom(newZoom)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("camera_zoom_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = LogisticsAmber,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    )
                )

                Text(
                    text = String.format(java.util.Locale.US, "%.1fx", 1f + (linearZoom * 2f)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(32.dp)
                )
            }

            // Mode instruction
            Text(
                text = if (isBatchMode) "Continuous Mode: Tap shutter to log packets rapidly" else "Point at label and tap shutter",
                style = MaterialTheme.typography.bodySmall,
                color = if (isBatchMode) LogisticsAmber else Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Gallery Import button
                IconButton(
                    onClick = onGalleryPick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .testTag("camera_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick photo from gallery",
                        tint = Color.White
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .background(Color.Transparent)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isBatchMode) LogisticsAmber else Color.White)
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            triggerVibration(context)

                            // Instant TextureView frame grab first (100% reliable, zero Camera2 timeout)
                            val liveFrame = previewViewRef?.bitmap
                            if (liveFrame != null) {
                                isCapturing = false
                                if (isBatchMode) batchCount++
                                onCaptured(liveFrame, isBatchMode)
                                return@clickable
                            }

                            // Fallback to ImageCapture takePicture
                            val capture = imageCapture
                            if (capture != null) {
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(imageProxy)
                                            imageProxy.close()

                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                                if (bitmap != null) {
                                                    if (isBatchMode) batchCount++
                                                    onCaptured(bitmap, isBatchMode)
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                            }
                                        }
                                    }
                                )
                            } else {
                                isCapturing = false
                            }
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Batch Mode quick toggle icon
                IconButton(
                    onClick = { isBatchMode = !isBatchMode },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBatchMode) LogisticsAmber.copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.15f)
                        )
                        .testTag("camera_mode_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle batch mode",
                        tint = if (isBatchMode) LogisticsAmber else Color.White
                    )
                }
            }
        }
    }
}

private fun triggerVibration(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    } catch (_: Exception) {}
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val rotation = image.imageInfo.rotationDegrees
        if (rotation != 0 && bitmap != null) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        null
    }
}
