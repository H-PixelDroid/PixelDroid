package org.pixeldroid.app.profile

import android.os.Bundle
import org.pixeldroid.app.R
import org.pixeldroid.app.posts.feeds.uncachedFeeds.accountLists.AccountListFragment
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Account.Companion.ACCOUNT_ID_TAG
import org.pixeldroid.app.utils.api.objects.Account.Companion.ACCOUNT_TAG
import org.pixeldroid.app.utils.api.objects.Account.Companion.FOLLOWERS_TAG
import org.pixeldroid.app.utils.BaseActivity


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
            startFragment(user.user_id, user.display_name, followers)
        } else {
            startFragment(account.id!!, account.getDisplayName(), followers)
        }
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
