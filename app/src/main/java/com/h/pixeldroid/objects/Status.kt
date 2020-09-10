package com.h.pixeldroid.objects

import android.Manifest
import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.RequestBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.ImageFragment
import com.h.pixeldroid.fragments.feeds.postFeeds.PostViewHolder
import com.h.pixeldroid.utils.HtmlUtils.Companion.getDomain
import com.h.pixeldroid.utils.HtmlUtils.Companion.parseHTMLText
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.ImageUtils.Companion.downloadImage
import com.h.pixeldroid.utils.PostUtils.Companion.censorColorMatrix
import com.h.pixeldroid.utils.PostUtils.Companion.likePostCall
import com.h.pixeldroid.utils.PostUtils.Companion.postComment
import com.h.pixeldroid.utils.PostUtils.Companion.reblogPost
import com.h.pixeldroid.utils.PostUtils.Companion.retrieveComments
import com.h.pixeldroid.utils.PostUtils.Companion.toggleCommentInput
import com.h.pixeldroid.utils.PostUtils.Companion.unLikePostCall
import com.h.pixeldroid.utils.PostUtils.Companion.uncensorColorMatrix
import com.h.pixeldroid.utils.PostUtils.Companion.undoReblogPost
import com.h.pixeldroid.utils.Utils.Companion.setTextViewFromISO8601
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import kotlinx.android.synthetic.main.post_fragment.view.*
import java.io.Serializable
import java.util.Date
import kotlin.collections.ArrayList

/*
Represents a status posted by an account.
https://docs.joinmastodon.org/entities/status/
 */
