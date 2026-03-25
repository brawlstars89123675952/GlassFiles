package com.glassfiles.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream

object FileConverter {

    data class ConvertResult(val success: Boolean, val outputPath: String, val message: String)

    /**
     * Convert image to another format
     */
    fun convertImage(inputPath: String, outputFormat: ImageFormat, quality: Int = 85): ConvertResult {
        return try {
            val input = File(inputPath)
            if (!input.exists()) return ConvertResult(false, "", "File not found")

            val bitmap = BitmapFactory.decodeFile(inputPath)
                ?: return ConvertResult(false, "", "Failed to load image")

            val baseName = input.nameWithoutExtension
            val outputFile = File(input.parent, "${baseName}.${outputFormat.extension}")
            var finalFile = outputFile
            var counter = 1
            while (finalFile.exists()) {
                finalFile = File(input.parent, "${baseName}_${counter}.${outputFormat.extension}")
                counter++
            }

            FileOutputStream(finalFile).use { out ->
                bitmap.compress(outputFormat.compressFormat, quality, out)
            }
            bitmap.recycle()

            val savedSize = finalFile.length()
            val originalSize = input.length()
            val saved = if (savedSize < originalSize) " (−${formatBytes(originalSize - savedSize)})" else ""

            ConvertResult(true, finalFile.absolutePath, "Saved: ${finalFile.name}$saved")
        } catch (e: Exception) {
            ConvertResult(false, "", "Error: ${e.message}")
        }
    }

    /**
     * Resize image
     */
    fun resizeImage(inputPath: String, maxWidth: Int, maxHeight: Int, outputFormat: ImageFormat? = null, quality: Int = 85): ConvertResult {
        return try {
            val input = File(inputPath)
            val original = BitmapFactory.decodeFile(inputPath)
                ?: return ConvertResult(false, "", "Failed to load image")

            val ratio = minOf(maxWidth.toFloat() / original.width, maxHeight.toFloat() / original.height)
            val newWidth = (original.width * ratio).toInt()
            val newHeight = (original.height * ratio).toInt()
            val resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true)

            val format = outputFormat ?: ImageFormat.fromExtension(input.extension)
            val outputFile = File(input.parent, "${input.nameWithoutExtension}_${newWidth}x${newHeight}.${format.extension}")

            FileOutputStream(outputFile).use { out ->
                resized.compress(format.compressFormat, quality, out)
            }
            original.recycle(); resized.recycle()

            ConvertResult(true, outputFile.absolutePath, "Resized to ${newWidth}×${newHeight}")
        } catch (e: Exception) {
            ConvertResult(false, "", "Error: ${e.message}")
        }
    }

    /**
     * Extract video thumbnail as image
     */
    fun extractVideoThumbnail(videoPath: String): ConvertResult {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val bitmap = retriever.getFrameAtTime(1_000_000) // 1 second
            retriever.release()
            if (bitmap == null) return ConvertResult(false, "", "Failed to extract frame")

            val input = File(videoPath)
            val outputFile = File(input.parent, "${input.nameWithoutExtension}_thumb.jpg")
            FileOutputStream(outputFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()

            ConvertResult(true, outputFile.absolutePath, "Frame saved: ${outputFile.name}")
        } catch (e: Exception) {
            ConvertResult(false, "", "Error: ${e.message}")
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f МБ".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f КБ".format(bytes / 1024.0)
        else -> "$bytes Б"
    }
}

enum class ImageFormat(val extension: String, val compressFormat: Bitmap.CompressFormat, val label: String) {
    JPEG("jpg", Bitmap.CompressFormat.JPEG, "JPEG"),
    PNG("png", Bitmap.CompressFormat.PNG, "PNG"),
    WEBP("webp", Bitmap.CompressFormat.WEBP_LOSSY, "WebP");

    companion object {
        fun fromExtension(ext: String): ImageFormat = when (ext.lowercase()) {
            "png" -> PNG
            "webp" -> WEBP
            else -> JPEG
        }
    }
}
