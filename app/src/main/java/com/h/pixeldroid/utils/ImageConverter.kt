package com.h.pixeldroid.utils

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.h.pixeldroid.models.Post

class ImageConverter {
    companion object {
        fun setImageViewFromURL(activity: AppCompatActivity, url : String?, view : ImageView) {
            Glide.with(activity).load(url).into(view)
        }

        fun setImageViewFromURL(fragment: Fragment,  url : String?, view : ImageView) {
            Glide.with(fragment).load(url).into(view)
        }

        fun setImageViewFromURL(fragmentActivity: FragmentActivity,  url : String?, view : ImageView) {
            Glide.with(fragmentActivity).load(url).into(view)
        }
    }
}