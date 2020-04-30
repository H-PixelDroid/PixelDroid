package com.h.pixeldroid.utils

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.io.File

class ImageUtils {
    companion object {
        fun downloadImage(activity: FragmentActivity, context: Context, url: String) {
            var msg = ""
            var lastMsg = ""
            val directory = File(Environment.DIRECTORY_PICTURES)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE)
                    as DownloadManager
            val downloadUri = Uri.parse(url)
            val title = url.substring(url.lastIndexOf("/") + 1)
            val request = DownloadManager.Request(downloadUri).apply {
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                        or DownloadManager.Request.NETWORK_MOBILE)
                setTitle(title)
                setDestinationInExternalPublicDir(directory.toString(), title)
            }
            val downloadId = downloadManager.enqueue(request)
            val query = DownloadManager.Query().setFilterById(downloadId)

            Thread(Runnable {
                var downloading = true
                while (downloading) {
                    val cursor: Cursor = downloadManager.query(query)
                    cursor.moveToFirst()
                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                    }
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    msg = statusMessage(url, directory, status)
                    if (msg != lastMsg && msg != "") {
                        activity.runOnUiThread {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        lastMsg = msg
                    }
                    cursor.close()
                }
            }).start()
        }

        private fun statusMessage(url: String, directory: File, status: Int): String {
            return when (status) {
                DownloadManager.STATUS_FAILED -> "Download has been failed, please try again"
                DownloadManager.STATUS_RUNNING -> "Downloading..."
                DownloadManager.STATUS_SUCCESSFUL -> "Image downloaded successfully in $directory" + File.separator + url.substring(
                    url.lastIndexOf("/") + 1
                )
                else -> ""
            }
        }
    }
}