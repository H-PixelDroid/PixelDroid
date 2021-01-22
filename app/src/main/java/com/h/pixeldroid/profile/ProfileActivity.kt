package com.h.pixeldroid.profile

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.StringRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityProfileBinding
import com.h.pixeldroid.databinding.FragmentProfileFeedBinding
import com.h.pixeldroid.databinding.FragmentProfilePostsBinding
import com.h.pixeldroid.posts.parseHTMLText
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.openUrl
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ProfileActivity : BaseActivity() {
    private lateinit var pixelfedAPI : PixelfedAPI
//    private lateinit var adapter : ProfilePostsRecyclerViewAdapter
    private lateinit var accessToken : String
    private lateinit var domain : String

    private var user: UserDatabaseEntity? = null
    private var postsFragment = ProfileFeedFragment()

    private lateinit var activityBinding: ActivityProfileBinding
    private lateinit var feedFragmentBinding: FragmentProfileFeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBinding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(activityBinding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()
        pixelfedAPI = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)
        accessToken = user?.accessToken.orEmpty()

        // Set posts RecyclerView as a grid with 3 columns
        feedFragmentBinding.profilePostsRecyclerView.layoutManager = GridLayoutManager(applicationContext, 3)
//        adapter = ProfilePostsRecyclerViewAdapter()
//        binding.profilePostsRecyclerView.adapter = adapter

        // Set profile according to given account
        val account = intent.getSerializableExtra(Account.ACCOUNT_TAG) as Account?

        setContent(account)

        activityBinding.profileRefreshLayout.setOnRefreshListener {
            getAndSetAccount(account?.id ?: user!!.user_id)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setContent(account: Account?) {
        if(account != null) {
            setViews(account)
//            setPosts(account)
            startFragment(account)
        } else {
            lifecycleScope.launchWhenResumed {
                val myAccount: Account = try {
                    pixelfedAPI.verifyCredentials("Bearer $accessToken")
                } catch (exception: IOException) {
                    Log.e("ProfileActivity:", exception.toString())
                    return@launchWhenResumed showError()
                } catch (exception: HttpException) {
                    return@launchWhenResumed showError()
                }
                setViews(myAccount)
                // Populate profile page with user's posts
//                setPosts(myAccount)
                startFragment(myAccount)
            }
        }

        //if we aren't viewing our own account, activate follow button
        if(account != null && account.id != user?.user_id) activateFollow(account)
        //if we *are* viewing our own account, activate the edit button
        else activateEditButton()


        // On click open followers list
        activityBinding.nbFollowersTextView.setOnClickListener{ onClickFollowers(account) }
        // On click open followers list
        activityBinding.nbFollowingTextView.setOnClickListener{ onClickFollowing(account) }
    }

    private fun getAndSetAccount(id: String){
        lifecycleScope.launchWhenCreated {
            val account = try{
                pixelfedAPI.getAccount("Bearer $accessToken", id)
            } catch (exception: IOException) {
                Log.e("ProfileActivity:", exception.toString())
                return@launchWhenCreated showError()
            } catch (exception: HttpException) {
                return@launchWhenCreated showError()
            }
            setContent(account)
        }
    }

    private fun showError(@StringRes errorText: Int = R.string.loading_toast, show: Boolean = true){
        val motionLayout = activityBinding.motionLayout
        if(show){
            motionLayout.transitionToEnd()
        } else {
            motionLayout.transitionToStart()
        }
        activityBinding.profileProgressBar.visibility = View.GONE
        activityBinding.profileRefreshLayout.isRefreshing = false
    }

    /**
     * Populate profile page with user's data
     */
    private fun setViews(account: Account) {
        val profilePicture = activityBinding.profilePictureImageView
        ImageConverter.setRoundImageFromURL(
            View(applicationContext),
            account.avatar,
            profilePicture
        )

        activityBinding.descriptionTextView.text = parseHTMLText(
            account.note ?: "", emptyList(), pixelfedAPI,
            applicationContext, "Bearer $accessToken",
            lifecycleScope
        )

        val displayName = account.getDisplayName()

        activityBinding.accountNameTextView.text = displayName

        supportActionBar?.title = displayName
        if(displayName != "@${account.acct}"){
            supportActionBar?.subtitle = "@${account.acct}"
        }

        activityBinding.nbPostsTextView.text = applicationContext.getString(R.string.nb_posts)
            .format(account.statuses_count.toString())

        activityBinding.nbFollowersTextView.text = applicationContext.getString(R.string.nb_followers)
            .format(account.followers_count.toString())

        activityBinding.nbFollowingTextView.text = applicationContext.getString(R.string.nb_following)
            .format(account.following_count.toString())
    }

    private fun startFragment(account: Account) {

        val arguments = Bundle()
        arguments.putSerializable(Account.ACCOUNT_ID_TAG, account.id)
        postsFragment.arguments = arguments

        supportFragmentManager.beginTransaction().add(R.id.fragment_profile_feed, postsFragment).commit()
    }

    /**
     * Populate profile page with user's posts
     */
//    private fun setPosts(account: Account) {
//        pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account.id)
//            .enqueue(object : Callback<List<Status>> {
//
//                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
//                    showError()
//                    Log.e("ProfileActivity.Posts:", t.toString())
//                }
//
//                override fun onResponse(
//                    call: Call<List<Status>>,
//                    response: Response<List<Status>>
//                ) {
//                    if (response.code() == 200) {
//                        val statuses = response.body()!!
//                        adapter.addPosts(statuses)
//                        showError(show = false)
//                    } else {
//                        showError()
//                    }
//                }
//            })
//    }

    private fun onClickEditButton() {
        val url = "$domain/settings/home"

        if (!openUrl(url)) Log.e("ProfileActivity", "Cannot open this link")
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
        activityBinding.editButton.apply {
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
                val relationship = pixelfedAPI.checkRelationships(
                    "Bearer $accessToken", listOf(account.id.orEmpty())
                ).firstOrNull()

                if(relationship != null){
                    if (relationship.following) {
                        setOnClickUnfollow(account)
                    } else {
                        setOnClickFollow(account)
                    }
                    activityBinding.followButton.visibility = View.VISIBLE
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
        activityBinding.followButton.apply {
            setText(R.string.follow)
            setOnClickListener {
                lifecycleScope.launchWhenResumed {
                    try {
                        pixelfedAPI.follow(account.id.orEmpty(), "Bearer $accessToken")
                        setOnClickUnfollow(account)
                    } catch (exception: IOException) {
                        Log.e("FOLLOW ERROR", exception.toString())
                        Toast.makeText(
                            applicationContext, getString(R.string.follow_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (exception: HttpException) {
                        Toast.makeText(
                            applicationContext, getString(R.string.follow_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setOnClickUnfollow(account: Account) {
        activityBinding.followButton.apply {
            setText(R.string.unfollow)

            setOnClickListener {
                lifecycleScope.launchWhenResumed {
                    try {
                        pixelfedAPI.unfollow(account.id.orEmpty(), "Bearer $accessToken")
                        setOnClickFollow(account)
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
        }
    }
}