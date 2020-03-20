package com.h.pixeldroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceControl
import android.view.View
import androidx.fragment.app.Fragment
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.fragments.PostFragment
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.models.Post.Companion.POST_TAG

class PostActivity : AppCompatActivity() {
    lateinit var postFragment : PostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        val post = intent.getSerializableExtra(POST_TAG) as Post
        postFragment = PostFragment.newInstance(post)

        val db = AppDatabase.getDatabase(context = this)
        Log.d("PostA", db.toString())

        supportFragmentManager.beginTransaction()
            .add(R.id.postFragmentSingle, postFragment).commit()
    }
}
