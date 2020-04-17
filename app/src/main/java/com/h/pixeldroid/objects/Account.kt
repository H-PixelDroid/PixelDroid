package com.h.pixeldroid.objects

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.h.pixeldroid.ProfileActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.utils.ImageConverter
import kotlinx.android.synthetic.main.profile_fragment.view.*
import retrofit2.Call
import java.io.Serializable
import retrofit2.Callback
import retrofit2.Response

/*
Represents a user and their associated profile.
https://docs.joinmastodon.org/entities/account/
 */

data class Account(
    //Base attributes
    val id: String,
    val username: String,
    val acct: String,
    val url: String, //HTTPS URL
    //Display attributes
    val display_name: String,
    val note: String, //HTML
    val avatar: String, //URL
    val avatar_static: String, //URL
    val header: String, //URL
    val header_static: String, //URL
    val locked: Boolean,
    val emojis: List<Emoji>,
    val discoverable: Boolean,
    //Statistical attributes
    val created_at: String, //ISO 8601 Datetime (maybe can use a date type)
    val statuses_count: Int,
    val followers_count: Int,
    val following_count: Int,
    //Optional attributes
    val moved: Account? = null,
    val fields: List<Field>? = emptyList(),
    val bot: Boolean =  false,
    val source: Source? = null
) : Serializable {
    companion object {
        const val ACCOUNT_TAG = "AccountTag"
    }

    // Open profile activity with given account
    fun openProfile(context: Context) {
        val intent = Intent(context, ProfileActivity::class.java)
        intent.putExtra(Account.ACCOUNT_TAG, this)
        startActivity(context, intent, null)
    }

    // Populate myProfile page with user's data
    fun setContent(view: View) {
        val profilePicture = view.findViewById<ImageView>(R.id.profilePictureImageView)
        ImageConverter.setRoundImageFromURL(view, this.avatar, profilePicture)

        val description = view.findViewById<TextView>(R.id.descriptionTextView)
        description.text = this.note

        val accountName = view.findViewById<TextView>(R.id.accountNameTextView)
        accountName.text = this.username
        accountName.setTypeface(null, Typeface.BOLD)

        val nbPosts = view.findViewById<TextView>(R.id.nbPostsTextView)
        nbPosts.text = "${this.statuses_count}\nPosts"
        nbPosts.setTypeface(null, Typeface.BOLD)

        val nbFollowers = view.findViewById<TextView>(R.id.nbFollowersTextView)
        nbFollowers.text = "${this.followers_count}\nFollowers"
        nbFollowers.setTypeface(null, Typeface.BOLD)

        val nbFollowing = view.findViewById<TextView>(R.id.nbFollowingTextView)
        nbFollowing.text = "${this.following_count}\nFollowing"
        nbFollowing.setTypeface(null, Typeface.BOLD)
    }

    // Activate follow button
    fun activateFollow(
        view : View,
        context : Context,
        api : PixelfedAPI,
        credential : String
    ) {
        // Get relationship between the two users (credential and this) and set followButton accordingly
        api.checkRelationships("Bearer $credential", listOf(id)).enqueue(object : Callback<List<Relationship>> {
            override fun onFailure(call: Call<List<Relationship>>, t: Throwable) {
                Log.e("FOLLOW ERROR", t.toString())
            }

            override fun onResponse(call: Call<List<Relationship>>, response: Response<List<Relationship>>) {
                if(response.code() == 200) {
                    view.followButton.setOnClickListener {
                        if (response.body()!![0].following) {
                            setOnClickUnfollow(view, api, context, credential)
                        } else {
                            setOnClickFollow(view, api, context, credential)
                        }
                    }
                }
            }
        })
    }

    private fun setOnClickFollow(view: View, api: PixelfedAPI, context: Context, credential: String) {
        api.follow(id, "Bearer $credential").enqueue(object : Callback<Relationship> {
            override fun onFailure(call: Call<Relationship>, t: Throwable) {
                Log.e("FOLLOW ERROR", t.toString())
            }

            override fun onResponse(call: Call<Relationship>, response: Response<Relationship>) {
                if(response.code() == 200) {
                    view.followButton.text = "Unfollow"
                } else if(response.code() == 403) {
                    Toast.makeText(context,"This action is not allowed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setOnClickUnfollow(view: View, api: PixelfedAPI, context: Context, credential: String) {
        api.unfollow(id, "Bearer $credential").enqueue(object : Callback<Relationship> {
            override fun onFailure(call: Call<Relationship>, t: Throwable) {
                Log.e("UNFOLLOW ERROR", t.toString())
            }

            override fun onResponse(call: Call<Relationship>, response: Response<Relationship>) {
                if(response.code() == 200) {
                    view.followButton.text = "Follow"
                } else if(response.code() == 401) {
                    Toast.makeText(context,"The access token is invalid", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
