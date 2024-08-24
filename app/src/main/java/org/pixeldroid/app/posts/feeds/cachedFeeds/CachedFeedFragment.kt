package org.pixeldroid.app.posts.feeds.cachedFeeds

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.paging.RemoteMediator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.pixeldroid.app.databinding.FragmentFeedBinding
import org.pixeldroid.app.posts.feeds.BounceEdgeEffectFactory
import org.pixeldroid.app.posts.feeds.initAdapter
import org.pixeldroid.app.stories.StoriesAdapter
import org.pixeldroid.app.utils.BaseFragment
import org.pixeldroid.app.utils.api.objects.FeedContentDatabase
import org.pixeldroid.app.utils.bindingLifecycleAware
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.dao.feedContent.FeedContentDao
import org.pixeldroid.app.utils.limitedLengthSmoothScrollToPosition

/**
 * A fragment representing a list of [FeedContentDatabase] items that are cached by the database.
 */
open class CachedFeedFragment<T: FeedContentDatabase> : BaseFragment() {

    internal lateinit var viewModel: FeedViewModel<T>
    internal lateinit var adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>
    internal var headerAdapter: StoriesAdapter? = null

    private var binding: FragmentFeedBinding by bindingLifecycleAware()


    private var job: Job? = null


    @ExperimentalPagingApi
    internal fun launch() {
        // Make sure we cancel the previous job before creating a new one
        job?.cancel()
        job = lifecycleScope.launchWhenStarted {
            viewModel.flow.collectLatest {
                adapter.submitData(it)
            }
        }
    }

    //TODO rename function to something that makes sense
    internal fun initSearch() {
        // Scroll to top when the list is refreshed from network.
//        lifecycleScope.launchWhenStarted {
//            adapter.loadStateFlow
//                // Only emit when REFRESH LoadState for RemoteMediator changes.
//                .distinctUntilChangedBy {
//                    it.refresh
//                }
//                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
//                .filter { it.refresh is NotLoading}
//                .collect { binding.list.scrollToPosition(0) }
//        }
    }

    fun createView(inflater: LayoutInflater, container: ViewGroup?,
                   savedInstanceState: Bundle?, reverseLayout: Boolean = false): ConstraintLayout {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentFeedBinding.inflate(layoutInflater)

        val callback: () -> Unit = {
            binding.bottomLoadingBar.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.Main) {
                delay(1000) // Wait 1 second
                binding.bottomLoadingBar.visibility = View.GONE
            }
            adapter.refresh()
            adapter.notifyDataSetChanged()
        }

        val swipeRefreshLayout = if(reverseLayout) {
            binding.swipeRefreshLayout.isEnabled = false
            binding.list.apply {
                layoutManager = LinearLayoutManager(context).apply {
                    stackFromEnd = false
                    this.reverseLayout = true
                }
                edgeEffectFactory = BounceEdgeEffectFactory(callback, context)
            }
            null
        } else binding.swipeRefreshLayout

        initAdapter(binding.progressBar, swipeRefreshLayout,
            binding.list, binding.motionLayout, binding.errorLayout, adapter,
            headerAdapter
        )

        return binding.root
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return createView(inflater, container, savedInstanceState, false)
    }

    fun onTabReClicked() {
        binding.list.limitedLengthSmoothScrollToPosition(0)
    }

    private fun onPullUp() {
        // Handle the pull-up action
        Log.e("bottom", "reached")
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

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(feedContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}