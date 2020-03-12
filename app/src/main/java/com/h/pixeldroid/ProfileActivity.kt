package com.h.pixeldroid

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        var statuses: ArrayList<Status>? = null
        val BASE_URL = "https://pixelfed.de/"

        val pixelfedAPI = PixelfedAPI.create(BASE_URL)

        pixelfedAPI.timelinePublic(null, null, null, null, null)
            .enqueue(object : Callback<List<Status>> {
                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if (response.code() == 200) {
                        statuses = response.body() as ArrayList<Status>?

                        if(!statuses.isNullOrEmpty()) {

                            val account = statuses!![0].account

                            setContent(account)

                        }
                    }
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("Ouch, not OK", t.toString())
                }
            })

    }

    private fun setContent(account: Account) {
        // ImageView : profile picture
        val profilePicture = findViewById<ImageView>(R.id.profilePicture)
        Glide.with(applicationContext).load(account.avatar).into(profilePicture)

        // TextView : description / bio
        val description = findViewById<TextView>(R.id.description)
        description.text = account.note

        // TextView : account name
        val accountName = findViewById<TextView>(R.id.accountName)
        accountName.text = account.username

        // TextView : number of posts
        val nbPosts = findViewById<TextView>(R.id.nbPosts)
        nbPosts.text = account.statuses_count.toString()
        nbPosts.setTypeface(null, Typeface.BOLD)

        // TextView : number of followers
        val nbFollowers = findViewById<TextView>(R.id.nbFollowers)
        nbFollowers.text = account.followers_count.toString()
        nbFollowers.setTypeface(null, Typeface.BOLD)

        // TextView : number of following
        val nbFollowing = findViewById<TextView>(R.id.nbFollowing)
        nbFollowing.text = account.following_count.toString()
        nbFollowing.setTypeface(null, Typeface.BOLD)
    }
}
