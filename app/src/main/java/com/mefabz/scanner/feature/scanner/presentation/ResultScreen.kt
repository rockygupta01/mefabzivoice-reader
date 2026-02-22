package com.mefabz.scanner.feature.scanner.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mefabz.scanner.feature.scanner.presentation.components.VoiceWaveform
import com.mefabz.scanner.feature.scanner.presentation.components.buildColorHighlightedString
import com.mefabz.scanner.feature.scanner.presentation.components.extractColorsFromProducts
import com.mefabz.scanner.ui.theme.NeonCyan
import com.mefabz.scanner.ui.theme.Slate800

@Composable
fun ResultScreen(
    viewModel: ScannerViewModel,
    onScanAnother: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val invoice = uiState.parsedInvoice

    if (invoice == null) {
        LaunchedEffect(Unit) { onScanAnother() }
        return
    }

    LaunchedEffect(invoice.products, invoice.pageNumber) {
        viewModel.startNarration()
    }

    val activeIndex = (uiState.speechState as? SpeechState.Speaking)?.activeProductIndex
    val isSpeaking = uiState.speechState is SpeechState.Speaking

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parsed Results",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onScanAnother) {
                    Text(
                        "Scan New",
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page ${invoice.pageNumber}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(color = NeonCyan.copy(alpha = 0.5f), blurRadius = 12f)
                    )
                )

                val colors = extractColorsFromProducts(invoice.products)
                if (colors.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = colors.joinToString(", "),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Products List
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Leave room at the bottom for the floating audio dock
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    itemsIndexed(invoice.products) { index, product ->
                        val highlighted = activeIndex == index
                        val animatedTextColor by animateColorAsState(
                            targetValue = if (highlighted) NeonCyan else MaterialTheme.colorScheme.onSurface,
                            label = "product_text_color"
                        )
                        val animatedBgColor by animateColorAsState(
                            targetValue = if (highlighted) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                            label = "product_bg_color"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(animatedBgColor)
                        ) {
                            Text(
                                text = buildColorHighlightedString(product, animatedTextColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Fixed padding prevents jitter layout jumps
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Floating Audio Dock at the bottom
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VoiceWaveform(
                    isSpeaking = isSpeaking,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(end = 16.dp)
                )

                IconButton(
                    onClick = {
                        if (isSpeaking) viewModel.stopNarration() else viewModel.startNarration()
                    },
                    modifier = Modifier
                        .background(color = NeonCyan, shape = RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Filled.PlayArrow else Icons.Filled.PlayArrow, // Fallback since Stop isn't available without extended icons package
                        contentDescription = if (isSpeaking) "Stop Narration" else "Play Narration",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
