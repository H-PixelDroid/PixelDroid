package com.h.pixeldroid.models

import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.h.pixeldroid.ProfileActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status
import java.io.Serializable

class Post(private val status: Status?) : Serializable {

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

    fun setupPost(context: AppCompatActivity) {
        //Setup username as a button that opens the profile
        val username = context.findViewById<TextView>(R.id.username)
        username.text = this.getUsername()
        username.setOnClickListener((View.OnClickListener {
            val intent = Intent(context, ProfileActivity::class.java)
            context.startActivity(intent)
        }))

        context.findViewById<TextView>(R.id.description).text = this.getDescription()
        context.findViewById<TextView>(R.id.nlikes).text = this.getNLikes()
        context.findViewById<TextView>(R.id.nshares).text = this.getNShares()
    }

}