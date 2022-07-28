package org.pixeldroid.app.utils

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.pixeldroid.app.R

    /**
 * @brief Loads a given image (via url) as a round image into a given image view
 * @param view, the view in which this is happening
 * @param url, the url of the image that will be loaded
 * @param image, the imageView into which we will load the image
 */
fun setProfileImageFromURL(view : View, url : String?, image : ImageView) {
    Glide.with(view).load(url).apply(RequestOptions().circleCrop())
        .placeholder(R.drawable.ic_default_user).into(image)
}

/**
 * @brief Loads a given image (via url) as a square image into a given image view
 * @param view, the view in which this is happening
 * @param url, the url of the image that will be loaded
 * @param image, the imageView into which we will load the image
 */
fun setSquareImageFromURL(view : View, url : String?, image : ImageView, blurhash: String? = null) {
    Glide.with(view).load(url).placeholder(
        blurhash?.let { BlurHashDecoder.blurHashBitmap(view.resources, it, 32, 32) }
    ).apply(RequestOptions().centerCrop()).into(image)
}