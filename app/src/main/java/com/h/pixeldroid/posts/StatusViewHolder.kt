package com.h.pixeldroid.posts

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
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
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.AlbumImageViewBinding
import com.h.pixeldroid.databinding.CommentBinding
import com.h.pixeldroid.databinding.PostFragmentBinding
import com.h.pixeldroid.utils.BlurHashDecoder
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Attachment
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.displayDimensionsInPx
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.roundToInt


/**
 * View Holder for a [Status] RecyclerView list item.
 */
class StatusViewHolder(val binding: PostFragmentBinding) : RecyclerView.ViewHolder(binding.root) {

    private var status: Status? = null

    fun bind(status: Status?, pixelfedAPI: PixelfedAPI, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope, displayDimensionsInPx: Pair<Int, Int>) {

        this.itemView.visibility = View.VISIBLE
        this.status = status

        val maxImageRatio: Float = status?.media_attachments?.map {
            if (it.meta?.original?.width == null || it.meta.original.height == null) {
                1f
            } else {
                it.meta.original.width.toFloat() / it.meta.original.height.toFloat()
            }
        }?.maxOrNull() ?: 1f

        val (displayWidth, displayHeight) = displayDimensionsInPx
        val height = if (displayWidth / maxImageRatio > displayHeight * 3/4f) {
            binding.postPicture.layoutParams.width = ((displayHeight * 3 / 4f) * maxImageRatio).roundToInt()
            displayHeight * 3 / 4f
        } else displayWidth / maxImageRatio

        binding.postPicture.layoutParams.height = height.toInt()

        //Setup the post layout
        val picRequest = Glide.with(itemView)
            .asDrawable().fitCenter()

        val user = db.userDao().getActiveUser()!!

        setupPost(picRequest, user.instance_uri, false)

        activateButtons(pixelfedAPI, db, lifecycleScope)

    }

    private fun setupPost(
        request: RequestBuilder<Drawable>,
        domain: String,
        isActivity: Boolean
    ) {
        //Setup username as a button that opens the profile
        binding.username.apply {
            text = status?.account?.getDisplayName() ?: ""
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { status?.account?.openProfile(binding.root.context) }
        }

        binding.usernameDesc.apply {
            text = status?.account?.getDisplayName() ?: ""
            setTypeface(null, Typeface.BOLD)
        }

        binding.nlikes.apply {
            text = status?.getNLikes(binding.root.context)
            setTypeface(null, Typeface.BOLD)
        }

        binding.nshares.apply {
            text = status?.getNShares(binding.root.context)
            setTypeface(null, Typeface.BOLD)
        }

        //Convert the date to a readable string
        setTextViewFromISO8601(
            status?.created_at!!,
            binding.postDate,
            isActivity,
            binding.root.context
        )

        binding.postDomain.text = status?.getStatusDomain(domain)

        //Setup images
        ImageConverter.setRoundImageFromURL(
            binding.root,
            status?.getProfilePicUrl(),
            binding.profilePic
        )
        binding.profilePic.setOnClickListener { status?.account?.openProfile(binding.root.context) }

        //Setup post pic only if there are media attachments
        if(!status?.media_attachments.isNullOrEmpty()) {
            setupPostPics(binding, request)
        } else {
            binding.postPicture.visibility = View.GONE
            binding.postPager.visibility = View.GONE
            binding.postTabs.visibility = View.GONE
        }


        //Set comment initial visibility
        binding.commentIn.visibility = View.GONE
        binding.commentContainer.visibility = View.GONE
    }

    private fun setupPostPics(
        binding: PostFragmentBinding,
        request: RequestBuilder<Drawable>,
    ) {

        // Standard layout
        binding.postPicture.visibility = View.VISIBLE
        binding.postPager.visibility = View.GONE
        binding.postTabs.visibility = View.GONE


        if(status?.media_attachments?.size == 1) {
            request.placeholder(
                    BlurHashDecoder.blurHashBitmap(binding.root.context.resources, status?.media_attachments?.get(0))
            ).load(status?.getPostUrl()).into(binding.postPicture)
            val imgDescription = status?.media_attachments?.get(0)?.description.orEmpty().ifEmpty { binding.root.context.getString(
                R.string.no_description) }
            binding.postPicture.contentDescription = imgDescription

            binding.postPicture.setOnLongClickListener {
                Snackbar.make(it, imgDescription, Snackbar.LENGTH_SHORT).show()
                true
            }

        } else if(status?.media_attachments?.size!! > 1) {
            setupTabsLayout(binding, request)
        }

        if (status?.sensitive!!) {
            status?.setupSensitiveLayout(binding)
        }
    }

