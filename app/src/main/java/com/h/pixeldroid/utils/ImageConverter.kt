package com.h.pixeldroid.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ImageConverter {
    companion object {
        fun retrieveBitmapFromUrl(src : String?) : Bitmap? {
            return try {
                val url: URL = URL(src)
                val connection : HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input : InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e : IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}
