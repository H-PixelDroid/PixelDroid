package com.h.pixeldroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.h.pixeldroid.fragments.feeds.FollowsFragment
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.objects.Account.Companion.FOLLOWING_TAG

class FollowersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)

        // Get account id
        val id = intent.getSerializableExtra(ACCOUNT_ID_TAG) as String
        val following = intent.getSerializableExtra(FOLLOWING_TAG) as Boolean

        val arguments = Bundle()
        arguments.putSerializable(ACCOUNT_ID_TAG, id)
        arguments.putSerializable(FOLLOWING_TAG, following)
        FollowsFragment().arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.followsFragment, FollowsFragment()).commit()
    }
}
