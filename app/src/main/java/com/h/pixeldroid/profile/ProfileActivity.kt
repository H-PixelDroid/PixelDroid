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
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Relationship
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.posts.parseHTMLText
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.openUrl
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ProfileActivity : BaseActivity() {
    private lateinit var pixelfedAPI : PixelfedAPI
    private lateinit var adapter : ProfilePostsRecyclerViewAdapter
    private lateinit var recycler : RecyclerView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var accessToken : String
    private lateinit var domain : String
    private var user: UserDatabaseEntity? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()
        pixelfedAPI = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)
        accessToken = user?.accessToken.orEmpty()

        // Set posts RecyclerView as a grid with 3 columns
        recycler = findViewById(R.id.profilePostsRecyclerView)
        recycler.layoutManager = GridLayoutManager(applicationContext, 3)
        adapter = ProfilePostsRecyclerViewAdapter()
        recycler.adapter = adapter

        // Set profile according to given account
        val account = intent.getSerializableExtra(Account.ACCOUNT_TAG) as Account?

        setContent(account)

        refreshLayout = findViewById(R.id.profileRefreshLayout)

        refreshLayout.setOnRefreshListener {
            getAndSetAccount(account?.id ?: user!!.user_id)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setContent(account: Account?) {
        if(account != null){
            setViews(account)
            setPosts(account)
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
                setPosts(myAccount)
            }
        }

        //if we aren't viewing our own account, activate follow button
        if(account != null && account.id != user?.user_id) activateFollow(account)
        //if we *are* viewing our own account, activate the edit button
        else activateEditButton()


        // On click open followers list
        findViewById<TextView>(R.id.nbFollowersTextView).setOnClickListener{ onClickFollowers(account) }
        // On click open followers list
        findViewById<TextView>(R.id.nbFollowingTextView).setOnClickListener{ onClickFollowing(account) }
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
        val motionLayout = findViewById<MotionLayout>(R.id.motionLayout)
        if(show){
            motionLayout?.transitionToEnd()
        } else {
            motionLayout?.transitionToStart()
        }
        findViewById<ProgressBar>(R.id.profileProgressBar).visibility = View.GONE
        refreshLayout.isRefreshing = false
    }

    /**
     * Populate profile page with user's data
     */
    private fun setViews(account: Account) {
        val profilePicture = findViewById<ImageView>(R.id.profilePictureImageView)
        ImageConverter.setRoundImageFromURL(
            View(applicationContext),
            account.avatar,
            profilePicture
        )

        val description = findViewById<TextView>(R.id.descriptionTextView)
        description.text = parseHTMLText(
            account.note ?: "", emptyList(), pixelfedAPI,
            applicationContext, "Bearer $accessToken",
            lifecycleScope
        )

        val accountName = findViewById<TextView>(R.id.accountNameTextView)
        accountName.text = account.getDisplayName()

        val displayName = account.getDisplayName()
        supportActionBar?.title = displayName
        if(displayName != "@${account.acct}"){
            supportActionBar?.subtitle = "@${account.acct}"
        }

        accountName.setTypeface(null, Typeface.BOLD)

        val nbPosts = findViewById<TextView>(R.id.nbPostsTextView)
        nbPosts.text = applicationContext.getString(R.string.nb_posts)
            .format(account.statuses_count.toString())
        nbPosts.setTypeface(null, Typeface.BOLD)

        val nbFollowers = findViewById<TextView>(R.id.nbFollowersTextView)
        nbFollowers.text = applicationContext.getString(R.string.nb_followers)
            .format(account.followers_count.toString())
        nbFollowers.setTypeface(null, Typeface.BOLD)

        val nbFollowing = findViewById<TextView>(R.id.nbFollowingTextView)
        nbFollowing.text = applicationContext.getString(R.string.nb_following)
            .format(account.following_count.toString())
        nbFollowing.setTypeface(null, Typeface.BOLD)
    }

    /**
     * Populate profile page with user's posts
     */
    private fun setPosts(account: Account) {
        pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account.id)
            .enqueue(object : Callback<List<Status>> {

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    showError()
                    Log.e("ProfileActivity.Posts:", t.toString())
                }

                override fun onResponse(
                    call: Call<List<Status>>,
                    response: Response<List<Status>>
                ) {
                    if (response.code() == 200) {
                        val statuses = response.body()!!
                        adapter.addPosts(statuses)
                        showError(show = false)
                    } else {
                        showError()
                    }
                }
            })
    }

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
        val editButton = findViewById<Button>(R.id.editButton)
        editButton.visibility = View.VISIBLE
        editButton.setOnClickListener{ onClickEditButton() }
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
                    val followButton = findViewById<Button>(R.id.followButton)

                    if (relationship.following) {
                        setOnClickUnfollow(account)
                    } else {
                        setOnClickFollow(account)
                    }
                    followButton.visibility = View.VISIBLE
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
        val followButton = findViewById<Button>(R.id.followButton)

        followButton.setText(R.string.follow)

        followButton.setOnClickListener {
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

    private fun setOnClickUnfollow(account: Account) {
        val followButton = findViewById<Button>(R.id.followButton)

        followButton.setText(R.string.unfollow)

        followButton.setOnClickListener {

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