package com.h.pixeldroid.posts

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import at.connyduck.sparkbutton.SparkButton
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Attachment
import com.h.pixeldroid.utils.api.objects.Context
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.db.AppDatabase
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import kotlinx.android.synthetic.main.comment.view.*
import kotlinx.android.synthetic.main.post_fragment.view.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException


/**
 * View Holder for a [Status] RecyclerView list item.
 */
class StatusViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val profilePic  : ImageView = view.findViewById(R.id.profilePic)
    val postPic     : ImageView = view.findViewById(R.id.postPicture)
    val username    : TextView = view.findViewById(R.id.username)
    val usernameDesc: TextView = view.findViewById(R.id.usernameDesc)
    val description : TextView = view.findViewById(R.id.description)
    val nlikes      : TextView = view.findViewById(R.id.nlikes)
    val nshares     : TextView = view.findViewById(R.id.nshares)

    //Spark buttons
    val liker       : SparkButton = view.findViewById(R.id.liker)
    val reblogger   : SparkButton = view.findViewById(R.id.reblogger)

    val submitCmnt  : ImageButton = view.findViewById(R.id.submitComment)
    val commenter   : ImageView = view.findViewById(R.id.commenter)
    val comment     : EditText = view.findViewById(R.id.editComment)
    val commentCont : LinearLayout = view.findViewById(R.id.commentContainer)
    val commentIn   : LinearLayout = view.findViewById(R.id.commentIn)
    val viewComment : TextView = view.findViewById(R.id.ViewComments)
    val postDate    : TextView = view.findViewById(R.id.postDate)
    val postDomain  : TextView = view.findViewById(R.id.postDomain)
    val sensitiveW  : TextView = view.findViewById(R.id.sensitiveWarning)
    val postPager  : ViewPager2 = view.findViewById(R.id.postPager)
    val more        : ImageButton = view.findViewById(R.id.status_more)

    private var status: Status? = null

    fun bind(status: Status?, pixelfedAPI: PixelfedAPI, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope) {

        this.itemView.visibility = View.VISIBLE
        this.status = status

        val metrics = itemView.context.resources.displayMetrics
        //Limit the height of the different images
        postPic.maxHeight = metrics.heightPixels * 3/4

        //Setup the post layout
        val picRequest = Glide.with(itemView)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        val user = db.userDao().getActiveUser()!!

        setupPost(itemView, picRequest, user.instance_uri, false)

        activateButtons(this, pixelfedAPI, db, lifecycleScope)

    }

    private fun setupPost(
        rootView: View,
        request: RequestBuilder<Drawable>,
        //homeFragment: Fragment,
        domain: String,
        isActivity: Boolean
    ) {
        //Setup username as a button that opens the profile
        rootView.findViewById<TextView>(R.id.username).apply {
            text = status?.account?.getDisplayName() ?: ""
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { status?.account?.openProfile(rootView.context) }
        }

        rootView.findViewById<TextView>(R.id.usernameDesc).apply {
            text = status?.account?.getDisplayName() ?: ""
            setTypeface(null, Typeface.BOLD)
        }

        rootView.findViewById<TextView>(R.id.nlikes).apply {
            text = status?.getNLikes(rootView.context)
            setTypeface(null, Typeface.BOLD)
        }

        rootView.findViewById<TextView>(R.id.nshares).apply {
            text = status?.getNShares(rootView.context)
            setTypeface(null, Typeface.BOLD)
        }

        //Convert the date to a readable string
        setTextViewFromISO8601(
            status?.created_at!!,
            rootView.postDate,
            isActivity,
            rootView.context
        )

        rootView.postDomain.text = status?.getStatusDomain(domain)

        //Setup images
        ImageConverter.setRoundImageFromURL(
            rootView,
            status?.getProfilePicUrl(),
            rootView.profilePic
        )
        rootView.profilePic.setOnClickListener { status?.account?.openProfile(rootView.context) }

        //Setup post pic only if there are media attachments
        if(!status?.media_attachments.isNullOrEmpty()) {
            setupPostPics(rootView, request)
        } else {
            rootView.postPicture.visibility = View.GONE
            rootView.postPager.visibility = View.GONE
            rootView.postTabs.visibility = View.GONE
        }


        //Set comment initial visibility
        rootView.findViewById<LinearLayout>(R.id.commentIn).visibility = View.GONE
        rootView.findViewById<LinearLayout>(R.id.commentContainer).visibility = View.GONE
    }

    private fun setupPostPics(
        rootView: View,
        request: RequestBuilder<Drawable>,
        //homeFragment: Fragment
    ) {

        // Standard layout
        rootView.postPicture.visibility = View.VISIBLE
        rootView.postPager.visibility = View.GONE
        rootView.postTabs.visibility = View.GONE


        if(status?.media_attachments?.size == 1) {
            request.load(status?.getPostUrl()).into(rootView.postPicture)
            val imgDescription = status?.media_attachments?.get(0)?.description.orEmpty().ifEmpty { rootView.context.getString(
                R.string.no_description) }
            rootView.postPicture.contentDescription = imgDescription

            rootView.postPicture.setOnLongClickListener {
                Snackbar.make(it, imgDescription, Snackbar.LENGTH_SHORT).show()
                true
            }

        } else if(status?.media_attachments?.size!! > 1) {
            setupTabsLayout(rootView, request)
        }

        if (status?.sensitive!!) {
            status?.setupSensitiveLayout(rootView)
        }
    }

    private fun setupTabsLayout(
        rootView: View,
        request: RequestBuilder<Drawable>,
    ) {
        //Only show the viewPager and tabs
        rootView.postPicture.visibility = View.GONE
        rootView.postPager.visibility = View.VISIBLE
        rootView.postTabs.visibility = View.VISIBLE

        //Attach the given tabs to the view pager
        rootView.postPager.adapter = AlbumViewPagerAdapter(status?.media_attachments ?: emptyList())

        TabLayoutMediator(rootView.postTabs, rootView.postPager) { tab, _ ->
            tab.icon = ContextCompat.getDrawable(rootView.context, R.drawable.ic_dot_blue_12dp)
        }.attach()
    }

    private fun setDescription(
        rootView: View,
        api: PixelfedAPI,
        credential: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        rootView.findViewById<TextView>(R.id.description).apply {
            if (status?.content.isNullOrBlank()) {
                visibility = View.GONE
            } else {
                text = parseHTMLText(
                    status?.content.orEmpty(),
                    status?.mentions,
                    api,
                    rootView.context,
                    credential,
                    lifecycleScope
                )
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    private fun activateButtons(holder: StatusViewHolder, api: PixelfedAPI, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope){
        val user = db.userDao().getActiveUser()!!

        val credential = "Bearer ${user.accessToken}"
        //Set the special HTML text
        setDescription(holder.view, api, credential, lifecycleScope)

        //Activate onclickListeners
        activateLiker(
            holder, api, credential,
            status?.favourited ?: false,
                lifecycleScope
        )
        activateReblogger(
            holder, api, credential,
            status?.reblogged ?: false,
                lifecycleScope
        )
        activateCommenter(holder, api, credential, lifecycleScope)

        showComments(holder, api, credential, lifecycleScope)

        activateMoreButton(holder, api, db, lifecycleScope)
    }

    private fun activateReblogger(
            holder: StatusViewHolder,
            api: PixelfedAPI,
            credential: String,
            isReblogged: Boolean,
            lifecycleScope: LifecycleCoroutineScope
    ) {
        holder.reblogger.apply {
            //Set initial button state
            isChecked = isReblogged

            //Activate the button
            setEventListener { _, buttonState ->
                lifecycleScope.launchWhenCreated {
                    if (buttonState) {
                        // Button is active
                        undoReblogPost(holder, api, credential)
                    } else {
                        // Button is inactive
                        reblogPost(holder, api, credential)
                    }
                }
                //show animation or not?
                true
            }
        }
    }

    private suspend fun reblogPost(
        holder : StatusViewHolder,
        api: PixelfedAPI,
        credential: String
    ) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.reblogStatus(credential, it)

                //Update shown share count
                holder.nshares.text = resp.getNShares(holder.view.context)
                holder.reblogger.isChecked = resp.reblogged!!
            } catch (exception: IOException) {
                Log.e("REBLOG ERROR", exception.toString())
                holder.reblogger.isChecked = false
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                holder.reblogger.isChecked = false
            }
        }
    }

    private suspend fun undoReblogPost(
        holder : StatusViewHolder,
        api: PixelfedAPI,
        credential: String,
    ) {
        //Call the api function
        status?.id?.let {
            try {
                val resp = api.undoReblogStatus(credential, it)

                //Update shown share count
                holder.nshares.text = resp.getNShares(holder.view.context)
                holder.reblogger.isChecked = resp.reblogged!!
            } catch (exception: IOException) {
                Log.e("REBLOG ERROR", exception.toString())
                holder.reblogger.isChecked = true
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                holder.reblogger.isChecked = true
            }
        }
    }

    private fun activateMoreButton(holder: StatusViewHolder, api: PixelfedAPI, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope){
        holder.more.setOnClickListener {
            PopupMenu(it.context, it).apply {
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.post_more_menu_report -> {
                            val intent = Intent(it.context, ReportActivity::class.java)
                            intent.putExtra(Status.POST_TAG, status)
                            ContextCompat.startActivity(it.context, intent, null)
                            true
                        }
                        R.id.post_more_menu_share_link -> {
                            val share = Intent.createChooser(Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, status?.uri)

                                type = "text/plain"

                                putExtra(Intent.EXTRA_TITLE, status?.content)
                            }, null)
                            ContextCompat.startActivity(it.context, share, null)

                            true
                        }
                        R.id.post_more_menu_save_to_gallery -> {
                            Dexter.withContext(holder.view.context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object : BasePermissionListener() {
                                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                        Toast.makeText(
                                            holder.view.context,
                                            holder.view.context.getString(R.string.write_permission_download_pic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                        status?.downloadImage(
                                            holder.view.context,
                                            status?.media_attachments?.get(holder.postPager.currentItem)?.url
                                                ?: "",
                                            holder.view
                                        )
                                    }
                                }).check()
                            true
                        }
                        R.id.post_more_menu_share_picture -> {
                            Dexter.withContext(holder.view.context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object : BasePermissionListener() {
                                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                        Toast.makeText(
                                            holder.view.context,
                                            holder.view.context.getString(R.string.write_permission_share_pic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                        status?.downloadImage(
                                            holder.view.context,
                                            status?.media_attachments?.get(holder.postPager.currentItem)?.url
                                                ?: "",
                                            holder.view,
                                            share = true,
                                        )
                                    }
                                }).check()
                            true
                        }
                        R.id.post_more_menu_delete -> {
                            val builder = AlertDialog.Builder(holder.itemView.context)
                            builder.apply {
                                setMessage(R.string.delete_dialog)
                                setPositiveButton(R.string.OK) { _, _ ->

                                    lifecycleScope.launch {
                                        val user = db.userDao().getActiveUser()!!
                                        status?.id?.let { id ->
                                            db.homePostDao().delete(id, user.user_id, user.instance_uri)
                                            db.publicPostDao().delete(id, user.user_id, user.instance_uri)
                                            try {
                                                api.deleteStatus("Bearer ${user.accessToken}", id)
                                                holder.itemView.visibility = View.GONE
                                            } catch (exception: IOException) {
                                            } catch (exception: HttpException) {
                                            }
                                        }
                                    }
                                }
                                setNegativeButton(R.string.cancel) { _, _ -> }
                                show()
                            }

                            true
                        }
                        else -> false
                    }
                }
                inflate(R.menu.post_more_menu)
                if(status?.media_attachments.isNullOrEmpty()) {
                    //make sure to disable image-related things if there aren't any
                    menu.setGroupVisible(R.id.post_more_group_picture, false)
                }
                if(status?.account?.id == db.userDao().getActiveUser()!!.user_id){
                    // Enable deleting post if it's the user's
                    menu.setGroupVisible(R.id.post_more_menu_group_delete, true)
                    // And disable reporting your own post (just delete it if you don't like it :P)
                    menu.setGroupVisible(R.id.post_more_menu_group_report, false)
                }
                show()
            }
        }
    }

    private fun activateLiker(
            holder: StatusViewHolder,
            api: PixelfedAPI,
            credential: String,
            isLiked: Boolean,
            lifecycleScope: LifecycleCoroutineScope
    ) {

        holder.liker.apply {
            //Set initial state
            isChecked = isLiked

            //Activate the liker
            setEventListener { _, buttonState ->
                lifecycleScope.launchWhenCreated {
                    if (buttonState) {
                        // Button is active, unlike
                        unLikePostCall(holder, api, credential)
                    } else {
                        // Button is inactive, like
                        likePostCall(holder, api, credential)
                    }
                }
                //show animation or not?
                true
            }
        }

        //Activate double tap liking
        holder.apply {
            var clicked = false
            postPic.setOnClickListener {
                lifecycleScope.launchWhenCreated {
                    //Check that the post isn't hidden
                    if(sensitiveW.visibility == View.GONE) {
                        //Check for double click
                        if(clicked) {
                            if (holder.liker.isChecked) {
                                // Button is active, unlike
                                holder.liker.isChecked = false
                                unLikePostCall(holder, api, credential)
                            } else {
                                // Button is inactive, like
                                holder.liker.playAnimation()
                                holder.liker.isChecked = true
                                likePostCall(holder, api, credential)
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
    }

    private suspend fun likePostCall(
        holder : StatusViewHolder,
        api: PixelfedAPI,
        credential: String,
    ) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.likePost(credential, it)

                //Update shown like count and internal like toggle
                holder.nlikes.text = resp.getNLikes(holder.view.context)
                holder.liker.isChecked = resp.favourited ?: false
            } catch (exception: IOException) {
                Log.e("LIKE ERROR", exception.toString())
                holder.liker.isChecked = false
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                holder.liker.isChecked = false
            }
        }
    }

    private suspend fun unLikePostCall(
        holder : StatusViewHolder,
        api: PixelfedAPI,
        credential: String,
    ) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.unlikePost(credential, it)

                //Update shown like count and internal like toggle
                holder.nlikes.text = resp.getNLikes(holder.view.context)
                holder.liker.isChecked = resp.favourited ?: false
            } catch (exception: IOException) {
                Log.e("UNLIKE ERROR", exception.toString())
                holder.liker.isChecked = true
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                holder.liker.isChecked = true
            }
        }
    }

    private fun showComments(
        holder: StatusViewHolder,
        api: PixelfedAPI,
        credential: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        //Show all comments of a post
        if (status?.replies_count == 0) {
            holder.viewComment.text =  holder.view.context.getString(R.string.NoCommentsToShow)
        } else {
            holder.viewComment.apply {
                text = holder.view.context.getString(R.string.number_comments)
                    .format(status?.replies_count)
                setOnClickListener {
                    visibility = View.GONE

                    lifecycleScope.launchWhenCreated {
                        //Retrieve the comments
                        retrieveComments(holder, api, credential)
                    }
                }
            }
        }
    }

    private fun activateCommenter(
        holder: StatusViewHolder,
        api: PixelfedAPI,
        credential: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        //Toggle comment button
        toggleCommentInput(holder)

        //Activate commenterpostPicture
        holder.submitCmnt.setOnClickListener {
            val textIn = holder.comment.text
            //Open text input
            if(textIn.isNullOrEmpty()) {
                Toast.makeText(
                    holder.view.context,
                    holder.view.context.getString(R.string.empty_comment),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //Post the comment
                lifecycleScope.launchWhenCreated {
                    postComment(holder, api, credential)
                }
            }
        }
    }

    private fun toggleCommentInput(
        holder : StatusViewHolder
    ) {
        //Toggle comment button
        holder.commenter.setOnClickListener {
            when(holder.commentIn.visibility) {
                View.VISIBLE -> {
                    holder.commentIn.visibility = View.GONE
                    ImageConverter.setImageFromDrawable(
                        holder.view,
                        holder.commenter,
                        R.drawable.ic_comment_empty
                    )
                }
                View.GONE -> {
                    holder.commentIn.visibility = View.VISIBLE
                    ImageConverter.setImageFromDrawable(
                        holder.view,
                        holder.commenter,
                        R.drawable.ic_comment_blue
                    )
                }
            }
        }
    }

    fun addComment(context: android.content.Context, commentContainer: LinearLayout, commentUsername: String, commentContent: String) {

        val view = LayoutInflater.from(context)
            .inflate(R.layout.comment, commentContainer, true)

        view.user.text = commentUsername
        view.commentText.text = commentContent
    }

    private suspend fun retrieveComments(
            holder: StatusViewHolder,
            api: PixelfedAPI,
            credential: String,
    ) {
        status?.id?.let {
            try {
                val statuses = api.statusComments(it, credential).descendants

                holder.commentCont.removeAllViews()

                //Create the new views for each comment
                for (status in statuses) {
                    addComment(holder.view.context, holder.commentCont, status.account!!.username!!,
                            status.content!!
                    )
                }
                holder.commentCont.visibility = View.VISIBLE

            } catch (exception: IOException) {
                Log.e("COMMENT FETCH ERROR", exception.toString())
            } catch (exception: HttpException) {
                Log.e("COMMENT ERROR", "${exception.code()} with body ${exception.response()?.errorBody()}")
            }
        }
    }

    private suspend fun postComment(
        holder : StatusViewHolder,
        api: PixelfedAPI,
        credential: String,
    ) {
        val textIn = holder.comment.text
        val nonNullText = textIn.toString()
        status?.id?.let {
            try {
                val response = api.postStatus(credential, nonNullText, it)
                holder.commentIn.visibility = View.GONE

                //Add the comment to the comment section
                addComment(
                    holder.view.context, holder.commentCont, response.account!!.username!!,
                    response.content!!
                )

                Toast.makeText(
                    holder.view.context,
                    holder.view.context.getString(R.string.comment_posted).format(textIn),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: IOException) {
                Log.e("COMMENT ERROR", exception.toString())
                Toast.makeText(
                    holder.view.context, holder.view.context.getString(R.string.comment_error),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: HttpException) {
                Toast.makeText(
                    holder.view.context, holder.view.context.getString(R.string.comment_error),
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ERROR_CODE", exception.code().toString())
            }
        }
    }


    companion object {
        fun create(parent: ViewGroup): StatusViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.post_fragment, parent, false)
            return StatusViewHolder(view)
        }
    }
}

class AlbumViewPagerAdapter(private val media_attachments: List<Attachment>) : RecyclerView.Adapter<AlbumViewPagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.album_image_view, parent, false))

    override fun getItemCount() = media_attachments.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.view)
            .asDrawable().fitCenter().placeholder(ColorDrawable(Color.GRAY))
            .load(media_attachments[position].url).into(holder.image)

        val description = media_attachments[position].description
            .orEmpty().ifEmpty{ holder.view.context.getString(R.string.no_description)}

        holder.image.setOnLongClickListener {
            Snackbar.make(it, description, Snackbar.LENGTH_SHORT).show()
            true
        }

        holder.image.contentDescription = description
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view){
        val image: ImageView = view.findViewById(R.id.imageImageView)
    }
}