package com.marveloushost.betterrank.fileManager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface

class ImageHelper(private val context: Context) {

/*    fun compressImageFromPath(
        inputPath: String?,
        quality: Int = 70,
        maxWidth: Int = 800,
        maxHeight: Int = 800
    ): String? {

        if (inputPath.isNullOrBlank())
            return null

        return try {
            val inputFile = File(inputPath)
            if (!inputFile.exists())
                return null

            val exif = ExifInterface(inputFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val originalBitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
            val rotatedBitmap = rotateBitmapIfRequired(originalBitmap, orientation)
            val resizedBitmap = resizeBitmapMaintainingAspectRatio(rotatedBitmap, maxWidth, maxHeight)

            val compressedFile = File(
                context.cacheDir,
                "compressed_${System.currentTimeMillis()}.jpg"
            )

            FileOutputStream(compressedFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            compressedFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }*/


    fun compressImageFromPath(
        inputPath: String?,
        quality: Int = 70,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        isFrontCamera: Boolean = false
    ): String? {

        if (inputPath.isNullOrBlank())
            return null

        return try {
            val inputFile = File(inputPath)
            if (!inputFile.exists())
                return null

            val exif = ExifInterface(inputFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val originalBitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
            var rotatedBitmap = rotateBitmapIfRequired(originalBitmap, orientation)

            if (isFrontCamera) {
                val matrix = Matrix().apply { postScale(-1f, 1f) }
                rotatedBitmap = Bitmap.createBitmap(
                    rotatedBitmap,
                    0,
                    0,
                    rotatedBitmap.width,
                    rotatedBitmap.height,
                    matrix,
                    true
                )
            }

            val resizedBitmap = resizeBitmapMaintainingAspectRatio(rotatedBitmap, maxWidth, maxHeight)

            val compressedFile = File(
                context.cacheDir,
                "compressed_${System.currentTimeMillis()}.jpg"
            )

            FileOutputStream(compressedFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            val newExif = ExifInterface(compressedFile.absolutePath)
            newExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            newExif.saveAttributes()

            compressedFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun resizeBitmapMaintainingAspectRatio(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val widthRatio = maxWidth.toFloat() / width
        val heightRatio = maxHeight.toFloat() / height
        val scaleFactor = minOf(widthRatio, heightRatio, 1.0f)

        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 ->
                matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 ->
                matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 ->
                matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL ->
                matrix.postScale(1f, -1f)

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            else ->
                return bitmap
        }

        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }


    fun getFileFromDrawable(resId: Int): File {
        val inputStream = context.resources.openRawResource(resId)
        val file = File(context.cacheDir, "${resId}.jpg")
        val outputStream = FileOutputStream(file)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    fun getUriFromFilePath(filePath: String): Uri {
        val file = File(filePath)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}