package com.sdd.marketplace.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.sqrt

object ImageCompressor {

    fun compress(context: Context, uri: Uri, maxSizeKb: Int = 500): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Resize if too large
            val maxDimension = 1080
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val scale = min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            val tempFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
            var quality = 90

            // Reduce quality until file size is acceptable
            do {
                val fos = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                fos.flush()
                fos.close()
                quality -= 10
            } while (tempFile.length() > maxSizeKb * 1024 && quality > 20)

            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
