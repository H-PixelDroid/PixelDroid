package com.h.pixeldroid.objects

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.toSpanned
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.RequestBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.ImageFragment
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.feeds.PostViewHolder
import com.h.pixeldroid.utils.HtmlUtils.Companion.getDomain
import com.h.pixeldroid.utils.HtmlUtils.Companion.parseHTMLText
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.PostUtils.Companion.likePostCall
import com.h.pixeldroid.utils.PostUtils.Companion.postComment
import com.h.pixeldroid.utils.PostUtils.Companion.reblogPost
import com.h.pixeldroid.utils.PostUtils.Companion.retrieveComments
import com.h.pixeldroid.utils.PostUtils.Companion.toggleCommentInput
import com.h.pixeldroid.utils.PostUtils.Companion.unLikePostCall
import com.h.pixeldroid.utils.PostUtils.Companion.undoReblogPost

import kotlinx.android.synthetic.main.post_fragment.view.*

import java.io.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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
        const val DOMAIN_TAG = "domainTag"
    }

    fun getPostUrl() : String? = media_attachments?.getOrNull(0)?.url
    fun getProfilePicUrl() : String? = account.avatar
    fun getPostPreviewURL() : String? = media_attachments?.getOrNull(0)?.preview_url

    /**
     * @brief returns the parsed version of the HTML description
     */
    private fun getDescription(api: PixelfedAPI, context: Context, credential: String) : Spanned {
        val description = content
        if(description.isEmpty()) {
            return "No description".toSpanned()
        }
        return parseHTMLText(description, mentions, api, context, credential)

    }

    fun getUsername() : CharSequence {
        var name = account.display_name
        if (name.isEmpty()) {
            name = account.username
        }
        return name
    }

    fun getNLikes() : CharSequence {
        val nLikes = favourites_count
        return "$nLikes Likes"
    }

    fun getNShares() : CharSequence {
        val nShares = reblogs_count
        return "$nShares Shares"
    }

    private fun ISO8601toDate(dateString : String, textView: TextView) {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'000000Z'")
        try {
            val date: Date? = format.parse(dateString)
            Log.e("DATE", date.toString())
            textView.text = "Posted on ${date.toString()}"
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun getStatusDomain(domain : String) : String {
        val accountDomain = getDomain(account.url)
        return if(getDomain(domain) == accountDomain) ""
        else " from $accountDomain"

    }

    private fun setupPostPics(rootView: View, request: RequestBuilder<Drawable>, homeFragment: Fragment) {
        //Check whether or not we need to activate the viewPager
        if(media_attachments?.size == 1) {
            rootView.postPicture.visibility = VISIBLE
            rootView.postPager.visibility = GONE
            rootView.postTabs.visibility = GONE
            request.load(this.getPostUrl()).into(rootView.postPicture)
        } else if(media_attachments?.size!! > 1) {
            //Only show the viewPager and tabs
            rootView.postPicture.visibility = GONE
            rootView.postPager.visibility = VISIBLE
            rootView.postTabs.visibility = VISIBLE

            val tabs : ArrayList<ImageFragment> = ArrayList()

            //Fill the tabs with each mediaAttachment
            for(media in media_attachments) {
                tabs.add(ImageFragment.newInstance(media.url))
            }
            setupTabs(tabs, rootView, homeFragment)
        }
    }

    private fun setupTabs(tabs: ArrayList<ImageFragment>, rootView: View, homeFragment: Fragment) {
        //Attach the given tabs to the view pager
        rootView.postPager.adapter = object : FragmentStateAdapter(homeFragment) {
            override fun createFragment(position: Int): Fragment {
                return tabs[position]
            }

            override fun getItemCount(): Int {
                return media_attachments?.size ?: 0
            }
        }
        TabLayoutMediator(rootView.postTabs, rootView.postPager) { tab, _ ->
            tab.icon = rootView.context.getDrawable(R.drawable.ic_dot_blue_12dp)
        }.attach()
    }

    fun setupPost(
        rootView: View,
        request: RequestBuilder<Drawable>,
        homeFragment: Fragment,
        domain : String
    ) {
        //Setup username as a button that opens the profile
        val username = rootView.findViewById<TextView>(R.id.username)
        username.text = this.getUsername()
        username.setTypeface(null, Typeface.BOLD)
        username.setOnClickListener { account.openProfile(rootView.context) }

        val usernameDesc = rootView.findViewById<TextView>(R.id.usernameDesc)
        usernameDesc.text = this.getUsername()
        usernameDesc.setTypeface(null, Typeface.BOLD)

        val nlikes = rootView.findViewById<TextView>(R.id.nlikes)
        nlikes.text = this.getNLikes()
        nlikes.setTypeface(null, Typeface.BOLD)

        val nshares = rootView.findViewById<TextView>(R.id.nshares)
        nshares.text = this.getNShares()
        nshares.setTypeface(null, Typeface.BOLD)

        //Convert the date to a readable string
        ISO8601toDate(created_at, rootView.postDate)

        rootView.postDomain.text = getStatusDomain(domain)

        //Setup images
        ImageConverter.setRoundImageFromURL(
            rootView,
            this.getProfilePicUrl(),
            rootView.profilePic
        )
        rootView.profilePic.setOnClickListener { account.openProfile(rootView.context) }

        //Setup post pic only if there are media attachments
        if(!media_attachments.isNullOrEmpty()) {
            setupPostPics(rootView, request, homeFragment)
        }


        //Set comment initial visibility
        rootView.findViewById<LinearLayout>(R.id.commentIn).visibility = View.GONE
    }

    fun setDescription(rootView: View, api : PixelfedAPI, credential: String) {
        val desc = rootView.findViewById<TextView>(R.id.description)

        desc.text = this.getDescription(api, rootView.context, credential)
        desc.movementMethod = LinkMovementMethod.getInstance()
    }

    fun activateReblogger(
        holder : PostViewHolder,
        api : PixelfedAPI,
        credential: String,
        isReblogged : Boolean
    ) {
        //Set initial button state
        holder.reblogger.isChecked = isReblogged

        //Activate the button
        holder.reblogger.setEventListener { _, buttonState ->
            if (buttonState) {
                Log.e("REBLOG", "Reblogged post")
                // Button is active
                reblogPost(holder, api, credential, this)
            } else {
                Log.e("REBLOG", "Undo Reblogged post")
                // Button is inactive
                undoReblogPost(holder, api, credential, this)
            }
        }
    }

    fun activateLiker(
        holder : PostViewHolder,
        api: PixelfedAPI,
        credential: String,
        isLiked: Boolean
    ) {
        //Set initial state
        holder.liker.isChecked = isLiked

        //Activate the liker
        holder.liker.setEventListener { _, buttonState ->
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
        holder : PostViewHolder,
        api: PixelfedAPI,
        credential: String
    ) {
        //Show all comments of a post
        if (replies_count == 0) {
            holder.viewComment.text =  "No comments on this post..."
        } else {
            holder.viewComment.text =  "View all $replies_count comments..."
            holder.viewComment.setOnClickListener {
                holder.viewComment.visibility = View.GONE

                //Retrieve the comments
                retrieveComments(holder, api, credential, this)
            }
        }
    }

    fun activateCommenter(
        holder : PostViewHolder,
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

    enum class Visibility : Serializable {
        public, unlisted, private, direct
    }
}