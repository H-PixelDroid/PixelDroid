package com.h.pixeldroid.objects

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.bumptech.glide.RequestBuilder
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.ImageConverter
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
        val username = rootView.findViewById<TextView>(R.id.username)
        username.text = this.getUsername()
        username.setTypeface(null, Typeface.BOLD)
        username.setOnClickListener { account.openProfile(rootView.context) }

        val usernameDesc = rootView.findViewById<TextView>(R.id.usernameDesc)
        usernameDesc.text = this.getUsername()
        usernameDesc.setTypeface(null, Typeface.BOLD)

        rootView.findViewById<TextView>(R.id.description).text = this.getDescription()

        val nlikes = rootView.findViewById<TextView>(R.id.nlikes)
        nlikes.text = this.getNLikes()
        nlikes.setTypeface(null, Typeface.BOLD)

        val nshares = rootView.findViewById<TextView>(R.id.nshares)
        nshares.text = this.getNShares()
        nshares.setTypeface(null, Typeface.BOLD)

        //Setup images
        request.load(this.getPostUrl()).into(postPic)
        ImageConverter.setRoundImageFromURL(
            rootView,
            this.getProfilePicUrl(),
            profilePic
        )
        profilePic.setOnClickListener { account.openProfile(rootView.context) }

        //Set comment initial visibility
        rootView.findViewById<LinearLayout>(R.id.commentIn).visibility = View.GONE
    }

    fun addComment(context: Context, commentContainer: LinearLayout, commentAccount: Account, commentContent: String) {
        //Create UI views
        val container = CardView(context)
        val layout = LinearLayout(context)
        val comment = TextView(context)
        val user = TextView(context)

        //Create comment view hierarchy
        layout.addView(user)
        layout.addView(comment)
        container.addView(layout)

        commentContainer.addView(container)

        //Set an id for the created comment (useful for testing)
        container.id = R.id.comment

        //Set layout constraints and content
        container.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        layout.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layout.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        user.text = commentAccount.username
        user.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        user.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        (user.layoutParams as LinearLayout.LayoutParams).weight = 8f
        user.typeface = Typeface.DEFAULT_BOLD
        comment.text = commentContent
        comment.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        comment.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        (comment.layoutParams as LinearLayout.LayoutParams).weight = 2f
    }

    enum class Visibility : Serializable {
        public, unlisted, private, direct
    }
}