package com.mefabz.scanner.feature.scanner.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mefabz.scanner.ui.theme.NeonCyan
import kotlin.math.absoluteValue
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "voice_wave")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(16) { index ->
            val dynamic = if (isSpeaking) {
                sin(phase.value + index * 0.45f).absoluteValue
            } else {
                0.18f
            }

            Box(
                modifier = Modifier
                    .height((10 + dynamic * 34).dp)
                    .width(5.dp)
                    .alpha(0.35f + dynamic * 0.65f)
                    .background(NeonCyan, RoundedCornerShape(16.dp))
            )

            if (index != 15) {
                Box(modifier = Modifier.width(4.dp))
            }
        }
    }
}
