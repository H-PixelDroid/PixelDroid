package org.pixeldroid.app.posts.feeds.uncachedFeeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.pixeldroid.app.databinding.FragmentFeedBinding
import org.pixeldroid.app.posts.feeds.initAdapter
import org.pixeldroid.app.posts.feeds.launch
import org.pixeldroid.app.utils.BaseFragment
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.limitedLengthSmoothScrollToPosition


/**
 * A fragment representing a list of [FeedContent], not backed by a db cache.
 */
open class UncachedFeedFragment<T: FeedContent> : BaseFragment() {

    internal lateinit var viewModel: FeedViewModel<T>
    internal lateinit var adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>

    var binding: FragmentFeedBinding? = null

    private var job: Job? = null


    internal fun launch() {
        job = launch(job, lifecycleScope, viewModel, adapter)
    }

    internal fun initSearch() {
        // Scroll to top when the list is refreshed from network.
        lifecycleScope.launch {
            adapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding?.list?.scrollToPosition(0) }
        }
    }

    fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?, swipeRefreshLayout: SwipeRefreshLayout?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentFeedBinding.inflate(layoutInflater)

        binding!!.let {
            initAdapter(
                it.progressBar, swipeRefreshLayout ?: it.swipeRefreshLayout, it.list,
                it.motionLayout, it.errorLayout, adapter
            )

        }
        return binding!!.root
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return onCreateView(inflater, container, savedInstanceState, null)
    }
    fun onTabReClicked() {
        binding?.list?.limitedLengthSmoothScrollToPosition(0)
    }
}

class ViewModelFactory<U: FeedContent> @ExperimentalPagingApi constructor(
    private val searchContentRepository: UncachedContentRepository<U>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(searchContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}