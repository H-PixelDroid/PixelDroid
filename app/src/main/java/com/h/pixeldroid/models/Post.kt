package com.h.pixeldroid.models

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter
import java.io.Serializable

class Post(private val status: Status?) : Serializable {
    companion object {
        const val POST_TAG = "postTag"
        const val POST_FRAG_TAG = "postFragTag"
    }

    fun getPostUrl() : String? = status?.media_attachments?.get(0)?.url
    fun getProfilePicUrl() : String? = status?.account?.avatar

    fun getDescription() : CharSequence {
        val description = status?.content as CharSequence
        if(description.isEmpty()) {
            return "No description"
        }
        return description
    }

   fun getUsername() : CharSequence {
       var name = status?.account?.username
       if (name.isNullOrEmpty()) {
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

    fun setupPost(fragment: Fragment, context : Context, rootView : View) {
        //Setup username as a button that opens the profile
        val username = rootView.findViewById<TextView>(R.id.username)
        username.text = this.getUsername()

        rootView.findViewById<TextView>(R.id.description).text = this.getDescription()
        rootView.findViewById<TextView>(R.id.nlikes).text = this.getNLikes()
        rootView.findViewById<TextView>(R.id.nshares).text = this.getNShares()

        //Setup post and profile images
        ImageConverter.setImageViewFromURL(
            fragment,
            getPostUrl(),
            rootView.findViewById(R.id.postPicture)
        )
        ImageConverter.setImageViewFromURL(
            fragment,
            getProfilePicUrl(),
            rootView.findViewById(R.id.profilePic)
        )
    }

}