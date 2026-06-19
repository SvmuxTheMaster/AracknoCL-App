package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.core.graphics.createBitmap

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraView(
    viewModel: AraknoViewModel,
    onResultSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(context, it)
            if (bitmap != null) {
                capturedBitmap = bitmap
                showConfirmation = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showConfirmation && capturedBitmap != null) {
                // CONFIRMATION SCREEN (HU02/HU03 - "Repetir" o "Usar foto")
                PhotoConfirmationScreen(
                    bitmap = capturedBitmap!!,
                    onRepeat = {
                        capturedBitmap = null
                        showConfirmation = false
                    },
                    onUse = {
                        viewModel.analyzeCapturedImage(capturedBitmap!!)
                        onResultSelected()
                        showConfirmation = false
                    }
                )
            } else {
                // CAMERA/SCANNER PORT VIEW
                if (cameraPermissionState.status.isGranted) {
                    CameraScannerLayout(
                        onImageCaptured = { bitmap ->
                            capturedBitmap = bitmap
                            showConfirmation = true
                        },
                        onGalleryClick = {
                            imageLauncher.launch("image/*")
                        }
                    )
                } else {
                    // NO PERMISSION STATE OR EMULATOR BACKUP
                    NoPermissionScreen(
                        onRequestPermission = {
                            cameraPermissionState.launchPermissionRequest()
                        },
                        onGalleryClick = {
                            imageLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CameraScannerLayout(
    onImageCaptured: (Bitmap) -> Unit,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraScannerLayout", "Binding failure", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                Log.e("CameraScannerLayout", "Termination cleanup failure", e)
            }
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Viewport
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Web/Spider styling Overlay
        SpiderScanningOverlay()

        // Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
                        startY = 0f
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Encuadra la araña en el visor central",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Button
                    IconButton(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), CircleShape)
                            .testTag("gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Cargar desde Galería",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Trigger Capture Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(6.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                            .clickable {
                                capturePhoto(imageCapture, cameraExecutor, onImageCaptured)
                            }
                            .testTag("shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capturar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Demo / Emulator helper
                    Box(modifier = Modifier.size(52.dp)) {
                        // Empty spacer or placeholder to balance Row
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SpiderScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_bar"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
    ) {
        // Visor target frame
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1f)
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
        ) {
            val maxHeight = maxHeight
            // Laser scan line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = maxHeight * scanOffset)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.primary, Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
fun PhotoConfirmationScreen(
    bitmap: Bitmap,
    onRepeat: () -> Unit,
    onUse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = "Confirmar Imagen",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "¿La foto es clara para la identificación?",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto capturada",
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onRepeat,
                modifier = Modifier
                    .weight(1f)
                    .testTag("repeat_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Repetir")
            }

            Button(
                onClick = onUse,
                modifier = Modifier
                    .weight(1f)
                    .testTag("use_photo_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Usar foto")
            }
        }
    }
}

@Composable
fun NoPermissionScreen(
    onRequestPermission: () -> Unit,
    onGalleryClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Acceso a la Cámara",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Requerimos permiso de la cámara para que escanees arañas en vivo. Alternativamente, puedes cargar desde la galería o usar fotos de prueba directas.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("request_permission_button")
        ) {
            Text("Conceder Permiso")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onGalleryClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("gallery_permission_fallback_button")
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Cargar desde Galería")
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(24.dp))
        

    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onImageCaptured: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                image.close()
                if (bitmap != null) {
                    onImageCaptured(bitmap)
                }
            }

            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                Log.e("CameraScannerLayout", "Capture error", exception)
                // Fallback inside thread
                val mockBitmap = createBitmap(300, 300)
                onImageCaptured(mockBitmap)
            }
        }
    )
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}
