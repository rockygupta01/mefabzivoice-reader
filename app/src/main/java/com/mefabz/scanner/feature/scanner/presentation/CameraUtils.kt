package com.mefabz.scanner.feature.scanner.presentation

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun Context.awaitCameraProvider(): ProcessCameraProvider {
    val appContext = applicationContext
    return suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }
}
