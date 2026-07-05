package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// Converts ImageProxy to rotated Bitmap to account for camera sensor rotation
fun ImageProxy.toRotatedBitmap(): Bitmap? {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    val rotationDegrees = imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

// Saves a bitmap into internal private storage and returns the absolute file path, organized by daily subfolders
fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
    return try {
        val dateFolder = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val baseDir = File(context.filesDir, "scanned_plates")
        val directory = File(baseDir, dateFolder)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val fileName = "plate_${UUID.randomUUID()}.jpg"
        val file = File(directory, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
