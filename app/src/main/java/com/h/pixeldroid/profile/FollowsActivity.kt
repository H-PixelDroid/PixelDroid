package com.h.pixeldroid.profile

import android.os.Bundle
import com.h.pixeldroid.R
import com.h.pixeldroid.posts.feeds.uncachedFeeds.accountLists.AccountListFragment
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.utils.api.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.utils.api.objects.Account.Companion.FOLLOWERS_TAG
import com.h.pixeldroid.utils.BaseActivity


class FollowsActivity : BaseActivity() {
    private var followsFragment = AccountListFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        // Get account id
        val account = intent.getSerializableExtra(ACCOUNT_TAG) as Account?
        val followers = intent.getSerializableExtra(FOLLOWERS_TAG) as Boolean

        if(account == null) {
            val user = db.userDao().getActiveUser()!!
            startFragment(user.user_id, user.username, followers)
        } else {
            startFragment(account.id!!, account.getusername(), followers)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun startFragment(id : String, displayName: String, followers : Boolean) {
        supportActionBar?.title =
            if (followers) {
                getString(R.string.followers_title).format(displayName)
            } else {
                getString(R.string.follows_title).format(displayName)
            }

        val arguments = Bundle()
        arguments.putSerializable(ACCOUNT_ID_TAG, id)
        arguments.putSerializable(FOLLOWERS_TAG, followers)
        followsFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.followsFragment, followsFragment).commit()

    }
}
