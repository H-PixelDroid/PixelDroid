package com.h.pixeldroid.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageViewFromURL
import com.h.pixeldroid.utils.ImageConverter.Companion.setRoundImageFromURL
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MyProfileFragment : Fragment() {
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        preferences = this.activity!!.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        val view = inflater.inflate(R.layout.fragment_my_profile, container, false)

        // Edit button redirects to pixelfed's "edit account" page
        val editButton: Button = view.findViewById(R.id.editButton)
        editButton.setOnClickListener((View.OnClickListener { onClickEditButton() }))

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        val accessToken = preferences.getString("accessToken", "")

        pixelfedAPI.verifyCredentials("Bearer $accessToken")
            .enqueue(object : Callback<Account> {
                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if(response.code() == 200) {
                        val account = response.body()!!

                        setContent(view, account)

                        // Populate profile page with user's posts
                        pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account.id).enqueue(object : Callback<List<Status>> {
                            override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                                Log.e("ProfileFragment.Posts:", t.toString())
                            }

                            override fun onResponse(
                                call: Call<List<Status>>,
                                response: Response<List<Status>>
                            ) {
                                val statuses = response.body()!!
                                // Display first post
                                if(statuses.isNotEmpty()) {
                                    val post1 = statuses[0].media_attachments[0].preview_url
                                    val post1View = view.findViewById<ImageView>(R.id.post1)
                                    setImageViewFromURL(view, post1, post1View)
                                }
                                // Display second post
                                if(statuses.size >= 2) {
                                    val post2 = statuses[1].media_attachments[0].preview_url
                                    val post2View = view.findViewById<ImageView>(R.id.post2)
                                    setImageViewFromURL(view, post2, post2View)
                                }
                                // Display third post
                                if(statuses.size >= 3) {
                                    val post3 = statuses[2].media_attachments[0].preview_url
                                    val post3View = view.findViewById<ImageView>(R.id.post3)
                                    setImageViewFromURL(view, post3, post3View)
                                }
                            }
                        })
                    }

                }

                override fun onFailure(call: Call<Account>, t: Throwable) {
                    Log.e("ProfileFragment:", t.toString())
                }
            })
    }

        // Populate myProfile page with user's data
    private fun setContent(view: View, account: Account) {
        // ImageView : profile picture
        val profilePicture = view.findViewById<ImageView>(R.id.profilePictureImageView)
        setRoundImageFromURL(view, account.avatar, profilePicture)

        // TextView : description / bio
        val description = view.findViewById<TextView>(R.id.descriptionTextView)
        description.text = account.note

        // TextView : account name
        val accountName = view.findViewById<TextView>(R.id.accountNameTextView)
        accountName.text = account.username
        accountName.setTypeface(null, Typeface.BOLD)

        // TextView : number of posts
        val nbPosts = view.findViewById<TextView>(R.id.nbPostsTextView)
        nbPosts.text = account.statuses_count.toString() + "\nPosts"
        nbPosts.setTypeface(null, Typeface.BOLD)

        // TextView : number of followers
        val nbFollowers = view.findViewById<TextView>(R.id.nbFollowersTextView)
        nbFollowers.text = account.followers_count.toString() + "\nFollowers"
        nbFollowers.setTypeface(null, Typeface.BOLD)

        // TextView : number of following
        val nbFollowing = view.findViewById<TextView>(R.id.nbFollowingTextView)
        nbFollowing.text = account.following_count.toString() + "\nFollowing"
        nbFollowing.setTypeface(null, Typeface.BOLD)
    }

    private fun onClickEditButton() {
        val url = "${preferences.getString("domain", "")}/settings/home"

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if(activity != null && browserIntent.resolveActivity(activity!!.packageManager) != null) {
            startActivity(browserIntent)
        } else {
            val text = "Cannot open this link"
            Log.e("ProfileFragment", text)
        }
    }
}
