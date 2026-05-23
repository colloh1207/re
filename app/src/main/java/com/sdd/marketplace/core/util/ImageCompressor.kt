package com.sdd.marketplace.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageCompressor {

    private const val MAX_WIDTH = 1920
    private const val MAX_HEIGHT = 1920
    private const val DEFAULT_QUALITY = 80
    private const val THUMBNAIL_SIZE = 400
    private const val THUMBNAIL_QUALITY = 70

    /**
     * Compress an image from a Uri to a ByteArray.
     * Resizes to max 1920x1920 and compresses to JPEG at 80% quality.
     * Automatically corrects EXIF orientation.
     */
    suspend fun compress(
        context: Context,
        uri: Uri,
        maxWidth: Int = MAX_WIDTH,
        maxHeight: Int = MAX_HEIGHT,
        quality: Int = DEFAULT_QUALITY
    ): ByteArray = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open URI: $uri")

        // Decode bounds only first to calculate sample size
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        inputStream.use { BitmapFactory.decodeStream(it, null, options) }

        val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxWidth, maxHeight)

        // Decode with sample size
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap: Bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalStateException("Failed to decode bitmap")

        // Correct orientation from EXIF
        val rotated = correctOrientation(context, uri, bitmap)

        // Scale down if still too large
        val scaled = scaleBitmap(rotated, maxWidth, maxHeight)
        if (rotated != scaled && rotated != bitmap) rotated.recycle()

        // Compress to JPEG bytes
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        if (scaled != bitmap) scaled.recycle()
        bitmap.recycle()

        output.toByteArray()
    }

    /**
     * Compress to a small thumbnail (e.g. for chat image previews).
     */
    suspend fun compressThumbnail(context: Context, uri: Uri): ByteArray =
        compress(context, uri, THUMBNAIL_SIZE, THUMBNAIL_SIZE, THUMBNAIL_QUALITY)

    /**
     * Compress for KYC documents — higher quality to preserve document readability.
     */
    suspend fun compressDocument(context: Context, uri: Uri): ByteArray =
        compress(context, uri, maxWidth = 2048, maxHeight = 2048, quality = 90)

    private fun calculateSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = 1
        if (originalHeight > targetHeight || originalWidth > targetWidth) {
            val heightRatio = Math.round(originalHeight.toFloat() / targetHeight.toFloat())
            val widthRatio = Math.round(originalWidth.toFloat() / targetWidth.toFloat())
            sampleSize = minOf(heightRatio, widthRatio)
        }
        return if (sampleSize < 1) 1 else sampleSize
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = stream.use { ExifInterface(it) }
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Estimate compressed file size in KB before actually compressing.
     * Useful for showing the user a size estimate.
     */
    fun estimateCompressedSizeKb(
        originalWidth: Int,
        originalHeight: Int,
        quality: Int = DEFAULT_QUALITY
    ): Int {
        val pixels = originalWidth.coerceAtMost(MAX_WIDTH) * originalHeight.coerceAtMost(MAX_HEIGHT)
        return (pixels * 3 * (quality / 100f) / 1024 / 8).toInt().coerceAtLeast(10)
    }
}
