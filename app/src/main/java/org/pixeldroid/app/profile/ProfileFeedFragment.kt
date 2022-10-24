package org.pixeldroid.app.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.FragmentProfilePostsBinding
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.posts.StatusViewHolder
import org.pixeldroid.app.posts.feeds.UIMODEL_STATUS_COMPARATOR
import org.pixeldroid.app.posts.feeds.uncachedFeeds.*
import org.pixeldroid.app.posts.feeds.uncachedFeeds.profile.ProfileContentRepository
import org.pixeldroid.app.utils.BlurHashDecoder
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.displayDimensionsInPx
import org.pixeldroid.app.utils.setSquareImageFromURL

/**
 * Fragment to show a list of [Account]s, as a result of a search.
 */
class ProfileFeedFragment : UncachedFeedFragment<Status>() {

    companion object {
        const val PROFILE_GRID = "ProfileGrid"
        const val BOOKMARKS = "Bookmarks"
    }

    private lateinit var accountId : String
    private var user: UserDatabaseEntity? = null
    private var grid: Boolean = true
    private var bookmarks: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        grid = arguments?.getSerializable(PROFILE_GRID) as Boolean
        bookmarks = arguments?.getSerializable(BOOKMARKS) as Boolean
        adapter = ProfilePostsAdapter()

        //get the currently active user
        user = db.userDao().getActiveUser()
        // Set profile according to given account
        val account = arguments?.getSerializable(Account.ACCOUNT_TAG) as Account?
        accountId = account?.id ?: user!!.user_id
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = super.onCreateView(inflater, container, savedInstanceState)

        if(grid || bookmarks) {
            binding.list.layoutManager = GridLayoutManager(context, 3)
        }

        // Get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(requireActivity(), ProfileViewModelFactory(
            ProfileContentRepository(
                apiHolder.setToCurrentUser(),
                accountId,
                bookmarks
            )
        )
        )[if(bookmarks) "Bookmarks" else "Profile", FeedViewModel::class.java] as FeedViewModel<Status>

        launch()
        initSearch()

        return view
    }

    inner class ProfilePostsAdapter() : PagingDataAdapter<Status, RecyclerView.ViewHolder>(
        UIMODEL_STATUS_COMPARATOR
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if(grid || bookmarks) {
                ProfilePostsViewHolder.create(parent)
            } else {
                StatusViewHolder.create(parent)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val post = getItem(position)

            post?.let {
                if(grid || bookmarks) {
                    (holder as ProfilePostsViewHolder).bind(it)
                } else {
                    (holder as StatusViewHolder).bind(it, apiHolder, db,
                        lifecycleScope, requireContext().displayDimensionsInPx())
                }
            }
        }
    }
}

class ProfilePostsViewHolder(binding: FragmentProfilePostsBinding) : RecyclerView.ViewHolder(binding.root) {
    private val postPreview: ImageView = binding.postPreview
    private val albumIcon: ImageView = binding.albumIcon
    private val videoIcon: ImageView = binding.videoIcon

    fun bind(post: Status) {

        if ((post.media_attachments?.size ?: 0) == 0){
            //No media in this post, so put a little icon there
            postPreview.scaleX = 0.3f
            postPreview.scaleY = 0.3f
            Glide.with(postPreview).load(R.drawable.ic_comment_empty).into(postPreview)
            albumIcon.visibility = View.GONE
            videoIcon.visibility = View.GONE
        } else {
            postPreview.scaleX = 1f
            postPreview.scaleY = 1f
            if (post.sensitive != false) {
                Glide.with(postPreview)
                    .load(post.media_attachments?.firstOrNull()?.blurhash?.let {
                        BlurHashDecoder.blurHashBitmap(itemView.resources, it, 32, 32)
                    }
                    ).placeholder(R.drawable.ic_sensitive).apply(RequestOptions().centerCrop())
                    .into(postPreview)
            } else {
                setSquareImageFromURL(postPreview,
                    post.getPostPreviewURL(),
                    postPreview,
                    post.media_attachments?.firstOrNull()?.blurhash)
            }
            if ((post.media_attachments?.size ?: 0) > 1) {
                albumIcon.visibility = View.VISIBLE
                videoIcon.visibility = View.GONE
            } else {
                albumIcon.visibility = View.GONE
                if (post.media_attachments?.getOrNull(0)?.type == Attachment.AttachmentType.video) {
                    videoIcon.visibility = View.VISIBLE
                } else videoIcon.visibility = View.GONE

            }
        }

        postPreview.setOnClickListener {
            val intent = Intent(postPreview.context, PostActivity::class.java)
            intent.putExtra(Status.POST_TAG, post)
            postPreview.context.startActivity(intent)
        }
    }

    companion object {
        fun create(parent: ViewGroup): ProfilePostsViewHolder {
            val itemBinding = FragmentProfilePostsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ProfilePostsViewHolder(itemBinding)
        }
    }
}


class ProfileViewModelFactory @ExperimentalPagingApi constructor(
    private val searchContentRepository: UncachedContentRepository<Status>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(searchContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
