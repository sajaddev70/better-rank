package com.marvelous.betterrank.fileManager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FilePickerHelper(
    private val context: Context,
    private val activityResultRegistry: ActivityResultRegistry,
    private val onFilePicked: (uri: Uri, name: String, stream: InputStream?, realPath: String?) -> Unit,
    private val onError: (error: String) -> Unit
) {

    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    fun register() {
        filePickerLauncher = activityResultRegistry.register(
            "filePicker",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val name = getFileName(uri)
                    val inputStream = getInputStream(uri)
                    val realPath = getRealPathCompat(uri)
                    onFilePicked(uri, name, inputStream, realPath)
                } else {
                    //onError("فایلی انتخاب نشد.")
                }
            } else {
                //onError("فرآیند لغو شد.")
            }
        }
    }

    fun pickFile(mimeType: String = "*/*") {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
        }
        filePickerLauncher.launch(intent)
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    private fun getInputStream(uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        }
    }


    private fun getRealPathCompat(uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "file" -> {
                    uri.path
                }
                "content" -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        val projection = arrayOf(MediaStore.Images.Media.DATA)
                        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                            val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                            if (columnIndex != -1 && cursor.moveToFirst()) {
                                val path = cursor.getString(columnIndex)
                                if (!path.isNullOrBlank()) return path
                            }
                        }
                    }

                    val fileName = getFileName(uri)
                    val tempFile = File(context.cacheDir, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile.absolutePath
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
