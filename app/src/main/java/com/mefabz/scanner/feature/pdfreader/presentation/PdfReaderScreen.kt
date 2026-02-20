package com.mefabz.scanner.feature.pdfreader.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefabz.scanner.feature.scanner.presentation.components.VoiceWaveform
import com.mefabz.scanner.feature.scanner.presentation.components.buildColorHighlightedString
import com.mefabz.scanner.feature.scanner.presentation.components.extractColorsFromProducts
import com.mefabz.scanner.ui.theme.Ink
import com.mefabz.scanner.ui.theme.NeonCyan
import com.mefabz.scanner.ui.theme.Slate800

@Composable
fun ErrorOverlay(message: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PdfReaderScreen(
    viewModel: PdfReaderViewModel = hiltViewModel(),
    uri: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uri) {
        viewModel.loadPdf(uri)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onNavigateBack) {
                    Text("Back")
                }
                Text(
                    text = "PDF Preview",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        bottomBar = {
            if (uiState.pageCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    // Voice Waveform (Active only when reading)
                    if (uiState.isReading) {
                        VoiceWaveform(
                            isSpeaking = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(bottom = 8.dp)
                        )
                    }

                    // Read / Stop Button
                    Button(
                        onClick = { viewModel.toggleRead() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isReading) MaterialTheme.colorScheme.error else NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                         enabled = !uiState.isLoading && uiState.pageBitmap != null
                    ) {
                        Text(
                            text = if (uiState.isReading) "Stop Reading" else "Read Current Page",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigation Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.previousPage() },
                            enabled = uiState.currentPage > 0
                        ) {
                            Text("Previous")
                        }

                        Text(
                            text = "Page ${uiState.currentPage + 1} of ${uiState.pageCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = { viewModel.nextPage() },
                            enabled = uiState.currentPage < uiState.pageCount - 1
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Slate800),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.pageBitmap != null) {
                    PdfPagePreview(bitmap = uiState.pageBitmap!!)
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(color = NeonCyan)
                } else if (uiState.error != null) {
                    // If no bitmap, show full screen error
                    if (uiState.pageBitmap == null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        // If bitmap exists (e.g. non-MEFABZ page), show error as a small snackbar-like overlay
                        // or just ignore it until user interacts. 
                        // For now, let's show a small error indicator at the bottom
                        ErrorOverlay(
                            message = uiState.error!!,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        )
                    }
                }
                
                if (uiState.detectedProducts.isNotEmpty()) {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Slate800.copy(alpha = 0.90f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Page: ${uiState.detectedInvoicePage}",
                                color = NeonCyan,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val colors = extractColorsFromProducts(uiState.detectedProducts)
                            if (colors.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Color: ${colors.joinToString(", ")}",
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(uiState.detectedProducts) { index, product ->
                                    val highlighted = uiState.activeProductIndex == index
                                    val animatedColor by androidx.compose.animation.animateColorAsState(
                                        targetValue = if (highlighted) NeonCyan else Ink,
                                        label = "color"
                                    )
                                    Text(
                                        text = buildColorHighlightedString(product, animatedColor),
                                        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPagePreview(bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "PDF Page",
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentScale = ContentScale.Fit
    )
}
