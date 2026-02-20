package com.mefabz.scanner.feature.scanner.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mefabz.scanner.ui.theme.Ink
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "MEFABZ Invoice detected.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Page ${invoice.pageNumber}",
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall.copy(
                        shadow = Shadow(color = NeonCyan.copy(alpha = 0.9f), blurRadius = 28f)
                    )
                )

                val colors = extractColorsFromProducts(invoice.products)
                if (colors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Color: ${colors.joinToString(", ")}",
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AsyncImage(
                    model = invoice.imageUri,
                    contentDescription = "Captured invoice",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }

        Text(
            text = "Products listed are:",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.85f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 10.dp)
            ) {
                itemsIndexed(invoice.products) { index, product ->
                    val highlighted = activeIndex == index
                    val animatedColor by animateColorAsState(
                        targetValue = if (highlighted) NeonCyan else Ink,
                        label = "product_color"
                    )
                    val animatedPadding by animateDpAsState(
                        targetValue = if (highlighted) 18.dp else 14.dp,
                        label = "product_padding"
                    )

                    Text(
                        text = buildColorHighlightedString(product, animatedColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = animatedPadding),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        VoiceWaveform(
            isSpeaking = isSpeaking,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
        )

        Button(
            onClick = {
                if (isSpeaking) viewModel.stopNarration() else viewModel.startNarration()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isSpeaking) "Stop Voice" else "Play Voice",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(
            onClick = onScanAnother,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Scan another invoice")
        }
    }
}
