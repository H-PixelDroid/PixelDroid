package com.h.pixeldroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.fragments.PostFragment
import com.h.pixeldroid.objects.Status.Companion.POST_TAG
import com.h.pixeldroid.objects.Status

class PostActivity : AppCompatActivity() {
    lateinit var postFragment : PostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        val status = intent.getSerializableExtra(POST_TAG) as Status

        postFragment = PostFragment()
        val arguments = Bundle()
        arguments.putSerializable(POST_TAG, status)
        postFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.postFragmentSingle, postFragment).commit()
    }
}
