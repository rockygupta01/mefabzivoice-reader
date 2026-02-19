package com.mefabz.scanner.feature.scanner.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mefabz.scanner.ui.theme.NeonCyan

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanner_laser")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanner_progress"
    )
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x1A00E7FF),
                        Color.Transparent
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * progress.value
            val glowRadius = with(density) { 48.dp.toPx() }
            val strokeWidth = with(density) { 3.dp.toPx() }

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        NeonCyan.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    startY = (y - glowRadius).coerceAtLeast(0f),
                    endY = (y + glowRadius).coerceAtMost(size.height)
                ),
                topLeft = Offset(0f, (y - glowRadius).coerceAtLeast(0f)),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width,
                    height = glowRadius * 2
                )
            )

            drawLine(
                color = NeonCyan,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
