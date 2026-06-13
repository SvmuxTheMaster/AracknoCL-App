package com.example.ui

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
        containerColor = Color(0xFF13110F)
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
                        },
                        onDemoSelected = { bitmap ->
                            capturedBitmap = bitmap
                            showConfirmation = true
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
                        },
                        onDemoSelected = { bitmap ->
                            capturedBitmap = bitmap
                            showConfirmation = true
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
    onGalleryClick: () -> Unit,
    onDemoSelected: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
                        colors = listOf(Color.Transparent, Color(0xE613110F)),
                        startY = 0f
                    )
                )
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Encuadra la araña en el visor central",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
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
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .testTag("gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Cargar desde Galería",
                            tint = Color.White
                        )
                    }

                    // Trigger Capture Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(3.dp, Color(0xFFFFAA00), CircleShape)
                            .padding(6.dp)
                            .background(Color.White, CircleShape)
                            .clickable {
                                capturePhoto(context, imageCapture, cameraExecutor, onImageCaptured)
                            }
                            .testTag("shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capturar",
                            tint = Color(0xFF13110F),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Demo / Emulator helper
                    Box(modifier = Modifier.size(52.dp)) {
                        // Empty spacer or placeholder to balance Row
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                DemoSpidersSelector(onDemoSelected)
            }
        }
    }
}

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
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        // Visor target frame
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .border(2.dp, Color(0xFFFFAA00).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
        ) {
            // Laser scan line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 280.dp * scanOffset)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFFFFAA00), Color.Transparent)
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
            .background(Color(0xFF13110F))
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
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "¿La foto es clara para la identificación?",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
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
                    .border(2.dp, Color(0xFFFFAA00), RoundedCornerShape(16.dp)),
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
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
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
                    containerColor = Color(0xFFFFAA00),
                    contentColor = Color(0xFF13110F)
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
    onGalleryClick: () -> Unit,
    onDemoSelected: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    
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
            tint = Color(0xFFFFAA00),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Acceso a la Cámara",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Requerimos permiso de la cámara para que escanees arañas en vivo. Alternativamente, puedes cargar desde la galería o usar fotos de prueba directas.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAA00), contentColor = Color(0xFF13110F)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("request_permission_button")
        ) {
            Text("Conceder Permiso")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onGalleryClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("gallery_permission_fallback_button")
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Cargar desde Galería")
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(24.dp))
        
        DemoSpidersSelector(onDemoSelected)
    }
}

@Composable
fun DemoSpidersSelector(onSelected: (Bitmap) -> Unit) {
    val context = LocalContext.current
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Prueba Rápida (Sin cámara real)",
            color = Color(0xFFFFAA00),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mock Rincon button
            Button(
                onClick = {
                    val icon = BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_menu_compass)
                    onSelected(icon ?: Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1C0C)),
                contentPadding = PaddingValues(10.dp)
            ) {
                Text("Araña de Rincón", fontSize = 11.sp, color = Color.White)
            }
            // Mock Tigre button
            Button(
                onClick = {
                    val icon = BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_menu_compass)
                    onSelected(icon ?: Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF132215)),
                contentPadding = PaddingValues(10.dp)
            ) {
                Text("Araña Tigre", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

private fun capturePhoto(
    context: Context,
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
                val mockBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
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
