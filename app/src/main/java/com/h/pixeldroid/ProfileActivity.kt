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
import com.h.pixeldroid.utils.ImageConverter.Companion.retrieveBitmapFromUrl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Array.get
import java.nio.file.Paths.get

class ProfileActivity() : AppCompatActivity() {

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

                        if(statuses.isNullOrEmpty()) {

                            // ImageView : profile picture
                            val profilePicture = findViewById<ImageView>(R.id.profilePicture)

                            // TextView : description / bio
                            val description = findViewById<TextView>(R.id.description)
                            description.setText("")

                            // TextView : account name
                            val accountName = findViewById<TextView>(R.id.accountName)
                            accountName.setText("No Name")

                            // TextView : number of posts
                            val nbPosts = findViewById<TextView>(R.id.nbPosts)
                            nbPosts.setText(0)
                            nbPosts.setTypeface(null, Typeface.BOLD)

                            // TextView : number of followers
                            val nbFollowers = findViewById<TextView>(R.id.nbFollowers)
                            nbFollowers.setText(0)
                            nbFollowers.setTypeface(null, Typeface.BOLD)

                            // TextView : number of following
                            val nbFollowing = findViewById<TextView>(R.id.nbFollowing)
                            nbFollowing.setText(0)
                            nbFollowing.setTypeface(null, Typeface.BOLD)

                        } else {

                            val account = statuses!![0].account

                            // ImageView : profile picture
                            val profilePicture = findViewById<ImageView>(R.id.profilePicture)
                            Glide.with(this@ProfileActivity).load(account.avatar).into(profilePicture)

                            // TextView : description / bio
                            val description = findViewById<TextView>(R.id.description)
                            description.setText(account.note)

                            // TextView : account name
                            val accountName = findViewById<TextView>(R.id.accountName)
                            accountName.setText(account.username)

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
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("Ouch, not OK", t.toString())
                }
            })

    }
}
