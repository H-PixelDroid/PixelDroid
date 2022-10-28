package org.pixeldroid.media_editor.photoEdit

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.TypedValue
import android.webkit.MimeTypeMap
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.exifinterface.media.ExifInterface
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.google.android.material.color.MaterialColors


fun bitmapFromUri(contentResolver: ContentResolver, uri: Uri?): Bitmap =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder
            .decodeBitmap(
                ImageDecoder.createSource(contentResolver, uri!!)
            )
            { decoder, _, _ -> decoder.isMutableRequired = true }
    } else {
        @Suppress("DEPRECATION")
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        modifyOrientation(bitmap!!, contentResolver, uri!!)
    }

fun modifyOrientation(
    bitmap: Bitmap,
    contentResolver: ContentResolver,
    uri: Uri
): Bitmap {
    val inputStream = contentResolver.openInputStream(uri)!!
    val ei = ExifInterface(inputStream)
    return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> bitmap.rotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> bitmap.rotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> bitmap.rotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> bitmap.flip(horizontal = true, vertical = false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> bitmap.flip(horizontal = false, vertical = true)
        else -> bitmap
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.flip(horizontal: Boolean, vertical: Boolean): Bitmap {
    val matrix = Matrix()
    matrix.preScale(if (horizontal) -1f else 1f, if (vertical) -1f else 1f)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

@ColorInt
fun Context.getColorFromAttr(@AttrRes attrColor: Int): Int = MaterialColors.getColor(this, attrColor, Color.BLACK)

fun Context.ffmpegCompliantUri(inputUri: Uri?): String =
    if (inputUri?.scheme == "content")
        FFmpegKitConfig.getSafParameterForRead(this, inputUri)
    else inputUri.toString()

/**
 * This method converts dp unit to equivalent pixels, depending on device density.
 */
fun Int.dpToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}


/** Maps a Float from this range to target range */
fun ClosedRange<Float>.convert(number: Float, target: ClosedRange<Float>): Float {
    val ratio = number / (endInclusive - start)
    return (ratio * (target.endInclusive - target.start))
}

fun Uri.fileExtension(contentResolver: ContentResolver): String? {
    return if (scheme == "content") {
        contentResolver.getType(this)?.takeLastWhile { it != '/' }
    } else {
        MimeTypeMap.getFileExtensionFromUrl(toString()).ifEmpty { null }
    }
}