package com.sdd.marketplace.feature.kyc.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.sdd.marketplace.core.ui.theme.SddPink
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

enum class LivenessStep(val instruction: String, val emoji: String) {
    WAITING_FACE("Position your face in the oval", "😐"),
    BLINK("Blink your eyes slowly", "👁️"),
    SMILE("Smile naturally", "😊"),
    TURN_LEFT("Turn your head slowly to the left", "⬅️"),
    TURN_RIGHT("Turn your head slowly to the right", "➡️"),
    COMPLETE("Verification complete!", "✅")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LivenessCameraScreen(
    onComplete: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    when {
        cameraPermission.status.isGranted -> {
            LivenessCameraContent(onComplete = onComplete, onCancel = onCancel)
        }
        cameraPermission.status.shouldShowRationale -> {
            CameraPermissionRationale(
                onRequest = { cameraPermission.launchPermissionRequest() },
                onCancel = onCancel
            )
        }
        else -> {
            LaunchedEffect(Unit) { cameraPermission.launchPermissionRequest() }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SddPink)
            }
        }
    }
}

@Composable
private fun LivenessCameraContent(
    onComplete: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by remember { mutableStateOf(LivenessStep.WAITING_FACE) }
    var stepProgress by remember { mutableStateOf(0f) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val imageAnalysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.25f)
            .build()
        FaceDetection.getClient(options)
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Per-session counters — scoped to this composable instance, not file-level
    var blinkFrames     by remember { mutableIntStateOf(0) }
    var smileFrames     by remember { mutableIntStateOf(0) }
    var turnLeftFrames  by remember { mutableIntStateOf(0) }
    var turnRightFrames by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            faceDetector.close()
            imageAnalysisExecutor.shutdown()
        }
    }

    LaunchedEffect(previewView) {
        val pv = previewView ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(imageAnalysisExecutor) { proxy ->
                if (currentStep == LivenessStep.COMPLETE) {
                    proxy.close()
                    return@setAnalyzer
                }
                val mediaImage = proxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    faceDetector.process(image)
                        .addOnSuccessListener { faces ->
                            if (faces.isNotEmpty()) {
                                val face = faces.first()
                                handleFaceDetection(
                                    face, currentStep,
                                    getBlinkFrames     = { blinkFrames },
                                    setBlinkFrames     = { blinkFrames = it },
                                    getSmileFrames     = { smileFrames },
                                    setSmileFrames     = { smileFrames = it },
                                    getTurnLeftFrames  = { turnLeftFrames },
                                    setTurnLeftFrames  = { turnLeftFrames = it },
                                    getTurnRightFrames = { turnRightFrames },
                                    setTurnRightFrames = { turnRightFrames = it }
                                ) { newStep, progress ->
                                    stepProgress = progress
                                    if (progress >= 1f) currentStep = newStep
                                }
                            }
                            proxy.close()
                        }
                        .addOnFailureListener {
                            proxy.close()
                        }
                } else {
                    proxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview, capture, analysis
                )
            } catch (e: Exception) {
                errorMsg = "Camera unavailable: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(currentStep) {
        if (currentStep == LivenessStep.COMPLETE) {
            val capture = imageCapture ?: return@LaunchedEffect
            captureAndSave(context, capture) { uri ->
                capturedUri = uri
                if (uri != null) onComplete(uri)
                else errorMsg = "Could not capture image. Please try again."
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        FaceOvalOverlay(
            modifier = Modifier.fillMaxSize(),
            step = currentStep,
            progress = stepProgress
        )

        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, "Cancel", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                LivenessStepIndicator(currentStep)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                errorMsg?.let {
                    Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(12.dp)) {
                        Text(it, color = Color.White, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        errorMsg = null
                        currentStep = LivenessStep.WAITING_FACE
                        stepProgress = 0f
                    }) { Text("Try Again", color = Color.White) }
                } ?: run {
                    InstructionBubble(step = currentStep, progress = stepProgress)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private const val REQUIRED_FRAMES = 8

private fun handleFaceDetection(
    face: Face,
    step: LivenessStep,
    getBlinkFrames: () -> Int,     setBlinkFrames: (Int) -> Unit,
    getSmileFrames: () -> Int,     setSmileFrames: (Int) -> Unit,
    getTurnLeftFrames: () -> Int,  setTurnLeftFrames: (Int) -> Unit,
    getTurnRightFrames: () -> Int, setTurnRightFrames: (Int) -> Unit,
    onProgress: (LivenessStep, Float) -> Unit
) {
    when (step) {
        LivenessStep.WAITING_FACE -> {
            if (face.boundingBox.width() > 100) onProgress(LivenessStep.BLINK, 0f)
        }
        LivenessStep.BLINK -> {
            val leftEye = face.leftEyeOpenProbability ?: 1f
            val rightEye = face.rightEyeOpenProbability ?: 1f
            if (leftEye < 0.2f && rightEye < 0.2f) {
                val n = getBlinkFrames() + 1
                setBlinkFrames(n)
                if (n >= REQUIRED_FRAMES) { setBlinkFrames(0); onProgress(LivenessStep.SMILE, 1f) }
                else onProgress(LivenessStep.BLINK, n.toFloat() / REQUIRED_FRAMES)
            } else {
                setBlinkFrames(maxOf(0, getBlinkFrames() - 1))
            }
        }
        LivenessStep.SMILE -> {
            val smile = face.smilingProbability ?: 0f
            if (smile > 0.7f) {
                val n = getSmileFrames() + 1
                setSmileFrames(n)
                if (n >= REQUIRED_FRAMES) { setSmileFrames(0); onProgress(LivenessStep.TURN_LEFT, 1f) }
                else onProgress(LivenessStep.SMILE, n.toFloat() / REQUIRED_FRAMES)
            } else {
                setSmileFrames(maxOf(0, getSmileFrames() - 1))
            }
        }
        LivenessStep.TURN_LEFT -> {
            val yaw = face.headEulerAngleY
            if (yaw < -20f) {
                val n = getTurnLeftFrames() + 1
                setTurnLeftFrames(n)
                if (n >= REQUIRED_FRAMES) { setTurnLeftFrames(0); onProgress(LivenessStep.TURN_RIGHT, 1f) }
                else onProgress(LivenessStep.TURN_LEFT, n.toFloat() / REQUIRED_FRAMES)
            } else {
                setTurnLeftFrames(maxOf(0, getTurnLeftFrames() - 1))
            }
        }
        LivenessStep.TURN_RIGHT -> {
            val yaw = face.headEulerAngleY
            if (yaw > 20f) {
                val n = getTurnRightFrames() + 1
                setTurnRightFrames(n)
                if (n >= REQUIRED_FRAMES) { setTurnRightFrames(0); onProgress(LivenessStep.COMPLETE, 1f) }
                else onProgress(LivenessStep.TURN_RIGHT, n.toFloat() / REQUIRED_FRAMES)
            } else {
                setTurnRightFrames(maxOf(0, getTurnRightFrames() - 1))
            }
        }
        LivenessStep.COMPLETE -> {}
    }
}

private fun captureAndSave(
    context: Context,
    imageCapture: ImageCapture,
    onResult: (Uri?) -> Unit
) {
    val file = File(context.cacheDir, "liveness_selfie_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult(Uri.fromFile(file))
            }
            override fun onError(exception: ImageCaptureException) {
                onResult(null)
            }
        }
    )
}

@Composable
private fun FaceOvalOverlay(modifier: Modifier, step: LivenessStep, progress: Float) {
    val borderColor by animateColorAsState(
        targetValue = when {
            step == LivenessStep.COMPLETE -> Color(0xFF4CAF50)
            progress > 0.5f -> Color(0xFFFFC107)
            else -> Color.White.copy(alpha = 0.7f)
        },
        animationSpec = tween(300), label = "borderColor"
    )
    Box(modifier) {
        Box(
            Modifier
                .size(240.dp, 300.dp)
                .align(Alignment.Center)
                .clip(androidx.compose.foundation.shape.GenericShape { size, _ ->
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                })
                .border(3.dp, borderColor, androidx.compose.foundation.shape.GenericShape { size, _ ->
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                })
        )
        if (step == LivenessStep.COMPLETE) {
            Icon(
                Icons.Filled.CheckCircle, "Complete",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp).align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun InstructionBubble(step: LivenessStep, progress: Float) {
    AnimatedContent(targetState = step, label = "instruction") { s ->
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(s.emoji, fontSize = 28.sp)
                Text(
                    s.instruction,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
                if (progress > 0f && progress < 1f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(CircleShape),
                        color = SddPink,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LivenessStepIndicator(currentStep: LivenessStep) {
    val steps = listOf(
        LivenessStep.BLINK,
        LivenessStep.SMILE,
        LivenessStep.TURN_LEFT,
        LivenessStep.TURN_RIGHT
    )
    val currentIndex = steps.indexOf(currentStep).coerceAtLeast(0)
    Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                val isDone = currentIndex > index || currentStep == LivenessStep.COMPLETE
                val isCurrent = currentIndex == index
                Box(
                    Modifier.size(if (isCurrent) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> Color(0xFF4CAF50)
                                isCurrent -> SddPink
                                else -> Color.White.copy(alpha = 0.4f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionRationale(onRequest: () -> Unit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, "Camera", tint = Color.White, modifier = Modifier.size(64.dp))
            Text("Camera Permission Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Text("We need camera access to verify your identity. This photo is only used for KYC verification.", color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
            Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = SddPink)) { Text("Grant Permission") }
            TextButton(onClick = onCancel) { Text("Cancel", color = Color.White.copy(0.7f)) }
        }
    }
}
