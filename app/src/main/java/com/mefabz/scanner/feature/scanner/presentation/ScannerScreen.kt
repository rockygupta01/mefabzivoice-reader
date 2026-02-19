package com.mefabz.scanner.feature.scanner.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mefabz.scanner.feature.scanner.presentation.components.ScannerOverlay
import com.mefabz.scanner.ui.theme.NeonCyan
import com.mefabz.scanner.ui.theme.Slate800
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onOpenPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCachingPdf by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { contentUri ->
            isCachingPdf = true
            scope.launch(Dispatchers.IO) {
                try {
                    // Copy to cache immediately to avoid permission issues later
                    val inputStream = context.contentResolver.openInputStream(contentUri)
                    if (inputStream != null) {
                        val fileName = "cached_pdf_${System.currentTimeMillis()}.pdf"
                        val file = File(context.cacheDir, fileName)
                        val outputStream = java.io.FileOutputStream(file)
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        // Navigate with the path to the local file
                        withContext(Dispatchers.Main) {
                            onOpenPdf(file.toUri().toString())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isCachingPdf = false
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var previewSurfaceRequest by remember { mutableStateOf<androidx.camera.core.SurfaceRequest?>(null) }

    DisposableEffect(hasCameraPermission, lifecycleOwner) {
        if (!hasCameraPermission) {
            onDispose { }
        } else {
            val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val previewUseCase = Preview.Builder().build().apply {
                    setSurfaceProvider { request ->
                        previewSurfaceRequest = request
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    imageCapture
                )
            }

            cameraProviderFuture.addListener(listener, mainExecutor)
            onDispose {
                runCatching {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    val isBusy = uiState.scanState is ScanState.Capturing || uiState.scanState is ScanState.Processing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "MEFABZ Scanner",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Invoice Voice Reader",
            style = MaterialTheme.typography.titleMedium,
            color = NeonCyan.copy(alpha = 0.9f)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (hasCameraPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxSize(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        previewSurfaceRequest?.let { request ->
                            CameraXViewfinder(
                                surfaceRequest = request,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        ScannerOverlay(modifier = Modifier.fillMaxSize())

                        // "Open PDF" Button (Top Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.End
                        ) {

                            androidx.compose.material3.FilledTonalButton(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                            ) {
                                Text("Open PDF")
                            }
                        }


                        // PDF Caching Loader
                        if (isCachingPdf) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeonCyan)
                            }
                        }

                        val loaderAlpha by animateFloatAsState(
                            targetValue = if (isBusy) 1f else 0f,
                            label = "processing_alpha"
                        )

                        if (loaderAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(loaderAlpha)
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = NeonCyan)
                                    Text(
                                        text = if (uiState.scanState is ScanState.Capturing) {
                                            "Capturing invoice..."
                                        } else {
                                            "Analyzing invoice..."
                                        },
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Camera permission is required to scan invoices",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = uiState.scanState is ScanState.Error) {
            val reason = (uiState.scanState as? ScanState.Error)?.reason
            if (reason != null) {
                ErrorCard(message = viewModel.errorLabel(reason))
            }
        }

        Button(
            onClick = {
                if (!hasCameraPermission || isBusy) return@Button
                val outputFile = File(context.cacheDir, "invoice_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                viewModel.onCaptureStarted()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val imageBytes = outputFile.readBytes()
                            val imageUri = outputFile.toUri().toString()
                            viewModel.processCapturedInvoice(imageBytes, imageUri)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            viewModel.onCaptureFailed()
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            enabled = hasCameraPermission && !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = "Scan MEFABZ Invoice",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(2.dp))
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
