package com.h.pixeldroid.fragments

import android.content.Context
import android.content.SharedPreferences

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        preferences = this.activity!!.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        val accessToken = preferences.getString("accessToken", "")

        pixelfedAPI.verifyCredentials("Bearer $accessToken")
            .enqueue(object : Callback<Account> {
                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if (response.code() == 200) {
                        val account = response.body()!!

                        setContent(view, account)

                    }
                }

                override fun onFailure(call: Call<Account>, t: Throwable) {
                    Log.e("ProfileFragment:", t.toString())
                }
            })
    }

    private fun setContent(view: View, account: Account) {
        // ImageView : profile picture
        val profilePicture = view.findViewById<ImageView>(R.id.profilePicture)
        Glide.with(view.context).load(account.avatar).into(profilePicture)

        // TextView : description / bio
        val description = view.findViewById<TextView>(R.id.description)
        description.text = account.note

        // TextView : account name
        val accountName = view.findViewById<TextView>(R.id.accountName)
        accountName.text = account.username

        // TextView : number of posts
        val nbPosts = view.findViewById<TextView>(R.id.nbPosts)
        nbPosts.text = account.statuses_count.toString()
        nbPosts.setTypeface(null, Typeface.BOLD)

        // TextView : number of followers
        val nbFollowers = view.findViewById<TextView>(R.id.nbFollowers)
        nbFollowers.text = account.followers_count.toString()
        nbFollowers.setTypeface(null, Typeface.BOLD)

        // TextView : number of following
        val nbFollowing = view.findViewById<TextView>(R.id.nbFollowing)
        nbFollowing.text = account.following_count.toString()
        nbFollowing.setTypeface(null, Typeface.BOLD)
    }
}
