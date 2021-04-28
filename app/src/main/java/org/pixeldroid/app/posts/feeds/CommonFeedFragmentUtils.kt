package org.pixeldroid.app.posts.feeds

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ErrorLayoutBinding
import org.pixeldroid.app.databinding.LoadStateFooterViewItemBinding
import org.pixeldroid.app.posts.feeds.uncachedFeeds.FeedViewModel
import org.pixeldroid.app.utils.api.objects.FeedContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Shows or hides the error in the different FeedFragments
 */
private fun showError(
        errorText: String, show: Boolean = true,
        motionLayout: MotionLayout,
        errorLayout: ErrorLayoutBinding){

    if(show) {
        motionLayout.transitionToEnd()
        errorLayout.errorText.text = errorText
    } else if(motionLayout.progress == 1F) {
        motionLayout.transitionToStart()
    }
}

/**
 * Initialises the [RecyclerView] adapter for the different FeedFragments.
 *
 * Makes the UI respond to various [LoadState]s, including errors when an error message is shown.
 */
internal fun <T: Any> initAdapter(
    progressBar: ProgressBar, swipeRefreshLayout: SwipeRefreshLayout,
    recyclerView: RecyclerView, motionLayout: MotionLayout, errorLayout: ErrorLayoutBinding,
    adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>) {

    recyclerView.adapter = adapter.withLoadStateFooter(
        footer = ReposLoadStateAdapter { adapter.retry() }
    )

    adapter.addLoadStateListener { loadState ->

        if(!progressBar.isVisible && swipeRefreshLayout.isRefreshing) {
            // Stop loading spinner when loading is done
            swipeRefreshLayout.isRefreshing = loadState.mediator?.refresh is LoadState.Loading
        } else {
            // ProgressBar should stop showing as soon as the source stops loading ("source"
            // meaning the database, so don't wait on the network)
            val sourceLoading = loadState.source.refresh is LoadState.Loading
            if(!sourceLoading && recyclerView.size > 0){
                recyclerView.isVisible = true
                progressBar.isVisible = false
            } else if(recyclerView.size ==  0
                    && loadState.append is LoadState.NotLoading
                    && loadState.append.endOfPaginationReached){
                progressBar.isVisible = false
                showError(motionLayout = motionLayout, errorLayout = errorLayout,
                        errorText = errorLayout.root.context.getString(R.string.empty_feed))
            }
        }


        // Toast on any error, regardless of whether it came from RemoteMediator or PagingSource
        val errorState = loadState.source.append as? LoadState.Error
            ?: loadState.source.prepend as? LoadState.Error
            ?: loadState.source.refresh as? LoadState.Error
            ?: loadState.append as? LoadState.Error
            ?: loadState.prepend as? LoadState.Error
            ?: loadState.refresh as? LoadState.Error
        errorState?.let {
            showError(motionLayout = motionLayout, errorLayout = errorLayout, errorText = it.error.toString())
        }
        if(errorState == null) {
            showError(motionLayout = motionLayout, errorLayout = errorLayout, show = false, errorText = "")
        }
    }
}

fun <T: FeedContent> launch(
        job: Job?, lifecycleScope: LifecycleCoroutineScope, viewModel: FeedViewModel<T>,
        pagingDataAdapter: PagingDataAdapter<T, RecyclerView.ViewHolder>): Job {
    // Make sure we cancel the previous job before creating a new one
    job?.cancel()
    return lifecycleScope.launch {
        viewModel.flow.collectLatest {
            pagingDataAdapter.submitData(it)
        }
    }


}


/**
 * Adapter to the show the a [RecyclerView] item for a [LoadState], with a callback to retry if
 * the retry button is pressed.
 */
class ReposLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<ReposLoadStateViewHolder>() {
    override fun onBindViewHolder(holder: ReposLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ReposLoadStateViewHolder {
        return ReposLoadStateViewHolder.create(parent, retry)
    }
}

/**
 * [RecyclerView.ViewHolder] that is shown at the end of the feed to indicate loading or errors
 * in the loading of appending values.
 */
class ReposLoadStateViewHolder(
    private val binding: LoadStateFooterViewItemBinding,
    retry: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.retryButton.setOnClickListener { retry.invoke() }
    }

    fun bind(loadState: LoadState) {
        if (loadState is LoadState.Error) {
            binding.errorMsg.text = loadState.error.localizedMessage
        }
        binding.progressBar.isVisible = loadState is LoadState.Loading
        binding.retryButton.isVisible = loadState !is LoadState.Loading
        binding.errorMsg.isVisible = loadState !is LoadState.Loading
    }

    companion object {
        fun create(parent: ViewGroup, retry: () -> Unit): ReposLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.load_state_footer_view_item, parent, false)
            val binding = LoadStateFooterViewItemBinding.bind(view)
            return ReposLoadStateViewHolder(binding, retry)
        }
    }
}