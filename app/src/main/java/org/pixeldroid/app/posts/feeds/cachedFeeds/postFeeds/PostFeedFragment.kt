package org.pixeldroid.app.posts.feeds.cachedFeeds.postFeeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.paging.RemoteMediator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.R
import org.pixeldroid.app.utils.db.dao.feedContent.FeedContentDao
import org.pixeldroid.app.posts.StatusViewHolder
import org.pixeldroid.app.posts.feeds.cachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.cachedFeeds.CachedFeedFragment
import org.pixeldroid.app.posts.feeds.cachedFeeds.ViewModelFactory
import org.pixeldroid.app.utils.api.objects.FeedContentDatabase
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.displayDimensionsInPx
import kotlin.properties.Delegates


/**
 * Fragment for the home feed or public feed tabs.
 *
 * Takes a "home" boolean in its arguments [Bundle] to determine which
 */
@ExperimentalPagingApi
class PostFeedFragment<T: FeedContentDatabase>: CachedFeedFragment<T>() {

    private lateinit var mediator: RemoteMediator<Int, T>
    private lateinit var dao: FeedContentDao<T>
    private var home by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = PostsAdapter(requireContext().displayDimensionsInPx())

        home = requireArguments().get("home") as Boolean

        @Suppress("UNCHECKED_CAST")
        if (home){
            mediator = HomeFeedRemoteMediator(apiHolder, db) as RemoteMediator<Int, T>
            dao = db.homePostDao() as FeedContentDao<T>
        }
        else {
            mediator = PublicFeedRemoteMediator(apiHolder, db) as RemoteMediator<Int, T>
            dao = db.publicPostDao() as FeedContentDao<T>
        }
    }

    @ExperimentalPagingApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(requireActivity(), ViewModelFactory(db, dao, mediator))
            .get(if(home) "home" else "public", FeedViewModel::class.java) as FeedViewModel<T>

        launch()
        initSearch()

        return view
    }

    inner class PostsAdapter(private val displayDimensionsInPx: Pair<Int, Int>) : PagingDataAdapter<T, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame   (oldItem: T, newItem: T): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem.id == newItem.id
        }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return StatusViewHolder.create(parent)
        }

        override fun getItemViewType(position: Int): Int {
            return R.layout.post_fragment
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val uiModel = getItem(position) as Status?
            uiModel?.let {
                (holder as StatusViewHolder).bind(it, apiHolder, db, lifecycleScope, displayDimensionsInPx)
            }
        }
    }
}