package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.h.pixeldroid.objects.Post

/**
 * @brief Shows a post using data retrieved from status
 * @param Profile, must be passed via the intent
 */
class PostActivity : AppCompatActivity() {
    companion object {
        val POST_TAG = "postTag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        val post : Post? = intent.getSerializableExtra(POST_TAG) as Post?

        //Set post fields
        Log.e("LOG: ", post.toString())

        //Setup username as a button that opens the profile
        val username = findViewById<TextView>(R.id.username)
        username.text = post?.getUsername()
        username.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }))

        findViewById<TextView>(R.id.description).text = post?.getDescription()
        findViewById<TextView>(R.id.nlikes).text = post?.getNLikes()
        findViewById<TextView>(R.id.nshares).text = post?.getNShares()

        //Load images into their respective locations
        if (post != null) {
            Glide.with(this@PostActivity).load(post.getPostUrl()).into(findViewById(R.id.postPicture))
            //Glide.with(this@PostActivity).load(post.getProfilePicUrl()).into(findViewById(R.id.profilePicture))
        }
    }
}
