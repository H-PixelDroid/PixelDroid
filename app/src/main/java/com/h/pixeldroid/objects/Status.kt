package com.h.pixeldroid.objects

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder
import com.h.pixeldroid.utils.ImageConverter
import kotlinx.android.synthetic.main.post_fragment.view.*
import java.io.Serializable

/*
Represents a status posted by an account.
https://docs.joinmastodon.org/entities/status/
 */
data class Status(
    //Base attributes
    override val id: String,
    val uri: String,
    val created_at: String, //ISO 8601 Datetime (maybe can use a date type)
    val account: Account,
    val content: String, //HTML
    val visibility: Visibility,
    val sensitive: Boolean,
    val spoiler_text: String,
    val media_attachments: List<Attachment>?,
    val application: Application,
    //Rendering attributes
    val mentions: List<Mention>,
    val tags: List<Tag>,
    val emojis: List<Emoji>,
    //Informational attributes
    val reblogs_count: Int,
    val favourites_count: Int,
    val replies_count: Int,
    //Nullable attributes
    val url: String?, //URL
    val in_reply_to_id: String?,
    val in_reply_to_account: String?,
    val reblog: Status?,
    val poll: Poll?,
    val card: Card?,
    val language: String?, //ISO 639 Part 1 two-letter language code
    val text: String?,
    //Authorized user attributes
    val favourited: Boolean,
    val reblogged: Boolean,
    val muted: Boolean,
    val bookmarked: Boolean,
    val pinned: Boolean
    ) : Serializable, FeedContent()
{

    companion object {
        const val POST_TAG = "postTag"
        const val POST_FRAG_TAG = "postFragTag"
    }

    fun getPostUrl() : String? = media_attachments?.getOrNull(0)?.url
    fun getProfilePicUrl() : String? = account.avatar
    fun getPostPreviewURL() : String? = media_attachments?.getOrNull(0)?.preview_url

    fun getDescription() : CharSequence {
        val description = content as CharSequence
        if(description.isEmpty()) {
            return "No description"
        }
        return description
    }

    fun getUsername() : CharSequence {
        var name = account?.display_name
        if (name.isNullOrEmpty()) {
            name = account?.username
        }
        return name!!
    }

    fun getNLikes() : CharSequence {
        val nLikes : Int = favourites_count ?: 0
        return "$nLikes Likes"
    }

    fun getNShares() : CharSequence {
        val nShares : Int = reblogs_count ?: 0
        return "$nShares Shares"
    }

    fun setupPost(
        rootView: View,
        request: RequestBuilder<Drawable>,
        postPic: ImageView,
        profilePic: ImageView
    ) {
        //Setup username as a button that opens the profile
        rootView.username.text = this.getUsername()
        rootView.username.setTypeface(null, Typeface.BOLD)
        rootView.username.setOnClickListener { account.openProfile(rootView.context) }

        rootView.usernameDesc.text = this.getUsername()
        rootView.usernameDesc.setTypeface(null, Typeface.BOLD)

        rootView.description.text = this.getDescription()

        rootView.nlikes.text = this.getNLikes()
        rootView.nlikes.setTypeface(null, Typeface.BOLD)

        rootView.nshares.text = this.getNShares()
        rootView.nshares.setTypeface(null, Typeface.BOLD)

        request.load(this.getPostUrl()).into(postPic)
        ImageConverter.setRoundImageFromURL(
            rootView,
            this.getProfilePicUrl(),
            profilePic
        )
        profilePic.setOnClickListener { account.openProfile(rootView.context) }
    }

    enum class Visibility : Serializable {
        public, unlisted, private, direct
    }
}