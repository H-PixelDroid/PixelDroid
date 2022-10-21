package org.pixeldroid.app.postCreation.photoEdit.cropper

// Simplified version of https://github.com/ArthurHub/Android-Image-Cropper , which is
// licensed under the Apache License, Version 2.0. The modifications made to it for PixelDroid
// are under licensed under the GPLv3 or later, just like the rest of the PixelDroid project

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.graphics.toRect
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.pixeldroid.app.databinding.CropImageViewBinding
import org.pixeldroid.app.postCreation.photoEdit.VideoEditActivity


/** Custom view that provides cropping capabilities to an image.  */
class CropImageView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    FrameLayout(context!!, attrs) {


    private val binding: CropImageViewBinding =
        CropImageViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.CropOverlayView.setInitialAttributeValues()
    }

    /**
     * Gets the crop window's position relative to the parent's view at screen.
     *
     * @return a Rect instance containing notCropped area boundaries of the source Bitmap
     */
    val cropWindowRect: RectF
        get() = binding.CropOverlayView.cropWindowRect


    /** Reset crop window to initial rectangle.  */
    fun resetCropRect() {
        binding.CropOverlayView.resetCropWindowRect()
    }

    fun getInitialCropWindowRect(): Rect = binding.CropOverlayView.initialCropWindowRect

    /**
     * Sets the image loaded from the given URI as the content of the CropImageView
     *
     * @param uri the URI to load the image from
     */
    fun setImageUriAsync(uri: Uri, cropRelativeDimensions: VideoEditActivity.RelativeCropPosition) {
        // either no existing task is working or we canceled it, need to load new URI
        binding.CropOverlayView.initialCropWindowRect = Rect()

        Glide.with(this).load(uri).fitCenter().listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                m: Any?,
                t: Target<Drawable>?,
                i: Boolean,
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean,
            ): Boolean {
                // Get width and height that the image will take on the screen
                val drawnWidth = resource?.intrinsicWidth ?: width
                val drawnHeight = resource?.intrinsicHeight ?: height

                binding.CropOverlayView.initialCropWindowRect = RectF(
                    (width - drawnWidth) / 2f,
                    (height - drawnHeight) / 2f,
                    (width + drawnWidth) / 2f,
                    (height + drawnHeight) / 2f
                ).toRect()
                binding.CropOverlayView.setCropWindowLimits(
                    drawnWidth.toFloat(),
                    drawnHeight.toFloat()
                )
                binding.CropOverlayView.invalidate()
                binding.CropOverlayView.setBounds(width, height)
                binding.CropOverlayView.resetCropOverlayView()
                if (!cropRelativeDimensions.notCropped()) binding.CropOverlayView.setRecordedCropWindowRect(cropRelativeDimensions)
                binding.CropOverlayView.visibility = VISIBLE


                // Indicate to Glide that the image hasn't been set yet
                return false
            }
        }).into(binding.ImageViewImage)
    }
}