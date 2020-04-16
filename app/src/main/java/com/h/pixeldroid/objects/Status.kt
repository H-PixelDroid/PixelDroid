package com.h.pixeldroid.objects

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.RequestBuilder
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.feeds.ViewHolder
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageFromDrawable
import com.h.pixeldroid.utils.PostUtils.Companion.likePostCall
import com.h.pixeldroid.utils.PostUtils.Companion.postComment
import com.h.pixeldroid.utils.PostUtils.Companion.retrieveComments
import com.h.pixeldroid.utils.PostUtils.Companion.toggleCommentInput
import com.h.pixeldroid.utils.PostUtils.Companion.unLikePostCall
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

    fun activateLiker(
        holder : ViewHolder,
        api: PixelfedAPI,
        credential: String,
        isLiked: Boolean
    ) {
        //Set initial state
        holder.liker.isChecked = isLiked

        //Activate the liker
        holder.liker.setEventListener { button, buttonState ->
                if (buttonState) {
                    // Button is active
                    likePostCall(holder, api, credential, this)
                } else {
                    // Button is inactive
                    unLikePostCall(holder, api, credential, this)
                }
            }
        }


    fun showComments(
        holder : ViewHolder,
        api: PixelfedAPI,
        credential: String
    ) {
        //Show all comments of a post
        if (replies_count == 0) {
            holder.viewComment.text =  "No comments on this post..."
        } else {
            holder.viewComment.text =  "View all ${replies_count} comments..."
            holder.viewComment.setOnClickListener {
                holder.viewComment.visibility = View.GONE

                //Retrieve the comments
                retrieveComments(holder, api, credential, this)
            }
        }
    }

    fun activateCommenter(
        holder : ViewHolder,
        api: PixelfedAPI,
        credential: String
    ) {
        //Toggle comment button
        toggleCommentInput(holder)

        //Activate commenter
        holder.submitCmnt.setOnClickListener {
            val textIn = holder.comment.text
            //Open text input
            if(textIn.isNullOrEmpty()) {
                Toast.makeText(holder.context,"Comment must not be empty!", Toast.LENGTH_SHORT).show()
            } else {

                //Post the comment
                postComment(holder, api, credential, this)
            }
        }
    }

    fun activateReblogger(
        holder : ViewHolder,
        api : PixelfedAPI,
        credential: String
    ) {
        holder.reblogger.setOnClickListener {

        }
    }

    enum class Visibility : Serializable {
        public, unlisted, private, direct
    }
}