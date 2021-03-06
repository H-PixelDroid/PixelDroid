package org.pixeldroid.app.posts

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.AnimatedVectorDrawable
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
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.google.android.material.snackbar.Snackbar
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.AlbumImageViewBinding
import org.pixeldroid.app.databinding.PostFragmentBinding
import org.pixeldroid.app.utils.BlurHashDecoder
import org.pixeldroid.app.utils.ImageConverter
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_COMMENT_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.VIEW_COMMENTS_TAG
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
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

    fun bind(status: Status?, pixelfedAPI: PixelfedAPIHolder, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope, displayDimensionsInPx: Pair<Int, Int>, isActivity: Boolean = false) {

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
            binding.postPager.visibility = View.GONE
            binding.postIndicator.visibility = View.GONE
        }
    }

    private fun setupPostPics(
        binding: PostFragmentBinding,
        request: RequestBuilder<Drawable>,
    ) {

        // Standard layout
        binding.postPager.visibility = View.VISIBLE

        //Attach the given tabs to the view pager
        binding.postPager.adapter = AlbumViewPagerAdapter(status?.media_attachments ?: emptyList(), status?.sensitive)

        if(status?.media_attachments?.size ?: 0 > 1) {
            binding.postIndicator.setViewPager(binding.postPager)
            binding.postIndicator.visibility = View.VISIBLE
        } else {
            binding.postIndicator.visibility = View.GONE
        }

        if (status?.sensitive == true) {
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
        isActivity: Boolean
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
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.reblogger.isChecked = false
            } catch (exception: IOException) {
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
            } catch (exception: IOException) {
                Log.e("REBLOG ERROR", exception.toString())
                binding.reblogger.isChecked = true
            }
        }
    }

    private fun activateMoreButton(apiHolder: PixelfedAPIHolder, db: AppDatabase, lifecycleScope: LifecycleCoroutineScope){
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
                                                val api = apiHolder.api ?: apiHolder.setToCurrentUser()
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

        //Activate double tap liking
        var clicked = false
        binding.postPagerHost.doubleTapCallback = {
            if(!it) clicked = false
            else lifecycleScope.launchWhenCreated {
                //Check that the post isn't hidden
                if(binding.sensitiveWarning.visibility == View.GONE) {
                    //Check for double click
                    if(clicked) {
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
                    } else {
                        clicked = true

                        //Reset clicked to false after 500ms
                        binding.postPager.handler.postDelayed(fun() { clicked = false }, 500)
                    }
                }
            }
        }
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
            } catch (exception: IOException) {
                Log.e("LIKE ERROR", exception.toString())
                binding.liker.isChecked = false
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
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
            } catch (exception: IOException) {
                Log.e("UNLIKE ERROR", exception.toString())
                binding.liker.isChecked = true
            } catch (exception: HttpException) {
                Log.e("RESPONSE_CODE", exception.code().toString())
                binding.liker.isChecked = true
            }
        }
    }
    //endregion

    private fun showComments(
            lifecycleScope: LifecycleCoroutineScope,
            isActivity: Boolean
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




    companion object {
        fun create(parent: ViewGroup): StatusViewHolder {
            val itemBinding = PostFragmentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return StatusViewHolder(itemBinding)
        }
    }
}

private class AlbumViewPagerAdapter(private val media_attachments: List<Attachment>, private var sensitive: Boolean?) :
    RecyclerView.Adapter<AlbumViewPagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = AlbumImageViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(itemBinding)
    }

    override fun getItemCount() = media_attachments.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        media_attachments[position].apply {
            val blurhashBitMap = blurhash?.let {
                BlurHashDecoder.blurHashBitmap(
                        holder.binding.root.resources,
                        it,
                        meta?.original?.width,
                        meta?.original?.height
                )
            }
            if (sensitive == false) {
                Glide.with(holder.binding.root)
                        .asDrawable().fitCenter()
                        .placeholder(blurhashBitMap)
                        .load(url).into(holder.image)
            } else {
                Glide.with(holder.binding.root)
                        .asDrawable().fitCenter()
                        .load(blurhashBitMap).into(holder.image)
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

    fun uncensor(){
        sensitive = false
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: AlbumImageViewBinding) : RecyclerView.ViewHolder(binding.root){
        val image: ImageView = binding.imageImageView
    }
}