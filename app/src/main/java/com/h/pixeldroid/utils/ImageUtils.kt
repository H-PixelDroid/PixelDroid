package com.h.pixeldroid.utils

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore.Images
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.h.pixeldroid.R
import java.io.File


class ImageUtils {
    companion object {
        fun downloadImage(activity: FragmentActivity, url: String, share: Boolean = false) {
            val context = activity.applicationContext
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
            val ext = url.substring(url.lastIndexOf("."))
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
                    if(!share) {
                        msg = when (status) {
                            DownloadManager.STATUS_FAILED ->
                                context.getString(R.string.image_download_failed)
                            DownloadManager.STATUS_RUNNING ->
                                context.getString(R.string.image_download_downloading)
                            DownloadManager.STATUS_SUCCESSFUL ->
                                context.getString(R.string.image_download_success)
                            else -> ""
                        }
                        if (msg != lastMsg && msg != "") {
                            activity.runOnUiThread {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            lastMsg = msg
                        }
                    } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val icon: Bitmap = BitmapFactory.decodeFile(
                            Uri.parse(cursor.getString(
                                cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            )).path
                        )
                        val intentShare = Intent(Intent.ACTION_SEND)
                        intentShare.type = "image/$ext"
                        val values = ContentValues()
                        values.put(Images.Media.TITLE, title)
                        values.put(Images.Media.MIME_TYPE, "image/$ext")
                        val uri: Uri = context.contentResolver.insert(
                            Images.Media.EXTERNAL_CONTENT_URI,
                            values
                        )!!
                        try {
                            val outstream = context.contentResolver.openOutputStream(uri)!!
                            icon.compress(Bitmap.CompressFormat.JPEG, 100, outstream)
                            outstream.close()
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                        intentShare.putExtra(Intent.EXTRA_STREAM, uri)
                        activity.startActivity(Intent.createChooser(intentShare, "Share Image"))
                    }
                    cursor.close()
                }
            }).start()
        }
    }
}