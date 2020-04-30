package com.h.pixeldroid

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.fragments.PostFragment
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.objects.Status.Companion.DOMAIN_TAG
import com.h.pixeldroid.objects.Status.Companion.POST_TAG

class PostActivity : AppCompatActivity() {
    lateinit var postFragment : PostFragment
    lateinit var domain : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        val status = intent.getSerializableExtra(POST_TAG) as Status?

        domain = getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        ).getString("domain", "")!!

        postFragment = PostFragment()
        val arguments = Bundle()
        arguments.putSerializable(POST_TAG, status)
        arguments.putString(DOMAIN_TAG, domain)
        postFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.postFragmentSingle, postFragment).commit()
    }
}
