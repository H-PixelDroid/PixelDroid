package com.h.pixeldroid

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.ProfileBookmarkFragment
import com.h.pixeldroid.fragments.ProfilePostFragment
import com.h.pixeldroid.fragments.ProfileTabsFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.objects.Relationship
import com.h.pixeldroid.utils.DBUtils
import com.h.pixeldroid.utils.HtmlUtils.Companion.parseHTMLText
import com.h.pixeldroid.utils.ImageConverter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {
    private lateinit var pixelfedAPI : PixelfedAPI
    private var accessToken : String? = null

    private var account: Account? = null
    private lateinit var domain : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val db = DBUtils.initDB(applicationContext)

        val user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()
        pixelfedAPI = PixelfedAPI.create(domain)
        accessToken = user?.accessToken.orEmpty()
        db.close()

        setContent()
    }

    private fun setContent() {
        // Set profile according to given account
        account = intent.getSerializableExtra(ACCOUNT_TAG) as Account?

        account?.let {
            setViews()
            activateFollow()

            val tabs : Array<ProfileTabsFragment> = arrayOf(ProfilePostFragment())
            setTabs(tabs)
        } ?: run {
            pixelfedAPI.verifyCredentials("Bearer $accessToken")
                .enqueue(object : Callback<Account> {
                    override fun onResponse(call: Call<Account>, response: Response<Account>) {
                        if (response.code() == 200) {
                            account = response.body()!!
                            setViews()

                            val tabs = arrayOf(ProfilePostFragment(), ProfileBookmarkFragment())
                            setTabs(tabs)
                        }
                    }

                    override fun onFailure(call: Call<Account>, t: Throwable) {
                        Log.e("ProfileActivity:", t.toString())
                    }
                })

            // Edit button redirects to Pixelfed's "edit account" page
            val editButton = findViewById<Button>(R.id.editButton)
            editButton.visibility = View.VISIBLE
            editButton.setOnClickListener{ onClickEditButton() }
        }
    }

    /**
     * Populate myProfile page with user's data
     */
    private fun setViews() {
        val profilePicture = findViewById<ImageView>(R.id.profilePictureImageView)
        ImageConverter.setRoundImageFromURL(View(applicationContext), account!!.avatar, profilePicture)

        val description = findViewById<TextView>(R.id.descriptionTextView)
        description.text = parseHTMLText(account!!.note, emptyList(), pixelfedAPI,
            applicationContext, "Bearer $accessToken")

        val accountName = findViewById<TextView>(R.id.accountNameTextView)
        accountName.text = account!!.display_name
        accountName.setTypeface(null, Typeface.BOLD)

        val nbPosts = findViewById<TextView>(R.id.nbPostsTextView)
        nbPosts.text = applicationContext.getString(R.string.nb_posts)
            .format(account!!.statuses_count.toString())
        nbPosts.setTypeface(null, Typeface.BOLD)

        val nbFollowers = findViewById<TextView>(R.id.nbFollowersTextView)
        nbFollowers.text = applicationContext.getString(R.string.nb_followers)
            .format(account!!.followers_count.toString())
        nbFollowers.setTypeface(null, Typeface.BOLD)
        // On click open followers list
        nbFollowers.setOnClickListener{ onClickFollowers() }

        val nbFollowing = findViewById<TextView>(R.id.nbFollowingTextView)
        nbFollowing.text = applicationContext.getString(R.string.nb_following)
            .format(account!!.following_count.toString())
        nbFollowing.setTypeface(null, Typeface.BOLD)
        // On click open followers list
        nbFollowing.setOnClickListener{ onClickFollowing() }
    }

    private fun setTabs(tabs: Array<ProfileTabsFragment>) {
        val viewPager = findViewById<ViewPager2>(R.id.profile_view_pager)
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                val arguments = Bundle()
                arguments.putSerializable(ACCOUNT_TAG, account)
                tabs[0].arguments = arguments
                return tabs[position]
            }

            override fun getItemCount(): Int {
                return tabs.size
            }
        }

        TabLayoutMediator(findViewById(R.id.profile_tabs), viewPager) { tab, position ->
            when(position){
                0 -> tab.icon = getDrawable(R.drawable.ic_grid_black_24dp)
                1 -> tab.icon = getDrawable(R.drawable.ic_collections_black_24dp)
                2 -> tab.icon = getDrawable(R.drawable.ic_bookmark_black_24dp)
            }
        }.attach()
    }

    private fun onClickEditButton() {
        val url = "$domain/settings/home"

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if(browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            Log.e("ProfileActivity", "Cannot open this link")
        }
    }

    private fun onClickFollowers() {
        val intent = Intent(this, FollowsActivity::class.java)
        intent.putExtra(Account.FOLLOWING_TAG, true)
        intent.putExtra(Account.ACCOUNT_ID_TAG, account?.id)

        ContextCompat.startActivity(this, intent, null)
    }

    private fun onClickFollowing() {
        val intent = Intent(this, FollowsActivity::class.java)
        intent.putExtra(Account.FOLLOWING_TAG, false)
        intent.putExtra(Account.ACCOUNT_ID_TAG, account?.id)

        ContextCompat.startActivity(this, intent, null)
    }

    /**
     * Set up follow button
     */
    private fun activateFollow() {
        // Get relationship between the two users (credential and this) and set followButton accordingly
        pixelfedAPI.checkRelationships("Bearer $accessToken", listOf(account!!.id))
            .enqueue(object : Callback<List<Relationship>> {

            override fun onFailure(call: Call<List<Relationship>>, t: Throwable) {
                Log.e("FOLLOW ERROR", t.toString())
                Toast.makeText(applicationContext,getString(R.string.follow_status_failed),
                    Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<List<Relationship>>, response: Response<List<Relationship>>) {
                if(response.code() == 200) {
                    if(response.body()!!.isNotEmpty()) {
                        val followButton = findViewById<Button>(R.id.followButton)

                        if (response.body()!![0].following) {
                            followButton.text = "Unfollow"
                            setOnClickUnfollow()
                        } else {
                            followButton.text = "Follow"
                            setOnClickFollow()
                        }
                        followButton.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(applicationContext, getString(R.string.follow_button_failed),
                        Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setOnClickFollow() {
        val followButton = findViewById<Button>(R.id.followButton)

        followButton.setOnClickListener {
            pixelfedAPI.follow(account!!.id, "Bearer $accessToken")
                .enqueue(object : Callback<Relationship> {

                override fun onFailure(call: Call<Relationship>, t: Throwable) {
                    Log.e("FOLLOW ERROR", t.toString())
                    Toast.makeText(applicationContext, getString(R.string.follow_error),
                        Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(
                    call: Call<Relationship>,
                    response: Response<Relationship>
                ) {
                    if (response.code() == 200) {
                        followButton.text = "Unfollow"
                        setOnClickUnfollow()
                    } else if (response.code() == 403) {
                        Toast.makeText(applicationContext, getString(R.string.action_not_allowed),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun setOnClickUnfollow() {
        val followButton = findViewById<Button>(R.id.followButton)

        followButton.setOnClickListener {
            pixelfedAPI.unfollow(account!!.id, "Bearer $accessToken")
                .enqueue(object : Callback<Relationship> {

                override fun onFailure(call: Call<Relationship>, t: Throwable) {
                    Log.e("UNFOLLOW ERROR", t.toString())
                    Toast.makeText(applicationContext, getString(R.string.unfollow_error),
                        Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<Relationship>, response: Response<Relationship>) {
                    if (response.code() == 200) {
                        followButton.text = "Follow"
                        setOnClickFollow()
                    } else if (response.code() == 401) {
                        Toast.makeText(applicationContext, getString(R.string.access_token_invalid),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}