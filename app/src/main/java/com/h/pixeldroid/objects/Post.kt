package com.h.pixeldroid.objects

import java.io.Serializable

class Post(private val status: Status?) : Serializable {
    private val defaultAvatar = "https://raw.githubusercontent.com/pixelfed/pixelfed/dev/public/img/pixelfed-icon-color.png"

    fun getPostUrl() : String? = status?.media_attachments?.get(0)?.url
    fun getProfilePicUrl() : String? = status?.account?.avatar ?: defaultAvatar

    fun getDescription() : CharSequence {
        val description = status?.content as CharSequence
        if(description.isEmpty()) {
            return "No description"
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