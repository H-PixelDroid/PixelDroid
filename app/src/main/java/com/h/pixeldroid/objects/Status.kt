package com.h.pixeldroid.objects

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.marginStart
import com.bumptech.glide.RequestBuilder
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.feeds.HomeFragment
import com.h.pixeldroid.fragments.feeds.ViewHolder
import com.h.pixeldroid.utils.ImageConverter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

        //Set overall margin
        val containerParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        containerParams.setMargins(20, 10, 20, 10)
        container.layoutParams = containerParams

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

    fun activateLiker(
        holder : ViewHolder,
        api: PixelfedAPI,
        credential: String
    ) {
        //Activate the liker
        holder.liker.setOnClickListener {
            if (holder.isLiked) {
                api.unlikePost(credential, id).enqueue(object : Callback<Status> {
                    override fun onFailure(call: Call<Status>, t: Throwable) {
                        Log.e("UNLIKE ERROR", t.toString())
                    }

                    override fun onResponse(call: Call<Status>, response: Response<Status>) {
                        if(response.code() == 200) {
                            val resp = response.body()!!

                            //Update shown like count and internal like toggle
                            holder.nlikes.text = resp.getNLikes()
                            holder.isLiked = resp.favourited
                        } else {
                            Log.e("RESPOSE_CODE", response.code().toString())
                        }

                    }

                })

            } else {
                api.likePost(credential, id).enqueue(object : Callback<Status> {
                    override fun onFailure(call: Call<Status>, t: Throwable) {
                        Log.e("LIKE ERROR", t.toString())
                    }

                    override fun onResponse(call: Call<Status>, response: Response<Status>) {
                        if(response.code() == 200) {
                            val resp = response.body()!!

                            //Update shown like count and internal like toggle
                            holder.nlikes.text = resp.getNLikes()
                            holder.isLiked = resp.favourited
                        } else {
                            Log.e("RESPOSE_CODE", response.code().toString())
                        }
                    }

                })

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
                api.statusComments(id, credential).enqueue(object :
                    Callback<com.h.pixeldroid.objects.Context> {
                    override fun onFailure(call: Call<com.h.pixeldroid.objects.Context>, t: Throwable) {
                        Log.e("COMMENT FETCH ERROR", t.toString())
                    }

                    override fun onResponse(
                        call: Call<com.h.pixeldroid.objects.Context>,
                        response: Response<com.h.pixeldroid.objects.Context>
                    ) {
                        if(response.code() == 200) {
                            val statuses = response.body()!!.descendants

                            //Create the new views for each comment
                            for (status in statuses) {
                                addComment(
                                    holder.context,
                                    holder.commentCont,
                                    status.account,
                                    status.content
                                )
                            }
                        } else {
                            Log.e("COMMENT ERROR", "${response.code()} with body ${response.errorBody()}")
                        }
                    }
                })
            }
        }
    }

    fun activateCommenter(
        holder : ViewHolder,
        api: PixelfedAPI,
        credential: String
    ) {
        //Toggle comment button
        holder.commenter.setOnClickListener {
            when(holder.commentIn.visibility) {
                View.VISIBLE -> holder.commentIn.visibility = View.GONE
                View.INVISIBLE -> holder.commentIn.visibility = View.VISIBLE
                View.GONE -> holder.commentIn.visibility = View.VISIBLE
            }
        }

        //Activate commenter
        holder.submitCmnt.setOnClickListener {
            val textIn = holder.comment.text

            //Open text input
            if(textIn.isNullOrEmpty()) {
                Toast.makeText(holder.context,"Comment must not be empty!", Toast.LENGTH_SHORT).show()
            } else {
                val nonNullText = textIn.toString()
                api.postStatus(credential, nonNullText, id).enqueue(object :
                    Callback<Status> {
                    override fun onFailure(call: Call<Status>, t: Throwable) {
                        Log.e("COMMENT ERROR", t.toString())
                        Toast.makeText(holder.context,"Comment error!", Toast.LENGTH_SHORT).show()
                    }

                    override fun onResponse(call: Call<Status>, response: Response<Status>) {
                        //Check that the received response code is valid
                        if(response.code() == 200) {
                            val resp = response.body()!!
                            holder.commentIn.visibility = View.GONE

                            //Add the comment to the comment section
                            addComment(holder.context, holder.commentCont, resp.account, resp.content)

                            Toast.makeText(holder.context,"Comment: \"$textIn\" posted!", Toast.LENGTH_SHORT).show()
                            Log.e("COMMENT SUCCESS", "posted: $textIn")
                        } else {
                            Log.e("ERROR_CODE", response.code().toString())
                        }
                    }
                })
            }
        }
    }

    enum class Visibility : Serializable {
        public, unlisted, private, direct
    }
}