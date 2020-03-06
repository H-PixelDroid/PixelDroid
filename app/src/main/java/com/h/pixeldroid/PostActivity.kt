package com.h.pixeldroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import com.h.pixeldroid.models.Post

/**
 * @brief Shows a post using data retrieved from status
 * @param post, must be passed via the intent
 */
class PostActivity : AppCompatActivity() {
    companion object {
        const val POST_TAG = "postTag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        val post : Post? = intent.getSerializableExtra(POST_TAG) as Post?

        post?.setupPost(this)

        //Load images into their respective locations
        if (post != null) {
            Glide.with(this@PostActivity).load(post.getPostUrl()).into(findViewById(R.id.postPicture))
            Glide.with(this@PostActivity).load(post.getProfilePicUrl()).into(findViewById(R.id.profilePic))
        }
    }
}
