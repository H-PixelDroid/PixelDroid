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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.pixeldroid.app.databinding.FragmentFeedBinding
import org.pixeldroid.app.posts.feeds.initAdapter
import org.pixeldroid.app.posts.feeds.launch
import org.pixeldroid.app.utils.BaseFragment
import org.pixeldroid.app.utils.api.objects.FeedContent


/**
 * A fragment representing a list of [FeedContent], not backed by a db cache.
 */
open class UncachedFeedFragment<T: FeedContent> : BaseFragment() {

    internal lateinit var viewModel: FeedViewModel<T>
    internal lateinit var adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>

    lateinit var binding: FragmentFeedBinding


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
                .collect { binding.list.scrollToPosition(0) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentFeedBinding.inflate(layoutInflater)

        initAdapter(binding.progressBar, binding.swipeRefreshLayout, binding.list,
            binding.motionLayout, binding.errorLayout, adapter)

        binding.swipeRefreshLayout.setOnRefreshListener {
            adapter.refresh()
        }

        return binding.root
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