    private fun setupTabsLayout(
        binding: PostFragmentBinding,
        request: RequestBuilder<Drawable>,
    ) {
        //Only show the viewPager and tabs
        binding.postPicture.visibility = View.GONE
        binding.postPager.visibility = View.VISIBLE
        binding.postTabs.visibility = View.VISIBLE

        //Attach the given tabs to the view pager
        binding.postPager.adapter = AlbumViewPagerAdapter(status?.media_attachments ?: emptyList())

        TabLayoutMediator(binding.postTabs, binding.postPager) { tab, _ ->
            tab.icon = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_dot_blue_12dp)
        }.attach()
    }

    private fun setDescription(
        api: PixelfedAPI,
        credential: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        binding.description.apply {
            if (status?.content.isNullOrBlank()) {
                visibility = View.GONE
            } else {
                text = parseHTMLText(
                    status?.content.orEmpty(),
                    status?.mentions,
                    api,
                    binding.root.context,
                    credential,
                    lifecycleScope
                )
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    private fun activateButtons(api: PixelfedAPI, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope){
        val user = db.userDao().getActiveUser()!!

        val credential = "Bearer ${user.accessToken}"
        //Set the special HTML text
        setDescription(api, credential, lifecycleScope)

        //Activate onclickListeners
        activateLiker(
            api, credential, status?.favourited ?: false,
            lifecycleScope
        )
        activateReblogger(
            api, credential, status?.reblogged ?: false,
            lifecycleScope
        )
        activateCommenter(api, credential, lifecycleScope)

        showComments(api, credential, lifecycleScope)

        activateMoreButton(api, db, lifecycleScope)
    }

    private fun activateReblogger(
            api: PixelfedAPI,
            credential: String,
            isReblogged: Boolean,
            lifecycleScope: LifecycleCoroutineScope
    ) {
        binding.reblogger.apply {
            //Set initial button state
            isChecked = isReblogged

            //Activate the button
            setEventListener { _, buttonState ->
                lifecycleScope.launchWhenCreated {
                    if (buttonState) {
                        // Button is active
                        undoReblogPost(api, credential)
                    } else {
                        // Button is inactive
                        reblogPost(api, credential)
                    }
                }
                //show animation or not?
                true
            }
        }
    }

    private suspend fun reblogPost(
        api: PixelfedAPI,
        credential: String
    ) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.reblogStatus(credential, it)

                //Update shown share count
                binding.nshares.text = resp.getNShares(binding.root.context)
                binding.reblogger.isChecked = resp.reblogged!!
            } catch (exception: IOException) {
                Log.e("REBLOG ERROR", exception.toString())
                binding.reblogger.isChecked = false
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.reblogger.isChecked = false
            }
        }
    }

    private suspend fun undoReblogPost(
        api: PixelfedAPI,
        credential: String,
    ) {
        //Call the api function
        status?.id?.let {
            try {
                val resp = api.undoReblogStatus(credential, it)

                //Update shown share count
                binding.nshares.text = resp.getNShares(binding.root.context)
                binding.reblogger.isChecked = resp.reblogged!!
            } catch (exception: IOException) {
                Log.e("REBLOG ERROR", exception.toString())
                binding.reblogger.isChecked = true
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.reblogger.isChecked = true
            }
        }
    }

    private fun activateMoreButton(api: PixelfedAPI, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope){
        binding.statusMore.setOnClickListener {
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
                            Dexter.withContext(binding.root.context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object : BasePermissionListener() {
                                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                        Toast.makeText(
                                            binding.root.context,
                                            binding.root.context.getString(R.string.write_permission_download_pic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                        status?.downloadImage(
                                            binding.root.context,
                                            status?.media_attachments?.get(binding.postPager.currentItem)?.url
                                                ?: "",
                                            binding.root
                                        )
                                    }
                                }).check()
                            true
                        }
                        R.id.post_more_menu_share_picture -> {
                            Dexter.withContext(binding.root.context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object : BasePermissionListener() {
                                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                        Toast.makeText(
                                            binding.root.context,
                                            binding.root.context.getString(R.string.write_permission_share_pic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                        status?.downloadImage(
                                            binding.root.context,
                                            status?.media_attachments?.get(binding.postPager.currentItem)?.url
                                                ?: "",
                                            binding.root,
                                            share = true,
                                        )
                                    }
                                }).check()
                            true
                        }
                        R.id.post_more_menu_delete -> {
                            val builder = AlertDialog.Builder(binding.root.context)
                            builder.apply {
                                setMessage(R.string.delete_dialog)
                                setPositiveButton(android.R.string.ok) { _, _ ->

                                    lifecycleScope.launch {
                                        val user = db.userDao().getActiveUser()!!
                                        status?.id?.let { id ->
                                            db.homePostDao().delete(id, user.user_id, user.instance_uri)
                                            db.publicPostDao().delete(id, user.user_id, user.instance_uri)
                                            try {
                                                api.deleteStatus("Bearer ${user.accessToken}", id)
                                                binding.root.visibility = View.GONE
                                            } catch (exception: IOException) {
                                            } catch (exception: HttpException) {
                                            }
                                        }
                                    }
                                }
                                setNegativeButton(android.R.string.cancel) { _, _ -> }
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
            api: PixelfedAPI,
            credential: String,
            isLiked: Boolean,
            lifecycleScope: LifecycleCoroutineScope
    ) {

        binding.liker.apply {
            //Set initial state
            isChecked = isLiked

            //Activate the liker
            setEventListener { _, buttonState ->
                lifecycleScope.launchWhenCreated {
                    if (buttonState) {
                        // Button is active, unlike
                        unLikePostCall(api, credential)
                    } else {
                        // Button is inactive, like
                        likePostCall(api, credential)
                    }
                }
                //show animation or not?
                true
            }
        }

        //Activate double tap liking
        var clicked = false
        binding.postPicture.setOnClickListener {
            lifecycleScope.launchWhenCreated {
                //Check that the post isn't hidden
                if(binding.sensitiveWarning.visibility == View.GONE) {
                    //Check for double click
                    if(clicked) {
                        if (binding.liker.isChecked) {
                            // Button is active, unlike
                            binding.liker.isChecked = false
                            unLikePostCall(api, credential)
                        } else {
                            // Button is inactive, like
                            binding.liker.playAnimation()
                            binding.liker.isChecked = true
                            likePostCall(api, credential)
                        }
                    } else {
                        clicked = true

                        //Reset clicked to false after 500ms
                        binding.postPicture.handler.postDelayed(fun() { clicked = false }, 500)
                    }
                }

            }
        }
    }

    private suspend fun likePostCall(
        api: PixelfedAPI,
        credential: String,
    ) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.likePost(credential, it)

                //Update shown like count and internal like toggle
                binding.nlikes.text = resp.getNLikes(binding.root.context)
                binding.liker.isChecked = resp.favourited ?: false
            } catch (exception: IOException) {
                Log.e("LIKE ERROR", exception.toString())
                binding.liker.isChecked = false
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.liker.isChecked = false
            }
        }
    }

    private suspend fun unLikePostCall(
        api: PixelfedAPI,
        credential: String,
    ) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.unlikePost(credential, it)

                //Update shown like count and internal like toggle
                binding.nlikes.text = resp.getNLikes(binding.root.context)
                binding.liker.isChecked = resp.favourited ?: false
            } catch (exception: IOException) {
                Log.e("UNLIKE ERROR", exception.toString())
                binding.liker.isChecked = true
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.liker.isChecked = true
            }
        }
    }

    private fun showComments(
        api: PixelfedAPI,
        credential: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        //Show all comments of a post
        if (status?.replies_count == 0) {
            binding.viewComments.text =  binding.root.context.getString(R.string.NoCommentsToShow)
        } else {
            binding.viewComments.apply {
                text = resources.getQuantityString(R.plurals.number_comments,
                                                    status?.replies_count ?: 0,
                                                    status?.replies_count ?: 0
                )
                setOnClickListener {
                    visibility = View.GONE

                    lifecycleScope.launchWhenCreated {
                        //Retrieve the comments
                        retrieveComments(api, credential)
                    }
                }
            }
        }
    }

    private fun activateCommenter(
        api: PixelfedAPI,
        credential: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        //Toggle comment button
        toggleCommentInput()

        //Activate commenterpostPicture
        binding.submitComment.setOnClickListener {
            val textIn = binding.editComment.text
            //Open text input
            if(textIn.isNullOrEmpty()) {
                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(R.string.empty_comment),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //Post the comment
                lifecycleScope.launchWhenCreated {
                    postComment(api, credential)
                }
            }
        }
    }

    private fun toggleCommentInput() {
        //Toggle comment button
        binding.commenter.setOnClickListener {
            when(binding.commentIn.visibility) {
                View.VISIBLE -> {
                    binding.commentIn.visibility = View.GONE
                    ImageConverter.setImageFromDrawable(
                        binding.root,
                        binding.commenter,
                        R.drawable.ic_comment_empty
                    )
                }
                View.GONE -> {
                    binding.commentIn.visibility = View.VISIBLE
                    ImageConverter.setImageFromDrawable(
                        binding.root,
                        binding.commenter,
                        R.drawable.ic_comment_blue
                    )
                }
            }
        }
    }

    fun addComment(context: android.content.Context, commentContainer: LinearLayout, commentUsername: String, commentContent: String) {


        val itemBinding = CommentBinding.inflate(
            LayoutInflater.from(context), commentContainer, false
        )

        itemBinding.user.text = commentUsername
        itemBinding.commentText.text = commentContent
    }

    private suspend fun retrieveComments(
            api: PixelfedAPI,
            credential: String,
    ) {
        status?.id?.let {
            try {
                val statuses = api.statusComments(it, credential).descendants

                binding.commentContainer.removeAllViews()

                //Create the new views for each comment
                for (status in statuses) {
                    addComment(binding.root.context, binding.commentContainer, status.account!!.username!!,
                            status.content!!
                    )
                }
                binding.commentContainer.visibility = View.VISIBLE

            } catch (exception: IOException) {
                Log.e("COMMENT FETCH ERROR", exception.toString())
            } catch (exception: HttpException) {
                Log.e("COMMENT ERROR", "${exception.code()} with body ${exception.response()?.errorBody()}")
            }
        }
    }

    private suspend fun postComment(
        api: PixelfedAPI,
        credential: String,
    ) {
        val textIn = binding.editComment.text
        val nonNullText = textIn.toString()
        status?.id?.let {
            try {
                val response = api.postStatus(credential, nonNullText, it)
                binding.commentIn.visibility = View.GONE

                //Add the comment to the comment section
                addComment(
                    binding.root.context, binding.commentContainer, response.account!!.username!!,
                    response.content!!
                )

                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(R.string.comment_posted).format(textIn),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: IOException) {
                Log.e("COMMENT ERROR", exception.toString())
                Toast.makeText(
                    binding.root.context, binding.root.context.getString(R.string.comment_error),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: HttpException) {
                Toast.makeText(
                    binding.root.context, binding.root.context.getString(R.string.comment_error),
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ERROR_CODE", exception.code().toString())
            }
        }
    }


    companion object {
        fun create(parent: ViewGroup): StatusViewHolder {
            val itemBinding = PostFragmentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return StatusViewHolder(itemBinding)
        }
    }
}

class AlbumViewPagerAdapter(private val media_attachments: List<Attachment>) :
    RecyclerView.Adapter<AlbumViewPagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = AlbumImageViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(itemBinding)
    }

    override fun getItemCount() = media_attachments.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.binding.root)
            .asDrawable().fitCenter().placeholder(
                BlurHashDecoder.blurHashBitmap(
                        holder.binding.root.context.resources, media_attachments[position])
                )
            .load(media_attachments[position].url).into(holder.image)

        val description = media_attachments[position].description
            .orEmpty().ifEmpty{ holder.binding.root.context.getString(R.string.no_description)}

        holder.image.setOnLongClickListener {
            Snackbar.make(it, description, Snackbar.LENGTH_SHORT).show()
            true
        }

        holder.image.contentDescription = description
    }

    class ViewHolder(val binding: AlbumImageViewBinding) : RecyclerView.ViewHolder(binding.root){
        val image: ImageView = binding.imageImageView
    }
}