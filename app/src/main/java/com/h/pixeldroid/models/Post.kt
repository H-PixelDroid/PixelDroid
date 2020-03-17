package com.h.pixeldroid.models

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter
import java.io.Serializable

class Post(private val status: Status?) : Serializable {
    val id : String = status?.id ?: "0"

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

    fun getNLikes(add : Int = 0) : CharSequence {
        val nLikes : Int = (status?.favourites_count ?: 0) + add
        return "$nLikes Likes"
    }

    fun isLiked() : Boolean {
        return status?.favourited ?: false
    }

    fun getNShares(add : Int = 0) : CharSequence {
        val nShares : Int = (status?.reblogs_count ?: 0) + add
        return "$nShares Shares"
    }

    fun setupPost(fragment: Fragment, rootView : View) {
        //Setup username as a button that opens the profile
        val username = rootView.findViewById<TextView>(R.id.username)
        username.text = this.getUsername()
        username.setTypeface(null, Typeface.BOLD)

        val usernameDesc = rootView.findViewById<TextView>(R.id.usernameDesc)
        usernameDesc.text = this.getUsername()
        usernameDesc.setTypeface(null, Typeface.BOLD)

        val description = rootView.findViewById<TextView>(R.id.description)
        description.text = this.getDescription()

        val nlikes = rootView.findViewById<TextView>(R.id.nlikes)
        nlikes.text = this.getNLikes()
        nlikes.setTypeface(null, Typeface.BOLD)

        val nshares = rootView.findViewById<TextView>(R.id.nshares)
        nshares.text = this.getNShares()
        nshares.setTypeface(null, Typeface.BOLD)

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