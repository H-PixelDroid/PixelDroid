package org.pixeldroid.app.posts.feeds.uncachedFeeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.pixeldroid.app.R
import org.pixeldroid.app.posts.StatusViewHolder
import org.pixeldroid.app.posts.feeds.UIMODEL_STATUS_COMPARATOR
import org.pixeldroid.app.posts.feeds.uncachedFeeds.hashtags.HashTagContentRepository
import org.pixeldroid.app.posts.feeds.uncachedFeeds.search.SearchContentRepository
import org.pixeldroid.app.utils.api.objects.Results
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.api.objects.Tag.Companion.HASHTAG_TAG
import org.pixeldroid.app.utils.displayDimensionsInPx


/**
 * Fragment to show a list of [Status]es, as a result of a search or a hashtag.
 */
class UncachedPostsFragment : UncachedFeedFragment<Status>() {

    private var hashtagOrQuery: String? = null
    private var search: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = PostsAdapter(requireContext().displayDimensionsInPx())

        hashtagOrQuery = arguments?.getString(HASHTAG_TAG)

        if (hashtagOrQuery == null) {
            search = true
            hashtagOrQuery = arguments?.getString("searchFeed")!!
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = if(search) {
            ViewModelProvider(
                requireActivity(), ViewModelFactory(
                    SearchContentRepository<Status>(
                        apiHolder.setToCurrentUser(),
                        Results.SearchType.statuses,
                        hashtagOrQuery!!
                    )
                )
            )["searchPosts", FeedViewModel::class.java] as FeedViewModel<Status>
        } else {
            ViewModelProvider(requireActivity(), ViewModelFactory(
                HashTagContentRepository(
                    apiHolder.setToCurrentUser(),
                    hashtagOrQuery!!
                )
            )
            )[HASHTAG_TAG, FeedViewModel::class.java] as FeedViewModel<Status>
        }

        launch()
        initSearch()

        return view
    }

    inner class PostsAdapter(private val displayDimensionsInPx: Pair<Int, Int>)
        : PagingDataAdapter<Status, RecyclerView.ViewHolder>(UIMODEL_STATUS_COMPARATOR) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return StatusViewHolder.create(parent)
        }

        override fun getItemViewType(position: Int): Int = R.layout.post_fragment

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            getItem(position)?.let {
                (holder as StatusViewHolder).bind(
                    it, apiHolder, db, lifecycleScope, displayDimensionsInPx, requestPermissionDownloadPic
                )
            }
        }
    }

}