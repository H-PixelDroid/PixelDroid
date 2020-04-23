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

    fun getBitmapFromGallery(context: Context, path: Uri, width: Int, height: Int): Bitmap {
        Log.d("edit", path.toString())
        val filePatchColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(path, filePatchColumn, null, null, null)
        Log.d("edit", (cursor!=null).toString())
        cursor!!.moveToFirst()
        val columnIndex = cursor.getColumnIndex(filePatchColumn[0])
        val picturePath = cursor.getString(columnIndex)
        cursor.close()

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(picturePath, options)
        options.inSampleSize = calculateInSampleSize(options, width, height)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(picturePath, options)
    }

    private fun storeThumbnail(cr: ContentResolver, miniThumb: Bitmap?, id: Long, width: Float, height: Float, mini_kind: Int): Bitmap? {
        val matrix = Matrix()
        val scaleX = width/miniThumb!!.width
        val scaleY = height/miniThumb.height

        matrix.setScale(scaleX, scaleY)

        val thumb = Bitmap.createBitmap(miniThumb, 0, 0, miniThumb.width, miniThumb.height, matrix, true)

        val values = ContentValues(4)
        values.put(MediaStore.Images.Thumbnails.KIND, mini_kind)
        values.put(MediaStore.Images.Thumbnails.IMAGE_ID, id.toInt())
        values.put(MediaStore.Images.Thumbnails.HEIGHT, thumb.height)
        values.put(MediaStore.Images.Thumbnails.WIDTH, thumb.width)

        val uri = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values)

        try {
            val thumbOut = cr.openOutputStream(uri!!)
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut)
            thumbOut!!.close()
            return thumb
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
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