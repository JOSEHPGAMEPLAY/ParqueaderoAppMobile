package com.example.ui.components

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.toRotatedBitmap

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit,
    onError: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    LaunchedEffect(cameraProviderFuture, lifecycleOwner) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
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
            onError(e)
        }
    }

    DisposableEffect(lifecycleOwner, cameraProviderFuture) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                // Ignore any exceptions during unbind on disposal
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Close/Turn Off Camera Button
        FilledTonalButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .height(36.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Desactivar cámara",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apagar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Overlay focusing bracket
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bracketWidth = w * 0.70f
            val bracketHeight = h * 0.35f
            val left = (w - bracketWidth) / 2f
            val top = (h - bracketHeight) / 2f
            val right = left + bracketWidth
            val bottom = top + bracketHeight

            val strokeWidthPx = 4.dp.toPx()
            val len = 24.dp.toPx()
            val primaryColor = Color(0xFFD0BCFF)

            // Top Left
            drawLine(color = primaryColor, start = Offset(left, top), end = Offset(left + len, top), strokeWidth = strokeWidthPx)
            drawLine(color = primaryColor, start = Offset(left, top), end = Offset(left, top + len), strokeWidth = strokeWidthPx)
            
            // Top Right
            drawLine(color = primaryColor, start = Offset(right, top), end = Offset(right - len, top), strokeWidth = strokeWidthPx)
            drawLine(color = primaryColor, start = Offset(right, top), end = Offset(right, top + len), strokeWidth = strokeWidthPx)
            
            // Bottom Left
            drawLine(color = primaryColor, start = Offset(left, bottom), end = Offset(left + len, bottom), strokeWidth = strokeWidthPx)
            drawLine(color = primaryColor, start = Offset(left, bottom), end = Offset(left, bottom - len), strokeWidth = strokeWidthPx)
            
            // Bottom Right
            drawLine(color = primaryColor, start = Offset(right, bottom), end = Offset(right - len, bottom), strokeWidth = strokeWidthPx)
            drawLine(color = primaryColor, start = Offset(right, bottom), end = Offset(right, bottom - len), strokeWidth = strokeWidthPx)
        }

        // Shutter Button
        FloatingActionButton(
            onClick = {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = image.toRotatedBitmap()
                            image.close()
                            if (bitmap != null) {
                                onBitmapCaptured(bitmap)
                            } else {
                                onError(NullPointerException("Se produjo un error al procesar los pixeles de la foto."))
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            onError(exception)
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .testTag("shutter_button"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Capturar foto de placa",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
