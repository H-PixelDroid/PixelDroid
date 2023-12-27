package org.pixeldroid.app.posts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import okhttp3.*
import okio.BufferedSink
import okio.buffer
import okio.sink
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.AlbumImageViewBinding
import org.pixeldroid.app.databinding.OpenedAlbumBinding
import org.pixeldroid.app.databinding.PostFragmentBinding
import org.pixeldroid.app.postCreation.PostCreationActivity
import org.pixeldroid.app.posts.MediaViewerActivity.Companion.openActivity
import org.pixeldroid.app.utils.BlurHashDecoder
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_COMMENT_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.VIEW_COMMENTS_TAG
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.setProfileImageFromURL
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt


/**
 * View Holder for a [Status] RecyclerView list item.
 */
class StatusViewHolder(val binding: PostFragmentBinding) : RecyclerView.ViewHolder(binding.root) {

    private var status: Status? = null

    fun bind(status: Status?, pixelfedAPI: PixelfedAPIHolder, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope, displayDimensionsInPx: Pair<Int, Int>, isActivity: Boolean = false) {

        this.itemView.visibility = View.VISIBLE
        this.status = status

        val maxImageRatio: Float = status?.media_attachments?.maxOfOrNull {
            if (it.meta?.original?.width == null || it.meta.original.height == null) {
                1f
            } else {
                it.meta.original.width.toFloat() / it.meta.original.height.toFloat()
            }
        } ?: 1f

        val (displayWidth, displayHeight) = displayDimensionsInPx
        if (displayWidth / maxImageRatio > displayHeight * 3/4f) {
            binding.postPager.layoutParams.width = ((displayHeight * 3 / 4f) * maxImageRatio).roundToInt()
            binding.postPager.layoutParams.height = (displayHeight * 3 / 4f).toInt()
        } else {
            binding.postPager.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.postPager.layoutParams.height = (displayWidth / maxImageRatio).toInt()
        }

        //Setup the post layout
        val picRequest = Glide.with(itemView).asDrawable().fitCenter()

        val user = db.userDao().getActiveUser()!!

        setupPost(picRequest, user.instance_uri, isActivity)

        activateButtons(pixelfedAPI, db, lifecycleScope, isActivity)

    }

    private fun setupPost(
        request: RequestBuilder<Drawable>,
        domain: String,
        isActivity: Boolean,
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
            isActivity
        )

        binding.postDomain.text = status?.getStatusDomain(domain, binding.postDomain.context)

        //Setup images
        setProfileImageFromURL(
            binding.root,
            status?.getProfilePicUrl(),
            binding.profilePic
        )
        binding.profilePic.setOnClickListener { status?.account?.openProfile(binding.root.context) }

