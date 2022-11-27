package org.pixeldroid.app.profile

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityProfileBinding
import org.pixeldroid.app.posts.parseHTMLText
import org.pixeldroid.app.utils.BaseThemedWithBarActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.setProfileImageFromURL
import retrofit2.HttpException
import java.io.IOException

class ProfileActivity : BaseThemedWithBarActivity() {

    private lateinit var domain : String
    private lateinit var accountId : String
    private lateinit var binding: ActivityProfileBinding

    private var user: UserDatabaseEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()

        // Set profile according to given account
        val account = intent.getSerializableExtra(Account.ACCOUNT_TAG) as Account?
        accountId = account?.id ?: user!!.user_id

        val tabs = createProfileTabs(account)
        setupTabs(tabs)
        setContent(account)
    }

    private fun createProfileTabs(account: Account?): Array<Fragment>{

        val profileFeedFragment = ProfileFeedFragment()
        profileFeedFragment.arguments = Bundle().apply {
            putSerializable(Account.ACCOUNT_TAG, account)
            putSerializable(ProfileFeedFragment.PROFILE_GRID, false)
            putSerializable(ProfileFeedFragment.BOOKMARKS, false)
        }

        val profileGridFragment = ProfileFeedFragment()
        profileGridFragment.arguments = Bundle().apply {
            putSerializable(Account.ACCOUNT_TAG, account)
            putSerializable(ProfileFeedFragment.PROFILE_GRID, true)
            putSerializable(ProfileFeedFragment.BOOKMARKS, false)
        }

        val profileCollectionsFragment = ProfileFeedFragment()
        profileCollectionsFragment.arguments = Bundle().apply {
            putSerializable(Account.ACCOUNT_TAG, account)
            putSerializable(ProfileFeedFragment.PROFILE_GRID, true)
            putSerializable(ProfileFeedFragment.BOOKMARKS, false)
            putSerializable(ProfileFeedFragment.COLLECTIONS, true)
        }

        val returnArray: Array<Fragment> = arrayOf(
            profileGridFragment,
            profileFeedFragment,
            profileCollectionsFragment
        )

        // If we are viewing our own account, show bookmarks
        if(account == null || account.id == user?.user_id) {
            val profileBookmarksFragment = ProfileFeedFragment()
            profileBookmarksFragment.arguments = Bundle().apply {
                putSerializable(Account.ACCOUNT_TAG, account)
                putSerializable(ProfileFeedFragment.PROFILE_GRID, true)
                putSerializable(ProfileFeedFragment.BOOKMARKS, true)
            }
            return returnArray + profileBookmarksFragment
        }
        return returnArray
    }

    private fun setupTabs(
        tabs: Array<Fragment>
    ){
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return tabs[position]
            }

            override fun getItemCount(): Int {
                return tabs.size
            }
        }
        TabLayoutMediator(binding.profileTabs, binding.viewPager) { tab, position ->
            tab.tabLabelVisibility = TabLayout.TAB_LABEL_VISIBILITY_UNLABELED
            when (position) {
                0 -> {
                    tab.setText(R.string.grid_view)
                    tab.setIcon(R.drawable.grid_on_black_24dp)
                }
                1 -> {
                    tab.setText(R.string.feed_view)
                    tab.setIcon(R.drawable.feed_view)
                }
                2 -> {
                    tab.setText(R.string.collections)
                    tab.setIcon(R.drawable.collections)
                }
                3 -> {
                    tab.setText(R.string.bookmarks)
                    tab.setIcon(R.drawable.bookmark)
                }
            }
        }.attach()
    }


    private fun setContent(account: Account?) {
        if(account != null) {
            setViews(account)
        } else {
            supportActionBar?.setTitle(R.string.menu_account)
            lifecycleScope.launchWhenResumed {
                val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                val myAccount: Account = try {
                    api.verifyCredentials()
                } catch (exception: Exception) {
                    Log.e("ProfileActivity:", exception.toString())
                    Toast.makeText(
                        applicationContext, "Could not get your profile",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launchWhenResumed
                }
                setViews(myAccount)
            }
        }

        if(account != null && account.id != user?.user_id) {
            //if we aren't viewing our own account, activate follow button
            activateFollow(account)
        } else {
            //if we *are* viewing our own account, activate the edit button
            activateEditButton()
        }

        // On click open followers list
        binding.nbFollowersTextView.setOnClickListener{ onClickFollowers(account) }
        // On click open followers list
        binding.nbFollowingTextView.setOnClickListener{ onClickFollowing(account) }
    }

    /**
     * Populate profile page with user's data
     */
    private fun setViews(account: Account) {
        val profilePicture = binding.profilePictureImageView
        setProfileImageFromURL(
            View(applicationContext),
            account.anyAvatar(),
            profilePicture
        )

        binding.descriptionTextView.text = parseHTMLText(
                account.note ?: "", emptyList(), apiHolder,
                binding.descriptionTextView.context,
                lifecycleScope
        )
        // This is so that the clicks in the text (eg #, @) work.
        binding.descriptionTextView.movementMethod = LinkMovementMethod.getInstance();

        val displayName = account.getDisplayName()

        binding.accountNameTextView.text = displayName

        supportActionBar?.title = displayName
        if(displayName != "@${account.acct}") {
            supportActionBar?.subtitle = "@${account.acct}"
        }

        binding.nbPostsTextView.text = resources.getQuantityString(
                R.plurals.nb_posts,
                account.statuses_count ?: 0,
                account.statuses_count ?: 0
        )

        binding.nbFollowersTextView.text = resources.getQuantityString(
                R.plurals.nb_followers,
                account.followers_count ?: 0,
                account.followers_count ?: 0
        )

        binding.nbFollowingTextView.text = resources.getQuantityString(
                R.plurals.nb_following,
                account.following_count ?: 0,
                account.following_count ?: 0
        )
    }

    private fun onClickEditButton() {
        val intent = Intent(this, EditProfileActivity::class.java)
        ContextCompat.startActivity(this, intent, null)
    }

    private fun onClickFollowers(account: Account?) {
        val intent = Intent(this, FollowsActivity::class.java)
        intent.putExtra(Account.FOLLOWERS_TAG, true)
        intent.putExtra(Account.ACCOUNT_TAG, account)

        ContextCompat.startActivity(this, intent, null)
    }

    private fun onClickFollowing(account: Account?) {
        val intent = Intent(this, FollowsActivity::class.java)
        intent.putExtra(Account.FOLLOWERS_TAG, false)
        intent.putExtra(Account.ACCOUNT_TAG, account)

        ContextCompat.startActivity(this, intent, null)
    }

    private fun activateEditButton() {
        // Edit button redirects to Pixelfed's "edit account" page
        binding.editButton.apply {
            visibility = View.VISIBLE
            setOnClickListener{ onClickEditButton() }
        }
    }

    /**
     * Set up follow button
     */
    private fun activateFollow(account: Account) {
        // Get relationship between the two users (credential and this) and set followButton accordingly
        lifecycleScope.launch {
            try {
                val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                val relationship = api.checkRelationships(
                    listOf(account.id.orEmpty())
                ).firstOrNull()

                if(relationship != null){
                    if (relationship.following == true || relationship.requested == true) {
                        setOnClickUnfollow(account, relationship.requested == true)
                    } else {
                        setOnClickFollow(account)
                    }
                    binding.followButton.visibility = View.VISIBLE
                }
            } catch (exception: IOException) {
                Log.e("FOLLOW ERROR", exception.toString())
                Toast.makeText(
                    applicationContext, getString(R.string.follow_status_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: HttpException) {
                Toast.makeText(
                    applicationContext, getString(R.string.follow_button_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setOnClickFollow(account: Account) {
        binding.followButton.apply {
            setText(R.string.follow)
            setOnClickListener {
                lifecycleScope.launchWhenResumed {
                    try {
                        val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                        val rel = api.follow(account.id.orEmpty())
                        if(rel.following == true) setOnClickUnfollow(account, rel.requested == true)
                        else setOnClickFollow(account)
                    } catch (exception: Exception) {
                        Log.e("FOLLOW ERROR", exception.toString())
                        Toast.makeText(
                            applicationContext, getString(R.string.follow_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setOnClickUnfollow(account: Account, requested: Boolean) {
        binding.followButton.apply {
            if(account.locked == true && requested) {
                setText(R.string.follow_requested)
            } else setText(R.string.unfollow)


            fun unfollow() {
                lifecycleScope.launchWhenResumed {
                    try {
                        val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                        val rel = api.unfollow(account.id.orEmpty())
                        if(rel.following == false && rel.requested == false) setOnClickFollow(account)
                        else setOnClickUnfollow(account, rel.requested == true)
                    } catch (exception: IOException) {
                        Log.e("FOLLOW ERROR", exception.toString())
                        Toast.makeText(
                                applicationContext, getString(R.string.unfollow_error),
                                Toast.LENGTH_SHORT
                        ).show()
                    } catch (exception: HttpException) {
                        Toast.makeText(
                                applicationContext, getString(R.string.unfollow_error),
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            setOnClickListener {
                if(account.locked == true && requested){
                    AlertDialog.Builder(context)
                            .setMessage(R.string.dialog_message_cancel_follow_request)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                unfollow()
                            }
                            .setNegativeButton(android.R.string.cancel){_, _ -> }
                            .show()
                } else unfollow()
            }
        }
    }
}
