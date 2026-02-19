package com.mefabz.scanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MefabzDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonIndigo,
    background = Slate950,
    surface = Slate900,
    onPrimary = Slate950,
    onSecondary = Ink,
    onBackground = Ink,
    onSurface = Ink,
    error = ErrorRed,
    onError = Slate950
)

@Composable
fun MefabzScannerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MefabzDarkColorScheme,
        content = content
    )
}
