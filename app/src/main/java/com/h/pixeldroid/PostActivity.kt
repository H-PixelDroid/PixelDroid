package com.h.pixeldroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.fragments.PostFragment
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.models.Post.Companion.POST_TAG


class PostActivity : AppCompatActivity() {
    lateinit var postFragment : PostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        val post = intent.getSerializableExtra(POST_TAG) as Post

        postFragment = PostFragment()
        val arguments = Bundle()
        arguments.putSerializable(POST_TAG, post)
        postFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.postFragmentSingle, postFragment).commit()
    }
}
