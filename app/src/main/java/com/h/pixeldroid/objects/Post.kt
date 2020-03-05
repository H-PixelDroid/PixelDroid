package com.h.pixeldroid.objects

import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.utils.ImageConverter.Companion.retrieveBitmapFromUrl
import java.io.Serializable

class Post(private val status: Status) : Serializable {

    fun getPostImage(context : AppCompatActivity) : ImageView {
        //Retreive the url from the list of media attachments
        val imgUrl = status.component9()[0].component3()

        //Convert retrieved bitmap to an ImageView and return it
        val imageView: ImageView = ImageView(context)
        imageView.setImageBitmap(retrieveBitmapFromUrl(imgUrl))
        return imageView
    }

}