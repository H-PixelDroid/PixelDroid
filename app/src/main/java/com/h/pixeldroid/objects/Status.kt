package com.h.pixeldroid.objects

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.RequestBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.R
import com.h.pixeldroid.ReportActivity
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.ImageFragment
import com.h.pixeldroid.fragments.feeds.postFeeds.PostViewHolder
import com.h.pixeldroid.utils.HtmlUtils.Companion.getDomain
import com.h.pixeldroid.utils.HtmlUtils.Companion.parseHTMLText
import com.h.pixeldroid.utils.ImageConverter
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
import java.io.File
import java.io.Serializable
import java.util.*
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

    private fun getStatusDomain(domain: String) : String {
        val accountDomain = getDomain(account!!.url)
        return if(getDomain(domain) == accountDomain) ""
        else " from $accountDomain"

    }

    private fun setupPostPics(
        rootView: View,
        request: RequestBuilder<Drawable>,
        homeFragment: Fragment
    ) {

        // Standard layout
        rootView.postPicture.visibility = VISIBLE
        rootView.postPager.visibility = GONE
        rootView.postTabs.visibility = GONE


        if(media_attachments?.size == 1) {
            request.load(this.getPostUrl()).into(rootView.postPicture)
            val imgDescription = media_attachments[0].description.orEmpty().ifEmpty { rootView.context.getString(R.string.no_description) }
            rootView.postPicture.contentDescription = imgDescription

            rootView.postPicture.setOnLongClickListener {
                Snackbar.make(it, imgDescription, Snackbar.LENGTH_SHORT).show()
                true
            }

        } else if(media_attachments?.size!! > 1) {
            setupTabsLayout(rootView, request, homeFragment)
        }

        if (sensitive!!) {
            setupSensitiveLayout(rootView)
        }
    }

    private fun setupSensitiveLayout(view: View) {

        // Set dark layout and warning message
        view.sensitiveWarning.visibility = VISIBLE
        view.postPicture.colorFilter = ColorMatrixColorFilter(censorColorMatrix())

        fun uncensorPicture(view: View) {
                view.sensitiveWarning.visibility = GONE
                view.postPicture.colorFilter = ColorMatrixColorFilter(uncensorColorMatrix())
        }


        view.findViewById<TextView>(R.id.sensitiveWarning).setOnClickListener {
            uncensorPicture(view)
        }

        view.findViewById<ImageView>(R.id.postPicture).setOnClickListener {
            uncensorPicture(view)
        }
    }

    private fun setupTabsLayout(
        rootView: View,
        request: RequestBuilder<Drawable>,
        homeFragment: Fragment
    ) {
        //Only show the viewPager and tabs
        rootView.postPicture.visibility = GONE
        rootView.postPager.visibility = VISIBLE
        rootView.postTabs.visibility = VISIBLE

        val tabs : ArrayList<ImageFragment> = ArrayList()

        //Fill the tabs with each mediaAttachment
        for(media in media_attachments!!) {
            tabs.add(ImageFragment.newInstance(media.url!!, media.description.orEmpty()))
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
        domain: String,
        isActivity: Boolean
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

    fun setDescription(rootView: View, api: PixelfedAPI, credential: String) {
        rootView.findViewById<TextView>(R.id.description).apply {
            if (content.isNullOrBlank()) {
                visibility = GONE
            } else {
                text = parseHTMLText(content, mentions, api, rootView.context, credential)
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    fun activateButtons(holder: PostViewHolder, api: PixelfedAPI, credential: String){

        //Set the special HTML text
        setDescription(holder.postView, api, credential)

        //Activate onclickListeners
        activateLiker(
            holder, api, credential,
            this.favourited ?: false
        )
        activateReblogger(
            holder, api, credential,
            this.reblogged ?: false
        )
        activateCommenter(holder, api, credential)

        showComments(holder, api, credential)

        //Activate double tap liking
        activateDoubleTapLiker(holder, api, credential)

        activateMoreButton(holder)
    }

    fun activateReblogger(
        holder: PostViewHolder,
        api: PixelfedAPI,
        credential: String,
        isReblogged: Boolean
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

    fun downloadImage(context: Context, url: String, view: View, share: Boolean = false) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(url)

        val title = url.substringAfterLast("/")
        val request = DownloadManager.Request(downloadUri).apply {
            setTitle(title)
            if(!share) {
                val directory = File(Environment.DIRECTORY_PICTURES)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                setDestinationInExternalPublicDir(directory.toString(), title)
            }
        }
        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)

        Thread {

            var msg = ""
            var lastMsg = ""
            var downloading = true

            while (downloading) {
                val cursor: Cursor = downloadManager.query(query)
                cursor.moveToFirst()
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    == DownloadManager.STATUS_SUCCESSFUL
                ) {
                    downloading = false
                }
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                if (!share) {
                    msg = when (status) {
                        DownloadManager.STATUS_FAILED ->
                            context.getString(R.string.image_download_failed)
                        DownloadManager.STATUS_RUNNING ->
                            context.getString(R.string.image_download_downloading)
                        DownloadManager.STATUS_SUCCESSFUL ->
                            context.getString(R.string.image_download_success)
                        else -> ""
                    }
                    if (msg != lastMsg && msg != "") {
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                        lastMsg = msg
                    }
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {

                    val ext = url.substringAfterLast(".", "*")

                    val path = cursor.getString(
                        cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    )
                    val file = path.toUri()

                    val shareIntent: Intent = Intent.createChooser(Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, file)
                        data = file
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        type = "image/$ext"
                    }, null)

                    context.startActivity(shareIntent)
                }
                cursor.close()
            }
        }.start()
    }

    fun activateMoreButton(holder: PostViewHolder){
        holder.more.setOnClickListener {
            PopupMenu(it.context, it).apply {
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.post_more_menu_report -> {
                            val intent = Intent(it.context, ReportActivity::class.java)
                            intent.putExtra(POST_TAG, this@Status)
                            startActivity(it.context, intent, null)
                            true
                        }
                        R.id.post_more_menu_share_link -> {
                            val share = Intent.createChooser(Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, uri)

                                type = "text/plain"

                                putExtra(Intent.EXTRA_TITLE, content)
                            }, null)
                            startActivity(it.context, share, null)

                            true
                        }
                        R.id.post_more_menu_save_to_gallery -> {
                            Dexter.withContext(holder.context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object : BasePermissionListener() {
                                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                        Toast.makeText(
                                            holder.context,
                                            holder.context.getString(R.string.write_permission_download_pic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                        downloadImage(
                                            holder.context,
                                            media_attachments?.get(holder.postPager.currentItem)?.url
                                                ?: "",
                                            holder.postView
                                        )
                                    }
                                }).check()
                            true
                        }
                        R.id.post_more_menu_share_picture -> {
                            Dexter.withContext(holder.context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object : BasePermissionListener() {
                                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                        Toast.makeText(
                                            holder.context,
                                            holder.context.getString(R.string.write_permission_share_pic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                        downloadImage(
                                            holder.context,
                                            media_attachments?.get(holder.postPager.currentItem)?.url
                                                ?: "",
                                            holder.postView,
                                            share = true,
                                        )
                                    }
                                }).check()
                            true
                        }
                        else -> false
                    }
                }
                inflate(R.menu.post_more_menu)
                if(media_attachments.isNullOrEmpty()) {
                    //make sure to disable image-related things if there aren't any
                    menu.setGroupVisible(R.id.post_more_group_picture, false)
                }
                show()
            }
        }
    }


    fun activateDoubleTapLiker(
        holder: PostViewHolder,
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
        holder: PostViewHolder,
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
        holder: PostViewHolder,
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
        holder: PostViewHolder,
        api: PixelfedAPI,
        credential: String
    ) {
        //Toggle comment button
        toggleCommentInput(holder)

        //Activate commenterpostPicture
        holder.submitCmnt.setOnClickListener {
            val textIn = holder.comment.text
            //Open text input
            if(textIn.isNullOrEmpty()) {
                Toast.makeText(
                    holder.context,
                    holder.context.getString(R.string.empty_comment),
                    Toast.LENGTH_SHORT
                ).show()
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