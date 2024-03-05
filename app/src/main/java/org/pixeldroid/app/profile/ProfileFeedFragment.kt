package org.pixeldroid.app.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.FragmentProfilePostsBinding
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.posts.StatusViewHolder
import org.pixeldroid.app.posts.feeds.uncachedFeeds.*
import org.pixeldroid.app.posts.feeds.uncachedFeeds.profile.CollectionsContentRepository
import org.pixeldroid.app.posts.feeds.uncachedFeeds.profile.ProfileContentRepository
import org.pixeldroid.app.profile.CollectionActivity.Companion.ADD_COLLECTION_TAG
import org.pixeldroid.app.profile.CollectionActivity.Companion.ADD_TO_COLLECTION_RESULT
import org.pixeldroid.app.profile.CollectionActivity.Companion.DELETE_FROM_COLLECTION_RESULT
import org.pixeldroid.app.profile.CollectionActivity.Companion.DELETE_FROM_COLLECTION_TAG
import org.pixeldroid.app.utils.BlurHashDecoder
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.api.objects.Collection
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.displayDimensionsInPx
import org.pixeldroid.app.utils.openUrl
import org.pixeldroid.app.utils.setSquareImageFromURL

/**
 * Fragment to show a list of [Account]s, as a result of a search.
 */
class ProfileFeedFragment : UncachedFeedFragment<FeedContent>() {

    companion object {
        // List of collections
        const val COLLECTIONS = "Collections"
        // Content of collection
        const val COLLECTION = "Collection"
        const val COLLECTION_ID = "CollectionId"
        const val PROFILE_GRID = "ProfileGrid"
        const val BOOKMARKS = "Bookmarks"
    }

    private lateinit var accountId : String
    private var user: UserDatabaseEntity? = null
    private var grid: Boolean = true
    private var bookmarks: Boolean = false
    private var collections: Boolean = false
    private var collection: Collection? = null
    private var addCollection: Boolean = false
    private var deleteFromCollection: Boolean = false
    private var collectionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        grid = arguments?.getBoolean(PROFILE_GRID, true) ?: true
        bookmarks = arguments?.getBoolean(BOOKMARKS) ?: false
        collections = arguments?.getBoolean(COLLECTIONS) ?: false
        collection = arguments?.getSerializable(COLLECTION) as? Collection
        addCollection = arguments?.getBoolean(ADD_COLLECTION_TAG) ?: false
        deleteFromCollection = arguments?.getBoolean(DELETE_FROM_COLLECTION_TAG) ?: false
        collectionId = arguments?.getString(COLLECTION_ID)
        if(addCollection){
            // We want the user's profile, set all the rest to false to be sure
            collections = false
            bookmarks = false
        }

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

        if(grid || bookmarks || collections || addCollection) {
            binding?.list?.layoutManager = GridLayoutManager(context, 3)
        }

        // Get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(requireActivity(), ProfileViewModelFactory(
            (if(!collections) ProfileContentRepository(
                apiHolder.setToCurrentUser(),
                accountId,
                bookmarks,
                if (addCollection) null else collectionId
            )
            else CollectionsContentRepository(apiHolder.setToCurrentUser(), accountId)) as UncachedContentRepository<FeedContent>
        )
        )[if (addCollection) "AddCollection" else if (collections) "Collections" else if(bookmarks) "Bookmarks" else "Profile",
                FeedViewModel::class.java] as FeedViewModel<FeedContent>

        launch()
        initSearch()

        return view
    }

    inner class ProfilePostsAdapter : PagingDataAdapter<FeedContent, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<FeedContent>() {
            override fun areItemsTheSame(oldItem: FeedContent, newItem: FeedContent): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FeedContent, newItem: FeedContent): Boolean =
                oldItem.id == newItem.id
        }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return if(collections) {
                    if (viewType == 1) {
                        val view =
                            LayoutInflater.from(parent.context)
                                .inflate(R.layout.create_new_collection, parent, false)
                        AddCollectionViewHolder(view)
                    } else CollectionsViewHolder.create(parent)
                }
                else if(grid || bookmarks) {
                    ProfilePostsViewHolder.create(parent)
                } else {
                    StatusViewHolder.create(parent)
                }
        }

        override fun getItemViewType(position: Int): Int {
            return if(position == 0 && user?.user_id == accountId) 1
            else 0
        }

        override fun getItemCount(): Int {
            return if (collections && user?.user_id == accountId) {
                super.getItemCount() + 1
            } else super.getItemCount()
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val post = if(collections && user?.user_id == accountId && position == 0) null else getItem(if(collections && user?.user_id == accountId) position - 1 else position)

            post?.let {
                if(collections) {
                    (holder as CollectionsViewHolder).bind(it as Collection)
                } else if(grid || bookmarks || addCollection) {
                    (holder as ProfilePostsViewHolder).bind(
                        it as Status,
                        lifecycleScope,
                        apiHolder.api ?: apiHolder.setToCurrentUser(),
                        addCollection,
                        collection,
                        deleteFromCollection
                    )
                } else {
                    (holder as StatusViewHolder).bind(
                        it as Status, apiHolder, db, lifecycleScope,
                        requireContext().displayDimensionsInPx(), requestPermissionDownloadPic
                    )
                }
            }

            if(collections && post == null){
                (holder as AddCollectionViewHolder).itemView.setOnClickListener {
                    val domain = user?.instance_uri
                    val url = "$domain/i/collections/create"

                    if(domain.isNullOrEmpty() || !requireContext().openUrl(url)) {
                        binding?.let { binding ->
                            Snackbar.make(
                                binding.root, getString(R.string.new_collection_link_failed),
                                Snackbar.LENGTH_LONG).show()
                        }
                    }
                }

            }
        }
    }
    class AddCollectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}