data class Status(
    //Base attributes
    override val id: String?,
    val uri: String? = "",
    val created_at: Date? = Date(0), //ISO 8601 Datetime
    val account: Account?,
    val content: String? = "", //HTML
    val visibility: Visibility? = Visibility.public,
    val sensitive: Boolean? = false,
    val spoiler_text: String? = "",
    val media_attachments: List<Attachment>? = null,
    val application: Application? = null,
    //Rendering attributes
    val mentions: List<Mention>? = null,
    val tags: List<Tag>? = null,
    val emojis: List<Emoji>? = null,
    //Informational attributes
    val reblogs_count: Int? = 0,
    val favourites_count: Int? = 0,
    val replies_count: Int? = 0,
    //Nullable attributes
    val url: String? = null, //URL
    val in_reply_to_id: String? = null,
    val in_reply_to_account: String? = null,
    val reblog: Status? = null,
    val poll: Poll? = null,
    val card: Card? = null,
    val language: String? = null, //ISO 639 Part 1 two-letter language code
    val text: String? = null,
    //Authorized user attributes
    val favourited: Boolean? = false,
    val reblogged: Boolean? = false,
    val muted: Boolean? = false,
    val bookmarked: Boolean? = false,
    val pinned: Boolean? = false
) : Serializable, FeedContent()
{

    companion object {
        const val POST_TAG = "postTag"
        const val DOMAIN_TAG = "domainTag"
        const val DISCOVER_TAG = "discoverTag"
    }

    fun getPostUrl() : String? = media_attachments?.firstOrNull()?.url
    fun getProfilePicUrl() : String? = account?.avatar
    fun getPostPreviewURL() : String? = media_attachments?.firstOrNull()?.preview_url

    /**
     * @brief returns the parsed version of the HTML description
     */
    private fun getDescription(api: PixelfedAPI, context: Context, credential: String) : Spanned =
        parseHTMLText(content ?: "", mentions, api, context, credential)

    fun getUsername() : CharSequence = when {
        account?.username.isNullOrBlank() && account?.display_name.isNullOrBlank() -> "No Name"
        account!!.username.isNullOrBlank() -> account.display_name as CharSequence
        else -> account.username as CharSequence
    }

    fun getNLikes(context: Context) : CharSequence {
        return context.getString(R.string.likes).format(favourites_count.toString())
    }

    fun getNShares(context: Context) : CharSequence {
        return context.getString(R.string.shares).format(reblogs_count.toString())
    }

    private fun getStatusDomain(domain : String) : String {
        val accountDomain = getDomain(account!!.url)
        return if(getDomain(domain) == accountDomain) ""
        else " from $accountDomain"

    }

    private fun setupPostPics(rootView: View, request: RequestBuilder<Drawable>, homeFragment: Fragment) {

        // Standard layout
        rootView.postPicture.visibility = VISIBLE
        rootView.postPager.visibility = GONE
        rootView.postTabs.visibility = GONE

        if (sensitive!!) {
            setupSensitiveLayout(rootView, request, homeFragment)
            request.load(this.getPostUrl()).into(rootView.postPicture)

        } else {
            rootView.sensitiveWarning.visibility = GONE

            if(media_attachments?.size == 1) {
                request.load(this.getPostUrl()).into(rootView.postPicture)

            } else if(media_attachments?.size!! > 1) {
                setupTabsLayout(rootView, request, homeFragment)
            }

            imagePopUpMenu(rootView, homeFragment.requireActivity())
        }
    }

    private fun setupTabsLayout(rootView: View, request: RequestBuilder<Drawable>, homeFragment: Fragment) {
        //Only show the viewPager and tabs
        rootView.postPicture.visibility = GONE
        rootView.postPager.visibility = VISIBLE
        rootView.postTabs.visibility = VISIBLE

        val tabs : ArrayList<ImageFragment> = ArrayList()

        //Fill the tabs with each mediaAttachment
        for(media in media_attachments!!) {
            tabs.add(ImageFragment.newInstance(media.url!!))
        }

        setupTabs(tabs, rootView, homeFragment)
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
        domain : String,
        isActivity : Boolean
    ) {
        //Setup username as a button that opens the profile
        rootView.findViewById<TextView>(R.id.username).apply {
            text = this@Status.getUsername()
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { account?.openProfile(rootView.context) }
        }

        rootView.findViewById<TextView>(R.id.usernameDesc).apply {
            text = this@Status.getUsername()
            setTypeface(null, Typeface.BOLD)
        }

        rootView.findViewById<TextView>(R.id.nlikes).apply {
            text = this@Status.getNLikes(rootView.context)
            setTypeface(null, Typeface.BOLD)
        }

        rootView.findViewById<TextView>(R.id.nshares).apply {
            text = this@Status.getNShares(rootView.context)
            setTypeface(null, Typeface.BOLD)
        }

        //Convert the date to a readable string
        setTextViewFromISO8601(created_at!!, rootView.postDate, isActivity, rootView.context)

        rootView.postDomain.text = getStatusDomain(domain)

        //Setup images
        ImageConverter.setRoundImageFromURL(
            rootView,
            this.getProfilePicUrl(),
            rootView.profilePic
        )
        rootView.profilePic.setOnClickListener { account?.openProfile(rootView.context) }

        //Setup post pic only if there are media attachments
        if(!media_attachments.isNullOrEmpty()) {
            setupPostPics(rootView, request, homeFragment)
        }


        //Set comment initial visibility
        rootView.findViewById<LinearLayout>(R.id.commentIn).visibility = GONE
    }

    fun setDescription(rootView: View, api : PixelfedAPI, credential: String) {
        val desc = rootView.findViewById<TextView>(R.id.description)

        desc.apply {
            if (content.isNullOrBlank()) {
                visibility = GONE
            } else {
                text = parseHTMLText(content, mentions, api, rootView.context, credential)
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    fun activateReblogger(
        holder : PostViewHolder,
        api : PixelfedAPI,
        credential: String,
        isReblogged : Boolean
    ) {
        holder.reblogger.apply {
            //Set initial button state
            isChecked = isReblogged

            //Activate the button
            setEventListener { _, buttonState ->
                if (buttonState) {
                    // Button is active
                    undoReblogPost(holder, api, credential, this@Status)
                } else {
                    // Button is inactive
                    reblogPost(holder, api, credential, this@Status)
                }
                //show animation or not?
                true
            }
        }
    }

    fun activateDoubleTapLiker(
        holder : PostViewHolder,
        api: PixelfedAPI,
        credential: String
    ) {
        holder.apply {
            var clicked = false
            postPic.setOnClickListener {
                //Check that the post isn't hidden
                if(sensitiveW.visibility == GONE) {
                    //Check for double click
                    if(clicked) {
                        if (holder.liker.isChecked) {
                            // Button is active, unlike
                            holder.liker.isChecked = false
                            unLikePostCall(holder, api, credential, this@Status)
                        } else {
                            // Button is inactive, like
                            holder.liker.playAnimation()
                            holder.liker.isChecked = true
                            likePostCall(holder, api, credential, this@Status)
                        }
                    } else {
                        clicked = true

                        //Reset clicked to false after 500ms
                        postPic.handler.postDelayed(fun() { clicked = false }, 500)
                    }
                }
            }
        }
    }

    fun activateLiker(
        holder : PostViewHolder,
        api: PixelfedAPI,
        credential: String,
        isLiked: Boolean
    ) {

        holder.liker.apply {
            //Set initial state
            isChecked = isLiked

            //Activate the liker
            setEventListener { _, buttonState ->
                if (buttonState) {
                    // Button is active, unlike
                    unLikePostCall(holder, api, credential, this@Status)
                } else {
                    // Button is inactive, like
                    likePostCall(holder, api, credential, this@Status)
                }
            //show animation or not?
            true
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
            holder.viewComment.text =  holder.context.getString(R.string.NoCommentsToShow)
        } else {
            holder.viewComment.apply {
                text = "$replies_count ${holder.context.getString(R.string.CommentDisplay)}"
                setOnClickListener {
                    visibility = GONE

                    //Retrieve the comments
                    retrieveComments(holder, api, credential, this@Status)
                }
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
                Toast.makeText(holder.context, holder.context.getString(R.string.empty_comment), Toast.LENGTH_SHORT).show()
            } else {

                //Post the comment
                postComment(holder, api, credential, this)
            }
        }
    }

    enum class Visibility : Serializable {
        public, unlisted, private, direct
    }


    private fun imagePopUpMenu(view: View, activity: FragmentActivity) {
        val anchor = view.findViewById<FrameLayout>(R.id.post_fragment_image_popup_menu_anchor)
        if (!media_attachments.isNullOrEmpty() && media_attachments.size == 1) {
            view.findViewById<ImageView>(R.id.postPicture).setOnLongClickListener {
                PopupMenu(view.context, anchor).apply {
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.image_popup_menu_save_to_gallery -> {
                                Dexter.withContext(view.context)
                                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .withListener(object: BasePermissionListener() {
                                        override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                            Toast.makeText(view.context, view.context.getString(R.string.write_permission_download_pic), Toast.LENGTH_SHORT).show()
                                        }

                                        override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                            downloadImage(activity, getPostUrl()!!)
                                        }
                                    }).check()
                                true
                            }
                            R.id.image_popup_menu_share_picture -> {
                                Dexter.withContext(view.context)
                                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .withListener(object: BasePermissionListener() {
                                        override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                            Toast.makeText(view.context, view.context.getString(R.string.write_permission_share_pic), Toast.LENGTH_SHORT).show()
                                        }

                                        override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                            downloadImage(activity, getPostUrl()!!, share = true)
                                        }
                                    }).check()
                                true
                            }
                            else -> false
                        }
                    }
                    inflate(R.menu.image_popup_menu)
                    show()
                }
                true
            }
        }
    }

    private fun setupSensitiveLayout(view: View, request: RequestBuilder<Drawable>, homeFragment: Fragment) {

        // Set dark layout and warning message
        view.sensitiveWarning.visibility = VISIBLE
        view.postPicture.colorFilter = ColorMatrixColorFilter(censorColorMatrix())

        fun uncensorPicture(view: View) {
            if (!media_attachments.isNullOrEmpty()) {
                view.sensitiveWarning.visibility = GONE
                view.postPicture.colorFilter = ColorMatrixColorFilter(uncensorColorMatrix())

                if (media_attachments.size > 1)
                    setupTabsLayout(view, request, homeFragment)
            }
            imagePopUpMenu(view, homeFragment.requireActivity())
        }


        view.findViewById<TextView>(R.id.sensitiveWarning).setOnClickListener {
            uncensorPicture(view)
        }

        view.findViewById<ImageView>(R.id.postPicture).setOnClickListener {
            uncensorPicture(view)
        }
    }
}