package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.ProfilePostsRecyclerViewAdapter
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.objects.Relationship
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {
    private lateinit var preferences : SharedPreferences
    private lateinit var pixelfedAPI : PixelfedAPI
    private lateinit var adapter : ProfilePostsRecyclerViewAdapter
    private lateinit var recycler : RecyclerView
    private var accessToken : String? = null
    private var account: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        preferences = this.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE)
        pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "")

        // Set posts RecyclerView as a grid with 3 columns
        recycler = findViewById(R.id.profilePostsRecyclerView)
        recycler.layoutManager = GridLayoutManager(applicationContext, 3)
        adapter = ProfilePostsRecyclerViewAdapter(this)
        recycler.adapter = adapter

        setContent()
    }

    private fun setContent() {
        // Set profile according to given account
        account = intent.getSerializableExtra(ACCOUNT_TAG) as Account?

        if(account == null) {
            pixelfedAPI.verifyCredentials("Bearer $accessToken")
                .enqueue(object : Callback<Account> {
                    override fun onResponse(call: Call<Account>, response: Response<Account>) {
                        if (response.code() == 200) {
                            account = response.body()!!

                            setViews()
                            // Populate profile page with user's posts
                            setPosts()
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

        } else {
            setViews()
            activateFollow()
            setPosts()
        }

        // On click open followers list
        findViewById<TextView>(R.id.nbFollowersTextView).setOnClickListener{ onClickFollowers() }
        // On click open followers list
        findViewById<TextView>(R.id.nbFollowingTextView).setOnClickListener{ onClickFollowing() }
    }

    /**
     * Populate myProfile page with user's data
     */
    private fun setViews() {
        val profilePicture = findViewById<ImageView>(R.id.profilePictureImageView)
        ImageConverter.setRoundImageFromURL(View(applicationContext), account!!.avatar, profilePicture)

        val description = findViewById<TextView>(R.id.descriptionTextView)
        description.text = account!!.note

        val accountName = findViewById<TextView>(R.id.accountNameTextView)
        accountName.text = account!!.username
        accountName.setTypeface(null, Typeface.BOLD)

        val nbPosts = findViewById<TextView>(R.id.nbPostsTextView)
        nbPosts.text = "${account!!.statuses_count}\nPosts"
        nbPosts.setTypeface(null, Typeface.BOLD)

        val nbFollowers = findViewById<TextView>(R.id.nbFollowersTextView)
        nbFollowers.text = "${account!!.followers_count}\nFollowers"
        nbFollowers.setTypeface(null, Typeface.BOLD)

        val nbFollowing = findViewById<TextView>(R.id.nbFollowingTextView)
        nbFollowing.text = "${account!!.following_count}\nFollowing"
        nbFollowing.setTypeface(null, Typeface.BOLD)
    }

    /**
     * Populate profile page with user's posts
     */
    private fun setPosts() {
        pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account!!.id)
            .enqueue(object : Callback<List<Status>> {

            override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                Log.e("ProfileActivity.Posts:", t.toString())
            }

            override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                if(response.code() == 200) {
                    val posts = ArrayList<Status>()
                    val statuses = response.body()!!
                    for(status in statuses) {
                        posts.add(status)
                    }
                    adapter.addPosts(posts)
                }
            }
        })
    }

    private fun onClickEditButton() {
        val url = "${preferences.getString("domain", "")}/settings/home"

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if(browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            val text = "Cannot open this link"
            Log.e("ProfileActivity", text)
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
                Toast.makeText(applicationContext,"Could not get follow status", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(applicationContext, "Could not display follow button", Toast.LENGTH_SHORT)
                        .show()
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
                    Toast.makeText(applicationContext, "Could not follow", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onResponse(
                    call: Call<Relationship>,
                    response: Response<Relationship>
                ) {
                    if (response.code() == 200) {
                        followButton.text = "Unfollow"
                        setOnClickUnfollow()
                    } else if (response.code() == 403) {
                        Toast.makeText(applicationContext, "This action is not allowed", Toast.LENGTH_SHORT)
                            .show()
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
                    Toast.makeText(applicationContext, "Could not unfollow", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onResponse(call: Call<Relationship>, response: Response<Relationship>) {
                    if (response.code() == 200) {
                        followButton.text = "Follow"
                        setOnClickFollow()
                    } else if (response.code() == 401) {
                        Toast.makeText(applicationContext, "The access token is invalid", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            })
        }
    }
}