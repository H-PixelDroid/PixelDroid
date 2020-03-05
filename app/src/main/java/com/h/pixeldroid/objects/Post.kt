package com.h.pixeldroid.objects

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.utils.ImageConverter.Companion.retrieveBitmapFromUrl
import java.io.Serializable

class Post(private val status: Status?) : Serializable {
    //val postImage : Bitmap? = retrieveBitmapFromUrl(status?.media_attachments[0]?.url)

    fun getPostImage(context : AppCompatActivity) : ImageView {
        //Retrieve the url from the list of media attachments
        val imgUrl = status?.component9()?.get(0)?.component3()!!

        //Convert retrieved bitmap to an ImageView and return it
        val imageView: ImageView = ImageView(context)
        imageView.setImageBitmap(retrieveBitmapFromUrl(imgUrl))
        return imageView
    }

    fun getDescription() : CharSequence {
        val description = status?.content as CharSequence
        if(description.isEmpty()) {
            val default : CharSequence = "No description"
            return default
        }
        return description
    }

   fun getUsername() : CharSequence {
       var name = status?.account?.username
       if (name == null) {
           name = status?.account?.display_name
       }
        return name!!
   }

    fun getNLikes() : CharSequence {
        val nLikes : Int = status?.favourites_count ?: 0
        return "$nLikes Likes"
    }

    fun getNShares() : CharSequence {
        val nShares : Int = status?.reblogs_count ?: 0
        return "$nShares Shares"
    }

}