package com.h.pixeldroid.fragments

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.h.pixeldroid.LoginActivity

import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

const val BASE_URL = "https://pixelfed.de/"

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_profile, container, false)

        var statuses: ArrayList<Status>?

        val pixelfedAPI = PixelfedAPI.create(BASE_URL)

        pixelfedAPI.timelinePublic(null, null, null, null, null)
            .enqueue(object : Callback<List<Status>> {
                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if (response.code() == 200) {
                        statuses = response.body() as ArrayList<Status>?

                        if(!statuses.isNullOrEmpty()) {

                            val account = statuses!![0].account

                            setContent(view, account)

                        }
                    }
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("Ouch, not OK", t.toString())
                }
            })

        return view
    }

    private fun setContent(view: View, account: Account) {
        // ImageView : profile picture
        val profilePicture = view.findViewById<ImageView>(R.id.profilePicture)
        Glide.with(view.context.applicationContext).load(account.avatar).into(profilePicture)

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
