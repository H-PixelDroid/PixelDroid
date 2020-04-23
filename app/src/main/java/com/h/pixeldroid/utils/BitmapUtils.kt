package com.h.pixeldroid.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.h.pixeldroid.PhotoEditActivity
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.System.currentTimeMillis
import java.net.URI

object BitmapUtils {
    fun getBitmapFromAssets(context: Context, fileName: String, width: Int, height: Int): Bitmap? {
        val assetManager = context.assets

        val inputStream: InputStream
        val bitmap: Bitmap? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = assetManager.open(fileName)

            options.inSampleSize = calculateInSampleSize(options, width, height)
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e:IOException) {
            Log.e("Debug", e.message!!)
        }

        return null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, regWidth: Int, regHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if(height > regHeight || width > regWidth) {
            while(height/ 2 / inSampleSize >= regHeight
                && width / 2 / inSampleSize >= regHeight)
                inSampleSize *= 2
        }

        return inSampleSize
    }
}