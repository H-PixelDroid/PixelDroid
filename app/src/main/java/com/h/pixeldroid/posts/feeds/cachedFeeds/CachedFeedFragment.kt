package com.h.pixeldroid.posts.feeds.cachedFeeds

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
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.db.dao.feedContent.FeedContentDao
import com.h.pixeldroid.utils.BaseFragment
import com.h.pixeldroid.posts.feeds.initAdapter
import com.h.pixeldroid.utils.api.objects.FeedContentDatabase


/**
 * A fragment representing a list of [FeedContentDatabase] items that are cached by the database.
 */
open class CachedFeedFragment<T: FeedContentDatabase> : BaseFragment() {

    internal lateinit var viewModel: FeedViewModel<T>
    internal lateinit var adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>

    private lateinit var binding: FragmentFeedBinding


    private var job: Job? = null


    @ExperimentalPagingApi
    internal fun launch() {
        // Make sure we cancel the previous job before creating a new one
        job?.cancel()
        job = lifecycleScope.launchWhenStarted {
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

        //binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.setOnRefreshListener {
            //It shouldn't be necessary to also retry() in addition to refresh(),
            //but if we don't do this, reloads after an error fail immediately...
            // https://issuetracker.google.com/issues/173438474
            adapter.retry()
            adapter.refresh()
        }

        return binding.root
    }
}


/**
 * Factory that creates ViewModel from a [FeedContentRepository], to be used in cached feeds to
 * fetch the ViewModel that is responsible for preparing and managing the data
 * for a CachedFeedFragment
 */
class ViewModelFactory<U: FeedContentDatabase> @ExperimentalPagingApi constructor(private val db: AppDatabase?,
                                                                                  private val dao: FeedContentDao<U>?,
                                                                                  private val remoteMediator: RemoteMediator<Int, U>?,
                                                                                  private val feedContentRepository: FeedContentRepository<U> = FeedContentRepository(db!!, dao!!, remoteMediator!!)
) : ViewModelProvider.Factory {

    @ExperimentalPagingApi
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(feedContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}