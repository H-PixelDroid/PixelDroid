package com.h.pixeldroid.utils

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.h.pixeldroid.R
import java.io.File

class ImageConverter {
    companion object {
        /**
         * @brief Loads a given image (via url) into a given image view
         * @param activity, the activity in which this is happening
         * @param url, the url of the image that will be loaded
         * @param view, the imageView into which we will load the image
         */
        fun setImageViewFromURL(activity: AppCompatActivity, url : String?, view : ImageView) {
            Glide.with(activity).load(url).into(view)
        }

        /**
         * @brief Loads a given image (via url) into a given image view
         * @param fragment, the fragment in which this is happening
         * @param url, the url of the image that will be loaded
         * @param view, the imageView into which we will load the image
         */
        fun setImageViewFromURL(fragment: Fragment,  url : String?, view : ImageView) {
            Glide.with(fragment).load(url).into(view)
        }

        /**
         * @brief Loads a given image (via url) into a given image view
         * @param fragmentActivity, the fragmentActivity in which this is happening
         * @param url, the url of the image that will be loaded
         * @param view, the imageView into which we will load the image
         */
        fun setImageViewFromURL(fragmentActivity: FragmentActivity,  url : String?, view : ImageView) {
            Glide.with(fragmentActivity).load(url).into(view)
        }

        /**
         * @brief Loads a given image (via url) into a given image view
         * @param fragView, the view in which this is happening
         * @param url, the url of the image that will be loaded
         * @param view, the imageView into which we will load the image
         */
        fun setImageViewFromURL(fragView: View, url : String?, view : ImageView) {
            Glide.with(fragView).load(url).into(view)
        }

        /**
         * @brief Loads a given image (via url) as a round image into a given image view
         * @param view, the view in which this is happening
         * @param url, the url of the image that will be loaded
         * @param image, the imageView into which we will load the image
         */
        fun setRoundImageFromURL(view : View, url : String?, image : ImageView) {
            Glide.with(view).load(url).apply(RequestOptions().circleCrop())
                .placeholder(R.drawable.ic_default_user).into(image)
        }

        /**
         * @brief Loads a given image (via url) as a square image into a given image view
         * @param view, the view in which this is happening
         * @param url, the url of the image that will be loaded
         * @param image, the imageView into which we will load the image
         */
        fun setSquareImageFromURL(view : View, url : String?, image : ImageView) {
            Glide.with(view).load(url).apply(RequestOptions().centerCrop()).into(image)

        }

        /**
         * @brief Loads a given image (via url) as a square image into a given image view
         * @param view, the view in which this is happening
         * @param drawable, the drawable of the image
         * @param image, the imageView into which we will load the image
         */
        fun setSquareImageFromDrawable(view : View, drawable : Drawable?, image : ImageView) {
            Glide.with(view).load(drawable).apply(RequestOptions().centerCrop()).into(image)

        }

        /**
         * @brief Loads a default image into a given image view
         * @param view, the view in which this is happening
         * @param image, the imageView into which we will load the image
         */
        fun setImageFromDrawable(view : View, image : ImageView, drawable : Int) {
            Glide.with(view).load(drawable).into(image)
        }
    }
}