package com.h.pixeldroid

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.ProfilePostsRecyclerViewAdapter
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter.Companion.setRoundImageFromURL
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var adapter : ProfilePostsRecyclerViewAdapter
    private lateinit var recycler : RecyclerView
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Set RecyclerView as a grid with 3 columns
        recycler = findViewById(R.id.profilePostsRecyclerView)
        recycler.layoutManager = GridLayoutManager(this, 3)
        adapter = ProfilePostsRecyclerViewAdapter(this)
        recycler.adapter = adapter

        preferences = getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )

        // Set profile according to given account
        val account = intent.getSerializableExtra(ACCOUNT_TAG) as Account
        setContent(account)
        setPosts(account)
    }

    // Populate profile page with user's posts
    private fun setPosts(account: Account) {
        val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        val accessToken = preferences.getString("accessToken", "")

        pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account.id).enqueue(object :
            Callback<List<Status>> {
            override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                Log.e("ProfileFragment.Posts:", t.toString())
            }

            override fun onResponse(
                call: Call<List<Status>>,
                response: Response<List<Status>>
            ) {
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

    private fun setContent(account: Account) {
        // ImageView : profile picture
        val profilePicture = findViewById<ImageView>(R.id.profilePictureImageView)
        setRoundImageFromURL(View(this), account.avatar, profilePicture)

        // TextView : description / bio
        val description = findViewById<TextView>(R.id.descriptionTextView)
        description.text = account.note

        // TextView : account name
        val accountName = findViewById<TextView>(R.id.accountNameTextView)
        accountName.text = account.username

        // TextView : number of posts
        val nbPosts = findViewById<TextView>(R.id.nbPostsTextView)
        nbPosts.text = "${account.statuses_count}\nPosts"
        nbPosts.setTypeface(null, Typeface.BOLD)

        // TextView : number of followers
        val nbFollowers = findViewById<TextView>(R.id.nbFollowersTextView)
        nbFollowers.text = "${account.followers_count}\nFollowers"
        nbFollowers.setTypeface(null, Typeface.BOLD)

        // TextView : number of following
        val nbFollowing = findViewById<TextView>(R.id.nbFollowingTextView)
        nbFollowing.text = "${account.following_count}\nFollowing"
        nbFollowing.setTypeface(null, Typeface.BOLD)
    }
}
