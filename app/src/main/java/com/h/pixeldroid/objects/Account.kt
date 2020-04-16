package com.h.pixeldroid.objects

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import com.h.pixeldroid.ProfileActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.utils.ImageConverter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Serializable

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

        /**
         * @brief Opens an activity of the profile withn the given id
         */
        fun getAccountFromId(id: String, api : PixelfedAPI, context: Context) {
            api.getAccount(id).enqueue( object : Callback<Account> {
                override fun onFailure(call: Call<Account>, t: Throwable) {
                    Log.e("GET ACCOUNT ERROR", t.toString())
                }

                override fun onResponse(
                    call: Call<Account>,
                    response: Response<Account>
                ) {
                    if(response.code() == 200) {
                        val account = response.body()!!

                        //Open the account page in a seperate activity
                        account.openProfile(context)
                    } else {
                        Log.e("ERROR CODE", response.code().toString())
                    }
                }

            })
        }
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
}

