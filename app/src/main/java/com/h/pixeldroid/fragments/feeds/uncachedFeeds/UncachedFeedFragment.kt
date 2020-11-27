package com.h.pixeldroid.fragments.feeds.uncachedFeeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

import com.h.pixeldroid.databinding.FragmentFeedBinding
import com.h.pixeldroid.fragments.BaseFragment
import com.h.pixeldroid.fragments.feeds.initAdapter
import com.h.pixeldroid.objects.FeedContent


/**
 * A fragment representing a list of [FeedContent], not backed by a db cache.
 */
open class UncachedFeedFragment<T: FeedContent> : BaseFragment() {

    internal lateinit var viewModel: FeedViewModel<T>
    internal lateinit var adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>

    private lateinit var binding: FragmentFeedBinding


    private var job: Job? = null


    internal fun launch() {
        // Make sure we cancel the previous job before creating a new one
        job?.cancel()
        job = lifecycleScope.launch {
            viewModel.flow().collectLatest {
                adapter.submitData(it)
            }
        }
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

        initAdapter(binding, adapter)

        binding.swipeRefreshLayout.setOnRefreshListener {
            //It shouldn't be necessary to also retry() in addition to refresh(),
            //but if we don't do this, reloads after an error fail immediately...
            adapter.retry()
            adapter.refresh()
        }

        return binding.root
    }
}

class ViewModelFactory<U: FeedContent> @ExperimentalPagingApi constructor(
    private val searchContentRepository: UncachedContentRepository<U>
) : ViewModelProvider.Factory {

    @ExperimentalPagingApi
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(searchContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}