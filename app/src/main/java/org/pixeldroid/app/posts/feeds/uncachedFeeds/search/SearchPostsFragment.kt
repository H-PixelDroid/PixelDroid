package org.pixeldroid.app.posts.feeds.uncachedFeeds.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.R
import org.pixeldroid.app.posts.StatusViewHolder
import org.pixeldroid.app.posts.feeds.uncachedFeeds.*
import org.pixeldroid.app.utils.api.objects.Results
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.displayDimensionsInPx

/**
 * Fragment to show a list of [Status]es, as a result of a search.
 */
class SearchPostsFragment : UncachedFeedFragment<Status>() {

    private lateinit var query: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = PostsAdapter(requireContext().displayDimensionsInPx())

        query = arguments?.getSerializable("searchFeed") as String

    }

    @ExperimentalPagingApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(this, ViewModelFactory(
            SearchContentRepository<Status>(
                apiHolder.setToCurrentUser(),
                Results.SearchType.statuses,
                query
            )
        )
        )
            .get(FeedViewModel::class.java) as FeedViewModel<Status>

        launch()
        initSearch()

        return view
    }

    inner class PostsAdapter(private val displayDimensionsInPx: Pair<Int, Int>) : PagingDataAdapter<Status, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<Status>() {
            override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean =
                oldItem.id == newItem.id
        }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return StatusViewHolder.create(parent)
        }

        override fun getItemViewType(position: Int): Int {
            return R.layout.post_fragment
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val uiModel = getItem(position) as Status
            uiModel.let {
                (holder as StatusViewHolder).bind(it, apiHolder, db, lifecycleScope, displayDimensionsInPx)
            }
        }
    }

}