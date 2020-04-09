package com.h.pixeldroid.objects

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.h.pixeldroid.ProfileActivity
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
        }

        // Open profile activity with given account
        fun openProfile(context: Context) {
                val intent = Intent(context, ProfileActivity::class.java)
                intent.putExtra(Account.ACCOUNT_TAG, this)
                startActivity(context, intent, null)
        }
}

