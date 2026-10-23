package com.marveloushost.betterrank.fileManager

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri

object  DownloadHelper {

    fun downloadFile(context: Context, url: String, fileName: String, startDownloading:()->Unit) {
        try {
            val request = DownloadManager.Request(url.toUri())
                .setTitle(fileName)
                .setDescription("Downloading file...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            startDownloading()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}