        //Setup post pic only if there are media attachments
        if(!status?.media_attachments.isNullOrEmpty()) {
            setupPostPics(binding, request)
        } else {
            binding.postPager.visibility = View.GONE
            binding.postIndicator.visibility = View.GONE
        }
    }

    private fun setupPostPics(
        binding: PostFragmentBinding,
        request: RequestBuilder<Drawable>,
    ) {
        val alwaysShowNsfw =
            PreferenceManager.getDefaultSharedPreferences(binding.root.context.applicationContext)
                .getBoolean("always_show_nsfw", false)

        // Standard layout
        binding.postPager.visibility = View.VISIBLE

        //Attach the given tabs to the view pager
        binding.postPager.adapter = AlbumViewPagerAdapter(status?.media_attachments ?: emptyList(), status?.sensitive, false, alwaysShowNsfw)

        if((status?.media_attachments?.size ?: 0) > 1) {
            binding.postIndicator.setViewPager(binding.postPager)
            binding.postIndicator.visibility = View.VISIBLE
        } else {
            binding.postIndicator.visibility = View.GONE
        }

        if (status?.sensitive == true && !alwaysShowNsfw) {
            setupSensitiveLayout()
        } else {
            // GONE is the default, but have to set it again because of how RecyclerViews work
            binding.sensitiveWarning.visibility = View.GONE
        }
    }


    private fun setupSensitiveLayout() {

        // Set dark layout and warning message
        binding.sensitiveWarning.visibility = View.VISIBLE
        //binding.postPicture.colorFilter = ColorMatrixColorFilter(censorMatrix)

        fun uncensorPicture(binding: PostFragmentBinding) {
            binding.sensitiveWarning.visibility = View.GONE
            (binding.postPager.adapter as AlbumViewPagerAdapter).uncensor()
        }
        
        binding.sensitiveWarning.setOnClickListener {
            uncensorPicture(binding)
        }
    }

    private fun setDescription(
        apiHolder: PixelfedAPIHolder,
        lifecycleScope: LifecycleCoroutineScope,
    ) {
        binding.description.apply {
            if (status?.content.isNullOrBlank()) {
                visibility = View.GONE
            } else {
                text = parseHTMLText(
                        status?.content.orEmpty(),
                        status?.mentions,
                        apiHolder,
                        binding.root.context,
                        lifecycleScope
                )
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }
    //region buttons
    private fun activateButtons(
        apiHolder: PixelfedAPIHolder,
        db: AppDatabase,
        lifecycleScope: LifecycleCoroutineScope,
        isActivity: Boolean,
    ){
        //Set the special HTML text
        setDescription(apiHolder, lifecycleScope)

        //Activate onclickListeners
        activateLiker(
                apiHolder, status?.favourited ?: false, lifecycleScope
        )
        activateReblogger(
                apiHolder, status?.reblogged ?: false, lifecycleScope
        )

        if(isActivity){
            binding.commenter.visibility = View.INVISIBLE
        }
        else {
            binding.commenter.setOnClickListener {
                lifecycleScope.launchWhenCreated {
                    //Open status in activity
                    val intent = Intent(it.context, PostActivity::class.java)
                    intent.putExtra(POST_TAG, status)
                    intent.putExtra(POST_COMMENT_TAG, true)
                    it.context.startActivity(intent)
                }
            }
        }

        showComments(lifecycleScope, isActivity)

        activateMoreButton(apiHolder, db, lifecycleScope)
    }

    private fun activateReblogger(
        apiHolder: PixelfedAPIHolder,
        isReblogged: Boolean,
        lifecycleScope: LifecycleCoroutineScope,
    ) {
        binding.reblogger.apply {
            //Set initial button state
            isChecked = isReblogged

            //Activate the button
            setEventListener { _, buttonState ->
                lifecycleScope.launchWhenCreated {
                    val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                    if (buttonState) {
                        // Button is active
                        undoReblogPost(api)
                    } else {
                        // Button is inactive
                        reblogPost(api)
                    }
                }
                //show animation or not?
                true
            }
        }
    }

    private suspend fun reblogPost(api: PixelfedAPI) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.reblogStatus(it)

                //Update shown share count
                binding.nshares.text = resp.getNShares(binding.root.context)
                binding.reblogger.isChecked = resp.reblogged!!
            } catch (exception: Exception) {
                Log.e("REBLOG ERROR", exception.toString())
                binding.reblogger.isChecked = false
            }
        }
    }

    private suspend fun undoReblogPost(api: PixelfedAPI) {
        //Call the api function
        status?.id?.let {
            try {
                val resp = api.undoReblogStatus(it)

                //Update shown share count
                binding.nshares.text = resp.getNShares(binding.root.context)
                binding.reblogger.isChecked = resp.reblogged!!
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.reblogger.isChecked = true
            } catch (exception: Exception) {
                Log.e("REBLOG ERROR", exception.toString())
                binding.reblogger.isChecked = true
            }
        }
    }

    private suspend fun bookmarkPost(api: PixelfedAPI, db: AppDatabase, menu: Menu, bookmarked: Boolean) : Boolean? {
        //Call the api function
        status?.id?.let { id ->
            try {
                if (bookmarked) {
                    api.bookmarkStatus(id)
                } else {
                    api.undoBookmarkStatus(id)
                }
                val user = db.userDao().getActiveUser()!!
                db.homePostDao().bookmarkStatus(id, user.user_id, user.instance_uri, bookmarked)
                db.publicPostDao().bookmarkStatus(id, user.user_id, user.instance_uri, bookmarked)

                menu.setGroupVisible(R.id.post_more_menu_group_bookmark, !bookmarked)
                menu.setGroupVisible(R.id.post_more_menu_group_unbookmark, bookmarked)
                return bookmarked
            } catch (exception: HttpException) {
                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(
                        R.string.bookmark_post_failed_error,
                        exception.code()
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: IOException) {
                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(R.string.bookmark_post_failed_io_except),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return null
    }

    private fun activateMoreButton(apiHolder: PixelfedAPIHolder, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope){
        var bookmarked: Boolean? = null
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
                        R.id.post_more_menu_bookmark -> {
                            lifecycleScope.launch {
                                bookmarked = bookmarkPost(apiHolder.api ?: apiHolder.setToCurrentUser(), db, menu, true)
                            }
                            true
                        }
                        R.id.post_more_menu_unbookmark -> {
                            lifecycleScope.launch {
                                bookmarked = bookmarkPost(apiHolder.api ?: apiHolder.setToCurrentUser(), db, menu, false)
                            }
                            true
                        }
                        R.id.post_more_menu_save_to_gallery -> {
                            status?.downloadImage(
                                binding.root.context,
                                status?.media_attachments?.getOrNull(binding.postPager.currentItem)?.url
                                    ?: "",
                                binding.root
                            )
                            true
                        }

                        R.id.post_more_menu_share_picture -> {
                            status?.downloadImage(
                                binding.root.context,
                                status?.media_attachments?.getOrNull(binding.postPager.currentItem)?.url
                                    ?: "",
                                binding.root,
                                share = true,
                            )
                            true
                        }
                        R.id.post_more_menu_delete -> {
                            MaterialAlertDialogBuilder(binding.root.context)
                                .setMessage(R.string.delete_dialog)
                                .setPositiveButton(android.R.string.ok) { _, _ ->

                                    lifecycleScope.launch {
                                        deletePost(apiHolder.api ?: apiHolder.setToCurrentUser(), db)
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                .show()

                            true
                        }
                        R.id.post_more_menu_redraft -> {
                            MaterialAlertDialogBuilder(binding.root.context).apply {
                                setMessage(R.string.redraft_dialog_launch)
                                setPositiveButton(android.R.string.ok) { _, _ ->

                                    lifecycleScope.launch {
                                        try {
                                            // Create new post creation activity
                                            val intent =
                                                Intent(context, PostCreationActivity::class.java)

                                            // Get descriptions and images from original post
                                            val postDescription = status?.content ?: ""
                                            val postAttachments =
                                                status?.media_attachments!!  // Catch possible exception from !! (?)
                                            val postNSFW = status?.sensitive

                                            val imageUriStrings = postAttachments.map { postAttachment ->
                                                postAttachment.url ?: ""
                                            }
                                            val imageNames = imageUriStrings.map { imageUriString ->
                                                Uri.parse(imageUriString).lastPathSegment.toString()
                                            }
                                            val downloadedFiles = imageNames.map { imageName ->
                                                File(context.cacheDir, imageName)
                                            }
                                            val imageUris = downloadedFiles.map { downloadedFile ->
                                                Uri.fromFile(downloadedFile)
                                            }
                                            val imageDescriptions = postAttachments.map { postAttachment ->
                                                fromHtml(postAttachment.description ?: "").toString()
                                            }
                                            val downloadRequests: List<Request> = imageUriStrings.map { imageUriString ->
                                                Request.Builder().url(imageUriString).build()
                                            }

                                            val counter = AtomicInteger(0)

                                            // Define callback function for after downloading the images
                                            fun continuation() {
                                                // Wait for all outstanding downloads to finish
                                                if (counter.incrementAndGet() == imageUris.size) {
                                                    if (allFilesExist(imageNames)) {
                                                        // Delete original post
                                                        lifecycleScope.launch {
                                                            deletePost(apiHolder.api ?: apiHolder.setToCurrentUser(), db)
                                                        }

                                                        val counterInt = counter.get()
                                                        Toast.makeText(
                                                            binding.root.context,
                                                            binding.root.context.resources.getQuantityString(
                                                                R.plurals.items_load_success,
                                                                counterInt,
                                                                counterInt
                                                            ),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        // Pass downloaded images to new post creation activity
                                                        intent.apply {
                                                            imageUris.zip(imageDescriptions).map { (imageUri, imageDescription) ->
                                                                ClipData.Item(imageDescription, null, imageUri)
                                                            }.forEach { imageItem ->
                                                                if (clipData == null) {
                                                                    clipData = ClipData(
                                                                        "",
                                                                        emptyArray(),
                                                                        imageItem
                                                                    )
                                                                } else {
                                                                    clipData!!.addItem(imageItem)
                                                                }
                                                            }
                                                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }

                                                        // Pass post description of existing post to new post creation activity
                                                        intent.putExtra(
                                                            PostCreationActivity.PICTURE_DESCRIPTION,
                                                            fromHtml(postDescription).toString()
                                                        )
                                                        if (imageNames.isNotEmpty()) {
                                                            intent.putExtra(
                                                                PostCreationActivity.TEMP_FILES,
                                                                imageNames.toTypedArray()
                                                            )
                                                        }
                                                        intent.putExtra(
                                                            PostCreationActivity.POST_REDRAFT,
                                                            true
                                                        )
                                                        intent.putExtra(
                                                            PostCreationActivity.POST_NSFW,
                                                            postNSFW
                                                        )

                                                        // Launch post creation activity
                                                        binding.root.context.startActivity(intent)
                                                    }
                                                }
                                            }

                                            if (!allFilesExist(imageNames)) {
                                                // Track download progress
                                                Toast.makeText(
                                                    binding.root.context,
                                                    binding.root.context.getString(R.string.image_download_downloading),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            // Iterate through all pictures of the original post
                                            downloadRequests.zip(downloadedFiles).forEach { (downloadRequest, downloadedFile) ->
                                                // Check whether image is in cache directory already (maybe rather do so using Glide in the future?)
                                                if (!downloadedFile.exists()) {
                                                    OkHttpClient().newCall(downloadRequest)
                                                        .enqueue(object : Callback {
                                                            override fun onFailure(
                                                                call: Call,
                                                                e: IOException
                                                            ) {
                                                                Looper.prepare()
                                                                downloadedFile.delete()
                                                                Toast.makeText(
                                                                    binding.root.context,
                                                                    binding.root.context.getString(R.string.redraft_post_failed_io_except),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }

                                                            @Throws(IOException::class)
                                                            override fun onResponse(
                                                                call: Call,
                                                                response: Response
                                                            ) {
                                                                val sink: BufferedSink =
                                                                    downloadedFile.sink().buffer()
                                                                sink.writeAll(response.body!!.source())
                                                                sink.close()
                                                                Looper.prepare()
                                                                continuation()
                                                            }
                                                        })
                                                } else {
                                                    continuation()
                                                }
                                            }
                                        } catch (exception: HttpException) {
                                            Toast.makeText(
                                                binding.root.context,
                                                binding.root.context.getString(
                                                    R.string.redraft_post_failed_error,
                                                    exception.code()
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (exception: IOException) {
                                            Toast.makeText(
                                                binding.root.context,
                                                binding.root.context.getString(R.string.redraft_post_failed_io_except),
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                if(bookmarked == true || status?.bookmarked == true) {
                    menu.setGroupVisible(R.id.post_more_menu_group_bookmark, false)
                } else if(bookmarked == false || status?.bookmarked != true) {
                    menu.setGroupVisible(R.id.post_more_menu_group_unbookmark, false)
                }
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
        apiHolder: PixelfedAPIHolder,
        isLiked: Boolean,
        lifecycleScope: LifecycleCoroutineScope,
    ) {

        binding.liker.apply {
            //Set initial state
            isChecked = isLiked

            //Activate the liker
            setEventListener { _, buttonState ->
                lifecycleScope.launchWhenCreated {
                    val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                    if (buttonState) {
                        // Button is active, unlike
                        unLikePostCall(api)
                    } else {
                        // Button is inactive, like
                        likePostCall(api)
                    }
                }
                //show animation or not?
                true
            }
        }

        //Activate tap interactions (double and single)
        binding.postPagerHost.doubleTapCallback = {
            lifecycleScope.launchWhenCreated {
                //Check that the post isn't hidden
                if (binding.sensitiveWarning.visibility == View.GONE) {
                    val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                    if (binding.liker.isChecked) {
                        // Button is active, unlike
                        binding.liker.isChecked = false
                        unLikePostCall(api)
                    } else {
                        // Button is inactive, like
                        binding.liker.playAnimation()
                        binding.liker.isChecked = true
                        binding.likeAnimation.animateView()
                        likePostCall(api)
                    }
                }
            }
        }
        status?.media_attachments?.let { binding.postPagerHost.images = ArrayList(it) }

    }

    private fun ImageView.animateView() {
        visibility = View.VISIBLE
        when (val drawable = drawable) {
            is AnimatedVectorDrawableCompat -> {
                drawable.start()
            }
            is AnimatedVectorDrawable -> {
                drawable.start()
            }
        }
    }

    private suspend fun likePostCall(api: PixelfedAPI) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.likePost(it)

                //Update shown like count and internal like toggle
                binding.nlikes.text = resp.getNLikes(binding.root.context)
                binding.liker.isChecked = resp.favourited ?: false
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.liker.isChecked = false
            } catch (exception: Exception) {
                Log.e("LIKE ERROR", exception.toString())
                binding.liker.isChecked = false
            }
        }
    }

    private suspend fun unLikePostCall(api: PixelfedAPI) {
        //Call the api function
        status?.id?.let {

            try {
                val resp = api.unlikePost(it)

                //Update shown like count and internal like toggle
                binding.nlikes.text = resp.getNLikes(binding.root.context)
                binding.liker.isChecked = resp.favourited ?: false
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.liker.isChecked = true
            } catch (exception: Exception) {
                Log.e("UNLIKE ERROR", exception.toString())
                binding.liker.isChecked = true
            }
        }
    }
    //endregion

    private fun showComments(
        lifecycleScope: LifecycleCoroutineScope,
        isActivity: Boolean,
    ) {
        //Show number of comments on the post
        if (status?.replies_count == 0) {
            binding.viewComments.text =  binding.root.context.getString(R.string.NoCommentsToShow)
        } else {
            binding.viewComments.apply {
                text = resources.getQuantityString(R.plurals.number_comments,
                                                    status?.replies_count ?: 0,
                                                    status?.replies_count ?: 0
                )
                if(!isActivity) {
                    setOnClickListener {
                        lifecycleScope.launchWhenCreated {
                            //Open status in activity
                            val intent = Intent(context, PostActivity::class.java)
                            intent.putExtra(POST_TAG, status)
                            intent.putExtra(VIEW_COMMENTS_TAG, true)
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    private suspend fun deletePost(api: PixelfedAPI, db: AppDatabase) {
        val user = db.userDao().getActiveUser()!!
        status?.id?.let { id ->
            db.homePostDao().delete(id, user.user_id, user.instance_uri)
            db.publicPostDao().delete(id, user.user_id, user.instance_uri)
            try {
                api.deleteStatus(id)
                binding.root.visibility = View.GONE
            } catch (exception: HttpException) {
                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(R.string.delete_post_failed_error, exception.code()),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: IOException) {
                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(R.string.delete_post_failed_io_except),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun allFilesExist(listOfNames: List<String>): Boolean {
        return listOfNames.all {
            File(binding.root.context.cacheDir, it).exists()
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

class AlbumViewPagerAdapter(
    private val media_attachments: List<Attachment>, private var sensitive: Boolean?,
    private val opened: Boolean, private val alwaysShowNsfw: Boolean,
) :
    RecyclerView.Adapter<AlbumViewPagerAdapter.ViewHolder>() {

    private var isActionBarHidden: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if(!opened) ViewHolderClosed(AlbumImageViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )) else ViewHolderOpen(OpenedAlbumBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun getItemCount() = media_attachments.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        media_attachments[position].apply {
            val video = type == Attachment.AttachmentType.video
            val blurhashBitMap = blurhash?.let {
                BlurHashDecoder.blurHashBitmap(
                        holder.binding.root.resources,
                        it,
                        meta?.original?.width,
                        meta?.original?.height
                )
            }
            if (sensitive == false || alwaysShowNsfw) {
                val imageUrl = if(video) preview_url else url
                if(opened){
                    Glide.with(holder.binding.root)
                        .download(GlideUrl(imageUrl))
                        .into(object : CustomViewTarget<SubsamplingScaleImageView, File>((holder.image as SubsamplingScaleImageView)) {
                            override fun onResourceReady(resource: File, t: Transition<in File>?) =
                                view.setImage(ImageSource.uri(Uri.fromFile(resource)))
                            override fun onLoadFailed(errorDrawable: Drawable?) {}
                            override fun onResourceCleared(placeholder: Drawable?) {}
                        })
                    (holder.image as SubsamplingScaleImageView).apply {
                        setMinimumDpi(80)
                        setDoubleTapZoomDpi(240)
                        resetScaleAndCenter()
                    }
                    holder.image.setOnClickListener {
                        val windowInsetsController = WindowCompat.getInsetsController((it.context as Activity).window, it)
                        // Configure the behavior of the hidden system bars
                        if (isActionBarHidden) {
                            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            // Hide both the status bar and the navigation bar
                            (it.context as AppCompatActivity).supportActionBar?.show()
                            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                            isActionBarHidden = false
                        } else {
                            // Configure the behavior of the hidden system bars
                            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            // Hide both the status bar and the navigation bar
                            (it.context as AppCompatActivity).supportActionBar?.hide()
                            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                            isActionBarHidden = true
                        }
                    }
                }
                else Glide.with(holder.binding.root)
                        .asDrawable().fitCenter()
                        .placeholder(blurhashBitMap)
                        .load(imageUrl).into(holder.image as ImageView)
            } else if(!opened){
                Glide.with(holder.binding.root)
                        .asDrawable().fitCenter()
                        .load(blurhashBitMap).into(holder.image as ImageView)
            }

            holder.videoPlayButton.visibility = if(video) View.VISIBLE else View.GONE

            if(video && (opened || media_attachments.size == 1)){
                holder.videoPlayButton.setOnClickListener {
                    openActivity(holder.binding.root.context, url, description)
                }
                holder.image.setOnClickListener {
                    openActivity(holder.binding.root.context, url, description)
                }
            }

            val description = description
                .orEmpty()
                .ifEmpty { holder.binding.root.context.getString(R.string.no_description) }

            holder.image.setOnLongClickListener {
                Snackbar.make(it, description, Snackbar.LENGTH_SHORT).show()
                true
            }

            holder.image.contentDescription = description
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun uncensor(){
        sensitive = false
        notifyDataSetChanged()
    }
    abstract class ViewHolder(open val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root){
        abstract val image: View
        abstract val videoPlayButton: ImageView
    }

    class ViewHolderOpen(override val binding: OpenedAlbumBinding) : ViewHolder(binding) {
        override val image: SubsamplingScaleImageView = binding.imageImageView
        override val videoPlayButton: ImageView = binding.videoPlayButton
    }
    class ViewHolderClosed(override val binding: AlbumImageViewBinding) : ViewHolder(binding) {
        override val image: ImageView = binding.imageImageView
        override val videoPlayButton: ImageView = binding.videoPlayButton
    }
}
