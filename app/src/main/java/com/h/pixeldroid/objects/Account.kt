package com.h.pixeldroid.objects

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import com.h.pixeldroid.ProfileActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.ImageConverter.Companion.setRoundImageFromURL
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

                // Open profile activity with given account
                fun openProfile(context: Context, account: Account) {
                        val intent = Intent(context, ProfileActivity::class.java)
                        intent.putExtra(Account.ACCOUNT_TAG, account)
                        startActivity(context, intent, null)
                }

                // Set views with account's data
                fun setContent(view: View, account: Account) {
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
                        nbPosts.text = "${account.statuses_count}\nPosts"
                        nbPosts.setTypeface(null, Typeface.BOLD)

                        // TextView : number of followers
                        val nbFollowers = view.findViewById<TextView>(R.id.nbFollowersTextView)
                        nbFollowers.text = "${account.followers_count}\nFollowers"
                        nbFollowers.setTypeface(null, Typeface.BOLD)

                        // TextView : number of following
                        val nbFollowing = view.findViewById<TextView>(R.id.nbFollowingTextView)
                        nbFollowing.text = "${account.following_count}\nFollowing"
                        nbFollowing.setTypeface(null, Typeface.BOLD)
                }
        }
}

