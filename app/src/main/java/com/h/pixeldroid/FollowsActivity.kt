package com.h.pixeldroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.fragments.feeds.uncachedFeeds.accountLists.AccountListFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.objects.Account.Companion.FOLLOWERS_TAG
import javax.inject.Inject


class FollowsActivity : AppCompatActivity() {
    private var followsFragment = AccountListFragment()
    @Inject
    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)
        (this.application as Pixeldroid).getAppComponent().inject(this)
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