class CollectionsViewHolder(binding: FragmentProfilePostsBinding) : RecyclerView.ViewHolder(binding.root) {
    private val postPreview: ImageView = binding.postPreview
    private val albumIcon: ImageView = binding.albumIcon
    private val videoIcon: ImageView = binding.videoIcon

    fun bind(collection: Collection) {

        if (collection.post_count == 0){
            //No media in this collection, so put a little icon there
            postPreview.scaleX = 0.3f
            postPreview.scaleY = 0.3f
            Glide.with(postPreview).load(R.drawable.ic_comment_empty).into(postPreview)
            albumIcon.visibility = View.GONE
            videoIcon.visibility = View.GONE
        } else {
            postPreview.scaleX = 1f
            postPreview.scaleY = 1f
            setSquareImageFromURL(postPreview, collection.thumb, postPreview)
            if (collection.post_count > 1) {
                albumIcon.visibility = View.VISIBLE
            } else {
                albumIcon.visibility = View.GONE
            }
            videoIcon.visibility = View.GONE
        }

        postPreview.setOnClickListener {
            val intent = Intent(postPreview.context, CollectionActivity::class.java)
            intent.putExtra(CollectionActivity.COLLECTION_TAG, collection)
            postPreview.context.startActivity(intent)
        }
    }

    companion object {
        fun create(parent: ViewGroup): CollectionsViewHolder {
            val itemBinding = FragmentProfilePostsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return CollectionsViewHolder(itemBinding)
        }
    }
}

class ProfilePostsViewHolder(binding: FragmentProfilePostsBinding) : RecyclerView.ViewHolder(binding.root) {
    private val postPreview: ImageView = binding.postPreview
    private val albumIcon: ImageView = binding.albumIcon
    private val videoIcon: ImageView = binding.videoIcon

    fun bind(post: Status, lifecycleScope: LifecycleCoroutineScope, api: PixelfedAPI,
             addCollection: Boolean = false, collection: Collection? = null, deleteFromCollection: Boolean = false
    ) {

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
            if(addCollection && collection != null){
                lifecycleScope.launch {
                    try {
                        api.addToCollection(collection.id, post.id)
                        val intent = Intent(postPreview.context, CollectionActivity::class.java)
                            .apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra(ADD_TO_COLLECTION_RESULT, true)
                                putExtra(CollectionActivity.COLLECTION_TAG, collection)
                            }
                        postPreview.context.startActivity(intent)
                    } catch (exception: Exception) {
                        Snackbar.make(postPreview, postPreview.context.getString(R.string.error_add_post_to_collection),
                            Snackbar.LENGTH_LONG).show()
                    }
                }
            } else if (deleteFromCollection && (collection != null)){
                lifecycleScope.launch {
                    try {
                        api.removeFromCollection(collection.id, post.id)
                        val intent = Intent(postPreview.context, CollectionActivity::class.java)
                            .apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra(DELETE_FROM_COLLECTION_RESULT, true)
                                putExtra(CollectionActivity.COLLECTION_TAG, collection)
                            }
                        postPreview.context.startActivity(intent)
                    } catch (exception: Exception) {
                        Snackbar.make(postPreview, postPreview.context.getString(R.string.error_remove_post_from_collection),
                            Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            else {
                val intent = Intent(postPreview.context, PostActivity::class.java)
                intent.putExtra(Status.POST_TAG, post)
                postPreview.context.startActivity(intent)
            }
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
    private val searchContentRepository: UncachedContentRepository<FeedContent>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(searchContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
