package org.pixeldroid.app.postCreation.photoEdit.cropper

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toRect
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.pixeldroid.app.R


/** Custom view that provides cropping capabilities to an image.  */
class CropImageView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    FrameLayout(context!!, attrs) {
    /** Image view widget used to show the image for cropping.  */
    private val mImageView: ImageView

    /** Overlay over the image view to show cropping UI.  */
    private val mCropOverlayView: CropOverlayView?

    /** The sample size the image was loaded by if was loaded by URI  */
    private var mLoadedSampleSize = 1

    init {
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.crop_image_view, this, true)
        mImageView = v.findViewById(R.id.ImageView_image)
        mCropOverlayView = v.findViewById(R.id.CropOverlayView)
        mCropOverlayView.setInitialAttributeValues()
    }

    /**
     * Gets the crop window's position relative to the parent's view at screen.
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     */
    val cropWindowRect: RectF?
        get() = mCropOverlayView?.cropWindowRect// Get crop window position relative to the displayed image.

    /**
     * Set the crop window position and size to the given rectangle.
     * Image to crop must be first set before invoking this, for async - after complete callback.
     *
     * @param rect window rectangle (position and size) relative to source bitmap
     */
    fun setCropRect(rect: Rect?) {
        mCropOverlayView!!.initialCropWindowRect = rect
    }

    /** Reset crop window to initial rectangle.  */
    fun resetCropRect() {
        mCropOverlayView!!.resetCropWindowRect()
    }

    /**
     * Sets a bitmap loaded from the given Android URI as the content of the CropImageView.<br></br>
     * Can be used with URI from gallery or camera source.<br></br>
     * Will rotate the image by exif data.<br></br>
     *
     * @param uri the URI to load the image from
     */
    fun setImageUriAsync(uri: Uri) {
            // either no existing task is working or we canceled it, need to load new URI
            mCropOverlayView!!.initialCropWindowRect = null

        Glide.with(this).load(uri).fitCenter().listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {return false }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                // Get width and height that the image will take on the screen
                val drawnWidth = resource?.intrinsicWidth ?: width
                val drawnHeight = resource?.intrinsicHeight ?: height

                mCropOverlayView.cropWindowRect =
                    RectF((width - drawnWidth)/2f, (height - drawnHeight)/2f, (width + drawnWidth)/2f,  (height + drawnHeight)/2f)
                mCropOverlayView.initialCropWindowRect = mCropOverlayView.cropWindowRect.toRect()
                mCropOverlayView.setCropWindowLimits(drawnWidth.toFloat(), drawnHeight.toFloat(), 1f, 1f)
                setBitmap()

                // Indicate to Glide that the image hasn't been set yet
                return false
            }
        }).into(mImageView)
    }

    /**
     * Set the given bitmap to be used in for cropping<br></br>
     * Optionally clear full if the bitmap is new, or partial clear if the bitmap has been
     * manipulated.
     */
    private fun setBitmap() {
        mLoadedSampleSize = 1
        if (mCropOverlayView != null) {
            mCropOverlayView.invalidate()
            mCropOverlayView.setBounds(width, height)
            mCropOverlayView.resetCropOverlayView()
            mCropOverlayView.visibility = VISIBLE
        }
    }
}